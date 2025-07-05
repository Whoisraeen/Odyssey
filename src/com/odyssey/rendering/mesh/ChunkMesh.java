package com.odyssey.rendering.mesh;

/**
 * A simple data holder for chunk mesh data, containing vertex and index arrays.
 * This class does no native memory allocation.
 */
public class ChunkMesh {
    private final float[] vertices;
    private final int[] indices;
    
    public ChunkMesh(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }
    
    public float[] getVertices() {
        return vertices;
    }
    
    public int[] getIndices() {
        return indices;
    }
    
    public int getVertexCount() {
        return vertices.length;
    }

    public int getIndexCount() {
        return indices.length;
    }
} 