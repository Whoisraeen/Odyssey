package com.odyssey.services.impl;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.RenderingService;
import com.odyssey.services.EnvironmentService;
import com.odyssey.services.EntityService;
import com.odyssey.rendering.*;
import com.odyssey.rendering.scene.Scene;
import com.odyssey.world.Chunk;
import com.odyssey.world.ChunkRenderObject;
import com.odyssey.services.impl.WorldServiceImpl;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

@Service
public class RenderingServiceImpl implements RenderingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RenderingServiceImpl.class);
    
    @Autowired
    private GameConfiguration config;
    
    @Autowired
    private EnvironmentService environmentService;
    
    @Autowired
    private WorldServiceImpl worldService;
    
    @Autowired
    private EntityService entityService;
    
    private ShaderManager shaderManager;
    private AdvancedRenderingPipeline renderingPipeline;
    private SelectionBoxRenderer selectionBoxRenderer;
    private int chunkShaderProgram;
    
    @Override
    public void initialize(int width, int height) {
        this.shaderManager = new ShaderManager();
        
        // Get EnvironmentManager from EnvironmentService
        com.odyssey.environment.EnvironmentManager envManager = null;
        if (environmentService instanceof EnvironmentServiceImpl envServiceImpl) {
            envManager = envServiceImpl.getEnvironmentManager();
        }
        
        this.renderingPipeline = new AdvancedRenderingPipeline(width, height, envManager);
        this.selectionBoxRenderer = new SelectionBoxRenderer();
        
        // Load shaders
        this.chunkShaderProgram = shaderManager.loadProgram("shaders/geometry.vert", "shaders/geometry.frag");
        
        if (chunkShaderProgram == 0) {
            logger.error("CRITICAL ERROR: Failed to load chunk shader program!");
            throw new RuntimeException("Chunk shader program failed to load");
        }
        
        logger.info("Rendering service initialized with chunk shader program ID: {}", chunkShaderProgram);
    }
    
    @Override
    public void render(Camera camera, Scene scene, float deltaTime) {
        updateClearColor();
        
        // Set wetness level in the rendering pipeline
        setWetness(environmentService.getWetness());
        
        // Update scene with current chunks if needed
        if (worldService.hasChunksChanged()) {
            updateScene(scene);
            worldService.markChunksUpdated();
        }
        
        // Update scene
        scene.update(deltaTime);
        
        // Render using advanced pipeline
        float time = (float)glfwGetTime();
        float cloudCoverage = environmentService.getCloudCoverage();
        float cloudDensity = environmentService.getCloudDensity();
        float lightningFlash = environmentService.getLightningFlashIntensity();
        
        renderingPipeline.render(camera, scene, deltaTime, time, cloudCoverage, cloudDensity, lightningFlash);
        
        // Render entities (ships and mobs)
        if (entityService instanceof EntityServiceImpl entityServiceImpl) {
            entityServiceImpl.getEntityManager().render(camera, 
                environmentService instanceof EnvironmentServiceImpl envServiceImpl ? 
                envServiceImpl.getEnvironmentManager() : null);
        }
        
        logger.debug("Frame rendered with {} scene objects", scene.getObjects().size());
    }
    
    @Override
    public void updateClearColor() {
        Vector3f color = environmentService.getSkyColor();
        glClearColor(color.x, color.y, color.z, 1.0f);
    }
    
    @Override
    public void renderSelectionBox(Camera camera, Vector3i position) {
        if (position != null) {
            selectionBoxRenderer.render(camera.getViewMatrix(), camera.getProjectionMatrix(), position);
        }
    }
    
    @Override
    public void updateScene(Scene scene) {
        // Clear existing chunk objects from scene
        scene.clear();
        
        int chunksWithVao = 0;
        int totalChunks = worldService.getChunks().size();
        
        // Add chunks as render objects
        for (Chunk chunk : worldService.getChunks().values()) {
            if (chunk.getVao() != 0) {
                ChunkRenderObject chunkObject = new ChunkRenderObject(chunk);
                scene.addObject(chunkObject);
                chunksWithVao++;
            }
        }
        
        // Debug chunk rendering status
        if (chunksWithVao == 0 && totalChunks > 0) {
            logger.warn("{} chunks loaded but 0 have VAOs (meshes still generating)", totalChunks);
        } else if (chunksWithVao > 0) {
            logger.debug("Rendering {}/{} chunks with VAOs", chunksWithVao, totalChunks);
        }
        
        // Re-add default lighting
        scene.setupDefaultLighting();
    }
    
    @Override
    public void setWetness(float wetness) {
        if (renderingPipeline != null) {
            renderingPipeline.setWetness(wetness);
        }
    }
    
    public ShaderManager getShaderManager() {
        return shaderManager;
    }
    
    public int getChunkShaderProgram() {
        return chunkShaderProgram;
    }
    
    @Override
    public void cleanup() {
        if (shaderManager != null) {
            shaderManager.cleanup();
        }
        if (selectionBoxRenderer != null) {
            selectionBoxRenderer.cleanup();
        }
        if (renderingPipeline != null) {
            renderingPipeline.cleanup();
        }
        logger.info("Rendering service cleaned up");
    }
}