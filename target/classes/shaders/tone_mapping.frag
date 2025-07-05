#version 450 core

in vec2 TexCoord;

out vec4 FragColor;

uniform sampler2D hdrBuffer;
uniform sampler2D bloomBlur;
uniform sampler2D volumetric;
uniform sampler2D cloudTexture;
uniform sampler2D depthTexture;

uniform float exposure;
uniform float u_lightningFlash;

vec3 toneMapping(vec3 color) {
    // Reinhard tone mapping
    return color / (color + vec3(1.0));
}

void main() {
    vec3 hdrColor = texture(hdrBuffer, TexCoord).rgb;
    vec3 bloomColor = texture(bloomBlur, TexCoord).rgb;
    vec3 volumetricColor = texture(volumetric, TexCoord).rgb;
    vec3 cloudColor = texture(cloudTexture, TexCoord).rgb;
    
    // Combine all effects
    vec3 finalColor = hdrColor + bloomColor * 0.04 + volumetricColor + cloudColor;
    
    // Apply exposure
    finalColor *= exposure;
    
    // Add lightning flash
    finalColor += vec3(u_lightningFlash);
    
    // Tone mapping
    finalColor = toneMapping(finalColor);
    
    // Gamma correction
    finalColor = pow(finalColor, vec3(1.0/2.2));
    
    FragColor = vec4(finalColor, 1.0);
}