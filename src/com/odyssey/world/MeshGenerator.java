package com.odyssey.world;

import com.odyssey.rendering.mesh.ChunkMesh;
import static com.odyssey.core.VoxelEngine.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.odyssey.core.GameConstants.*;

public class MeshGenerator {
    
    // Placeholder enum to resolve compilation errors.
    private enum Faces {
        TOP, BOTTOM, LEFT, RIGHT, FRONT, BACK;
    }

    private final ExecutorService executor;
    
    public MeshGenerator(ExecutorService executor) {
        this.executor = executor;
    }
    
    public Future<ChunkMesh[]> generateMesh(Chunk chunk) {
        return executor.submit(() -> {
            List<Float> opaqueVertices = new ArrayList<>();
            List<Float> transparentVertices = new ArrayList<>();
            List<Integer> opaqueIndices = new ArrayList<>();
            List<Integer> transparentIndices = new ArrayList<>();

            for (int y = 0; y < MAX_HEIGHT; y++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        BlockType blockType = chunk.getBlock(x, y, z);
                        if (!blockType.isTransparent() && blockType != BlockType.AIR) {
                            addBlockFaces(x, y, z, chunk, opaqueVertices, opaqueIndices, false);
                        } else if (blockType.isTransparent() && blockType != BlockType.AIR) {
                            addBlockFaces(x, y, z, chunk, transparentVertices, transparentIndices, true);
                        }
                    }
                }
            }

            return new ChunkMesh[]{
                new ChunkMesh(toFloatArray(opaqueVertices), toIntArray(opaqueIndices)),
                new ChunkMesh(toFloatArray(transparentVertices), toIntArray(transparentIndices))
            };
        });
    }

    private void addBlockFaces(int x, int y, int z, Chunk chunk, List<Float> vertices, List<Integer> indices, boolean forTransparent) {
        BlockType blockType = chunk.getBlock(x, y, z);
        if (blockType == BlockType.AIR) return;

        // Check each face and add it if it's exposed
        if (isFaceExposed(x, y + 1, z, chunk, forTransparent)) { // Top
            addFaceVertices(x, y, z, "TOP", blockType, vertices, indices);
        }
        if (isFaceExposed(x, y - 1, z, chunk, forTransparent)) { // Bottom
            addFaceVertices(x, y, z, "BOTTOM", blockType, vertices, indices);
        }
        if (isFaceExposed(x, y, z + 1, chunk, forTransparent)) { // Front
            addFaceVertices(x, y, z, "FRONT", blockType, vertices, indices);
        }
        if (isFaceExposed(x, y, z - 1, chunk, forTransparent)) { // Back
            addFaceVertices(x, y, z, "BACK", blockType, vertices, indices);
        }
        if (isFaceExposed(x + 1, y, z, chunk, forTransparent)) { // Right
            addFaceVertices(x, y, z, "RIGHT", blockType, vertices, indices);
        }
        if (isFaceExposed(x - 1, y, z, chunk, forTransparent)) { // Left
            addFaceVertices(x, y, z, "LEFT", blockType, vertices, indices);
        }
    }
    
    private boolean isFaceExposed(int x, int y, int z, Chunk chunk, boolean forTransparent) {
        if (y < 0 || y >= MAX_HEIGHT) return true; // Exposed at chunk boundaries
        BlockType adjacentBlock = chunk.getBlockInChunk(x, y, z);
        if (forTransparent) {
            return adjacentBlock == BlockType.AIR;
        } else {
            return adjacentBlock == BlockType.AIR || adjacentBlock.isTransparent();
        }
    }
    
    private void addFaceVertices(int x, int y, int z, String face, BlockType blockType, List<Float> vertices, List<Integer> indices) {
        float[][] faceVertices = getFaceVertices(x, y, z, face, blockType);
        
        // Get current vertex count to calculate indices
        int baseIndex = vertices.size() / 8; // 8 floats per vertex
        
        // Add vertices (each vertex has: position (3), normal (3), texture coords (2) = 8 floats)
        for (float[] vertex : faceVertices) {
            for (float component : vertex) {
                vertices.add(component);
            }
        }
        
        // Add indices for two triangles (6 vertices total)
        // Triangle 1: 0, 1, 2
        indices.add(baseIndex + 0);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        // Triangle 2: 3, 4, 5
        indices.add(baseIndex + 3);
        indices.add(baseIndex + 4);
        indices.add(baseIndex + 5);
    }
    
    private float[][] getFaceVertices(int x, int y, int z, String face, BlockType blockType) {
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;
        
        // Get texture UV coordinates from the block type
        float[] uv = blockType.getTextureUV(face);
        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];
        
        switch (face) {
            case "TOP":
                return new float[][] {
                    // Triangle 1
                    {x0, y1, z0,  0, 1, 0,  u0, v0}, // Bottom-left
                    {x1, y1, z0,  0, 1, 0,  u1, v0}, // Bottom-right
                    {x1, y1, z1,  0, 1, 0,  u1, v1}, // Top-right
                    // Triangle 2
                    {x0, y1, z0,  0, 1, 0,  u0, v0}, // Bottom-left
                    {x1, y1, z1,  0, 1, 0,  u1, v1}, // Top-right
                    {x0, y1, z1,  0, 1, 0,  u0, v1}  // Top-left
                };
            case "BOTTOM":
                return new float[][] {
                    // Triangle 1
                    {x0, y0, z0,  0, -1, 0,  u0, v0}, // Bottom-left
                    {x1, y0, z1,  0, -1, 0,  u1, v1}, // Top-right
                    {x1, y0, z0,  0, -1, 0,  u1, v0}, // Bottom-right
                    // Triangle 2
                    {x0, y0, z0,  0, -1, 0,  u0, v0}, // Bottom-left
                    {x0, y0, z1,  0, -1, 0,  u0, v1}, // Top-left
                    {x1, y0, z1,  0, -1, 0,  u1, v1}  // Top-right
                };
            case "FRONT":
                return new float[][] {
                    // Triangle 1
                    {x0, y0, z1,  0, 0, 1,  u0, v0}, // Bottom-left
                    {x1, y1, z1,  0, 0, 1,  u1, v1}, // Top-right
                    {x1, y0, z1,  0, 0, 1,  u1, v0}, // Bottom-right
                    // Triangle 2
                    {x0, y0, z1,  0, 0, 1,  u0, v0}, // Bottom-left
                    {x0, y1, z1,  0, 0, 1,  u0, v1}, // Top-left
                    {x1, y1, z1,  0, 0, 1,  u1, v1}  // Top-right
                };
            case "BACK":
                return new float[][] {
                    // Triangle 1
                    {x0, y0, z0,  0, 0, -1,  u1, v0}, // Bottom-right
                    {x1, y0, z0,  0, 0, -1,  u0, v0}, // Bottom-left
                    {x1, y1, z0,  0, 0, -1,  u0, v1}, // Top-left
                    // Triangle 2
                    {x0, y0, z0,  0, 0, -1,  u1, v0}, // Bottom-right
                    {x1, y1, z0,  0, 0, -1,  u0, v1}, // Top-left
                    {x0, y1, z0,  0, 0, -1,  u1, v1}  // Top-right
                };
            case "RIGHT":
                return new float[][] {
                    // Triangle 1
                    {x1, y0, z0,  1, 0, 0,  u0, v0}, // Bottom-left
                    {x1, y1, z1,  1, 0, 0,  u1, v1}, // Top-right
                    {x1, y1, z0,  1, 0, 0,  u0, v1}, // Top-left
                    // Triangle 2
                    {x1, y0, z0,  1, 0, 0,  u0, v0}, // Bottom-left
                    {x1, y0, z1,  1, 0, 0,  u1, v0}, // Bottom-right
                    {x1, y1, z1,  1, 0, 0,  u1, v1}  // Top-right
                };
            case "LEFT":
                return new float[][] {
                    // Triangle 1
                    {x0, y0, z0,  -1, 0, 0,  u1, v0}, // Bottom-right
                    {x0, y1, z0,  -1, 0, 0,  u1, v1}, // Top-right
                    {x0, y1, z1,  -1, 0, 0,  u0, v1}, // Top-left
                    // Triangle 2
                    {x0, y0, z0,  -1, 0, 0,  u1, v0}, // Bottom-right
                    {x0, y1, z1,  -1, 0, 0,  u0, v1}, // Top-left
                    {x0, y0, z1,  -1, 0, 0,  u0, v0}  // Bottom-left
                };
            default:
                return new float[0][0];
        }
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}