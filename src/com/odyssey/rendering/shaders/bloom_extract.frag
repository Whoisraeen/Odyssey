#version 330 core

out vec4 FragColor;

in vec2 TexCoord;

uniform sampler2D hdrBuffer;
uniform float threshold;
uniform float knee;

vec3 prefilter(vec3 color) {
    float brightness = max(color.r, max(color.g, color.b));
    float soft = brightness - threshold + knee;
    soft = clamp(soft, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee + 0.00001);
    
    float contribution = max(soft, brightness - threshold);
    contribution /= max(brightness, 0.00001);
    
    return color * contribution;
}

void main() {
    vec3 color = texture(hdrBuffer, TexCoord).rgb;
    color = prefilter(color);
    FragColor = vec4(color, 1.0);
}