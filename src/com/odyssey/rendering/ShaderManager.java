package com.odyssey.rendering;

import com.odyssey.utils.FileUtils;
import org.lwjgl.opengl.GL20;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public class ShaderManager {
    private static final Logger LOGGER = Logger.getLogger(ShaderManager.class.getName());
    
    // Manages shaders
    private final List<Integer> programs = new ArrayList<>();
    private final List<Integer> shaders = new ArrayList<>();
    private final Map<String, Integer> fallbackShaders = new HashMap<>();
    
    // Fallback shader sources
    private static final String FALLBACK_VERTEX_SHADER = 
        "#version 330 core\n" +
        "layout (location = 0) in vec3 aPos;\n" +
        "uniform mat4 mvpMatrix;\n" +
        "void main() {\n" +
        "    gl_Position = mvpMatrix * vec4(aPos, 1.0);\n" +
        "}";
    
    private static final String FALLBACK_FRAGMENT_SHADER = 
        "#version 330 core\n" +
        "out vec4 FragColor;\n" +
        "void main() {\n" +
        "    FragColor = vec4(1.0, 0.0, 1.0, 1.0); // Magenta to indicate fallback\n" +
        "}";
    
    private static final String FALLBACK_DEFERRED_FRAGMENT_SHADER = 
        "#version 330 core\n" +
        "layout (location = 0) out vec4 gPosition;\n" +
        "layout (location = 1) out vec4 gNormal;\n" +
        "layout (location = 2) out vec4 gAlbedo;\n" +
        "layout (location = 3) out vec4 gMaterial;\n" +
        "void main() {\n" +
        "    gPosition = vec4(0.0, 0.0, 0.0, 1.0);\n" +
        "    gNormal = vec4(0.0, 1.0, 0.0, 1.0);\n" +
        "    gAlbedo = vec4(1.0, 0.0, 1.0, 1.0);\n" +
        "    gMaterial = vec4(0.5, 0.0, 0.0, 1.0);\n" +
        "}";
    
    public ShaderManager() {
        initializeFallbackShaders();
    }

    public int loadProgram(String vertPath, String fragPath) {
        return loadProgramWithFallback(vertPath, fragPath, true);
    }
    
    public int loadProgramWithFallback(String vertPath, String fragPath, boolean useFallback) {
        try {
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
                String errorLog = glGetProgramInfoLog(program, 1024);
                LOGGER.log(Level.SEVERE, "Shader linking failed for " + vertPath + ", " + fragPath + ": " + errorLog);
                
                // Clean up failed program
                glDetachShader(program, vertShader);
                glDetachShader(program, fragShader);
                glDeleteShader(vertShader);
                glDeleteShader(fragShader);
                glDeleteProgram(program);
                
                if (useFallback) {
                    LOGGER.log(Level.WARNING, "Using fallback shader for " + vertPath + ", " + fragPath);
                    return getFallbackProgram(vertPath, fragPath);
                } else {
                    throw new RuntimeException("Shader linking failed: " + errorLog);
                }
            }

            // Detach and delete shaders as they are no longer needed
            glDetachShader(program, vertShader);
            glDetachShader(program, fragShader);
            glDeleteShader(vertShader);
            glDeleteShader(fragShader);

            programs.add(program);
            LOGGER.log(Level.INFO, "Successfully loaded shader program: " + vertPath + ", " + fragPath);
            return program;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load shader program " + vertPath + ", " + fragPath, e);
            if (useFallback) {
                LOGGER.log(Level.WARNING, "Using fallback shader due to exception");
                return getFallbackProgram(vertPath, fragPath);
            } else {
                throw new RuntimeException("Shader loading failed", e);
            }
        }
    }

    private int createShader(String path, int type) {
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Failed to create shader of type " + getShaderTypeName(type));
        }

        try {
            String source = FileUtils.readFromFile(path);
            if (source == null || source.trim().isEmpty()) {
                throw new RuntimeException("Shader source is empty or null for: " + path);
            }
            
            glShaderSource(shader, source);
            glCompileShader(shader);

            // Check for compilation errors
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
                String errorLog = glGetShaderInfoLog(shader, 1024);
                glDeleteShader(shader);
                throw new RuntimeException("Shader compilation failed for " + path + ": " + errorLog);
            }
            
            shaders.add(shader);
            return shader;
            
        } catch (Exception e) {
            if (glIsShader(shader)) {
                glDeleteShader(shader);
            }
            throw new RuntimeException("Failed to create shader from " + path, e);
        }
    }
    
    private int createShaderFromSource(String source, int type, String name) {
        int shader = glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Failed to create fallback shader of type " + getShaderTypeName(type));
        }

        glShaderSource(shader, source);
        glCompileShader(shader);

        // Check for compilation errors
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String errorLog = glGetShaderInfoLog(shader, 1024);
            glDeleteShader(shader);
            throw new RuntimeException("Fallback shader compilation failed for " + name + ": " + errorLog);
        }
        
        return shader;
    }
    
    private String getShaderTypeName(int type) {
        switch (type) {
            case GL_VERTEX_SHADER: return "VERTEX";
            case GL_FRAGMENT_SHADER: return "FRAGMENT";
            case GL_GEOMETRY_SHADER: return "GEOMETRY";
            case GL_COMPUTE_SHADER: return "COMPUTE";
            default: return "UNKNOWN(" + type + ")";
        }
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

    private void initializeFallbackShaders() {
        try {
            // Create basic fallback program
            int vertShader = createShaderFromSource(FALLBACK_VERTEX_SHADER, GL_VERTEX_SHADER, "fallback_vertex");
            int fragShader = createShaderFromSource(FALLBACK_FRAGMENT_SHADER, GL_FRAGMENT_SHADER, "fallback_fragment");
            
            int program = glCreateProgram();
            glAttachShader(program, vertShader);
            glAttachShader(program, fragShader);
            glLinkProgram(program);
            
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                throw new RuntimeException("Fallback shader linking failed: " + glGetProgramInfoLog(program, 1024));
            }
            
            glDetachShader(program, vertShader);
            glDetachShader(program, fragShader);
            glDeleteShader(vertShader);
            glDeleteShader(fragShader);
            
            fallbackShaders.put("basic", program);
            
            // Create deferred fallback program
            int deferredFragShader = createShaderFromSource(FALLBACK_DEFERRED_FRAGMENT_SHADER, GL_FRAGMENT_SHADER, "fallback_deferred_fragment");
            int deferredVertShader = createShaderFromSource(FALLBACK_VERTEX_SHADER, GL_VERTEX_SHADER, "fallback_deferred_vertex");
            
            int deferredProgram = glCreateProgram();
            glAttachShader(deferredProgram, deferredVertShader);
            glAttachShader(deferredProgram, deferredFragShader);
            glLinkProgram(deferredProgram);
            
            if (glGetProgrami(deferredProgram, GL_LINK_STATUS) == 0) {
                LOGGER.log(Level.WARNING, "Deferred fallback shader linking failed: " + glGetProgramInfoLog(deferredProgram, 1024));
                glDeleteProgram(deferredProgram);
            } else {
                glDetachShader(deferredProgram, deferredVertShader);
                glDetachShader(deferredProgram, deferredFragShader);
                glDeleteShader(deferredVertShader);
                glDeleteShader(deferredFragShader);
                fallbackShaders.put("deferred", deferredProgram);
            }
            
            LOGGER.log(Level.INFO, "Fallback shaders initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize fallback shaders", e);
        }
    }
    
    private int getFallbackProgram(String vertPath, String fragPath) {
        // Determine appropriate fallback based on shader names
        if (fragPath.contains("gbuffer") || fragPath.contains("deferred") || vertPath.contains("gbuffer")) {
            Integer deferredFallback = fallbackShaders.get("deferred");
            if (deferredFallback != null) {
                return deferredFallback;
            }
        }
        
        // Default to basic fallback
        Integer basicFallback = fallbackShaders.get("basic");
        if (basicFallback != null) {
            return basicFallback;
        }
        
        // If no fallback available, throw exception
        throw new RuntimeException("No fallback shader available for " + vertPath + ", " + fragPath);
    }
    
    public boolean validateShader(int shader) {
        if (!glIsShader(shader)) {
            return false;
        }
        return glGetShaderi(shader, GL_COMPILE_STATUS) != 0;
    }
    
    public boolean validateProgram(int program) {
        if (!glIsProgram(program)) {
            return false;
        }
        return glGetProgrami(program, GL_LINK_STATUS) != 0;
    }
    
    public String getShaderInfoLog(int shader) {
        return glGetShaderInfoLog(shader, 1024);
    }
    
    public String getProgramInfoLog(int program) {
        return glGetProgramInfoLog(program, 1024);
    }

    public void cleanup() {
        for (int program : programs) {
            if (glIsProgram(program)) {
                glDeleteProgram(program);
            }
        }
        
        for (int program : fallbackShaders.values()) {
            if (glIsProgram(program)) {
                glDeleteProgram(program);
            }
        }
        
        programs.clear();
        fallbackShaders.clear();
        
        LOGGER.log(Level.INFO, "ShaderManager cleanup completed");
    }
}