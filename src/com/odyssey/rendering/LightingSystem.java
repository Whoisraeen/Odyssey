package com.odyssey.rendering;

import com.odyssey.rendering.lighting.ShadowMapping;
import com.odyssey.rendering.scene.Camera;
import com.odyssey.rendering.scene.Light;

import java.util.List;

public class LightingSystem {
    // Lighting system implementation
    public void renderLighting(GBuffer gBuffer, Camera camera, List<Light> lights, ShadowMapping shadowMapping, int ssaoTexture) {}
    public int getColorTexture() { return 0; }
    public void bindLightingTextures() {}
    public void cleanup() {}
} 