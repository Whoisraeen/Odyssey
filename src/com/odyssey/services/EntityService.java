package com.odyssey.services;

import com.odyssey.entity.Entity;
import com.odyssey.entity.Ship;
import com.odyssey.player.Player;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Service interface for entity management
 */
public interface EntityService {
    
    /**
     * Add entity to the world
     */
    void addEntity(Entity entity);
    
    /**
     * Remove entity from the world
     */
    void removeEntity(Entity entity);
    
    /**
     * Get entity at specified position
     */
    Entity getEntityAt(Vector3i position);
    
    /**
     * Update all entities
     */
    void update(float deltaTime);
    
    /**
     * Handle player ship control
     */
    void handleShipControl(Player player, Vector3i helmPosition);
    
    /**
     * Assemble ship at specified position
     */
    Ship assembleShip(Vector3i helmPosition);
    
    /**
     * Update mob spawning
     */
    void updateMobSpawning(float deltaTime);
    
    /**
     * Cleanup entity resources
     */
    void cleanup();
}