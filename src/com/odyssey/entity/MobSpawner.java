package com.odyssey.entity;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entities.Mob;
import com.odyssey.entities.ZombieMob;
import com.odyssey.entities.CowMob;
import com.odyssey.player.Player;
import com.odyssey.world.BlockType;
import com.odyssey.world.biome.Biome;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Manages mob spawning and lifecycle in the game world
 */
public class MobSpawner {
    private final VoxelEngine engine;
    private final List<Mob> activeMobs;
    private final Random random;
    
    // Spawning configuration
    private static final int MAX_MOBS = 50;
    private static final float SPAWN_RADIUS = 32.0f;
    private static final float DESPAWN_RADIUS = 64.0f;
    private static final float SPAWN_COOLDOWN = 5.0f; // seconds
    private static final int MAX_SPAWN_ATTEMPTS = 10;
    
    private float spawnTimer;
    
    public MobSpawner(VoxelEngine engine) {
        this.engine = engine;
        this.activeMobs = new ArrayList<>();
        this.random = new Random();
        this.spawnTimer = 0.0f;
    }
    
    public void update(float deltaTime) {
        updateActiveMobs(deltaTime);
        attemptSpawning(deltaTime);
        despawnDistantMobs();
    }
    
    private void updateActiveMobs(float deltaTime) {
        Iterator<Mob> iterator = activeMobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            
            // Remove dead mobs
            if (mob.getHealth() <= 0) {
                iterator.remove();
                continue;
            }
            
            // Update mob
            Player player = engine.getPlayer();
            Vector3f playerPosition = player != null ? player.getPosition() : new Vector3f(0, 0, 0);
            mob.update(deltaTime, engine.getWorld(), playerPosition);
        }
    }
    
    private void attemptSpawning(float deltaTime) {
        spawnTimer += deltaTime;
        
        if (spawnTimer >= SPAWN_COOLDOWN && activeMobs.size() < MAX_MOBS) {
            spawnTimer = 0.0f;
            
            Player player = engine.getPlayer();
            Vector3f playerPos = player.getPosition();
            
            for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
                Vector3f spawnPos = generateSpawnPosition(playerPos);
                if (spawnPos != null && isValidSpawnPosition(spawnPos)) {
                    Mob.MobType mobType = selectMobType(spawnPos);
                    if (mobType != null) {
                        Mob mob = createMob(mobType, spawnPos);
                        if (mob != null) {
                            activeMobs.add(mob);
                            break; // Successfully spawned, stop attempting
                        }
                    }
                }
            }
        }
    }
    
    private Vector3f generateSpawnPosition(Vector3f playerPos) {
        // Generate random position around player
        float angle = random.nextFloat() * 2.0f * (float)Math.PI;
        float distance = 16.0f + random.nextFloat() * (SPAWN_RADIUS - 16.0f);
        
        float x = playerPos.x + (float)Math.cos(angle) * distance;
        float z = playerPos.z + (float)Math.sin(angle) * distance;
        
        // Find suitable Y position (ground level)
        for (int y = (int)playerPos.y + 10; y >= 0; y--) {
            BlockType blockBelow = engine.getBlock((int)x, y - 1, (int)z);
            BlockType blockAt = engine.getBlock((int)x, y, (int)z);
            BlockType blockAbove = engine.getBlock((int)x, y + 1, (int)z);
            
            if (blockBelow != BlockType.AIR && blockAt == BlockType.AIR && blockAbove == BlockType.AIR) {
                return new Vector3f(x, y, z);
            }
        }
        
        return null; // No valid spawn position found
    }
    
    private boolean isValidSpawnPosition(Vector3f pos) {
        // Check if position is not too close to player
        Vector3f playerPos = engine.getPlayer().getPosition();
        float distanceToPlayer = pos.distance(playerPos);
        
        if (distanceToPlayer < 16.0f) {
            return false;
        }
        
        // Check if there's enough space for the mob
        int x = (int)pos.x;
        int y = (int)pos.y;
        int z = (int)pos.z;
        
        return engine.getBlock(x, y, z) == BlockType.AIR && 
               engine.getBlock(x, y + 1, z) == BlockType.AIR &&
               engine.getBlock(x, y - 1, z) != BlockType.AIR;
    }
    
    private Mob.MobType selectMobType(Vector3f pos) {
        int x = (int)pos.x;
        int z = (int)pos.z;
        
        Biome biome = engine.getBiomeAt(x, z);
        
        // Get light level at spawn position
        int lightLevel = getLightLevel(pos);
        
        // Hostile mobs spawn in dark areas
        if (lightLevel < 7) {
            return Mob.MobType.ZOMBIE;
        }
        
        // Passive mobs spawn in light areas
        if (lightLevel > 8) {
            // Biome-specific spawning could be added here
            if (random.nextFloat() < 0.3f) {
                return Mob.MobType.COW;
            }
        }
        
        return null; // No suitable mob type
    }
    
    private int getLightLevel(Vector3f pos) {
        // Simplified light level calculation
        // In a full implementation, this would check the lighting engine
        int x = (int)pos.x;
        int y = (int)pos.y;
        int z = (int)pos.z;
        
        // Check if position is exposed to sky
        for (int checkY = y + 1; checkY < com.odyssey.core.GameConstants.MAX_HEIGHT; checkY++) {
            if (engine.getBlock(x, checkY, z) != BlockType.AIR) {
                return 0; // Underground or covered
            }
        }
        
        return 15; // Exposed to sky
    }
    
    private Mob createMob(Mob.MobType type, Vector3f position) {
        switch (type) {
            case ZOMBIE:
                return new ZombieMob(position);
            case COW:
                return new CowMob(position);
            default:
                return null;
        }
    }
    
    private void despawnDistantMobs() {
        Vector3f playerPos = engine.getPlayer().getPosition();
        
        activeMobs.removeIf(mob -> {
            float distance = mob.getPosition().distance(playerPos);
            return distance > DESPAWN_RADIUS;
        });
    }
    
    // Utility methods for external access
    public List<Mob> getActiveMobs() {
        return new ArrayList<>(activeMobs);
    }
    
    public void addMob(Mob mob) {
        if (activeMobs.size() < MAX_MOBS) {
            activeMobs.add(mob);
        }
    }
    
    public void removeMob(Mob mob) {
        activeMobs.remove(mob);
    }
    
    public int getActiveMobCount() {
        return activeMobs.size();
    }
    
    public int getMaxMobs() {
        return MAX_MOBS;
    }
}