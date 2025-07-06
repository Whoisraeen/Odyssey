package com.odyssey.rendering;

import com.odyssey.core.PerformanceProfiler;
import com.odyssey.rendering.Camera;
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
        
        // 6. Forward rendering pass (transparent objects)
        profiler.startSection("Forward Rendering");
        renderForward(camera, scene);
        profiler.endSection();
        
        // 7. Cloud Pass
        profiler.startSection("Clouds");
        cloudRenderer.render(camera, time, cloudCoverage, cloudDensity);
        profiler.endSection();
        
        // 8. Post-processing pipeline
        profiler.startSection("Post-Processing");
        postProcessing.render(lightingSystem.getLitTexture(), 
                            volumetricLighting.getVolumetricTexture(),
                            cloudRenderer.getCloudTexture(),
                            gBuffer.getDepthTexture(),
                            lightningFlash);
        profiler.endSection();
        
        // 9. Final composition - render to screen
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
        // Copy depth buffer from G-Buffer
        gBuffer.copyDepthToDefaultFramebuffer(screenWidth, screenHeight);
        
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
        // Load and compile all shaders
        shaderPrograms.put("geometry", shaderManager.loadProgram(
            "resources/shaders/geometry.vert", "resources/shaders/geometry.frag"));
        shaderPrograms.put("forward", shaderManager.loadProgram(
            "resources/shaders/forward.vert", "resources/shaders/forward.frag"));
        shaderPrograms.put("deferred_lighting", shaderManager.loadProgram(
            "resources/shaders/deferred_lighting.vert", "resources/shaders/deferred_lighting.frag"));
        shaderPrograms.put("volumetric", shaderManager.loadProgram(
            "resources/shaders/volumetric.vert", "resources/shaders/volumetric.frag"));
        shaderPrograms.put("ssao", shaderManager.loadProgram(
            "resources/shaders/ssao.vert", "resources/shaders/ssao.frag"));
        shaderPrograms.put("shadow", shaderManager.loadProgram(
            "resources/shaders/shadow.vert", "resources/shaders/shadow.frag"));
        shaderPrograms.put("final_composite", shaderManager.loadProgram(
            "resources/shaders/final_composite.vert", "resources/shaders/final_composite.frag"));
            
        // Pass the deferred lighting shader to the lighting system
        lightingSystem.setDeferredLightingShader(shaderPrograms.get("deferred_lighting"));
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
        // Save current OpenGL state
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int previousVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        
        // Bind default framebuffer (screen)
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Use final composition shader
        int finalShader = shaderPrograms.get("final_composite");
        glUseProgram(finalShader);
        
        // Bind the final processed image texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, postProcessing.getFinalImageTexture());
        glUniform1i(glGetUniformLocation(finalShader, "finalImage"), 0);
        
        // Render fullscreen quad
        glBindVertexArray(finalQuadVAO);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        
        // Restore previous OpenGL state
        glBindVertexArray(previousVAO);
        glUseProgram(previousProgram);
        glActiveTexture(previousActiveTexture);
        glBindTexture(GL_TEXTURE_2D, previousTexture);
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
}