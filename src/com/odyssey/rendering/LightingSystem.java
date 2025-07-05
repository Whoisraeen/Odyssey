package com.odyssey.rendering;

import com.odyssey.rendering.lighting.ShadowMapping;
import com.odyssey.rendering.scene.Camera;
import com.odyssey.rendering.scene.Light;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;
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
        
        glBindFramebuffer(GL_FRAMEBUFFER, lightingFBO);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(deferredLightingShader);
        
        // Bind G-Buffer textures
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
        glUniform1i(glGetUniformLocation(deferredLightingShader, "ssao"), 4);
        
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
        glUniform3f(glGetUniformLocation(deferredLightingShader, "fogColor"), 0.7f, 0.8f, 0.9f);
        glUniform1f(glGetUniformLocation(deferredLightingShader, "fogDensity"), 0.02f);
        
        // Set light uniforms
        setLightUniforms(lights);
        
        // Render fullscreen quad
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void setLightUniforms(List<Light> lights) {
        // Set number of lights
        int numLights = Math.min(lights.size(), 8); // Limit to 8 lights to match MAX_LIGHTS
        glUniform1i(glGetUniformLocation(deferredLightingShader, "numLights"), numLights);
        
        // Set individual light properties
        for (int i = 0; i < numLights; i++) {
            Light light = lights.get(i);
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