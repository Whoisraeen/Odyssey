#version 450 core

// Vertex Attributes
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

// Outputs to Fragment Shader
out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;

// Uniforms
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    // Transform vertex position to clip space
    gl_Position = projection * view * model * vec4(aPos, 1.0);
    
    // Pass position and normal in world space to the fragment shader
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(model))) * aNormal; // For non-uniform scaling
    TexCoords = aTexCoords;
} 