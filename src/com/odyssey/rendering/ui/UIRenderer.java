package com.odyssey.rendering.ui;

import com.odyssey.rendering.ShaderManager;
import com.odyssey.rendering.Texture;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

public class UIRenderer {

    private final int shaderProgram;
    private final int vao;
    private final int vbo;
    private FontManager fontManager;
    private TextRenderer textRenderer;
    private int screenWidth, screenHeight;

    public UIRenderer(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        
        ShaderManager shaderManager = new ShaderManager();
        this.shaderProgram = shaderManager.loadProgram("resources/shaders/ui.vert", "resources/shaders/ui.frag");
        Matrix4f projection = new Matrix4f().ortho(0.0f, width, height, 0.0f, -1.0f, 1.0f);
        ShaderManager.setUniform(shaderProgram, "projection", projection);

        // Initialize font system
        fontManager = FontManager.getInstance();
        
        try {
            textRenderer = fontManager.createTextRenderer(screenWidth, screenHeight);
            System.out.println("UIRenderer: Text rendering system initialized");
        } catch (Exception e) {
            System.err.println("UIRenderer: Failed to initialize text renderer: " + e.getMessage());
            textRenderer = null;
        }

        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
    }
    
    public void drawText(String text, float x, float y, float scale, Vector3f color) {
        if (textRenderer != null) {
            textRenderer.drawText(text, x, y, scale, color);
        } else {
            System.err.println("UIRenderer: Text renderer not available, cannot render text: " + text);
        }
    }
    
    public void drawText(String text, float x, float y, float scale) {
        if (textRenderer != null) {
            // Use white color as default
            textRenderer.drawText(text, x, y, scale, new Vector3f(1.0f, 1.0f, 1.0f));
        } else {
            System.err.println("UIRenderer: Text renderer not available, cannot render text: " + text);
        }
    }

    public void draw(int textureId, float x, float y, int width, int height, int rotation, Vector3f color) {
        glUseProgram(shaderProgram);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        ShaderManager.setUniform(shaderProgram, "spriteColor", color);
        
        drawTexture(x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f);
    }
    
    public void drawTexture(float x, float y, float width, float height, float u, float v, float uWidth, float vHeight) {
        float[] vertices = {
            // pos      // tex
            x, y + height, u, v + vHeight,
            x, y, u, v,
            x + width, y, u + uWidth, v,

            x, y + height, u, v + vHeight,
            x + width, y, u + uWidth, v,
            x + width, y + height, u + uWidth, v + vHeight
        };

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
    
    public void drawRect(float x, float y, float width, float height, int color) {
        // Convert int color to Vector3f
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        Vector3f colorVec = new Vector3f(r, g, b);
        
        glUseProgram(shaderProgram);
        
        // Disable texture binding for solid color rendering
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        ShaderManager.setUniform(shaderProgram, "spriteColor", colorVec);
        
        float[] vertices = {
            // pos only (no texture coordinates needed for solid color)
            x, y + height, 0.0f, 0.0f,
            x, y, 0.0f, 0.0f,
            x + width, y, 0.0f, 0.0f,

            x, y + height, 0.0f, 0.0f,
            x + width, y, 0.0f, 0.0f,
            x + width, y + height, 0.0f, 0.0f
        };

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    /**
     * Update screen dimensions (useful for window resizing).
     */
    public void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        
        if (textRenderer != null) {
            textRenderer.updateScreenSize(width, height);
        }
    }
    
    /**
     * Get the width of text in pixels.
     */
    public float getTextWidth(String text, float scale) {
        if (textRenderer != null) {
            return textRenderer.getTextWidth(text, scale);
        }
        return 0.0f;
    }
    
    /**
     * Get the height of text in pixels.
     */
    public float getTextHeight(float scale) {
        if (textRenderer != null) {
            return textRenderer.getTextHeight(scale);
        }
        return 0.0f;
    }
    
    /**
     * Check if text rendering is available.
     */
    public boolean isTextRenderingAvailable() {
        return textRenderer != null;
    }
    
    /**
     * Get the font manager instance.
     */
    public FontManager getFontManager() {
        return fontManager;
    }
    
    /**
     * Switch to a different font.
     */
    public void setFont(String fontName) {
        if (fontManager != null && fontManager.hasFont(fontName)) {
            if (textRenderer != null) {
                textRenderer.cleanup();
            }
            
            try {
                textRenderer = fontManager.createTextRenderer(screenWidth, screenHeight, fontName);
                System.out.println("UIRenderer: Switched to font: " + fontName);
            } catch (Exception e) {
                System.err.println("UIRenderer: Failed to switch to font '" + fontName + "': " + e.getMessage());
                // Try to fall back to default font
                try {
                    textRenderer = fontManager.createTextRenderer(screenWidth, screenHeight, "default");
                } catch (Exception e2) {
                    System.err.println("UIRenderer: Failed to fall back to default font: " + e2.getMessage());
                    textRenderer = null;
                }
            }
        } else {
            System.err.println("UIRenderer: Font '" + fontName + "' not found");
        }
    }
    
    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        
        // Cleanup text renderer
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        
        // Note: FontManager cleanup is handled globally, not per UIRenderer instance
    }
}