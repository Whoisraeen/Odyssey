#version 330 core

out vec4 FragColor;

in vec2 TexCoords;

// G-Buffer textures
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gAlbedoSpec;
uniform sampler2D gMaterial;

// Shadow mapping
uniform sampler2D shadowMap;
uniform mat4 lightSpaceMatrix;

// Lighting uniforms
struct Light {
    vec3 position;
    vec3 direction;
    vec3 color;
    float intensity;
    float radius;
    float innerCone;  // For spot lights
    float outerCone;  // For spot lights
    int type; // 0 = directional, 1 = point, 2 = spot
};

#define MAX_LIGHTS 8
uniform Light lights[MAX_LIGHTS];
uniform int numLights;
uniform vec3 viewPos;

// Environment
uniform float time;
uniform vec3 fogColor;
uniform float fogDensity;

// Constants
const float PI = 3.14159265359;

// PBR Functions
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    
    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    
    return num / denom;
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    
    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    
    return num / denom;
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);
    
    return ggx1 * ggx2;
}

// Shadow mapping calculation
float calculateShadow(vec3 worldPos, vec3 normal, vec3 lightDir) {
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(worldPos, 1.0);
    
    // Perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    
    // Outside shadow map bounds
    if(projCoords.z > 1.0)
        return 0.0;
    
    // Current depth
    float currentDepth = projCoords.z;
    
    // Bias to prevent shadow acne
    float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
    
    // PCF (Percentage Closer Filtering) for soft shadows
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x) {
        for(int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    
    return shadow;
}

// Spot light attenuation calculation
float calculateSpotLight(vec3 lightDir, vec3 spotDir, float innerCone, float outerCone) {
    float theta = dot(lightDir, normalize(-spotDir));
    float epsilon = innerCone - outerCone;
    float intensity = clamp((theta - outerCone) / epsilon, 0.0, 1.0);
    return intensity;
}

vec3 calculateLighting(vec3 worldPos, vec3 normal, vec3 albedo, float metallic, float roughness, float emissive, vec3 viewDir) {
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);
    
    vec3 Lo = vec3(0.0);
    
    // Calculate lighting from all lights
    for(int i = 0; i < numLights && i < MAX_LIGHTS; ++i) {
        vec3 lightDir;
        float attenuation = 1.0;
        float spotIntensity = 1.0;
        
        if(lights[i].type == 0) { // Directional light
            lightDir = normalize(-lights[i].direction);
        } else if(lights[i].type == 1) { // Point light
            lightDir = normalize(lights[i].position - worldPos);
            float distance = length(lights[i].position - worldPos);
            attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * (distance * distance));
        } else if(lights[i].type == 2) { // Spot light
            lightDir = normalize(lights[i].position - worldPos);
            float distance = length(lights[i].position - worldPos);
            attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * (distance * distance));
            spotIntensity = calculateSpotLight(lightDir, lights[i].direction, lights[i].innerCone, lights[i].outerCone);
        }
        
        vec3 halfwayDir = normalize(lightDir + viewDir);
        vec3 radiance = lights[i].color * lights[i].intensity * attenuation * spotIntensity;
        
        // Calculate shadow (only for first light - typically the sun)
        float shadow = 0.0;
        if(i == 0 && lights[i].type == 0) {
            shadow = calculateShadow(worldPos, normal, lightDir);
        }
        
        // Cook-Torrance BRDF
        float NdotV = max(dot(normal, viewDir), 0.0);
        float NdotL = max(dot(normal, lightDir), 0.0);
        float HdotV = max(dot(halfwayDir, viewDir), 0.0);
        
        float NDF = distributionGGX(normal, halfwayDir, roughness);
        float G = geometrySmith(normal, viewDir, lightDir, roughness);
        vec3 F = fresnelSchlick(HdotV, F0);
        
        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - metallic;
        
        vec3 numerator = NDF * G * F;
        float denominator = 4.0 * NdotV * NdotL + 0.0001;
        vec3 specular = numerator / denominator;
        
        // Apply shadow
        float shadowFactor = 1.0 - shadow;
        Lo += (kD * albedo / PI + specular) * radiance * NdotL * shadowFactor;
    }
    
    // Enhanced ambient lighting with subtle color variation
    vec3 ambient = vec3(0.03, 0.04, 0.06) * albedo;
    
    // Add subtle rim lighting for depth
    float rimFactor = 1.0 - max(dot(normal, viewDir), 0.0);
    vec3 rimLight = vec3(0.1, 0.2, 0.4) * pow(rimFactor, 3.0) * 0.5;
    
    // Add emissive contribution
    vec3 emissiveContribution = albedo * emissive * 2.0;
    
    return ambient + Lo + rimLight + emissiveContribution;
}

// Atmospheric fog
vec3 applyFog(vec3 color, float distance) {
    float fogAmount = 1.0 - exp(-distance * fogDensity);
    return mix(color, fogColor, fogAmount);
}

// Advanced tone mapping with filmic curve
vec3 filmicToneMapping(vec3 color) {
    vec3 x = max(vec3(0.0), color - 0.004);
    return (x * (6.2 * x + 0.5)) / (x * (6.2 * x + 1.7) + 0.06);
}

// ACES tone mapping
vec3 aces(vec3 color) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

void main()
{
    // Retrieve data from G-buffer
    vec3 worldPos = texture(gPosition, TexCoords).rgb;
    vec3 normal = normalize(texture(gNormal, TexCoords).rgb);
    vec4 albedoSpec = texture(gAlbedoSpec, TexCoords);
    vec3 albedo = albedoSpec.rgb;
    float specular = albedoSpec.a;
    
    // Material properties (metallic, roughness, AO, emissive)
    vec4 material = texture(gMaterial, TexCoords);
    float metallic = material.r;
    float roughness = max(material.g, 0.05); // Prevent completely smooth surfaces
    float ao = material.b;
    float emissive = material.a; // Emissive strength
    
    // Early exit for skybox/background
    if(length(normal) < 0.1) {
        // Beautiful gradient sky with time-based variation
        float height = TexCoords.y;
        vec3 skyTop = vec3(0.4, 0.7, 1.0);
        vec3 skyBottom = vec3(0.8, 0.9, 1.0);
        vec3 skyColor = mix(skyBottom, skyTop, height);
        
        // Add subtle time-based variation for dynamic sky
        skyColor += 0.1 * sin(time * 0.1) * vec3(0.1, 0.05, 0.0);
        
        // Add clouds using noise
        float cloudNoise = sin(TexCoords.x * 10.0 + time * 0.5) * sin(TexCoords.y * 8.0 + time * 0.3);
        skyColor += cloudNoise * 0.05 * vec3(1.0, 1.0, 1.0);
        
        FragColor = vec4(skyColor, 1.0);
        return;
    }
    
    vec3 viewDir = normalize(viewPos - worldPos);
    
    // Calculate PBR lighting with all enhancements
    vec3 color = calculateLighting(worldPos, normal, albedo, metallic, roughness, emissive, viewDir);
    
    // Apply ambient occlusion
    color *= ao;
    
    // Apply atmospheric fog
    float distance = length(viewPos - worldPos);
    color = applyFog(color, distance * 0.01);
    
    // Advanced tone mapping
    color = aces(color);
    
    // Gamma correction
    color = pow(color, vec3(1.0/2.2));
    
    // Add subtle film grain for cinematic feel
    float grain = (fract(sin(dot(TexCoords, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.02;
    color += grain;
    
    // Color grading for enhanced visuals
    color = mix(color, color * vec3(1.02, 1.0, 0.98), 0.1); // Slight warm tint
    
    FragColor = vec4(color, 1.0);
} 