package com.odyssey.world;

import com.odyssey.services.WorldService;
import com.odyssey.world.biome.Biome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * World class that provides a simplified interface for entities to interact with the world.
 * This class wraps the WorldService to provide backward compatibility for entity classes.
 */
@Component
public class World {
    
    private final WorldService worldService;
    
    @Autowired
    public World(WorldService worldService) {
        this.worldService = worldService;
    }
    
    /**
     * Get block type at world coordinates
     */
    public BlockType getBlock(int x, int y, int z) {
        return worldService.getBlock(x, y, z);
    }
    
    /**
     * Set block type at world coordinates
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        worldService.setBlock(x, y, z, type);
    }
    
    /**
     * Get chunk at specified position
     */
    public Chunk getChunk(ChunkPosition position) {
        return worldService.getChunk(position);
    }
    
    /**
     * Get biome at world coordinates
     */
    public Biome getBiomeAt(int x, int z) {
        return worldService.getBiomeAt(x, z);
    }
    
    /**
     * Check if a position is loaded
     */
    public boolean isLoaded(int x, int z) {
        return worldService.isLoaded(x, z);
    }
    
    /**
     * Get world metadata
     */
    public WorldMetadata getMetadata() {
        return worldService.getMetadata();
    }
    
    /**
     * Save world data
     */
    public void save() {
        worldService.save();
    }
    
    /**
     * Load world data
     */
    public void load(String worldName) {
        worldService.load(worldName);
    }
    
    /**
     * Generate new chunk
     */
    public void generateChunk(ChunkPosition position) {
        worldService.generateChunk(position);
    }
    
    /**
     * Unload chunk
     */
    public void unloadChunk(ChunkPosition position) {
        worldService.unloadChunk(position);
    }
    
    /**
     * Get light level at position
     */
    public int getLightLevel(int x, int y, int z) {
        return worldService.getLightLevel(x, y, z);
    }
    
    /**
     * Update light at position
     */
    public void updateLight(int x, int y, int z) {
        worldService.updateLight(x, y, z);
    }
    
    /**
     * Get spawn position
     */
    public org.joml.Vector3f getSpawnPosition() {
        return worldService.getSpawnPosition();
    }
    
    /**
     * Set spawn position
     */
    public void setSpawnPosition(org.joml.Vector3f position) {
        worldService.setSpawnPosition(position);
    }
}