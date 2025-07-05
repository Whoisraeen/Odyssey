#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D depthTexture;
uniform mat4 invViewProj;
uniform vec3 cameraPos;
uniform vec3 lightDir;
uniform vec3 lightColor;
uniform float lightIntensity;

// Volumetric parameters
uniform float density;
uniform float scattering;
uniform float absorption;
uniform int numSamples;
uniform float maxDistance;

// Noise for dithering
uniform sampler2D noiseTexture;
uniform float time;

vec3 worldPosFromDepth(float depth, vec2 texCoord) {
    vec4 clipSpacePosition = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 worldSpacePosition = invViewProj * clipSpacePosition;
    return worldSpacePosition.xyz / worldSpacePosition.w;
}

float phaseFunction(float cosTheta) {
    // Henyey-Greenstein phase function
    const float g = 0.76; // anisotropy factor
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * 3.14159265 * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
}

float noise3D(vec3 p) {
    // Simple 3D noise function
    return fract(sin(dot(p, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
}

void main() {
    float depth = texture(depthTexture, TexCoord).r;
    vec3 worldPos = worldPosFromDepth(depth, TexCoord);
    
    vec3 rayStart = cameraPos;
    vec3 rayEnd = worldPos;
    vec3 rayDir = normalize(rayEnd - rayStart);
    float rayLength = min(length(rayEnd - rayStart), maxDistance);
    
    // Add noise for dithering to reduce banding
    float noise = texture(noiseTexture, TexCoord * 4.0 + time * 0.1).r;
    float stepSize = rayLength / float(numSamples);
    float offset = noise * stepSize;
    
    vec3 scatteredLight = vec3(0.0);
    float transmittance = 1.0;
    
    // Ray marching
    for (int i = 0; i < numSamples; ++i) {
        float t = (float(i) + 0.5) * stepSize + offset;
        if (t >= rayLength) break;
        
        vec3 samplePos = rayStart + rayDir * t;
        
        // Sample density (could be from a 3D texture or procedural)
        float sampleDensity = density * (1.0 + 0.3 * noise3D(samplePos * 0.01 + time * 0.02));
        
        // Calculate lighting
        float cosTheta = dot(rayDir, -lightDir);
        float phase = phaseFunction(cosTheta);
        
        // Simple exponential falloff for atmospheric density
        float heightFactor = exp(-max(0.0, samplePos.y - cameraPos.y) * 0.0001);
        sampleDensity *= heightFactor;
        
        // In-scattering
        vec3 lightContribution = lightColor * lightIntensity * phase * scattering * sampleDensity;
        scatteredLight += lightContribution * transmittance * stepSize;
        
        // Out-scattering and absorption
        float extinction = (scattering + absorption) * sampleDensity;
        transmittance *= exp(-extinction * stepSize);
        
        // Early termination if transmittance is very low
        if (transmittance < 0.01) break;
    }
    
    FragColor = vec4(scatteredLight, 1.0 - transmittance);
}