package com.odyssey.rendering;

import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL45.*;

public class PostProcessing {
    private int width, height;
    
    // Framebuffers for multi-pass effects
    private int hdrFBO, bloomFBO, pingpongFBO1, pingpongFBO2, finalFBO;
    private int hdrColorBuffer, bloomColorBuffer, pingpongBuffer1, pingpongBuffer2, finalColorBuffer;
    
    // Shaders
    private int toneMappingShader, bloomExtractShader, bloomBlurShader, bloomCombineShader, fxaaShader;
    
    // Quad for fullscreen rendering
    private int quadVAO, quadVBO;
    
    // Post-processing parameters
    private float exposure = 1.0f;
    private float bloomThreshold = 1.0f;
    private float bloomIntensity = 0.04f;
    private boolean enableBloom = true;
    private boolean enableFXAA = true;
    
    public PostProcessing(int width, int height) {
        this.width = width;
        this.height = height;
        
        setupQuad();
        setupFramebuffers();
        loadShaders();
        
        System.out.println("Post-processing initialized with bloom and FXAA");
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
    
    private void setupFramebuffers() {
        // HDR framebuffer (for tone mapping input)
        hdrFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, hdrFBO);
        
        hdrColorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, hdrColorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, hdrColorBuffer, 0);
        
        // Check HDR framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("HDR framebuffer not complete: " + getFramebufferStatusString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
        
        // Bloom extraction framebuffer
        bloomFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, bloomFBO);
        
        bloomColorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, bloomColorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width / 2, height / 2, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, bloomColorBuffer, 0);
        
        // Check bloom framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Bloom framebuffer not complete: " + getFramebufferStatusString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
        
        // Ping-pong framebuffers for bloom blur
        pingpongFBO1 = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO1);
        
        pingpongBuffer1 = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, pingpongBuffer1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width / 2, height / 2, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pingpongBuffer1, 0);
        
        // Check ping-pong framebuffer 1 completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Ping-pong framebuffer 1 not complete: " + getFramebufferStatusString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
        
        pingpongFBO2 = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO2);
        
        pingpongBuffer2 = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, pingpongBuffer2);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width / 2, height / 2, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pingpongBuffer2, 0);
        
        // Check ping-pong framebuffer 2 completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Ping-pong framebuffer 2 not complete: " + getFramebufferStatusString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
        
        // Final output framebuffer
        finalFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, finalFBO);
        
        finalColorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, finalColorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, finalColorBuffer, 0);
        
        // Check final framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Final framebuffer not complete: " + getFramebufferStatusString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void loadShaders() {
        ShaderManager shaderManager = new ShaderManager();
        
        // Load actual shader programs using fullscreen vertex shader
        toneMappingShader = shaderManager.loadProgram(
            "resources/shaders/fullscreen.vert", "resources/shaders/tone_mapping.frag");
        bloomExtractShader = shaderManager.loadProgram(
            "resources/shaders/fullscreen.vert", "resources/shaders/bloom_extract.frag");
        bloomBlurShader = shaderManager.loadProgram(
            "resources/shaders/fullscreen.vert", "resources/shaders/bloom_blur.frag");
        bloomCombineShader = toneMappingShader; // Reuse tone mapping for combine
        fxaaShader = shaderManager.loadProgram(
            "resources/shaders/fullscreen.vert", "resources/shaders/fxaa.frag");
    }
    
    public void render(int colorTexture, int volumetricTexture, int cloudTexture, int depthTexture, float lightningFlash) {
        // 1. Bloom extraction pass
        if (enableBloom) {
            renderBloomExtraction(colorTexture);
            renderBloomBlur();
        }
        
        // 2. Tone mapping and final composition
        renderToneMapping(colorTexture, volumetricTexture, cloudTexture, depthTexture, lightningFlash);
        
        // 3. FXAA anti-aliasing
        if (enableFXAA) {
            renderFXAA();
        }
        
        // Note: Final composition to screen is now handled by AdvancedRenderingPipeline
    }
    
    private void renderBloomExtraction(int colorTexture) {
        glBindFramebuffer(GL_FRAMEBUFFER, bloomFBO);
        glViewport(0, 0, width / 2, height / 2);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(bloomExtractShader);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glUniform1i(glGetUniformLocation(bloomExtractShader, "hdrBuffer"), 0);
        glUniform1f(glGetUniformLocation(bloomExtractShader, "threshold"), bloomThreshold);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
    
    private void renderBloomBlur() {
        boolean horizontal = true;
        boolean firstIteration = true;
        int amount = 10; // Number of blur passes
        
        glUseProgram(bloomBlurShader);
        
        for (int i = 0; i < amount; i++) {
            glBindFramebuffer(GL_FRAMEBUFFER, horizontal ? pingpongFBO2 : pingpongFBO1);
            glUniform1i(glGetUniformLocation(bloomBlurShader, "horizontal"), horizontal ? 1 : 0);
            
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, firstIteration ? bloomColorBuffer : 
                         (horizontal ? pingpongBuffer1 : pingpongBuffer2));
            
            glBindVertexArray(quadVAO);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            
            horizontal = !horizontal;
            if (firstIteration) firstIteration = false;
        }
    }
    
    private void renderToneMapping(int colorTexture, int volumetricTexture, int cloudTexture, int depthTexture, float lightningFlash) {
        glBindFramebuffer(GL_FRAMEBUFFER, finalFBO);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(toneMappingShader);
        
        // Bind HDR color texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glUniform1i(glGetUniformLocation(toneMappingShader, "hdrBuffer"), 0);
        
        // Bind bloom texture
        if (enableBloom) {
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, pingpongBuffer1); // Final bloom result
            glUniform1i(glGetUniformLocation(toneMappingShader, "bloomBlur"), 1);
        }
        
        // Bind volumetric lighting
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, volumetricTexture);
        glUniform1i(glGetUniformLocation(toneMappingShader, "volumetric"), 2);
        
        // Bind CLOUD texture
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, cloudTexture);
        glUniform1i(glGetUniformLocation(toneMappingShader, "cloudTexture"), 3);

        // Bind depth texture (for sky check)
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glUniform1i(glGetUniformLocation(toneMappingShader, "depthTexture"), 4);
        
        // Set other uniforms
        glUniform1f(glGetUniformLocation(toneMappingShader, "exposure"), exposure);
        glUniform1f(glGetUniformLocation(toneMappingShader, "u_lightningFlash"), lightningFlash);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
    
    private void renderFXAA() {
        // Create a temporary buffer swap: render FXAA result back to finalFBO
        // First, copy finalColorBuffer to hdrColorBuffer as input
        glBindFramebuffer(GL_READ_FRAMEBUFFER, finalFBO);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, hdrFBO);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_LINEAR);
        
        // Now render FXAA from hdrColorBuffer back to finalFBO
        glBindFramebuffer(GL_FRAMEBUFFER, finalFBO);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(fxaaShader);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hdrColorBuffer);
        glUniform1i(glGetUniformLocation(fxaaShader, "screenTexture"), 0);
        glUniform2f(glGetUniformLocation(fxaaShader, "texelSize"), 1.0f / width, 1.0f / height);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }
    

    
    // Parameter setters
    public void setExposure(float exposure) {
        this.exposure = Math.max(0.1f, exposure);
    }
    
    public void setBloomThreshold(float threshold) {
        this.bloomThreshold = Math.max(0.0f, threshold);
    }
    
    public void setBloomIntensity(float intensity) {
        this.bloomIntensity = Math.max(0.0f, intensity);
    }
    
    public void setBloomEnabled(boolean enabled) {
        this.enableBloom = enabled;
    }
    
    public void setFXAAEnabled(boolean enabled) {
        this.enableFXAA = enabled;
    }
    
    /**
     * Get the final processed image texture for rendering to screen
     * @return The texture ID of the final processed image
     */
    public int getFinalImageTexture() {
        // Always return finalColorBuffer as it contains the final processed result
        // Whether FXAA is enabled or not, the final result is in finalColorBuffer
        return finalColorBuffer;
    }
    
    /**
     * Helper method to get human-readable framebuffer status strings
     */
    private String getFramebufferStatusString(int status) {
        switch (status) {
            case GL_FRAMEBUFFER_COMPLETE:
                return "GL_FRAMEBUFFER_COMPLETE";
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case GL_FRAMEBUFFER_UNSUPPORTED:
                return "GL_FRAMEBUFFER_UNSUPPORTED";
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                return "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
            default:
                return "Unknown status: 0x" + Integer.toHexString(status);
        }
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        
        cleanup();
        setupFramebuffers();
    }

    public void cleanup() {
        glDeleteFramebuffers(hdrFBO);
        glDeleteFramebuffers(bloomFBO);
        glDeleteFramebuffers(pingpongFBO1);
        glDeleteFramebuffers(pingpongFBO2);
        glDeleteFramebuffers(finalFBO);
        
        glDeleteTextures(hdrColorBuffer);
        glDeleteTextures(bloomColorBuffer);
        glDeleteTextures(pingpongBuffer1);
        glDeleteTextures(pingpongBuffer2);
        glDeleteTextures(finalColorBuffer);
        
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
    }
}