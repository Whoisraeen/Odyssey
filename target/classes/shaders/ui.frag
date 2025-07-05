#version 330 core

in vec2 TexCoords;
out vec4 FragColor;

uniform sampler2D image;
uniform vec3 spriteColor;

void main() {
    FragColor = texture(image, TexCoords) * vec4(spriteColor, 1.0);
} 