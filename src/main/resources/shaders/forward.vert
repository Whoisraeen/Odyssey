/**
 * Enhanced Forward Rendering Vertex Shader
 * 
 * Features:
 * - Robust vertex transformation
 * - Tangent space calculation
 * - Wind animation for foliage
 * - Input validation
 * - Multiple output variables
 * - Performance optimizations
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// INPUT ATTRIBUTES
// ========================================================================
layout (location = 0) in vec3 aPos;        // Vertex position
layout (location = 1) in vec3 aNormal;     // Vertex normal
layout (location = 2) in vec2 aTexCoord;   // Texture coordinates
layout (location = 3) in vec3 aTangent;    // Tangent vector
layout (location = 4) in float aMaterialID; // Material identifier

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec3 FragPos;       // World space position
out vec3 Normal;        // World space normal
out vec2 TexCoord;      // Texture coordinates
out vec3 Tangent;       // World space tangent
out vec3 Bitangent;     // World space bitangent
out float MaterialID;   // Material identifier
out float VertexDepth;  // Vertex depth for effects
out vec3 ViewPos;       // View space position

// ========================================================================
// UNIFORMS - TRANSFORMATION MATRICES
// ========================================================================
uniform mat4 model;         // Model matrix
uniform mat4 view;          // View matrix
uniform mat4 projection;    // Projection matrix
uniform mat3 normalMatrix;  // Pre-computed normal matrix

// ========================================================================
// UNIFORMS - ENVIRONMENTAL
// ========================================================================
uniform float time;             // Time for animations
uniform float windStrength;     // Wind strength [0.0, 1.0]
uniform vec3 windDirection;     // Wind direction (normalized)
uniform float windFrequency;    // Wind frequency

// ========================================================================
// UNIFORMS - QUALITY AND FEATURES
// ========================================================================
uniform bool enableWindAnimation;   // Enable wind effects
uniform bool enableTangentSpace;    // Enable tangent space calculation
uniform int vertexQuality;          // Vertex processing quality [0-2]

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;
const float PI = 3.14159265359;
const float WIND_SCALE = 0.1;
const float FOLIAGE_THRESHOLD = 0.5;

// Material type constants
const float MATERIAL_FOLIAGE = 1.0;
const float MATERIAL_WOOD = 2.0;
const float MATERIAL_STONE = 3.0;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate float input
 */
float validateFloat(float value, float defaultVal) {
    return (isnan(value) || isinf(value)) ? defaultVal : value;
}

/**
 * Validate vec3 input
 */
vec3 validateVec3(vec3 value, vec3 defaultVal) {
    if (any(isnan(value)) || any(isinf(value))) return defaultVal;
    return value;
}

/**
 * Calculate wind displacement for foliage
 */
vec3 calculateWindDisplacement(vec3 worldPos, float materialID, float time) {
    if (!enableWindAnimation || materialID != MATERIAL_FOLIAGE) {
        return vec3(0.0);
    }
    
    // Multi-octave wind simulation
    float windTime = time * windFrequency;
    vec3 windPos = worldPos * 0.1;
    
    // Primary wind wave
    float wind1 = sin(windTime + windPos.x * 2.0 + windPos.z * 1.5) * 0.5;
    // Secondary wind wave
    float wind2 = sin(windTime * 1.3 + windPos.x * 1.2 + windPos.z * 2.1) * 0.3;
    // Tertiary wind wave (gusts)
    float wind3 = sin(windTime * 0.7 + windPos.x * 0.8 + windPos.z * 0.9) * 0.2;
    
    float totalWind = (wind1 + wind2 + wind3) * windStrength * WIND_SCALE;
    
    // Apply wind primarily in the wind direction with some vertical component
    vec3 displacement = windDirection * totalWind;
    displacement.y *= 0.3; // Reduce vertical displacement
    
    return displacement;
}

/**
 * Calculate tangent space vectors
 */
void calculateTangentSpace(vec3 normal, vec3 tangent, out vec3 outTangent, out vec3 outBitangent) {
    if (!enableTangentSpace || length(tangent) < EPSILON) {
        // Generate tangent space from normal
        vec3 up = abs(normal.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        outTangent = normalize(cross(up, normal));
        outBitangent = cross(normal, outTangent);
    } else {
        // Use provided tangent
        outTangent = normalize(tangent);
        outBitangent = normalize(cross(normal, outTangent));
    }
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Input validation
    vec3 safePos = validateVec3(aPos, vec3(0.0));
    vec3 safeNormal = validateVec3(normalize(aNormal), vec3(0.0, 1.0, 0.0));
    vec2 safeTexCoord = clamp(aTexCoord, vec2(0.0), vec2(1.0));
    vec3 safeTangent = validateVec3(aTangent, vec3(1.0, 0.0, 0.0));
    float safeMaterialID = validateFloat(aMaterialID, 0.0);
    
    // World space transformation
    vec4 worldPos4 = model * vec4(safePos, 1.0);
    vec3 worldPos = worldPos4.xyz;
    
    // Apply wind displacement
    vec3 windDisplacement = calculateWindDisplacement(worldPos, safeMaterialID, time);
    worldPos += windDisplacement;
    
    // Update world position
    FragPos = worldPos;
    
    // Normal transformation using pre-computed normal matrix
    Normal = normalize(normalMatrix * safeNormal);
    
    // Tangent space calculation
    vec3 worldTangent, worldBitangent;
    calculateTangentSpace(Normal, normalize(normalMatrix * safeTangent), worldTangent, worldBitangent);
    
    Tangent = worldTangent;
    Bitangent = worldBitangent;
    
    // Texture coordinates
    TexCoord = safeTexCoord;
    
    // Material ID
    MaterialID = safeMaterialID;
    
    // View space calculations
    vec4 viewPos4 = view * vec4(worldPos, 1.0);
    ViewPos = viewPos4.xyz;
    VertexDepth = -viewPos4.z; // Positive depth
    
    // Final projection
    gl_Position = projection * viewPos4;
    
    // Ensure valid output
    if (any(isnan(gl_Position)) || any(isinf(gl_Position))) {
        gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
    }
}