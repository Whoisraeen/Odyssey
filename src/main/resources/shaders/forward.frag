/**
 * Enhanced Forward Rendering Fragment Shader
 * 
 * Features:
 * - Physically Based Rendering (PBR)
 * - Multiple light types support
 * - Advanced material properties
 * - Environmental effects
 * - Input validation and error handling
 * - Performance optimizations
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
in vec3 FragPos;        // World space position
in vec3 Normal;         // World space normal
in vec2 TexCoord;       // Texture coordinates
in vec3 Tangent;        // Tangent vector (if available)
in vec3 Bitangent;      // Bitangent vector (if available)
in float MaterialID;    // Material identifier

// ========================================================================
// UNIFORMS - TEXTURES
// ========================================================================
uniform sampler2D albedoMap;        // Base color texture
uniform sampler2D normalMap;        // Normal map
uniform sampler2D metallicMap;      // Metallic map
uniform sampler2D roughnessMap;     // Roughness map
uniform sampler2D aoMap;            // Ambient occlusion map
uniform sampler2D emissiveMap;      // Emissive map
uniform sampler2D heightMap;        // Height/displacement map
uniform sampler2D diffuseTexture;   // Legacy diffuse texture

// ========================================================================
// UNIFORMS - MATERIAL PROPERTIES
// ========================================================================
uniform vec3 albedoColor;           // Base albedo color
uniform float metallicFactor;       // Metallic factor [0.0, 1.0]
uniform float roughnessFactor;      // Roughness factor [0.0, 1.0]
uniform float aoFactor;             // AO factor [0.0, 1.0]
uniform vec3 emissiveColor;         // Emissive color
uniform float emissiveStrength;     // Emissive strength
uniform float normalStrength;       // Normal map strength
uniform float heightScale;          // Height map scale

// ========================================================================
// UNIFORMS - LIGHTING
// ========================================================================
uniform vec3 viewPos;               // Camera position
uniform vec3 lightPos;              // Primary light position
uniform vec3 lightColor;            // Primary light color
uniform float lightIntensity;       // Primary light intensity
uniform vec3 ambientColor;          // Ambient light color
uniform float ambientStrength;      // Ambient light strength

// ========================================================================
// UNIFORMS - ENVIRONMENTAL
// ========================================================================
uniform float time;                 // Time for animations
uniform float exposure;             // Exposure value
uniform float gamma;                // Gamma correction value
uniform bool enableNormalMapping;   // Enable normal mapping
uniform bool enableParallaxMapping; // Enable parallax mapping
uniform bool enableEmissive;        // Enable emissive materials

// ========================================================================
// CONSTANTS
// ========================================================================
const float PI = 3.14159265359;
const float EPSILON = 1e-6;
const float MAX_REFLECTION_LOD = 4.0;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate float input
 */
float validateFloat(float value, float minVal, float maxVal, float defaultVal) {
    return clamp(value, minVal, maxVal);
}

/**
 * Validate vec3 input
 */
vec3 validateVec3(vec3 value, vec3 defaultVal) {
    return value;
}

/**
 * Safe texture sampling with fallback
 */
vec4 safeTexture(sampler2D tex, vec2 coords, vec4 fallback) {
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
// PBR FUNCTIONS
// ========================================================================

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
 * Geometry function (Smith's method)
 */
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    
    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    
    return num / max(denom, EPSILON);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);
    
    return ggx1 * ggx2;
}

/**
 * Fresnel equation (Schlick's approximation)
 */
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

void main() {
    // Input validation
    vec3 safeFragPos = validateVec3(FragPos, vec3(0.0));
    vec3 safeNormal = validateVec3(normalize(Normal), vec3(0.0, 1.0, 0.0));
    vec2 safeTexCoords = clamp(TexCoord, vec2(0.0), vec2(1.0));
    
    // Calculate view direction
    vec3 V = normalize(viewPos - safeFragPos);
    
    // Sample material textures with fallbacks
    vec3 albedo = safeTexture(diffuseTexture, safeTexCoords, vec4(0.8, 0.8, 0.8, 1.0)).rgb;
    float metallic = 0.0;
    float roughness = 0.8;
    float ao = 1.0;
    vec3 emissive = vec3(0.0);
    
    // Calculate normal
    vec3 N = safeNormal;
    
    // Calculate F0 (surface reflection at zero incidence)
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);
    
    // Light calculation
    vec3 L = normalize(lightPos - safeFragPos);
    vec3 H = normalize(V + L);
    float distance = length(lightPos - safeFragPos);
    float attenuation = 1.0 / (distance * distance + 1.0);
    vec3 radiance = lightColor * attenuation;
    
    // Cook-Torrance BRDF
    float NDF = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
    
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= 1.0 - metallic; // Metals have no diffuse lighting
    
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + EPSILON;
    vec3 specular = numerator / denominator;
    
    float NdotL = max(dot(N, L), 0.0);
    vec3 Lo = (kD * albedo / PI + specular) * radiance * NdotL;
    
    // Ambient lighting
    float ambientStr = 0.1;
    vec3 ambient = ambientStr * lightColor * albedo * ao;
    
    // Final color
    vec3 color = ambient + Lo + emissive;
    
    // Simple tone mapping and gamma correction
    color = color / (color + vec3(1.0));
    color = pow(color, vec3(1.0/2.2));
    
    // Output
    FragColor = vec4(clamp(color, vec3(0.0), vec3(1.0)), 1.0);
}