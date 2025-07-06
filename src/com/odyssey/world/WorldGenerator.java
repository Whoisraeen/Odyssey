package com.odyssey.world;

import static com.odyssey.core.GameConstants.CHUNK_SIZE;
import static com.odyssey.core.GameConstants.MAX_HEIGHT;
import com.odyssey.utility.SimplexNoise;
import com.odyssey.world.biome.Biome;
import java.util.Random;

public class WorldGenerator {
    
    private static final int SEA_LEVEL = 62;
    private static final int SEA_FLOOR = 40;
    
    private final SimplexNoise elevationNoise;
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final SimplexNoise oreNoise;
    private final Random random;
    private final long seed;
    private final CaveGenerator caveGenerator;

    public WorldGenerator() {
        this.seed = new java.util.Random().nextLong();
        this.elevationNoise = new SimplexNoise(seed);
        this.temperatureNoise = new SimplexNoise(seed + 1); // Use a different seed for each map
        this.humidityNoise = new SimplexNoise(seed + 2);
        this.oreNoise = new SimplexNoise(seed + 3);
        this.random = new Random(seed);
        this.caveGenerator = new CaveGenerator(seed);
    }
    
    public WorldGenerator(long seed) {
        this.seed = seed;
        this.elevationNoise = new SimplexNoise(seed);
        this.temperatureNoise = new SimplexNoise(seed + 1); // Use a different seed for each map
        this.humidityNoise = new SimplexNoise(seed + 2);
        this.oreNoise = new SimplexNoise(seed + 3);
        this.random = new Random(seed);
        this.caveGenerator = new CaveGenerator(seed);
    }
    
    public long getSeed() {
        return seed;
    }
    
    public Biome getBiome(int x, int z) {
        float scale = 0.001f;
        float temp = (float) temperatureNoise.eval(x * scale, z * scale); // Range -1 to 1
        float humidity = (float) humidityNoise.eval(x * scale, z * scale); // Range -1 to 1
        
        if (temp > 0.5f && humidity < -0.3f) {
            return Biome.DESERT;
        } else if (temp < -0.5f) {
            return Biome.TUNDRA;
        } else if (temp > 0.6f && humidity > 0.6f) {
            return Biome.SWAMP;
        } else if (temp > 0.2f && humidity < 0.5f) { // Mountains are often drier
            // We can also use elevation to determine mountains, but this is a start
            return Biome.MOUNTAINS;
        }
        
        // Use elevation to distinguish between ocean and forest
        int height = (int) (elevationNoise.eval(x * 0.005, z * 0.005) * 40 + 60);
        if (height < 62) { // Water level
            return Biome.OCEAN;
        }

        return Biome.FOREST; // Default
    }

    public void generateChunk(Chunk chunk) {
        ChunkPosition position = chunk.getPosition();
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int worldX = position.x * CHUNK_SIZE + x;
                int worldZ = position.z * CHUNK_SIZE + z;

                // Low-frequency noise for continent/ocean separation
                double continentFrequency = 0.004;
                double continentNoise = (Math.sin(worldX * continentFrequency) + Math.cos(worldZ * continentFrequency)) / 2.0;

                // Higher-frequency noise for terrain elevation
                double elevationFrequency = 0.03;
                double elevationNoise = (Math.sin(worldX * elevationFrequency) * 15 + Math.cos(worldZ * elevationFrequency) * 15);

                int height;
                if (continentNoise > 0.15) { // Threshold for land
                    height = SEA_FLOOR + 15 + (int)elevationNoise;
                } else {
                    height = SEA_FLOOR; // Ocean floor
                }

                Biome biome = getBiome(worldX, worldZ);
                chunk.setBiome(x, z, biome);
                
                // Modify terrain based on biome
                if (biome == Biome.MOUNTAINS) {
                    height += elevationNoise * 50; // Extra noise for ruggedness
                }

                for (int y = 0; y < MAX_HEIGHT; y++) {
                    if (y < height) {
                        if (y < height - 5) {
                            chunk.setBlock(x, y, z, BlockType.STONE);
                        } else {
                            chunk.setBlock(x, y, z, BlockType.DIRT);
                        }
                    } else if (y <= SEA_LEVEL) {
                        chunk.setBlock(x, y, z, BlockType.WATER);
                    } else {
                        chunk.setBlock(x, y, z, BlockType.AIR);
                    }
                }
                
                // Place grass on top of dirt if it's just above sea level
                if (height > SEA_LEVEL && height < MAX_HEIGHT) {
                    if (chunk.getBlock(x, height - 1, z) == BlockType.DIRT) {
                        chunk.setBlock(x, height, z, BlockType.GRASS);
                    }
                }
            }
        }
        
        // Generate ores after terrain generation
        generateOres(chunk);
        
        // Generate caves and caverns
        caveGenerator.generateCaves(chunk);
        caveGenerator.generateCaverns(chunk);
    }
    
    /**
     * Generate ore deposits in the chunk
     */
    private void generateOres(Chunk chunk) {
        ChunkPosition position = chunk.getPosition();
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = 1; y < MAX_HEIGHT - 1; y++) {
                    int worldX = position.x * CHUNK_SIZE + x;
                    int worldZ = position.z * CHUNK_SIZE + z;
                    
                    // Only replace stone blocks with ores
                    if (chunk.getBlock(x, y, z) == BlockType.STONE) {
                        BlockType oreType = generateOreAt(worldX, y, worldZ);
                        if (oreType != null) {
                            chunk.setBlock(x, y, z, oreType);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Determine what ore (if any) should be generated at a specific location
     */
    private BlockType generateOreAt(int worldX, int y, int worldZ) {
        // Use noise to create ore veins
        double oreValue = oreNoise.eval(worldX * 0.1, y * 0.1, worldZ * 0.1);
        
        // Coal ore (most common, found at all depths)
        if (oreValue > 0.7 && random.nextFloat() < 0.02f) {
            return BlockType.COAL_ORE;
        }
        
        // Iron ore (common, found below y=64)
        if (y < 64 && oreValue > 0.75 && random.nextFloat() < 0.015f) {
            return BlockType.IRON_ORE;
        }
        
        // Gold ore (rare, found below y=32)
        if (y < 32 && oreValue > 0.8 && random.nextFloat() < 0.008f) {
            return BlockType.GOLD_ORE;
        }
        
        // Diamond ore (very rare, found below y=16)
        if (y < 16 && oreValue > 0.85 && random.nextFloat() < 0.003f) {
            return BlockType.DIAMOND_ORE;
        }
        
        return null; // No ore
    }
}