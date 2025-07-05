package com.odyssey.entity;

import com.odyssey.core.VoxelEngine;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.rendering.scene.Camera;
import org.joml.Vector3f;

public abstract class Entity {
    
    protected Vector3f position;
    protected Vector3f velocity;
    protected Vector3f rotation; // Euler angles (pitch, yaw, roll)
    protected boolean removed = false;

    public Entity(Vector3f position) {
        this.position = position;
        this.velocity = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
    }
    
    public abstract void update(float deltaTime, VoxelEngine engine, EnvironmentManager environmentManager);
    public abstract void render(int shaderProgram, Camera camera);
    public abstract void cleanup();
    
    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity = velocity;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public boolean isRemoved() {
        return removed;
    }
} 