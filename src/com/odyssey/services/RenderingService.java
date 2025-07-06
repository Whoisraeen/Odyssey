package com.odyssey.services;

import com.odyssey.rendering.Camera;
import com.odyssey.rendering.scene.Scene;
import org.joml.Vector3i;

/**
 * Service interface for rendering operations
 */
public interface RenderingService {
    
    /**
     * Initialize rendering system
     */
    void initialize(int width, int height);
    
    /**
     * Render the current frame
     */
    void render(Camera camera, Scene scene, float deltaTime);
    
    /**
     * Update clear color based on environment
     */
    void updateClearColor();
    
    /**
     * Render selection box at specified position
     */
    void renderSelectionBox(Camera camera, Vector3i position);
    
    /**
     * Update scene with current chunks
     */
    void updateScene(Scene scene);
    
    /**
     * Set wetness level for rendering effects
     */
    void setWetness(float wetness);
    
    /**
     * Cleanup rendering resources
     */
    void cleanup();
}