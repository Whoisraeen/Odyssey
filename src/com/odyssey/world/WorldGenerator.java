package com.odyssey.world;

import static com.odyssey.core.VoxelEngine.CHUNK_HEIGHT;
import static com.odyssey.core.VoxelEngine.CHUNK_SIZE;

public class WorldGenerator {
    public void generateChunk(Chunk chunk) {
        // Simple terrain generation
        ChunkPosition position = chunk.getPosition();
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int height = 64 + (int)(Math.sin((position.x * CHUNK_SIZE + x) * 0.1) * 10 + Math.cos((position.z * CHUNK_SIZE + z) * 0.1) * 10);
                
                for (int y = 0; y < height && y < CHUNK_HEIGHT; y++) {
                    if (y < height - 3) {
                        chunk.setBlock(x, y, z, BlockType.STONE);
                    } else if (y < height - 1) {
                        chunk.setBlock(x, y, z, BlockType.DIRT);
                    } else {
                        chunk.setBlock(x, y, z, BlockType.GRASS);
                    }
                }
            }
        }
    }
} 