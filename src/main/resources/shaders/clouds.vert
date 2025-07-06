/**
 * Enhanced Cloud Vertex Shader
 * 
 * Features:
 * - Robust vertex processing for fullscreen cloud rendering
 * - Input validation
 * - Multiple output variables for cloud effects
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// INPUT ATTRIBUTES
// ========================================================================
layout (location = 0) in vec2 aPosition;    // Vertex position
layout (location = 1) in vec2 aTexCoord;    // Texture coordinates

// ========================================================================
// OUTPUT VARIABLES
// ========================================================================
out vec2 TexCoord;          // Texture coordinates for fragment shader
out vec3 ViewRay;           // View ray for raymarching
out float FragDepth;        // Fragment depth

// ========================================================================
// UNIFORMS
// ========================================================================
uniform mat4 invProjection; // Inverse projection matrix
uniform mat4 invView;       // Inverse view matrix
uniform bool enableViewRay; // Enable view ray calculation

// ========================================================================
// CONSTANTS
// ========================================================================
const float EPSILON = 1e-6;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate vec2 input
 */
vec2 validateVec2(vec2 value, vec2 defaultVal) {
    if (any(isnan(value)) || any(isinf(value))) return defaultVal;
    return value;
}

/**
 * Calculate view ray for raymarching
 */
vec3 calculateViewRay(vec2 texCoord) {
    if (!enableViewRay) return vec3(0.0, 0.0, 1.0);
    
    vec4 ray_clip = vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
    vec4 ray_view = invProjection * ray_clip;
    ray_view.z = -1.0;
    ray_view.w = 0.0;
    
    vec3 ray_world = vec3(invView * ray_view);
    return normalize(ray_world);
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main() {
    // Input validation
    vec2 safePosition = validateVec2(aPosition, vec2(0.0));
    vec2 safeTexCoord = validateVec2(aTexCoord, vec2(0.5));
    
    // Clamp inputs to valid ranges
    safePosition = clamp(safePosition, vec2(-1.0), vec2(1.0));
    safeTexCoord = clamp(safeTexCoord, vec2(0.0), vec2(1.0));
    
    // Set output variables
    TexCoord = safeTexCoord;
    ViewRay = calculateViewRay(safeTexCoord);
    FragDepth = 0.5; // Middle depth for clouds
    
    // Set vertex position
    gl_Position = vec4(safePosition, 0.0, 1.0);
    
    // Validate final position
    if (any(isnan(gl_Position)) || any(isinf(gl_Position))) {
        gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
    }
}