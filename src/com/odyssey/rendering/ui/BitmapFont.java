package com.odyssey.rendering.ui;

import com.odyssey.rendering.Texture;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.*;
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
        System.out.println("BitmapFont: Initializing with atlas: " + atlasPath);
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.lineHeight = lineHeight;
        this.baseline = baseline;
        this.glyphs = new HashMap<>();
        
        // Load the texture atlas
        System.out.println("BitmapFont: Loading texture from: " + atlasPath);
        try {
            this.fontAtlas = new Texture(atlasPath);
            System.out.println("BitmapFont: Texture loaded successfully");
        } catch (Exception e) {
            System.err.println("BitmapFont: Failed to load texture: " + e.getMessage());
            throw e;
        }
        
        // Load font data from .fnt file
        String fntPath = atlasPath.replace(".png", ".fnt");
        if (fntPath.contains("_0.png")) {
            fntPath = fntPath.replace("_0.fnt", ".fnt");
        }
        
        System.out.println("BitmapFont: Attempting to load font data from: " + fntPath);
        try {
            loadFontData(fntPath);
            System.out.println("BitmapFont loaded: " + glyphs.size() + " glyphs from " + fntPath);
        } catch (Exception e) {
            System.err.println("Failed to load .fnt file: " + fntPath + ", falling back to ASCII grid");
            e.printStackTrace();
            // Fallback to ASCII initialization
            System.out.println("BitmapFont: Using ASCII fallback");
            initializeASCIIGlyphs();
        }
    }
    
    private void loadFontData(String fntPath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fntPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            if (is == null) {
                throw new IOException("Font file not found: " + fntPath);
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("char id=")) {
                    parseCharLine(line);
                }
            }
        }
    }
    
    private void parseCharLine(String line) {
        // Parse: char id=65   x=101   y=0    width=11    height=10    xoffset=-1    yoffset=3     xadvance=9     page=0  chnl=15
        String[] parts = line.split("\\s+");
        
        int id = 0, x = 0, y = 0, width = 0, height = 0, xoffset = 0, yoffset = 0, xadvance = 0;
        
        for (String part : parts) {
            if (part.startsWith("id=")) {
                id = Integer.parseInt(part.substring(3));
            } else if (part.startsWith("x=")) {
                x = Integer.parseInt(part.substring(2));
            } else if (part.startsWith("y=")) {
                y = Integer.parseInt(part.substring(2));
            } else if (part.startsWith("width=")) {
                width = Integer.parseInt(part.substring(6));
            } else if (part.startsWith("height=")) {
                height = Integer.parseInt(part.substring(7));
            } else if (part.startsWith("xoffset=")) {
                xoffset = Integer.parseInt(part.substring(8));
            } else if (part.startsWith("yoffset=")) {
                yoffset = Integer.parseInt(part.substring(8));
            } else if (part.startsWith("xadvance=")) {
                xadvance = Integer.parseInt(part.substring(9));
            }
        }
        
        // Convert to normalized UV coordinates
        float u = (float) x / atlasWidth;
        float v = (float) y / atlasHeight;
        float normWidth = (float) width / atlasWidth;
        float normHeight = (float) height / atlasHeight;
        
        char character = (char) id;
        Glyph glyph = new Glyph(u, v, normWidth, normHeight, xadvance, xoffset, yoffset, width, height);
        glyphs.put(character, glyph);
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