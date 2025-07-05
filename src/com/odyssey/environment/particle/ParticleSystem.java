package com.odyssey.environment.particle;

import com.odyssey.rendering.ShaderManager;
import com.odyssey.rendering.scene.Camera;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ParticleSystem {

    private final List<Particle> particles = new ArrayList<>();
    private final int maxParticles;
    private final int shaderProgram;
    private final int vao;
    private final int vbo;
    private final Random random = new Random();

    public ParticleSystem(int maxParticles) {
        this.maxParticles = maxParticles;
        ShaderManager shaderManager = new ShaderManager();
        this.shaderProgram = shaderManager.loadProgram("shaders/particle.vert", "shaders/particle.frag");
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        // Allocate buffer size for max particles (vec3 pos + vec3 color)
        glBufferData(GL_ARRAY_BUFFER, maxParticles * 6 * Float.BYTES, GL_DYNAMIC_DRAW);
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Color attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }
    
    public void emit(Vector3f position, Vector3f velocity, Vector3f color) {
        if (particles.size() < maxParticles) {
            particles.add(new Particle(position, velocity, color, 3f)); // 3-second life for sand/etc
        }
    }

    public void update(float deltaTime) {
        particles.removeIf(particle -> !particle.update(deltaTime));
    }

    public void render(Camera camera) {
        if (particles.isEmpty()) {
            return;
        }

        glUseProgram(shaderProgram);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"), false, camera.getViewMatrix().get(new float[16]));
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, camera.getProjectionMatrix().get(new float[16]));

        float[] data = new float[particles.size() * 6];
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            data[i * 6 + 0] = p.position.x;
            data[i * 6 + 1] = p.position.y;
            data[i * 6 + 2] = p.position.z;
            data[i * 6 + 3] = p.color.x;
            data[i * 6 + 4] = p.color.y;
            data[i * 6 + 5] = p.color.z;
        }
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        glDrawArrays(GL_POINTS, 0, particles.size());

        glDepthMask(true);
        glDisable(GL_BLEND);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}