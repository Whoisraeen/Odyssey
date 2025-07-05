package com.odyssey.rendering.scene;

import java.util.List;
import java.util.ArrayList;
import org.joml.Vector3f;

public class Scene {
    private List<RenderObject> objects;
    private List<Light> lights;
    
    public Scene() {
        objects = new ArrayList<>();
        lights = new ArrayList<>();
        
        // Initialize with default lighting setup
        setupDefaultLighting();
        
        System.out.println("Scene initialized with default lighting");
    }
    
    public void setupDefaultLighting() {
        // Add a primary directional light (sun)
        Light sunLight = Light.createDirectionalLight(
            new Vector3f(0.3f, -0.7f, 0.2f),  // direction
            new Vector3f(1.0f, 0.9f, 0.8f),   // warm sunlight color
            2.0f                               // intensity
        );
        sunLight.setCastsShadows(true);
        lights.add(sunLight);
        
        // Add ambient/fill light
        Light ambientLight = Light.createDirectionalLight(
            new Vector3f(-0.2f, 0.5f, -0.3f), // opposite direction
            new Vector3f(0.4f, 0.5f, 0.7f),   // cool blue fill light
            0.3f                               // lower intensity
        );
        lights.add(ambientLight);
        
        // Add a point light for local illumination
        Light pointLight = Light.createPointLight(
            new Vector3f(0.0f, 10.0f, 0.0f),  // position above origin
            new Vector3f(1.0f, 0.8f, 0.6f),   // warm white
            1.5f,                              // intensity
            25.0f                              // radius
        );
        lights.add(pointLight);
    }
    
    public void addObject(RenderObject object) {
        objects.add(object);
    }
    
    public void removeObject(RenderObject object) {
        objects.remove(object);
    }
    
    public void addLight(Light light) {
        lights.add(light);
    }
    
    public void removeLight(Light light) {
        lights.remove(light);
    }
    
    // Scene graph and management
    public List<RenderObject> getOpaqueObjects() {
        return objects.stream()
            .filter(obj -> !obj.isTransparent())
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<RenderObject> getTransparentObjects() {
        return objects.stream()
            .filter(RenderObject::isTransparent)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<Light> getLights() {
        return lights;
    }
    
    public List<RenderObject> getObjects() {
        return objects;
    }
    
    public void update(float deltaTime) {
        // Update dynamic objects and lights
        for (RenderObject object : objects) {
            // Update object animations, transformations, etc.
        }
        
        // Update light animations (day/night cycle, flickering, etc.)
        updateLighting(deltaTime);
    }
    
    private void updateLighting(float deltaTime) {
        // Example: Animate sun light for day/night cycle
        // This could be expanded to create dynamic lighting effects
        
        // Find the sun light and potentially rotate it over time
        for (Light light : lights) {
            if (light.getLightType() == Light.LightType.DIRECTIONAL && light.castsShadows()) {
                // This is likely our sun light - could animate its direction here
                // For now, keep it static
                break;
            }
        }
    }
    
    public void clear() {
        objects.clear();
        lights.clear();
    }
    
    public void cleanup() {
        // Clean up any resources held by objects and lights
        for (RenderObject object : objects) {
            if (object.getMesh() != null) {
                object.getMesh().cleanup();
            }
        }
        clear();
    }
}