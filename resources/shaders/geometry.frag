#version 330 core

/**
 * Geometry Pass Fragment Shader - Enhanced G-Buffer Generation
 * Generates robust G-Buffer data with advanced material classification,
 * seasonal effects, wetness simulation, and comprehensive error handling.
 * 
 * Features:
 * - Multi-layer material detection with fallback systems
 * - Physically-based seasonal color transitions
 * - Advanced wetness simulation with surface property modification
 * - Robust input validation and error handling
 * - Optimized texture sampling with mipmap support
 * - Enhanced material property calculation
 */

// Inputs from Vertex Shader
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

// G-Buffer Outputs with enhanced precision
layout (location = 0) out vec4 gPosition;    // xyz: world position, w: depth
layout (location = 1) out vec4 gNormal;      // xyz: world normal, w: normal strength
layout (location = 2) out vec4 gAlbedoSpec;  // rgb: albedo, a: specular strength
layout (location = 3) out vec4 gMaterial;    // x: metallic, y: roughness, z: ao, w: emissive

// Enhanced texture uniforms with validation
uniform sampler2D texture_diffuse1;
uniform sampler2D texture_normal1;     // Optional normal map
uniform sampler2D texture_specular1;   // Optional specular map
uniform sampler2D texture_roughness1;  // Optional roughness map

// Environmental uniforms with bounds checking
uniform float u_wetness;           // [0.0, 1.0] - wetness factor
uniform float u_temperature;       // [-40.0, 50.0] - temperature in Celsius
uniform float u_humidity;          // [0.0, 1.0] - humidity factor
uniform float u_windStrength;      // [0.0, 1.0] - wind strength

// Enhanced seasonal uniforms
uniform int u_season;              // [0, 3] - season index
uniform float u_seasonTransition;  // [0.0, 1.0] - transition progress
uniform float u_timeOfDay;         // [0.0, 1.0] - time of day

// Quality and performance controls
uniform int u_materialQuality;     // [0, 3] - material quality level
uniform bool u_enableSeasonalEffects;
uniform bool u_enableWetnessEffects;
uniform bool u_enableAdvancedMaterials;

// Enhanced seasonal color palettes with multiple variants
const vec3 springColors[3] = vec3[](
    vec3(0.7, 0.9, 0.4),   // Fresh green
    vec3(0.8, 0.95, 0.5),  // Bright spring
    vec3(0.6, 0.85, 0.35)  // Deep spring
);

const vec3 summerColors[3] = vec3[](
    vec3(0.4, 0.8, 0.3),   // Rich green
    vec3(0.35, 0.75, 0.25), // Deep summer
    vec3(0.45, 0.85, 0.35)  // Lush green
);

const vec3 autumnColors[3] = vec3[](
    vec3(0.9, 0.6, 0.2),   // Orange
    vec3(0.95, 0.4, 0.1),  // Red-orange
    vec3(0.85, 0.7, 0.3)   // Yellow-orange
);

const vec3 winterColors[3] = vec3[](
    vec3(0.85, 0.85, 0.9), // Frost
    vec3(0.9, 0.9, 0.95),  // Snow
    vec3(0.8, 0.82, 0.88)  // Ice
);

// Material type constants for robust classification
const int MATERIAL_UNKNOWN = 0;
const int MATERIAL_FOLIAGE = 1;
const int MATERIAL_STONE = 2;
const int MATERIAL_METAL = 3;
const int MATERIAL_WOOD = 4;
const int MATERIAL_EMISSIVE = 5;
const int MATERIAL_WATER = 6;
const int MATERIAL_GLASS = 7;

// Quality level constants
const int QUALITY_LOW = 0;
const int QUALITY_MEDIUM = 1;
const int QUALITY_HIGH = 2;
const int QUALITY_ULTRA = 3;

// Utility constants
const float EPSILON = 1e-6;
const float PI = 3.14159265359;
const float INV_PI = 0.31830988618;

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Validates and clamps input values to safe ranges
 */
float validateFloat(float value, float minVal, float maxVal, float defaultVal) {
    if (isnan(value) || isinf(value)) {
        return defaultVal;
    }
    return clamp(value, minVal, maxVal);
}

int validateInt(int value, int minVal, int maxVal, int defaultVal) {
    return clamp(value, minVal, maxVal);
}

vec2 validateTexCoords(vec2 coords) {
    return clamp(coords, vec2(0.0), vec2(1.0));
}

vec3 validateColor(vec3 color) {
    return clamp(color, vec3(0.0), vec3(1.0));
}

/**
 * Safe texture sampling with fallback
 */
vec4 safeTexture2D(sampler2D tex, vec2 coords, vec4 fallback) {
    vec2 safeCoords = validateTexCoords(coords);
    vec4 sample = texture(tex, safeCoords);
    
    // Check for invalid samples
    if (any(isnan(sample)) || any(isinf(sample))) {
        return fallback;
    }
    
    return sample;
}

/**
 * Advanced material classification with multiple detection methods
 */
int classifyMaterial(vec3 albedo, vec2 texCoords) {
    float avgColor = (albedo.r + albedo.g + albedo.b) / 3.0;
    float colorVariance = length(albedo - vec3(avgColor));
    
    // Enhanced foliage detection
    bool isFoliage = (albedo.g > albedo.r && albedo.g > albedo.b && 
                     albedo.g > 0.3 && colorVariance > 0.1);
    
    // Emissive material detection
    bool isEmissive = (avgColor > 0.8 && 
                      (albedo.r > 0.8 || (albedo.r > 0.9 && albedo.g > 0.7)));
    
    // Metal detection (low albedo, high reflectance)
    bool isMetal = (avgColor < 0.4 && colorVariance < 0.1 && 
                   (albedo.r > albedo.g && albedo.r > albedo.b));
    
    // Stone/rock detection
    bool isStone = (avgColor < 0.5 && colorVariance < 0.2 && 
                   abs(albedo.r - albedo.g) < 0.1);
    
    // Wood detection
    bool isWood = (albedo.r > albedo.g && albedo.g > albedo.b && 
                  avgColor > 0.3 && avgColor < 0.7);
    
    // Glass detection (high brightness, low saturation)
    bool isGlass = (avgColor > 0.7 && colorVariance < 0.05);
    
    // Return material type with priority order
    if (isEmissive) return MATERIAL_EMISSIVE;
    if (isFoliage) return MATERIAL_FOLIAGE;
    if (isMetal) return MATERIAL_METAL;
    if (isGlass) return MATERIAL_GLASS;
    if (isWood) return MATERIAL_WOOD;
    if (isStone) return MATERIAL_STONE;
    
    return MATERIAL_UNKNOWN;
}

/**
 * Enhanced seasonal color calculation with smooth transitions
 */
vec3 calculateSeasonalColor(vec3 baseColor, int materialType, int season, float transition) {
    if (!u_enableSeasonalEffects || materialType != MATERIAL_FOLIAGE) {
        return baseColor;
    }
    
    // Validate season and transition
    int safeSeason = validateInt(season, 0, 3, 0);
    float safeTransition = validateFloat(transition, 0.0, 1.0, 0.0);
    
    // Select color variant based on texture coordinates for variety
    int colorVariant = int(fract(TexCoords.x * TexCoords.y * 7.0) * 3.0);
    
    vec3 currentSeasonColor, nextSeasonColor;
    
    // Get seasonal colors
    if (safeSeason == 0) {
        currentSeasonColor = springColors[colorVariant];
        nextSeasonColor = summerColors[colorVariant];
    } else if (safeSeason == 1) {
        currentSeasonColor = summerColors[colorVariant];
        nextSeasonColor = autumnColors[colorVariant];
    } else if (safeSeason == 2) {
        currentSeasonColor = autumnColors[colorVariant];
        nextSeasonColor = winterColors[colorVariant];
    } else {
        currentSeasonColor = winterColors[colorVariant];
        nextSeasonColor = springColors[colorVariant];
    }
    
    // Smooth interpolation between seasons
    vec3 seasonalColor = mix(currentSeasonColor, nextSeasonColor, 
                            smoothstep(0.0, 1.0, safeTransition));
    
    // Preserve original texture detail while applying seasonal tint
    float mixFactor = 0.6 + 0.2 * sin(u_timeOfDay * PI); // Vary with time of day
    return mix(baseColor, seasonalColor, mixFactor);
}

/**
 * Advanced wetness effects with surface property modification
 */
vec3 applyWetnessEffects(vec3 albedo, float wetness, inout float roughness, 
                        inout float metallic, inout float specular) {
    if (!u_enableWetnessEffects || wetness < EPSILON) {
        return albedo;
    }
    
    float safeWetness = validateFloat(wetness, 0.0, 1.0, 0.0);
    
    // Darken albedo (wet surfaces appear darker)
    float darkeningFactor = 1.0 - (safeWetness * 0.4);
    vec3 wetAlbedo = albedo * darkeningFactor;
    
    // Increase saturation slightly
    float luminance = dot(wetAlbedo, vec3(0.299, 0.587, 0.114));
    wetAlbedo = mix(vec3(luminance), wetAlbedo, 1.0 + safeWetness * 0.2);
    
    // Modify surface properties
    roughness = mix(roughness, 0.05, safeWetness * 0.8); // Smoother when wet
    metallic = mix(metallic, 0.1, safeWetness * 0.3);    // Slight metallic look
    specular = mix(specular, 0.9, safeWetness);          // Higher specular
    
    return wetAlbedo;
}

/**
 * Calculate material properties based on type and environmental factors
 */
vec4 calculateMaterialProperties(int materialType, vec3 albedo, float wetness, float temperature) {
    float metallic = 0.0;
    float roughness = 0.8;
    float ao = 1.0;
    float emissive = 0.0;
    
    // Base material properties
    if (materialType == MATERIAL_FOLIAGE) {
        metallic = 0.0;
        roughness = 0.9;
        ao = 0.8;
        emissive = 0.0;
    } else if (materialType == MATERIAL_METAL) {
        metallic = 0.9;
        roughness = 0.1;
        ao = 1.0;
        emissive = 0.0;
    } else if (materialType == MATERIAL_STONE) {
        metallic = 0.0;
        roughness = 0.95;
        ao = 0.9;
        emissive = 0.0;
    } else if (materialType == MATERIAL_WOOD) {
        metallic = 0.0;
        roughness = 0.8;
        ao = 0.85;
        emissive = 0.0;
    } else if (materialType == MATERIAL_EMISSIVE) {
        metallic = 0.0;
        roughness = 0.3;
        ao = 1.0;
        emissive = 0.8;
        
        // Enhanced emissive detection
        float avgColor = (albedo.r + albedo.g + albedo.b) / 3.0;
        if (albedo.r > 0.9 && albedo.g > 0.7 && albedo.b > 0.3) {
            emissive = 1.0; // Bright yellow/white emissive
        } else if (albedo.r > 0.8 && albedo.g < 0.4 && albedo.b < 0.3) {
            emissive = 0.9; // Red emissive
        }
    } else if (materialType == MATERIAL_GLASS) {
        metallic = 0.0;
        roughness = 0.0;
        ao = 1.0;
        emissive = 0.0;
    } else if (materialType == MATERIAL_WATER) {
        metallic = 0.0;
        roughness = 0.0;
        ao = 1.0;
        emissive = 0.0;
    }
    
    // Environmental modifications
    if (temperature < 0.0) {
        // Frost effects
        roughness = mix(roughness, 0.3, abs(temperature) / 40.0);
        ao = mix(ao, 0.95, abs(temperature) / 40.0);
    }
    
    // Validate final values
    metallic = validateFloat(metallic, 0.0, 1.0, 0.0);
    roughness = validateFloat(roughness, 0.05, 1.0, 0.8);
    ao = validateFloat(ao, 0.0, 1.0, 1.0);
    emissive = validateFloat(emissive, 0.0, 1.0, 0.0);
    
    return vec4(metallic, roughness, ao, emissive);
}

void main() {
    // ========================================================================
    // INPUT VALIDATION AND EARLY EXITS
    // ========================================================================
    
    // Validate input coordinates and normal
    vec2 safeTexCoords = validateTexCoords(TexCoords);
    vec3 safeNormal = normalize(Normal);
    
    // Check for degenerate normal
    if (length(Normal) < EPSILON) {
        // Use default upward normal for degenerate cases
        safeNormal = vec3(0.0, 1.0, 0.0);
    }
    
    // Validate environmental parameters
    float safeWetness = validateFloat(u_wetness, 0.0, 1.0, 0.0);
    float safeTemperature = validateFloat(u_temperature, -40.0, 50.0, 20.0);
    float safeHumidity = validateFloat(u_humidity, 0.0, 1.0, 0.5);
    int safeSeason = validateInt(u_season, 0, 3, 0);
    float safeTransition = validateFloat(u_seasonTransition, 0.0, 1.0, 0.0);
    int safeQuality = validateInt(u_materialQuality, 0, 3, 1);
    
    // ========================================================================
    // TEXTURE SAMPLING WITH FALLBACKS
    // ========================================================================
    
    // Primary diffuse texture with fallback
    vec4 texColor = safeTexture2D(texture_diffuse1, safeTexCoords, vec4(0.5, 0.5, 0.5, 1.0));
    
    // Alpha testing with enhanced threshold
    float alphaThreshold = 0.1;
    if (safeQuality >= QUALITY_HIGH) {
        alphaThreshold = 0.05; // More precise alpha testing for high quality
    }
    
    if (texColor.a < alphaThreshold) {
        discard;
    }
    
    // Optional texture maps (only sample if quality allows)
    vec3 normalMap = vec3(0.0, 0.0, 1.0);
    float specularMap = 0.5;
    float roughnessMap = 0.8;
    
    if (safeQuality >= QUALITY_MEDIUM && u_enableAdvancedMaterials) {
        // Sample additional maps with fallbacks
        vec4 normalSample = safeTexture2D(texture_normal1, safeTexCoords, vec4(0.5, 0.5, 1.0, 1.0));
        normalMap = normalize(normalSample.rgb * 2.0 - 1.0);
        
        specularMap = safeTexture2D(texture_specular1, safeTexCoords, vec4(0.5)).r;
        roughnessMap = safeTexture2D(texture_roughness1, safeTexCoords, vec4(0.8)).r;
    }
    
    // ========================================================================
    // MATERIAL CLASSIFICATION AND PROCESSING
    // ========================================================================
    
    // Validate and process base color
    vec3 baseAlbedo = validateColor(texColor.rgb);
    
    // Advanced material classification
    int materialType = classifyMaterial(baseAlbedo, safeTexCoords);
    
    // Apply seasonal effects
    vec3 seasonalAlbedo = calculateSeasonalColor(baseAlbedo, materialType, safeSeason, safeTransition);
    
    // Calculate base material properties
    vec4 materialProps = calculateMaterialProperties(materialType, seasonalAlbedo, safeWetness, safeTemperature);
    float metallic = materialProps.x;
    float roughness = materialProps.y;
    float ao = materialProps.z;
    float emissive = materialProps.w;
    
    // Apply texture-based modifications
    if (safeQuality >= QUALITY_MEDIUM && u_enableAdvancedMaterials) {
        roughness = mix(roughness, roughnessMap, 0.8);
    }
    
    float specular = mix(0.3, specularMap, 0.7);
    
    // ========================================================================
    // ENVIRONMENTAL EFFECTS
    // ========================================================================
    
    // Apply wetness effects
    vec3 finalAlbedo = applyWetnessEffects(seasonalAlbedo, safeWetness, roughness, metallic, specular);
    
    // Temperature effects
    if (safeTemperature < 0.0) {
        // Frost effects - slight blue tint and increased reflectance
        float frostFactor = clamp(abs(safeTemperature) / 20.0, 0.0, 1.0);
        finalAlbedo = mix(finalAlbedo, finalAlbedo * vec3(0.9, 0.95, 1.1), frostFactor * 0.3);
        specular = mix(specular, 0.9, frostFactor * 0.5);
    }
    
    // Humidity effects (subtle)
    if (safeHumidity > 0.7) {
        float humidityFactor = (safeHumidity - 0.7) / 0.3;
        roughness = mix(roughness, roughness * 0.9, humidityFactor * 0.2);
    }
    
    // Wind effects on foliage (subtle color variation)
    if (materialType == MATERIAL_FOLIAGE && u_windStrength > 0.1) {
        float windNoise = sin(safeTexCoords.x * 10.0 + u_timeOfDay * 5.0) * 0.1;
        finalAlbedo += windNoise * u_windStrength * 0.05;
    }
    
    // ========================================================================
    // G-BUFFER OUTPUT WITH VALIDATION
    // ========================================================================
    
    // Position with enhanced depth information
    float linearDepth = length(FragPos) / 1000.0; // Normalize to reasonable range
    gPosition = vec4(FragPos, clamp(linearDepth, 0.0, 1.0));
    
    // Normal with strength information
    vec3 finalNormal = safeNormal;
    if (safeQuality >= QUALITY_HIGH && u_enableAdvancedMaterials) {
        // Apply normal mapping
        vec3 tangent = normalize(cross(safeNormal, vec3(0.0, 1.0, 0.0)));
        vec3 bitangent = cross(safeNormal, tangent);
        mat3 TBN = mat3(tangent, bitangent, safeNormal);
        finalNormal = normalize(TBN * normalMap);
    }
    
    float normalStrength = 1.0;
    gNormal = vec4(finalNormal, normalStrength);
    
    // Albedo and specular with validation
    gAlbedoSpec = vec4(validateColor(finalAlbedo), clamp(specular, 0.0, 1.0));
    
    // Material properties with final validation
    gMaterial = vec4(
        clamp(metallic, 0.0, 1.0),
        clamp(roughness, 0.05, 1.0), // Prevent completely smooth surfaces
        clamp(ao, 0.0, 1.0),
        clamp(emissive, 0.0, 1.0)
    );
    
    // ========================================================================
    // DEBUG OUTPUT (if needed)
    // ========================================================================
    
    // Uncomment for debugging material classification
    // if (materialType == MATERIAL_EMISSIVE) {
    //     gAlbedoSpec.rgb = vec3(1.0, 0.0, 0.0); // Red for emissive
    // } else if (materialType == MATERIAL_FOLIAGE) {
    //     gAlbedoSpec.rgb = vec3(0.0, 1.0, 0.0); // Green for foliage
    // }
}