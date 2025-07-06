package com.odyssey.core;

/**
 * Central configuration constants for the Odyssey game engine.
 * This class contains all game-wide constants to improve maintainability.
 */
public final class GameConstants {
    
    // Rendering constants
    public static final int RENDER_DISTANCE = 8;
    public static final int CHUNK_SIZE = 16;
    public static final int MAX_HEIGHT = 256;
    public static final int WORLD_HEIGHT = 16; // Number of chunks vertically
    
    // Performance constants
    public static final int MAX_CHUNKS_PER_FRAME = 4;
    public static final int VERTEX_BUFFER_SIZE = 1024 * 1024; // 1MB
    
    // Audio constants
    public static final float DEFAULT_MASTER_VOLUME = 1.0f;
    public static final float DEFAULT_SFX_VOLUME = 0.8f;
    public static final float DEFAULT_AMBIENT_VOLUME = 0.6f;
    public static final float MAX_AUDIO_DISTANCE = 100.0f;
    
    // Player constants
    public static final float PLAYER_SPEED = 5.0f;
    public static final float PLAYER_JUMP_FORCE = 8.0f;
    public static final float PLAYER_MAX_HEALTH = 100.0f;
    
    // World generation constants
    public static final int WORLD_SEED_DEFAULT = 12345;
    public static final float TERRAIN_SCALE = 0.01f;
    public static final int SEA_LEVEL = 64;
    
    // UI constants
    public static final float UI_SCALE = 1.0f;
    public static final int BUTTON_HEIGHT = 40;
    public static final int BUTTON_WIDTH = 200;
    
    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("GameConstants should not be instantiated");
    }
}