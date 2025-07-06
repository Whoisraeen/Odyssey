package com.odyssey.services.impl;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.WorldService;
import com.odyssey.world.*;
import com.odyssey.world.biome.Biome;
import com.odyssey.effects.ParticleSystem;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.odyssey.rendering.mesh.ChunkMesh;

@Service
public class WorldServiceImpl implements WorldService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldServiceImpl.class);
    
    @Autowired
    private GameConfiguration config;
    
    private final Map<ChunkPosition, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<ChunkPosition, Future<ChunkMesh[]>> meshingFutures = new ConcurrentHashMap<>();
    private final ExecutorService meshGenerationPool;
    private final WorldGenerator worldGenerator;
    private final MeshGenerator meshGenerator;
    private final BlockUpdateManager blockUpdateManager;
    private final FluidSimulation fluidSimulation;
    private final LightingEngine lightingEngine;
    private final Random random = new Random();
    
    private ParticleSystem particleSystem;
    private boolean chunksChanged = true;
    private Vector3f spawnPosition = new Vector3f(0, 72, 0); // Default spawn position
    
    public WorldServiceImpl() {
        this.meshGenerationPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.worldGenerator = new WorldGenerator();
        this.meshGenerator = new MeshGenerator(meshGenerationPool);
        this.blockUpdateManager = new BlockUpdateManager(null, null); // Will be set later
        this.fluidSimulation = new FluidSimulation(null); // Will be set later
        this.lightingEngine = new LightingEngine(null); // Will be set later
    }
    
    public void setParticleSystem(ParticleSystem particleSystem) {
        this.particleSystem = particleSystem;
    }
    
    @Override
    public BlockType getBlock(int x, int y, int z) {
        if (y < 0 || y >= config.world().chunkHeight()) {
            return BlockType.AIR;
        }

        ChunkPosition chunkPos = new ChunkPosition(
            (int) Math.floor((float) x / config.world().chunkSize()),
            0,
            (int) Math.floor((float) z / config.world().chunkSize())
        );

        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) {
            return BlockType.STONE; // Treat unloaded chunks as solid
        }

        int localX = x - chunkPos.x * config.world().chunkSize();
        int localZ = z - chunkPos.z * config.world().chunkSize();
        
        if (localX < 0) localX += config.world().chunkSize();
        if (localZ < 0) localZ += config.world().chunkSize();

        return chunk.getBlock(localX, y, localZ);
    }
    
    @Override
    public void setBlock(int x, int y, int z, BlockType type) {
        if (y < 0 || y >= config.world().chunkHeight()) {
            return;
        }

        ChunkPosition chunkPos = new ChunkPosition(
            (int) Math.floor((float) x / config.world().chunkSize()),
            0,
            (int) Math.floor((float) z / config.world().chunkSize())
        );

        Chunk chunk = chunks.get(chunkPos);
        if (chunk != null) {
            int localX = x - chunkPos.x * config.world().chunkSize();
            int localZ = z - chunkPos.z * config.world().chunkSize();
            
            if (localX < 0) localX += config.world().chunkSize();
            if (localZ < 0) localZ += config.world().chunkSize();
            
            BlockType oldType = chunk.getBlock(localX, y, localZ);
            chunk.setBlock(localX, y, localZ, type);
            
            // Trigger system updates
            blockUpdateManager.notifyNeighbors(x, y, z, oldType, type);
            blockUpdateManager.onBlockChanged(x, y, z, oldType, type);
            fluidSimulation.onBlockChanged(x, y, z, oldType, type);
            lightingEngine.onBlockChanged(x, y, z, oldType, type);
            
            // Create particles for certain block changes
            if (particleSystem != null && type == BlockType.WATER && oldType == BlockType.AIR) {
                particleSystem.createWaterSplashParticles(x, y, z);
            }
            
            // Invalidate mesh
            if (chunk.getVao() != 0) {
                chunk.cleanup();
            }
            chunksChanged = true;
            
            logger.debug("Block changed at ({}, {}, {}) from {} to {}", x, y, z, oldType, type);
        }
    }
    
    @Override
    public Chunk getChunk(ChunkPosition position) {
        return chunks.get(position);
    }
    
    @Override
    public Biome getBiomeAt(int x, int z) {
        ChunkPosition chunkPos = new ChunkPosition(
            x / config.world().chunkSize(), 0, z / config.world().chunkSize());
        Chunk chunk = chunks.get(chunkPos);
        
        if (chunk == null) {
            return worldGenerator.getBiome(x, z);
        }
        
        int localX = x % config.world().chunkSize();
        if (localX < 0) localX += config.world().chunkSize();
        int localZ = z % config.world().chunkSize();
        if (localZ < 0) localZ += config.world().chunkSize();
        
        return chunk.getBiome(localX, localZ);
    }
    
    @Override
    public Vector3f findSafeSpawnLocation() {
        return SpawnFinder.findSafeSpawnLocation(worldGenerator);
    }
    
    @Override
    public void loadChunksAround(Vector3f position, int radius) {
        ChunkPosition centerChunk = new ChunkPosition(
            (int)Math.floor(position.x / config.world().chunkSize()),
            0,
            (int)Math.floor(position.z / config.world().chunkSize())
        );
        
        for (int x = centerChunk.x - radius; x <= centerChunk.x + radius; x++) {
            for (int z = centerChunk.z - radius; z <= centerChunk.z + radius; z++) {
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                if (!chunks.containsKey(pos)) {
                    Chunk chunk = new Chunk(pos);
                    worldGenerator.generateChunk(chunk);
                    chunks.put(pos, chunk);
                    logger.debug("Generated chunk at {}", pos);
                }
            }
        }
    }
    
    @Override
    public void performRandomTicks() {
        int ticksPerFrame = config.performance().randomTicksPerChunk();
        
        for (Chunk chunk : chunks.values()) {
            if (chunk == null) continue;
            
            for (int i = 0; i < ticksPerFrame; i++) {
                int x = random.nextInt(config.world().chunkSize());
                int y = random.nextInt(config.world().chunkHeight());
                int z = random.nextInt(config.world().chunkSize());
                
                BlockType block = chunk.getBlock(x, y, z);
                Vector3i globalPos = new Vector3i(
                    chunk.getPosition().x * config.world().chunkSize() + x, 
                    y, 
                    chunk.getPosition().z * config.world().chunkSize() + z
                );

                handleBlockTick(block, globalPos);
            }
        }
    }
    
    private void handleBlockTick(BlockType block, Vector3i globalPos) {
        // Wheat growth
        if (block.getId() >= BlockType.WHEAT_0.getId() && block.getId() < BlockType.WHEAT_7.getId()) {
            if (random.nextFloat() < 0.1f) { // Growth chance
                BlockType nextStage = BlockType.fromId(block.getId() + 1);
                setBlock(globalPos.x, globalPos.y, globalPos.z, nextStage);
            }
        }
        
        // Fire behavior
        if (block == BlockType.FIRE) {
            if (particleSystem != null && random.nextFloat() < 0.3f) {
                particleSystem.createFireParticles(globalPos.x, globalPos.y, globalPos.z);
            }
            
            if (random.nextFloat() < 0.2f) { // Burnout chance
                if (particleSystem != null) {
                    particleSystem.createSmokeParticles(globalPos.x, globalPos.y, globalPos.z);
                }
                setBlock(globalPos.x, globalPos.y, globalPos.z, BlockType.AIR);
                return;
            }

            // Fire spread
            if (random.nextFloat() < 0.5f) {
                for (int face = 0; face < 6; face++) {
                    Vector3i neighborPos = new Vector3i(globalPos).add(Block.FACE_OFFSETS[face]);
                    BlockType neighborBlock = getBlock(neighborPos.x, neighborPos.y, neighborPos.z);
                    if (isFlammable(neighborBlock)) {
                        setBlock(neighborPos.x, neighborPos.y, neighborPos.z, BlockType.FIRE);
                    }
                }
            }
        }
    }
    
    private boolean isFlammable(BlockType blockType) {
        return blockType == BlockType.WOOD || blockType == BlockType.LEAVES;
    }
    
    @Override
    public void update(float deltaTime) {
        // Process block updates
        blockUpdateManager.processUpdates();
        
        // Process fluid simulation
        fluidSimulation.processFluidUpdates();
        
        // Process lighting updates
        lightingEngine.processLightUpdates();
        
        // Check for completed mesh futures
        meshingFutures.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone()) {
                try {
                    ChunkMesh[] meshes = entry.getValue().get();
                    Chunk chunk = chunks.get(entry.getKey());
                    if (chunk != null) {
                        chunk.uploadMeshToGPU(meshes[0], meshes[1]);
                        chunksChanged = true;
                        logger.debug("Mesh uploaded for chunk {}", entry.getKey());
                    }
                } catch (Exception e) {
                    logger.error("Error uploading mesh for chunk {}: {}", entry.getKey(), e.getMessage());
                }
                return true;
            }
            return false;
        });

        // Request new meshes
        for (Chunk chunk : chunks.values()) {
            if (chunk.getVao() == 0 && !meshingFutures.containsKey(chunk.getPosition())) {
                meshingFutures.put(chunk.getPosition(), meshGenerator.generateMesh(chunk));
            }
        }
    }
    
    public boolean hasChunksChanged() {
        return chunksChanged;
    }
    
    public void markChunksUpdated() {
        chunksChanged = false;
    }
    
    public Map<ChunkPosition, Chunk> getChunks() {
        return chunks;
    }
    
    @Override
    public void cleanup() {
        meshGenerationPool.shutdown();
        chunks.values().forEach(Chunk::cleanup);
        chunks.clear();
        logger.info("World service cleaned up");
    }
    
    @Override
    public com.odyssey.world.WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
    
    @Override
    public boolean isLoaded(int x, int z) {
        ChunkPosition chunkPos = new ChunkPosition(
            x / config.world().chunkSize(), 0, z / config.world().chunkSize());
        return chunks.containsKey(chunkPos);
    }
    
    @Override
    public com.odyssey.world.WorldMetadata getMetadata() {
        // Return a basic metadata object
        return new com.odyssey.world.WorldMetadata("default");
    }
    
    @Override
    public void save() {
        // TODO: Implement world saving
        logger.info("World save requested (not yet implemented)");
    }
    
    @Override
    public void load(String worldName) {
        // TODO: Implement world loading
        logger.info("World load requested for: {} (not yet implemented)", worldName);
    }
    
    @Override
    public void generateChunk(ChunkPosition position) {
        if (!chunks.containsKey(position)) {
            Chunk chunk = new Chunk(position);
            worldGenerator.generateChunk(chunk);
            chunks.put(position, chunk);
            logger.debug("Generated chunk at {}", position);
        }
    }
    
    @Override
    public void unloadChunk(ChunkPosition position) {
        Chunk chunk = chunks.remove(position);
        if (chunk != null) {
            chunk.cleanup();
            logger.debug("Unloaded chunk at {}", position);
        }
    }
    
    @Override
    public int getLightLevel(int x, int y, int z) {
        // TODO: Implement proper lighting system
        return 15; // Full light for now
    }
    
    @Override
    public void updateLight(int x, int y, int z) {
        // TODO: Implement light updates
        logger.debug("Light update requested at ({}, {}, {})", x, y, z);
    }
    
    @Override
    public Vector3f getSpawnPosition() {
        return new Vector3f(spawnPosition);
    }
    
    @Override
    public void setSpawnPosition(Vector3f position) {
        this.spawnPosition.set(position);
        logger.debug("Spawn position set to ({}, {}, {})", position.x, position.y, position.z);
    }
}