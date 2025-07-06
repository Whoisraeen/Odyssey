package com.odyssey;

import com.odyssey.rendering.Texture;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class SimpleFontTest {
    public static void main(String[] args) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("font_test_output.txt"))) {
            writer.println("SimpleFontTest: Starting texture loading test...");
            System.out.println("SimpleFontTest: Starting texture loading test...");
            
            try {
                writer.println("SimpleFontTest: Attempting to load texture from assets/font_atlas.png_0.png");
                System.out.println("SimpleFontTest: Attempting to load texture from assets/font_atlas.png_0.png");
                
                Texture texture = new Texture("assets/font_atlas.png_0.png");
                
                writer.println("SimpleFontTest: Texture loaded successfully! ID: " + texture.getId());
                System.out.println("SimpleFontTest: Texture loaded successfully! ID: " + texture.getId());
                
            } catch (Exception e) {
                writer.println("SimpleFontTest: Texture loading failed: " + e.getMessage());
                System.err.println("SimpleFontTest: Texture loading failed: " + e.getMessage());
                e.printStackTrace(writer);
                e.printStackTrace();
            }
            
            writer.flush();
            
        } catch (IOException e) {
            System.err.println("Failed to write to output file: " + e.getMessage());
        }
    }
}