package com.odyssey.ui.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ShaderManager for advanced shader handling and management
 * Provides shader compilation, linking, and uniform management
 */
public class ShaderManager {
    private static final String TAG = "ShaderManager";
    
    // Singleton instance
    private static ShaderManager instance;
    private static final Object lock = new Object();
    
    // Shader storage
    private final Map<String, ShaderProgram> programs;
    private final Map<String, Integer> shaders;
    private final Map<String, String> shaderSources;
    
    // Current program
    private ShaderProgram currentProgram;
    
    /**
     * Shader program wrapper
     */
    public static class ShaderProgram {
        private final int programId;
        private final String name;
        private final Map<String, Integer> uniformLocations;
        private final Map<String, Integer> attributeLocations;
        private boolean isLinked;
        
        public ShaderProgram(String name, int programId) {
            this.name = name;
            this.programId = programId;
            this.uniformLocations = new HashMap<>();
            this.attributeLocations = new HashMap<>();
            this.isLinked = false;
        }
        
        public int getProgramId() {
            return programId;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isLinked() {
            return isLinked;
        }
        
        public void setLinked(boolean linked) {
            this.isLinked = linked;
        }
        
        public int getUniformLocation(String name) {
            return uniformLocations.computeIfAbsent(name, 
                    uniformName -> GL20.glGetUniformLocation(programId, uniformName));
        }
        
        public int getAttributeLocation(String name) {
            return attributeLocations.computeIfAbsent(name, 
                    attrName -> GL20.glGetAttribLocation(programId, attrName));
        }
        
        public void use() {
            if (!isLinked) {
                throw new IllegalStateException("Shader program not linked: " + name);
            }
            GL20.glUseProgram(programId);
        }
        
        public void setUniform(String name, float value) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniform1f(location, value);
            }
        }
        
        public void setUniform(String name, int value) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniform1i(location, value);
            }
        }
        
        public void setUniform(String name, float x, float y) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniform2f(location, x, y);
            }
        }
        
        public void setUniform(String name, float x, float y, float z) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniform3f(location, x, y, z);
            }
        }
        
        public void setUniform(String name, float x, float y, float z, float w) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniform4f(location, x, y, z, w);
            }
        }
        
        public void setUniformMatrix4(String name, FloatBuffer matrix) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniformMatrix4fv(location, false, matrix);
            }
        }
        
        public void setUniformMatrix3(String name, FloatBuffer matrix) {
            int location = getUniformLocation(name);
            if (location != -1) {
                GL20.glUniformMatrix3fv(location, false, matrix);
            }
        }
        
        @Override
        public String toString() {
            return String.format("ShaderProgram[name=%s, id=%d, linked=%b]", name, programId, isLinked);
        }
    }
    
    /**
     * Private constructor
     */
    private ShaderManager() {
        this.programs = new ConcurrentHashMap<>();
        this.shaders = new ConcurrentHashMap<>();
        this.shaderSources = new ConcurrentHashMap<>();
    }
    
    /**
     * Get the singleton instance
     */
    public static ShaderManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new ShaderManager();
            }
            return instance;
        }
    }
    
    /**
     * Initialize the shader manager
     */
    public void initialize() {
        System.out.println("ShaderManager initializing...");
        
        // Load default shaders
        loadDefaultShaders();
        
        System.out.println("ShaderManager initialized with " + programs.size() + " programs");
    }
    
    /**
     * Load default shaders
     */
    private void loadDefaultShaders() {
        // Basic vertex shader
        String basicVertexShader = 
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "layout (location = 2) in vec4 aColor;\n" +
            "\n" +
            "uniform mat4 uProjection;\n" +
            "uniform mat4 uView;\n" +
            "uniform mat4 uModel;\n" +
            "\n" +
            "out vec2 vTexCoord;\n" +
            "out vec4 vColor;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    vColor = aColor;\n" +
            "}\n";
        
        // Basic fragment shader
        String basicFragmentShader = 
            "#version 330 core\n" +
            "\n" +
            "in vec2 vTexCoord;\n" +
            "in vec4 vColor;\n" +
            "\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform bool uUseTexture;\n" +
            "\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    if (uUseTexture) {\n" +
            "        FragColor = texture(uTexture, vTexCoord) * vColor;\n" +
            "    } else {\n" +
            "        FragColor = vColor;\n" +
            "    }\n" +
            "}\n";
        
        // Text rendering vertex shader
        String textVertexShader = 
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "\n" +
            "uniform mat4 uProjection;\n" +
            "\n" +
            "out vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}\n";
        
        // Text rendering fragment shader
        String textFragmentShader = 
            "#version 330 core\n" +
            "\n" +
            "in vec2 vTexCoord;\n" +
            "\n" +
            "uniform sampler2D uText;\n" +
            "uniform vec3 uTextColor;\n" +
            "\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 sampled = vec4(1.0, 1.0, 1.0, texture(uText, vTexCoord).r);\n" +
            "    FragColor = vec4(uTextColor, 1.0) * sampled;\n" +
            "}\n";
        
        try {
            // Create basic shader program
            createProgram("basic", basicVertexShader, basicFragmentShader);
            
            // Create text shader program
            createProgram("text", textVertexShader, textFragmentShader);
            
        } catch (Exception e) {
            System.err.println("Failed to load default shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a shader program from source strings
     */
    public ShaderProgram createProgram(String name, String vertexSource, String fragmentSource) {
        try {
            // Compile vertex shader
            int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
            
            // Compile fragment shader
            int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            
            // Create and link program
            int programId = GL20.glCreateProgram();
            GL20.glAttachShader(programId, vertexShader);
            GL20.glAttachShader(programId, fragmentShader);
            GL20.glLinkProgram(programId);
            
            // Check linking status
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer success = stack.mallocInt(1);
                GL20.glGetProgramiv(programId, GL20.GL_LINK_STATUS, success);
                
                if (success.get(0) == GL11.GL_FALSE) {
                    String infoLog = GL20.glGetProgramInfoLog(programId);
                    GL20.glDeleteProgram(programId);
                    throw new RuntimeException("Shader program linking failed: " + infoLog);
                }
            }
            
            // Clean up individual shaders
            GL20.glDetachShader(programId, vertexShader);
            GL20.glDetachShader(programId, fragmentShader);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            
            // Create shader program wrapper
            ShaderProgram program = new ShaderProgram(name, programId);
            program.setLinked(true);
            
            // Store program
            programs.put(name, program);
            
            System.out.println("Shader program created: " + name);
            return program;
            
        } catch (Exception e) {
            System.err.println("Failed to create shader program " + name + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Load shader program from files
     */
    public ShaderProgram loadProgram(String name, String vertexPath, String fragmentPath) {
        try {
            String vertexSource = loadShaderSource(vertexPath);
            String fragmentSource = loadShaderSource(fragmentPath);
            
            return createProgram(name, vertexSource, fragmentSource);
            
        } catch (IOException e) {
            System.err.println("Failed to load shader files for " + name + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Compile a shader
     */
    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        
        // Check compilation status
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer success = stack.mallocInt(1);
            GL20.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, success);
            
            if (success.get(0) == GL11.GL_FALSE) {
                String infoLog = GL20.glGetShaderInfoLog(shader);
                GL20.glDeleteShader(shader);
                String shaderType = (type == GL20.GL_VERTEX_SHADER) ? "vertex" : "fragment";
                throw new RuntimeException("Shader compilation failed (" + shaderType + "): " + infoLog);
            }
        }
        
        return shader;
    }
    
    /**
     * Load shader source from file
     */
    private String loadShaderSource(String path) throws IOException {
        // Check cache first
        if (shaderSources.containsKey(path)) {
            return shaderSources.get(path);
        }
        
        StringBuilder source = new StringBuilder();
        
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IOException("Shader file not found: " + path);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    source.append(line).append("\n");
                }
            }
        }
        
        String sourceString = source.toString();
        shaderSources.put(path, sourceString);
        return sourceString;
    }
    
    /**
     * Get a shader program by name
     */
    public ShaderProgram getProgram(String name) {
        return programs.get(name);
    }
    
    /**
     * Use a shader program
     */
    public void useProgram(String name) {
        ShaderProgram program = programs.get(name);
        if (program == null) {
            throw new IllegalArgumentException("Shader program not found: " + name);
        }
        
        if (currentProgram != program) {
            program.use();
            currentProgram = program;
        }
    }
    
    /**
     * Get the current program
     */
    public ShaderProgram getCurrentProgram() {
        return currentProgram;
    }
    
    /**
     * Delete a shader program
     */
    public void deleteProgram(String name) {
        ShaderProgram program = programs.remove(name);
        if (program != null) {
            if (currentProgram == program) {
                currentProgram = null;
                GL20.glUseProgram(0);
            }
            GL20.glDeleteProgram(program.getProgramId());
            System.out.println("Shader program deleted: " + name);
        }
    }
    
    /**
     * Reload a shader program
     */
    public ShaderProgram reloadProgram(String name, String vertexPath, String fragmentPath) {
        // Remove from cache
        shaderSources.remove(vertexPath);
        shaderSources.remove(fragmentPath);
        
        // Delete existing program
        deleteProgram(name);
        
        // Load new program
        return loadProgram(name, vertexPath, fragmentPath);
    }
    
    /**
     * Get all program names
     */
    public String[] getProgramNames() {
        return programs.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if a program exists
     */
    public boolean hasProgram(String name) {
        return programs.containsKey(name);
    }
    
    /**
     * Clear all shader sources cache
     */
    public void clearSourceCache() {
        shaderSources.clear();
    }
    
    /**
     * Cleanup all resources
     */
    public void cleanup() {
        System.out.println("ShaderManager cleaning up...");
        
        // Delete all programs
        for (ShaderProgram program : programs.values()) {
            GL20.glDeleteProgram(program.getProgramId());
        }
        
        programs.clear();
        shaders.clear();
        shaderSources.clear();
        currentProgram = null;
        
        System.out.println("ShaderManager cleanup complete");
    }
    
    /**
     * Get shader manager statistics
     */
    public String getStatistics() {
        return String.format("Programs: %d, Cached Sources: %d, Current: %s", 
                programs.size(), shaderSources.size(), 
                currentProgram != null ? currentProgram.getName() : "none");
    }
    
    @Override
    public String toString() {
        return "ShaderManager[" + getStatistics() + "]";
    }
}