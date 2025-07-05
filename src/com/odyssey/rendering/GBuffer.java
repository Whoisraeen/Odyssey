package com.odyssey.rendering;

import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL45.*;

/**
 * G-Buffer for deferred rendering
 * Layout: Position, Normal, Albedo, Material Properties
 */
public class GBuffer {
    private int framebuffer;
    private int[] colorTextures = new int[4]; // Position, Normal, Albedo, Material
    private int depthTexture;
    
    private int width, height;
    
    public GBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        
        initialize();
    }
    
    private void initialize() {
        // Create framebuffer
        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        
        // Create color textures
        for (int i = 0; i < colorTextures.length; i++) {
            colorTextures[i] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTextures[i]);
            
            // Different formats for different G-Buffer components
            int format = getGBufferFormat(i);
            int internalFormat = getGBufferInternalFormat(i);
            
            glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_FLOAT, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i, GL_TEXTURE_2D, colorTextures[i], 0);
        }
        
        // Create depth texture
        depthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);

        // Check framebuffer status
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("GBuffer framebuffer is not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        glDeleteFramebuffers(framebuffer);
        glDeleteTextures(colorTextures);
        glDeleteTextures(depthTexture);
    }

    public int getDepthTexture() {
        return depthTexture;
    }

    public void copyDepthToDefaultFramebuffer(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, screenWidth, screenHeight, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        cleanup();
        initialize();
    }

    private int getGBufferFormat(int index) {
        switch (index) {
            case 0: // Position
            case 1: // Normal
                return GL_RGB;
            case 2: // Albedo
                return GL_RGBA;
            case 3: // Material
                return GL_RGB;
            default:
                throw new IllegalArgumentException("Invalid G-Buffer texture index");
        }
    }

    private int getGBufferInternalFormat(int index) {
        switch (index) {
            case 0: // Position (float precision)
            case 1: // Normal (float precision)
                return GL_RGB16F;
            case 2: // Albedo (standard)
                return GL_RGBA8;
            case 3: // Material (metallic, roughness, ao)
                return GL_RGB16F;
            default:
                throw new IllegalArgumentException("Invalid G-Buffer texture index");
        }
    }
} 