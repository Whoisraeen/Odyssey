package com.odyssey.world;

import com.odyssey.rendering.scene.RenderObject;
import com.odyssey.rendering.scene.Material;
import com.odyssey.rendering.mesh.Mesh;
import com.odyssey.rendering.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.odyssey.core.GameConstants.CHUNK_SIZE;
import static org.lwjgl.opengl.GL45.*;

/**
 * A RenderObject wrapper for Chunk that integrates with the advanced rendering pipeline
 */
public class ChunkRenderObject extends RenderObject {
    private final Chunk chunk;
    private final ChunkMesh chunkMesh;
    
    // Shared terrain texture for all chunks
    private static int terrainTexture = 0;
    
    public ChunkRenderObject(Chunk chunk) {
        super(new ChunkMesh(chunk), createChunkMaterial());
        this.chunk = chunk;
        this.chunkMesh = (ChunkMesh) getMesh();
        
        // Set position based on chunk coordinates
        if (chunk != null && chunk.getPosition() != null) {
            Vector3f chunkWorldPos = new Vector3f(
                chunk.getPosition().x * CHUNK_SIZE,
                0,
                chunk.getPosition().z * CHUNK_SIZE
            );
            setPosition(chunkWorldPos);
        } else {
            System.err.println("ERROR: ChunkRenderObject created with null chunk or null chunk position!");
            setPosition(new Vector3f(0, 0, 0));
        }
    }
    
    /**
     * Creates a material with the terrain texture for chunks
     */
    private static Material createChunkMaterial() {
        // Load terrain texture if not already loaded
        if (terrainTexture == 0) {
            try {
                terrainTexture = Texture.loadTexture("assets/textures/terrain.png");
                if (terrainTexture == 0) {
                    System.err.println("CRITICAL ERROR: Failed to load terrain texture - chunks will be invisible!");
                    // Try fallback texture path
                    terrainTexture = Texture.loadTexture("assets/textures/terrain.png");
                    if (terrainTexture == 0) {
                        System.err.println("CRITICAL ERROR: Fallback terrain texture also failed to load!");
                    } else {
                        System.out.println("DEBUG: Loaded terrain texture from fallback path");
                    }
                } else {
                    System.out.println("DEBUG: Terrain texture loaded successfully");
                }
            } catch (Exception e) {
                System.err.println("CRITICAL ERROR: Exception loading terrain texture: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Create material with terrain texture
        Material material = Material.createDielectric(new Vector3f(1.0f, 1.0f, 1.0f), 0.8f);
        if (material == null) {
            System.err.println("ERROR: Failed to create dielectric material!");
            material = new Material(); // Fallback to default material
        }
        material.setAlbedoTexture(terrainTexture);
        return material;
    }
    
    // Inherited getMaterial() and getMesh() from parent class
    
    /**
     * Custom Mesh implementation for chunks
     */
    private static class ChunkMesh extends Mesh {
        private final Chunk chunk;
        
        public ChunkMesh(Chunk chunk) {
            if (chunk == null) {
                System.err.println("ERROR: ChunkMesh created with null chunk!");
                throw new IllegalArgumentException("Chunk cannot be null");
            }
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