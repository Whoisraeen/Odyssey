package com.odyssey.rendering;

import com.odyssey.rendering.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL45.*;

public class SSAORenderer {
    private int ssaoFBO, ssaoBlurFBO;
    private int ssaoColorBuffer, ssaoBlurBuffer;
    private int noiseTexture;
    private int ssaoShader, ssaoBlurShader;
    private int quadVAO, quadVBO;
    private int width, height;
    
    // SSAO parameters
    private static final int KERNEL_SIZE = 64;
    private static final float RADIUS = 0.5f;
    private static final float BIAS = 0.025f;
    private Vector3f[] ssaoKernel;
    private Vector3f[] ssaoNoise;
    
    public SSAORenderer(int width, int height) {
        this.width = width;
        this.height = height;
        
        generateSSAOKernel();
        generateSSAONoise();
        setupFramebuffers();
        setupQuad();
        loadShaders();
        
        System.out.println("SSAO Renderer initialized with " + KERNEL_SIZE + " samples");
    }
    
    private void generateSSAOKernel() {
        ssaoKernel = new Vector3f[KERNEL_SIZE];
        Random random = new Random();
        
        for (int i = 0; i < KERNEL_SIZE; i++) {
            Vector3f sample = new Vector3f(
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat()
            );
            sample.normalize();
            sample.mul(random.nextFloat());
            
            // Scale samples to be more aligned to center of kernel
            float scale = (float) i / KERNEL_SIZE;
            scale = lerp(0.1f, 1.0f, scale * scale);
            sample.mul(scale);
            
            ssaoKernel[i] = sample;
        }
    }
    
    private void generateSSAONoise() {
        ssaoNoise = new Vector3f[16];
        Random random = new Random();
        
        for (int i = 0; i < 16; i++) {
            Vector3f noise = new Vector3f(
                random.nextFloat() * 2.0f - 1.0f,
                random.nextFloat() * 2.0f - 1.0f,
                0.0f
            );
            ssaoNoise[i] = noise;
        }
        
        // Create noise texture
        noiseTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        
        FloatBuffer noiseData = BufferUtils.createFloatBuffer(16 * 3);
        for (Vector3f noise : ssaoNoise) {
            noiseData.put(noise.x).put(noise.y).put(noise.z);
        }
        noiseData.flip();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 4, 4, 0, GL_RGB, GL_FLOAT, noiseData);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }
    
    private void setupFramebuffers() {
        // SSAO framebuffer
        ssaoFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoFBO);
        
        ssaoColorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, ssaoColorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoColorBuffer, 0);
        
        // SSAO blur framebuffer
        ssaoBlurFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBlurFBO);
        
        ssaoBlurBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, ssaoBlurBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoBlurBuffer, 0);
        
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
    
    private void loadShaders() {
        // For now, create placeholder shader IDs
        // These would be loaded from actual shader files
        ssaoShader = 1; // Placeholder
        ssaoBlurShader = 2; // Placeholder
    }
    
    public void render(GBuffer gBuffer, Camera camera) {
        // 1. SSAO pass
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoFBO);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(ssaoShader);
        
        // Bind G-Buffer textures
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getPositionTexture());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getNormalTexture());
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        
        // Set uniforms
        Matrix4f projection = camera.getProjectionMatrix();
        glUniformMatrix4fv(glGetUniformLocation(ssaoShader, "projection"), false, projection.get(new float[16]));
        
        // Send kernel samples
        for (int i = 0; i < KERNEL_SIZE; i++) {
            String uniformName = "samples[" + i + "]";
            int location = glGetUniformLocation(ssaoShader, uniformName);
            glUniform3f(location, ssaoKernel[i].x, ssaoKernel[i].y, ssaoKernel[i].z);
        }
        
        glUniform1f(glGetUniformLocation(ssaoShader, "radius"), RADIUS);
        glUniform1f(glGetUniformLocation(ssaoShader, "bias"), BIAS);
        
        // Render quad
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        // 2. Blur pass
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBlurFBO);
        glClear(GL_COLOR_BUFFER_BIT);
        
        glUseProgram(ssaoBlurShader);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoColorBuffer);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    public int getSSAOTexture() {
        return ssaoBlurBuffer;
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        
        // Recreate framebuffers with new size
        cleanup();
        setupFramebuffers();
    }
    
    public void cleanup() {
        glDeleteFramebuffers(ssaoFBO);
        glDeleteFramebuffers(ssaoBlurFBO);
        glDeleteTextures(ssaoColorBuffer);
        glDeleteTextures(ssaoBlurBuffer);
        glDeleteTextures(noiseTexture);
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
    }
    
    private float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
}