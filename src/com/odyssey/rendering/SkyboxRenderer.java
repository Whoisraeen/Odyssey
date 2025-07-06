package com.odyssey.rendering;

import com.odyssey.rendering.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

/**
 * Skybox Renderer for atmospheric sky rendering with sun, moon, stars, and clouds
 */
public class SkyboxRenderer {
    
    private final int shaderProgram;
    private final int skyboxVAO;
    private final int skyboxVBO;
    
    // Uniform locations
    private final int viewLoc;
    private final int projectionLoc;
    private final int sunDirectionLoc;
    private final int moonDirectionLoc;
    private final int sunColorLoc;
    private final int moonColorLoc;
    private final int sunIntensityLoc;
    private final int moonIntensityLoc;
    private final int timeOfDayLoc;
    private final int cloudCoverageLoc;
    private final int cloudDensityLoc;
    private final int enableAtmosphericScatteringLoc;
    private final int enableProceduralCloudsLoc;
    private final int enableStarsLoc;
    private final int enableSunMoonLoc;
    private final int skyboxQualityLoc;
    
    // Skybox cube vertices (positions only, no texture coordinates needed for procedural sky)
    private static final float[] SKYBOX_VERTICES = {
        // Front face
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
        -1.0f,  1.0f, -1.0f,
        
        // Back face
        -1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
        -1.0f, -1.0f,  1.0f,
        
        // Left face
        -1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        
        // Right face
         1.0f,  1.0f, -1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f, -1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
        
        // Bottom face
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f, -1.0f,  1.0f,
         1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        
        // Top face
        -1.0f,  1.0f, -1.0f,
         1.0f,  1.0f, -1.0f,
         1.0f,  1.0f,  1.0f,
         1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f,  1.0f,
        -1.0f,  1.0f, -1.0f
    };
    
    public SkyboxRenderer() {
        System.out.println("DEBUG: Starting SkyboxRenderer initialization...");
        
        // Load skybox shaders
        ShaderManager shaderManager = new ShaderManager();
        System.out.println("DEBUG: Loading skybox shaders...");
        this.shaderProgram = shaderManager.loadProgram("shaders/skybox.vert", "shaders/skybox.frag");
        
        if (shaderProgram == 0) {
            System.err.println("ERROR: Failed to load skybox shaders - shader program ID is 0");
            throw new RuntimeException("Failed to load skybox shaders!");
        }
        System.out.println("DEBUG: Skybox shaders loaded successfully, program ID: " + shaderProgram);
        
        // Get uniform locations
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projectionLoc = glGetUniformLocation(shaderProgram, "projection");
        sunDirectionLoc = glGetUniformLocation(shaderProgram, "sunDirection");
        moonDirectionLoc = glGetUniformLocation(shaderProgram, "moonDirection");
        sunColorLoc = glGetUniformLocation(shaderProgram, "sunColor");
        moonColorLoc = glGetUniformLocation(shaderProgram, "moonColor");
        sunIntensityLoc = glGetUniformLocation(shaderProgram, "sunIntensity");
        moonIntensityLoc = glGetUniformLocation(shaderProgram, "moonIntensity");
        timeOfDayLoc = glGetUniformLocation(shaderProgram, "timeOfDay");
        cloudCoverageLoc = glGetUniformLocation(shaderProgram, "cloudCoverage");
        cloudDensityLoc = glGetUniformLocation(shaderProgram, "cloudDensity");
        enableAtmosphericScatteringLoc = glGetUniformLocation(shaderProgram, "enableAtmosphericScattering");
        enableProceduralCloudsLoc = glGetUniformLocation(shaderProgram, "enableProceduralClouds");
        enableStarsLoc = glGetUniformLocation(shaderProgram, "enableStars");
        enableSunMoonLoc = glGetUniformLocation(shaderProgram, "enableSunMoon");
        skyboxQualityLoc = glGetUniformLocation(shaderProgram, "skyboxQuality");
        
        // Create skybox VAO and VBO
        skyboxVAO = glGenVertexArrays();
        skyboxVBO = glGenBuffers();
        
        glBindVertexArray(skyboxVAO);
        glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO);
        glBufferData(GL_ARRAY_BUFFER, SKYBOX_VERTICES, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glBindVertexArray(0);
        
        System.out.println("SkyboxRenderer initialized successfully");
    }
    
    /**
     * Render the skybox with atmospheric effects
     */
    public void render(Camera camera, float timeOfDay, float cloudCoverage, float cloudDensity) {
        // Save current OpenGL state
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        int depthFunc = glGetInteger(GL_DEPTH_FUNC);
        
        // Configure OpenGL state for skybox rendering
        glDepthFunc(GL_LEQUAL); // Change depth function so depth test passes when values are equal to depth buffer's content
        if (!depthTestEnabled) {
            glEnable(GL_DEPTH_TEST);
        }
        
        glUseProgram(shaderProgram);
        
        // Remove translation from view matrix (keep only rotation)
        Matrix4f viewMatrix = new Matrix4f(camera.getViewMatrix());
        viewMatrix.m30(0.0f); // Remove translation
        viewMatrix.m31(0.0f);
        viewMatrix.m32(0.0f);
        
        // Set matrices
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = stack.mallocFloat(16);
            FloatBuffer projBuffer = stack.mallocFloat(16);
            
            viewMatrix.get(viewBuffer);
            camera.getProjectionMatrix().get(projBuffer);
            
            glUniformMatrix4fv(viewLoc, false, viewBuffer);
            glUniformMatrix4fv(projectionLoc, false, projBuffer);
        }
        
        // Calculate sun and moon directions based on time of day
        float sunAngle = timeOfDay * 2.0f * (float)Math.PI; // Full rotation over day
        Vector3f sunDirection = new Vector3f(
            (float)Math.sin(sunAngle),
            (float)Math.cos(sunAngle),
            0.0f
        ).normalize();
        
        Vector3f moonDirection = new Vector3f(
            -sunDirection.x,
            -sunDirection.y,
            sunDirection.z
        ).normalize();
        
        // Set sun/moon uniforms
        glUniform3f(sunDirectionLoc, sunDirection.x, sunDirection.y, sunDirection.z);
        glUniform3f(moonDirectionLoc, moonDirection.x, moonDirection.y, moonDirection.z);
        
        // Set colors and intensities
        glUniform3f(sunColorLoc, 1.0f, 0.9f, 0.7f); // Warm sun color
        glUniform3f(moonColorLoc, 0.8f, 0.8f, 1.0f); // Cool moon color
        
        float dayFactor = Math.max(0.0f, sunDirection.y);
        float nightFactor = Math.max(0.0f, -sunDirection.y);
        
        glUniform1f(sunIntensityLoc, dayFactor * 3.0f);
        glUniform1f(moonIntensityLoc, nightFactor * 0.5f);
        
        // Set time and weather uniforms
        glUniform1f(timeOfDayLoc, timeOfDay);
        glUniform1f(cloudCoverageLoc, cloudCoverage);
        glUniform1f(cloudDensityLoc, cloudDensity);
        
        // Enable all atmospheric effects
        glUniform1i(enableAtmosphericScatteringLoc, 1);
        glUniform1i(enableProceduralCloudsLoc, 1);
        glUniform1i(enableStarsLoc, 1);
        glUniform1i(enableSunMoonLoc, 1);
        glUniform1i(skyboxQualityLoc, 2); // High quality
        
        // Render skybox cube
        glBindVertexArray(skyboxVAO);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);
        
        // Restore OpenGL state
        glDepthFunc(depthFunc);
        if (!depthTestEnabled) {
            glDisable(GL_DEPTH_TEST);
        }
    }
    
    /**
     * Get current time of day based on game time
     */
    public static float getTimeOfDay() {
        // Convert game time to time of day [0.0, 1.0]
        // This is a simple implementation - you might want to tie this to your game's day/night cycle
        float gameTime = (float)glfwGetTime();
        return (gameTime * 0.1f) % 1.0f; // Slow day/night cycle for testing
    }
    
    public void cleanup() {
        glDeleteVertexArrays(skyboxVAO);
        glDeleteBuffers(skyboxVBO);
        glDeleteProgram(shaderProgram);
    }
}