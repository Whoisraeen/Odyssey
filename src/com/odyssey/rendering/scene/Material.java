package com.odyssey.rendering.scene;

import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Material {
    // PBR Material properties
    private Vector3f albedo;
    private float metallic;
    private float roughness;
    private float ao; // Ambient occlusion
    private Vector3f emissive;
    private float emissiveStrength;
    
    // Texture IDs
    private int albedoTexture;
    private int normalTexture;
    private int metallicTexture;
    private int roughnessTexture;
    private int aoTexture;
    private int emissiveTexture;
    
    // Material flags
    private boolean hasAlbedoTexture;
    private boolean hasNormalTexture;
    private boolean hasMetallicTexture;
    private boolean hasRoughnessTexture;
    private boolean hasAoTexture;
    private boolean hasEmissiveTexture;
    private boolean isTransparent;
    private boolean doubleSided;
    
    // Alpha properties
    private float alpha;
    private float alphaCutoff;
    
    public Material() {
        // Default PBR values
        this.albedo = new Vector3f(0.8f, 0.8f, 0.8f);
        this.metallic = 0.0f;
        this.roughness = 0.5f;
        this.ao = 1.0f;
        this.emissive = new Vector3f(0.0f);
        this.emissiveStrength = 1.0f;
        this.alpha = 1.0f;
        this.alphaCutoff = 0.5f;
        
        // Initialize texture flags
        this.hasAlbedoTexture = false;
        this.hasNormalTexture = false;
        this.hasMetallicTexture = false;
        this.hasRoughnessTexture = false;
        this.hasAoTexture = false;
        this.hasEmissiveTexture = false;
        this.isTransparent = false;
        this.doubleSided = false;
        
        // Default texture IDs (0 = no texture)
        this.albedoTexture = 0;
        this.normalTexture = 0;
        this.metallicTexture = 0;
        this.roughnessTexture = 0;
        this.aoTexture = 0;
        this.emissiveTexture = 0;
    }
    
    public Material(Vector3f albedo, float metallic, float roughness) {
        this();
        this.albedo.set(albedo);
        this.metallic = metallic;
        this.roughness = roughness;
    }
    
    public void bind() {
        // Bind textures to appropriate texture units
        if (hasAlbedoTexture) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, albedoTexture);
        }
        
        if (hasNormalTexture) {
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, normalTexture);
        }
        
        if (hasMetallicTexture) {
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, metallicTexture);
        }
        
        if (hasRoughnessTexture) {
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, roughnessTexture);
        }
        
        if (hasAoTexture) {
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, aoTexture);
        }
        
        if (hasEmissiveTexture) {
            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, emissiveTexture);
        }
        
        // Reset to texture unit 0
        glActiveTexture(GL_TEXTURE0);
    }
    
    public void unbind() {
        // Unbind all textures
        for (int i = 0; i < 6; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glActiveTexture(GL_TEXTURE0);
    }
    
    // Setters for material properties
    public void setAlbedo(Vector3f albedo) {
        this.albedo.set(albedo);
    }
    
    public void setAlbedo(float r, float g, float b) {
        this.albedo.set(r, g, b);
    }
    
    public void setMetallic(float metallic) {
        this.metallic = Math.max(0.0f, Math.min(1.0f, metallic));
    }
    
    public void setRoughness(float roughness) {
        this.roughness = Math.max(0.0f, Math.min(1.0f, roughness));
    }
    
    public void setAo(float ao) {
        this.ao = Math.max(0.0f, Math.min(1.0f, ao));
    }
    
    public void setEmissive(Vector3f emissive) {
        this.emissive.set(emissive);
    }
    
    public void setEmissive(float r, float g, float b) {
        this.emissive.set(r, g, b);
    }
    
    public void setEmissiveStrength(float strength) {
        this.emissiveStrength = Math.max(0.0f, strength);
    }
    
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.isTransparent = alpha < 1.0f;
    }
    
    public void setAlphaCutoff(float cutoff) {
        this.alphaCutoff = Math.max(0.0f, Math.min(1.0f, cutoff));
    }
    
    public void setTransparent(boolean transparent) {
        this.isTransparent = transparent;
    }
    
    public void setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }
    
    // Texture setters
    public void setAlbedoTexture(int textureId) {
        this.albedoTexture = textureId;
        this.hasAlbedoTexture = textureId != 0;
    }
    
    public void setNormalTexture(int textureId) {
        this.normalTexture = textureId;
        this.hasNormalTexture = textureId != 0;
    }
    
    public void setMetallicTexture(int textureId) {
        this.metallicTexture = textureId;
        this.hasMetallicTexture = textureId != 0;
    }
    
    public void setRoughnessTexture(int textureId) {
        this.roughnessTexture = textureId;
        this.hasRoughnessTexture = textureId != 0;
    }
    
    public void setAoTexture(int textureId) {
        this.aoTexture = textureId;
        this.hasAoTexture = textureId != 0;
    }
    
    public void setEmissiveTexture(int textureId) {
        this.emissiveTexture = textureId;
        this.hasEmissiveTexture = textureId != 0;
    }
    
    // Getters
    public Vector3f getAlbedo() { return new Vector3f(albedo); }
    public float getMetallic() { return metallic; }
    public float getRoughness() { return roughness; }
    public float getAo() { return ao; }
    public Vector3f getEmissive() { return new Vector3f(emissive); }
    public float getEmissiveStrength() { return emissiveStrength; }
    public float getAlpha() { return alpha; }
    public float getAlphaCutoff() { return alphaCutoff; }
    public boolean isTransparent() { return isTransparent; }
    public boolean isDoubleSided() { return doubleSided; }
    
    // Texture getters
    public int getAlbedoTexture() { return albedoTexture; }
    public int getNormalTexture() { return normalTexture; }
    public int getMetallicTexture() { return metallicTexture; }
    public int getRoughnessTexture() { return roughnessTexture; }
    public int getAoTexture() { return aoTexture; }
    public int getEmissiveTexture() { return emissiveTexture; }
    
    // Texture flags
    public boolean hasAlbedoTexture() { return hasAlbedoTexture; }
    public boolean hasNormalTexture() { return hasNormalTexture; }
    public boolean hasMetallicTexture() { return hasMetallicTexture; }
    public boolean hasRoughnessTexture() { return hasRoughnessTexture; }
    public boolean hasAoTexture() { return hasAoTexture; }
    public boolean hasEmissiveTexture() { return hasEmissiveTexture; }
    
    // Utility methods
    public static Material createMetal(Vector3f albedo, float roughness) {
        Material material = new Material(albedo, 1.0f, roughness);
        return material;
    }
    
    public static Material createDielectric(Vector3f albedo, float roughness) {
        Material material = new Material(albedo, 0.0f, roughness);
        return material;
    }
    
    public static Material createEmissive(Vector3f color, float strength) {
        Material material = new Material();
        material.setEmissive(color);
        material.setEmissiveStrength(strength);
        return material;
    }
    
    public void cleanup() {
        // Note: We don't delete textures here as they might be shared
        // Texture cleanup should be handled by a texture manager
    }
}