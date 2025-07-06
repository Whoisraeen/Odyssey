#version 330 core

in vec2 TexCoords;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform vec3 spriteColor;
uniform bool useTexture;

void main() {
    if (useTexture) {
        FragColor = texture(uTexture, TexCoords) * vec4(spriteColor, 1.0);
    } else {
        FragColor = vec4(spriteColor, 1.0);
    }
}