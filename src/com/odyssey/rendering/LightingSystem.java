package com.odyssey.rendering;

import com.odyssey.rendering.lighting.ShadowMapping;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.scene.Light;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import static org.lwjgl.opengl.GL45.*;

public class LightingSystem {
    private int lightingFBO;
    private int colorTexture;
    private int deferredLightingShader;
    private int quadVAO, quadVBO;
    private int width, height;
    
    // PBR parameters
    private static final float PI = 3.14159265359f;
    
    public LightingSystem() {
        setupQuad();
    }
    
    public void setDeferredLightingShader(int shaderProgram) {
        this.deferredLightingShader = shaderProgram;
    }
    
    public void initialize(int width, int height) {
        this.width = width;
        this.height = height;
        setupFramebuffer();
    }
    
    private void setupFramebuffer() {
        // Create lighting framebuffer
        lightingFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, lightingFBO);
        
        // Create color texture for final lit result
        colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Lighting framebuffer is not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void setupQuad() {
        float[] quadVertices = {
            -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
             1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
             1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        };
        
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        
        glBindVertexArray(0);
    }
    
    public void renderLighting(GBuffer gBuffer, Camera camera, List<Light> lights, ShadowMapping shadowMapping, int ssaoTexture) {
        if (lightingFBO == 0) {
            initialize(1920, 1080); // Default size
        }
        
        System.out.println("Lighting pass: Starting deferred lighting with " + lights.size() + " lights");
        
        glBindFramebuffer(GL_FRAMEBUFFER, lightingFBO);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(deferredLightingShader);
        
        // Bind G-buffer textures
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getPositionTexture());
        glUniform1i(glGetUniformLocation(deferredLightingShader, "gPosition"), 0);
        
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getNormalTexture());
        glUniform1i(glGetUniformLocation(deferredLightingShader, "gNormal"), 1);
        
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getAlbedoTexture());
        glUniform1i(glGetUniformLocation(deferredLightingShader, "gAlbedoSpec"), 2);
        
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getMaterialTexture());
        glUniform1i(glGetUniformLocation(deferredLightingShader, "gMaterial"), 3);
        
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, ssaoTexture);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "ssaoTexture"), 4);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableSSAO"), ssaoTexture != 0 ? 1 : 0);
        
        // Bind shadow map
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, shadowMapping.getShadowMapTexture());
        glUniform1i(glGetUniformLocation(deferredLightingShader, "shadowMap"), 5);
        
        // Set shadow mapping matrix
        Matrix4f lightSpaceMatrix = shadowMapping.getLightSpaceMatrix();
        glUniformMatrix4fv(glGetUniformLocation(deferredLightingShader, "lightSpaceMatrix"), 
                          false, lightSpaceMatrix.get(new float[16]));
        
        // Set camera position for specular calculations
        Vector3f cameraPos = camera.getPosition();
        glUniform3f(glGetUniformLocation(deferredLightingShader, "viewPos"), 
                   cameraPos.x, cameraPos.y, cameraPos.z);
        
        // Set environment uniforms for enhanced shader
        glUniform1f(glGetUniformLocation(deferredLightingShader, "time"), System.currentTimeMillis() / 1000.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "timeOfDay"), 0.5f); // Noon
        glUniform1i(glGetUniformLocation(deferredLightingShader, "season"), 1); // Summer
        glUniform1f(glGetUniformLocation(deferredLightingShader, "seasonTransition"), 0.5f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "temperature"), 20.0f);
        
        // Fog settings
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableFog"), 1);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "fogType"), 1); // Exponential
        glUniform3f(glGetUniformLocation(deferredLightingShader, "fogColor"), 0.7f, 0.8f, 0.9f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "fogDensity"), 0.02f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "fogStart"), 10.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "fogEnd"), 100.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "fogHeightFalloff"), 0.1f);
        
        // Weather effects
        glUniform1f(glGetUniformLocation(deferredLightingShader, "weatherIntensity"), 0.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "rainIntensity"), 0.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "snowIntensity"), 0.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "windStrength"), 0.0f);
        
        // Atmosphere settings
        glUniform1f(glGetUniformLocation(deferredLightingShader, "atmosphereThickness"), 1.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "scatteringStrength"), 0.1f);
        
        // Quality and performance settings
        glUniform1i(glGetUniformLocation(deferredLightingShader, "lightingQuality"), 2); // High
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableAdvancedLighting"), 1);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableVolumetricLighting"), 1);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableScreenSpaceReflections"), 0);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "maxLightSamples"), 8);
        
        // Camera settings
        glUniform1f(glGetUniformLocation(deferredLightingShader, "nearPlane"), 0.1f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "farPlane"), 1000.0f);
        
        // Set ambient lighting parameters
        glUniform1f(glGetUniformLocation(deferredLightingShader, "ambientStrength"), 0.3f);
        glUniform3f(glGetUniformLocation(deferredLightingShader, "ambientColor"), 0.4f, 0.5f, 0.7f);
        glUniform3f(glGetUniformLocation(deferredLightingShader, "skyColor"), 0.5f, 0.7f, 1.0f);
        
        // Set post-processing parameters
        glUniform1f(glGetUniformLocation(deferredLightingShader, "exposure"), 2.0f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "gamma"), 2.2f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "brightness"), 1.2f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "contrast"), 1.1f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "saturation"), 1.0f);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "enableToneMapping"), 1);
        glUniform1i(glGetUniformLocation(deferredLightingShader, "toneMappingType"), 1); // Filmic tone mapping
        
        // Set light uniforms
        setLightUniforms(lights);
        
        // Render fullscreen quad
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void setLightUniforms(List<Light> lights) {
        // Find and set directional light (sun) separately
        Light directionalLight = null;
        List<Light> nonDirectionalLights = new ArrayList<>();
        
        for (Light light : lights) {
            if (light.getLightType() == Light.LightType.DIRECTIONAL) {
                if (directionalLight == null) {
                    directionalLight = light; // Use the first directional light as primary
                } else {
                    nonDirectionalLights.add(light); // Additional directional lights go to array
                }
            } else {
                nonDirectionalLights.add(light);
            }
        }
        
        // Set primary directional light (sun)
        if (directionalLight != null) {
            Vector3f dir = directionalLight.getDirection();
            glUniform3f(glGetUniformLocation(deferredLightingShader, "dirLight.direction"),
                       dir.x, dir.y, dir.z);
            
            Vector3f color = directionalLight.getColor();
            glUniform3f(glGetUniformLocation(deferredLightingShader, "dirLight.color"),
                       color.x, color.y, color.z);
            
            glUniform1f(glGetUniformLocation(deferredLightingShader, "dirLight.intensity"),
                       directionalLight.getIntensity());
            
            glUniform1f(glGetUniformLocation(deferredLightingShader, "dirLight.shadowBias"), 0.005f);
            
            glUniform1i(glGetUniformLocation(deferredLightingShader, "dirLight.castShadows"),
                       directionalLight.castsShadows() ? 1 : 0);
        } else {
            // No directional light - set defaults
            glUniform3f(glGetUniformLocation(deferredLightingShader, "dirLight.direction"), 0.0f, -1.0f, 0.0f);
            glUniform3f(glGetUniformLocation(deferredLightingShader, "dirLight.color"), 0.0f, 0.0f, 0.0f);
            glUniform1f(glGetUniformLocation(deferredLightingShader, "dirLight.intensity"), 0.0f);
            glUniform1f(glGetUniformLocation(deferredLightingShader, "dirLight.shadowBias"), 0.005f);
            glUniform1i(glGetUniformLocation(deferredLightingShader, "dirLight.castShadows"), 0);
        }
        
        // Set number of other lights
        int numLights = Math.min(nonDirectionalLights.size(), 8); // Limit to 8 lights to match MAX_LIGHTS
        glUniform1i(glGetUniformLocation(deferredLightingShader, "numLights"), numLights);
        
        // Set individual light properties for non-directional lights
        for (int i = 0; i < numLights; i++) {
            Light light = nonDirectionalLights.get(i);
            String baseName = "lights[" + i + "]";
            
            // Light position
            Vector3f pos = light.getPosition();
            glUniform3f(glGetUniformLocation(deferredLightingShader, baseName + ".position"),
                       pos.x, pos.y, pos.z);
            
            // Light direction
            Vector3f dir = light.getDirection();
            glUniform3f(glGetUniformLocation(deferredLightingShader, baseName + ".direction"),
                       dir.x, dir.y, dir.z);
            
            // Light color
            Vector3f color = light.getColor();
            glUniform3f(glGetUniformLocation(deferredLightingShader, baseName + ".color"),
                       color.x, color.y, color.z);
            
            // Light intensity and radius
            glUniform1f(glGetUniformLocation(deferredLightingShader, baseName + ".intensity"),
                       light.getIntensity());
            glUniform1f(glGetUniformLocation(deferredLightingShader, baseName + ".radius"),
                       light.getRadius());
            
            // Spot light properties
            glUniform1f(glGetUniformLocation(deferredLightingShader, baseName + ".innerCone"),
                       light.getInnerCone());
            glUniform1f(glGetUniformLocation(deferredLightingShader, baseName + ".outerCone"),
                       light.getOuterCone());
            
            // Light type (0 = directional, 1 = point, 2 = spot)
            glUniform1i(glGetUniformLocation(deferredLightingShader, baseName + ".type"),
                       light.getType());
            
            // Shadow casting
            glUniform1i(glGetUniformLocation(deferredLightingShader, baseName + ".castShadows"),
                       light.castsShadows() ? 1 : 0);
        }
    }
    
    public int getColorTexture() {
        return colorTexture;
    }
    
    public int getLitTexture() {
        return colorTexture;
    }
    
    public void bindLightingTextures() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        
        if (lightingFBO != 0) {
            cleanup();
            setupFramebuffer();
        }
    }
    
    public void cleanup() {
        if (lightingFBO != 0) {
            glDeleteFramebuffers(lightingFBO);
            glDeleteTextures(colorTexture);
        }
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
    }
}