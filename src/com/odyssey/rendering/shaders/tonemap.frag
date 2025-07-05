#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D hdrBuffer;
uniform sampler2D bloomBlur;
uniform sampler2D volumetric;
uniform sampler2D cloudTexture;
uniform sampler2D depthTexture;

uniform float exposure;
uniform bool enableBloom;
uniform float u_lightningFlash;

// Tone mapping functions
vec3 reinhardToneMapping(vec3 color) {
    return color / (color + vec3(1.0));
}

vec3 exposureToneMapping(vec3 color, float exposure) {
    return vec3(1.0) - exp(-color * exposure);
}

vec3 acesToneMapping(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

vec3 uncharted2ToneMapping(vec3 color) {
    const float A = 0.15;
    const float B = 0.50;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.02;
    const float F = 0.30;
    const float W = 11.2;
    
    color = ((color * (A * color + C * B) + D * E) / (color * (A * color + B) + D * F)) - E / F;
    float white = ((W * (A * W + C * B) + D * E) / (W * (A * W + B) + D * F)) - E / F;
    return color / white;
}

void main() {
    float depth = texture(depthTexture, TexCoord).r;
    vec3 finalColor;
    
    // If depth is 1.0, it's the skybox. Draw clouds.
    if (depth >= 1.0) {
        vec3 cloudColor = texture(cloudTexture, TexCoord).rgb;
        vec3 volumetricColor = texture(volumetric, TexCoord).rgb;
        finalColor = cloudColor + volumetricColor;
    } else {
        // It's the main scene
        const float gamma = 2.2;
        vec3 hdrColor = texture(hdrBuffer, TexCoord).rgb;
        
        // Add bloom
        if (enableBloom) {
            hdrColor += texture(bloomBlur, TexCoord).rgb;
        }

        // Add volumetric lighting
        hdrColor += texture(volumetric, TexCoord).rgb;
        
        // Exposure tone mapping
        vec3 mapped = vec3(1.0) - exp(-hdrColor * exposure);
        // Gamma correction 
        finalColor = pow(mapped, vec3(1.0 / gamma));
    }

    // Apply lightning flash over everything
    finalColor += u_lightningFlash * vec3(0.8, 0.8, 1.0);
    
    FragColor = vec4(finalColor, 1.0);
}