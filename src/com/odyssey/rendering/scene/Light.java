package com.odyssey.rendering.scene;

import org.joml.Vector3f;

public class Light {
    public enum LightType {
        DIRECTIONAL(0),
        POINT(1),
        SPOT(2);
        
        private final int value;
        
        LightType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    private Vector3f position;
    private Vector3f direction;
    private Vector3f color;
    private float intensity;
    private float radius;
    private float innerCone;
    private float outerCone;
    private LightType type;
    private boolean castsShadows;
    
    // Constructors
    public Light(LightType type) {
        this.type = type;
        this.position = new Vector3f(0.0f, 0.0f, 0.0f);
        this.direction = new Vector3f(0.0f, -1.0f, 0.0f);
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
        this.intensity = 1.0f;
        this.radius = 10.0f;
        this.innerCone = 12.5f;
        this.outerCone = 17.5f;
        this.castsShadows = true;
    }
    
    // Static factory methods
    public static Light createDirectionalLight(Vector3f direction, Vector3f color, float intensity) {
        Light light = new Light(LightType.DIRECTIONAL);
        light.setDirection(direction);
        light.setColor(color);
        light.setIntensity(intensity);
        return light;
    }
    
    public static Light createPointLight(Vector3f position, Vector3f color, float intensity, float radius) {
        Light light = new Light(LightType.POINT);
        light.setPosition(position);
        light.setColor(color);
        light.setIntensity(intensity);
        light.setRadius(radius);
        return light;
    }
    
    public static Light createSpotLight(Vector3f position, Vector3f direction, Vector3f color, 
                                       float intensity, float radius, float innerCone, float outerCone) {
        Light light = new Light(LightType.SPOT);
        light.setPosition(position);
        light.setDirection(direction);
        light.setColor(color);
        light.setIntensity(intensity);
        light.setRadius(radius);
        light.setInnerCone(innerCone);
        light.setOuterCone(outerCone);
        return light;
    }
    
    // Getters and Setters
    public Vector3f getPosition() {
        return position;
    }
    
    public void setPosition(Vector3f position) {
        this.position = position;
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
    
    public Vector3f getDirection() {
        return direction;
    }
    
    public void setDirection(Vector3f direction) {
        this.direction = direction.normalize();
    }
    
    public void setDirection(float x, float y, float z) {
        this.direction.set(x, y, z).normalize();
    }
    
    public Vector3f getColor() {
        return color;
    }
    
    public void setColor(Vector3f color) {
        this.color = color;
    }
    
    public void setColor(float r, float g, float b) {
        this.color.set(r, g, b);
    }
    
    public float getIntensity() {
        return intensity;
    }
    
    public void setIntensity(float intensity) {
        this.intensity = Math.max(0.0f, intensity);
    }
    
    public float getRadius() {
        return radius;
    }
    
    public void setRadius(float radius) {
        this.radius = Math.max(0.1f, radius);
    }
    
    public float getInnerCone() {
        return innerCone;
    }
    
    public void setInnerCone(float innerCone) {
        this.innerCone = Math.max(0.0f, Math.min(innerCone, outerCone));
    }
    
    public float getOuterCone() {
        return outerCone;
    }
    
    public void setOuterCone(float outerCone) {
        this.outerCone = Math.max(innerCone, outerCone);
    }
    
    public LightType getLightType() {
        return type;
    }
    
    public int getType() {
        return type.getValue();
    }
    
    public void setType(LightType type) {
        this.type = type;
    }
    
    public boolean castsShadows() {
        return castsShadows;
    }
    
    public void setCastsShadows(boolean castsShadows) {
        this.castsShadows = castsShadows;
    }
    
    // Utility methods
    public float getAttenuationAt(float distance) {
        if (type == LightType.DIRECTIONAL) {
            return 1.0f; // No attenuation for directional lights
        }
        
        if (distance >= radius) {
            return 0.0f;
        }
        
        // Quadratic attenuation
        float attenuation = 1.0f / (1.0f + 0.09f * distance + 0.032f * distance * distance);
        
        // Smooth falloff near radius
        float falloff = Math.max(0.0f, (radius - distance) / radius);
        return attenuation * falloff * falloff;
    }
    
    public float getSpotlightFactor(Vector3f fragmentDirection) {
        if (type != LightType.SPOT) {
            return 1.0f;
        }
        
        float theta = (float) Math.acos(Math.max(0.0f, direction.dot(fragmentDirection)));
        float epsilon = (float) (Math.cos(Math.toRadians(innerCone)) - Math.cos(Math.toRadians(outerCone)));
        float intensity = (float) Math.max(0.0f, 
            (Math.cos(Math.toRadians(outerCone)) - Math.cos(theta)) / epsilon);
        
        return Math.min(1.0f, intensity);
    }
    
    @Override
    public String toString() {
        return String.format("Light{type=%s, pos=(%.2f,%.2f,%.2f), color=(%.2f,%.2f,%.2f), intensity=%.2f}",
                type, position.x, position.y, position.z, color.x, color.y, color.z, intensity);
    }
}