package com.odyssey.entities;

import com.odyssey.world.World;
import org.joml.Vector3f;

public class CowMob extends Mob {
    private static final float COW_SPEED = 1.0f;
    private static final float COW_HEALTH = 10.0f;
    private static final float COW_DETECTION_RANGE = 8.0f;
    private static final float COW_FLEE_RANGE = 4.0f;
    
    private float graze_timer;
    private boolean isGrazing;
    
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