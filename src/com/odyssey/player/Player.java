package com.odyssey.player;

import com.odyssey.core.VoxelEngine;
import com.odyssey.world.BlockType;
import org.joml.Vector3f;

public class Player {

    private static final float GRAVITY = -30.0f;
    public static final float MOVE_SPEED = 5.0f;
    public static final float JUMP_POWER = 10.0f;

    private final Vector3f position;
    private final Vector3f velocity;

    private boolean onGround = false;

    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_WIDTH = 0.6f;

    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
    }

    public void update(float deltaTime, VoxelEngine world) {
        // Apply gravity
        velocity.y += GRAVITY * deltaTime;

        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;

        // Reset horizontal velocity after applying it for this frame
        velocity.x = 0;
        velocity.z = 0;

        // Broad-phase collision check extents
        float checkWidth = PLAYER_WIDTH / 2f;

        // Check for collisions and resolve them one axis at a time
        // Y-axis
        position.y += dy;
        if (checkCollision(world, 0, dy, 0)) {
            position.y -= dy;
            if (velocity.y < 0) {
                onGround = true;
            }
            velocity.y = 0;
        } else {
            onGround = false;
        }

        // X-axis
        position.x += dx;
        if (checkCollision(world, dx, 0, 0)) {
            position.x -= dx;
        }

        // Z-axis
        position.z += dz;
        if (checkCollision(world, 0, 0, dz)) {
            position.z -= dz;
        }
    }

    private boolean checkCollision(VoxelEngine world, float dx, float dy, float dz) {
        // Create an AABB for the player
        Vector3f min = new Vector3f(position.x - PLAYER_WIDTH / 2, position.y, position.z - PLAYER_WIDTH / 2);
        Vector3f max = new Vector3f(position.x + PLAYER_WIDTH / 2, position.y + PLAYER_HEIGHT, position.z + PLAYER_WIDTH / 2);

        // Check all blocks that could be colliding with the player AABB
        for (int x = (int) Math.floor(min.x); x <= Math.floor(max.x); x++) {
            for (int y = (int) Math.floor(min.y); y <= Math.floor(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= Math.floor(max.z); z++) {
                    if (world.getBlock(x, y, z) != BlockType.AIR) {
                        return true; // Collision detected
                    }
                }
            }
        }
        return false;
    }


    public void jump() {
        if (onGround) {
            velocity.y = JUMP_POWER;
            onGround = false;
        }
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }
} 