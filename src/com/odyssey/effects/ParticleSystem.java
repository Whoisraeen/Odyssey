package com.odyssey.effects;

import com.odyssey.core.VoxelEngine;
import com.odyssey.world.BlockType;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles particle effects for the voxel world
 */
public class ParticleSystem {
    private final VoxelEngine voxelEngine;
    private final Queue<Particle> particles;
    private final Random random;
    
    // Particle limits to prevent performance issues
    private static final int MAX_PARTICLES = 1000;
    private static final int MAX_PARTICLES_PER_FRAME = 50;
    
    public ParticleSystem(VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
        this.particles = new ConcurrentLinkedQueue<>();
        this.random = new Random();
    }
    
    /**
     * Update all particles
     */
    public void update(float deltaTime) {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(deltaTime, voxelEngine);
            
            if (particle.isDead()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Create block break particles
     */
    public void createBlockBreakParticles(int x, int y, int z, BlockType blockType) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        
        Vector4f color = getBlockColor(blockType);
        int particleCount = Math.min(8, MAX_PARTICLES_PER_FRAME);
        
        for (int i = 0; i < particleCount; i++) {
            Vector3f position = new Vector3f(
                x + 0.5f + (random.nextFloat() - 0.5f) * 0.8f,
                y + 0.5f + (random.nextFloat() - 0.5f) * 0.8f,
                z + 0.5f + (random.nextFloat() - 0.5f) * 0.8f
            );
            
            Vector3f velocity = new Vector3f(
                (random.nextFloat() - 0.5f) * 4.0f,
                random.nextFloat() * 3.0f + 1.0f,
                (random.nextFloat() - 0.5f) * 4.0f
            );
            
            particles.offer(new Particle(
                position, velocity, color, 
                1.0f + random.nextFloat() * 1.0f, // lifetime 1-2 seconds
                0.1f + random.nextFloat() * 0.05f, // size 0.1-0.15
                ParticleType.BLOCK_BREAK
            ));
        }
    }
    
    /**
     * Create water splash particles
     */
    public void createWaterSplashParticles(int x, int y, int z) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        
        Vector4f waterColor = new Vector4f(0.2f, 0.5f, 1.0f, 0.8f);
        int particleCount = Math.min(6, MAX_PARTICLES_PER_FRAME);
        
        for (int i = 0; i < particleCount; i++) {
            Vector3f position = new Vector3f(
                x + 0.5f + (random.nextFloat() - 0.5f) * 0.6f,
                y + 0.8f,
                z + 0.5f + (random.nextFloat() - 0.5f) * 0.6f
            );
            
            Vector3f velocity = new Vector3f(
                (random.nextFloat() - 0.5f) * 2.0f,
                random.nextFloat() * 2.0f + 0.5f,
                (random.nextFloat() - 0.5f) * 2.0f
            );
            
            particles.offer(new Particle(
                position, velocity, waterColor,
                0.5f + random.nextFloat() * 0.5f, // lifetime 0.5-1 seconds
                0.08f + random.nextFloat() * 0.04f, // size 0.08-0.12
                ParticleType.WATER_SPLASH
            ));
        }
    }
    
    /**
     * Create fire particles
     */
    public void createFireParticles(int x, int y, int z) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        
        // Create 2-3 fire particles per frame
        int particleCount = Math.min(2 + random.nextInt(2), MAX_PARTICLES_PER_FRAME);
        
        for (int i = 0; i < particleCount; i++) {
            Vector3f position = new Vector3f(
                x + 0.3f + random.nextFloat() * 0.4f,
                y + 0.1f + random.nextFloat() * 0.3f,
                z + 0.3f + random.nextFloat() * 0.4f
            );
            
            Vector3f velocity = new Vector3f(
                (random.nextFloat() - 0.5f) * 0.5f,
                0.5f + random.nextFloat() * 1.0f,
                (random.nextFloat() - 0.5f) * 0.5f
            );
            
            // Fire colors: red to orange to yellow
            Vector4f fireColor = new Vector4f(
                1.0f,
                0.3f + random.nextFloat() * 0.7f,
                random.nextFloat() * 0.3f,
                0.9f
            );
            
            particles.offer(new Particle(
                position, velocity, fireColor,
                0.3f + random.nextFloat() * 0.4f, // lifetime 0.3-0.7 seconds
                0.05f + random.nextFloat() * 0.03f, // size 0.05-0.08
                ParticleType.FIRE
            ));
        }
    }
    
    /**
     * Create smoke particles
     */
    public void createSmokeParticles(int x, int y, int z) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        
        Vector4f smokeColor = new Vector4f(0.3f, 0.3f, 0.3f, 0.6f);
        
        Vector3f position = new Vector3f(
            x + 0.4f + random.nextFloat() * 0.2f,
            y + 0.8f,
            z + 0.4f + random.nextFloat() * 0.2f
        );
        
        Vector3f velocity = new Vector3f(
            (random.nextFloat() - 0.5f) * 0.3f,
            0.8f + random.nextFloat() * 0.4f,
            (random.nextFloat() - 0.5f) * 0.3f
        );
        
        particles.offer(new Particle(
            position, velocity, smokeColor,
            2.0f + random.nextFloat() * 1.0f, // lifetime 2-3 seconds
            0.1f + random.nextFloat() * 0.05f, // size 0.1-0.15
            ParticleType.SMOKE
        ));
    }
    
    /**
     * Get color for a block type
     */
    private Vector4f getBlockColor(BlockType blockType) {
        switch (blockType) {
            case DIRT:
                return new Vector4f(0.6f, 0.4f, 0.2f, 1.0f);
            case GRASS:
                return new Vector4f(0.3f, 0.7f, 0.2f, 1.0f);
            case STONE:
                return new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
            case COBBLESTONE:
                return new Vector4f(0.4f, 0.4f, 0.4f, 1.0f);
            case SAND:
                return new Vector4f(0.9f, 0.8f, 0.6f, 1.0f);
            case WOOD:
                return new Vector4f(0.6f, 0.4f, 0.2f, 1.0f);
            case LEAVES:
                return new Vector4f(0.2f, 0.6f, 0.2f, 1.0f);
            case WATER:
                return new Vector4f(0.2f, 0.5f, 1.0f, 0.8f);
            default:
                return new Vector4f(0.7f, 0.7f, 0.7f, 1.0f);
        }
    }
    
    /**
     * Emit a single particle with specified parameters
     */
    public void emit(Vector3f position, Vector3f velocity, Vector3f color) {
        if (particles.size() >= MAX_PARTICLES) {
            return;
        }
        
        Vector4f particleColor = new Vector4f(color.x, color.y, color.z, 1.0f);
        
        particles.offer(new Particle(
            position, velocity, particleColor,
            1.0f + random.nextFloat() * 2.0f, // lifetime 1-3 seconds
            0.05f + random.nextFloat() * 0.05f, // size 0.05-0.1
            ParticleType.BLOCK_BREAK
        ));
    }
    
    /**
     * Get all active particles for rendering
     */
    public Collection<Particle> getParticles() {
        return particles;
    }
    
    /**
     * Clear all particles
     */
    public void clear() {
        particles.clear();
    }
    
    /**
     * Get particle count for debugging
     */
    public int getParticleCount() {
        return particles.size();
    }
    
    /**
     * Individual particle class
     */
    public static class Particle {
        private final Vector3f position;
        private final Vector3f velocity;
        private final Vector4f color;
        private final float maxLifetime;
        private final float size;
        private final ParticleType type;
        
        private float lifetime;
        private float alpha;
        
        public Particle(Vector3f position, Vector3f velocity, Vector4f color, 
                       float maxLifetime, float size, ParticleType type) {
            this.position = new Vector3f(position);
            this.velocity = new Vector3f(velocity);
            this.color = new Vector4f(color);
            this.maxLifetime = maxLifetime;
            this.size = size;
            this.type = type;
            this.lifetime = 0.0f;
            this.alpha = color.w;
        }
        
        public void update(float deltaTime, VoxelEngine voxelEngine) {
            lifetime += deltaTime;
            
            // Update position
            position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
            
            // Apply gravity
            if (type != ParticleType.FIRE && type != ParticleType.SMOKE) {
                velocity.y -= 9.8f * deltaTime;
            }
            
            // Apply air resistance
            velocity.mul(0.98f);
            
            // Fade out over time
            float lifeRatio = lifetime / maxLifetime;
            alpha = color.w * (1.0f - lifeRatio);
            
            // Special behavior for different particle types
            switch (type) {
                case FIRE:
                    // Fire particles rise and fade
                    velocity.y *= 0.99f;
                    break;
                case SMOKE:
                    // Smoke particles rise slowly and expand
                    velocity.y *= 0.995f;
                    break;
                case WATER_SPLASH:
                    // Water particles have more gravity
                    velocity.y -= 5.0f * deltaTime;
                    break;
            }
            
            // Check collision with blocks (simple ground collision)
            if (position.y <= 0) {
                position.y = 0;
                velocity.y = 0;
                velocity.mul(0.5f); // Bounce dampening
            }
        }
        
        public boolean isDead() {
            return lifetime >= maxLifetime || alpha <= 0.01f;
        }
        
        // Getters
        public Vector3f getPosition() { return position; }
        public Vector3f getVelocity() { return velocity; }
        public Vector4f getColor() { return new Vector4f(color.x, color.y, color.z, alpha); }
        public float getSize() { return size; }
        public ParticleType getType() { return type; }
        public float getLifetime() { return lifetime; }
        public float getMaxLifetime() { return maxLifetime; }
    }
    
    /**
     * Particle types for different behaviors
     */
    public enum ParticleType {
        BLOCK_BREAK,
        WATER_SPLASH,
        FIRE,
        SMOKE
    }
}