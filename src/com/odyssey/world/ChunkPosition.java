package com.odyssey.world;

import java.util.Objects;

/**
 * Chunk position with proper hashCode and equals
 */
public class ChunkPosition {
    public final int x, y, z;
    
    public ChunkPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChunkPosition)) return false;
        ChunkPosition other = (ChunkPosition) obj;
        return x == other.x && y == other.y && z == other.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
    
    @Override
    public String toString() {
        return "ChunkPos(" + x + ", " + y + ", " + z + ")";
    }
} 