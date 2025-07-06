package com.odyssey;

import com.odyssey.rendering.ui.FontManager;
import com.odyssey.rendering.ui.TextRenderer;

public class FontTest {
    public static void main(String[] args) {
        try {
            System.out.println("Starting FontTest...");
            
            System.out.println("Getting FontManager instance...");
            FontManager fontManager = FontManager.getInstance();
            System.out.println("FontManager instance obtained successfully");
            
            System.out.println("Creating TextRenderer...");
            TextRenderer textRenderer = fontManager.createTextRenderer(800, 600);
            System.out.println("TextRenderer created successfully");
            
            System.out.println("FontTest completed successfully!");
            
        } catch (Exception e) {
            System.err.println("FontTest failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}