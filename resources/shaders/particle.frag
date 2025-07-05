#version 450 core

/**
 * Enhanced Particle Fragment Shader
 * 
 * Features:
 * - Advanced particle rendering with multiple blending modes
 * - Texture atlas support for animated particles
 * - Procedural particle shapes and effects
 * - Distance-based fading and size attenuation
 * - Color gradients and animation over lifetime
 * - Soft particle rendering with depth testing
 * - Environmental interaction (wind, gravity effects)
 * - Performance optimizations and validation
 * 
 * Author: Enhanced Shader System
 * Version: 2.0
 * Last Modified: 2024
 */

// ============================================================================
// INPUT VARIABLES
// ============================================================================

in vec3 particleColor;           // Base particle color
in vec2 texCoords;               // Texture coordinates
in float particleAlpha;          // Base alpha value
in float particleLife;           // Normalized lifetime [0, 1]
in float particleSize;           // Particle size
in vec3 worldPos;                // World position
in vec3 velocity;                // Particle velocity
in float particleType;           // Particle type identifier
in vec2 atlasCoords;             // Texture atlas coordinates
in float distanceToCamera;       // Distance to camera

// ============================================================================
// OUTPUT VARIABLES
// ============================================================================

out vec4 FragColor;

// ============================================================================
// UNIFORMS
// ============================================================================

// Textures
uniform sampler2D u_particleTexture;     // Main particle texture
uniform sampler2D u_noiseTexture;        // Noise texture for effects
uniform sampler2D u_depthTexture;        // Scene depth for soft particles
uniform sampler2D u_atlasTexture;        // Texture atlas for animated particles

// Camera and rendering
uniform vec3 u_cameraPos;                // Camera world position
uniform mat4 u_viewMatrix;               // View matrix
uniform mat4 u_projMatrix;               // Projection matrix
uniform vec2 u_screenSize;               // Screen dimensions
uniform float u_nearPlane;               // Near clipping plane
uniform float u_farPlane;                // Far clipping plane

// Particle system properties
uniform float u_time;                    // Global time
uniform float u_deltaTime;               // Frame delta time
uniform vec3 u_gravity;                  // Gravity vector
uniform vec3 u_windDirection;            // Wind direction
uniform float u_windStrength;            // Wind strength

// Rendering properties
uniform float u_softParticleFactor;      // Soft particle blending factor
uniform float u_fadeDistance;            // Distance fade range
uniform float u_maxAlpha;                // Maximum alpha value
uniform int u_blendMode;                 // Blending mode
uniform bool u_useTexture;               // Enable texture sampling
uniform bool u_useAtlas;                 // Enable atlas animation
uniform bool u_enableSoftParticles;      // Enable soft particle rendering
uniform bool u_enableDistanceFade;       // Enable distance-based fading

// Color and animation
uniform vec3 u_colorStart;               // Start color
uniform vec3 u_colorEnd;                 // End color
uniform float u_alphaStart;              // Start alpha
uniform float u_alphaEnd;                // End alpha
uniform float u_emissionStrength;        // Emission intensity
uniform vec2 u_atlasSize;                // Atlas dimensions (frames)
uniform float u_animationSpeed;          // Animation speed

// Quality and performance
uniform int u_particleQuality;           // Quality level [0-3]
uniform bool u_enableValidation;         // Enable input validation

// ============================================================================
// CONSTANTS
// ============================================================================

const float EPSILON = 1e-6;
const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

// Blending modes
const int BLEND_ALPHA = 0;
const int BLEND_ADDITIVE = 1;
const int BLEND_MULTIPLY = 2;
const int BLEND_SCREEN = 3;
const int BLEND_OVERLAY = 4;

// Particle types
const float PARTICLE_SMOKE = 0.0;
const float PARTICLE_FIRE = 1.0;
const float PARTICLE_SPARK = 2.0;
const float PARTICLE_DUST = 3.0;
const float PARTICLE_MAGIC = 4.0;
const float PARTICLE_WATER = 5.0;

// Quality levels
const int QUALITY_LOW = 0;
const int QUALITY_MEDIUM = 1;
const int QUALITY_HIGH = 2;
const int QUALITY_ULTRA = 3;

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Validates a float value within specified range
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
 * Validates a vec3 color value
 */
vec3 validateColor(vec3 color) {
    if (u_enableValidation) {
        bool isValid = all(greaterThanEqual(color, vec3(0.0))) && 
                      all(lessThanEqual(color, vec3(1.0))) &&
                      !any(isnan(color)) && !any(isinf(color));
        return isValid ? color : vec3(0.5);
    }
    return color;
}

/**
 * Safe texture sampling with fallback
 */
vec4 safeTexture2D(sampler2D tex, vec2 coords, vec4 fallback) {
    if (any(isnan(coords)) || any(isinf(coords))) {
        return fallback;
    }
    
    vec2 safeCoords = clamp(coords, 0.0, 1.0);
    return texture(tex, safeCoords);
}

/**
 * Generates smooth noise for particle effects
 */
float smoothNoise(vec2 coord) {
    return safeTexture2D(u_noiseTexture, coord, vec4(0.5)).r;
}

/**
 * Calculates distance-based fade factor
 */
float calculateDistanceFade(float distance) {
    if (!u_enableDistanceFade) {
        return 1.0;
    }
    
    float fadeStart = u_fadeDistance * 0.7;
    float fadeEnd = u_fadeDistance;
    
    return 1.0 - smoothstep(fadeStart, fadeEnd, distance);
}

/**
 * Calculates soft particle factor using depth buffer
 */
float calculateSoftParticleFactor(vec2 screenCoords) {
    if (!u_enableSoftParticles) {
        return 1.0;
    }
    
    // Sample scene depth
    float sceneDepth = safeTexture2D(u_depthTexture, screenCoords, vec4(1.0)).r;
    
    // Convert to linear depth
    float linearSceneDepth = (2.0 * u_nearPlane) / 
                            (u_farPlane + u_nearPlane - sceneDepth * (u_farPlane - u_nearPlane));
    
    // Calculate particle depth
    float particleDepth = distanceToCamera;
    
    // Calculate soft factor
    float depthDiff = abs(linearSceneDepth - particleDepth);
    return clamp(depthDiff / u_softParticleFactor, 0.0, 1.0);
}

/**
 * Interpolates color over particle lifetime
 */
vec3 calculateLifetimeColor(float life) {
    vec3 startColor = validateColor(u_colorStart);
    vec3 endColor = validateColor(u_colorEnd);
    
    // Smooth interpolation with easing
    float t = smoothstep(0.0, 1.0, life);
    return mix(startColor, endColor, t);
}

/**
 * Calculates alpha over particle lifetime
 */
float calculateLifetimeAlpha(float life) {
    float startAlpha = validateFloat(u_alphaStart, 0.0, 1.0, 1.0);
    float endAlpha = validateFloat(u_alphaEnd, 0.0, 1.0, 0.0);
    
    // Fade in and out curve
    float fadeIn = smoothstep(0.0, 0.2, life);
    float fadeOut = 1.0 - smoothstep(0.8, 1.0, life);
    
    float baseAlpha = mix(startAlpha, endAlpha, life);
    return baseAlpha * fadeIn * fadeOut;
}

/**
 * Generates procedural particle shape
 */
float generateParticleShape(vec2 coords, float particleType) {
    vec2 center = vec2(0.5);
    vec2 offset = coords - center;
    float distance = length(offset);
    
    if (particleType == PARTICLE_SMOKE) {
        // Soft circular shape with noise
        float noise = smoothNoise(coords * 4.0 + u_time * 0.5) * 0.3;
        return 1.0 - smoothstep(0.3 + noise, 0.5 + noise, distance);
    } else if (particleType == PARTICLE_FIRE) {
        // Flickering flame shape
        float flicker = sin(u_time * 10.0 + coords.x * 20.0) * 0.1;
        float shape = 1.0 - smoothstep(0.2, 0.4 + flicker, distance);
        return shape * (1.0 + sin(u_time * 15.0) * 0.2);
    } else if (particleType == PARTICLE_SPARK) {
        // Sharp star shape
        float angle = atan(offset.y, offset.x);
        float star = abs(sin(angle * 4.0)) * 0.3 + 0.7;
        return 1.0 - smoothstep(0.1 * star, 0.3 * star, distance);
    } else if (particleType == PARTICLE_MAGIC) {
        // Swirling magical effect
        float angle = atan(offset.y, offset.x) + u_time * 2.0;
        float spiral = sin(angle * 3.0 + distance * 10.0) * 0.5 + 0.5;
        return (1.0 - smoothstep(0.2, 0.4, distance)) * spiral;
    }
    
    // Default circular shape
    return 1.0 - smoothstep(0.3, 0.5, distance);
}

/**
 * Calculates texture atlas coordinates for animation
 */
vec2 calculateAtlasCoords(vec2 baseCoords) {
    if (!u_useAtlas) {
        return baseCoords;
    }
    
    // Calculate current frame
    float totalFrames = u_atlasSize.x * u_atlasSize.y;
    float currentFrame = mod(u_time * u_animationSpeed + particleLife * totalFrames, totalFrames);
    
    // Calculate frame coordinates
    float frameX = mod(currentFrame, u_atlasSize.x);
    float frameY = floor(currentFrame / u_atlasSize.x);
    
    // Calculate UV offset
    vec2 frameSize = 1.0 / u_atlasSize;
    vec2 frameOffset = vec2(frameX, frameY) * frameSize;
    
    return frameOffset + baseCoords * frameSize;
}

/**
 * Applies blending mode to color
 */
vec3 applyBlendMode(vec3 baseColor, vec3 blendColor, int mode) {
    switch (mode) {
        case BLEND_ADDITIVE:
            return baseColor + blendColor;
        case BLEND_MULTIPLY:
            return baseColor * blendColor;
        case BLEND_SCREEN:
            return 1.0 - (1.0 - baseColor) * (1.0 - blendColor);
        case BLEND_OVERLAY:
            return mix(2.0 * baseColor * blendColor, 
                      1.0 - 2.0 * (1.0 - baseColor) * (1.0 - blendColor),
                      step(0.5, baseColor));
        default: // BLEND_ALPHA
            return blendColor;
    }
}

void main() {
    // ========================================================================
    // INPUT VALIDATION
    // ========================================================================
    
    vec3 safeParticleColor = validateColor(particleColor);
    float safeParticleAlpha = validateFloat(particleAlpha, 0.0, 1.0, 0.7);
    float safeParticleLife = validateFloat(particleLife, 0.0, 1.0, 0.5);
    float safeParticleType = validateFloat(particleType, 0.0, 10.0, 0.0);
    vec2 safeTexCoords = clamp(texCoords, 0.0, 1.0);
    
    // ========================================================================
    // TEXTURE SAMPLING
    // ========================================================================
    
    vec4 textureColor = vec4(1.0);
    
    if (u_useTexture) {
        vec2 finalTexCoords = safeTexCoords;
        
        // Use atlas coordinates if enabled
        if (u_useAtlas) {
            finalTexCoords = calculateAtlasCoords(safeTexCoords);
        }
        
        textureColor = safeTexture2D(u_particleTexture, finalTexCoords, vec4(1.0));
    } else {
        // Generate procedural shape
        float shape = generateParticleShape(safeTexCoords, safeParticleType);
        textureColor = vec4(1.0, 1.0, 1.0, shape);
    }
    
    // ========================================================================
    // COLOR CALCULATION
    // ========================================================================
    
    // Calculate lifetime-based color
    vec3 lifetimeColor = calculateLifetimeColor(safeParticleLife);
    
    // Combine base color with lifetime color
    vec3 finalColor = applyBlendMode(safeParticleColor, lifetimeColor, u_blendMode);
    
    // Apply texture color
    finalColor *= textureColor.rgb;
    
    // Add emission for glowing effects
    if (safeParticleType == PARTICLE_FIRE || safeParticleType == PARTICLE_MAGIC) {
        finalColor += finalColor * u_emissionStrength;
    }
    
    // ========================================================================
    // ALPHA CALCULATION
    // ========================================================================
    
    // Calculate base alpha
    float finalAlpha = safeParticleAlpha;
    
    // Apply texture alpha
    finalAlpha *= textureColor.a;
    
    // Apply lifetime alpha
    finalAlpha *= calculateLifetimeAlpha(safeParticleLife);
    
    // Apply distance fade
    finalAlpha *= calculateDistanceFade(distanceToCamera);
    
    // Apply soft particle factor
    vec2 screenCoords = gl_FragCoord.xy / u_screenSize;
    finalAlpha *= calculateSoftParticleFactor(screenCoords);
    
    // Clamp to maximum alpha
    finalAlpha = min(finalAlpha, u_maxAlpha);
    
    // ========================================================================
    // FINAL OUTPUT
    // ========================================================================
    
    // Validate final color
    finalColor = validateColor(finalColor);
    finalAlpha = validateFloat(finalAlpha, 0.0, 1.0, 0.0);
    
    // Early discard for transparent particles
    if (finalAlpha < 0.01) {
        discard;
    }
    
    FragColor = vec4(finalColor, finalAlpha);
}