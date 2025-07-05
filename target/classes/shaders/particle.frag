#version 330 core

in vec3 particleColor;
out vec4 FragColor;

void main() {
    FragColor = vec4(particleColor, 0.7); // Use the color passed from the vertex shader
} 