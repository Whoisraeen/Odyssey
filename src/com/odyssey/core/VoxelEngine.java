package com.odyssey.core;

import com.odyssey.rendering.ShaderManager;

import com.odyssey.rendering.Camera;
import com.odyssey.rendering.AdvancedRenderingPipeline;
import com.odyssey.rendering.scene.Scene;
import com.odyssey.player.Player;
import com.odyssey.world.Block;
import com.odyssey.world.ChunkRenderObject;
import com.odyssey.world.Chunk;
import com.odyssey.world.ChunkManager;
import com.odyssey.world.ChunkPosition;
import com.odyssey.world.MeshGenerator;
import com.odyssey.world.WorldGenerator;
import com.odyssey.world.SpawnFinder;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.odyssey.rendering.mesh.ChunkMesh;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

import com.odyssey.input.InputManager;
import com.odyssey.math.Vector2f;
import com.odyssey.physics.Raycaster;
import com.odyssey.input.GameAction;
import com.odyssey.rendering.SelectionBoxRenderer;
import com.odyssey.player.ItemStack;
import com.odyssey.entity.EntityManager;
import com.odyssey.ship.Shipyard;
import com.odyssey.world.BlockType;
import com.odyssey.entity.Ship;
import com.odyssey.entity.Entity;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.environment.WeatherType;
import com.odyssey.audio.SoundManager;
import com.odyssey.world.biome.Biome;

import java.util.Random;

/**
 * Core Voxel Engine - High-performance world management with modern OpenGL
 * Features: Greedy meshing, multi-threaded chunk loading, memory optimization
 */
@SuppressWarnings("unused")
public class VoxelEngine {
    
    // World configuration
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int WORLD_HEIGHT = 16; // chunks vertically
    public static final int RENDER_DISTANCE = 16;
    
    // Threading
    private final ExecutorService meshGenerationPool;
    
    // Core systems
    private final Camera camera;
    private final ShaderManager shaderManager;
    private final MeshGenerator meshGenerator;
    private final WorldGenerator worldGenerator;
    private final Player player;
    private final SelectionBoxRenderer selectionBoxRenderer;
    private final EntityManager entityManager;
    private final EnvironmentManager environmentManager;
    private final AdvancedRenderingPipeline renderingPipeline;
    private final Scene scene;
    private InputManager inputManager;
    
    private final Map<ChunkPosition, Chunk> chunks = new ConcurrentHashMap<>();
    private final Map<ChunkPosition, Future<ChunkMesh[]>> meshingFutures = new ConcurrentHashMap<>();
    private int chunkShaderProgram;
    private boolean chunksChanged = true; // Flag to track when scene needs updating
    
    private Raycaster.RaycastResult selectedBlock;
    private final Random random = new Random();
    private int debugFrameCounter = 0;

    public VoxelEngine(SoundManager soundManager, int width, int height) {
        this.meshGenerationPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.camera = new Camera();
        this.shaderManager = new ShaderManager();
        this.meshGenerator = new MeshGenerator(meshGenerationPool);
        this.worldGenerator = new WorldGenerator();
        // Find a safe spawn location using SpawnFinder
        Vector3f spawnLocation = SpawnFinder.findSafeSpawnLocation(worldGenerator);
        
        // Validate spawn location
        if (!Float.isFinite(spawnLocation.x) || !Float.isFinite(spawnLocation.y) || !Float.isFinite(spawnLocation.z)) {
            System.err.println("Error: Invalid spawn location returned: " + spawnLocation + ". Using safe fallback.");
            spawnLocation.set(0, 72, 0); // Safe fallback above sea level
        }
        
        this.player = new Player(spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
        // Position camera at player location
        float cameraY = spawnLocation.y + 1.7f;
        if (Float.isFinite(cameraY)) {
            camera.setPosition(spawnLocation.x, cameraY, spawnLocation.z);
        } else {
            System.err.println("Error: Invalid camera Y position calculated: " + cameraY);
            camera.setPosition(spawnLocation.x, spawnLocation.y + 2.0f, spawnLocation.z);
        }
        camera.setPitch(-20.0f); // Look slightly downward to see terrain
        camera.setYaw(45.0f); // Look towards positive X and Z direction where chunks are
        this.selectionBoxRenderer = new SelectionBoxRenderer();
        this.entityManager = new EntityManager();
        this.environmentManager = new EnvironmentManager(soundManager);
        
        // Initialize advanced rendering pipeline
        this.renderingPipeline = new AdvancedRenderingPipeline(width, height, environmentManager);
        this.scene = new Scene();
        scene.setupDefaultLighting();

        // Load shaders with validation
        this.chunkShaderProgram = shaderManager.loadProgram("shaders/geometry.vert", "shaders/geometry.frag");
        
        // Validate critical shader programs
        if (chunkShaderProgram == 0) {
            System.err.println("CRITICAL ERROR: Failed to load chunk shader program!");
            throw new RuntimeException("Chunk shader program failed to load - this will cause black screen");
        } else {
            System.out.println("DEBUG: Chunk shader program loaded successfully (ID: " + chunkShaderProgram + ")");
        }

        // Create and generate chunks around spawn location
        ChunkPosition spawnChunk = new ChunkPosition(
            (int)Math.floor(spawnLocation.x / CHUNK_SIZE),
            0,
            (int)Math.floor(spawnLocation.z / CHUNK_SIZE)
        );
        
        System.out.println("DEBUG: Player spawn location: " + spawnLocation);
        System.out.println("DEBUG: Spawn chunk: " + spawnChunk.x + ", " + spawnChunk.z);
        
        // Verify spawn position is reasonable
        if (spawnLocation.y < 0 || spawnLocation.y > CHUNK_HEIGHT) {
            System.err.println("WARNING: Spawn location has invalid Y coordinate: " + spawnLocation.y);
        }
        
        int totalChunksGenerated = 0;
        int totalBlocksGenerated = 0;
        
        // Generate chunks in a larger area around spawn
        for (int x = spawnChunk.x - 8; x <= spawnChunk.x + 8; x++) {
            for (int z = spawnChunk.z - 8; z <= spawnChunk.z + 8; z++) {
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                Chunk chunk = new Chunk(pos);
                worldGenerator.generateChunk(chunk);
                totalChunksGenerated++;
                
                // Debug terrain generation
                int chunkBlocks = 0;
                for (int cx = 0; cx < CHUNK_SIZE; cx++) {
                    for (int cy = 0; cy < CHUNK_HEIGHT; cy++) {
                        for (int cz = 0; cz < CHUNK_SIZE; cz++) {
                            if (chunk.getBlock(cx, cy, cz) != BlockType.AIR) {
                                chunkBlocks++;
                            }
                        }
                    }
                }
                
                totalBlocksGenerated += chunkBlocks;
                
                if (chunkBlocks == 0) {
                    System.out.println("WARNING: Chunk " + pos + " was generated but contains no blocks!");
                } else {
                    System.out.println("DEBUG: Chunk " + pos + " generated with " + chunkBlocks + " blocks");
                }
                
                chunks.put(pos, chunk);
            }
        }
        
        System.out.println("DEBUG: Generated " + totalChunksGenerated + " chunks with " + totalBlocksGenerated + " total blocks around spawn");
        
        // Set camera to look at spawn area for debugging
        camera.setPosition(spawnLocation.x, spawnLocation.y + 20.0f, spawnLocation.z + 20.0f);
        System.out.println("DEBUG: Camera positioned at " + camera.getPosition() + " for spawn observation");
        
        // Request initial mesh generation for all chunks
        for (Chunk chunk : chunks.values()) {
            if (chunk.getVao() == 0 && !meshingFutures.containsKey(chunk.getPosition())) {
                meshingFutures.put(chunk.getPosition(), meshGenerator.generateMesh(chunk));
            }
        }
    }
    
    public void update(float deltaTime, InputManager inputManager) {
        this.inputManager = inputManager;
        environmentManager.update(deltaTime, this, player);
        
        if (player.isControllingShip()) {
            Ship ship = player.getControlledShip();
            ship.update(deltaTime, this, environmentManager);
            // Simple 3rd person camera
            Vector3f shipPos = ship.getPosition();
            float yaw = (float)Math.toRadians(ship.getRotation().y);
            float camX = shipPos.x - (float)Math.sin(yaw) * 20;
            float camY = shipPos.y + 15;
            float camZ = shipPos.z - (float)Math.cos(yaw) * 20;
            camera.setPosition(camX, camY, camZ);
            // Make camera look at the ship
            // camera.lookAt(shipPos); // This would require a lookAt method on Camera
        } else {
            player.update(deltaTime, this, inputManager);
            // Update camera to follow player (first-person view)
            Vector3f playerPos = player.getPosition();
            
            // Validate player position before setting camera
            if (!Float.isFinite(playerPos.x) || !Float.isFinite(playerPos.y) || !Float.isFinite(playerPos.z)) {
                System.err.println("Error: Player position is invalid: " + playerPos + ". Resetting to safe location.");
                // Reset player to a safe location
                Vector3f safeSpawn = SpawnFinder.findSafeSpawnLocation(worldGenerator);
                player.setPosition(safeSpawn.x, safeSpawn.y, safeSpawn.z);
                playerPos = player.getPosition();
            }
            
            float cameraY = playerPos.y + 1.6f;
            if (Float.isFinite(cameraY)) {
                camera.setPosition(playerPos.x, cameraY, playerPos.z); // Eye level height
            } else {
                System.err.println("Error: Invalid camera Y position calculated from player: " + cameraY);
                camera.setPosition(playerPos.x, playerPos.y + 2.0f, playerPos.z);
            }
        }

        // Pass EnvironmentManager to EntityManager
        entityManager.update(deltaTime, this, environmentManager);

        handleBlockInteraction(inputManager);
        
        performRandomTicks();

        // Load chunks around the player
        ChunkPosition playerChunkPos = new ChunkPosition(
            (int)Math.floor(player.getPosition().x / CHUNK_SIZE),
            0,
            (int)Math.floor(player.getPosition().z / CHUNK_SIZE)
        );

        // Load chunks in a larger radius around player
        for (int x = playerChunkPos.x - RENDER_DISTANCE; x <= playerChunkPos.x + RENDER_DISTANCE; x++) {
            for (int z = playerChunkPos.z - RENDER_DISTANCE; z <= playerChunkPos.z + RENDER_DISTANCE; z++) {
                ChunkPosition pos = new ChunkPosition(x, 0, z);
                if (!chunks.containsKey(pos)) {
                    Chunk chunk = new Chunk(pos);
                    worldGenerator.generateChunk(chunk);
                    chunks.put(pos, chunk);
                }
            }
        }

        // Check for completed mesh futures
        meshingFutures.entrySet().removeIf(entry -> {
            if (entry.getValue().isDone()) {
                try {
                    ChunkMesh[] meshes = entry.getValue().get();
                    Chunk chunk = chunks.get(entry.getKey());
                    if (chunk != null) {
                        // Debug mesh generation
                        int opaqueVertices = meshes[0].getVertices().length;
                        int transparentVertices = meshes[1].getVertices().length;
                        
                        if (opaqueVertices == 0 && transparentVertices == 0) {
                            System.out.println("DEBUG: Chunk " + entry.getKey() + " generated empty mesh (no visible blocks)");
                            
                            // Let's check if the chunk actually has any blocks
                            int totalBlocks = 0;
                            for (int x = 0; x < CHUNK_SIZE; x++) {
                                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                                    for (int z = 0; z < CHUNK_SIZE; z++) {
                                        if (chunk.getBlock(x, y, z) != BlockType.AIR) {
                                            totalBlocks++;
                                        }
                                    }
                                }
                            }
                            System.out.println("DEBUG: Chunk " + entry.getKey() + " has " + totalBlocks + " non-air blocks");
                        } else {
                            System.out.println("DEBUG: Chunk " + entry.getKey() + " mesh generated: " + 
                                opaqueVertices/8 + " opaque vertices, " + transparentVertices/8 + " transparent vertices");
                        }
                        
                        chunk.uploadMeshToGPU(meshes[0], meshes[1]); // [0] is opaque, [1] is transparent
                        chunksChanged = true; // Mark that chunks need scene update
                    }
                } catch (Exception e) {
                    System.err.println("Error uploading mesh for chunk " + entry.getKey() + ": " + e.getMessage());
                    e.printStackTrace();
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
    
    public void render() {
        updateClearColor();
        
        // Set wetness level in the rendering pipeline
        renderingPipeline.setWetness(environmentManager.getWetness());
        
        // Update scene with current chunks only when needed
        if (chunksChanged) {
            updateSceneWithChunks();
            chunksChanged = false;
        }
        
        // Debug system status every 300 frames (5 seconds at 60 FPS)
        if (debugFrameCounter % 300 == 0) {
            printSystemStatus();
        }
        debugFrameCounter++;
        
        // Update scene
        scene.update(0.016f); // Assuming 60 FPS
        
        // Render using advanced pipeline
        float time = (float)glfwGetTime();
        float cloudCoverage = environmentManager.getCloudCoverage();
        float cloudDensity = environmentManager.getCloudDensity();
        float lightningFlash = environmentManager.getLightningFlashIntensity();
        renderingPipeline.render(camera, scene, 0.016f, time, cloudCoverage, cloudDensity, lightningFlash);
        
        // Render UI elements that don't go through deferred pipeline
        if (selectedBlock != null) {
            selectionBoxRenderer.render(camera.getViewMatrix(), camera.getProjectionMatrix(), selectedBlock.blockPos);
        }
        
        // Render particles last, so they are drawn over everything else
        environmentManager.getParticleSystem().render(camera);
    }
    
    private void updateSceneWithChunks() {
        // Clear existing chunk objects from scene
        scene.clear();
        
        int chunksWithVao = 0;
        int totalChunks = chunks.size();
        
        // Add chunks as render objects
        for (Chunk chunk : chunks.values()) {
            if (chunk.getVao() != 0) {
                ChunkRenderObject chunkObject = new ChunkRenderObject(chunk);
                scene.addObject(chunkObject);
                chunksWithVao++;
            }
        }
        
        // Debug chunk rendering status
        if (chunksWithVao == 0 && totalChunks > 0) {
            System.out.println("Warning: " + totalChunks + " chunks loaded but 0 have VAOs (meshes still generating)");
        } else if (chunksWithVao > 0) {
            System.out.println("Rendering " + chunksWithVao + "/" + totalChunks + " chunks with VAOs");
        }
        
        // Re-add default lighting
        scene.setupDefaultLighting();
    }
    
    private void updateClearColor() {
        Vector3f color = new Vector3f(0.5f, 0.8f, 1.0f); // Default sunny day color
        
        WeatherType weather = environmentManager.getCurrentWeather();
        if (weather == WeatherType.RAIN || weather == WeatherType.STORM || weather == WeatherType.FOG) {
            color.lerp(new Vector3f(0.4f, 0.4f, 0.45f), 0.7f);
        } else if (weather == WeatherType.SNOW) {
            color.lerp(new Vector3f(0.6f, 0.6f, 0.7f), 0.7f);
        } else if (weather == WeatherType.SANDSTORM) {
            color.set(0.8f, 0.7f, 0.5f); // Sandy color
        }

        // Day/Night Cycle tint
        float time = environmentManager.getWorldClock().getTimeOfDay();
        if (time > 12000 || time < 500) { // Night
             color.mul(0.4f); // Increased from 0.1f to make night more visible
        } else if (time > 10000) { // Sunset
            color.lerp(new Vector3f(1.0f, 0.5f, 0.2f), (time - 10000) / 2000f);
        } else if (time < 2000) { // Sunrise
            color.lerp(new Vector3f(1.0f, 0.5f, 0.2f), (2000 - time) / 2000f);
        }
        
        glClearColor(color.x, color.y, color.z, 1.0f);
    }
    
    public void cleanup() {
        meshGenerationPool.shutdown();
        shaderManager.cleanup();
        selectionBoxRenderer.cleanup();
        entityManager.cleanup();
        environmentManager.cleanup();
        renderingPipeline.cleanup();
        scene.cleanup();
        chunks.values().forEach(Chunk::cleanup);
    }
    
    public BlockType getBlock(int x, int y, int z) {
        if (y < 0 || y >= CHUNK_HEIGHT) {
            return BlockType.AIR;
        }

        ChunkPosition chunkPos = new ChunkPosition(
            (int) Math.floor((float) x / CHUNK_SIZE),
            0,
            (int) Math.floor((float) z / CHUNK_SIZE)
        );

        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) {
            return BlockType.STONE; // Treat unloaded chunks as solid to prevent falling out of the world
        }

        int localX = x - chunkPos.x * CHUNK_SIZE;
        int localZ = z - chunkPos.z * CHUNK_SIZE;
        
        // Handle negative local coordinates correctly.
        if (localX < 0) localX += CHUNK_SIZE;
        if (localZ < 0) localZ += CHUNK_SIZE;

        return chunk.getBlock(localX, y, localZ);
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        if (y < 0 || y >= CHUNK_HEIGHT) {
            return;
        }

        ChunkPosition chunkPos = new ChunkPosition(
            (int) Math.floor((float) x / CHUNK_SIZE),
            0,
            (int) Math.floor((float) z / CHUNK_SIZE)
        );

        Chunk chunk = chunks.get(chunkPos);
        if (chunk != null) {
            int localX = x - chunkPos.x * CHUNK_SIZE;
            int localZ = z - chunkPos.z * CHUNK_SIZE;
            
            if (localX < 0) localX += CHUNK_SIZE;
            if (localZ < 0) localZ += CHUNK_SIZE;
            
            chunk.setBlock(localX, y, localZ, type);
            // Invalidate mesh so it gets rebuilt
            if (chunk.getVao() != 0) {
                 chunk.cleanup(); // Deletes old VAO
            }
            chunksChanged = true; // Mark that chunks need scene update
        }
    }
    
    private void handleBlockInteraction(InputManager inputManager) {
        selectedBlock = Raycaster.cast(this, camera.getPosition(), camera.getFront(), 5.0f);
        
        if (selectedBlock != null && inputManager.isActionJustPressed(GameAction.PLACE_BLOCK)) {
            BlockType targetBlock = getBlock(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
            
            if (targetBlock == BlockType.HELM) {
                // Check if this helm is part of an existing ship
                Entity shipEntity = entityManager.getEntityAt(selectedBlock.blockPos);
                if (shipEntity instanceof Ship) {
                    player.setControlledShip(player.isControllingShip() ? null : (Ship)shipEntity);
                } else {
                    Ship ship = Shipyard.assembleShip(this, selectedBlock.blockPos);
                    if (ship != null) {
                        entityManager.addEntity(ship);
                    }
                }
            } else {
                ItemStack selectedItem = player.getInventory().getSelectedItem();
                if (selectedItem != null) {
                    int newX = selectedBlock.blockPos.x + selectedBlock.face.x;
                    int newY = selectedBlock.blockPos.y + selectedBlock.face.y;
                    int newZ = selectedBlock.blockPos.z + selectedBlock.face.z;
                    
                    if (!player.isCollidingWith(newX, newY, newZ)) {
                        setBlock(newX, newY, newZ, selectedItem.type);
                        player.getInventory().consumeSelectedItem();
                    }
                }
            }
        } else if (inputManager.isActionJustPressed(GameAction.BREAK_BLOCK) && selectedBlock != null) {
            BlockType brokenBlockType = getBlock(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
            if (brokenBlockType != BlockType.AIR) {
                player.getInventory().addItem(new ItemStack(brokenBlockType, 1));
                setBlock(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z, BlockType.AIR);
            }
        }

        // Block placing and interaction
        if (inputManager.isActionPressed(GameAction.PLACE_BLOCK)) {
            if (selectedBlock != null && selectedBlock.face != null) {
                ItemStack heldItemStack = player.getInventory().getSelectedItem();
                if (heldItemStack == null) return;
                
                BlockType heldItem = heldItemStack.type;
                BlockType targetBlock = getBlock(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
                Vector3i targetPos = selectedBlock.blockPos;
                Vector3i placePos = new Vector3i(targetPos).add(selectedBlock.face);
                
                // --- Interaction Logic ---
                
                // 1. Hoe Interaction: Dirt/Grass -> Farmland
                // Using WOOD as a placeholder for a Hoe item for now
                if (heldItem == BlockType.WOOD && (targetBlock == BlockType.DIRT || targetBlock == BlockType.GRASS)) {
                    setBlock(targetPos.x, targetPos.y, targetPos.z, BlockType.FARMLAND);
                    return; // Interaction handled, don't place a block
                }

                // 2. Seed Interaction: Farmland -> Wheat
                // Using LEAVES as a placeholder for Wheat Seeds for now
                if (heldItem == BlockType.LEAVES && targetBlock == BlockType.FARMLAND) {
                    if (getBlock(placePos.x, placePos.y, placePos.z) == BlockType.AIR) {
                        setBlock(placePos.x, placePos.y, placePos.z, BlockType.WHEAT_0);
                        player.getInventory().removeItem(heldItem, 1);
                        return;
                    }
                }
                
                // --- Default Block Placing ---
                if (getBlock(placePos.x, placePos.y, placePos.z) == BlockType.AIR) {
                    setBlock(placePos.x, placePos.y, placePos.z, heldItem);
                    player.getInventory().removeItem(heldItem, 1);
                }
            }
        }
    }

    private void performRandomTicks() {
        int ticksPerFrame = 3; // Number of random ticks to perform each frame per chunk
        
        for (Chunk chunk : chunks.values()) {
            if (chunk == null) continue;
            
            for (int i = 0; i < ticksPerFrame; i++) {
                int x = random.nextInt(CHUNK_SIZE);
                int y = random.nextInt(CHUNK_HEIGHT);
                int z = random.nextInt(CHUNK_SIZE);
                
                Vector3i localPos = new Vector3i(x, y, z);
                BlockType block = chunk.getBlock(localPos.x, localPos.y, localPos.z);
                
                Vector3i globalPos = new Vector3i(chunk.getPosition().x * CHUNK_SIZE + x, y, chunk.getPosition().z * CHUNK_SIZE + z);

                // --- WHEAT GROWTH ---
                if (block.getId() >= BlockType.WHEAT_0.getId() && block.getId() < BlockType.WHEAT_7.getId()) {
                    if (random.nextFloat() < environmentManager.getCropGrowthChance()) {
                        BlockType nextStage = BlockType.fromId(block.getId() + 1);
                        setBlock(globalPos.x, globalPos.y, globalPos.z, nextStage);
                    }
                }
                
                // --- FIRE SPREAD/BURNOUT ---
                if (block == BlockType.FIRE) {
                    // Burnout chance
                    if (random.nextFloat() < 0.2f) { // 20% chance to burn out on a tick
                        setBlock(globalPos.x, globalPos.y, globalPos.z, BlockType.AIR);
                        continue; // Stop processing this fire block
                    }

                    // Spread chance
                    if (random.nextFloat() < 0.5f) { // 50% chance to attempt to spread
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
        }
    }

    private boolean isFlammable(BlockType blockType) {
        return blockType == BlockType.WOOD || blockType == BlockType.LEAVES;
    }

    public Camera getCamera() { return camera; }
    public Player getPlayer() {
        return player;
    }
    
    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }

    public Biome getBiomeAt(int x, int z) {
        ChunkPosition chunkPos = new ChunkPosition(x / CHUNK_SIZE, 0, z / CHUNK_SIZE);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) {
            // If chunk isn't loaded, get biome directly from generator as a fallback
            return worldGenerator.getBiome(x, z);
        }
        
        int localX = x % CHUNK_SIZE;
        if (localX < 0) localX += CHUNK_SIZE;
        int localZ = z % CHUNK_SIZE;
        if (localZ < 0) localZ += CHUNK_SIZE;
        
        return chunk.getBiome(localX, localZ);
    }
    
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * Print comprehensive system status for debugging black screen issues
     */
    private void printSystemStatus() {
        System.out.println("=== VOXEL ENGINE DEBUG STATUS ===");
        
        // Camera status
        Vector3f camPos = camera.getPosition();
        System.out.println("Camera Position: " + camPos);
        System.out.println("Camera Yaw: " + camera.getYaw() + ", Pitch: " + camera.getPitch());
        System.out.println("Camera FOV: " + camera.getFov() + ", Aspect: " + camera.getAspectRatio());
        
        // Player status
        Vector3f playerPos = player.getPosition();
        System.out.println("Player Position: " + playerPos);
        
        // Chunk status
        System.out.println("Total Chunks Loaded: " + chunks.size());
        int chunksWithVAO = 0;
        int totalBlocksInChunks = 0;
        
        for (Chunk chunk : chunks.values()) {
            if (chunk.getVao() != 0) {
                chunksWithVAO++;
            }
            
            // Count blocks in a sample of chunks
            if (chunksWithVAO < 5) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int y = 0; y < CHUNK_HEIGHT; y++) {
                        for (int z = 0; z < CHUNK_SIZE; z++) {
                            if (chunk.getBlock(x, y, z) != BlockType.AIR) {
                                totalBlocksInChunks++;
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("Chunks with VAO (renderable): " + chunksWithVAO + "/" + chunks.size());
        System.out.println("Pending mesh futures: " + meshingFutures.size());
        
        // Scene status
        System.out.println("Scene total objects: " + scene.getObjects().size());
        System.out.println("Scene opaque objects: " + scene.getOpaqueObjects().size());
        System.out.println("Scene transparent objects: " + scene.getTransparentObjects().size());
        System.out.println("Scene lights: " + scene.getLights().size());
        
        // Shader status
        System.out.println("Chunk shader program ID: " + chunkShaderProgram);
        
        System.out.println("================================");
    }
}