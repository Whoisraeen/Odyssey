package com.odyssey.world;

import com.odyssey.rendering.scene.RenderObject;
import com.odyssey.rendering.scene.Material;
import com.odyssey.rendering.mesh.Mesh;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.odyssey.core.VoxelEngine.CHUNK_SIZE;
import static org.lwjgl.opengl.GL45.*;

/**
 * A RenderObject wrapper for Chunk that integrates with the advanced rendering pipeline
 */
public class ChunkRenderObject extends RenderObject {
    private final Chunk chunk;
    private final ChunkMesh chunkMesh;
    
    public ChunkRenderObject(Chunk chunk) {
        super(new ChunkMesh(chunk), Material.createDielectric(new Vector3f(0.5f, 0.8f, 0.3f), 0.8f));
        this.chunk = chunk;
        this.chunkMesh = (ChunkMesh) getMesh();
        
        // Set position based on chunk coordinates
        Vector3f chunkWorldPos = new Vector3f(
            chunk.getPosition().x * CHUNK_SIZE,
            0,
            chunk.getPosition().z * CHUNK_SIZE
        );
        setPosition(chunkWorldPos);
    }
    
    // Inherited getMaterial() and getMesh() from parent class
    
    /**
     * Custom Mesh implementation for chunks
     */
    private static class ChunkMesh extends Mesh {
        private final Chunk chunk;
        
        public ChunkMesh(Chunk chunk) {
            this.chunk = chunk;
        }
        
        @Override
        public void render() {
            if (chunk.getVao() != 0) {
                glBindVertexArray(chunk.getVao());
                glDrawArrays(GL_TRIANGLES, 0, chunk.getVertexCount());
                glBindVertexArray(0);
            }
        }
        
        public void cleanup() {
            // Chunk handles its own cleanup
        }
        
        public void renderTransparent() {
            if (chunk.getTransparentVao() != 0) {
                glBindVertexArray(chunk.getTransparentVao());
                glDrawArrays(GL_TRIANGLES, 0, chunk.getTransparentVertexCount());
                glBindVertexArray(0);
            }
        }
    }
    public void renderTransparent() {
        if (chunkMesh instanceof ChunkMesh) {
            ((ChunkMesh) chunkMesh).renderTransparent();
        }
    }
    
    public Chunk getChunk() {
        return chunk;
    }
}