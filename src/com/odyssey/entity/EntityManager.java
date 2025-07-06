package com.odyssey.entity;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entities.Mob;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.rendering.Camera;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class EntityManager {
    
    private final List<Entity> entities = new ArrayList<>();
    private final List<Mob> mobs = new ArrayList<>();
    
    public void addEntity(Entity entity) {
        entities.add(entity);
    }
    
    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }
    
    public void addMob(Mob mob) {
        mobs.add(mob);
    }
    
    public void removeMob(Mob mob) {
        mobs.remove(mob);
    }
    
    public Entity getEntityAt(Vector3i worldPos) {
        for (Entity entity : entities) {
            if (entity instanceof Ship) {
                // A simple AABB check for now
                Vector3f shipMin = entity.getPosition();
                Vector3f shipMax = new Vector3f(shipMin).add(((Ship) entity).getDimensions());
                
                if (worldPos.x >= shipMin.x && worldPos.x < shipMax.x &&
                    worldPos.y >= shipMin.y && worldPos.y < shipMax.y &&
                    worldPos.z >= shipMin.z && worldPos.z < shipMax.z) {
                    return entity;
                }
            }
        }
        return null;
    }
    
    public void update(float deltaTime, VoxelEngine engine, EnvironmentManager environmentManager) {
        // Update entities
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            entity.update(deltaTime, engine, environmentManager);
            if (entity.isRemoved()) {
                entities.remove(i);
            }
        }
        
        // Update mobs
        for (int i = mobs.size() - 1; i >= 0; i--) {
            Mob mob = mobs.get(i);
            // Note: Mob update method signature is different, we'll need to adapt this
            // For now, we'll skip mob updates in EntityManager since they're handled elsewhere
            if (!mob.isAlive()) {
                mobs.remove(i);
            }
        }
    }
    
    public void render(int shaderProgram, Camera camera) {
        for (Entity entity : entities) {
            if (entity instanceof Ship) {
                ((Ship) entity).render(shaderProgram, camera);
            }
        }
    }
    
    public void render(Camera camera, EnvironmentManager environmentManager) {
        // Render entities
        for (Entity entity : entities) {
            if (entity instanceof Ship) {
                ((Ship) entity).render(camera, environmentManager);
            }
        }
        
        // Render mobs
        for (Mob mob : mobs) {
            if (mob.isAlive()) {
                mob.render(camera, environmentManager);
            }
        }
    }
    
    public int getEntityCount() {
        return entities.size();
    }
    
    public int getMobCount() {
        return mobs.size();
    }
    
    public int getTotalEntityCount() {
        return entities.size() + mobs.size();
    }
    
    public void cleanup() {
        for (Entity entity : entities) {
            if (entity instanceof Ship) {
                ((Ship) entity).cleanup();
            }
        }
        
        // Cleanup mobs (if they have cleanup methods in the future)
        mobs.clear();
    }
}