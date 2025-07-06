package com.odyssey.services.impl;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.*;
import com.odyssey.input.InputManager;
import com.odyssey.player.Player;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.scene.Scene;
import com.odyssey.crafting.CraftingSystem;
import com.odyssey.audio.GameSoundManager;
import com.odyssey.physics.Raycaster;
import com.odyssey.input.GameAction;
import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;
import com.odyssey.world.BlockType;
import com.odyssey.world.SpawnFinder;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameEngineServiceImpl implements GameEngineService {
    
    private static final Logger logger = LoggerFactory.getLogger(GameEngineServiceImpl.class);
    
    @Autowired
    private GameConfiguration config;
    
    @Autowired
    private WorldService worldService;
    
    @Autowired
    private com.odyssey.world.World world;
    
    @Autowired
    private RenderingService renderingService;
    
    @Autowired
    private EntityService entityService;
    
    @Autowired
    private EnvironmentService environmentService;
    
    private com.odyssey.core.VoxelEngine voxelEngine;
    private Camera camera;
    private Player player;
    private Scene scene;
    private CraftingSystem craftingSystem;
    private GameSoundManager gameSoundManager;
    private InputManager inputManager;
    private Raycaster.RaycastResult selectedBlock;
    private int debugFrameCounter = 0;
    
    @Override
    public void initialize(int width, int height) {
        initialize(width, height, null);
    }
    
    @Override
    public void initialize(int width, int height, com.odyssey.audio.SoundManager soundManager) {
        logger.info("Initializing game engine with resolution {}x{}", width, height);
        
        // Initialize core systems
        this.camera = new Camera();
        this.scene = new Scene();
        this.craftingSystem = new CraftingSystem();
        
        // Initialize environment service with sound manager first
        if (soundManager != null && environmentService instanceof EnvironmentServiceImpl envServiceImpl) {
            envServiceImpl.initialize(soundManager);
        }
        
        // Initialize services
        renderingService.initialize(width, height);
        
        // Find spawn location and create player
        Vector3f spawnLocation = worldService.findSafeSpawnLocation();
        validateSpawnLocation(spawnLocation);
        
        this.player = new Player(spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
        // Position camera
        setupCamera(spawnLocation);
        
        // Setup scene
        scene.setupDefaultLighting();
        
        // Load initial chunks around spawn
        worldService.loadChunksAround(spawnLocation, config.world().renderDistance());
        
        // Initialize cross-service dependencies
        initializeCrossServiceDependencies();
        
        logger.info("Game engine initialized successfully at spawn location: {}", spawnLocation);
    }
    
    private void validateSpawnLocation(Vector3f spawnLocation) {
        if (!Float.isFinite(spawnLocation.x) || !Float.isFinite(spawnLocation.y) || !Float.isFinite(spawnLocation.z)) {
            logger.error("Invalid spawn location: {}. Using safe fallback.", spawnLocation);
            spawnLocation.set(0, 72, 0);
        }
    }
    
    private void setupCamera(Vector3f spawnLocation) {
        float cameraY = spawnLocation.y + 1.7f;
        if (Float.isFinite(cameraY)) {
            camera.setPosition(spawnLocation.x, cameraY, spawnLocation.z);
        } else {
            logger.error("Invalid camera Y position: {}. Using fallback.", cameraY);
            camera.setPosition(spawnLocation.x, spawnLocation.y + 2.0f, spawnLocation.z);
        }
        camera.setPitch(-20.0f);
        camera.setYaw(45.0f);
    }
    
    private void initializeCrossServiceDependencies() {
        // Set up cross-service dependencies that couldn't be autowired
        if (worldService instanceof WorldServiceImpl worldServiceImpl) {
            worldServiceImpl.setParticleSystem(environmentService.getParticleSystem());
        }
        
        if (environmentService instanceof EnvironmentServiceImpl envServiceImpl) {
            envServiceImpl.setVoxelEngine(voxelEngine);
            envServiceImpl.setParticleSystem(environmentService.getParticleSystem());
        }
        
        if (entityService instanceof com.odyssey.services.impl.EntityServiceImpl entityServiceImpl) {
            entityServiceImpl.setVoxelEngine(voxelEngine);
        }
    }
    
    public void setVoxelEngine(com.odyssey.core.VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
    }
    
    @Override
    public void update(float deltaTime, InputManager inputManager) {
        this.inputManager = inputManager;
        
        // Update environment first
        environmentService.update(deltaTime, player);
        
        // Update player and camera
        updatePlayerAndCamera(deltaTime);
        
        // Update entities
        entityService.update(deltaTime);
        entityService.updateMobSpawning(deltaTime);
        
        // Handle player interactions
        handleBlockInteraction(inputManager);
        
        // Update world systems
        worldService.update(deltaTime);
        worldService.performRandomTicks();
        
        // Load chunks around player
        loadChunksAroundPlayer();
        
        logger.debug("Game engine updated - Delta: {}ms", deltaTime * 1000);
    }
    
    private void updatePlayerAndCamera(float deltaTime) {
        if (player.isControllingShip()) {
            // Ship control mode - update ship and camera
            // Ship update logic would go here
        } else {
            // Normal player mode
            player.update(deltaTime, voxelEngine, inputManager, gameSoundManager);
            
            Vector3f playerPos = player.getPosition();
            validatePlayerPosition(playerPos);
            
            float cameraY = playerPos.y + 1.6f;
            if (Float.isFinite(cameraY)) {
                camera.setPosition(playerPos.x, cameraY, playerPos.z);
            } else {
                logger.error("Invalid camera Y position from player: {}", cameraY);
                camera.setPosition(playerPos.x, playerPos.y + 2.0f, playerPos.z);
            }
        }
    }
    
    private void validatePlayerPosition(Vector3f playerPos) {
        if (!Float.isFinite(playerPos.x) || !Float.isFinite(playerPos.y) || !Float.isFinite(playerPos.z)) {
            logger.error("Player position is invalid: {}. Resetting to safe location.", playerPos);
            Vector3f safeSpawn = worldService.findSafeSpawnLocation();
            player.setPosition(safeSpawn.x, safeSpawn.y, safeSpawn.z);
        }
    }
    
    private void loadChunksAroundPlayer() {
        Vector3f playerPos = player.getPosition();
        worldService.loadChunksAround(playerPos, config.world().renderDistance());
    }
    
    @Override
    public void render() {
        // Debug system status periodically
        if (debugFrameCounter % 300 == 0) {
            printSystemStatus();
        }
        debugFrameCounter++;
        
        // Render main scene
        renderingService.render(camera, scene, 0.016f);
        
        // Render UI elements
        if (selectedBlock != null) {
            renderingService.renderSelectionBox(camera, selectedBlock.blockPos);
        }
        
        // Update environment particles (rendering handled by rendering service)
        if (environmentService.getParticleSystem() != null) {
            environmentService.getParticleSystem().update(0.016f);
        }
    }
    
    @Override
    public void handleBlockInteraction(InputManager inputManager) {
        selectedBlock = Raycaster.cast(this, camera.getPosition(), camera.getFront(), 5.0f);
        
        if (selectedBlock != null && inputManager.isActionJustPressed(GameAction.PLACE_BLOCK)) {
            handleBlockPlacement();
        } else if (inputManager.isActionJustPressed(GameAction.BREAK_BLOCK) && selectedBlock != null) {
            handleBlockBreaking();
        }
    }
    
    private void handleBlockPlacement() {
        BlockType targetBlock = worldService.getBlock(
            selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
        
        if (targetBlock == BlockType.HELM) {
            entityService.handleShipControl(player, selectedBlock.blockPos);
        } else {
            ItemStack selectedItem = player.getInventory().getSelectedItem();
            if (selectedItem != null) {
                int newX = selectedBlock.blockPos.x + selectedBlock.face.x;
                int newY = selectedBlock.blockPos.y + selectedBlock.face.y;
                int newZ = selectedBlock.blockPos.z + selectedBlock.face.z;
                
                if (!player.isCollidingWith(newX, newY, newZ)) {
                    BlockType blockType = convertItemTypeToBlockType(selectedItem.getType());
                    worldService.setBlock(newX, newY, newZ, blockType);
                    player.getInventory().consumeSelectedItem();
                    logger.debug("Placed block {} at ({}, {}, {})", blockType, newX, newY, newZ);
                }
            }
        }
    }
    
    private void handleBlockBreaking() {
        BlockType brokenBlockType = worldService.getBlock(
            selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
        
        if (brokenBlockType != BlockType.AIR) {
            // Create particles and play sound
            if (environmentService.getParticleSystem() != null) {
                environmentService.getParticleSystem().createBlockBreakParticles(
                    selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z, brokenBlockType);
            }
            
            if (gameSoundManager != null) {
                gameSoundManager.playBlockBreakSound(brokenBlockType, 
                    new Vector3f(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z));
            }
            
            ItemType itemType = convertBlockToItemType(brokenBlockType);
            player.getInventory().addItem(new ItemStack(itemType, 1));
            worldService.setBlock(selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z, BlockType.AIR);
            
            logger.debug("Broke block {} at ({}, {}, {})", brokenBlockType, 
                        selectedBlock.blockPos.x, selectedBlock.blockPos.y, selectedBlock.blockPos.z);
        }
    }
    
    @Override
    public Camera getCamera() {
        return camera;
    }
    
    @Override
    public Player getPlayer() {
        return player;
    }
    
    @Override
    public CraftingSystem getCraftingSystem() {
        return craftingSystem;
    }
    
    @Override
    public com.odyssey.world.WorldGenerator getWorldGenerator() {
        return worldService.getWorldGenerator();
    }
    
    @Override
    public com.odyssey.world.World getWorld() {
        return world;
    }
    
    @Override
    public com.odyssey.world.biome.Biome getBiomeAt(int x, int z) {
        return worldService.getBiomeAt(x, z);
    }
    
    @Override
    public InputManager getInputManager() {
        return inputManager;
    }
    
    @Override
    public BlockType getBlock(int x, int y, int z) {
        return worldService.getBlock(x, y, z);
    }
    
    @Override
    public void setBlock(int x, int y, int z, BlockType type) {
        worldService.setBlock(x, y, z, type);
    }
    
    @Override
    public void interactWithBlock(int x, int y, int z, boolean isBreaking) {
        // Create a temporary selected block for interaction
        selectedBlock = new Raycaster.RaycastResult(
            new Vector3i(x, y, z),
            new Vector3i(0, 1, 0) // Default face (up)
        );
        
        if (isBreaking) {
            handleBlockBreaking();
        } else {
            handleBlockPlacement();
        }
    }
    
    @Override
    public void printSystemStatus() {
        logger.info("=== GAME ENGINE DEBUG STATUS ===");
        
        // Camera status
        Vector3f camPos = camera.getPosition();
        logger.info("Camera Position: {}", camPos);
        logger.info("Camera Yaw: {}, Pitch: {}", camera.getYaw(), camera.getPitch());
        
        // Player status
        Vector3f playerPos = player.getPosition();
        logger.info("Player Position: {}", playerPos);
        
        // Scene status
        logger.info("Scene total objects: {}", scene.getObjects().size());
        logger.info("Scene opaque objects: {}", scene.getOpaqueObjects().size());
        logger.info("Scene transparent objects: {}", scene.getTransparentObjects().size());
        logger.info("Scene lights: {}", scene.getLights().size());
        
        // Environment status
        logger.info("Current weather: {}", environmentService.getCurrentWeather());
        logger.info("Time of day: {}", environmentService.getTimeOfDay());
        
        logger.info("================================");
    }
    
    private ItemType convertBlockToItemType(BlockType blockType) {
        // Simple conversion - in a full implementation this would be more comprehensive
        switch (blockType) {
            case STONE: return ItemType.STONE;
            case DIRT: return ItemType.DIRT;
            case GRASS: return ItemType.DIRT; // Grass drops dirt when broken
            case COBBLESTONE: return ItemType.COBBLESTONE;
            case WOOD: return ItemType.WOOD;
            case LEAVES: return ItemType.LEAVES;
            case SAND: return ItemType.SAND;
            case WATER: return ItemType.WATER_BUCKET;
            case GLASS: return ItemType.GLASS;
            case COAL_ORE: return ItemType.COAL_ORE;
            case IRON_ORE: return ItemType.IRON_ORE;
            case GOLD_ORE: return ItemType.GOLD_ORE;
            case DIAMOND_ORE: return ItemType.DIAMOND_ORE;
            default: return ItemType.STONE; // Fallback
        }
    }

    private BlockType convertItemTypeToBlockType(ItemType itemType) {
        // Simple conversion - in a full implementation this would be more comprehensive
        switch (itemType) {
            case STONE: return BlockType.STONE;
            case DIRT: return BlockType.DIRT;
            case COBBLESTONE: return BlockType.COBBLESTONE;
            case WOOD: return BlockType.WOOD;
            case LEAVES: return BlockType.LEAVES;
            case SAND: return BlockType.SAND;
            case GLASS: return BlockType.GLASS;
            case COAL_ORE: return BlockType.COAL_ORE;
            case IRON_ORE: return BlockType.IRON_ORE;
            case GOLD_ORE: return BlockType.GOLD_ORE;
            case DIAMOND_ORE: return BlockType.DIAMOND_ORE;
            default: return BlockType.STONE; // Fallback
        }
    }

    @Override
    public void cleanup() {
        logger.info("Cleaning up game engine...");
        
        worldService.cleanup();
        renderingService.cleanup();
        entityService.cleanup();
        environmentService.cleanup();
        
        if (scene != null) {
            scene.cleanup();
        }
        
        logger.info("Game engine cleanup completed");
    }
}