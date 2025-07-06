#version 450 core

in vec2 TexCoords;

out vec4 FragColor;

uniform sampler2D finalImage;

void main() {
    // Simple passthrough - just sample the final processed image
    FragColor = texture(finalImage, TexCoords);
}