#version 330 core

/**
 * Enhanced Geometry Vertex Shader
 * 
 * Features:
 * - Robust vertex attribute processing
 * - Optimized normal transformation
 * - Enhanced texture coordinate handling
 * - Wind animation support for foliage
 * - Vertex-based material classification
 * - Error handling and validation
 * 
 * Author: Odyssey Engine
 * Version: 2.0
 * Last Modified: 2024
 */

// ========================================================================
// INPUT ATTRIBUTES
// ========================================================================
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in vec3 aTangent;     // Optional tangent for normal mapping
layout (location = 4) in float aMaterialID; // Optional material ID

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec3 Tangent;
out vec3 Bitangent;
out float MaterialID;
out float VertexDepth;
out vec3 ViewPos;

// ========================================================================
// UNIFORM MATRICES
// ========================================================================
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 normalMatrix; // Pre-computed normal matrix for efficiency

// ========================================================================
// ENVIRONMENTAL UNIFORMS
// ========================================================================
uniform float u_time;           // Global time for animations
uniform float u_windStrength;   // Wind strength [0.0, 1.0]
uniform vec3 u_windDirection;   // Wind direction (normalized)
uniform float u_windFrequency;  // Wind frequency multiplier

// ========================================================================
// QUALITY AND FEATURE FLAGS
// ========================================================================
uniform bool u_enableWindAnimation;  // Enable wind effects
uniform bool u_enableTangentSpace;   // Enable tangent space calculations
uniform int u_vertexQuality;         // Vertex processing quality level

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;
const float PI = 3.14159265359;
const float WIND_SCALE = 0.1;
const float FOLIAGE_THRESHOLD = 0.5; // Y-position threshold for foliage detection

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validates and clamps a float value within specified range
 */
float validateFloat(float value, float minVal, float maxVal, float defaultVal) {
    if (isnan(value) || isinf(value)) {
        return defaultVal;
    }
    return clamp(value, minVal, maxVal);
}

/**
 * Validates a 3D vector for NaN/Inf values
 */
vec3 validateVec3(vec3 v, vec3 defaultVal) {
    if (any(isnan(v)) || any(isinf(v))) {
        return defaultVal;
    }
    return v;
}

/**
 * Calculates wind displacement for foliage vertices
 */
vec3 calculateWindDisplacement(vec3 worldPos, vec3 normal, float materialID) {
    if (!u_enableWindAnimation || u_windStrength < EPSILON) {
        return vec3(0.0);
    }
    
    // Detect foliage based on position and material ID
    bool isFoliage = (worldPos.y > FOLIAGE_THRESHOLD) || (materialID > 0.5 && materialID < 1.5);
    
    if (!isFoliage) {
        return vec3(0.0);
    }
    
    // Calculate wind effect based on position and time
    float windPhase = u_time * u_windFrequency + worldPos.x * 0.1 + worldPos.z * 0.1;
    float windWave = sin(windPhase) * 0.5 + 0.5;
    
    // Add some noise for more natural movement
    float windNoise = sin(windPhase * 2.3) * 0.3 + sin(windPhase * 4.7) * 0.2;
    
    // Calculate displacement
    vec3 windDisplacement = u_windDirection * u_windStrength * WIND_SCALE;
    windDisplacement *= (windWave + windNoise) * 0.5;
    
    // Reduce displacement for lower parts of foliage
    float heightFactor = clamp((worldPos.y - FOLIAGE_THRESHOLD) / 2.0, 0.0, 1.0);
    windDisplacement *= heightFactor;
    
    return windDisplacement;
}

/**
 * Calculates tangent space vectors
 */
void calculateTangentSpace(vec3 normal, out vec3 tangent, out vec3 bitangent) {
    // Use input tangent if available and valid
    if (u_enableTangentSpace && length(aTangent) > EPSILON) {
        tangent = normalize(aTangent);
        bitangent = normalize(cross(normal, tangent));
    } else {
        // Generate tangent space from normal
        vec3 up = abs(normal.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        tangent = normalize(cross(up, normal));
        bitangent = cross(normal, tangent);
    }
}

void main() {
    // ========================================================================
    // INPUT VALIDATION
    // ========================================================================
    
    // Validate input attributes
    vec3 safePos = validateVec3(aPos, vec3(0.0));
    vec3 safeNormal = validateVec3(aNormal, vec3(0.0, 1.0, 0.0));
    vec2 safeTexCoords = clamp(aTexCoords, vec2(0.0), vec2(1.0));
    float safeMaterialID = validateFloat(aMaterialID, 0.0, 10.0, 0.0);
    
    // Normalize normal if it's not zero
    if (length(safeNormal) > EPSILON) {
        safeNormal = normalize(safeNormal);
    } else {
        safeNormal = vec3(0.0, 1.0, 0.0); // Default upward normal
    }
    
    // ========================================================================
    // WORLD SPACE TRANSFORMATION
    // ========================================================================
    
    // Transform to world space
    vec4 worldPos4 = model * vec4(safePos, 1.0);
    vec3 worldPos = worldPos4.xyz / worldPos4.w; // Handle perspective division
    
    // Apply wind displacement
    vec3 windDisplacement = calculateWindDisplacement(worldPos, safeNormal, safeMaterialID);
    worldPos += windDisplacement;
    
    // ========================================================================
    // NORMAL TRANSFORMATION
    // ========================================================================
    
    // Transform normal to world space using pre-computed normal matrix
    vec3 worldNormal;
    if (u_vertexQuality >= 1) {
        // Use pre-computed normal matrix for better performance
        worldNormal = normalize((normalMatrix * vec4(safeNormal, 0.0)).xyz);
    } else {
        // Fallback to manual calculation
        worldNormal = normalize(mat3(transpose(inverse(model))) * safeNormal);
    }
    
    // ========================================================================
    // TANGENT SPACE CALCULATION
    // ========================================================================
    
    vec3 worldTangent, worldBitangent;
    calculateTangentSpace(worldNormal, worldTangent, worldBitangent);
    
    // ========================================================================
    // VIEW SPACE CALCULATIONS
    // ========================================================================
    
    vec4 viewPos4 = view * vec4(worldPos, 1.0);
    vec3 viewPos = viewPos4.xyz / viewPos4.w;
    
    // Calculate vertex depth for various effects
    float vertexDepth = -viewPos.z; // Positive depth in view space
    
    // ========================================================================
    // FINAL PROJECTION
    // ========================================================================
    
    gl_Position = projection * viewPos4;
    
    // ========================================================================
    // OUTPUT ASSIGNMENT
    // ========================================================================
    
    FragPos = worldPos;
    Normal = worldNormal;
    TexCoords = safeTexCoords;
    Tangent = worldTangent;
    Bitangent = worldBitangent;
    MaterialID = safeMaterialID;
    VertexDepth = vertexDepth;
    ViewPos = viewPos;
}