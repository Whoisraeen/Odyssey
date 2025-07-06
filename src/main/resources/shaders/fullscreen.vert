#version 450 core

/**
 * Enhanced Fullscreen Vertex Shader
 * 
 * Features:
 * - Comprehensive input validation and error handling
 * - Multiple output variables for advanced post-processing
 * - Screen space coordinate calculations
 * - Utility functions for coordinate transformations
 * - Performance optimizations and safety checks
 * - Support for various fullscreen rendering techniques
 * 
 * Author: Enhanced Shader System
 * Version: 2.0
 * Last Modified: 2024
 */

// ============================================================================
// INPUT ATTRIBUTES
// ============================================================================

layout (location = 0) in vec2 aPosition;     // Screen space position [-1, 1]
layout (location = 1) in vec2 aTexCoord;     // Texture coordinates [0, 1]

// ============================================================================
// OUTPUT VARIABLES
// ============================================================================

out vec2 TexCoord;           // Primary texture coordinates
out vec2 ScreenPos;          // Screen position [0, 1]
out vec2 NDCPos;             // Normalized device coordinates [-1, 1]
out vec3 ViewRay;            // View ray for screen space calculations
out float FragDepth;         // Fragment depth for depth-aware effects

// ============================================================================
// UNIFORMS
// ============================================================================

// Camera and projection matrices
uniform mat4 u_invProjection;        // Inverse projection matrix
uniform mat4 u_invView;              // Inverse view matrix
uniform vec3 u_cameraPos;            // Camera world position
uniform vec2 u_screenSize;           // Screen dimensions
uniform float u_nearPlane;           // Near clipping plane
uniform float u_farPlane;            // Far clipping plane

// Feature toggles
uniform bool u_enableViewRay;        // Enable view ray calculation
uniform bool u_enableDepthCalc;      // Enable depth calculations
uniform bool u_enableValidation;     // Enable input validation

// ============================================================================
// CONSTANTS
// ============================================================================

const float EPSILON = 1e-6;
const vec2 VALID_RANGE_POS = vec2(-1.0, 1.0);    // Valid position range
const vec2 VALID_RANGE_TEX = vec2(0.0, 1.0);     // Valid texture coord range

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Validates a vec2 value within specified range
 * @param value Input value to validate
 * @param minVal Minimum valid value
 * @param maxVal Maximum valid value
 * @param fallback Fallback value if invalid
 * @return Validated value
 */
vec2 validateVec2(vec2 value, vec2 minVal, vec2 maxVal, vec2 fallback) {
    if (u_enableValidation) {
        bool isValid = all(greaterThanEqual(value, minVal)) && 
                      all(lessThanEqual(value, maxVal)) &&
                      !any(isnan(value)) && !any(isinf(value));
        return isValid ? value : fallback;
    }
    return value;
}

/**
 * Validates a float value within specified range
 * @param value Input value to validate
 * @param minVal Minimum valid value
 * @param maxVal Maximum valid value
 * @param fallback Fallback value if invalid
 * @return Validated value
 */
float validateFloat(float value, float minVal, float maxVal, float fallback) {
    if (u_enableValidation) {
        bool isValid = value >= minVal && value <= maxVal && 
                      !isnan(value) && !isinf(value);
        return isValid ? value : fallback;
    }
    return value;
}

/**
 * Converts screen position to view ray
 * @param screenPos Screen position in [0, 1] range
 * @return View ray in world space
 */
vec3 calculateViewRay(vec2 screenPos) {
    if (!u_enableViewRay) {
        return vec3(0.0, 0.0, -1.0); // Default forward direction
    }
    
    // Convert to NDC
    vec2 ndc = screenPos * 2.0 - 1.0;
    
    // Create clip space position
    vec4 clipPos = vec4(ndc, 1.0, 1.0);
    
    // Transform to view space
    vec4 viewPos = u_invProjection * clipPos;
    viewPos.xyz /= viewPos.w;
    
    // Transform to world space
    vec4 worldPos = u_invView * vec4(viewPos.xyz, 0.0);
    
    return normalize(worldPos.xyz);
}

/**
 * Calculates normalized fragment depth
 * @param position Screen position
 * @return Normalized depth value [0, 1]
 */
float calculateFragmentDepth(vec2 position) {
    if (!u_enableDepthCalc) {
        return 1.0; // Far plane
    }
    
    // Calculate depth based on distance from center
    vec2 center = vec2(0.0);
    float distance = length(position - center);
    
    // Normalize to [0, 1] range
    float maxDistance = sqrt(2.0); // Maximum distance in NDC
    return clamp(distance / maxDistance, 0.0, 1.0);
}

/**
 * Converts position coordinates between different spaces
 * @param pos Input position
 * @param fromNDC True if converting from NDC to screen, false for screen to NDC
 * @return Converted position
 */
vec2 convertCoordinates(vec2 pos, bool fromNDC) {
    if (fromNDC) {
        // NDC [-1, 1] to screen [0, 1]
        return (pos + 1.0) * 0.5;
    } else {
        // Screen [0, 1] to NDC [-1, 1]
        return pos * 2.0 - 1.0;
    }
}

// ============================================================================
// MAIN FUNCTION
// ============================================================================

void main() {
    // ========================================================================
    // INPUT VALIDATION
    // ========================================================================
    
    // Validate input attributes
    vec2 safePosition = validateVec2(aPosition, 
                                   vec2(VALID_RANGE_POS.x), 
                                   vec2(VALID_RANGE_POS.y), 
                                   vec2(0.0));
    
    vec2 safeTexCoord = validateVec2(aTexCoord, 
                                   vec2(VALID_RANGE_TEX.x), 
                                   vec2(VALID_RANGE_TEX.y), 
                                   vec2(0.5));
    
    // ========================================================================
    // COORDINATE CALCULATIONS
    // ========================================================================
    
    // Primary texture coordinates
    TexCoord = safeTexCoord;
    
    // Screen position [0, 1]
    ScreenPos = convertCoordinates(safePosition, true);
    
    // Normalized device coordinates [-1, 1]
    NDCPos = safePosition;
    
    // ========================================================================
    // ADVANCED CALCULATIONS
    // ========================================================================
    
    // Calculate view ray for screen space effects
    ViewRay = calculateViewRay(ScreenPos);
    
    // Calculate fragment depth
    FragDepth = calculateFragmentDepth(safePosition);
    
    // ========================================================================
    // FINAL POSITION TRANSFORMATION
    // ========================================================================
    
    // Set final clip space position
    gl_Position = vec4(safePosition, 0.0, 1.0);
    
    // ========================================================================
    // VALIDATION AND SAFETY CHECKS
    // ========================================================================
    
    // Ensure output position is valid
    if (u_enableValidation) {
        // Check for degenerate positions
        if (any(isnan(gl_Position.xy)) || any(isinf(gl_Position.xy))) {
            gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
        }
        
        // Clamp to valid range
        gl_Position.xy = clamp(gl_Position.xy, -1.0, 1.0);
    }
}