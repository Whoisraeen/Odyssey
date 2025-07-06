package com.odyssey.rendering;

import com.odyssey.core.PerformanceProfiler;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.GLErrorChecker;
import com.odyssey.rendering.scene.RenderObject;
import com.odyssey.rendering.scene.Scene;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.opengl.GL20;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import com.odyssey.rendering.lighting.VolumetricLighting;
import com.odyssey.rendering.lighting.ShadowMapping;
import com.odyssey.rendering.clouds.CloudRenderer;
import com.odyssey.core.VoxelEngine;
import com.odyssey.environment.EnvironmentManager;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform1f;

/**
 * Advanced Modern Rendering Pipeline
 * Features: PBR, Deferred Rendering, Volumetric Lighting, SSAO, Post-Processing
 */
public class AdvancedRenderingPipeline {
    
    // Screen dimensions
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    
    // G-Buffer for deferred rendering
    private final GBuffer gBuffer;
    private final LightingSystem lightingSystem;
    private final PostProcessing postProcessing;
    private final VolumetricLighting volumetricLighting;
    private final SSAORenderer ssaoRenderer;
    private final ShadowMapping shadowMapping;
    private final CloudRenderer cloudRenderer;
    
    // Shader programs
    private final ShaderManager shaderManager;
    private final Map<String, Integer> shaderPrograms = new ConcurrentHashMap<>();
    
    // Final composition quad for rendering to screen
    private int finalQuadVAO, finalQuadVBO;
    
    // Uniform Buffer Objects for efficient data transfer
    private final UniformBufferManager uniformBuffers;
    
    // Performance tracking
    private final PerformanceProfiler profiler;
    
    private final EnvironmentManager environmentManager;

    public AdvancedRenderingPipeline(int width, int height, EnvironmentManager environmentManager) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.environmentManager = environmentManager;
        
        // Initialize systems
        this.gBuffer = new GBuffer(width, height);
        this.lightingSystem = new LightingSystem();
        this.lightingSystem.initialize(width, height);
        this.postProcessing = new PostProcessing(width, height);
        this.volumetricLighting = new VolumetricLighting(width, height);
        this.ssaoRenderer = new SSAORenderer(width, height);
        this.shadowMapping = new ShadowMapping(2048, 2048); // High-res shadow maps
        this.cloudRenderer = new CloudRenderer(width, height);
        this.shaderManager = new ShaderManager();
        this.uniformBuffers = new UniformBufferManager();
        this.profiler = new PerformanceProfiler();
        
        setupFinalQuad();
        initializeShaders();
        initializeUniforms();
        
        // Configure post-processing parameters for proper brightness
        postProcessing.setExposure(2.0f);  // Increase exposure for brighter image
        postProcessing.setBloomThreshold(1.0f);
        postProcessing.setBloomIntensity(0.04f);
        postProcessing.setBloomEnabled(true);
        postProcessing.setFXAAEnabled(true);
        
        // Enable advanced OpenGL features
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        System.out.println("Advanced Rendering Pipeline initialized");
        System.out.println("Resolution: " + width + "x" + height);
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
    }
    
    /**
     * Main render function - implements full deferred rendering pipeline
     */
    public void render(Camera camera, Scene scene, float deltaTime, float time, float cloudCoverage, float cloudDensity, float lightningFlash) {
        profiler.startFrame();
        
        // Debug scene content
        int totalObjects = scene.getObjects().size();
        int opaqueObjects = scene.getOpaqueObjects().size();
        int transparentObjects = scene.getTransparentObjects().size();
        
        if (totalObjects == 0) {
            System.out.println("WARNING: Scene has no objects to render - this will cause black screen!");
        } else {
            System.out.println("DEBUG: Rendering scene with " + totalObjects + " objects (" + 
                opaqueObjects + " opaque, " + transparentObjects + " transparent)");
        }
        
        // Update uniform buffers
        updateUniforms(camera, scene, deltaTime);
        
        // 1. Shadow mapping pass
        profiler.startSection("Shadow Mapping");
        shadowMapping.renderShadowMaps(scene.getLights(), scene.getObjects());
        profiler.endSection();
        
        // 2. G-Buffer pass (Geometry)
        profiler.startSection("G-Buffer");
        renderGBuffer(camera, scene);
        profiler.endSection();
        
        // 3. SSAO pass
        profiler.startSection("SSAO");
        ssaoRenderer.render(gBuffer, camera);
        profiler.endSection();
        
        // 4. Deferred lighting pass
        profiler.startSection("Deferred Lighting");
        lightingSystem.renderLighting(gBuffer, camera, scene.getLights(), 
                                    shadowMapping, ssaoRenderer.getSSAOTexture());
        profiler.endSection();
        
        // 5. Volumetric lighting pass
        profiler.startSection("Volumetric Lighting");
        volumetricLighting.render(camera, scene.getLights(), gBuffer.getDepthTexture(), shadowMapping.getShadowMapTexture());
        profiler.endSection();

        // The G-Buffer depth buffer needs to be copied before any forward rendering
        gBuffer.copyDepthToDefaultFramebuffer(screenWidth, screenHeight);

        // 6. Skybox rendering pass
        profiler.startSection("Skybox");
        skyboxRenderer.render(camera, SkyboxRenderer.getTimeOfDay(), cloudCoverage, cloudDensity);
        profiler.endSection();

        // 7. Forward rendering pass (transparent objects)
        profiler.startSection("Forward Rendering");
        renderForward(camera, scene);
        profiler.endSection();
        
        // 8. Cloud Pass
        profiler.startSection("Clouds");
        cloudRenderer.render(camera, time, cloudCoverage, cloudDensity);
        profiler.endSection();
        
        // 9. Post-processing pipeline
        profiler.startSection("Post-Processing");
        postProcessing.render(lightingSystem.getLitTexture(), 
                            volumetricLighting.getVolumetricTexture(),
                            cloudRenderer.getCloudTexture(),
                            gBuffer.getDepthTexture(),
                            lightningFlash);
        profiler.endSection();
        
        // 10. Final composition - render to screen
        profiler.startSection("Final Composition");
        renderFinalComposition();
        profiler.endSection();
        
        profiler.endFrame();
        
        // Print performance stats every second
        if (profiler.shouldPrintStats()) {
            profiler.printStats();
        }
    }
    
    private void renderGBuffer(Camera camera, Scene scene) {
        gBuffer.bind();
        
        // Clear G-Buffer
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Use geometry shader program
        int shaderProgram = shaderPrograms.get("geometry");
        glUseProgram(shaderProgram);
        
        // Bind uniform buffers
        uniformBuffers.bindCameraUniforms(camera);
        uniformBuffers.bindMaterialUniforms();
        
        // Pass environmental uniforms to the geometry shader
        glUniform1f(glGetUniformLocation(shaderProgram, "u_time"), (float)glfwGetTime());
        glUniform1f(glGetUniformLocation(shaderProgram, "u_windStrength"), environmentManager.getWindSpeed());
        glUniform3f(glGetUniformLocation(shaderProgram, "u_windDirection"), 
                   environmentManager.getWindDirection().x, 
                   0.0f, 
                   environmentManager.getWindDirection().y);
        glUniform1f(glGetUniformLocation(shaderProgram, "u_windFrequency"), 2.0f); // Default wind frequency
        
        // Pass quality flags
        glUniform1i(glGetUniformLocation(shaderProgram, "u_enableWindAnimation"), 1);
        glUniform1i(glGetUniformLocation(shaderProgram, "u_enableTangentSpace"), 1);
        glUniform1i(glGetUniformLocation(shaderProgram, "u_vertexQuality"), 1);
        
        // Pass season uniforms to the geometry shader
        glUniform1i(glGetUniformLocation(shaderProgram, "u_season"), environmentManager.getCurrentSeasonId());
        glUniform1f(glGetUniformLocation(shaderProgram, "u_seasonTransition"), environmentManager.getSeasonTransition());
        
        // Render all opaque objects
        for (RenderObject obj : scene.getOpaqueObjects()) {
            renderObject(obj, shaderProgram);
        }
        
        gBuffer.unbind();
    }
    
    private void renderForward(Camera camera, Scene scene) {
        // Use forward shader program
        int shaderProgram = shaderPrograms.get("forward");
        glUseProgram(shaderProgram);
        
        // Bind uniform buffers
        uniformBuffers.bindCameraUniforms(camera);
        uniformBuffers.bindMaterialUniforms();
        
        // Bind lighting textures
        lightingSystem.bindLightingTextures();
        
        // Pass season uniforms to the forward shader
        glUniform1i(glGetUniformLocation(shaderProgram, "u_season"), environmentManager.getCurrentSeasonId());
        glUniform1f(glGetUniformLocation(shaderProgram, "u_seasonTransition"), environmentManager.getSeasonTransition());
        
        // Render transparent objects
        for (RenderObject obj : scene.getTransparentObjects()) {
            renderObject(obj, shaderProgram);
        }
    }
    
    private void renderObject(RenderObject obj, int shaderProgram) {
        // Bind object-specific uniforms
        uniformBuffers.bindObjectUniforms(obj);
        
        // Bind textures
        obj.getMaterial().bind();
        
        // Render mesh
        obj.getMesh().render();
    }
    
    private void updateUniforms(Camera camera, Scene scene, float deltaTime) {
        uniformBuffers.updateCameraUniforms(camera);
        uniformBuffers.updateLightUniforms(scene.getLights());
        uniformBuffers.updateTimeUniforms(deltaTime);
    }
    
    private void initializeShaders() {
        System.out.println("DEBUG: Loading rendering pipeline shaders...");
        
        // Load and compile all shaders with validation
        String[] criticalShaders = {
            "geometry", "forward", "deferred_lighting", "volumetric", 
            "ssao", "shadow", "final_composite"
        };
        
        String[][] shaderPaths = {
            {"shaders/geometry.vert", "shaders/geometry.frag"},
            {"shaders/forward.vert", "shaders/forward.frag"},
            {"shaders/deferred_lighting.vert", "shaders/deferred_lighting.frag"},
            {"shaders/volumetric.vert", "shaders/volumetric.frag"},
            {"shaders/ssao.vert", "shaders/ssao.frag"},
            {"shaders/shadow.vert", "shaders/shadow.frag"},
            {"shaders/final_composite.vert", "shaders/final_composite.frag"}
        };
        
        for (int i = 0; i < criticalShaders.length; i++) {
            String shaderName = criticalShaders[i];
            String[] paths = shaderPaths[i];
            
            int shaderProgram = shaderManager.loadProgram(paths[0], paths[1]);
            shaderPrograms.put(shaderName, shaderProgram);
            
            if (shaderProgram == 0) {
                System.err.println("CRITICAL ERROR: Failed to load " + shaderName + " shader (" + paths[0] + ", " + paths[1] + ")");
                throw new RuntimeException("Critical shader " + shaderName + " failed to load - this will cause black screen");
            } else {
                System.out.println("DEBUG: " + shaderName + " shader loaded successfully (ID: " + shaderProgram + ")");
            }
        }
        
        // Pass the deferred lighting shader to the lighting system
        lightingSystem.setDeferredLightingShader(shaderPrograms.get("deferred_lighting"));
        
        System.out.println("DEBUG: All rendering pipeline shaders loaded successfully");
    }
    
    private void setupFinalQuad() {
        float[] quadVertices = {
            -1.0f,  1.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
             1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
             1.0f, -1.0f, 0.0f, 1.0f, 0.0f,
        };
        
        finalQuadVAO = glGenVertexArrays();
        finalQuadVBO = glGenBuffers();
        
        glBindVertexArray(finalQuadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, finalQuadVBO);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        
        glBindVertexArray(0);
    }
    
    private void initializeUniforms() {
        uniformBuffers.createCameraUniforms();
        uniformBuffers.createLightUniforms();
        uniformBuffers.createMaterialUniforms();
        uniformBuffers.createTimeUniforms();
    }
    
    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        
        gBuffer.resize(width, height);
        postProcessing.resize(width, height);
        volumetricLighting.resize(width, height);
        ssaoRenderer.resize(width, height);
        shadowMapping.resize(width, height);
        cloudRenderer.resize(width, height);
        
        glViewport(0, 0, width, height);
    }
    
    private void renderFinalComposition() {
        System.out.println("Final composition: Rendering to screen");
        
        // Clear any accumulated OpenGL errors before we start
        GLErrorChecker.clearGLErrors();
        
        // Validate and reset OpenGL state before final composition
        validateAndResetOpenGLState();
        
        // Check if state reset fixed any issues
        if (GLErrorChecker.hasGLError()) {
            System.err.println("WARNING: OpenGL errors still present after state reset!");
            logOpenGLState();
            GLErrorChecker.clearGLErrors();
            
            // Try a more aggressive reset
            System.out.println("DEBUG: Attempting aggressive OpenGL state reset...");
            aggressiveStateReset();
        } else {
            System.out.println("DEBUG: OpenGL state is clean, proceeding with final composition");
        }
        
        // Save current OpenGL state
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        
        // Bind default framebuffer (screen)
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GLErrorChecker.checkGLError("binding default framebuffer");
        
        glClear(GL_COLOR_BUFFER_BIT);
        GLErrorChecker.checkGLError("clearing screen");
        
        // Use final composition shader
        Integer finalShaderObj = shaderPrograms.get("final_composite");
        if (finalShaderObj == null) {
            System.err.println("CRITICAL ERROR: final_composite shader not found!");
            return;
        }
        
        int finalShader = finalShaderObj;
        if (!GLErrorChecker.validateShaderProgram(finalShader, "renderFinalComposition")) {
            System.err.println("CRITICAL ERROR: Invalid final_composite shader program!");
            return;
        }
        
        glUseProgram(finalShader);
        GLErrorChecker.checkGLError("using final shader");
        
        // Validate and bind the final processed image texture
        int finalImageTexture = postProcessing.getFinalImageTexture();
        if (!GLErrorChecker.validateTexture(finalImageTexture, "final image texture")) {
            System.err.println("CRITICAL ERROR: Invalid final image texture from post-processing!");
            return;
        }
        
        glActiveTexture(GL_TEXTURE0);
        GLErrorChecker.checkGLError("activating texture unit 0");
        
        glBindTexture(GL_TEXTURE_2D, finalImageTexture);
        GLErrorChecker.checkGLError("binding final image texture");
        
        int uniformLocation = glGetUniformLocation(finalShader, "finalImage");
        GLErrorChecker.validateUniformLocation(uniformLocation, "finalImage", "renderFinalComposition");
        glUniform1i(uniformLocation, 0);
        GLErrorChecker.checkGLError("setting finalImage uniform");
        
        // Validate and render fullscreen quad
        if (!GLErrorChecker.validateVAO(finalQuadVAO, "final quad VAO")) {
            System.err.println("CRITICAL ERROR: Invalid final quad VAO!");
            return;
        }
        
        glBindVertexArray(finalQuadVAO);
        GLErrorChecker.checkGLError("binding final quad VAO");
        
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GLErrorChecker.checkGLError("drawing final quad");
        
        // Restore previous OpenGL state
        glBindVertexArray(previousVAO);
        glUseProgram(previousProgram);
        glActiveTexture(previousActiveTexture);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
        
        GLErrorChecker.checkGLError("renderFinalComposition end");
        System.out.println("Final composition completed successfully");
    }
    
    private void renderSceneGeometry() {
        // This method is now handled by renderGeometryPass()
        // which properly renders all opaque objects to the G-Buffer
    }
    
    private void renderTransparentObjects() {
        // Placeholder for forward rendering of transparent objects
        // This includes water, glass, particles, etc.
    }
    
    public void cleanup() {
        gBuffer.cleanup();
        lightingSystem.cleanup();
        postProcessing.cleanup();
        volumetricLighting.cleanup();
        ssaoRenderer.cleanup();
        shadowMapping.cleanup();
        cloudRenderer.cleanup();
        shaderManager.cleanup();
        uniformBuffers.cleanup();
        
        // Clean up final quad resources
        glDeleteVertexArrays(finalQuadVAO);
        glDeleteBuffers(finalQuadVBO);
        
        // Delete shader programs
        shaderPrograms.values().forEach(GL20::glDeleteProgram);
        shaderPrograms.clear();
    }
    
    public void setWetness(float wetness) {
        // Save current shader program
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        
        int shaderProgram = shaderPrograms.get("geometry");
        glUseProgram(shaderProgram);
        GL20.glUniform1f(GL20.glGetUniformLocation(shaderProgram, "u_wetness"), wetness);
        
        // Restore previous shader program
        glUseProgram(previousProgram);
    }
    
    /**
     * Validate and reset OpenGL state to prevent GL_INVALID_OPERATION errors
     * in final composition. This ensures we start with a clean slate.
     */
    private void validateAndResetOpenGLState() {
        // 1. Ensure no framebuffer is bound from previous passes
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        
        // 2. Reset shader program to 0 (unbound)
        glUseProgram(0);
        
        // 3. Reset vertex array binding
        glBindVertexArray(0);
        
        // 4. Reset texture bindings for commonly used texture units
        for (int i = 0; i < 8; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        }
        
        // 5. Reset to texture unit 0
        glActiveTexture(GL_TEXTURE0);
        
        // 6. Reset buffer bindings
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        
        // 7. Disable any potentially enabled states that might interfere
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_CULL_FACE);
        
        // 8. Reset viewport to screen size (in case it was changed)
        glViewport(0, 0, screenWidth, screenHeight);
        
        System.out.println("DEBUG: OpenGL state reset for final composition");
    }
    
    /**
     * More aggressive OpenGL state reset for when the standard reset doesn't work
     */
    private void aggressiveStateReset() {
        // First, clear all errors
        while (glGetError() != GL_NO_ERROR) {
            // Clear accumulated errors
        }
        
        // Reset absolutely everything we can think of
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glUseProgram(0);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        
        // Reset all texture units
        for (int i = 0; i < 16; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
            glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
        }
        glActiveTexture(GL_TEXTURE0);
        
        // Disable all states
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_POLYGON_OFFSET_FILL);
        glDisable(GL_SCISSOR_TEST);
        
        // Reset to default OpenGL state
        glDepthMask(true);
        glColorMask(true, true, true, true);
        glStencilMask(0xFF);
        
        // Reset viewport
        glViewport(0, 0, screenWidth, screenHeight);
        
        System.out.println("DEBUG: Aggressive OpenGL state reset completed");
    }
    
    /**
     * Debug method to log current OpenGL state when errors occur
     */
    private void logOpenGLState() {
        System.out.println("=== OpenGL State Debug ===");
        System.out.println("Current Program: " + glGetInteger(GL_CURRENT_PROGRAM));
        System.out.println("Bound VAO: " + glGetInteger(GL_VERTEX_ARRAY_BINDING));
        System.out.println("Bound Framebuffer: " + glGetInteger(GL_FRAMEBUFFER_BINDING));
        System.out.println("Active Texture Unit: " + (glGetInteger(GL_ACTIVE_TEXTURE) - GL_TEXTURE0));
        System.out.println("Bound Texture 2D: " + glGetInteger(GL_TEXTURE_BINDING_2D));
        System.out.println("Viewport: " + glGetInteger(GL_VIEWPORT));
        System.out.println("Depth Test: " + (glIsEnabled(GL_DEPTH_TEST) ? "enabled" : "disabled"));
        System.out.println("Blend: " + (glIsEnabled(GL_BLEND) ? "enabled" : "disabled"));
        System.out.println("Cull Face: " + (glIsEnabled(GL_CULL_FACE) ? "enabled" : "disabled"));
        System.out.println("==========================");
    }
}