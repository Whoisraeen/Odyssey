package com.odyssey.core;

import com.odyssey.rendering.RenderManager;
import com.odyssey.rendering.scene.Camera;
import com.odyssey.world.ChunkManager;
import com.odyssey.world.MeshGenerator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core Voxel Engine - High-performance world management with modern OpenGL
 * Features: Greedy meshing, multi-threaded chunk loading, memory optimization
 */
public class VoxelEngine {
    
    // World configuration
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int WORLD_HEIGHT = 16; // chunks vertically
    public static final int RENDER_DISTANCE = 16;
    
    // Threading
    private final ExecutorService chunkLoadingPool;
    private final ExecutorService meshGenerationPool;
    private final ScheduledExecutorService backgroundTasks;
    
    // Core systems
    private final ChunkManager chunkManager;
    private final MeshGenerator meshGenerator;
    private final RenderManager renderManager;
    private final Camera camera;
    
    // Performance metrics
    private final AtomicInteger chunksLoaded = new AtomicInteger(0);
    private final AtomicInteger meshesGenerated = new AtomicInteger(0);
    
    public VoxelEngine() {
        // Initialize thread pools based on CPU cores
        int cores = Runtime.getRuntime().availableProcessors();
        this.chunkLoadingPool = Executors.newFixedThreadPool(Math.max(4, cores / 2));
        this.meshGenerationPool = Executors.newFixedThreadPool(Math.max(2, cores / 4));
        this.backgroundTasks = Executors.newScheduledThreadPool(2);
        
        // Initialize core systems
        this.chunkManager = new ChunkManager(chunkLoadingPool);
        this.meshGenerator = new MeshGenerator(meshGenerationPool);
        this.renderManager = new RenderManager();
        this.camera = new Camera();
        
        System.out.println("VoxelEngine initialized with " + cores + " CPU cores");
        System.out.println("Chunk loading threads: " + (cores / 2));
        System.out.println("Mesh generation threads: " + (cores / 4));
    }
    
    public void update(float deltaTime) {
        camera.update(deltaTime);
        chunkManager.update(camera.getPosition());
        
        // Update performance metrics
        if (System.currentTimeMillis() % 1000 < 16) { // Every second
            System.out.println("Chunks loaded: " + chunksLoaded.get() + 
                             ", Meshes generated: " + meshesGenerated.get());
        }
    }
    
    public void render() {
        renderManager.render(camera, chunkManager.getVisibleChunks());
    }
    
    public void cleanup() {
        chunkLoadingPool.shutdown();
        meshGenerationPool.shutdown();
        backgroundTasks.shutdown();
        chunkManager.cleanup();
        renderManager.cleanup();
    }
    
    public Camera getCamera() { return camera; }
    public ChunkManager getChunkManager() { return chunkManager; }
} 