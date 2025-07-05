/**
 * Enhanced Shadow Mapping Vertex Shader
 * 
 * Features:
 * - Robust vertex transformation for shadow mapping
 * - Wind animation support for foliage
 * - Multiple output variables for advanced shadow techniques
 * - Input validation
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
layout (location = 1) in vec3 aNormal;     // Vertex normal (for wind)
layout (location = 2) in vec2 aTexCoord;   // Texture coordinates
layout (location = 3) in float aMaterialID; // Material identifier

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec2 TexCoords;     // Texture coordinates for alpha testing
out float MaterialID;   // Material identifier
out float FragDepth;    // Fragment depth for advanced shadow mapping

// ========================================================================
// UNIFORMS - TRANSFORMATION MATRICES
// ========================================================================
uniform mat4 lightSpaceMatrix;  // Light space transformation matrix
uniform mat4 model;             // Model matrix

// ========================================================================
// UNIFORMS - ENVIRONMENTAL (for wind animation)
// ========================================================================
uniform float time;             // Time for animations
uniform float windStrength;     // Wind strength [0.0, 1.0]
uniform vec3 windDirection;     // Wind direction (normalized)
uniform float windFrequency;    // Wind frequency
uniform bool enableWindAnimation; // Enable wind effects

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;
const float WIND_SCALE = 0.1;

// Material type constants
const float MATERIAL_FOLIAGE = 1.0;
const float MATERIAL_WOOD = 2.0;

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
 * Calculate wind displacement for foliage (simplified for shadow pass)
 */
vec3 calculateWindDisplacement(vec3 worldPos, float materialID, float time) {
    if (!enableWindAnimation || materialID != MATERIAL_FOLIAGE) {
        return vec3(0.0);
    }
    
    // Simplified wind calculation for shadow pass (performance)
    float windTime = time * windFrequency;
    vec3 windPos = worldPos * 0.1;
    
    // Single wind wave for shadows
    float wind = sin(windTime + windPos.x * 2.0 + windPos.z * 1.5) * windStrength * WIND_SCALE;
    
    // Apply wind primarily in the wind direction
    vec3 displacement = windDirection * wind;
    displacement.y *= 0.2; // Minimal vertical displacement for shadows
    
    return displacement;
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Input validation
    vec3 safePos = validateVec3(aPos, vec3(0.0));
    vec2 safeTexCoord = clamp(aTexCoord, vec2(0.0), vec2(1.0));
    float safeMaterialID = validateFloat(aMaterialID, 0.0);
    
    // World space transformation
    vec4 worldPos4 = model * vec4(safePos, 1.0);
    vec3 worldPos = worldPos4.xyz;
    
    // Apply wind displacement (if enabled)
    vec3 windDisplacement = calculateWindDisplacement(worldPos, safeMaterialID, time);
    worldPos += windDisplacement;
    
    // Transform to light space
    vec4 lightSpacePos = lightSpaceMatrix * vec4(worldPos, 1.0);
    
    // Output variables
    TexCoords = safeTexCoord;
    MaterialID = safeMaterialID;
    FragDepth = lightSpacePos.z / lightSpacePos.w; // Normalized depth
    
    // Final position
    gl_Position = lightSpacePos;
    
    // Ensure valid output
    if (any(isnan(gl_Position)) || any(isinf(gl_Position))) {
        gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
    }
}