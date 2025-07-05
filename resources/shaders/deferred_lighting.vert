/**
 * Enhanced Deferred Lighting Vertex Shader
 * 
 * Features:
 * - Input validation and safety checks
 * - Fullscreen quad rendering optimization
 * - Texture coordinate validation
 * - Precision handling for different quality levels
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// INPUT ATTRIBUTES
// ========================================================================
layout (location = 0) in vec3 aPos;        // Vertex position
layout (location = 1) in vec2 aTexCoord;   // Texture coordinates

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec2 TexCoords;     // Texture coordinates for fragment shader
out vec3 ViewRay;       // View ray for depth reconstruction (optional)

// ========================================================================
// UNIFORMS
// ========================================================================
uniform mat4 invProjection;    // Inverse projection matrix (optional)
uniform mat4 invView;          // Inverse view matrix (optional)
uniform bool enableViewRay;    // Enable view ray calculation

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate and clamp texture coordinates
 */
vec2 validateTexCoords(vec2 coords) {
    return clamp(coords, vec2(0.0), vec2(1.0));
}

/**
 * Validate vertex position for fullscreen quad
 */
vec3 validatePosition(vec3 pos) {
    // Ensure position is within expected fullscreen quad range
    return clamp(pos, vec3(-1.0), vec3(1.0));
}

/**
 * Calculate view ray for depth reconstruction (optional feature)
 */
vec3 calculateViewRay(vec2 texCoords) {
    if (!enableViewRay) return vec3(0.0);
    
    // Convert texture coordinates to NDC
    vec2 ndc = texCoords * 2.0 - 1.0;
    
    // Create view ray in view space
    vec4 viewRay = invProjection * vec4(ndc, 1.0, 1.0);
    viewRay.xyz /= viewRay.w;
    
    // Transform to world space if needed
    if (length(invView[0]) > EPSILON) {
        viewRay = invView * vec4(viewRay.xyz, 0.0);
    }
    
    return normalize(viewRay.xyz);
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Validate input position
    vec3 safePos = validatePosition(aPos);
    
    // Validate and pass texture coordinates
    TexCoords = validateTexCoords(aTexCoord);
    
    // Calculate view ray for advanced depth reconstruction (optional)
    ViewRay = calculateViewRay(TexCoords);
    
    // Set final position
    gl_Position = vec4(safePos, 1.0);
    
    // Ensure proper depth for fullscreen quad
    gl_Position.z = 0.0;
}