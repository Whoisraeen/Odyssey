package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SelectionBoxRenderer {

    private final int shaderProgram;
    private final int vao;
    private final int vbo;

    private final int modelLoc;
    private final int viewLoc;
    private final int projectionLoc;

    public SelectionBoxRenderer() {
        ShaderManager shaderManager = new ShaderManager();
        shaderProgram = shaderManager.loadProgram("shaders/selection.vert", "shaders/selection.frag");

        modelLoc = glGetUniformLocation(shaderProgram, "model");
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projectionLoc = glGetUniformLocation(shaderProgram, "projection");

        float[] vertices = {
            // Bottom face
            0, 0, 0,  1, 0, 0,
            1, 0, 0,  1, 0, 1,
            1, 0, 1,  0, 0, 1,
            0, 0, 1,  0, 0, 0,
            // Top face
            0, 1, 0,  1, 1, 0,
            1, 1, 0,  1, 1, 1,
            1, 1, 1,  0, 1, 1,
            0, 1, 1,  0, 1, 0,
            // Connecting lines
            0, 0, 0,  0, 1, 0,
            1, 0, 0,  1, 1, 0,
            1, 0, 1,  1, 1, 1,
            0, 0, 1,  0, 1, 1
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render(Matrix4f view, Matrix4f projection, Vector3i blockPos) {
        // Save current OpenGL state
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        
        glUseProgram(shaderProgram);

        Matrix4f model = new Matrix4f()
            .translate(blockPos.x, blockPos.y, blockPos.z)
            .scale(1.01f); // Slightly larger to avoid Z-fighting

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(viewLoc, false, view.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(projectionLoc, false, projection.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(modelLoc, false, model.get(stack.mallocFloat(16)));
        }

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 24);
        
        // Restore previous OpenGL state
        glBindVertexArray(previousVAO);
        glUseProgram(previousProgram);
    }

    public void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}