package com.odyssey.entities;

import com.odyssey.world.World;
import org.joml.Vector3f;

public class ZombieMob extends Mob {
    private static final float ZOMBIE_SPEED = 2.0f;
    private static final float ZOMBIE_HEALTH = 20.0f;
    private static final float ZOMBIE_DETECTION_RANGE = 16.0f;
    private static final float ZOMBIE_ATTACK_RANGE = 2.0f;
    private static final float ZOMBIE_ATTACK_DAMAGE = 4.0f;
    private static final float ZOMBIE_ATTACK_COOLDOWN = 1.0f;
    
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