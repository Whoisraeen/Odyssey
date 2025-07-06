package com.odyssey.services;

import com.odyssey.world.BlockType;
import com.odyssey.world.Chunk;
import com.odyssey.world.ChunkPosition;
import com.odyssey.world.biome.Biome;
import org.joml.Vector3f;

/**
 * Service interface for world management operations
 */
public interface WorldService {
    
    /**
     * Get block type at world coordinates
     */
    BlockType getBlock(int x, int y, int z);
    
    /**
     * Set block type at world coordinates
     */
    void setBlock(int x, int y, int z, BlockType type);
    
    /**
     * Get chunk at specified position
     */
    Chunk getChunk(ChunkPosition position);
    
    /**
     * Get biome at world coordinates
     */
    Biome getBiomeAt(int x, int z);
    
    /**
     * Find safe spawn location
     */
    Vector3f findSafeSpawnLocation();
    
    /**
     * Load chunks around specified position
     */
    void loadChunksAround(Vector3f position, int radius);
    
    /**
     * Perform random ticks for world simulation
     */
    void performRandomTicks();
    
    /**
     * Update world systems
     */
    void update(float deltaTime);
    
    /**
     * Cleanup world resources
     */
    void cleanup();
    
    /**
     * Get the world generator
     */
    com.odyssey.world.WorldGenerator getWorldGenerator();
    
    /**
     * Check if a position is loaded
     */
    boolean isLoaded(int x, int z);
    
    /**
     * Get world metadata
     */
    com.odyssey.world.WorldMetadata getMetadata();
    
    /**
     * Save world data
     */
    void save();
    
    /**
     * Load world data
     */
    void load(String worldName);
    
    /**
     * Generate new chunk
     */
    void generateChunk(ChunkPosition position);
    
    /**
     * Unload chunk
     */
    void unloadChunk(ChunkPosition position);
    
    /**
     * Get light level at position
     */
    int getLightLevel(int x, int y, int z);
    
    /**
     * Update light at position
     */
    void updateLight(int x, int y, int z);
    
    /**
     * Get spawn position
     */
    Vector3f getSpawnPosition();
    
    /**
     * Set spawn position
     */
    void setSpawnPosition(Vector3f position);
}