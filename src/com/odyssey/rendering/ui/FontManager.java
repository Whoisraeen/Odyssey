package com.odyssey.rendering.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages multiple bitmap fonts and provides a centralized font loading system.
 * Supports font atlasing for improved performance.
 */
public class FontManager {
    
    private static FontManager instance;
    private final Map<String, BitmapFont> fonts;
    private BitmapFont defaultFont;
    
    private FontManager() {
        this.fonts = new HashMap<>();
        loadDefaultFonts();
    }
    
    /**
     * Get the singleton instance of FontManager.
     */
    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }
    
    /**
     * Load default fonts for the engine.
     */
    private void loadDefaultFonts() {
        // Load the main bitmap font atlas
        try {
            BitmapFont mainFont = new BitmapFont(
                "resources/assets/textures/font_atlas.png", 
                256, 96,  // Atlas dimensions
                16,       // Line height
                12        // Baseline
            );
            
            fonts.put("default", mainFont);
            fonts.put("main", mainFont);
            defaultFont = mainFont;
            
            System.out.println("FontManager: Loaded default bitmap font atlas");
            
        } catch (Exception e) {
            System.err.println("FontManager: Failed to load default font: " + e.getMessage());
            
            // Create a fallback font with minimal functionality
            createFallbackFont();
        }
    }
    
    /**
     * Create a minimal fallback font when the main font fails to load.
     */
    private void createFallbackFont() {
        try {
            // Try to use the old font.png if it exists
            BitmapFont fallbackFont = new BitmapFont(
                "resources/textures/font.png", 
                256, 256, // Assume square atlas
                16,       // Line height
                12        // Baseline
            );
            
            fonts.put("default", fallbackFont);
            fonts.put("fallback", fallbackFont);
            defaultFont = fallbackFont;
            
            System.out.println("FontManager: Using fallback font");
            
        } catch (Exception e2) {
            System.err.println("FontManager: Failed to load fallback font: " + e2.getMessage());
            // At this point, text rendering will be disabled
        }
    }
    
    /**
     * Load a custom font from a file.
     * 
     * @param name Identifier for the font
     * @param atlasPath Path to the font atlas texture
     * @param atlasWidth Width of the atlas in pixels
     * @param atlasHeight Height of the atlas in pixels
     * @param lineHeight Height of a line of text
     * @param baseline Distance from top to baseline
     */
    public void loadFont(String name, String atlasPath, int atlasWidth, int atlasHeight, 
                        int lineHeight, int baseline) {
        try {
            BitmapFont font = new BitmapFont(atlasPath, atlasWidth, atlasHeight, lineHeight, baseline);
            fonts.put(name, font);
            System.out.println("FontManager: Loaded font '" + name + "' from " + atlasPath);
        } catch (Exception e) {
            System.err.println("FontManager: Failed to load font '" + name + "': " + e.getMessage());
        }
    }
    
    /**
     * Get a font by name.
     * 
     * @param name Font identifier
     * @return The requested font, or the default font if not found
     */
    public BitmapFont getFont(String name) {
        return fonts.getOrDefault(name, defaultFont);
    }
    
    /**
     * Get the default font.
     */
    public BitmapFont getDefaultFont() {
        return defaultFont;
    }
    
    /**
     * Check if a font exists.
     */
    public boolean hasFont(String name) {
        return fonts.containsKey(name);
    }
    
    /**
     * Remove a font from memory.
     */
    public void unloadFont(String name) {
        BitmapFont font = fonts.remove(name);
        if (font != null && font != defaultFont) {
            font.cleanup();
            System.out.println("FontManager: Unloaded font '" + name + "'");
        }
    }
    
    /**
     * Get all loaded font names.
     */
    public String[] getFontNames() {
        return fonts.keySet().toArray(new String[0]);
    }
    
    /**
     * Create a text renderer with the default font.
     */
    public TextRenderer createTextRenderer(int screenWidth, int screenHeight) {
        return createTextRenderer(screenWidth, screenHeight, "default");
    }
    
    /**
     * Create a text renderer with a specific font.
     */
    public TextRenderer createTextRenderer(int screenWidth, int screenHeight, String fontName) {
        BitmapFont font = getFont(fontName);
        if (font == null) {
            System.err.println("FontManager: Font '" + fontName + "' not found, using default");
            font = defaultFont;
        }
        
        if (font == null) {
            throw new RuntimeException("FontManager: No fonts available for text rendering");
        }
        
        return new TextRenderer(screenWidth, screenHeight, font);
    }
    
    /**
     * Reload all fonts (useful for resource pack changes).
     */
    public void reloadFonts() {
        System.out.println("FontManager: Reloading all fonts...");
        
        // Store font configurations
        Map<String, FontConfig> configs = new HashMap<>();
        for (Map.Entry<String, BitmapFont> entry : fonts.entrySet()) {
            // Note: In a real implementation, you'd store the original parameters
            // For now, we'll just reload the default fonts
        }
        
        // Clean up existing fonts
        for (BitmapFont font : fonts.values()) {
            if (font != null) {
                font.cleanup();
            }
        }
        fonts.clear();
        
        // Reload default fonts
        loadDefaultFonts();
        
        System.out.println("FontManager: Font reload complete");
    }
    
    /**
     * Clean up all fonts and resources.
     */
    public void cleanup() {
        System.out.println("FontManager: Cleaning up fonts...");
        
        for (BitmapFont font : fonts.values()) {
            if (font != null) {
                font.cleanup();
            }
        }
        fonts.clear();
        defaultFont = null;
        instance = null;
    }
    
    /**
     * Helper class to store font configuration for reloading.
     */
    private static class FontConfig {
        String atlasPath;
        int atlasWidth, atlasHeight;
        int lineHeight, baseline;
        
        FontConfig(String atlasPath, int atlasWidth, int atlasHeight, int lineHeight, int baseline) {
            this.atlasPath = atlasPath;
            this.atlasWidth = atlasWidth;
            this.atlasHeight = atlasHeight;
            this.lineHeight = lineHeight;
            this.baseline = baseline;
        }
    }
}