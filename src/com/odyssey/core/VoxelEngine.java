package com.odyssey.core;

import com.odyssey.rendering.ShaderManager;
import com.odyssey.rendering.scene.Camera;
import com.odyssey.world.Chunk;
import com.odyssey.world.ChunkManager;
import com.odyssey.world.ChunkPosition;
import com.odyssey.world.MeshGenerator;
import com.odyssey.world.WorldGenerator;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.odyssey.rendering.mesh.ChunkMesh;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL20.*;

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
    private final ExecutorService meshGenerationPool;
    
    // Core systems
    private final Camera camera;
    private final ShaderManager shaderManager;
    private final MeshGenerator meshGenerator;
    private final WorldGenerator worldGenerator;
    
    private final Map<ChunkPosition, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<ChunkPosition, Future<ChunkMesh>> meshingFutures = new ConcurrentHashMap<>();
    private int chunkShaderProgram;
    
    public VoxelEngine(int windowWidth, int windowHeight) {
        int cores = Runtime.getRuntime().availableProcessors();
        this.meshGenerationPool = Executors.newFixedThreadPool(Math.max(2, cores / 4));
        
        this.camera = new Camera();
        this.shaderManager = new ShaderManager();
        this.meshGenerator = new MeshGenerator(meshGenerationPool);
        this.worldGenerator = new WorldGenerator();
        
        // Load shaders
        this.chunkShaderProgram = shaderManager.loadProgram("shaders/geometry.vert", "shaders/geometry.frag");

        // Create and generate a few chunks for testing
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                Chunk chunk = new Chunk(pos);
                worldGenerator.generateChunk(chunk);
                chunks.put(pos, chunk);
            }
        }
    }
    
    public void update(float deltaTime) {
        camera.update(deltaTime);

        // Check for completed mesh futures
        meshingFutures.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone()) {
                try {
                    ChunkMesh mesh = entry.getValue().get();
                    chunks.get(entry.getKey()).setMesh(mesh);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });
        
        // Check for dirty chunks and generate meshes
        chunks.forEach((pos, chunk) -> {
            if (chunk.isMeshDirty() && !meshingFutures.containsKey(pos)) {
                Future<ChunkMesh> future = meshGenerator.generateMesh(chunk);
                meshingFutures.put(pos, future);
            }
        });
    }
    
    public void render(float deltaTime) {
        glUseProgram(chunkShaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Set Projection and View matrices
            FloatBuffer p = camera.getProjectionMatrix().get(stack.mallocFloat(16));
            FloatBuffer v = camera.getViewMatrix().get(stack.mallocFloat(16));
            glUniformMatrix4fv(glGetUniformLocation(chunkShaderProgram, "projection"), false, p);
            glUniformMatrix4fv(glGetUniformLocation(chunkShaderProgram, "view"), false, v);
            
            // Render each chunk
            for (Chunk chunk : chunks.values()) {
                if (chunk.getVao() != 0) {
                    Matrix4f model = new Matrix4f()
                        .translate(chunk.getPosition().x * CHUNK_SIZE, 0, chunk.getPosition().z * CHUNK_SIZE);
                    FloatBuffer m = model.get(stack.mallocFloat(16));
                    glUniformMatrix4fv(glGetUniformLocation(chunkShaderProgram, "model"), false, m);
                    chunk.render();
                }
            }
        }
    }
    
    public void cleanup() {
        meshGenerationPool.shutdown();
        shaderManager.cleanup();
        chunks.values().forEach(Chunk::cleanup);
    }
    
    public Camera getCamera() { return camera; }
} 