package com.odyssey.rendering;

import com.odyssey.rendering.Camera;
import com.odyssey.rendering.scene.Light;
import com.odyssey.rendering.scene.RenderObject;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

public class UniformBufferManager {
    private int cameraUBO;
    private int lightUBO;
    private int materialUBO;
    private int timeUBO;
    
    // Manages uniform buffers
    public void createCameraUniforms() {
        cameraUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, cameraUBO);
        // Camera UBO: view matrix (64 bytes) + projection matrix (64 bytes) + position (16 bytes) = 144 bytes
        glBufferData(GL_UNIFORM_BUFFER, 144, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, cameraUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    public void createLightUniforms() {
        lightUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, lightUBO);
        glBufferData(GL_UNIFORM_BUFFER, 1024, GL_DYNAMIC_DRAW); // Placeholder size
        glBindBufferBase(GL_UNIFORM_BUFFER, 1, lightUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    public void createMaterialUniforms() {
        materialUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, materialUBO);
        glBufferData(GL_UNIFORM_BUFFER, 256, GL_DYNAMIC_DRAW); // Placeholder size
        glBindBufferBase(GL_UNIFORM_BUFFER, 2, materialUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    public void createTimeUniforms() {
        timeUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, timeUBO);
        glBufferData(GL_UNIFORM_BUFFER, 16, GL_DYNAMIC_DRAW); // Just time data
        glBindBufferBase(GL_UNIFORM_BUFFER, 3, timeUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    public void bindCameraUniforms(Camera camera) {
        // For now, use traditional uniforms as fallback
        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        if (currentProgram != 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer viewBuffer = stack.mallocFloat(16);
                FloatBuffer projBuffer = stack.mallocFloat(16);
                
                camera.getViewMatrix().get(viewBuffer);
                camera.getProjectionMatrix().get(projBuffer);
                
                int viewLoc = glGetUniformLocation(currentProgram, "view");
                int projLoc = glGetUniformLocation(currentProgram, "projection");
                
                if (viewLoc != -1) {
                    glUniformMatrix4fv(viewLoc, false, viewBuffer);
                }
                if (projLoc != -1) {
                    glUniformMatrix4fv(projLoc, false, projBuffer);
                }
            }
        }
    }
    
    public void bindMaterialUniforms() {
        // Placeholder implementation
    }
    
    public void bindObjectUniforms(RenderObject obj) {
        // Placeholder implementation
    }
    
    public void updateCameraUniforms(Camera camera) {
        if (cameraUBO != 0) {
            glBindBuffer(GL_UNIFORM_BUFFER, cameraUBO);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(36); // 16 + 16 + 4 floats
                
                // View matrix (16 floats)
                camera.getViewMatrix().get(0, buffer);
                // Projection matrix (16 floats)
                camera.getProjectionMatrix().get(16, buffer);
                // Camera position (3 floats + 1 padding)
                buffer.put(32, camera.getPosition().x);
                buffer.put(33, camera.getPosition().y);
                buffer.put(34, camera.getPosition().z);
                buffer.put(35, 1.0f); // padding
                
                glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
            }
            glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }
    }
    
    public void updateLightUniforms(List<Light> lights) {
        // Placeholder implementation
    }
    
    public void updateTimeUniforms(float deltaTime) {
        // Placeholder implementation
    }
    
    public void cleanup() {
        if (cameraUBO != 0) glDeleteBuffers(cameraUBO);
        if (lightUBO != 0) glDeleteBuffers(lightUBO);
        if (materialUBO != 0) glDeleteBuffers(materialUBO);
        if (timeUBO != 0) glDeleteBuffers(timeUBO);
    }
}