package com.odyssey.services.impl;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.EntityService;
import com.odyssey.services.EnvironmentService;
import com.odyssey.entity.*;
import com.odyssey.player.Player;
import com.odyssey.ship.Shipyard;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityServiceImpl implements EntityService {
    
    private static final Logger logger = LoggerFactory.getLogger(EntityServiceImpl.class);
    
    @Autowired
    private GameConfiguration config;
    
    @Autowired
    private EnvironmentService environmentService;
    
    private EntityManager entityManager;
    private MobSpawner mobSpawner;
    
    public EntityServiceImpl() {
        this.entityManager = new EntityManager();
        this.mobSpawner = new MobSpawner(null); // Will be set later
    }
    
    public void setMobSpawner(MobSpawner mobSpawner) {
        this.mobSpawner = mobSpawner;
    }
    
    public void setVoxelEngine(com.odyssey.core.VoxelEngine voxelEngine) {
        // Recreate MobSpawner with proper VoxelEngine reference
        this.mobSpawner = new MobSpawner(voxelEngine);
        this.mobSpawner.setEntityManager(this.entityManager);
    }
    
    @Override
    public void addEntity(Entity entity) {
        entityManager.addEntity(entity);
        logger.debug("Added entity: {} at position {}", entity.getClass().getSimpleName(), entity.getPosition());
    }
    
    @Override
    public void removeEntity(Entity entity) {
        entityManager.removeEntity(entity);
        logger.debug("Removed entity: {}", entity.getClass().getSimpleName());
    }
    
    @Override
    public Entity getEntityAt(Vector3i position) {
        return entityManager.getEntityAt(position);
    }
    
    @Override
    public void update(float deltaTime) {
        entityManager.update(deltaTime, null, environmentService.getEnvironmentManager()); // VoxelEngine reference will be set later
        logger.debug("Updated {} entities", entityManager.getEntityCount());
    }
    
    @Override
    public void handleShipControl(Player player, Vector3i helmPosition) {
        // Check if this helm is part of an existing ship
        Entity shipEntity = getEntityAt(helmPosition);
        if (shipEntity instanceof Ship) {
            player.setControlledShip(player.isControllingShip() ? null : (Ship)shipEntity);
            logger.info("Player {} ship control", player.isControllingShip() ? "enabled" : "disabled");
        } else {
            Ship ship = assembleShip(helmPosition);
            if (ship != null) {
                addEntity(ship);
                player.setControlledShip(ship);
                logger.info("Assembled and took control of new ship at {}", helmPosition);
            }
        }
    }
    
    @Override
    public Ship assembleShip(Vector3i helmPosition) {
        Ship ship = Shipyard.assembleShip(null, helmPosition); // VoxelEngine reference will be set later
        if (ship != null) {
            logger.info("Successfully assembled ship at {}", helmPosition);
        } else {
            logger.warn("Failed to assemble ship at {}", helmPosition);
        }
        return ship;
    }
    
    @Override
    public void updateMobSpawning(float deltaTime) {
        if (mobSpawner != null) {
            mobSpawner.update(deltaTime);
        }
    }
    
    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    @Override
    public void cleanup() {
        if (entityManager != null) {
            entityManager.cleanup();
        }
        logger.info("Entity service cleaned up");
    }
}