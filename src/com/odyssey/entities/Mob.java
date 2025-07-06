package com.odyssey.entities;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entity.Entity;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.rendering.Camera;
import com.odyssey.world.BlockType;
import com.odyssey.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Random;

public abstract class Mob extends Entity {
    protected Vector3f position;
    protected Vector3f velocity;
    protected Vector3f targetPosition;
    protected float health;
    protected float maxHealth;
    protected MobType type;
    protected MobState state;
    protected float speed;
    protected float jumpHeight;
    protected boolean onGround;
    protected Random random;
    protected float stateTimer;
    protected float attackCooldown;
    protected float detectionRange;
    protected boolean isHostile;
    
    // AI behavior timers
    protected float wanderTimer;
    protected float idleTimer;
    protected Vector3f wanderTarget;
    
    public enum MobType {
        ZOMBIE, SKELETON, SPIDER, CREEPER, COW, PIG, SHEEP, CHICKEN
    }
    
    public enum MobState {
        IDLE, WANDERING, CHASING, ATTACKING, FLEEING, DEAD
    }
    
    public Mob(Vector3f position, MobType type) {
        super(position);
        this.position = new Vector3f(position);
        this.velocity = new Vector3f();
        this.targetPosition = new Vector3f();
        this.type = type;
        this.state = MobState.IDLE;
        this.random = new Random();
        this.onGround = false;
        this.stateTimer = 0;
        this.attackCooldown = 0;
        this.wanderTimer = 0;
        this.idleTimer = 0;
        this.wanderTarget = new Vector3f();
        
        initializeMobStats();
        initializeMobModel();
    }
    
    protected abstract void initializeMobStats();
    
    protected abstract void initializeMobModel();
    
    @Override
    public void render(Camera camera, EnvironmentManager environmentManager) {
        if (state == MobState.DEAD) {
            return; // Don't render dead mobs
        }
        
        // Create model matrix for mob positioning
        Matrix4f modelMatrix = new Matrix4f()
            .translate(position.x, position.y, position.z)
            .scale(0.8f, 1.8f, 0.8f); // Mob size (width, height, depth)
        
        // Render mob model based on type
        renderMobModel(modelMatrix, camera, environmentManager);
    }
    
    protected abstract void renderMobModel(Matrix4f modelMatrix, Camera camera, EnvironmentManager environmentManager);
    
    public void update(float deltaTime, World world, Vector3f playerPosition) {
        // Update timers
        stateTimer += deltaTime;
        attackCooldown = Math.max(0, attackCooldown - deltaTime);
        wanderTimer = Math.max(0, wanderTimer - deltaTime);
        idleTimer = Math.max(0, idleTimer - deltaTime);
        
        if (health <= 0) {
            state = MobState.DEAD;
            return;
        }
        
        // Update AI behavior
        updateAI(deltaTime, world, playerPosition);
        
        // Apply physics
        updatePhysics(deltaTime, world);
        
        // Update position
        position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
    }
    
    protected abstract void updateAI(float deltaTime, World world, Vector3f playerPosition);
    
    protected void updatePhysics(float deltaTime, World world) {
        // Apply gravity
        if (!onGround) {
            velocity.y -= 9.8f * deltaTime; // Gravity
        }
        
        // Check ground collision
        Vector3i blockBelow = new Vector3i(
            (int) Math.floor(position.x),
            (int) Math.floor(position.y - 0.1f),
            (int) Math.floor(position.z)
        );
        
        BlockType blockType = world.getBlock(blockBelow.x, blockBelow.y, blockBelow.z);
        if (blockType != BlockType.AIR && blockType != BlockType.WATER) {
            if (velocity.y < 0) {
                velocity.y = 0;
                onGround = true;
                position.y = blockBelow.y + 1.0f;
            }
        } else {
            onGround = false;
        }
        
        // Apply friction
        velocity.x *= 0.8f;
        velocity.z *= 0.8f;
    }
    
    protected void moveTowards(Vector3f target, float deltaTime) {
        Vector3f direction = new Vector3f(target).sub(position);
        direction.y = 0; // Don't move vertically
        
        if (direction.length() > 0.1f) {
            direction.normalize();
            velocity.x += direction.x * speed * deltaTime;
            velocity.z += direction.z * speed * deltaTime;
            
            // Limit horizontal velocity
            float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed > speed) {
                velocity.x = (velocity.x / horizontalSpeed) * speed;
                velocity.z = (velocity.z / horizontalSpeed) * speed;
            }
        }
    }
    
    protected void jump() {
        if (onGround) {
            velocity.y = jumpHeight;
            onGround = false;
        }
    }
    
    protected float distanceToPlayer(Vector3f playerPosition) {
        return position.distance(playerPosition);
    }
    
    protected boolean canSeePlayer(Vector3f playerPosition, World world) {
        // Simple line-of-sight check
        float distance = distanceToPlayer(playerPosition);
        if (distance > detectionRange) {
            return false;
        }
        
        // Ray casting for line of sight (simplified)
        Vector3f direction = new Vector3f(playerPosition).sub(position).normalize();
        Vector3f rayPos = new Vector3f(position);
        
        for (float t = 0; t < distance; t += 0.5f) {
            rayPos.add(direction.x * 0.5f, direction.y * 0.5f, direction.z * 0.5f);
            
            BlockType blockType = world.getBlock(
                (int) Math.floor(rayPos.x),
                (int) Math.floor(rayPos.y),
                (int) Math.floor(rayPos.z)
            );
            
            if (blockType != BlockType.AIR && blockType != BlockType.WATER) {
                return false; // Blocked by solid block
            }
        }
        
        return true;
    }
    
    protected void generateWanderTarget() {
        float angle = random.nextFloat() * 2 * (float) Math.PI;
        float distance = 5 + random.nextFloat() * 10; // 5-15 blocks away
        
        wanderTarget.set(
            position.x + (float) Math.cos(angle) * distance,
            position.y,
            position.z + (float) Math.sin(angle) * distance
        );
        
        wanderTimer = 3 + random.nextFloat() * 5; // Wander for 3-8 seconds
    }
    
    public void takeDamage(float damage) {
        health = Math.max(0, health - damage);
        
        if (health <= 0) {
            state = MobState.DEAD;
        }
    }
    
    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }
    
    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public MobType getType() { return type; }
    public MobState getState() { return state; }
    public boolean isAlive() { return health > 0; }
    public boolean isHostile() { return isHostile; }
    
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
    
    public void setState(MobState state) {
        this.state = state;
        this.stateTimer = 0;
    }
}