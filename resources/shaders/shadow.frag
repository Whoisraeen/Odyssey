/**
 * Enhanced Shadow Mapping Fragment Shader
 * 
 * Features:
 * - Depth-only rendering for shadow maps
 * - Alpha testing for transparent materials
 * - Variance shadow mapping support
 * - Exponential shadow mapping support
 * - Input validation
 * - Performance optimizations
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// INPUT VARIABLES
// ========================================================================
in vec2 TexCoords;      // Texture coordinates
in float MaterialID;    // Material identifier
in float FragDepth;     // Fragment depth

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec4 FragColor;     // Output for VSM/ESM

// ========================================================================
// UNIFORMS - TEXTURES
// ========================================================================
uniform sampler2D albedoMap;        // Albedo texture for alpha testing
uniform sampler2D opacityMap;       // Opacity map

// ========================================================================
// UNIFORMS - MATERIAL PROPERTIES
// ========================================================================
uniform float alphaThreshold;       // Alpha cutoff threshold [0.0, 1.0]
uniform bool enableAlphaTesting;    // Enable alpha testing
uniform bool enableOpacityMap;      // Use opacity map

// ========================================================================
// UNIFORMS - SHADOW MAPPING
// ========================================================================
uniform int shadowMapType;          // 0=Standard, 1=VSM, 2=ESM
uniform float vsmBias;              // VSM bias
uniform float esmExponent;          // ESM exponent

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;
const float ESM_CONSTANT = 80.0;

// Material type constants
const float MATERIAL_TRANSPARENT = 4.0;
const float MATERIAL_FOLIAGE = 1.0;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Safe texture sampling with fallback
 */
vec4 safeTexture(sampler2D tex, vec2 coords, vec4 fallback) {
    vec2 safeCoords = clamp(coords, vec2(0.0), vec2(1.0));
    return texture(tex, safeCoords);
}

/**
 * Calculate alpha value for alpha testing
 */
float calculateAlpha(vec2 texCoords, float materialID) {
    if (!enableAlphaTesting) return 1.0;
    
    float alpha = 1.0;
    
    // Sample opacity from opacity map if enabled
    if (enableOpacityMap) {
        alpha *= safeTexture(opacityMap, texCoords, vec4(1.0)).r;
    }
    
    // Sample alpha from albedo map for transparent materials
    if (materialID == MATERIAL_TRANSPARENT || materialID == MATERIAL_FOLIAGE) {
        alpha *= safeTexture(albedoMap, texCoords, vec4(1.0)).a;
    }
    
    return alpha;
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Input validation
    vec2 safeTexCoords = clamp(TexCoords, vec2(0.0), vec2(1.0));
    float safeMaterialID = MaterialID;
    
    // Alpha testing
    if (enableAlphaTesting) {
        float alpha = calculateAlpha(safeTexCoords, safeMaterialID);
        if (alpha < alphaThreshold) {
            discard;
        }
    }
    
    // Shadow map output based on type
    if (shadowMapType == 1) {
        // Variance Shadow Mapping (VSM)
        float depth = gl_FragCoord.z;
        float moment1 = depth;
        float moment2 = depth * depth;
        
        // Add bias to reduce light bleeding
        moment2 += vsmBias;
        
        FragColor = vec4(moment1, moment2, 0.0, 1.0);
    }
    else if (shadowMapType == 2) {
        // Exponential Shadow Mapping (ESM)
        float depth = gl_FragCoord.z;
        float exponential = exp(esmExponent * depth);
        FragColor = vec4(exponential, 0.0, 0.0, 1.0);
    }
    else {
        // Standard depth-only shadow mapping
         // gl_FragDepth is automatically written
         FragColor = vec4(1.0); // Not used in standard shadow mapping
     }
 }