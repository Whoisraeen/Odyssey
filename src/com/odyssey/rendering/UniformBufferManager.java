package com.odyssey.rendering;

import com.odyssey.rendering.scene.Camera;
import com.odyssey.rendering.scene.Light;
import com.odyssey.rendering.scene.RenderObject;

import java.util.List;

public class UniformBufferManager {
    // Manages uniform buffers
    public void createCameraUniforms() {}
    public void createLightUniforms() {}
    public void createMaterialUniforms() {}
    public void createTimeUniforms() {}
    
    public void bindCameraUniforms(Camera camera) {}
    public void bindMaterialUniforms() {}
    public void bindObjectUniforms(RenderObject obj) {}
    
    public void updateCameraUniforms(Camera camera) {}
    public void updateLightUniforms(List<Light> lights) {}
    public void updateTimeUniforms(float deltaTime) {}
    
    public void cleanup() {}
} 