#version 330 core

in vec2 TexCoord;

uniform sampler2D depthTexture;
uniform sampler2D shadowMap;
uniform vec3 lightPos;
uniform vec3 lightColor;
uniform mat4 view;
uniform mat4 projection;

out vec4 FragColor;

void main() {
    // Simple volumetric lighting effect
    float depth = texture(depthTexture, TexCoord).r;
    
    // Basic fog/volumetric effect
    float fogDensity = 0.02;
    float fogFactor = exp(-depth * fogDensity);
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    
    vec3 volumetricColor = lightColor * 0.1 * (1.0 - fogFactor);
    
    FragColor = vec4(volumetricColor, 1.0);
}