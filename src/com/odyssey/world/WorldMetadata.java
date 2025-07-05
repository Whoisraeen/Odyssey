package com.odyssey.world;

import org.joml.Vector3f;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Handles saving and loading world metadata including seed, player position, and time.
 */
public class WorldMetadata {
    
    private static final String WORLDS_DIR = "worlds";
    private static final String METADATA_FILE = "world.properties";
    
    private String worldName;
    private long seed;
    private Vector3f playerPosition;
    private Vector3f playerRotation;
    private float worldTime;
    private long creationTime;
    private long lastPlayedTime;
    
    public WorldMetadata(String worldName, long seed) {
        this.worldName = worldName;
        this.seed = seed;
        this.playerPosition = new Vector3f(0, 80, 0);
        this.playerRotation = new Vector3f(0, 0, 0);
        this.worldTime = 0.0f;
        this.creationTime = System.currentTimeMillis();
        this.lastPlayedTime = System.currentTimeMillis();
    }
    
    public WorldMetadata(String worldName) {
        this.worldName = worldName;
    }
    
    /**
     * Save world metadata to file
     */
    public void save() throws IOException {
        Path worldDir = getWorldDirectory();
        Files.createDirectories(worldDir);
        
        Properties props = new Properties();
        props.setProperty("worldName", worldName);
        props.setProperty("seed", String.valueOf(seed));
        props.setProperty("playerX", String.valueOf(playerPosition.x));
        props.setProperty("playerY", String.valueOf(playerPosition.y));
        props.setProperty("playerZ", String.valueOf(playerPosition.z));
        props.setProperty("rotationX", String.valueOf(playerRotation.x));
        props.setProperty("rotationY", String.valueOf(playerRotation.y));
        props.setProperty("rotationZ", String.valueOf(playerRotation.z));
        props.setProperty("worldTime", String.valueOf(worldTime));
        props.setProperty("creationTime", String.valueOf(creationTime));
        props.setProperty("lastPlayedTime", String.valueOf(System.currentTimeMillis()));
        
        Path metadataFile = worldDir.resolve(METADATA_FILE);
        try (FileOutputStream fos = new FileOutputStream(metadataFile.toFile())) {
            props.store(fos, "Odyssey World Metadata");
        }
    }
    
    /**
     * Load world metadata from file
     */
    public boolean load() {
        Path metadataFile = getWorldDirectory().resolve(METADATA_FILE);
        
        if (!Files.exists(metadataFile)) {
            return false;
        }
        
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(metadataFile.toFile())) {
            props.load(fis);
            
            this.seed = Long.parseLong(props.getProperty("seed", "0"));
            
            float x = Float.parseFloat(props.getProperty("playerX", "0"));
            float y = Float.parseFloat(props.getProperty("playerY", "80"));
            float z = Float.parseFloat(props.getProperty("playerZ", "0"));
            this.playerPosition = new Vector3f(x, y, z);
            
            float rx = Float.parseFloat(props.getProperty("rotationX", "0"));
            float ry = Float.parseFloat(props.getProperty("rotationY", "0"));
            float rz = Float.parseFloat(props.getProperty("rotationZ", "0"));
            this.playerRotation = new Vector3f(rx, ry, rz);
            
            this.worldTime = Float.parseFloat(props.getProperty("worldTime", "0"));
            this.creationTime = Long.parseLong(props.getProperty("creationTime", String.valueOf(System.currentTimeMillis())));
            this.lastPlayedTime = Long.parseLong(props.getProperty("lastPlayedTime", String.valueOf(System.currentTimeMillis())));
            
            return true;
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to load world metadata: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a world exists
     */
    public static boolean worldExists(String worldName) {
        Path worldDir = Paths.get(WORLDS_DIR, worldName);
        Path metadataFile = worldDir.resolve(METADATA_FILE);
        return Files.exists(metadataFile);
    }
    
    /**
     * Get list of available worlds
     */
    public static String[] getAvailableWorlds() {
        Path worldsDir = Paths.get(WORLDS_DIR);
        
        if (!Files.exists(worldsDir)) {
            return new String[0];
        }
        
        try {
            return Files.list(worldsDir)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> worldExists(name))
                    .toArray(String[]::new);
        } catch (IOException e) {
            System.err.println("Failed to list worlds: " + e.getMessage());
            return new String[0];
        }
    }
    
    /**
     * Delete a world
     */
    public static boolean deleteWorld(String worldName) {
        Path worldDir = Paths.get(WORLDS_DIR, worldName);
        
        try {
            if (Files.exists(worldDir)) {
                Files.walk(worldDir)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + path);
                            }
                        });
                return true;
            }
        } catch (IOException e) {
            System.err.println("Failed to delete world: " + e.getMessage());
        }
        
        return false;
    }
    
    private Path getWorldDirectory() {
        return Paths.get(WORLDS_DIR, worldName);
    }
    
    // Getters and setters
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    
    public Vector3f getPlayerPosition() { return playerPosition; }
    public void setPlayerPosition(Vector3f playerPosition) { this.playerPosition = playerPosition; }
    
    public Vector3f getPlayerRotation() { return playerRotation; }
    public void setPlayerRotation(Vector3f playerRotation) { this.playerRotation = playerRotation; }
    
    public float getWorldTime() { return worldTime; }
    public void setWorldTime(float worldTime) { this.worldTime = worldTime; }
    
    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    
    public long getLastPlayedTime() { return lastPlayedTime; }
    public void setLastPlayedTime(long lastPlayedTime) { this.lastPlayedTime = lastPlayedTime; }
}