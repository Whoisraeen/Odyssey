#version 330 core

/**
 * Enhanced Deferred Lighting Fragment Shader
 * 
 * Features:
 * - Advanced PBR lighting model
 * - Multiple light types with shadows
 * - Enhanced environmental effects
 * - Robust error handling and validation
 * - Optimized performance with quality levels
 * - Advanced post-processing pipeline
 * 
 * Author: Odyssey Engine
 * Version: 2.0
 * Last Modified: 2024
 */

out vec4 FragColor;

in vec2 TexCoords;

// ========================================================================
// G-BUFFER TEXTURES
// ========================================================================
uniform sampler2D gPosition;    // RGB: World Position, A: Linear Depth
uniform sampler2D gNormal;      // RGB: World Normal, A: Normal Strength
uniform sampler2D gAlbedoSpec;  // RGB: Albedo, A: Specular
uniform sampler2D gMaterial;    // R: Metallic, G: Roughness, B: AO, A: Emissive

// ========================================================================
// SHADOW AND AMBIENT OCCLUSION
// ========================================================================
uniform sampler2D shadowMap;
uniform sampler2D ssaoTexture;
uniform sampler2DArray cascadedShadowMaps; // For cascaded shadow mapping
uniform mat4 lightSpaceMatrix;
uniform mat4 cascadeMatrices[4];           // Up to 4 cascade levels
uniform float cascadeDistances[4];
uniform int numCascades;
uniform bool enableSSAO;
uniform bool enableShadows;
uniform bool enableCascadedShadows;

// ========================================================================
// LIGHTING STRUCTURES
// ========================================================================

// Enhanced directional light (sun/moon)
struct DirLight {
    vec3 direction;
    vec3 color;
    float intensity;
    float shadowBias;
    bool castShadows;
};
uniform DirLight dirLight;

// Enhanced point lights
struct Light {
    vec3 position;
    vec3 direction;
    vec3 color;
    float intensity;
    float radius;
    float innerCone;  // For spot lights
    float outerCone;  // For spot lights
    int type; // 0 = directional, 1 = point, 2 = spot
    bool castShadows;
    int shadowMapIndex; // For shadow cube maps
};

#define MAX_LIGHTS 8
uniform Light lights[MAX_LIGHTS];
uniform int numLights;

// Enhanced spot lights
struct SpotLight {
    vec3 position;
    vec3 direction;
    vec3 color;
    float intensity;
    float cutOff;
    float outerCutOff;
    float linear;
    float quadratic;
    bool castShadows;
    mat4 lightSpaceMatrix;
};
const int MAX_SPOT_LIGHTS = 16;
uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
uniform int numSpotLights;

// ========================================================================
// CAMERA AND VIEW
// ========================================================================
uniform vec3 viewPos;
uniform mat4 view;
uniform mat4 projection;
uniform float nearPlane;
uniform float farPlane;

// ========================================================================
// ENVIRONMENT AND ATMOSPHERE
// ========================================================================
uniform float ambientStrength;
uniform vec3 ambientColor;
uniform vec3 skyColor;
uniform float atmosphereThickness;
uniform float scatteringStrength;

// ========================================================================
// FOG AND WEATHER
// ========================================================================
uniform bool enableFog;
uniform int fogType;            // 0: Linear, 1: Exponential, 2: Exponential Squared
uniform float fogDensity;
uniform float fogStart;
uniform float fogEnd;
uniform vec3 fogColor;
uniform float fogHeightFalloff; // For height-based fog

// Weather effects
uniform float weatherIntensity;  // 0.0 = clear, 1.0 = stormy
uniform float rainIntensity;     // 0.0 = no rain, 1.0 = heavy rain
uniform float snowIntensity;     // 0.0 = no snow, 1.0 = heavy snow
uniform float windStrength;      // Wind effect on lighting

// ========================================================================
// TIME AND SEASONAL
// ========================================================================
uniform float time;
uniform float timeOfDay;         // 0.0 = midnight, 0.5 = noon, 1.0 = midnight
uniform int season;              // 0: Spring, 1: Summer, 2: Autumn, 3: Winter
uniform float seasonTransition;  // Transition between seasons [0.0, 1.0]
uniform float temperature;       // Environmental temperature

// ========================================================================
// POST-PROCESSING
// ========================================================================
uniform float exposure;
uniform float gamma;
uniform bool enableToneMapping;
uniform int toneMappingType;     // 0: Reinhard, 1: Filmic, 2: ACES
uniform float contrast;
uniform float saturation;
uniform float brightness;

// ========================================================================
// QUALITY AND PERFORMANCE
// ========================================================================
uniform int lightingQuality;     // 0: Low, 1: Medium, 2: High, 3: Ultra
uniform bool enableAdvancedLighting;
uniform bool enableVolumetricLighting;
uniform bool enableScreenSpaceReflections;
uniform int maxLightSamples;     // Maximum lights to process per fragment

// ========================================================================
// CONSTANTS
// ========================================================================
const float PI = 3.14159265359;
const float TWO_PI = 6.28318530718;
const float HALF_PI = 1.57079632679;
const float INV_PI = 0.31830988618;
const float EPSILON = 1e-6;
const float SHADOW_BIAS_BASE = 0.001;
const float SHADOW_BIAS_MAX = 0.01;
const float AMBIENT_FACTOR = 0.1;
const float MAX_REFLECTION_LOD = 4.0;

// Quality levels
const int QUALITY_LOW = 0;
const int QUALITY_MEDIUM = 1;
const int QUALITY_HIGH = 2;
const int QUALITY_ULTRA = 3;

// Light types
const int LIGHT_DIRECTIONAL = 0;
const int LIGHT_POINT = 1;
const int LIGHT_SPOT = 2;

// Tone mapping types
const int TONEMAP_REINHARD = 0;
const int TONEMAP_FILMIC = 1;
const int TONEMAP_ACES = 2;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validates and clamps a float value within specified range
 */
float validateFloat(float value, float minVal, float maxVal, float defaultVal) {
    if (isnan(value) || isinf(value)) {
        return defaultVal;
    }
    return clamp(value, minVal, maxVal);
}

/**
 * Validates a 3D vector for NaN/Inf values
 */
vec3 validateVec3(vec3 v, vec3 defaultVal) {
    if (any(isnan(v)) || any(isinf(v))) {
        return defaultVal;
    }
    return v;
}

/**
 * Safe texture sampling with fallback
 */
vec4 safeTexture2D(sampler2D tex, vec2 coords, vec4 fallback) {
    vec2 safeCoords = clamp(coords, vec2(0.0), vec2(1.0));
    vec4 result = texture(tex, safeCoords);
    return validateVec3(result.rgb, fallback.rgb).rgbr + vec4(0, 0, 0, result.a - fallback.a);
}

/**
 * Luminance calculation
 */
float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

/**
 * Converts linear depth to logarithmic
 */
float linearToLogDepth(float linearDepth) {
    return log2(max(EPSILON, linearDepth)) / log2(farPlane);
}

// ========================================================================
// ENHANCED PBR FUNCTIONS
// ========================================================================

/**
 * Fresnel equation (Schlick approximation)
 */
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

/**
 * Enhanced Fresnel with roughness
 */
vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

/**
 * Normal Distribution Function (GGX/Trowbridge-Reitz)
 */
float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    
    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    
    return num / max(denom, EPSILON);
}

/**
 * Geometry function (Schlick-GGX approximation)
 */
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    
    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    
    return num / max(denom, EPSILON);
}

/**
 * Smith's method for geometry function
 */
float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);
    
    return ggx1 * ggx2;
}

/**
 * Calculate F0 for dielectrics and metals
 */
vec3 calculateF0(vec3 albedo, float metallic) {
    vec3 F0 = vec3(0.04); // Default dielectric F0
    return mix(F0, albedo, metallic);
}

// ========================================================================
// ENHANCED SHADOW MAPPING
// ========================================================================

/**
 * Calculate adaptive shadow bias based on surface angle and distance
 */
float calculateShadowBias(vec3 normal, vec3 lightDir, float distance) {
    float cosTheta = clamp(dot(normal, lightDir), 0.0, 1.0);
    float bias = SHADOW_BIAS_BASE * tan(acos(cosTheta));
    
    // Distance-based bias adjustment
    float distanceFactor = distance / 100.0; // Normalize to reasonable range
    bias += SHADOW_BIAS_BASE * distanceFactor;
    
    return clamp(bias, SHADOW_BIAS_BASE, SHADOW_BIAS_MAX);
}

/**
 * PCF (Percentage Closer Filtering) shadow sampling
 */
float calculateShadowPCF(vec3 worldPos, vec3 normal, vec3 lightDir, int samples) {
    if (!enableShadows) return 0.0;
    
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(worldPos, 1.0);
    
    // Perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    
    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;
    
    // Early exit if outside shadow map bounds
    if (projCoords.z > 1.0 || any(lessThan(projCoords.xy, vec2(0.0))) || any(greaterThan(projCoords.xy, vec2(1.0)))) {
        return 0.0;
    }
    
    // Get depth of current fragment from light's perspective
    float currentDepth = projCoords.z;
    
    // Calculate adaptive bias
    float distance = length(worldPos - viewPos);
    float bias = calculateShadowBias(normal, lightDir, distance);
    
    // PCF sampling
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    
    int halfSamples = samples / 2;
    for (int x = -halfSamples; x <= halfSamples; ++x) {
        for (int y = -halfSamples; y <= halfSamples; ++y) {
            vec2 offset = vec2(x, y) * texelSize;
            float pcfDepth = texture(shadowMap, projCoords.xy + offset).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    
    shadow /= float(samples * samples);
    return shadow;
}

/**
 * Standard shadow calculation (fallback)
 */
float calculateShadow(vec3 worldPos, vec3 normal, vec3 lightDir) {
    if (!enableShadows) return 0.0;
    
    // Use PCF for medium+ quality, simple sampling for low quality
    if (lightingQuality >= QUALITY_MEDIUM) {
        int samples = lightingQuality >= QUALITY_HIGH ? 5 : 3;
        return calculateShadowPCF(worldPos, normal, lightDir, samples);
    }
    
    // Simple shadow mapping for low quality
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(worldPos, 1.0);
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    
    if (projCoords.z > 1.0) return 0.0;
    
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    
    float distance = length(worldPos - viewPos);
    float bias = calculateShadowBias(normal, lightDir, distance);
    
    return currentDepth - bias > closestDepth ? 1.0 : 0.0;
}

/**
 * Cascaded shadow mapping
 */
float calculateCascadedShadow(vec3 worldPos, vec3 normal, vec3 lightDir, float viewDepth) {
    if (!enableCascadedShadows || !enableShadows) {
        return calculateShadow(worldPos, normal, lightDir);
    }
    
    // Determine which cascade to use
    int cascadeIndex = 0;
    for (int i = 0; i < numCascades - 1; ++i) {
        if (viewDepth < cascadeDistances[i]) {
            cascadeIndex = i;
            break;
        }
        cascadeIndex = numCascades - 1;
    }
    
    // Transform position to cascade light space
    vec4 fragPosLightSpace = cascadeMatrices[cascadeIndex] * vec4(worldPos, 1.0);
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    
    if (projCoords.z > 1.0) return 0.0;
    
    // Sample from cascade shadow map
    float closestDepth = texture(cascadedShadowMaps, vec3(projCoords.xy, float(cascadeIndex))).r;
    float currentDepth = projCoords.z;
    
    float distance = length(worldPos - viewPos);
    float bias = calculateShadowBias(normal, lightDir, distance);
    
    return currentDepth - bias > closestDepth ? 1.0 : 0.0;
}

// ========================================================================
// ENHANCED LIGHTING CALCULATIONS
// ========================================================================

/**
 * Enhanced spot light attenuation calculation
 */
float calculateSpotLight(vec3 lightDir, vec3 spotDir, float innerCone, float outerCone) {
    float theta = dot(lightDir, normalize(-spotDir));
    float epsilon = innerCone - outerCone;
    float intensity = clamp((theta - outerCone) / max(epsilon, EPSILON), 0.0, 1.0);
    
    // Smooth falloff using smoothstep
    return smoothstep(0.0, 1.0, intensity);
}

/**
 * Calculate light attenuation with distance
 */
float calculateAttenuation(float distance, float linear, float quadratic, float radius) {
    if (distance > radius) return 0.0;
    
    float attenuation = 1.0 / (1.0 + linear * distance + quadratic * distance * distance);
    
    // Smooth cutoff at radius
    float falloff = 1.0 - pow(distance / radius, 4.0);
    falloff = clamp(falloff, 0.0, 1.0);
    falloff = falloff * falloff;
    
    return attenuation * falloff;
}

/**
 * Enhanced PBR lighting calculation for a single light
 */
vec3 calculatePBRLighting(vec3 worldPos, vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, 
                         vec3 lightPos, vec3 lightColor, float lightIntensity, bool castShadows) {
    vec3 L = normalize(lightPos - worldPos);
    vec3 H = normalize(V + L);
    float distance = length(lightPos - worldPos);
    
    // Calculate F0
    vec3 F0 = calculateF0(albedo, metallic);
    
    // Cook-Torrance BRDF components
    float NDF = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
    
    // Energy conservation
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= 1.0 - metallic; // Metals have no diffuse
    
    // BRDF calculation
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + EPSILON;
    vec3 specular = numerator / denominator;
    
    float NdotL = max(dot(N, L), 0.0);
    
    // Shadow calculation
    float shadow = 0.0;
    if (castShadows && NdotL > 0.0) {
        shadow = calculateShadow(worldPos, N, L);
    }
    
    // Final lighting contribution
    vec3 radiance = lightColor * lightIntensity;
    return (kD * albedo * INV_PI + specular) * radiance * NdotL * (1.0 - shadow);
}

/**
 * Calculate atmospheric scattering effect
 */
vec3 calculateAtmosphericScattering(vec3 worldPos, vec3 lightDir, vec3 viewDir) {
    if (scatteringStrength <= 0.0) return vec3(0.0);
    
    float distance = length(worldPos - viewPos);
    float scattering = exp(-distance * atmosphereThickness * 0.001);
    
    // Simple Rayleigh scattering approximation
    float cosTheta = dot(viewDir, lightDir);
    float phase = 0.75 * (1.0 + cosTheta * cosTheta);
    
    return skyColor * scatteringStrength * phase * (1.0 - scattering);
}

/**
 * Enhanced ambient lighting with image-based lighting approximation
 */
vec3 calculateAmbientLighting(vec3 N, vec3 V, vec3 albedo, float metallic, float roughness, float ao) {
    vec3 F0 = calculateF0(albedo, metallic);
    vec3 F = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
    
    vec3 kS = F;
    vec3 kD = 1.0 - kS;
    kD *= 1.0 - metallic;
    
    // Diffuse ambient
    vec3 diffuse = ambientColor * albedo;
    
    // Specular ambient (simplified IBL)
    vec3 specular = ambientColor * 0.5; // Simplified specular ambient
    
    // Time of day modulation
    float dayFactor = smoothstep(0.2, 0.8, timeOfDay);
    dayFactor = dayFactor * (1.0 - dayFactor) * 4.0; // Peak at 0.5 (noon)
    
    vec3 ambient = (kD * diffuse + specular) * ambientStrength * ao;
    ambient *= mix(0.3, 1.0, dayFactor); // Darker at night
    
    return ambient;
}

/**
 * Main enhanced lighting calculation
 */
vec3 calculateLighting(vec3 worldPos, vec3 normal, vec3 albedo, float metallic, float roughness, float emissive, vec3 viewDir) {
    // Validate inputs
    vec3 safeWorldPos = validateVec3(worldPos, vec3(0.0));
    vec3 safeNormal = validateVec3(normalize(normal), vec3(0.0, 1.0, 0.0));
    vec3 safeAlbedo = validateVec3(albedo, vec3(0.5));
    float safeMetallic = validateFloat(metallic, 0.0, 1.0, 0.0);
    float safeRoughness = validateFloat(roughness, 0.05, 1.0, 0.8);
    float safeEmissive = validateFloat(emissive, 0.0, 1.0, 0.0);
    
    vec3 N = safeNormal;
    vec3 V = normalize(viewPos - safeWorldPos);
    
    vec3 Lo = vec3(0.0);
    
    // Directional light (sun/moon)
    if (dirLight.intensity > 0.0) {
        vec3 lightDir = normalize(-dirLight.direction);
        float viewDepth = length(safeWorldPos - viewPos);
        
        // Use cascaded shadows if available
        float shadow = 0.0;
        if (dirLight.castShadows) {
            shadow = calculateCascadedShadow(safeWorldPos, N, lightDir, viewDepth);
        }
        
        // PBR calculation
        vec3 H = normalize(V + lightDir);
        vec3 F0 = calculateF0(safeAlbedo, safeMetallic);
        
        float NDF = distributionGGX(N, H, safeRoughness);
        float G = geometrySmith(N, V, lightDir, safeRoughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
        
        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - safeMetallic;
        
        vec3 numerator = NDF * G * F;
        float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, lightDir), 0.0) + EPSILON;
        vec3 specular = numerator / denominator;
        
        float NdotL = max(dot(N, lightDir), 0.0);
        vec3 radiance = dirLight.color * dirLight.intensity;
        
        Lo += (kD * safeAlbedo * INV_PI + specular) * radiance * NdotL * (1.0 - shadow);
        
        // Add atmospheric scattering
        if (enableAdvancedLighting) {
            Lo += calculateAtmosphericScattering(safeWorldPos, lightDir, V);
        }
    }
    
    // Point lights (with performance optimization)
    int maxLights = min(numLights, min(MAX_LIGHTS, maxLightSamples));
    for (int i = 0; i < maxLights; ++i) {
        vec3 lightPos = lights[i].position;
        float distance = length(lightPos - safeWorldPos);
        
        // Early distance culling
        if (distance > lights[i].radius) continue;
        
        float attenuation = calculateAttenuation(distance, 0.09, 0.032, lights[i].radius);
        if (attenuation < 0.01) continue; // Skip negligible contributions
        
        vec3 lightContrib = calculatePBRLighting(safeWorldPos, N, V, safeAlbedo, safeMetallic, safeRoughness,
                                                lightPos, lights[i].color, lights[i].intensity * attenuation, lights[i].castShadows);
        Lo += lightContrib;
    }
    
    // Spot lights
    int maxSpotLights = min(numSpotLights, MAX_SPOT_LIGHTS);
    for (int i = 0; i < maxSpotLights; ++i) {
        vec3 lightPos = spotLights[i].position;
        float distance = length(lightPos - safeWorldPos);
        vec3 lightDir = normalize(lightPos - safeWorldPos);
        
        float attenuation = calculateAttenuation(distance, spotLights[i].linear, spotLights[i].quadratic, 50.0);
        float spotIntensity = calculateSpotLight(lightDir, spotLights[i].direction, spotLights[i].cutOff, spotLights[i].outerCutOff);
        
        if (attenuation * spotIntensity < 0.01) continue;
        
        vec3 lightContrib = calculatePBRLighting(safeWorldPos, N, V, safeAlbedo, safeMetallic, safeRoughness,
                                                lightPos, spotLights[i].color, spotLights[i].intensity * attenuation * spotIntensity, spotLights[i].castShadows);
        Lo += lightContrib;
    }
    
    // Enhanced ambient lighting
    vec3 ambient = calculateAmbientLighting(N, V, safeAlbedo, safeMetallic, safeRoughness, 1.0);
    
    // Add subtle rim lighting for depth
    float rimFactor = 1.0 - max(dot(N, V), 0.0);
    vec3 rimLight = vec3(0.1, 0.2, 0.4) * pow(rimFactor, 3.0) * 0.5;
    
    // Add emissive contribution
    vec3 emissiveContrib = safeAlbedo * safeEmissive * 2.0;
    
    return ambient + Lo + rimLight + emissiveContrib;
}

// ========================================================================
// ENHANCED FOG AND WEATHER EFFECTS
// ========================================================================

/**
 * Calculate volumetric fog with multiple layers
 */
vec3 calculateVolumetricFog(vec3 worldPos, vec3 viewDir, float distance) {
    if (fogDensity <= 0.0) return vec3(0.0);
    
    vec3 fogContribution = vec3(0.0);
    
    // Exponential fog
    float expFog = 1.0 - exp(-distance * fogDensity);
    
    // Height-based fog
    float height = worldPos.y;
    float heightFactor = exp(-max(0.0, height) * fogHeightFalloff);
    
    // Distance-based fog layers
    float nearFog = smoothstep(fogStart, fogEnd, distance);
    
    // Combine fog factors
    float totalFog = expFog * heightFactor * nearFog;
    totalFog = clamp(totalFog, 0.0, 1.0);
    
    // Atmospheric perspective
    vec3 atmosphericColor = mix(fogColor, skyColor, 0.3);
    
    return atmosphericColor * totalFog;
}

/**
 * Calculate weather-based fog effects
 */
vec3 calculateWeatherFog(vec3 worldPos, vec3 viewDir, float distance) {
    vec3 weatherFog = vec3(0.0);
    
    // Rain fog
    if (rainIntensity > 0.0) {
        float rainFog = rainIntensity * 0.5;
        float rainDistance = distance * (1.0 + rainFog);
        float rainFactor = 1.0 - exp(-rainDistance * fogDensity * 2.0);
        weatherFog += vec3(0.6, 0.7, 0.8) * rainFactor * rainIntensity;
    }
    
    // Snow fog
    if (snowIntensity > 0.0) {
        float snowFog = snowIntensity * 0.3;
        float snowDistance = distance * (1.0 + snowFog);
        float snowFactor = 1.0 - exp(-snowDistance * fogDensity * 1.5);
        weatherFog += vec3(0.9, 0.95, 1.0) * snowFactor * snowIntensity;
    }
    
    // Wind-based fog movement
    if (windStrength > 0.0) {
        vec2 windOffset = vec2(windStrength * time * 0.1);
        float windNoise = sin(worldPos.x * 0.01 + windOffset.x) * cos(worldPos.z * 0.01 + windOffset.y);
        weatherFog *= (1.0 + windNoise * 0.2);
    }
    
    return weatherFog;
}

/**
 * Enhanced fog application with multiple effects
 */
vec3 applyFog(vec3 color, vec3 worldPos, vec3 viewDir, float distance) {
    vec3 totalFog = vec3(0.0);
    
    // Base volumetric fog
    totalFog += calculateVolumetricFog(worldPos, viewDir, distance);
    
    // Weather-based fog
    totalFog += calculateWeatherFog(worldPos, viewDir, distance);
    
    // Time of day fog modulation
    float dayFactor = smoothstep(0.0, 0.3, timeOfDay) * smoothstep(1.0, 0.7, timeOfDay);
    totalFog *= mix(1.5, 1.0, dayFactor); // More fog at dawn/dusk
    
    // Seasonal fog variation
    float seasonalFog = mix(0.8, 1.2, seasonTransition); // More fog in winter
    totalFog *= seasonalFog;
    
    // Apply fog to color
    float fogStrength = length(totalFog);
    fogStrength = clamp(fogStrength, 0.0, 1.0);
    
    return mix(color, totalFog, fogStrength);
}

/**
 * Calculate rain effects on lighting
 */
vec3 calculateRainEffects(vec3 color, vec3 worldPos, vec3 normal) {
    if (rainIntensity <= 0.0) return color;
    
    // Rain darkening effect
    float rainDarkening = 1.0 - rainIntensity * 0.3;
    
    // Wet surface enhancement
    float wetness = rainIntensity;
    float normalDotUp = max(dot(normal, vec3(0.0, 1.0, 0.0)), 0.0);
    wetness *= normalDotUp; // More wetness on horizontal surfaces
    
    // Enhanced reflectivity for wet surfaces
    vec3 wetColor = color * (1.0 + wetness * 0.2);
    wetColor = mix(wetColor, wetColor * 1.1, wetness);
    
    return wetColor * rainDarkening;
}

/**
 * Calculate snow effects on lighting
 */
vec3 calculateSnowEffects(vec3 color, vec3 worldPos, vec3 normal) {
    if (snowIntensity <= 0.0) return color;
    
    // Snow accumulation on horizontal surfaces
    float normalDotUp = max(dot(normal, vec3(0.0, 1.0, 0.0)), 0.0);
    float snowAccumulation = snowIntensity * normalDotUp;
    
    // Snow color mixing
    vec3 snowColor = vec3(0.9, 0.95, 1.0);
    vec3 snowyColor = mix(color, snowColor, snowAccumulation);
    
    // Snow brightness enhancement
    snowyColor *= (1.0 + snowAccumulation * 0.3);
    
    return snowyColor;
}

/**
 * Apply comprehensive weather effects
 */
vec3 applyWeatherEffects(vec3 color, vec3 worldPos, vec3 normal) {
    vec3 weatherColor = color;
    
    // Apply rain effects
    weatherColor = calculateRainEffects(weatherColor, worldPos, normal);
    
    // Apply snow effects
    weatherColor = calculateSnowEffects(weatherColor, worldPos, normal);
    
    // Temperature-based color shifts
    if (temperature < 0.0) {
        // Cold blue tint
        float coldFactor = clamp(-temperature / 20.0, 0.0, 1.0);
        weatherColor = mix(weatherColor, weatherColor * vec3(0.8, 0.9, 1.2), coldFactor * 0.3);
    } else if (temperature > 25.0) {
        // Warm orange tint
        float warmFactor = clamp((temperature - 25.0) / 15.0, 0.0, 1.0);
        weatherColor = mix(weatherColor, weatherColor * vec3(1.1, 1.05, 0.9), warmFactor * 0.2);
    }
    
    return weatherColor;
}

// ========================================================================
// ENHANCED TONE MAPPING AND POST-PROCESSING
// ========================================================================

/**
 * Reinhard tone mapping with exposure control
 */
vec3 reinhardToneMapping(vec3 color) {
    color *= exposure;
    return color / (1.0 + color);
}

/**
 * Helper function for filmic tone mapping curve
 */
vec3 filmicCurve(vec3 x, float A, float B, float C, float D, float E, float F) {
    vec3 numerator = ((x * (A * x + C * B) + D * E));
    vec3 denominator = ((x * (A * x + B) + D * F));
    return numerator / (denominator + EPSILON);
}

/**
 * Enhanced filmic tone mapping with customizable parameters
 */
vec3 filmicToneMapping(vec3 color) {
    color *= exposure;

    // Filmic curve parameters
    float A = 0.15; // Shoulder strength
    float B = 0.50; // Linear strength
    float C = 0.10; // Linear angle
    float D = 0.20; // Toe strength
    float E = 0.02; // Toe numerator
    float F = 0.30; // Toe denominator

    // Apply curve to input color
    vec3 result = filmicCurve(color, A, B, C, D, E, F);

    // Calculate normalization factor based on a white point.
    // The white point must also be multiplied by exposure to match the original's behavior.
    float whitePoint = 11.2;
    vec3 exposedWhitePoint = vec3(whitePoint) * exposure;
    vec3 whiteResult = filmicCurve(exposedWhitePoint, A, B, C, D, E, F);

    // Normalize and return
    return result / whiteResult;
}

/**
 * Enhanced ACES tone mapping
 */
vec3 acesToneMapping(vec3 color) {
    color *= exposure;
    
    // ACES fitted curve
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

/**
 * Apply tone mapping based on selected type
 */
vec3 applyToneMapping(vec3 color) {
    if (!enableToneMapping) return color;
    
    vec3 result = color;
    
    if (toneMappingType == TONEMAP_REINHARD) {
        result = reinhardToneMapping(color);
    } else if (toneMappingType == TONEMAP_FILMIC) {
        result = filmicToneMapping(color);
    } else if (toneMappingType == TONEMAP_ACES) {
        result = acesToneMapping(color);
    }
    
    return result;
}

/**
 * Apply color grading and post-processing effects
 */
vec3 applyColorGrading(vec3 color) {
    // Contrast adjustment
    color = mix(vec3(0.5), color, contrast);
    
    // Saturation adjustment
    float lum = luminance(color);
    color = mix(vec3(lum), color, saturation);
    
    // Brightness adjustment
    color *= brightness;
    
    return color;
}

/**
 * Enhanced main function with comprehensive deferred lighting pipeline
 */
void main()
{
    // Input validation and setup
    vec2 texCoord = TexCoords;
    texCoord = clamp(texCoord, vec2(0.0), vec2(1.0));
    
    // Sample G-buffer with safe texture sampling
    vec3 albedo = safeTexture2D(gAlbedoSpec, texCoord, vec4(0.5, 0.5, 0.5, 0.0)).rgb;
    vec3 normal = safeTexture2D(gNormal, texCoord, vec4(0.0, 1.0, 0.0, 1.0)).rgb;
    vec3 position = safeTexture2D(gPosition, texCoord, vec4(0.0, 0.0, 0.0, 1.0)).rgb;
    vec4 material = safeTexture2D(gMaterial, texCoord, vec4(0.0, 0.8, 1.0, 0.0));
    
    // Validate and extract material properties
    float metallic = validateFloat(material.r, 0.0, 1.0, 0.0);
    float roughness = validateFloat(material.g, 0.05, 1.0, 0.8);
    float ao = validateFloat(material.b, 0.0, 1.0, 1.0);
    float emissive = validateFloat(material.a, 0.0, 1.0, 0.0);
    
    // Validate G-buffer data
    albedo = validateVec3(albedo, vec3(0.5));
    normal = validateVec3(normalize(normal), vec3(0.0, 1.0, 0.0));
    position = validateVec3(position, vec3(0.0));
    
    // Early exit for skybox or invalid data
    float normalLength = length(normal);
    if (normalLength < 0.1 || length(position) > 10000.0) {
        // Beautiful gradient sky with time-based variation
        float height = texCoord.y;
        vec3 skyTop = vec3(0.4, 0.7, 1.0);
        vec3 skyBottom = vec3(0.8, 0.9, 1.0);
        vec3 skyColor = mix(skyBottom, skyTop, height);
        
        // Add subtle time-based variation for dynamic sky
        skyColor += 0.1 * sin(time * 0.1) * vec3(0.1, 0.05, 0.0);
        
        // Add clouds using noise
        float cloudNoise = sin(texCoord.x * 10.0 + time * 0.5) * sin(texCoord.y * 8.0 + time * 0.3);
        skyColor += cloudNoise * 0.05 * vec3(1.0, 1.0, 1.0);
        
        // Apply basic post-processing to skybox
        skyColor = applyColorGrading(skyColor);
        skyColor = pow(skyColor, vec3(1.0/gamma));
        
        FragColor = vec4(skyColor, 1.0);
        return;
    }
    
    // Reconstruct world position and view direction
    vec3 worldPos = position;
    vec3 viewDir = normalize(viewPos - worldPos);
    float viewDistance = length(viewPos - worldPos);
    
    // Calculate base lighting
    vec3 color = calculateLighting(worldPos, normal, albedo, metallic, roughness, emissive, viewDir);
    
    // Apply ambient occlusion
    color *= ao;
    
    // Apply SSAO if enabled
    if (enableSSAO) {
        float ssaoFactor = safeTexture2D(ssaoTexture, texCoord, vec4(1.0)).r;
        ssaoFactor = validateFloat(ssaoFactor, 0.0, 1.0, 1.0);
        color *= ssaoFactor;
    }
    
    // Apply weather effects
    color = applyWeatherEffects(color, worldPos, normal);
    
    // Apply atmospheric fog
    if (enableFog) {
        color = applyFog(color, worldPos, viewDir, viewDistance);
    }
    
    // Apply tone mapping
    if (enableToneMapping) {
        color = applyToneMapping(color);
    }
    
    // Apply color grading and post-processing
    color = applyColorGrading(color);
    
    // Gamma correction
    color = pow(max(color, vec3(0.0)), vec3(1.0/gamma));
    
    // Add film grain for cinematic feel
    float grain = (fract(sin(dot(texCoord + time * 0.01, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.02;
    color += grain;
    
    // Color grading for enhanced visuals
    color = mix(color, color * vec3(1.02, 1.0, 0.98), 0.1); // Slight warm tint
    
    // Final color validation and output
    color = clamp(color, vec3(0.0), vec3(1.0));
    
    FragColor = vec4(color, 1.0);
}