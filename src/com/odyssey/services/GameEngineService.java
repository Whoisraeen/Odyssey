package com.odyssey.services;

import com.odyssey.input.InputManager;
import com.odyssey.player.Player;
import com.odyssey.rendering.Camera;
import com.odyssey.crafting.CraftingSystem;
import com.odyssey.world.BlockType;

/**
 * Main game engine service interface
 */
public interface GameEngineService {
    
    /**
     * Initialize the game engine
     */
    void initialize(int width, int height);
    
    /**
     * Initialize the game engine with sound manager
     */
    void initialize(int width, int height, com.odyssey.audio.SoundManager soundManager);
    
    /**
     * Update game systems
     */
    void update(float deltaTime, InputManager inputManager);
    
    /**
     * Render the current frame
     */
    void render();
    
    /**
     * Handle block interactions
     */
    void handleBlockInteraction(InputManager inputManager);
    
    /**
     * Get the main camera
     */
    Camera getCamera();
    
    /**
     * Get the player
     */
    Player getPlayer();
    
    /**
     * Get the crafting system
     */
    CraftingSystem getCraftingSystem();
    
    /**
     * Get the world generator
     */
    com.odyssey.world.WorldGenerator getWorldGenerator();
    
    /**
     * Get the world
     */
    com.odyssey.world.World getWorld();
    
    /**
     * Get biome at world coordinates
     */
    com.odyssey.world.biome.Biome getBiomeAt(int x, int z);
    
    /**
     * Get input manager
     */
    InputManager getInputManager();
    
    /**
     * Get block at world coordinates
     */
    BlockType getBlock(int x, int y, int z);
    
    /**
     * Set block at world coordinates
     */
    void setBlock(int x, int y, int z, BlockType type);
    
    /**
     * Interact with block at coordinates
     */
    void interactWithBlock(int x, int y, int z, boolean isBreaking);
    
    /**
     * Print system status for debugging
     */
    void printSystemStatus();
    
    /**
     * Cleanup all resources
     */
    void cleanup();
}