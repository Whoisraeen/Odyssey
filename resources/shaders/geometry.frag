#version 450 core

// Inputs from Vertex Shader
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

// G-Buffer Outputs
layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec4 gAlbedoSpec;

// For now, we'll output a simple color.
// Later, this will sample from a material's texture.
// uniform sampler2D texture_diffuse;

void main() {
    // Store the fragment's world-space position
    gPosition = FragPos;
    
    // Store the fragment's world-space normal
    gNormal = normalize(Normal);
    
    // Store albedo (color) data. We'll use a placeholder color.
    // In the future, this would be `texture(texture_diffuse, TexCoords).rgb`.
    gAlbedoSpec.rgb = vec3(0.9, 0.9, 0.9);
    
    // Store specular intensity in the alpha component.
    gAlbedoSpec.a = 1.0; 
} 