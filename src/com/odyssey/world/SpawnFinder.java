package com.odyssey.world;

import com.odyssey.core.VoxelEngine;
import org.joml.Vector3f;

public class SpawnFinder {
    
    private static final int SEARCH_RADIUS = 32;
    private static final int MAX_ATTEMPTS = 100;
    private static final int SEA_LEVEL = 62;
    private static final float PLAYER_HEIGHT = 1.8f;
    
    /**
     * Finds a safe spawn location around the world origin (0,0)
     * Prefers grass or dirt blocks above sea level and avoids water or steep terrain
     * 
     * @param worldGenerator The world generator to sample terrain
     * @return A safe spawn position as Vector3f
     */
    public static Vector3f findSafeSpawnLocation(WorldGenerator worldGenerator) {
        Vector3f bestSpawn = new Vector3f(0, SEA_LEVEL + 10, 0); // Fallback spawn
        int bestScore = -1;
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Generate random position within search radius
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * SEARCH_RADIUS;
            
            int x = (int) (Math.cos(angle) * distance);
            int z = (int) (Math.sin(angle) * distance);
            
            // Sample terrain height at this location
            int terrainHeight = sampleTerrainHeight(worldGenerator, x, z);
            
            // Check if this is a valid spawn location
            int score = evaluateSpawnLocation(worldGenerator, x, terrainHeight, z);
            
            if (score > bestScore) {
                bestScore = score;
                bestSpawn.set(x + 0.5f, terrainHeight + 1, z + 0.5f); // Center in block + 1 block above
            }
            
            // If we found a perfect spot, use it
            if (score >= 100) {
                break;
            }
        }
        
        System.out.println("Found spawn location at: " + bestSpawn + " with score: " + bestScore);
        return bestSpawn;
    }
    
    /**
     * Samples the terrain height at a given x,z coordinate
     * Uses the same logic as WorldGenerator but only calculates height
     */
    private static int sampleTerrainHeight(WorldGenerator worldGenerator, int worldX, int worldZ) {
        // Replicate WorldGenerator logic for height calculation
        double continentFrequency = 0.004;
        double continentNoise = (Math.sin(worldX * continentFrequency) + Math.cos(worldZ * continentFrequency)) / 2.0;
        
        double elevationFrequency = 0.03;
        double elevationNoise = (Math.sin(worldX * elevationFrequency) * 15 + Math.cos(worldZ * elevationFrequency) * 15);
        
        int height;
        if (continentNoise > 0.15) { // Threshold for land
            height = 40 + 15 + (int)elevationNoise; // SEA_FLOOR + 15 + elevation
        } else {
            height = 40; // Ocean floor
        }
        
        return height;
    }
    
    /**
     * Evaluates how good a spawn location is
     * Returns a score from 0-100, higher is better
     */
    private static int evaluateSpawnLocation(WorldGenerator worldGenerator, int x, int terrainHeight, int z) {
        int score = 0;
        
        // Must be above sea level
        if (terrainHeight <= SEA_LEVEL) {
            return 0; // Invalid spawn - underwater
        }
        
        // Prefer locations just above sea level (not too high)
        int heightAboveSea = terrainHeight - SEA_LEVEL;
        if (heightAboveSea >= 1 && heightAboveSea <= 10) {
            score += 30; // Good height range
        } else if (heightAboveSea <= 20) {
            score += 15; // Acceptable height
        }
        
        // Check for flat area (sample surrounding blocks)
        boolean isFlat = true;
        int flatBonus = 0;
        
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                int neighborHeight = sampleTerrainHeight(worldGenerator, x + dx, z + dz);
                int heightDiff = Math.abs(neighborHeight - terrainHeight);
                
                if (heightDiff > 2) {
                    isFlat = false;
                }
                
                if (heightDiff <= 1) {
                    flatBonus += 2;
                }
            }
        }
        
        if (isFlat) {
            score += 40; // Bonus for flat area
        }
        score += Math.min(flatBonus, 20); // Additional bonus for very flat areas
        
        // Determine block type at spawn location
        BlockType surfaceBlock = getSurfaceBlockType(worldGenerator, x, terrainHeight, z);
        
        // Prefer grass and dirt over other blocks
        switch (surfaceBlock) {
            case GRASS:
                score += 20;
                break;
            case DIRT:
                score += 15;
                break;
            case STONE:
                score += 5;
                break;
            default:
                // Other blocks are less desirable
                break;
        }
        
        // Ensure there's air space above for the player
        if (hasAirSpace(worldGenerator, x, terrainHeight + 1, z)) {
            score += 10;
        } else {
            return 0; // Invalid - no air space
        }
        
        return Math.min(score, 100);
    }
    
    /**
     * Determines what block type would be at the surface at given coordinates
     */
    private static BlockType getSurfaceBlockType(WorldGenerator worldGenerator, int x, int terrainHeight, int z) {
        // Replicate WorldGenerator surface block logic
        if (terrainHeight > SEA_LEVEL) {
            if (terrainHeight < VoxelEngine.CHUNK_HEIGHT) {
                // Check if it would be dirt (and thus have grass on top)
                if (terrainHeight > 40 + 15 + 5 - 5) { // Approximate dirt layer check
                    return BlockType.GRASS;
                } else {
                    return BlockType.DIRT;
                }
            }
        }
        return BlockType.STONE;
    }
    
    /**
     * Checks if there's enough air space above the spawn point for the player
     */
    private static boolean hasAirSpace(WorldGenerator worldGenerator, int x, int y, int z) {
        // Check if there are at least 2 blocks of air space above
        for (int dy = 0; dy < Math.ceil(PLAYER_HEIGHT) + 1; dy++) {
            int checkHeight = sampleTerrainHeight(worldGenerator, x, z);
            if (y + dy <= checkHeight) {
                return false; // Not enough air space
            }
        }
        return true;
    }
    
    /**
     * Finds a spawn location and validates it's safe for the player
     * This method can be called when creating a new world or respawning
     */
    public static Vector3f findAndValidateSpawn(WorldGenerator worldGenerator, VoxelEngine engine) {
        Vector3f spawnPos = findSafeSpawnLocation(worldGenerator);
        
        // Additional validation if we have access to the actual world
        if (engine != null) {
            // Ensure the blocks are actually generated and safe
            int x = (int) Math.floor(spawnPos.x);
            int y = (int) Math.floor(spawnPos.y);
            int z = (int) Math.floor(spawnPos.z);
            
            // Check if spawn location has solid ground
            BlockType groundBlock = engine.getBlock(x, y - 1, z);
            if (groundBlock == BlockType.AIR || groundBlock == BlockType.WATER) {
                // Fallback to a safe height if validation fails
                spawnPos.y = SEA_LEVEL + 20;
                System.out.println("Spawn validation failed, using fallback position: " + spawnPos);
            }
        }
        
        return spawnPos;
    }
}