package com.odyssey.world;

import com.odyssey.world.BlockType;
import com.odyssey.utility.SimplexNoise;
import com.odyssey.world.Chunk;
import com.odyssey.world.ChunkPosition;
import org.joml.Vector3i;

import java.util.Random;

import static com.odyssey.core.GameConstants.CHUNK_SIZE;
import static com.odyssey.core.GameConstants.MAX_HEIGHT;

public class CaveGenerator {
    private final SimplexNoise caveNoise1;
    private final SimplexNoise caveNoise2;
    private final SimplexNoise caveNoise3;
    private final SimplexNoise tunnelNoise;
    private final Random random;
    
    // Cave generation parameters
    private static final float CAVE_THRESHOLD = 0.6f;
    private static final float TUNNEL_THRESHOLD = 0.7f;
    private static final int MIN_CAVE_HEIGHT = 5;
    private static final int MAX_CAVE_HEIGHT = 50;
    private static final float CAVE_DENSITY = 0.02f;
    
    public CaveGenerator(long seed) {
        this.caveNoise1 = new SimplexNoise(seed + 100);
        this.caveNoise2 = new SimplexNoise(seed + 200);
        this.caveNoise3 = new SimplexNoise(seed + 300);
        this.tunnelNoise = new SimplexNoise(seed + 400);
        this.random = new Random(seed + 500);
    }
    
    public void generateCaves(Chunk chunk) {
        ChunkPosition position = chunk.getPosition();
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = MIN_CAVE_HEIGHT; y < MAX_CAVE_HEIGHT; y++) {
                    int worldX = position.x * CHUNK_SIZE + x;
                    int worldZ = position.z * CHUNK_SIZE + z;
                    
                    if (shouldGenerateCave(worldX, y, worldZ)) {
                        // Only carve out solid blocks
                        BlockType currentBlock = chunk.getBlock(x, y, z);
                        if (currentBlock == BlockType.STONE || currentBlock == BlockType.DIRT) {
                            chunk.setBlock(x, y, z, BlockType.AIR);
                            
                            // Add cave decorations
                            generateCaveDecorations(chunk, x, y, z);
                        }
                    }
                }
            }
        }
        
        // Generate tunnel systems
        generateTunnels(chunk);
    }
    
    private boolean shouldGenerateCave(int worldX, int y, int worldZ) {
        // Use multiple noise layers for complex cave shapes
        double scale1 = 0.02;
        double scale2 = 0.05;
        double scale3 = 0.1;
        
        double noise1 = caveNoise1.eval(worldX * scale1, y * scale1, worldZ * scale1);
        double noise2 = caveNoise2.eval(worldX * scale2, y * scale2, worldZ * scale2);
        double noise3 = caveNoise3.eval(worldX * scale3, y * scale3, worldZ * scale3);
        
        // Combine noise values for complex cave patterns
        double combinedNoise = (noise1 + noise2 * 0.5 + noise3 * 0.25) / 1.75;
        
        // Height-based cave probability (more caves at middle depths)
        float heightFactor = 1.0f - Math.abs(y - 25.0f) / 25.0f;
        heightFactor = Math.max(0.1f, heightFactor);
        
        return combinedNoise > CAVE_THRESHOLD * heightFactor;
    }
    
    private void generateTunnels(Chunk chunk) {
        ChunkPosition position = chunk.getPosition();
        
        // Generate horizontal tunnels
        for (int y = MIN_CAVE_HEIGHT; y < MAX_CAVE_HEIGHT; y += 5) {
            if (random.nextFloat() < 0.1f) { // 10% chance per layer
                generateTunnelLayer(chunk, y);
            }
        }
    }
    
    private void generateTunnelLayer(Chunk chunk, int y) {
        ChunkPosition position = chunk.getPosition();
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = position.x * CHUNK_SIZE + x;
                int worldZ = position.z * CHUNK_SIZE + z;
                
                double tunnelValue = tunnelNoise.eval(worldX * 0.03, y * 0.03, worldZ * 0.03);
                
                if (tunnelValue > TUNNEL_THRESHOLD) {
                    // Create tunnel with varying height
                    int tunnelHeight = 2 + random.nextInt(3); // 2-4 blocks high
                    
                    for (int dy = 0; dy < tunnelHeight && y + dy < MAX_HEIGHT; dy++) {
                        BlockType currentBlock = chunk.getBlock(x, y + dy, z);
                        if (currentBlock == BlockType.STONE || currentBlock == BlockType.DIRT) {
                            chunk.setBlock(x, y + dy, z, BlockType.AIR);
                        }
                    }
                }
            }
        }
    }
    
    private void generateCaveDecorations(Chunk chunk, int x, int y, int z) {
        // Add stalactites and stalagmites
        if (random.nextFloat() < 0.05f) { // 5% chance
            // Check if we can place decorations
            if (y > 0 && y < MAX_HEIGHT - 1) {
                BlockType above = chunk.getBlock(x, y + 1, z);
                BlockType below = chunk.getBlock(x, y - 1, z);
                
                // Stalactite (hanging from ceiling)
                if (above == BlockType.STONE && random.nextFloat() < 0.5f) {
                    // Use cobblestone as stalactite placeholder
                    if (y + 1 < MAX_HEIGHT) {
                        chunk.setBlock(x, y + 1, z, BlockType.COBBLESTONE);
                    }
                }
                
                // Stalagmite (rising from floor)
                if (below == BlockType.STONE && random.nextFloat() < 0.5f) {
                    // Use cobblestone as stalagmite placeholder
                    if (y - 1 >= 0) {
                        chunk.setBlock(x, y - 1, z, BlockType.COBBLESTONE);
                    }
                }
            }
        }
        
        // Add water pools in caves
        if (y <= 20 && random.nextFloat() < 0.01f) { // 1% chance below y=20
            if (chunk.getBlock(x, y - 1, z) == BlockType.STONE) {
                chunk.setBlock(x, y, z, BlockType.WATER);
            }
        }
        
        // Add lava pools in deep caves
        if (y <= 10 && random.nextFloat() < 0.005f) { // 0.5% chance below y=10
            if (chunk.getBlock(x, y - 1, z) == BlockType.STONE) {
                chunk.setBlock(x, y, z, BlockType.LAVA);
            }
        }
    }
    
    /**
     * Generate large caverns - bigger cave systems
     */
    public void generateCaverns(Chunk chunk) {
        ChunkPosition position = chunk.getPosition();
        
        // Check if this chunk should have a cavern (rare)
        int chunkHash = position.x * 31 + position.z * 17;
        Random chunkRandom = new Random(chunkHash);
        
        if (chunkRandom.nextFloat() < 0.02f) { // 2% chance per chunk
            generateLargeCavern(chunk, chunkRandom);
        }
    }
    
    private void generateLargeCavern(Chunk chunk, Random chunkRandom) {
        // Generate a large spherical cavern
        int centerX = chunkRandom.nextInt(CHUNK_SIZE);
        int centerY = 15 + chunkRandom.nextInt(20); // Between y=15 and y=35
        int centerZ = chunkRandom.nextInt(CHUNK_SIZE);
        
        int radius = 5 + chunkRandom.nextInt(8); // Radius 5-12 blocks
        
        for (int x = Math.max(0, centerX - radius); x < Math.min(CHUNK_SIZE, centerX + radius); x++) {
            for (int y = Math.max(0, centerY - radius); y < Math.min(MAX_HEIGHT, centerY + radius); y++) {
                for (int z = Math.max(0, centerZ - radius); z < Math.min(CHUNK_SIZE, centerZ + radius); z++) {
                    
                    double distance = Math.sqrt(
                        Math.pow(x - centerX, 2) + 
                        Math.pow(y - centerY, 2) + 
                        Math.pow(z - centerZ, 2)
                    );
                    
                    if (distance <= radius) {
                        BlockType currentBlock = chunk.getBlock(x, y, z);
                        if (currentBlock == BlockType.STONE || currentBlock == BlockType.DIRT) {
                            chunk.setBlock(x, y, z, BlockType.AIR);
                        }
                    }
                }
            }
        }
        
        // Add a small lake at the bottom of the cavern
        for (int x = Math.max(0, centerX - 3); x < Math.min(CHUNK_SIZE, centerX + 3); x++) {
            for (int z = Math.max(0, centerZ - 3); z < Math.min(CHUNK_SIZE, centerZ + 3); z++) {
                int lakeY = centerY - radius + 1;
                if (lakeY >= 0 && lakeY < com.odyssey.core.GameConstants.MAX_HEIGHT) {
                    if (chunk.getBlock(x, lakeY, z) == BlockType.AIR) {
                        chunk.setBlock(x, lakeY, z, BlockType.WATER);
                    }
                }
            }
        }
    }
}