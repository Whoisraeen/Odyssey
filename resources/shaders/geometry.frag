#version 330 core

// Inputs from Vertex Shader
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

// G-Buffer Outputs
layout (location = 0) out vec4 gPosition;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gAlbedoSpec;
layout (location = 3) out vec4 gMaterial; // x: metallic, y: roughness, z: ao, w: emissive

// For now, we'll output a simple color.
// Later, this will sample from a material's texture.
uniform sampler2D texture_diffuse1;
uniform float u_wetness; // 0.0 = dry, 1.0 = fully wet

// Seasonal uniforms
uniform int u_season; // 0: Spring, 1: Summer, 2: Autumn, 3: Winter
uniform float u_seasonTransition;

// Define base seasonal colors
const vec3 springColor = vec3(0.7, 0.9, 0.4);
const vec3 summerColor = vec3(0.4, 0.8, 0.3);
const vec3 autumnColor = vec3(0.9, 0.6, 0.2);
const vec3 winterColor = vec3(0.85, 0.85, 0.9); // Pale, slightly bluish for frost

const vec3 seasonColors[4] = vec3[](
    springColor,
    summerColor,
    autumnColor,
    winterColor
);

void main() {
    // Store the fragment's world-space position
    gPosition = vec4(FragPos, 1.0);
    
    // Store the fragment's world-space normal
    gNormal = vec4(normalize(Normal), 1.0);
    
    vec4 texColor = texture(texture_diffuse1, TexCoords);
    if(texColor.a < 0.1)
        discard;

    vec3 finalColor = texColor.rgb;

    // --- Seasonal Foliage Color Change ---
    // Identify foliage by its green color and some texture characteristics.
    // This is a heuristic and might need tuning based on the texture atlas.
    bool isFoliage = (texColor.g > texColor.r && texColor.g > texColor.b && texColor.g > 0.3);

    if (isFoliage) {
        // Determine current and next season's color
        vec3 currentColor = seasonColors[u_season];
        vec3 nextColor = seasonColors[(u_season + 1) % 4];

        // Interpolate between the two colors based on the transition
        vec3 seasonalColor = mix(currentColor, nextColor, u_seasonTransition);

        // Mix the original texture color with the seasonal tint.
        // This preserves some of the original texture detail.
        finalColor = mix(texColor.rgb, seasonalColor, 0.6);
    }
    
    // --- Wetness Effect ---
    // Darken the color based on wetness, but less so for very bright parts
    float wetnessFactor = 1.0 - (u_wetness * 0.4);
    finalColor *= wetnessFactor;

    // Set Albedo and Specular
    gAlbedoSpec.rgb = finalColor;
    gAlbedoSpec.a = 0.3; // Specular strength, increase for wet effect
    
    // If it's wet, increase specular highlight
    if (u_wetness > 0.1) {
        gAlbedoSpec.a = mix(0.3, 0.8, u_wetness);
    }

    // Material properties for PBR
    float metallic = 0.0;  // Most voxel materials are non-metallic
    float roughness = 0.8; // Default roughness for most surfaces
    float ao = 1.0;        // Default ambient occlusion
    float emissive = 0.0;  // Default no emission
    
    if (isFoliage) {
        // Foliage materials
        metallic = 0.0;
        roughness = 0.9;   // Leaves are quite rough
        ao = 0.8;          // Slight self-shadowing
        emissive = 0.0;
    } else {
        // Determine material type based on color
        float avgColor = (finalColor.r + finalColor.g + finalColor.b) / 3.0;
        
        // Detect emissive materials by color characteristics
        if (finalColor.r > 0.8 && finalColor.g > 0.3 && finalColor.b < 0.3) {
            // Reddish glowing materials (redstone, lava)
            emissive = 0.8;
            roughness = 0.3;
        } else if (finalColor.r > 0.9 && finalColor.g > 0.7 && finalColor.b > 0.3) {
            // Yellowish glowing materials (torches, glowstone)
            emissive = 1.0;
            roughness = 0.4;
        } else if (avgColor < 0.3) {
            // Dark materials (stone, dirt)
            roughness = 0.95;
            ao = 0.9;
        } else if (avgColor > 0.7) {
            // Light materials (sand, snow)
            roughness = 0.7;
            ao = 1.0;
        }
    }
    
    // Wetness affects material properties
    if (u_wetness > 0.1) {
        roughness = mix(roughness, 0.1, u_wetness); // Wet surfaces are smoother
        metallic = mix(metallic, 0.1, u_wetness * 0.5); // Slight metallic look when wet
        emissive *= (1.0 - u_wetness * 0.3); // Wetness reduces emission slightly
    }

    gMaterial = vec4(metallic, roughness, ao, emissive);
} 