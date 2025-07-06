package com.odyssey.entities;

import com.odyssey.environment.EnvironmentManager;
import com.odyssey.rendering.Camera;
import com.odyssey.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

public class ZombieMob extends Mob {
    private static final float ZOMBIE_SPEED = 2.0f;
    private static final float ZOMBIE_HEALTH = 20.0f;
    private static final float ZOMBIE_DETECTION_RANGE = 16.0f;
    private static final float ZOMBIE_ATTACK_RANGE = 2.0f;
    private static final float ZOMBIE_ATTACK_DAMAGE = 4.0f;
    private static final float ZOMBIE_ATTACK_COOLDOWN = 1.0f;
    
    // Rendering data
    private int vao, vbo, ebo;
    private int shaderProgram;
    private boolean modelInitialized = false;
    
    public ZombieMob(Vector3f position) {
        super(position, MobType.ZOMBIE);
    }
    
    @Override
    protected void initializeMobStats() {
        this.maxHealth = ZOMBIE_HEALTH;
        this.health = maxHealth;
        this.speed = ZOMBIE_SPEED;
        this.jumpHeight = 4.0f;
        this.detectionRange = ZOMBIE_DETECTION_RANGE;
        this.isHostile = true;
    }
    
    @Override
    protected void initializeMobModel() {
        if (modelInitialized) return;
        
        // Simple zombie model - a green rectangular prism
        float[] vertices = {
            // Front face (green)
            -0.4f, -0.9f,  0.4f,  0.0f, 0.6f, 0.0f, // Bottom left
             0.4f, -0.9f,  0.4f,  0.0f, 0.6f, 0.0f, // Bottom right
             0.4f,  0.9f,  0.4f,  0.0f, 0.6f, 0.0f, // Top right
            -0.4f,  0.9f,  0.4f,  0.0f, 0.6f, 0.0f, // Top left
            
            // Back face (dark green)
            -0.4f, -0.9f, -0.4f,  0.0f, 0.4f, 0.0f,
             0.4f, -0.9f, -0.4f,  0.0f, 0.4f, 0.0f,
             0.4f,  0.9f, -0.4f,  0.0f, 0.4f, 0.0f,
            -0.4f,  0.9f, -0.4f,  0.0f, 0.4f, 0.0f,
            
            // Left face
            -0.4f, -0.9f, -0.4f,  0.0f, 0.5f, 0.0f,
            -0.4f, -0.9f,  0.4f,  0.0f, 0.5f, 0.0f,
            -0.4f,  0.9f,  0.4f,  0.0f, 0.5f, 0.0f,
            -0.4f,  0.9f, -0.4f,  0.0f, 0.5f, 0.0f,
            
            // Right face
             0.4f, -0.9f, -0.4f,  0.0f, 0.5f, 0.0f,
             0.4f, -0.9f,  0.4f,  0.0f, 0.5f, 0.0f,
             0.4f,  0.9f,  0.4f,  0.0f, 0.5f, 0.0f,
             0.4f,  0.9f, -0.4f,  0.0f, 0.5f, 0.0f,
            
            // Top face (lighter green)
            -0.4f,  0.9f, -0.4f,  0.2f, 0.8f, 0.2f,
             0.4f,  0.9f, -0.4f,  0.2f, 0.8f, 0.2f,
             0.4f,  0.9f,  0.4f,  0.2f, 0.8f, 0.2f,
            -0.4f,  0.9f,  0.4f,  0.2f, 0.8f, 0.2f,
            
            // Bottom face (darker green)
            -0.4f, -0.9f, -0.4f,  0.0f, 0.3f, 0.0f,
             0.4f, -0.9f, -0.4f,  0.0f, 0.3f, 0.0f,
             0.4f, -0.9f,  0.4f,  0.0f, 0.3f, 0.0f,
            -0.4f, -0.9f,  0.4f,  0.0f, 0.3f, 0.0f
        };
        
        int[] indices = {
            // Front face
            0, 1, 2, 2, 3, 0,
            // Back face
            4, 5, 6, 6, 7, 4,
            // Left face
            8, 9, 10, 10, 11, 8,
            // Right face
            12, 13, 14, 14, 15, 12,
            // Top face
            16, 17, 18, 18, 19, 16,
            // Bottom face
            20, 21, 22, 22, 23, 20
        };
        
        // Create VAO, VBO, EBO
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        ebo = GL15.glGenBuffers();
        
        GL30.glBindVertexArray(vao);
        
        // Upload vertex data
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        
        // Upload index data
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
        
        // Position attribute (location 0)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        
        // Color attribute (location 1)
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        
        GL30.glBindVertexArray(0);
        
        modelInitialized = true;
    }
    
    @Override
    protected void renderMobModel(Matrix4f modelMatrix, Camera camera, EnvironmentManager environmentManager) {
        if (!modelInitialized) {
            initializeMobModel();
        }
        
        // Simple rendering without shaders for now - just draw the geometry
        GL30.glBindVertexArray(vao);
        
        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        
        // Draw the zombie model
        GL11.glDrawElements(GL11.GL_TRIANGLES, 36, GL11.GL_UNSIGNED_INT, 0);
        
        GL30.glBindVertexArray(0);
    }
    
    @Override
    protected void updateAI(float deltaTime, World world, Vector3f playerPosition) {
        float distanceToPlayer = distanceToPlayer(playerPosition);
        
        switch (state) {
            case IDLE:
                idleTimer += deltaTime;
                if (idleTimer > 2.0f) {
                    if (canSeePlayer(playerPosition, world) && distanceToPlayer < detectionRange) {
                        setState(MobState.CHASING);
                        targetPosition.set(playerPosition);
                    } else {
                        setState(MobState.WANDERING);
                        generateWanderTarget();
                    }
                }
                break;
                
            case WANDERING:
                if (canSeePlayer(playerPosition, world) && distanceToPlayer < detectionRange) {
                    setState(MobState.CHASING);
                    targetPosition.set(playerPosition);
                } else {
                    moveTowards(wanderTarget, deltaTime);
                    
                    // Check if reached wander target or timer expired
                    if (position.distance(wanderTarget) < 2.0f || wanderTimer <= 0) {
                        setState(MobState.IDLE);
                        idleTimer = 0;
                    }
                }
                break;
                
            case CHASING:
                if (distanceToPlayer > detectionRange * 1.5f) {
                    // Lost sight of player
                    setState(MobState.WANDERING);
                    generateWanderTarget();
                } else if (distanceToPlayer <= ZOMBIE_ATTACK_RANGE) {
                    setState(MobState.ATTACKING);
                } else {
                    // Chase the player
                    targetPosition.set(playerPosition);
                    moveTowards(targetPosition, deltaTime);
                    
                    // Jump if there's a block in the way
                    if (onGround && random.nextFloat() < 0.1f) {
                        jump();
                    }
                }
                break;
                
            case ATTACKING:
                if (distanceToPlayer > ZOMBIE_ATTACK_RANGE) {
                    setState(MobState.CHASING);
                } else if (attackCooldown <= 0) {
                    // Perform attack
                    attackPlayer(playerPosition);
                    attackCooldown = ZOMBIE_ATTACK_COOLDOWN;
                }
                break;
                
            case FLEEING:
                // Zombies don't flee, go back to chasing
                setState(MobState.CHASING);
                break;
                
            case DEAD:
                // Do nothing when dead
                break;
        }
    }
    
    private void attackPlayer(Vector3f playerPosition) {
        // In a real implementation, this would damage the player
        // For now, we'll just create a visual/audio effect
        System.out.println("Zombie attacks player for " + ZOMBIE_ATTACK_DAMAGE + " damage!");
    }
    
    public float getAttackDamage() {
        return ZOMBIE_ATTACK_DAMAGE;
    }
}