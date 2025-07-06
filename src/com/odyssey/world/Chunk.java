package com.odyssey.world;

import com.odyssey.rendering.mesh.ChunkMesh;
import com.odyssey.world.biome.Biome;
import org.joml.Vector3i;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.odyssey.core.GameConstants.CHUNK_SIZE;
import static com.odyssey.core.GameConstants.MAX_HEIGHT;
import static org.lwjgl.opengl.GL45.*;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Optimized Chunk with palette-based compression
 */
public class Chunk {
    private final ChunkPosition position;
    private final BlockPalette palette;
    private final byte[] blockData; // Compressed block indices
    private final AtomicBoolean meshDirty = new AtomicBoolean(true);
    
    // OpenGL resources
    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int vertexCount = 0;
    
    private int transparentVao;
    private int transparentVbo;
    private int transparentVertexCount;
    
    // Mesh data
    private volatile ChunkMesh mesh;
    
    private final BlockType[] blocks = new BlockType[CHUNK_SIZE * MAX_HEIGHT * CHUNK_SIZE];
    private final Biome[] biomes = new Biome[CHUNK_SIZE * CHUNK_SIZE];
    private ChunkMesh[] meshes;
    private boolean needsRemeshing = true;
    
    public Chunk(ChunkPosition position) {
        this.position = position;
        this.palette = new BlockPalette();
        this.blockData = new byte[CHUNK_SIZE * CHUNK_SIZE * MAX_HEIGHT];
    }
    
    public void setBlock(int x, int y, int z, BlockType block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= MAX_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        
        int index = x + (z * CHUNK_SIZE) + (y * CHUNK_SIZE * CHUNK_SIZE);
        byte paletteIndex = palette.getOrAdd(block);
        blockData[index] = paletteIndex;
        meshDirty.set(true);
    }
    
    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= MAX_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return BlockType.AIR;
        }
        
        int index = x + (z * CHUNK_SIZE) + (y * CHUNK_SIZE * CHUNK_SIZE);
        byte paletteIndex = blockData[index];
        return palette.getBlock(paletteIndex);
    }
    
    public boolean isMeshDirty() {
        return meshDirty.get();
    }
    
    public void markForRebuild() {
        meshDirty.set(true);
        needsRemeshing = true;
    }
    
    public void setMesh(ChunkMesh mesh) {
        this.mesh = mesh;
        meshDirty.set(false);
    }
    
    public void uploadMeshToGPU(ChunkMesh opaqueMesh, ChunkMesh transparentMesh) {
        cleanup(); // Clean up old mesh data first

        // Upload opaque mesh
        if (opaqueMesh.getVertices().length > 0) {
            this.vao = glGenVertexArrays();
            this.vbo = glGenBuffers();
            glBindVertexArray(this.vao);
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo);
            glBufferData(GL_ARRAY_BUFFER, opaqueMesh.getVertices(), GL_STATIC_DRAW);
            
            // Vertex format: position (3), normal (3), texture coords (2) = 8 floats per vertex
            int stride = 8 * Float.BYTES;
            
            // Position attribute (location 0)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);
            
            // Normal attribute (location 1)
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            
            // Texture coordinate attribute (location 2)
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
            glEnableVertexAttribArray(2);
            
            this.vertexCount = opaqueMesh.getVertices().length / 8;
        }

        // Upload transparent mesh
        if (transparentMesh.getVertices().length > 0) {
            this.transparentVao = glGenVertexArrays();
            this.transparentVbo = glGenBuffers();
            glBindVertexArray(this.transparentVao);
            glBindBuffer(GL_ARRAY_BUFFER, this.transparentVbo);
            glBufferData(GL_ARRAY_BUFFER, transparentMesh.getVertices(), GL_STATIC_DRAW);
            
            // Vertex format: position (3), normal (3), texture coords (2) = 8 floats per vertex
            int stride = 8 * Float.BYTES;
            
            // Position attribute (location 0)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glEnableVertexAttribArray(0);
            
            // Normal attribute (location 1)
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
            glEnableVertexAttribArray(1);
            
            // Texture coordinate attribute (location 2)
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
            glEnableVertexAttribArray(2);
            
            this.transparentVertexCount = transparentMesh.getVertices().length / 8;
        }

        glBindVertexArray(0);
    }
    
    public void render() {
        if (vao != 0) {
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
    }
    
    public void renderTransparent() {
        if (transparentVao != 0) {
            glBindVertexArray(transparentVao);
            glDrawArrays(GL_TRIANGLES, 0, transparentVertexCount);
        }
    }
    
    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            vao = 0;
            vbo = 0;
            vertexCount = 0;
        }
        if (transparentVao != 0) {
            glDeleteVertexArrays(transparentVao);
            glDeleteBuffers(transparentVbo);
            transparentVao = 0;
            transparentVbo = 0;
            transparentVertexCount = 0;
        }
    }
    
    public int getVao() { return vao; }
    public int getTransparentVao() { return transparentVao; }
    public int getVertexCount() { return vertexCount; }
    public int getTransparentVertexCount() { return transparentVertexCount; }
    public ChunkPosition getPosition() { return position; }

    /**
     * Gets a block within this chunk using local coordinates.
     * Returns AIR if coordinates are out of bounds.
     */
    public BlockType getBlockInChunk(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE ||
            y < 0 || y >= MAX_HEIGHT ||
            z < 0 || z >= CHUNK_SIZE) {
            return BlockType.AIR;
        }
        return getBlock(x, y, z);
    }

    public void setBiome(int x, int z, Biome biome) {
        if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            return; // Out of bounds
        }
        biomes[x + z * CHUNK_SIZE] = biome;
    }

    public Biome getBiome(int x, int z) {
        if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            return Biome.OCEAN; // Default to ocean for safety
        }
        return biomes[x + z * CHUNK_SIZE];
    }
}