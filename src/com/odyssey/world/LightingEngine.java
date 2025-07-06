package com.odyssey.world;

import com.odyssey.core.VoxelEngine;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles lighting calculations and propagation for the voxel world
 */
public class LightingEngine {
    private final VoxelEngine voxelEngine;
    private final Queue<LightUpdate> lightUpdates;
    private final Set<Vector3i> scheduledLightUpdates;
    
    // Light levels (0-15, where 15 is brightest)
    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_LIGHT_LEVEL = 0;
    
    // Light sources
    private static final Map<BlockType, Integer> LIGHT_SOURCES = new HashMap<>();
    static {
        LIGHT_SOURCES.put(BlockType.FIRE, 15);
        // Add more light sources as needed
    }
    
    // Light directions (6 neighbors)
    private static final Vector3i[] LIGHT_DIRECTIONS = {
        new Vector3i(1, 0, 0),   // East
        new Vector3i(-1, 0, 0),  // West
        new Vector3i(0, 1, 0),   // Up
        new Vector3i(0, -1, 0),  // Down
        new Vector3i(0, 0, 1),   // South
        new Vector3i(0, 0, -1),  // North
    };
    
    public LightingEngine(VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
        this.lightUpdates = new ConcurrentLinkedQueue<>();
        this.scheduledLightUpdates = new HashSet<>();
    }
    
    /**
     * Schedule a light update at the given position
     */
    public void scheduleLightUpdate(int x, int y, int z) {
        Vector3i pos = new Vector3i(x, y, z);
        if (!scheduledLightUpdates.contains(pos)) {
            lightUpdates.offer(new LightUpdate(x, y, z));
            scheduledLightUpdates.add(pos);
        }
    }
    
    /**
     * Process all pending light updates
     */
    public void processLightUpdates() {
        int maxUpdatesPerFrame = 100; // Limit to prevent lag
        int processed = 0;
        
        while (!lightUpdates.isEmpty() && processed < maxUpdatesPerFrame) {
            LightUpdate update = lightUpdates.poll();
            if (update != null) {
                Vector3i pos = new Vector3i(update.x, update.y, update.z);
                scheduledLightUpdates.remove(pos);
                propagateLight(update.x, update.y, update.z);
                processed++;
            }
        }
    }
    
    /**
     * Calculate and propagate light from a source position
     */
    private void propagateLight(int x, int y, int z) {
        BlockType sourceBlock = voxelEngine.getBlock(x, y, z);
        int lightLevel = getLightLevel(sourceBlock);
        
        if (lightLevel > 0) {
            // This is a light source, propagate light outward
            propagateLightFromSource(x, y, z, lightLevel);
        } else {
            // Check if this position should receive light from neighbors
            calculateLightAtPosition(x, y, z);
        }
    }
    
    /**
     * Propagate light from a light source using flood-fill algorithm
     */
    private void propagateLightFromSource(int sourceX, int sourceY, int sourceZ, int lightLevel) {
        Queue<LightNode> lightQueue = new LinkedList<>();
        Set<Vector3i> visited = new HashSet<>();
        
        lightQueue.offer(new LightNode(sourceX, sourceY, sourceZ, lightLevel));
        visited.add(new Vector3i(sourceX, sourceY, sourceZ));
        
        while (!lightQueue.isEmpty()) {
            LightNode current = lightQueue.poll();
            
            // Set light level at current position
            setLightLevel(current.x, current.y, current.z, current.lightLevel);
            
            // Propagate to neighbors if light level is high enough
            if (current.lightLevel > 1) {
                for (Vector3i dir : LIGHT_DIRECTIONS) {
                    int neighborX = current.x + dir.x;
                    int neighborY = current.y + dir.y;
                    int neighborZ = current.z + dir.z;
                    
                    Vector3i neighborPos = new Vector3i(neighborX, neighborY, neighborZ);
                    
                    if (!visited.contains(neighborPos)) {
                        BlockType neighborBlock = voxelEngine.getBlock(neighborX, neighborY, neighborZ);
                        
                        // Light can pass through transparent blocks
                        if (isTransparent(neighborBlock)) {
                            int newLightLevel = current.lightLevel - 1;
                            lightQueue.offer(new LightNode(neighborX, neighborY, neighborZ, newLightLevel));
                            visited.add(neighborPos);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Calculate light level at a position based on neighbors
     */
    private void calculateLightAtPosition(int x, int y, int z) {
        BlockType block = voxelEngine.getBlock(x, y, z);
        
        // Opaque blocks don't receive light
        if (!isTransparent(block)) {
            setLightLevel(x, y, z, 0);
            return;
        }
        
        int maxNeighborLight = 0;
        
        // Check all neighbors for light
        for (Vector3i dir : LIGHT_DIRECTIONS) {
            int neighborX = x + dir.x;
            int neighborY = y + dir.y;
            int neighborZ = z + dir.z;
            
            int neighborLight = getLightLevelAt(neighborX, neighborY, neighborZ);
            maxNeighborLight = Math.max(maxNeighborLight, neighborLight);
        }
        
        // Light diminishes by 1 for each block it travels through
        int lightLevel = Math.max(0, maxNeighborLight - 1);
        setLightLevel(x, y, z, lightLevel);
    }
    
    /**
     * Get the light level emitted by a block type
     */
    private int getLightLevel(BlockType blockType) {
        return LIGHT_SOURCES.getOrDefault(blockType, 0);
    }
    
    /**
     * Check if a block type is transparent to light
     */
    private boolean isTransparent(BlockType blockType) {
        return blockType.isTransparent() || blockType == BlockType.AIR;
    }
    
    /**
     * Get the current light level at a position
     */
    public int getLightLevelAt(int x, int y, int z) {
        // For now, return a simple calculation
        // In a full implementation, this would be stored per-block
        BlockType block = voxelEngine.getBlock(x, y, z);
        
        // Light sources emit their own light
        int emittedLight = getLightLevel(block);
        if (emittedLight > 0) {
            return emittedLight;
        }
        
        // Sky light (simplified - full daylight at surface)
        if (y > 100) {
            return MAX_LIGHT_LEVEL;
        }
        
        // Underground gets progressively darker
        return Math.max(0, (y - 20) / 8);
    }
    
    /**
     * Set the light level at a position (placeholder for future implementation)
     */
    private void setLightLevel(int x, int y, int z, int lightLevel) {
        // In a full implementation, this would store light data in chunks
        // For now, this is a placeholder
    }
    
    /**
     * Called when a block changes to update lighting
     */
    public void onBlockChanged(int x, int y, int z, BlockType oldType, BlockType newType) {
        // Schedule light updates for the changed block and its neighbors
        scheduleLightUpdate(x, y, z);
        
        for (Vector3i dir : LIGHT_DIRECTIONS) {
            scheduleLightUpdate(x + dir.x, y + dir.y, z + dir.z);
        }
        
        // If a light source was added or removed, update more extensively
        boolean wasLightSource = getLightLevel(oldType) > 0;
        boolean isLightSource = getLightLevel(newType) > 0;
        
        if (wasLightSource || isLightSource) {
            // Schedule updates in a larger radius for light sources
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        if (dx*dx + dy*dy + dz*dz <= 25) { // Within radius of 5
                            scheduleLightUpdate(x + dx, y + dy, z + dz);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Represents a pending light update
     */
    private static class LightUpdate {
        final int x, y, z;
        
        LightUpdate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    /**
     * Represents a node in the light propagation algorithm
     */
    private static class LightNode {
        final int x, y, z;
        final int lightLevel;
        
        LightNode(int x, int y, int z, int lightLevel) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lightLevel = lightLevel;
        }
    }
}