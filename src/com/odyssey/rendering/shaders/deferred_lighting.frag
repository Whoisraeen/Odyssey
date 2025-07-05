#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

// G-Buffer textures
uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gAlbedo;
uniform sampler2D gMaterial; // metallic, roughness, ao, unused
uniform sampler2D ssaoTexture;

// Camera
uniform vec3 viewPos;

// Lighting
#define MAX_LIGHTS 32
uniform int numLights;
uniform vec3 lightPositions[MAX_LIGHTS];
uniform vec3 lightColors[MAX_LIGHTS];
uniform float lightIntensities[MAX_LIGHTS];
uniform float lightRadii[MAX_LIGHTS];
uniform int lightTypes[MAX_LIGHTS]; // 0=directional, 1=point, 2=spot

// Constants
const float PI = 3.14159265359;

// PBR functions
vec3 getNormalFromMap(vec3 normal) {
    return normalize(normal * 2.0 - 1.0);
}

float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    
    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    
    return num / denom;
}

float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    
    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    
    return num / denom;
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);
    
    return ggx1 * ggx2;
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

vec3 calculateDirectionalLight(vec3 lightDir, vec3 lightColor, float intensity,
                              vec3 albedo, vec3 normal, vec3 viewDir,
                              float metallic, float roughness, vec3 F0) {
    vec3 L = normalize(-lightDir);
    vec3 H = normalize(viewDir + L);
    
    // Calculate radiance
    vec3 radiance = lightColor * intensity;
    
    // Cook-Torrance BRDF
    float NDF = DistributionGGX(normal, H, roughness);
    float G = GeometrySmith(normal, viewDir, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, viewDir), 0.0), F0);
    
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= 1.0 - metallic;
    
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(normal, viewDir), 0.0) * max(dot(normal, L), 0.0) + 0.0001;
    vec3 specular = numerator / denominator;
    
    float NdotL = max(dot(normal, L), 0.0);
    return (kD * albedo / PI + specular) * radiance * NdotL;
}

vec3 calculatePointLight(vec3 lightPos, vec3 lightColor, float intensity, float radius,
                        vec3 fragPos, vec3 albedo, vec3 normal, vec3 viewDir,
                        float metallic, float roughness, vec3 F0) {
    vec3 L = normalize(lightPos - fragPos);
    vec3 H = normalize(viewDir + L);
    
    // Calculate attenuation
    float distance = length(lightPos - fragPos);
    float attenuation = 1.0 / (1.0 + 0.09 * distance + 0.032 * distance * distance);
    
    // Apply radius-based falloff
    if (distance > radius) {
        attenuation *= max(0.0, 1.0 - (distance - radius) / radius);
    }
    
    vec3 radiance = lightColor * intensity * attenuation;
    
    // Cook-Torrance BRDF
    float NDF = DistributionGGX(normal, H, roughness);
    float G = GeometrySmith(normal, viewDir, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, viewDir), 0.0), F0);
    
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= 1.0 - metallic;
    
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(normal, viewDir), 0.0) * max(dot(normal, L), 0.0) + 0.0001;
    vec3 specular = numerator / denominator;
    
    float NdotL = max(dot(normal, L), 0.0);
    return (kD * albedo / PI + specular) * radiance * NdotL;
}

void main() {
    // Sample G-Buffer
    vec3 fragPos = texture(gPosition, TexCoord).rgb;
    vec3 normal = getNormalFromMap(texture(gNormal, TexCoord).rgb);
    vec3 albedo = texture(gAlbedo, TexCoord).rgb;
    vec3 material = texture(gMaterial, TexCoord).rgb;
    float ssao = texture(ssaoTexture, TexCoord).r;
    
    float metallic = material.r;
    float roughness = material.g;
    float ao = material.b * ssao; // Combine material AO with SSAO
    
    // Calculate view direction
    vec3 viewDir = normalize(viewPos - fragPos);
    
    // Calculate F0 (surface reflection at zero incidence)
    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo, metallic);
    
    // Lighting calculation
    vec3 Lo = vec3(0.0);
    
    for (int i = 0; i < numLights && i < MAX_LIGHTS; ++i) {
        if (lightTypes[i] == 0) { // Directional light
            Lo += calculateDirectionalLight(lightPositions[i], lightColors[i], lightIntensities[i],
                                          albedo, normal, viewDir, metallic, roughness, F0);
        } else if (lightTypes[i] == 1) { // Point light
            Lo += calculatePointLight(lightPositions[i], lightColors[i], lightIntensities[i], lightRadii[i],
                                    fragPos, albedo, normal, viewDir, metallic, roughness, F0);
        }
        // TODO: Add spot light support
    }
    
    // Ambient lighting
    vec3 ambient = vec3(0.03) * albedo * ao;
    vec3 color = ambient + Lo;
    
    // HDR tonemapping (simple Reinhard)
    // color = color / (color + vec3(1.0));
    
    // Gamma correction will be done in post-processing
    FragColor = vec4(color, 1.0);
}