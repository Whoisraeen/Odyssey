#version 330 core

out float FragColor;

in vec2 TexCoord;

uniform sampler2D ssaoInput;
uniform vec2 texelSize;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(ssaoInput, 0));
    float result = 0.0;
    
    // Simple 4x4 blur
    for (int x = -2; x < 2; ++x) {
        for (int y = -2; y < 2; ++y) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            result += texture(ssaoInput, TexCoord + offset).r;
        }
    }
    
    FragColor = result / (4.0 * 4.0);
}