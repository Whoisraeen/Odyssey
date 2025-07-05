#version 450 core

/**
 * Enhanced Particle Vertex Shader
 * 
 * Features:
 * - Advanced particle positioning and billboarding
 * - Dynamic size calculation based on distance and properties
 * - Particle animation and lifetime management
 * - Environmental effects (wind, gravity)
 * - Multiple output variables for fragment shader
 * - Performance optimizations and validation
 * - Support for various particle rendering modes
 * 
 * Author: Enhanced Shader System
 * Version: 2.0
 * Last Modified: 2024
 */

// ============================================================================
// INPUT ATTRIBUTES
// ============================================================================

layout (location = 0) in vec3 aPos;              // Particle world position
layout (location = 1) in vec3 aColor;            // Particle base color
layout (location = 2) in vec2 aTexCoord;         // Texture coordinates
layout (location = 3) in float aSize;            // Base particle size
layout (location = 4) in float aLife;            // Normalized lifetime [0, 1]
layout (location = 5) in vec3 aVelocity;         // Particle velocity
layout (location = 6) in float aType;            // Particle type identifier
layout (location = 7) in float aRotation;        // Particle rotation
layout (location = 8) in vec2 aAtlasCoords;      // Texture atlas coordinates

// ============================================================================
// OUTPUT VARIABLES
// ============================================================================

out vec3 particleColor;          // Particle color
out vec2 texCoords;              // Texture coordinates
out float particleAlpha;         // Base alpha value
out float particleLife;          // Normalized lifetime
out float particleSize;          // Final particle size
out vec3 worldPos;               // World position
out vec3 velocity;               // Particle velocity
out float particleType;          // Particle type
out vec2 atlasCoords;            // Atlas coordinates
out float distanceToCamera;      // Distance to camera

// ============================================================================
// UNIFORMS
// ============================================================================

// Transformation matrices
uniform mat4 u_view;                     // View matrix
uniform mat4 u_projection;               // Projection matrix
uniform mat4 u_model;                    // Model matrix
uniform mat3 u_normalMatrix;             // Normal matrix

// Camera properties
uniform vec3 u_cameraPos;                // Camera world position
uniform vec3 u_cameraRight;              // Camera right vector
uniform vec3 u_cameraUp;                 // Camera up vector
uniform vec3 u_cameraForward;            // Camera forward vector

// Particle system properties
uniform float u_time;                    // Global time
uniform float u_deltaTime;               // Frame delta time
uniform vec3 u_gravity;                  // Gravity vector
uniform vec3 u_windDirection;            // Wind direction
uniform float u_windStrength;            // Wind strength
uniform float u_windFrequency;           // Wind frequency

// Size and scaling
uniform float u_globalScale;             // Global particle scale
uniform float u_minSize;                 // Minimum particle size
uniform float u_maxSize;                 // Maximum particle size
uniform float u_sizeAttenuation;         // Distance-based size attenuation
uniform bool u_enableSizeAttenuation;    // Enable size attenuation

// Rendering modes
uniform int u_renderMode;                // Rendering mode
uniform bool u_enableBillboarding;       // Enable billboarding
uniform bool u_enableRotation;           // Enable particle rotation
uniform bool u_enableAnimation;          // Enable particle animation

// Environmental effects
uniform float u_turbulence;              // Turbulence strength
uniform float u_noiseScale;              // Noise scale for effects
uniform bool u_enableWind;               // Enable wind effects
uniform bool u_enableGravity;            // Enable gravity effects

// Quality and performance
uniform int u_particleQuality;           // Quality level [0-3]
uniform bool u_enableValidation;         // Enable input validation
uniform float u_lodDistance;             // LOD transition distance

// ============================================================================
// CONSTANTS
// ============================================================================

const float EPSILON = 1e-6;
const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;

// Render modes
const int RENDER_POINTS = 0;
const int RENDER_QUADS = 1;
const int RENDER_BILLBOARDS = 2;
const int RENDER_ORIENTED = 3;

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
 * Validates a vec3 value
 */
vec3 validateVec3(vec3 value, vec3 fallback) {
    if (u_enableValidation) {
        bool isValid = !any(isnan(value)) && !any(isinf(value));
        return isValid ? value : fallback;
    }
    return value;
}

/**
 * Generates noise for particle effects
 */
float noise(vec3 pos) {
    // Simple noise function for particle effects
    return fract(sin(dot(pos, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
}

/**
 * Calculates wind displacement
 */
vec3 calculateWindDisplacement(vec3 position, float time, float particleType) {
    if (!u_enableWind || u_windStrength < EPSILON) {
        return vec3(0.0);
    }
    
    // Base wind effect
    vec3 windEffect = u_windDirection * u_windStrength;
    
    // Add turbulence based on particle type
    float turbulenceScale = 1.0;
    if (particleType == PARTICLE_SMOKE) {
        turbulenceScale = 2.0; // Smoke is more affected by wind
    } else if (particleType == PARTICLE_DUST) {
        turbulenceScale = 1.5;
    } else if (particleType == PARTICLE_SPARK) {
        turbulenceScale = 0.3; // Sparks are less affected
    }
    
    // Calculate turbulence
    vec3 noisePos = position * u_noiseScale + time * u_windFrequency;
    vec3 turbulence = vec3(
        noise(noisePos),
        noise(noisePos + vec3(100.0)),
        noise(noisePos + vec3(200.0))
    ) * 2.0 - 1.0;
    
    windEffect += turbulence * u_turbulence * turbulenceScale;
    
    return windEffect;
}

/**
 * Calculates gravity displacement
 */
vec3 calculateGravityDisplacement(float particleType, float life) {
    if (!u_enableGravity) {
        return vec3(0.0);
    }
    
    // Different particle types have different gravity responses
    float gravityScale = 1.0;
    if (particleType == PARTICLE_SMOKE) {
        gravityScale = -0.5; // Smoke rises
    } else if (particleType == PARTICLE_FIRE) {
        gravityScale = -0.3; // Fire rises
    } else if (particleType == PARTICLE_DUST) {
        gravityScale = 0.8; // Dust falls slowly
    } else if (particleType == PARTICLE_WATER) {
        gravityScale = 1.5; // Water falls faster
    }
    
    // Apply gravity over lifetime
    float timeEffect = life * life; // Quadratic falloff
    return u_gravity * gravityScale * timeEffect;
}

/**
 * Calculates distance-based size attenuation
 */
float calculateSizeAttenuation(float distance, float baseSize) {
    if (!u_enableSizeAttenuation) {
        return baseSize;
    }
    
    // Calculate attenuation factor
    float attenuation = 1.0 / (1.0 + u_sizeAttenuation * distance * distance);
    
    // Apply size limits
    float finalSize = baseSize * attenuation;
    return clamp(finalSize, u_minSize, u_maxSize);
}

/**
 * Calculates particle alpha based on lifetime and distance
 */
float calculateParticleAlpha(float life, float distance, float particleType) {
    // Base alpha curve
    float alpha = 1.0;
    
    // Fade in and out over lifetime
    float fadeIn = smoothstep(0.0, 0.1, life);
    float fadeOut = 1.0 - smoothstep(0.8, 1.0, life);
    alpha *= fadeIn * fadeOut;
    
    // Distance-based fading
    if (distance > u_lodDistance) {
        float distanceFade = 1.0 - smoothstep(u_lodDistance, u_lodDistance * 2.0, distance);
        alpha *= distanceFade;
    }
    
    // Type-specific alpha modifications
    if (particleType == PARTICLE_SMOKE) {
        alpha *= 0.7; // Smoke is more transparent
    } else if (particleType == PARTICLE_FIRE) {
        alpha *= 0.9; // Fire is more opaque
    }
    
    return clamp(alpha, 0.0, 1.0);
}

/**
 * Creates billboard matrix for camera-facing particles
 */
mat3 createBillboardMatrix() {
    if (!u_enableBillboarding) {
        return mat3(1.0); // Identity matrix
    }
    
    vec3 right = normalize(u_cameraRight);
    vec3 up = normalize(u_cameraUp);
    vec3 forward = normalize(u_cameraForward);
    
    return mat3(right, up, forward);
}

/**
 * Applies particle rotation
 */
mat2 createRotationMatrix(float rotation) {
    if (!u_enableRotation) {
        return mat2(1.0, 0.0, 0.0, 1.0); // Identity matrix
    }
    
    float c = cos(rotation);
    float s = sin(rotation);
    return mat2(c, -s, s, c);
}

void main() {
    // ========================================================================
    // INPUT VALIDATION
    // ========================================================================
    
    vec3 safePos = validateVec3(aPos, vec3(0.0));
    vec3 safeColor = clamp(aColor, 0.0, 1.0);
    vec2 safeTexCoord = clamp(aTexCoord, 0.0, 1.0);
    float safeSize = validateFloat(aSize, 0.1, 100.0, 1.0);
    float safeLife = validateFloat(aLife, 0.0, 1.0, 0.5);
    vec3 safeVelocity = validateVec3(aVelocity, vec3(0.0));
    float safeType = validateFloat(aType, 0.0, 10.0, 0.0);
    float safeRotation = validateFloat(aRotation, -TWO_PI, TWO_PI, 0.0);
    
    // ========================================================================
    // ENVIRONMENTAL EFFECTS
    // ========================================================================
    
    // Calculate wind displacement
    vec3 windDisplacement = calculateWindDisplacement(safePos, u_time, safeType);
    
    // Calculate gravity displacement
    vec3 gravityDisplacement = calculateGravityDisplacement(safeType, safeLife);
    
    // Apply environmental effects to position
    vec3 finalPosition = safePos + windDisplacement + gravityDisplacement;
    
    // Add velocity-based animation if enabled
    if (u_enableAnimation) {
        finalPosition += safeVelocity * u_time;
    }
    
    // ========================================================================
    // DISTANCE AND SIZE CALCULATIONS
    // ========================================================================
    
    // Calculate distance to camera
    float cameraDistance = length(finalPosition - u_cameraPos);
    
    // Calculate final particle size
    float baseSize = safeSize * u_globalScale;
    float finalSize = calculateSizeAttenuation(cameraDistance, baseSize);
    
    // ========================================================================
    // TRANSFORMATION AND POSITIONING
    // ========================================================================
    
    // Transform to view space
    vec4 viewPos = u_view * vec4(finalPosition, 1.0);
    
    // Apply billboarding if enabled
    if (u_renderMode == RENDER_BILLBOARDS && u_enableBillboarding) {
        // Billboard positioning
        mat3 billboardMatrix = createBillboardMatrix();
        
        // Apply rotation if enabled
        vec2 offset = safeTexCoord * 2.0 - 1.0; // Convert to [-1, 1]
        if (u_enableRotation) {
            mat2 rotMatrix = createRotationMatrix(safeRotation + u_time * safeType);
            offset = rotMatrix * offset;
        }
        
        // Scale by particle size
        offset *= finalSize;
        
        // Apply billboard transformation
        vec3 billboardOffset = billboardMatrix * vec3(offset, 0.0);
        viewPos.xyz += billboardOffset;
    }
    
    // ========================================================================
    // FINAL TRANSFORMATION
    // ========================================================================
    
    // Project to clip space
    gl_Position = u_projection * viewPos;
    
    // Set point size for point rendering
    if (u_renderMode == RENDER_POINTS) {
        gl_PointSize = finalSize;
    }
    
    // ========================================================================
    // OUTPUT VARIABLES
    // ========================================================================
    
    // Pass through basic properties
    particleColor = safeColor;
    texCoords = safeTexCoord;
    particleLife = safeLife;
    particleSize = finalSize;
    worldPos = finalPosition;
    velocity = safeVelocity;
    particleType = safeType;
    atlasCoords = aAtlasCoords;
    distanceToCamera = cameraDistance;
    
    // Calculate final alpha
    particleAlpha = calculateParticleAlpha(safeLife, cameraDistance, safeType);
    
    // ========================================================================
    // VALIDATION AND SAFETY CHECKS
    // ========================================================================
    
    // Ensure output position is valid
    if (u_enableValidation) {
        // Check for degenerate positions
        if (any(isnan(gl_Position.xyzw)) || any(isinf(gl_Position.xyzw))) {
            gl_Position = vec4(0.0, 0.0, -1.0, 1.0); // Move to far plane
        }
        
        // Clamp point size
        if (u_renderMode == RENDER_POINTS) {
            gl_PointSize = clamp(gl_PointSize, 1.0, 64.0);
        }
    }
}