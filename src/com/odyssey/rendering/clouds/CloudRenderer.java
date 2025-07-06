package com.odyssey.rendering.clouds;

import com.odyssey.rendering.ShaderManager;
import com.odyssey.rendering.Camera;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL30.*;

public class CloudRenderer {

    private final int shaderProgram;
    private int fbo;
    private int cloudTexture;
    private final int quadVao;

    public CloudRenderer(int width, int height) {
        ShaderManager shaderManager = new ShaderManager();
        this.shaderProgram = shaderManager.loadProgram("resources/shaders/clouds.vert", "resources/shaders/clouds.frag");

        // Create FBO and texture
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        cloudTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, cloudTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cloudTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cloud FBO not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Fullscreen quad setup
        quadVao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        float[] quadVertices = {
            -1.0f,  1.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
             
            -1.0f,  1.0f, 0.0f, 1.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
             1.0f,  1.0f, 1.0f, 1.0f
        };
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    }

    public void render(Camera camera, float time, float coverage, float density) {
        glUseProgram(shaderProgram);
        
        // Set uniforms
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "invProjection"), false, camera.getProjectionMatrix().invert(new Matrix4f()).get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "invView"), false, camera.getViewMatrix().invert(new Matrix4f()).get(new float[16]));
        glUniform1f(glGetUniformLocation(shaderProgram, "time"), time);
        glUniform1f(glGetUniformLocation(shaderProgram, "cloudCoverage"), coverage);
        glUniform1f(glGetUniformLocation(shaderProgram, "cloudDensity"), density);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glClear(GL_COLOR_BUFFER_BIT);

        glBindVertexArray(quadVao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getCloudTexture() {
        return cloudTexture;
    }
    
    public void resize(int width, int height) {
        // Recreate the cloud texture with new dimensions
        glDeleteTextures(cloudTexture);
        glDeleteFramebuffers(fbo);
        
        // Recreate FBO and texture with new size
        int newFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, newFbo);
        
        int newCloudTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, newCloudTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, newCloudTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Cloud FBO not complete after resize!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        // Update the fields with new values
        this.fbo = newFbo;
        this.cloudTexture = newCloudTexture;
    }

    public void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteFramebuffers(fbo);
        glDeleteTextures(cloudTexture);
        glDeleteVertexArrays(quadVao);
    }
}