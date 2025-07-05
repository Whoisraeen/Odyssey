package com.odyssey.world;

import com.odyssey.rendering.mesh.ChunkMesh;
import static com.odyssey.core.VoxelEngine.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class MeshGenerator {
    private final ExecutorService meshPool;
    
    public MeshGenerator(ExecutorService meshPool) {
        this.meshPool = meshPool;
    }
    
    public Future<ChunkMesh> generateMesh(Chunk chunk) {
        return meshPool.submit(() -> {
            List<Float> vertices = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            int vertexIndex = 0;

            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        BlockType blockType = chunk.getBlock(x, y, z);
                        if (!blockType.solid) continue;

                        // Check each face and add it if it's exposed
                        if (!chunk.getBlock(x, y + 1, z).solid) { // Top
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.TOP);
                        }
                        if (!chunk.getBlock(x, y - 1, z).solid) { // Bottom
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.BOTTOM);
                        }
                        if (!chunk.getBlock(x, y, z + 1).solid) { // Front
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.FRONT);
                        }
                        if (!chunk.getBlock(x, y, z - 1).solid) { // Back
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.BACK);
                        }
                        if (!chunk.getBlock(x + 1, y, z).solid) { // Right
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.RIGHT);
                        }
                        if (!chunk.getBlock(x - 1, y, z).solid) { // Left
                            vertexIndex = addFace(vertices, indices, x, y, z, vertexIndex, Faces.LEFT);
                        }
                    }
                }
            }
            
            float[] vertArray = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) vertArray[i] = vertices.get(i);
            
            int[] indArray = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) indArray[i] = indices.get(i);

            return new ChunkMesh(vertArray, indArray);
        });
    }

    private int addFace(List<Float> vertices, List<Integer> indices, int x, int y, int z, int vertexIndex, float[] face) {
        for (int i = 0; i < face.length; i += 8) {
            vertices.add(face[i] + x);
            vertices.add(face[i+1] + y);
            vertices.add(face[i+2] + z);
            vertices.add(face[i+3]); // Normal X
            vertices.add(face[i+4]); // Normal Y
            vertices.add(face[i+5]); // Normal Z
            vertices.add(face[i+6]); // UV X
            vertices.add(face[i+7]); // UV Y
        }
        indices.add(vertexIndex);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 3);
        return vertexIndex + 4;
    }
    
    private static class Faces {
        // float[] format: posX, posY, posZ, normalX, normalY, normalZ, uvX, uvY
        public static final float[] TOP = {
            0.0f, 1.0f, 1.0f, 0, 1, 0, 0, 0,
            1.0f, 1.0f, 1.0f, 0, 1, 0, 1, 0,
            0.0f, 1.0f, 0.0f, 0, 1, 0, 0, 1,
            1.0f, 1.0f, 0.0f, 0, 1, 0, 1, 1,
        };
        public static final float[] BOTTOM = {
            0.0f, 0.0f, 0.0f, 0, -1, 0, 0, 0,
            1.0f, 0.0f, 0.0f, 0, -1, 0, 1, 0,
            0.0f, 0.0f, 1.0f, 0, -1, 0, 0, 1,
            1.0f, 0.0f, 1.0f, 0, -1, 0, 1, 1,
        };
        public static final float[] FRONT = {
            1.0f, 1.0f, 1.0f, 0, 0, 1, 0, 0,
            0.0f, 1.0f, 1.0f, 0, 0, 1, 1, 0,
            1.0f, 0.0f, 1.0f, 0, 0, 1, 0, 1,
            0.0f, 0.0f, 1.0f, 0, 0, 1, 1, 1,
        };
        public static final float[] BACK = {
            0.0f, 1.0f, 0.0f, 0, 0, -1, 0, 0,
            1.0f, 1.0f, 0.0f, 0, 0, -1, 1, 0,
            0.0f, 0.0f, 0.0f, 0, 0, -1, 0, 1,
            1.0f, 0.0f, 0.0f, 0, 0, -1, 1, 1,
        };
        public static final float[] RIGHT = {
            1.0f, 1.0f, 0.0f, 1, 0, 0, 0, 0,
            1.0f, 1.0f, 1.0f, 1, 0, 0, 1, 0,
            1.0f, 0.0f, 0.0f, 1, 0, 0, 0, 1,
            1.0f, 0.0f, 1.0f, 1, 0, 0, 1, 1,
        };
        public static final float[] LEFT = {
            0.0f, 1.0f, 1.0f, -1, 0, 0, 0, 0,
            0.0f, 1.0f, 0.0f, -1, 0, 0, 1, 0,
            0.0f, 0.0f, 1.0f, -1, 0, 0, 0, 1,
            0.0f, 0.0f, 0.0f, -1, 0, 0, 1, 1,
        };
    }
} 