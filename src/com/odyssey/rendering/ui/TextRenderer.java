package com.odyssey.rendering.ui;

import com.odyssey.rendering.ShaderManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Advanced text renderer with support for bitmap fonts, text effects, and proper typography.
 */
public class TextRenderer {
    
    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }
    
    public static class TextStyle {
        
        public enum Alignment {
            LEFT, CENTER, RIGHT, TOP, MIDDLE, BOTTOM,
            TOP_LEFT, TOP_CENTER, TOP_RIGHT,
            MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
            BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
        }
        
        public Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        public boolean bold = false;
        public boolean shadow = true;
        public Vector3f shadowColor = new Vector3f(0.25f, 0.25f, 0.25f);
        public float shadowOffsetX = 1.0f;
        public float shadowOffsetY = 1.0f;
        public TextAlign alignment = TextAlign.LEFT;
        
        public TextStyle() {}
        
        public TextStyle(Vector3f color) {
            this.color = new Vector3f(color);
        }
        
        public TextStyle bold() {
            this.bold = true;
            return this;
        }
        
        public TextStyle shadow(Vector3f shadowColor, float offsetX, float offsetY) {
            this.shadow = true;
            this.shadowColor = new Vector3f(shadowColor);
            this.shadowOffsetX = offsetX;
            this.shadowOffsetY = offsetY;
            return this;
        }
        
        public TextStyle align(TextAlign alignment) {
            this.alignment = alignment;
            return this;
        }
    }
    
    private final int shaderProgram;
    private final int vao;
    private final int vbo;
    private final BitmapFont font;
    
    // Batch rendering for performance
    private static final int MAX_QUADS = 1000;
    private static final int VERTICES_PER_QUAD = 6;
    private static final int FLOATS_PER_VERTEX = 4; // x, y, u, v
    private final float[] vertexBuffer;
    private int quadCount;
    
    public TextRenderer(int screenWidth, int screenHeight, BitmapFont font) {
        this.font = font;
        this.vertexBuffer = new float[MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];
        this.quadCount = 0;
        
        // Load shaders
        ShaderManager shaderManager = new ShaderManager();
        this.shaderProgram = shaderManager.loadProgram("shaders/ui.vert", "shaders/ui.frag");
        
        // Set up projection matrix
        Matrix4f projection = new Matrix4f().ortho(0.0f, screenWidth, screenHeight, 0.0f, -1.0f, 1.0f);
        glUseProgram(shaderProgram);
        ShaderManager.setUniform(shaderProgram, "projection", projection);
        
        // Set up VAO and VBO for batched rendering
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);
        
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.length * Float.BYTES, GL_DYNAMIC_DRAW);
        
        // Position attribute (location 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute (location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    /**
     * Render text with the specified style.
     */
    public void drawText(String text, float x, float y, float scale, TextStyle style) {
        if (text == null || text.isEmpty()) return;
        
        // Calculate alignment offset
        float alignmentOffset = calculateAlignmentOffset(text, scale, style.alignment);
        float renderX = x + alignmentOffset;
        
        // Render shadow first if enabled
        if (style.shadow) {
            drawTextInternal(text, renderX + style.shadowOffsetX * scale, 
                           y + style.shadowOffsetY * scale, scale, style.shadowColor, false);
        }
        
        // Render main text
        drawTextInternal(text, renderX, y, scale, style.color, style.bold);
        
        // Flush any remaining quads
        flush();
    }
    
    /**
     * Simple text rendering with default style.
     */
    public void drawText(String text, float x, float y, float scale) {
        drawText(text, x, y, scale, new TextStyle());
    }
    
    /**
     * Simple text rendering with color.
     */
    public void drawText(String text, float x, float y, float scale, Vector3f color) {
        drawText(text, x, y, scale, new TextStyle(color));
    }
    
    /**
     * Internal text rendering method.
     */
    private void drawTextInternal(String text, float x, float y, float scale, Vector3f color, boolean bold) {
        glUseProgram(shaderProgram);
        font.bind();
        ShaderManager.setUniform(shaderProgram, "spriteColor", color);
        
        float currentX = x;
        float currentY = y;
        
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                currentX = x;
                currentY += font.getTextHeight(scale);
                continue;
            }
            
            BitmapFont.Glyph glyph = font.getGlyph(c);
            if (glyph == null) continue;
            
            float glyphX = currentX + glyph.bearingX * scale;
            float glyphY = currentY + glyph.bearingY * scale;
            float glyphWidth = glyph.pixelWidth * scale;
            float glyphHeight = glyph.pixelHeight * scale;
            
            // Add quad to batch
            addQuad(glyphX, glyphY, glyphWidth, glyphHeight, 
                   glyph.u, glyph.v, glyph.width, glyph.height);
            
            // Render bold by drawing again with slight offset
            if (bold) {
                addQuad(glyphX + scale, glyphY, glyphWidth, glyphHeight, 
                       glyph.u, glyph.v, glyph.width, glyph.height);
            }
            
            currentX += glyph.advance * scale;
            
            // Flush if buffer is full
            if (quadCount >= MAX_QUADS - (bold ? 2 : 1)) {
                flush();
            }
        }
    }
    
    /**
     * Add a quad to the vertex buffer for batch rendering.
     */
    private void addQuad(float x, float y, float width, float height, 
                        float u, float v, float uWidth, float vHeight) {
        int index = quadCount * VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
        
        // Triangle 1
        // Bottom-left
        vertexBuffer[index++] = x;
        vertexBuffer[index++] = y + height;
        vertexBuffer[index++] = u;
        vertexBuffer[index++] = v + vHeight;
        
        // Top-left
        vertexBuffer[index++] = x;
        vertexBuffer[index++] = y;
        vertexBuffer[index++] = u;
        vertexBuffer[index++] = v;
        
        // Top-right
        vertexBuffer[index++] = x + width;
        vertexBuffer[index++] = y;
        vertexBuffer[index++] = u + uWidth;
        vertexBuffer[index++] = v;
        
        // Triangle 2
        // Bottom-left
        vertexBuffer[index++] = x;
        vertexBuffer[index++] = y + height;
        vertexBuffer[index++] = u;
        vertexBuffer[index++] = v + vHeight;
        
        // Top-right
        vertexBuffer[index++] = x + width;
        vertexBuffer[index++] = y;
        vertexBuffer[index++] = u + uWidth;
        vertexBuffer[index++] = v;
        
        // Bottom-right
        vertexBuffer[index++] = x + width;
        vertexBuffer[index++] = y + height;
        vertexBuffer[index++] = u + uWidth;
        vertexBuffer[index++] = v + vHeight;
        
        quadCount++;
    }
    
    /**
     * Flush the current batch of quads to the GPU.
     */
    private void flush() {
        if (quadCount == 0) return;
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
        
        glDrawArrays(GL_TRIANGLES, 0, quadCount * VERTICES_PER_QUAD);
        
        quadCount = 0;
        glBindVertexArray(0);
    }
    
    /**
     * Calculate alignment offset for text.
     */
    private float calculateAlignmentOffset(String text, float scale, TextAlign alignment) {
        if (alignment == TextAlign.LEFT) return 0.0f;
        
        float textWidth = font.getTextWidth(text, scale);
        
        switch (alignment) {
            case CENTER:
                return -textWidth / 2.0f;
            case RIGHT:
                return -textWidth;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Get the width of a text string.
     */
    public float getTextWidth(String text, float scale) {
        return font.getTextWidth(text, scale);
    }
    
    /**
     * Get the height of text.
     */
    public float getTextHeight(float scale) {
        return font.getTextHeight(scale);
    }
    
    /**
     * Update screen dimensions for projection matrix.
     */
    public void updateScreenSize(int width, int height) {
        Matrix4f projection = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
        glUseProgram(shaderProgram);
        ShaderManager.setUniform(shaderProgram, "projection", projection);
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        font.cleanup();
    }
}