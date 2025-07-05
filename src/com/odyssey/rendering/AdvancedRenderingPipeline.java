package com.odyssey.rendering;

import com.odyssey.core.PerformanceProfiler;
import com.odyssey.rendering.scene.Camera;
import com.odyssey.rendering.scene.RenderObject;
import com.odyssey.rendering.scene.Scene;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.opengl.GL20;
import static org.lwjgl.opengl.GL45.*;
import com.odyssey.rendering.lighting.VolumetricLighting;
import com.odyssey.rendering.lighting.ShadowMapping;


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
    
    // Shader programs
    private final ShaderManager shaderManager;
    private final Map<String, Integer> shaderPrograms = new ConcurrentHashMap<>();
    
    // Uniform Buffer Objects for efficient data transfer
    private final UniformBufferManager uniformBuffers;
    
    // Performance tracking
    private final PerformanceProfiler profiler;
    
    public AdvancedRenderingPipeline(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        
        // Initialize systems
        this.gBuffer = new GBuffer(width, height);
        this.lightingSystem = new LightingSystem();
        this.postProcessing = new PostProcessing(width, height);
        this.volumetricLighting = new VolumetricLighting(width, height);
        this.ssaoRenderer = new SSAORenderer(width, height);
        this.shadowMapping = new ShadowMapping(2048, 2048); // High-res shadow maps
        this.shaderManager = new ShaderManager();
        this.uniformBuffers = new UniformBufferManager();
        this.profiler = new PerformanceProfiler();
        
        initializeShaders();
        initializeUniforms();
        
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
    public void render(Camera camera, Scene scene, float deltaTime) {
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
        volumetricLighting.render(camera, scene.getLights(), shadowMapping);
        profiler.endSection();
        
        // 6. Forward rendering pass (transparent objects)
        profiler.startSection("Forward Rendering");
        renderForward(camera, scene);
        profiler.endSection();
        
        // 7. Post-processing pipeline
        profiler.startSection("Post-Processing");
        postProcessing.render(lightingSystem.getColorTexture(), 
                            volumetricLighting.getVolumetricTexture(),
                            gBuffer.getDepthTexture());
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
        
        // Bind lighting textures
        lightingSystem.bindLightingTextures();
        
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
            "shaders/geometry.vert", "shaders/geometry.frag"));
        shaderPrograms.put("forward", shaderManager.loadProgram(
            "shaders/forward.vert", "shaders/forward.frag"));
        shaderPrograms.put("deferred_lighting", shaderManager.loadProgram(
            "shaders/deferred_lighting.vert", "shaders/deferred_lighting.frag"));
        shaderPrograms.put("volumetric", shaderManager.loadProgram(
            "shaders/volumetric.vert", "shaders/volumetric.frag"));
        shaderPrograms.put("ssao", shaderManager.loadProgram(
            "shaders/ssao.vert", "shaders/ssao.frag"));
        shaderPrograms.put("shadow", shaderManager.loadProgram(
            "shaders/shadow.vert", "shaders/shadow.frag"));
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
        
        glViewport(0, 0, width, height);
    }
    
    public void cleanup() {
        gBuffer.cleanup();
        lightingSystem.cleanup();
        postProcessing.cleanup();
        volumetricLighting.cleanup();
        ssaoRenderer.cleanup();
        shadowMapping.cleanup();
        shaderManager.cleanup();
        uniformBuffers.cleanup();
        
        // Delete shader programs
        shaderPrograms.values().forEach(GL20::glDeleteProgram);
        shaderPrograms.clear();
    }
} 