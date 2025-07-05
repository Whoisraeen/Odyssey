package com.odyssey.world;

import static com.odyssey.core.VoxelEngine.CHUNK_HEIGHT;
import static com.odyssey.core.VoxelEngine.CHUNK_SIZE;
import com.odyssey.utility.SimplexNoise;
import com.odyssey.world.biome.Biome;

public class WorldGenerator {
    
    private static final int SEA_LEVEL = 62;
    private static final int SEA_FLOOR = 40;
    
    private final SimplexNoise elevationNoise;
    private final SimplexNoise temperatureNoise;
    private final SimplexNoise humidityNoise;
    private final long seed;

    public WorldGenerator() {
        this.seed = new java.util.Random().nextLong();
        this.elevationNoise = new SimplexNoise(seed);
        this.temperatureNoise = new SimplexNoise(seed + 1); // Use a different seed for each map
        this.humidityNoise = new SimplexNoise(seed + 2);
    }
    
    public WorldGenerator(long seed) {
        this.seed = seed;
        this.elevationNoise = new SimplexNoise(seed);
        this.temperatureNoise = new SimplexNoise(seed + 1); // Use a different seed for each map
        this.humidityNoise = new SimplexNoise(seed + 2);
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

                for (int y = 0; y < CHUNK_HEIGHT; y++) {
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
                if (height > SEA_LEVEL && height < CHUNK_HEIGHT) {
                    if (chunk.getBlock(x, height - 1, z) == BlockType.DIRT) {
                        chunk.setBlock(x, height, z, BlockType.GRASS);
                    }
                }
            }
        }
    }
}