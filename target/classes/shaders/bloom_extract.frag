#version 450 core

in vec2 TexCoord;

out vec4 FragColor;

uniform sampler2D hdrBuffer;
uniform float threshold;

void main() {
    vec3 color = texture(hdrBuffer, TexCoord).rgb;
    
    // Calculate luminance
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    
    // Extract bright areas
    if (luminance > threshold) {
        FragColor = vec4(color, 1.0);
    } else {
        FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}