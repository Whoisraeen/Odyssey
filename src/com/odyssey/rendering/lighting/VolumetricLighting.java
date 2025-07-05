package com.odyssey.rendering.lighting;

import com.odyssey.rendering.scene.Light;
import com.odyssey.rendering.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import static org.lwjgl.opengl.GL45.*;

public class VolumetricLighting {
    private int width, height;
    
    // Framebuffers and textures
    private int volumetricFBO;
    private int volumetricTexture;
    private int depthTexture;
    
    // Ray marching parameters
    private static final int NUM_SAMPLES = 64;
    private static final float SCATTERING_COEFFICIENT = 0.1f;
    private static final float ABSORPTION_COEFFICIENT = 0.05f;
    private static final float PHASE_G = 0.76f; // Henyey-Greenstein phase function parameter
    
    // Shaders
    private int volumetricShader;
    
    // Quad for fullscreen rendering
    private int quadVAO, quadVBO;
    
    // Volumetric lighting parameters
    private float density = 0.02f;
    private float scattering = 0.1f;
    private float absorption = 0.05f;
    private Vector3f fogColor = new Vector3f(0.5f, 0.6f, 0.7f);
    private boolean enableVolumetricFog = true;
    
    public VolumetricLighting(int width, int height) {
        this.width = width;
        this.height = height;
        
        setupQuad();
        setupFramebuffer();
        loadShaders();
        
        System.out.println("Volumetric lighting initialized with ray marching");
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
    
    private void setupFramebuffer() {
        volumetricFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, volumetricFBO);
        
        // Volumetric lighting texture (lower resolution for performance)
        int volWidth = width / 2;
        int volHeight = height / 2;
        
        volumetricTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, volumetricTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, volWidth, volHeight, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, volumetricTexture, 0);
        
        // Depth texture for ray marching
        depthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, volWidth, volHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Volumetric lighting framebuffer not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void loadShaders() {
        // Placeholder shader ID
        volumetricShader = 30;
    }
    
    public void render(Camera camera, List<Light> lights, int sceneDepthTexture, int shadowMapTexture) {
        if (!enableVolumetricFog) {
            // Clear volumetric texture to black
            glBindFramebuffer(GL_FRAMEBUFFER, volumetricFBO);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            return;
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, volumetricFBO);
        glViewport(0, 0, width / 2, height / 2);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        glUseProgram(volumetricShader);
        
        // Bind scene depth texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneDepthTexture);
        glUniform1i(glGetUniformLocation(volumetricShader, "sceneDepth"), 0);
        
        // Bind shadow map
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowMapTexture);
        glUniform1i(glGetUniformLocation(volumetricShader, "shadowMap"), 1);
        
        // Set camera uniforms
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projMatrix = camera.getProjectionMatrix();
        Matrix4f invViewProj = new Matrix4f(projMatrix).mul(viewMatrix).invert();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrixBuffer = stack.mallocFloat(16);
            glUniformMatrix4fv(glGetUniformLocation(volumetricShader, "invViewProj"), false, 
                              invViewProj.get(matrixBuffer));
        }
        Vector3f cameraPos = camera.getPosition();
        glUniform3f(glGetUniformLocation(volumetricShader, "cameraPos"), 
                   cameraPos.x, cameraPos.y, cameraPos.z);
        
        // Set volumetric parameters
        glUniform1i(glGetUniformLocation(volumetricShader, "numSamples"), NUM_SAMPLES);
        glUniform1f(glGetUniformLocation(volumetricShader, "density"), density);
        glUniform1f(glGetUniformLocation(volumetricShader, "scattering"), scattering);
        glUniform1f(glGetUniformLocation(volumetricShader, "absorption"), absorption);
        glUniform1f(glGetUniformLocation(volumetricShader, "phaseG"), PHASE_G);
        glUniform3f(glGetUniformLocation(volumetricShader, "fogColor"), 
                   fogColor.x, fogColor.y, fogColor.z);
        
        // Set light uniforms
        setLightUniforms(lights);
        
        // Render fullscreen quad
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void setLightUniforms(List<Light> lights) {
        // Find primary directional light for volumetric scattering
        Light primaryLight = null;
        for (Light light : lights) {
            if (light.getLightType() == Light.LightType.DIRECTIONAL) {
                primaryLight = light;
                break;
            }
        }
        
        if (primaryLight != null) {
            Vector3f lightDir = primaryLight.getDirection();
            Vector3f lightColor = primaryLight.getColor();
            glUniform3f(glGetUniformLocation(volumetricShader, "lightDirection"), 
                       lightDir.x, lightDir.y, lightDir.z);
            glUniform3f(glGetUniformLocation(volumetricShader, "lightColor"), 
                       lightColor.x, lightColor.y, lightColor.z);
            glUniform1f(glGetUniformLocation(volumetricShader, "lightIntensity"), 
                       primaryLight.getIntensity());
        } else {
            // Default sun light
            glUniform3f(glGetUniformLocation(volumetricShader, "lightDirection"), 0.3f, -0.7f, 0.2f);
            glUniform3f(glGetUniformLocation(volumetricShader, "lightColor"), 1.0f, 0.9f, 0.8f);
            glUniform1f(glGetUniformLocation(volumetricShader, "lightIntensity"), 1.0f);
        }
    }
    
    // Parameter setters
    public void setDensity(float density) {
        this.density = Math.max(0.0f, density);
    }
    
    public void setScattering(float scattering) {
        this.scattering = Math.max(0.0f, scattering);
    }
    
    public void setAbsorption(float absorption) {
        this.absorption = Math.max(0.0f, absorption);
    }
    
    public void setFogColor(Vector3f color) {
        this.fogColor.set(color);
    }
    
    public void setVolumetricFogEnabled(boolean enabled) {
        this.enableVolumetricFog = enabled;
    }
    
    public int getVolumetricTexture() {
        return volumetricTexture;
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        
        // Delete old textures
        glDeleteTextures(volumetricTexture);
        glDeleteTextures(depthTexture);
        glDeleteFramebuffers(volumetricFBO);
        
        // Recreate framebuffer
        setupFramebuffer();
    }

    public void cleanup() {
        glDeleteFramebuffers(volumetricFBO);
        glDeleteTextures(volumetricTexture);
        glDeleteTextures(depthTexture);
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
    }
}