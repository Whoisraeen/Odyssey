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

public class CowMob extends Mob {
    private static final float COW_SPEED = 1.0f;
    private static final float COW_HEALTH = 10.0f;
    private static final float COW_DETECTION_RANGE = 8.0f;
    private static final float COW_FLEE_RANGE = 4.0f;
    
    private float graze_timer;
    private boolean isGrazing;
    
    // Rendering data
    private int vao, vbo, ebo;
    private boolean modelInitialized = false;
    
    public CowMob(Vector3f position) {
        super(position, MobType.COW);
        this.graze_timer = 0;
        this.isGrazing = false;
    }
    
    @Override
    protected void initializeMobStats() {
        this.maxHealth = COW_HEALTH;
        this.health = maxHealth;
        this.speed = COW_SPEED;
        this.jumpHeight = 3.0f;
        this.detectionRange = COW_DETECTION_RANGE;
        this.isHostile = false;
    }
    
    @Override
    protected void initializeMobModel() {
        if (modelInitialized) return;
        
        // Simple cow model - a brown and white rectangular prism
        float[] vertices = {
            // Front face (brown and white pattern)
            -0.5f, -0.7f,  0.5f,  0.6f, 0.4f, 0.2f, // Bottom left
             0.5f, -0.7f,  0.5f,  0.6f, 0.4f, 0.2f, // Bottom right
             0.5f,  0.7f,  0.5f,  1.0f, 1.0f, 1.0f, // Top right (white)
            -0.5f,  0.7f,  0.5f,  1.0f, 1.0f, 1.0f, // Top left (white)
            
            // Back face
            -0.5f, -0.7f, -0.5f,  0.6f, 0.4f, 0.2f,
             0.5f, -0.7f, -0.5f,  0.6f, 0.4f, 0.2f,
             0.5f,  0.7f, -0.5f,  1.0f, 1.0f, 1.0f,
            -0.5f,  0.7f, -0.5f,  1.0f, 1.0f, 1.0f,
            
            // Left face
            -0.5f, -0.7f, -0.5f,  0.5f, 0.3f, 0.1f,
            -0.5f, -0.7f,  0.5f,  0.5f, 0.3f, 0.1f,
            -0.5f,  0.7f,  0.5f,  0.9f, 0.9f, 0.9f,
            -0.5f,  0.7f, -0.5f,  0.9f, 0.9f, 0.9f,
            
            // Right face
             0.5f, -0.7f, -0.5f,  0.5f, 0.3f, 0.1f,
             0.5f, -0.7f,  0.5f,  0.5f, 0.3f, 0.1f,
             0.5f,  0.7f,  0.5f,  0.9f, 0.9f, 0.9f,
             0.5f,  0.7f, -0.5f,  0.9f, 0.9f, 0.9f,
            
            // Top face (white)
            -0.5f,  0.7f, -0.5f,  1.0f, 1.0f, 1.0f,
             0.5f,  0.7f, -0.5f,  1.0f, 1.0f, 1.0f,
             0.5f,  0.7f,  0.5f,  1.0f, 1.0f, 1.0f,
            -0.5f,  0.7f,  0.5f,  1.0f, 1.0f, 1.0f,
            
            // Bottom face (brown)
            -0.5f, -0.7f, -0.5f,  0.4f, 0.2f, 0.1f,
             0.5f, -0.7f, -0.5f,  0.4f, 0.2f, 0.1f,
             0.5f, -0.7f,  0.5f,  0.4f, 0.2f, 0.1f,
            -0.5f, -0.7f,  0.5f,  0.4f, 0.2f, 0.1f
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
        
        // Draw the cow model
        GL11.glDrawElements(GL11.GL_TRIANGLES, 36, GL11.GL_UNSIGNED_INT, 0);
        
        GL30.glBindVertexArray(0);
    }
    
    @Override
    protected void updateAI(float deltaTime, World world, Vector3f playerPosition) {
        float distanceToPlayer = distanceToPlayer(playerPosition);
        graze_timer += deltaTime;
        
        switch (state) {
            case IDLE:
                idleTimer += deltaTime;
                
                // Check if player is too close and cow should flee
                if (distanceToPlayer < COW_FLEE_RANGE) {
                    setState(MobState.FLEEING);
                    generateFleeTarget(playerPosition);
                } else if (idleTimer > 3.0f) {
                    if (random.nextFloat() < 0.3f) {
                        // Start grazing
                        isGrazing = true;
                        graze_timer = 0;
                    } else {
                        // Start wandering
                        setState(MobState.WANDERING);
                        generateWanderTarget();
                    }
                }
                
                // Grazing behavior
                if (isGrazing) {
                    if (graze_timer > 5.0f) {
                        isGrazing = false;
                        setState(MobState.WANDERING);
                        generateWanderTarget();
                    }
                }
                break;
                
            case WANDERING:
                // Check if player is too close
                if (distanceToPlayer < COW_FLEE_RANGE) {
                    setState(MobState.FLEEING);
                    generateFleeTarget(playerPosition);
                } else {
                    moveTowards(wanderTarget, deltaTime);
                    
                    // Check if reached wander target or timer expired
                    if (position.distance(wanderTarget) < 2.0f || wanderTimer <= 0) {
                        setState(MobState.IDLE);
                        idleTimer = 0;
                    }
                }
                break;
                
            case FLEEING:
                if (distanceToPlayer > COW_DETECTION_RANGE) {
                    // Player is far enough, stop fleeing
                    setState(MobState.IDLE);
                    idleTimer = 0;
                } else {
                    // Continue fleeing
                    moveTowards(wanderTarget, deltaTime);
                    
                    // If flee timer expired, generate new flee target
                    if (wanderTimer <= 0) {
                        generateFleeTarget(playerPosition);
                    }
                }
                break;
                
            case CHASING:
                // Cows don't chase, go back to idle
                setState(MobState.IDLE);
                break;
                
            case ATTACKING:
                // Cows don't attack, go back to idle
                setState(MobState.IDLE);
                break;
                
            case DEAD:
                // Do nothing when dead
                break;
        }
    }
    
    private void generateFleeTarget(Vector3f playerPosition) {
        // Generate a target away from the player
        Vector3f fleeDirection = new Vector3f(position).sub(playerPosition).normalize();
        float fleeDistance = 10 + random.nextFloat() * 10; // 10-20 blocks away
        
        wanderTarget.set(
            position.x + fleeDirection.x * fleeDistance,
            position.y,
            position.z + fleeDirection.z * fleeDistance
        );
        
        wanderTimer = 5 + random.nextFloat() * 3; // Flee for 5-8 seconds
    }
    
    public boolean isGrazing() {
        return isGrazing;
    }
    
    /**
     * Called when player interacts with cow (e.g., milking)
     */
    public void interact() {
        // In a real implementation, this might give milk or other resources
        System.out.println("Cow moos softly!");
    }
}