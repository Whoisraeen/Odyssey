package com.odyssey.core;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.GameEngineService;
import com.odyssey.input.InputManager;
import com.odyssey.player.Player;
import com.odyssey.rendering.Camera;
import com.odyssey.crafting.CraftingSystem;
import com.odyssey.world.BlockType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Core Voxel Engine - Refactored to use dependency injection with Spring
 * Delegates core functionality to GameEngineService for better testability
 */
@Component
@SuppressWarnings("unused")
public class VoxelEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(VoxelEngine.class);
    
    @Autowired
    private GameConfiguration config;
    
    @Autowired
    private GameEngineService gameEngineService;
    
    private InputManager inputManager;
    private final Random random = new Random();
    private int debugFrameCounter = 0;

    public void initialize(int width, int height, com.odyssey.audio.SoundManager soundManager) {
        logger.info("Initializing VoxelEngine with dependency injection");
        
        // Set VoxelEngine reference in GameEngineService before initialization
        if (gameEngineService instanceof com.odyssey.services.impl.GameEngineServiceImpl gameEngineServiceImpl) {
            gameEngineServiceImpl.setVoxelEngine(this);
        }
        
        gameEngineService.initialize(width, height, soundManager);
        logger.info("VoxelEngine initialization completed");
    }
    
    // Backward compatibility method
    public void initialize(int width, int height) {
        initialize(width, height, null);
    }
    
    public void update(float deltaTime, InputManager inputManager) {
        this.inputManager = inputManager;
        gameEngineService.update(deltaTime, inputManager);
        
        // Debug system status periodically
        if (debugFrameCounter % 300 == 0) {
            gameEngineService.printSystemStatus();
        }
        debugFrameCounter++;
    }
    
    public void render() {
        gameEngineService.render();
    }
    
    public void cleanup() {
        gameEngineService.cleanup();
    }
    
    public BlockType getBlock(int x, int y, int z) {
        return gameEngineService.getBlock(x, y, z);
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        gameEngineService.setBlock(x, y, z, type);
    }

    public void interactWithBlock(int x, int y, int z, boolean isBreaking) {
        gameEngineService.interactWithBlock(x, y, z, isBreaking);
    }
    
    public Camera getCamera() {
        return gameEngineService.getCamera();
    }
    
    public Player getPlayer() {
        return gameEngineService.getPlayer();
    }
    
    public InputManager getInputManager() {
        return inputManager;
    }
    
    public CraftingSystem getCraftingSystem() {
        return gameEngineService.getCraftingSystem();
    }
    
    public com.odyssey.world.WorldGenerator getWorldGenerator() {
        return gameEngineService.getWorldGenerator();
    }
    
    public com.odyssey.world.World getWorld() {
        return gameEngineService.getWorld();
    }
    
    public com.odyssey.world.biome.Biome getBiomeAt(int x, int z) {
        return gameEngineService.getBiomeAt(x, z);
    }
}