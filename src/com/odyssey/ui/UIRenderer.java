package com.odyssey.ui;

import com.odyssey.rendering.ui.TextRenderer;
import com.odyssey.ui.graphics.CommandBuffer;
import com.odyssey.ui.graphics.RenderContext;
import com.odyssey.ui.graphics.ShaderManager;
import com.odyssey.ui.graphics.Surface;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Stack;

/**
 * Modern UI Renderer with advanced graphics capabilities and command recording
 * Handles rendering of UI components with proper layering, transparency, and effects
 */
public class UIRenderer {
    
    private final TextRenderer textRenderer;
    
    // Graphics components
    private ShaderManager shaderManager;
    private RenderContext renderContext;
    private CommandBuffer commandBuffer;
    private Surface mainSurface;
    
    // OpenGL resources
    private int shaderProgram;
    private int vao, vbo, ebo;
    
    // Shader uniforms
    private int uProjection, uModel, uColor, uTexture, uCornerRadius, uSize;
    
    // Rendering state
    private final Stack<RenderState> stateStack = new Stack<>();
    private RenderState currentState;
    private boolean isRecording = false;
    
    // Vertex data for rectangles
    private static final float[] RECT_VERTICES = {
        // Position     // TexCoord
        0.0f, 1.0f,     0.0f, 1.0f,  // Top-left
        1.0f, 1.0f,     1.0f, 1.0f,  // Top-right
        1.0f, 0.0f,     1.0f, 0.0f,  // Bottom-right
        0.0f, 0.0f,     0.0f, 0.0f   // Bottom-left
    };
    
    private static final int[] RECT_INDICES = {
        0, 1, 2,
        2, 3, 0
    };
    
    // Shader sources
    private static final String VERTEX_SHADER = 
        "#version 330 core\n" +
        "layout (location = 0) in vec2 aPos;\n" +
        "layout (location = 1) in vec2 aTexCoord;\n" +
        "\n" +
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uModel;\n" +
        "\n" +
        "out vec2 TexCoord;\n" +
        "out vec2 FragPos;\n" +
        "\n" +
        "void main() {\n" +
        "    gl_Position = uProjection * uModel * vec4(aPos, 0.0, 1.0);\n" +
        "    TexCoord = aTexCoord;\n" +
        "    FragPos = aPos;\n" +
        "}\n";
    
    private static final String FRAGMENT_SHADER = 
        "#version 330 core\n" +
        "in vec2 TexCoord;\n" +
        "in vec2 FragPos;\n" +
        "\n" +
        "uniform vec4 uColor;\n" +
        "uniform sampler2D uTexture;\n" +
        "uniform float uCornerRadius;\n" +
        "uniform vec2 uSize;\n" +
        "\n" +
        "out vec4 FragColor;\n" +
        "\n" +
        "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
        "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
        "}\n" +
        "\n" +
        "void main() {\n" +
        "    vec4 color = uColor;\n" +
        "    \n" +
        "    // Apply texture if available\n" +
        "    if (textureSize(uTexture, 0).x > 1) {\n" +
        "        vec4 texColor = texture(uTexture, TexCoord);\n" +
        "        color = mix(color, texColor, texColor.a);\n" +
        "    }\n" +
        "    \n" +
        "    // Apply rounded corners\n" +
        "    if (uCornerRadius > 0.0) {\n" +
        "        vec2 centerPos = (FragPos - 0.5) * uSize;\n" +
        "        float distance = roundedBoxSDF(centerPos, uSize * 0.5, uCornerRadius);\n" +
        "        float alpha = 1.0 - smoothstep(-1.0, 1.0, distance);\n" +
        "        color.a *= alpha;\n" +
        "    }\n" +
        "    \n" +
        "    FragColor = color;\n" +
        "}\n";
    
    private static class RenderState {
        float[] projectionMatrix = new float[16];
        float[] modelMatrix = new float[16];
        float alpha = 1.0f;
        
        RenderState() {
            // Initialize as identity matrices
            setIdentity(projectionMatrix);
            setIdentity(modelMatrix);
        }
        
        RenderState(RenderState other) {
            System.arraycopy(other.projectionMatrix, 0, projectionMatrix, 0, 16);
            System.arraycopy(other.modelMatrix, 0, modelMatrix, 0, 16);
            this.alpha = other.alpha;
        }
        
        private void setIdentity(float[] matrix) {
            for (int i = 0; i < 16; i++) {
                matrix[i] = (i % 5 == 0) ? 1.0f : 0.0f;
            }
        }
    }
    
    public UIRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
        this.currentState = new RenderState();
        initializeGraphics();
    }
    
    private void initializeGraphics() {
        // Initialize graphics components
        shaderManager = ShaderManager.getInstance();
        shaderManager.initialize();
        
        renderContext = RenderContext.getInstance();
        renderContext.initialize();
        
        commandBuffer = new CommandBuffer();
        
        initializeOpenGL();
    }
    
    private void initializeOpenGL() {
        try {
            // Create and compile shaders
            int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            
            // Create shader program
            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vertexShader);
            GL20.glAttachShader(shaderProgram, fragmentShader);
            GL20.glLinkProgram(shaderProgram);
            
            // Check for linking errors
            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(shaderProgram);
                throw new RuntimeException("Shader program linking failed: " + log);
            }
            
            // Clean up individual shaders
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            
            // Get uniform locations
            uProjection = GL20.glGetUniformLocation(shaderProgram, "uProjection");
            uModel = GL20.glGetUniformLocation(shaderProgram, "uModel");
            uColor = GL20.glGetUniformLocation(shaderProgram, "uColor");
            uTexture = GL20.glGetUniformLocation(shaderProgram, "uTexture");
            uCornerRadius = GL20.glGetUniformLocation(shaderProgram, "uCornerRadius");
            uSize = GL20.glGetUniformLocation(shaderProgram, "uSize");
            
            // Create VAO, VBO, EBO
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            ebo = GL15.glGenBuffers();
            
            GL30.glBindVertexArray(vao);
            
            // Upload vertex data
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, RECT_VERTICES, GL15.GL_STATIC_DRAW);
            
            // Upload index data
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, RECT_INDICES, GL15.GL_STATIC_DRAW);
            
            // Set vertex attributes
            // Position attribute
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
            GL20.glEnableVertexAttribArray(0);
            
            // Texture coordinate attribute
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);
            
            GL30.glBindVertexArray(0);
            
            System.out.println("UIRenderer: OpenGL resources initialized successfully");
            
        } catch (Exception e) {
            System.err.println("UIRenderer: Failed to initialize OpenGL resources - " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + log);
        }
        
        return shader;
    }
    
    public void beginFrame() {
        // Begin command recording
        isRecording = true;
        commandBuffer.begin();
        
        // Setup OpenGL state for UI rendering
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        // Use our shader program
        GL20.glUseProgram(shaderProgram);
        GL30.glBindVertexArray(vao);
        
        // Set default texture unit
        GL20.glUniform1i(uTexture, 0);
    }
    
    public void endFrame() {
        // End command recording and execute
        if (isRecording) {
            commandBuffer.end();
            commandBuffer.execute(renderContext);
            isRecording = false;
        }
        
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
        
        // Restore OpenGL state
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
    
    public void setProjectionMatrix(float[] matrix) {
        System.arraycopy(matrix, 0, currentState.projectionMatrix, 0, 16);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            buffer.put(matrix).flip();
            GL20.glUniformMatrix4fv(uProjection, false, buffer);
        }
    }
    
    public void pushMatrix() {
        stateStack.push(new RenderState(currentState));
    }
    
    public void popMatrix() {
        if (!stateStack.isEmpty()) {
            currentState = stateStack.pop();
            updateUniforms();
        }
    }
    
    public void translate(float x, float y) {
        // Apply translation to model matrix
        currentState.modelMatrix[12] += x;
        currentState.modelMatrix[13] += y;
        updateModelMatrix();
    }
    
    public void scale(float x, float y) {
        // Apply scale to model matrix
        currentState.modelMatrix[0] *= x;
        currentState.modelMatrix[5] *= y;
        updateModelMatrix();
    }
    
    public void setAlpha(float alpha) {
        currentState.alpha = alpha;
    }
    
    private void updateUniforms() {
        updateModelMatrix();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            buffer.put(currentState.projectionMatrix).flip();
            GL20.glUniformMatrix4fv(uProjection, false, buffer);
        }
    }
    
    private void updateModelMatrix() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            buffer.put(currentState.modelMatrix).flip();
            GL20.glUniformMatrix4fv(uModel, false, buffer);
        }
    }
    
    public void drawRect(float x, float y, float width, float height, int color) {
        drawRect(x, y, width, height, color, 0.0f);
    }
    
    public void drawRect(float x, float y, float width, float height, int color, float cornerRadius) {
        if (isRecording) {
            // Record draw command
            commandBuffer.recordDrawRect(x, y, width, height, color, cornerRadius);
        } else {
            // Immediate rendering fallback
            drawRectImmediate(x, y, width, height, color, cornerRadius);
        }
    }
    
    private void drawRectImmediate(float x, float y, float width, float height, int color, float cornerRadius) {
        pushMatrix();
        
        // Set position and size
        translate(x, y);
        scale(width, height);
        
        // Set color with alpha
        float alpha = ((color >> 24) & 0xFF) / 255.0f * currentState.alpha;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        
        GL20.glUniform4f(uColor, red, green, blue, alpha);
        GL20.glUniform1f(uCornerRadius, cornerRadius);
        GL20.glUniform2f(uSize, width, height);
        
        // Draw
        GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
        
        popMatrix();
    }
    
    public void drawText(String text, float x, float y, int color) {
        if (isRecording) {
            // Record text draw command
            commandBuffer.recordDrawText(text, x, y, color);
        } else {
            // Immediate rendering fallback
            drawTextImmediate(text, x, y, color);
        }
    }
    
    private void drawTextImmediate(String text, float x, float y, int color) {
        if (textRenderer != null) {
            // Apply current alpha to text color
            float alpha = ((color >> 24) & 0xFF) / 255.0f * currentState.alpha;
            int alphaInt = (int) (alpha * 255) << 24;
            int finalColor = (color & 0x00FFFFFF) | alphaInt;
            
            textRenderer.drawText(text, x, y, finalColor);
        }
    }
    
    public void drawText(String text, float x, float y, int color, float scale) {
        if (textRenderer != null) {
            pushMatrix();
            translate(x, y);
            scale(scale, scale);
            
            // Apply current alpha to text color
            float alpha = ((color >> 24) & 0xFF) / 255.0f * currentState.alpha;
            int alphaInt = (int) (alpha * 255) << 24;
            int finalColor = (color & 0x00FFFFFF) | alphaInt;
            
            textRenderer.drawText(text, 0, 0, finalColor);
            popMatrix();
        }
    }
    
    public float getTextWidth(String text) {
        return textRenderer != null ? textRenderer.getTextWidth(text) : 0;
    }
    
    public float getTextHeight() {
        return textRenderer != null ? textRenderer.getTextHeight() : 0;
    }
    
    public void drawLine(float x1, float y1, float x2, float y2, int color, float thickness) {
        // Calculate line direction and perpendicular
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        // Normalize direction
        dx /= length;
        dy /= length;
        
        // Calculate perpendicular (for thickness)
        float px = -dy * thickness * 0.5f;
        float py = dx * thickness * 0.5f;
        
        // Draw line as a rotated rectangle
        pushMatrix();
        translate(x1, y1);
        
        // This is a simplified line drawing - in a real implementation,
        // you'd want to use a proper line shader or geometry
        drawRect(0, -thickness * 0.5f, length, thickness, color);
        
        popMatrix();
    }
    
    public void drawCircle(float centerX, float centerY, float radius, int color) {
        // Draw circle as a square with rounded corners equal to radius
        drawRect(centerX - radius, centerY - radius, radius * 2, radius * 2, color, radius);
    }
    
    public void drawRectOutline(float x, float y, float width, float height, int color, int thickness) {
        // Draw outline as four rectangles forming a border
        drawRect(x, y, width, thickness, color); // Top
        drawRect(x, y + height - thickness, width, thickness, color); // Bottom
        drawRect(x, y, thickness, height, color); // Left
        drawRect(x + width - thickness, y, thickness, height, color); // Right
    }
    
    public void drawRoundedRect(float x, float y, float width, float height, float cornerRadius, int color) {
        drawRect(x, y, width, height, color, cornerRadius);
    }
    
    public void drawRoundedRectOutline(float x, float y, float width, float height, float cornerRadius, int color, int thickness) {
        // For rounded rectangles, we'll draw a filled rounded rect and then a smaller one inside to create outline effect
        // This is a simplified approach - a proper implementation would use more sophisticated geometry
        drawRoundedRect(x, y, width, height, cornerRadius, color);
        if (width > thickness * 2 && height > thickness * 2) {
            // Draw inner rect with background color to create outline effect
            // Note: This assumes background is transparent - in a real implementation you'd need the actual background color
            drawRoundedRect(x + thickness, y + thickness, width - thickness * 2, height - thickness * 2, 
                          Math.max(0, cornerRadius - thickness), 0x00000000);
        }
    }
    
    public void setClipRect(float x, float y, float width, float height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) x, (int) y, (int) width, (int) height);
    }
    
    public void clearClipRect() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    
    private final Stack<ClipRect> clipStack = new Stack<>();
    private boolean clipping = false;
    
    private static class ClipRect {
        float x, y, width, height;
        ClipRect(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }
    
    public boolean isClipping() {
        return clipping;
    }
    
    public void pushClip(float x, float y, float width, float height) {
        clipStack.push(new ClipRect(x, y, width, height));
        setClipRect(x, y, width, height);
        clipping = true;
    }
    
    public void popClip() {
        if (!clipStack.isEmpty()) {
            clipStack.pop();
            if (!clipStack.isEmpty()) {
                ClipRect clip = clipStack.peek();
                setClipRect(clip.x, clip.y, clip.width, clip.height);
            } else {
                clearClipRect();
                clipping = false;
            }
        }
    }
    
    public void cleanup() {
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
        }
        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
        }
        if (ebo != 0) {
            GL15.glDeleteBuffers(ebo);
        }
        if (shaderProgram != 0) {
            GL20.glDeleteProgram(shaderProgram);
        }
    }
}