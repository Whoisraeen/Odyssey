/**
 * Enhanced Skybox Vertex Shader
 * 
 * Features:
 * - Robust vertex transformation for skybox rendering
 * - Multiple output variables for atmospheric effects
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
layout (location = 0) in vec3 aPos;    // Vertex position

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec3 TexCoords;     // Texture coordinates for skybox sampling
out vec3 WorldPos;      // World position for atmospheric calculations
out vec3 ViewDir;       // View direction

// ========================================================================
// UNIFORMS - TRANSFORMATION MATRICES
// ========================================================================
uniform mat4 view;          // View matrix (without translation)
uniform mat4 projection;    // Projection matrix

// ========================================================================
// UNIFORMS - CAMERA
// ========================================================================
uniform vec3 cameraPos;     // Camera position

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate vec3 input
 */
vec3 validateVec3(vec3 value, vec3 defaultVal) {
    if (any(isnan(value)) || any(isinf(value))) return defaultVal;
    return value;
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Input validation
    vec3 safePos = validateVec3(aPos, vec3(0.0, 1.0, 0.0));
    
    // Use position as texture coordinates for skybox
    TexCoords = safePos;
    
    // Calculate world position (skybox is centered at camera)
    WorldPos = safePos + cameraPos;
    
    // View direction is the same as position for skybox
    ViewDir = normalize(safePos);
    
    // Transform position to clip space
    // Remove translation from view matrix to keep skybox centered
    mat4 viewNoTranslation = mat4(mat3(view));
    vec4 pos = projection * viewNoTranslation * vec4(safePos, 1.0);
    
    // Set z to w to ensure skybox is always at far plane
    gl_Position = pos.xyww;
    
    // Ensure valid output
    if (any(isnan(gl_Position)) || any(isinf(gl_Position))) {
        gl_Position = vec4(0.0, 0.0, 1.0, 1.0);
    }
}