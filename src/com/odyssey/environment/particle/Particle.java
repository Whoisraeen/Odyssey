package com.odyssey.environment.particle;

import org.joml.Vector3f;

public class Particle {
    public Vector3f position;
    public Vector3f velocity;
    public Vector3f color;
    public float life;

    public Particle(Vector3f position, Vector3f velocity, Vector3f color, float life) {
        this.position = new Vector3f(position);
        this.velocity = new Vector3f(velocity);
        this.color = new Vector3f(color);
        this.life = life;
    }

    public boolean update(float deltaTime) {
        position.add(new Vector3f(velocity).mul(deltaTime));
        life -= deltaTime;
        return life > 0;
    }
} 