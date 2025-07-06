package com.odyssey;

import com.odyssey.rendering.ui.FontManager;
import com.odyssey.rendering.ui.BitmapFont;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class FontTest {
    private long window;
    
    public static void main(String[] args) {
        new FontTest().run();
    }
    
    public void run() {
        System.out.println("FontTest: Starting font loading test...");
        
        init();
        testFontLoading();
        cleanup();
    }
    
    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Create the window
        window = glfwCreateWindow(800, 600, "Font Test", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        System.out.println("FontTest: OpenGL context initialized");
    }
    
    private void testFontLoading() {
        System.out.println("FontTest: Testing font loading...");
        
        try {
            // Test direct BitmapFont loading first
            System.out.println("FontTest: Testing direct BitmapFont loading...");
            BitmapFont font = new BitmapFont("assets/font_atlas.png_0.png", 512, 512, 16, 13);
            System.out.println("FontTest: BitmapFont created successfully");
            
            // Test FontManager initialization
            System.out.println("FontTest: Testing FontManager singleton...");
            FontManager fontManager = FontManager.getInstance();
            System.out.println("FontTest: FontManager created successfully");
            
        } catch (Exception e) {
            System.err.println("FontTest: Font loading failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanup() {
        // Free the window callbacks and destroy the window
        glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        
        System.out.println("FontTest: Cleanup completed");
    }
}