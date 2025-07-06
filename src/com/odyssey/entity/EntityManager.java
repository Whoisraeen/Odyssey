package com.odyssey.entity;

import com.odyssey.core.VoxelEngine;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.rendering.Camera;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class EntityManager {
    
    private final List<Entity> entities = new ArrayList<>();
    
    public void addEntity(Entity entity) {
        entities.add(entity);
    }
    
    public void removeEntity(Entity entity) {
        entities.remove(entity);
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
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            entity.update(deltaTime, engine, environmentManager);
            if (entity.isRemoved()) {
                entities.remove(i);
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
    
    public int getEntityCount() {
        return entities.size();
    }
    
    public void cleanup() {
        for (Entity entity : entities) {
            if (entity instanceof Ship) {
                ((Ship) entity).cleanup();
            }
        }
    }
}