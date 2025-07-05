package com.odyssey.rendering;

import com.odyssey.utils.FileUtils;
import org.lwjgl.opengl.GL20;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;

public class ShaderManager {
    // Manages shaders
    private final List<Integer> programs = new ArrayList<>();
    private final List<Integer> shaders = new ArrayList<>();

    public int loadProgram(String vertPath, String fragPath) {
        // Load and compile shaders
        int vertShader = createShader(vertPath, GL_VERTEX_SHADER);
        int fragShader = createShader(fragPath, GL_FRAGMENT_SHADER);

        // Link shaders into a program
        int program = glCreateProgram();
        glAttachShader(program, vertShader);
        glAttachShader(program, fragShader);
        glLinkProgram(program);

        // Check for linking errors
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            System.err.println("Shader linking failed: " + glGetProgramInfoLog(program, 1024));
            System.exit(1);
        }

        // Detach and delete shaders as they are no longer needed
        glDetachShader(program, vertShader);
        glDetachShader(program, fragShader);
        glDeleteShader(vertShader);
        glDeleteShader(fragShader);

        programs.add(program);
        return program;
    }

    private int createShader(String path, int type) {
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Failed to create shader of type " + type);
        }

        String source = FileUtils.readFromFile(path);
        glShaderSource(shader, source);
        glCompileShader(shader);

        // Check for compilation errors
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.err.println("Shader compilation failed for " + path + ": " + glGetShaderInfoLog(shader, 1024));
            System.exit(1);
        }
        
        shaders.add(shader);
        return shader;
    }

    public static void setUniform(int program, String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            int location = glGetUniformLocation(program, name);
            glUniformMatrix4fv(location, false, buffer);
        }
    }
    
    public static void setUniform(int program, String name, Vector3f vector) {
        int location = glGetUniformLocation(program, name);
        glUniform3f(location, vector.x, vector.y, vector.z);
    }
    
    public static void setUniform(int program, String name, float value) {
        int location = glGetUniformLocation(program, name);
        glUniform1f(location, value);
    }
    
    public static void setUniform(int program, String name, int value) {
        int location = glGetUniformLocation(program, name);
        glUniform1i(location, value);
    }

    public void cleanup() {
        for (int program : programs) {
            glDeleteProgram(program);
        }
    }
}