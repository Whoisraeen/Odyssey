package com.odyssey.rendering.ui;

import com.odyssey.rendering.Texture;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * A bitmap font system that uses texture atlases for efficient text rendering.
 * Supports variable-width characters, proper spacing, and text effects.
 */
public class BitmapFont {
    
    public static class Glyph {
        public float u, v;           // UV coordinates in atlas
        public float width, height;  // Size in atlas (normalized)
        public float advance;        // How much to advance cursor
        public float bearingX, bearingY; // Offset from baseline
        public int pixelWidth, pixelHeight; // Actual pixel dimensions
        
        public Glyph(float u, float v, float width, float height, float advance, 
                    float bearingX, float bearingY, int pixelWidth, int pixelHeight) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.advance = advance;
            this.bearingX = bearingX;
            this.bearingY = bearingY;
            this.pixelWidth = pixelWidth;
            this.pixelHeight = pixelHeight;
        }
    }
    
    private final Texture fontAtlas;
    private final Map<Character, Glyph> glyphs;
    private final int atlasWidth, atlasHeight;
    private final int lineHeight;
    private final int baseline;
    
    /**
     * Creates a bitmap font from a texture atlas.
     * @param atlasPath Path to the font atlas texture
     * @param atlasWidth Width of the atlas in pixels
     * @param atlasHeight Height of the atlas in pixels
     * @param lineHeight Height of a line of text
     * @param baseline Distance from top to baseline
     */
    public BitmapFont(String atlasPath, int atlasWidth, int atlasHeight, int lineHeight, int baseline) {
        this.fontAtlas = new Texture(atlasPath);
        this.glyphs = new HashMap<>();
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.lineHeight = lineHeight;
        this.baseline = baseline;
        
        // Initialize ASCII character set (32-126)
        initializeASCIIGlyphs();
    }
    
    /**
     * Initialize glyphs for standard ASCII characters in a 16x6 grid layout.
     * This assumes a standard bitmap font layout with 95 printable ASCII characters.
     */
    private void initializeASCIIGlyphs() {
        int charsPerRow = 16;
        int charWidth = atlasWidth / charsPerRow;
        int charHeight = atlasHeight / 6; // 6 rows for 95 characters
        
        for (int i = 0; i < 95; i++) {
            char c = (char) (32 + i); // Start from space character (32)
            
            int row = i / charsPerRow;
            int col = i % charsPerRow;
            
            float u = (float) (col * charWidth) / atlasWidth;
            float v = (float) (row * charHeight) / atlasHeight;
            float width = (float) charWidth / atlasWidth;
            float height = (float) charHeight / atlasHeight;
            
            // Calculate character-specific metrics
            float advance = getCharacterAdvance(c, charWidth);
            float bearingX = getCharacterBearingX(c);
            float bearingY = getCharacterBearingY(c, charHeight);
            
            glyphs.put(c, new Glyph(u, v, width, height, advance, bearingX, bearingY, charWidth, charHeight));
        }
    }
    
    /**
     * Get the advance width for a specific character.
     * This can be customized for variable-width fonts.
     */
    private float getCharacterAdvance(char c, int defaultWidth) {
        // For now, use character-specific advances for better typography
        switch (c) {
            case ' ': return defaultWidth * 0.5f;
            case 'i': case 'l': case 'I': case '|': case '!': return defaultWidth * 0.4f;
            case 'f': case 't': case 'r': return defaultWidth * 0.6f;
            case 'w': case 'W': case 'M': case 'm': return defaultWidth * 1.2f;
            case '.': case ',': case ':': case ';': return defaultWidth * 0.3f;
            default: return defaultWidth * 0.8f;
        }
    }
    
    /**
     * Get horizontal bearing (offset from left edge) for a character.
     */
    private float getCharacterBearingX(char c) {
        // Most characters start at the left edge
        return 0.0f;
    }
    
    /**
     * Get vertical bearing (offset from baseline) for a character.
     */
    private float getCharacterBearingY(char c, int charHeight) {
        // Adjust for characters that extend below baseline
        switch (c) {
            case 'g': case 'j': case 'p': case 'q': case 'y':
                return charHeight * 0.2f; // Descenders
            default:
                return 0.0f;
        }
    }
    
    /**
     * Get glyph information for a character.
     */
    public Glyph getGlyph(char c) {
        return glyphs.getOrDefault(c, glyphs.get(' ')); // Fallback to space
    }
    
    /**
     * Calculate the width of a text string.
     */
    public float getTextWidth(String text, float scale) {
        float width = 0.0f;
        for (char c : text.toCharArray()) {
            Glyph glyph = getGlyph(c);
            width += glyph.advance * scale;
        }
        return width;
    }
    
    /**
     * Calculate the height of text (line height).
     */
    public float getTextHeight(float scale) {
        return lineHeight * scale;
    }
    
    /**
     * Get the font's baseline offset.
     */
    public float getBaseline(float scale) {
        return baseline * scale;
    }
    
    /**
     * Bind the font atlas texture.
     */
    public void bind() {
        fontAtlas.bind();
    }
    
    /**
     * Get the font atlas texture ID.
     */
    public int getTextureId() {
        return fontAtlas.getId();
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        fontAtlas.cleanup();
    }
}