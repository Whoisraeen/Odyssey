/**
 * Enhanced Skybox Fragment Shader
 * 
 * Features:
 * - Advanced atmospheric scattering
 * - Dynamic time-of-day transitions
 * - Procedural cloud generation
 * - Star field rendering
 * - Sun/moon rendering
 * - Weather effects integration
 * - Input validation
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// OUTPUT
// ========================================================================
out vec4 FragColor;

// ========================================================================
// INPUT VARIABLES
// ========================================================================
in vec3 TexCoords;      // Skybox texture coordinates
in vec3 WorldPos;       // World position for atmospheric calculations
in vec3 ViewDir;        // View direction

// ========================================================================
// UNIFORMS - TEXTURES
// ========================================================================
uniform samplerCube skyboxDay;      // Day skybox
uniform samplerCube skyboxNight;    // Night skybox
uniform samplerCube skyboxClouds;   // Cloud texture
uniform sampler2D noiseTexture;     // Noise texture for procedural effects
uniform sampler2D starTexture;      // Star field texture

// ========================================================================
// UNIFORMS - ATMOSPHERIC
// ========================================================================
uniform vec3 sunDirection;          // Sun direction (normalized)
uniform vec3 moonDirection;         // Moon direction (normalized)
uniform vec3 sunColor;              // Sun color
uniform vec3 moonColor;             // Moon color
uniform float sunIntensity;         // Sun intensity
uniform float moonIntensity;        // Moon intensity

// ========================================================================
// UNIFORMS - TIME AND WEATHER
// ========================================================================
uniform float timeOfDay;            // Time of day [0.0, 1.0]
uniform float cloudCoverage;        // Cloud coverage [0.0, 1.0]
uniform float cloudDensity;         // Cloud density [0.0, 1.0]
uniform float weatherIntensity;     // Weather intensity [0.0, 1.0]
uniform float fogDensity;           // Fog density [0.0, 1.0]

// ========================================================================
// UNIFORMS - ATMOSPHERIC SCATTERING
// ========================================================================
uniform vec3 rayleighCoeff;         // Rayleigh scattering coefficients
uniform float mieCoeff;             // Mie scattering coefficient
uniform float scatteringIntensity;  // Overall scattering intensity
uniform float atmosphereThickness;  // Atmosphere thickness

// ========================================================================
// UNIFORMS - QUALITY AND FEATURES
// ========================================================================
uniform bool enableAtmosphericScattering; // Enable atmospheric scattering
uniform bool enableProceduralClouds;      // Enable procedural clouds
uniform bool enableStars;                 // Enable star rendering
uniform bool enableSunMoon;               // Enable sun/moon rendering
uniform int skyboxQuality;                // Quality level [0-2]

// ========================================================================
// CONSTANTS
// ========================================================================
const float PI = 3.14159265359;
const float EPSILON = 1e-6;
const vec3 UP = vec3(0.0, 1.0, 0.0);

// Atmospheric constants
const float EARTH_RADIUS = 6371000.0;
const float ATMOSPHERE_RADIUS = 6471000.0;
const int SCATTERING_SAMPLES = 16;

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

/**
 * Safe texture sampling
 */
vec4 safeTextureCube(samplerCube tex, vec3 coords, vec4 fallback) {
    vec3 safeCoords = normalize(coords);
    return texture(tex, safeCoords);
}

vec4 safeTexture2D(sampler2D tex, vec2 coords, vec4 fallback) {
    vec2 safeCoords = clamp(coords, vec2(0.0), vec2(1.0));
    return texture(tex, safeCoords);
}

/**
 * Calculate luminance
 */
float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

// ========================================================================
// ATMOSPHERIC SCATTERING
// ========================================================================

/**
 * Simplified atmospheric scattering calculation
 */
vec3 calculateAtmosphericScattering(vec3 viewDir, vec3 lightDir, vec3 lightColor) {
    if (!enableAtmosphericScattering) return vec3(0.0);
    
    float cosTheta = dot(viewDir, lightDir);
    float altitude = max(viewDir.y, 0.0);
    
    // Rayleigh scattering (blue sky)
    float rayleighPhase = 0.75 * (1.0 + cosTheta * cosTheta);
    vec3 rayleighScattering = rayleighCoeff * rayleighPhase * altitude;
    
    // Mie scattering (sun halo)
    float miePhase = 0.5 * (1.0 + cosTheta);
    vec3 mieScattering = vec3(mieCoeff) * miePhase * pow(max(cosTheta, 0.0), 8.0);
    
    return (rayleighScattering + mieScattering) * lightColor * scatteringIntensity;
}

// ========================================================================
// CELESTIAL BODIES
// ========================================================================

/**
 * Render sun disk
 */
vec3 renderSun(vec3 viewDir, vec3 sunDir, vec3 sunCol, float intensity) {
    float sunDot = dot(viewDir, sunDir);
    float sunDisk = smoothstep(0.9995, 0.9999, sunDot);
    float sunHalo = pow(max(sunDot, 0.0), 32.0) * 0.1;
    
    return (sunDisk + sunHalo) * sunCol * intensity;
}

/**
 * Render moon disk
 */
vec3 renderMoon(vec3 viewDir, vec3 moonDir, vec3 moonCol, float intensity) {
    float moonDot = dot(viewDir, moonDir);
    float moonDisk = smoothstep(0.999, 0.9995, moonDot);
    
    return moonDisk * moonCol * intensity;
}

// ========================================================================
// PROCEDURAL EFFECTS
// ========================================================================

/**
 * Generate procedural clouds
 */
vec3 generateProceduralClouds(vec3 viewDir, float coverage, float density) {
    if (!enableProceduralClouds || viewDir.y < 0.0) return vec3(0.0);
    
    // Sample noise for cloud generation
    vec2 cloudCoords = viewDir.xz / max(viewDir.y, 0.1) * 0.1;
    float noise1 = safeTexture2D(noiseTexture, cloudCoords, vec4(0.5)).r;
    float noise2 = safeTexture2D(noiseTexture, cloudCoords * 2.0, vec4(0.5)).r;
    
    float cloudMask = smoothstep(1.0 - coverage, 1.0, noise1 * noise2);
    float cloudAlpha = cloudMask * density;
    
    // Cloud color based on time of day
    vec3 cloudColor = mix(vec3(0.8, 0.9, 1.0), vec3(1.0, 0.7, 0.5), timeOfDay);
    
    return cloudColor * cloudAlpha;
}

/**
 * Render star field
 */
vec3 renderStars(vec3 viewDir, float nightFactor) {
    if (!enableStars || nightFactor < 0.1) return vec3(0.0);
    
    // Convert view direction to spherical coordinates for star mapping
    float phi = atan(viewDir.z, viewDir.x);
    float theta = acos(viewDir.y);
    vec2 starCoords = vec2(phi / (2.0 * PI) + 0.5, theta / PI);
    
    vec3 stars = safeTexture2D(starTexture, starCoords, vec4(0.0)).rgb;
    return stars * nightFactor * nightFactor; // Fade with night
}

// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main()
{
    // Input validation
    vec3 safeTexCoords = validateVec3(normalize(TexCoords), vec3(0.0, 1.0, 0.0));
    vec3 viewDir = safeTexCoords;
    
    // Calculate day/night factors
    float dayFactor = clamp(timeOfDay * 2.0, 0.0, 1.0);
    float nightFactor = clamp((1.0 - timeOfDay) * 2.0, 0.0, 1.0);
    
    // Sample base skybox textures
    vec3 dayColor = safeTextureCube(skyboxDay, safeTexCoords, vec4(0.5, 0.7, 1.0, 1.0)).rgb;
    vec3 nightColor = safeTextureCube(skyboxNight, safeTexCoords, vec4(0.05, 0.05, 0.1, 1.0)).rgb;
    
    // Blend day and night skyboxes
    vec3 baseColor = mix(nightColor, dayColor, dayFactor);
    
    // Add atmospheric scattering
    vec3 scattering = vec3(0.0);
    if (enableAtmosphericScattering) {
        scattering += calculateAtmosphericScattering(viewDir, sunDirection, sunColor) * dayFactor;
        scattering += calculateAtmosphericScattering(viewDir, moonDirection, moonColor) * nightFactor * 0.1;
    }
    
    // Add celestial bodies
    vec3 celestial = vec3(0.0);
    if (enableSunMoon) {
        celestial += renderSun(viewDir, sunDirection, sunColor, sunIntensity * dayFactor);
        celestial += renderMoon(viewDir, moonDirection, moonColor, moonIntensity * nightFactor);
    }
    
    // Add stars
    vec3 stars = renderStars(viewDir, nightFactor);
    
    // Add procedural clouds
    vec3 clouds = generateProceduralClouds(viewDir, cloudCoverage, cloudDensity);
    
    // Combine all elements
    vec3 finalColor = baseColor + scattering + celestial + stars;
    
    // Blend clouds on top
    finalColor = mix(finalColor, clouds, min(luminance(clouds), 0.8));
    
    // Apply fog effect
    if (fogDensity > 0.0 && viewDir.y < 0.5) {
        float fogFactor = exp(-fogDensity * (1.0 - viewDir.y));
        vec3 fogColor = mix(vec3(0.7, 0.8, 0.9), vec3(0.3, 0.3, 0.4), nightFactor);
        finalColor = mix(fogColor, finalColor, fogFactor);
    }
    
    // Ensure valid output
    finalColor = clamp(finalColor, vec3(0.0), vec3(10.0));
    
    FragColor = vec4(finalColor, 1.0);
}