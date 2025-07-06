package com.odyssey.audio;

import com.odyssey.world.BlockType;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages game-specific sound effects and audio
 */
public class GameSoundManager {
    private final SoundManager soundManager;
    private final Queue<SoundEvent> soundQueue;
    private final Random random;
    
    // Sound categories
    private static final Map<BlockType, String> BLOCK_BREAK_SOUNDS = new HashMap<>();
    private static final Map<BlockType, String> BLOCK_PLACE_SOUNDS = new HashMap<>();
    
    static {
        // Block break sounds
        BLOCK_BREAK_SOUNDS.put(BlockType.STONE, "block.stone.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.COBBLESTONE, "block.stone.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.DIRT, "block.dirt.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.GRASS, "block.grass.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.WOOD, "block.wood.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.LEAVES, "block.leaves.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.SAND, "block.sand.break");
        BLOCK_BREAK_SOUNDS.put(BlockType.WATER, "block.water.break");
        
        // Block place sounds
        BLOCK_PLACE_SOUNDS.put(BlockType.STONE, "block.stone.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.COBBLESTONE, "block.stone.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.DIRT, "block.dirt.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.GRASS, "block.grass.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.WOOD, "block.wood.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.LEAVES, "block.leaves.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.SAND, "block.sand.place");
        BLOCK_PLACE_SOUNDS.put(BlockType.WATER, "block.water.place");
    }
    
    public GameSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
        this.soundQueue = new ConcurrentLinkedQueue<>();
        this.random = new Random();
        
        // Load game sounds (placeholder - in a real implementation, these would be actual audio files)
        loadGameSounds();
    }
    
    /**
     * Load all game-specific sounds
     */
    private void loadGameSounds() {
        // In a real implementation, this would load actual audio files
        // For now, we'll just register the sound names
        
        // Block sounds
        registerSound("block.stone.break");
        registerSound("block.stone.place");
        registerSound("block.dirt.break");
        registerSound("block.dirt.place");
        registerSound("block.grass.break");
        registerSound("block.grass.place");
        registerSound("block.wood.break");
        registerSound("block.wood.place");
        registerSound("block.leaves.break");
        registerSound("block.leaves.place");
        registerSound("block.sand.break");
        registerSound("block.sand.place");
        registerSound("block.water.break");
        registerSound("block.water.place");
        
        // Environment sounds
        registerSound("ambient.fire.crackle");
        registerSound("ambient.water.flow");
        registerSound("ambient.wind");
        registerSound("ambient.rain");
        registerSound("ambient.thunder");
        
        // Player sounds
        registerSound("player.footstep.grass");
        registerSound("player.footstep.stone");
        registerSound("player.footstep.sand");
        registerSound("player.footstep.wood");
        registerSound("player.jump");
        registerSound("player.land");
        
        // UI sounds
        registerSound("ui.click");
        registerSound("ui.hover");
        registerSound("ui.open");
        registerSound("ui.close");
    }
    
    /**
     * Register a sound with the sound manager (placeholder)
     */
    private void registerSound(String soundName) {
        // In a real implementation, this would load the actual audio file
        // For now, we'll just log that the sound is registered
        System.out.println("Registered sound: " + soundName);
    }
    
    /**
     * Update the sound system
     */
    public void update(float deltaTime) {
        // Process queued sound events
        while (!soundQueue.isEmpty()) {
            SoundEvent event = soundQueue.poll();
            if (event != null) {
                playSound(event);
            }
        }
    }
    
    /**
     * Play a block break sound
     */
    public void playBlockBreakSound(BlockType blockType, Vector3f position) {
        String soundName = BLOCK_BREAK_SOUNDS.get(blockType);
        if (soundName != null) {
            queueSound(soundName, position, 1.0f, getPitchVariation());
        }
    }
    
    /**
     * Play a block place sound
     */
    public void playBlockPlaceSound(BlockType blockType, Vector3f position) {
        String soundName = BLOCK_PLACE_SOUNDS.get(blockType);
        if (soundName != null) {
            queueSound(soundName, position, 0.8f, getPitchVariation());
        }
    }
    
    /**
     * Play footstep sound based on block type
     */
    public void playFootstepSound(BlockType blockType, Vector3f position) {
        String soundName = getFootstepSound(blockType);
        if (soundName != null) {
            queueSound(soundName, position, 0.3f, getPitchVariation());
        }
    }
    
    /**
     * Play ambient fire sound
     */
    public void playFireSound(Vector3f position) {
        if (random.nextFloat() < 0.1f) { // 10% chance per call
            queueSound("ambient.fire.crackle", position, 0.5f, getPitchVariation());
        }
    }
    
    /**
     * Play water flow sound
     */
    public void playWaterSound(Vector3f position) {
        if (random.nextFloat() < 0.05f) { // 5% chance per call
            queueSound("ambient.water.flow", position, 0.4f, getPitchVariation());
        }
    }
    
    /**
     * Play weather sounds
     */
    public void playWeatherSound(String weatherType, Vector3f position) {
        switch (weatherType.toLowerCase()) {
            case "rain":
                queueSound("ambient.rain", position, 0.6f, 1.0f);
                break;
            case "thunder":
                queueSound("ambient.thunder", position, 1.0f, getPitchVariation());
                break;
            case "wind":
                queueSound("ambient.wind", position, 0.3f, getPitchVariation());
                break;
        }
    }
    
    /**
     * Play UI sound
     */
    public void playUISound(String soundType) {
        String soundName = "ui." + soundType.toLowerCase();
        queueSound(soundName, null, 0.7f, 1.0f);
    }
    
    /**
     * Queue a sound to be played
     */
    private void queueSound(String soundName, Vector3f position, float volume, float pitch) {
        soundQueue.offer(new SoundEvent(soundName, position, volume, pitch));
    }
    
    /**
     * Actually play the sound
     */
    private void playSound(SoundEvent event) {
        try {
            if (event.position != null) {
                // 3D positioned sound
                soundManager.playSound3D(event.soundName, event.position, event.volume, event.pitch);
            } else {
                // 2D sound (UI, etc.)
                soundManager.playSound2D(event.soundName, event.volume, event.pitch);
            }
        } catch (Exception e) {
            // Sound not found or other error - fail silently in production
            System.err.println("Failed to play sound: " + event.soundName + " - " + e.getMessage());
        }
    }
    
    /**
     * Get footstep sound for block type
     */
    private String getFootstepSound(BlockType blockType) {
        switch (blockType) {
            case GRASS:
            case LEAVES:
                return "player.footstep.grass";
            case STONE:
            case COBBLESTONE:
                return "player.footstep.stone";
            case SAND:
                return "player.footstep.sand";
            case WOOD:
                return "player.footstep.wood";
            default:
                return "player.footstep.stone"; // Default footstep
        }
    }
    
    /**
     * Get random pitch variation for more natural sounds
     */
    private float getPitchVariation() {
        return 0.9f + random.nextFloat() * 0.2f; // 0.9 to 1.1
    }
    
    /**
     * Set master volume
     */
    public void setMasterVolume(float volume) {
        soundManager.setMasterVolume(volume);
    }
    
    /**
     * Set sound effects volume
     */
    public void setSFXVolume(float volume) {
        soundManager.setSFXVolume(volume);
    }
    
    /**
     * Set ambient volume
     */
    public void setAmbientVolume(float volume) {
        soundManager.setAmbientVolume(volume);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        soundQueue.clear();
        // soundManager cleanup is handled by the main SoundManager
    }
    
    /**
     * Represents a queued sound event
     */
    private static class SoundEvent {
        final String soundName;
        final Vector3f position;
        final float volume;
        final float pitch;
        
        SoundEvent(String soundName, Vector3f position, float volume, float pitch) {
            this.soundName = soundName;
            this.position = position != null ? new Vector3f(position) : null;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}