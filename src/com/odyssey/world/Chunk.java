package com.odyssey.world;

import com.odyssey.rendering.mesh.ChunkMesh;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.odyssey.core.VoxelEngine.CHUNK_HEIGHT;
import static com.odyssey.core.VoxelEngine.CHUNK_SIZE;
import static org.lwjgl.opengl.GL45.*;

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
    
    // Mesh data
    private volatile ChunkMesh mesh;
    
    public Chunk(ChunkPosition position) {
        this.position = position;
        this.palette = new BlockPalette();
        this.blockData = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
    }
    
    public void setBlock(int x, int y, int z, BlockType block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        
        int index = x + (z * CHUNK_SIZE) + (y * CHUNK_SIZE * CHUNK_SIZE);
        byte paletteIndex = palette.getOrAdd(block);
        blockData[index] = paletteIndex;
        meshDirty.set(true);
    }
    
    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return BlockType.AIR;
        }
        
        int index = x + (z * CHUNK_SIZE) + (y * CHUNK_SIZE * CHUNK_SIZE);
        byte paletteIndex = blockData[index];
        return palette.getBlock(paletteIndex);
    }
    
    public boolean isMeshDirty() {
        return meshDirty.get();
    }
    
    public void setMesh(ChunkMesh mesh) {
        this.mesh = mesh;
        meshDirty.set(false);
        
        // Upload to GPU
        uploadMeshToGPU();
    }
    
    private void uploadMeshToGPU() {
        if (mesh == null) return;
        
        // Generate OpenGL objects if needed
        if (vao == 0) {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            ebo = glGenBuffers();
        }
        
        glBindVertexArray(vao);
        
        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, mesh.getVertexBuffer(), GL_STATIC_DRAW);
        
        // Upload index data
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh.getIndexBuffer(), GL_STATIC_DRAW);
        
        // Set vertex attributes
        // Position (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Normal (vec3)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // UV (vec2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        vertexCount = mesh.getIndexCount();
        
        glBindVertexArray(0);
    }
    
    public void render() {
        if (vao != 0 && vertexCount > 0) {
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }
    
    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            vao = vbo = ebo = 0;
        }
    }
    
    public ChunkPosition getPosition() { return position; }
} 