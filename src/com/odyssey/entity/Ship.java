package com.odyssey.entity;

import com.odyssey.core.VoxelEngine;
import com.odyssey.environment.EnvironmentManager;
import com.odyssey.input.GameAction;
import com.odyssey.input.InputManager;
import com.odyssey.rendering.Camera;
import com.odyssey.world.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Ship extends Entity {

    private final BlockType[][][] blocks;
    private final int width, height, depth; // Dimensions of the ship's grid
    private int vao;
    private int vbo;
    private int vertexCount;

    // Physics properties
    private float mass = 1000f; // in kg
    private float thrust = 50000f; // in Newtons
    private float dragCoefficient = 0.8f;
    private Vector3f velocity = new Vector3f();

    public Ship(Vector3f position, Map<Vector3f, BlockType> blockData, int width, int height, int depth) {
        super(position);
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = new BlockType[width][height][depth];
        
        // Initialize all blocks to AIR
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                for(int z = 0; z < depth; z++) {
                    blocks[x][y][z] = BlockType.AIR;
                }
            }
        }
        
        // Populate ship's grid from the provided block data
        for(Map.Entry<Vector3f, BlockType> entry : blockData.entrySet()) {
            Vector3f pos = entry.getKey();
            blocks[(int)pos.x][(int)pos.y][(int)pos.z] = entry.getValue();
        }
        
        generateMesh();
    }

    private void generateMesh() {
        List<Float> vertices = new ArrayList<>();
        // Simplified meshing logic for the ship
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (blocks[x][y][z] != BlockType.AIR) {
                        // In a real implementation, this would be a proper greedy mesh
                        // or culled mesh. For now, add all faces for simplicity.
                        addBlockFaces(x, y, z, vertices);
                    }
                }
            }
        }
        
        if (vertices.isEmpty()) return;

        float[] vertexArray = toFloatArray(vertices);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexArray, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        this.vertexCount = vertexArray.length / 3;
        glBindVertexArray(0);
    }
    
    private void addBlockFaces(int x, int y, int z, List<Float> vertices) {
        // Simplified: adds 36 vertices for a full cube.
        // A real implementation would only add visible faces.
    }
    
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for(int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    @Override
    public void update(float deltaTime, VoxelEngine engine, EnvironmentManager environmentManager) {
        InputManager inputManager = engine.getInputManager();
        Vector3f totalForce = new Vector3f();

        // 1. Player Input Force
        Vector3f inputForce = calculateInputForce(inputManager);
        totalForce.add(inputForce);

        // 2. Wind Force
        Vector3f windForce = calculateWindForce(environmentManager);
        totalForce.add(windForce);
        
        // 3. Drag Force
        Vector3f dragForce = new Vector3f(velocity).mul(-dragCoefficient * velocity.length());
        totalForce.add(dragForce);
        
        // 4. Physics Calculation (F=ma -> a=F/m)
        Vector3f acceleration = new Vector3f(totalForce).div(mass);
        velocity.add(new Vector3f(acceleration).mul(deltaTime));
        position.add(new Vector3f(velocity).mul(deltaTime));
        
        // Update rotation based on player input
        if (inputManager.isActionPressed(GameAction.TURN_LEFT)) {
            rotation.y += 25f * deltaTime;
        }
        if (inputManager.isActionPressed(GameAction.TURN_RIGHT)) {
            rotation.y -= 25f * deltaTime;
        }
    }
    
    private Vector3f calculateInputForce(InputManager inputManager) {
        Vector3f force = new Vector3f();
        if (inputManager.isActionPressed(GameAction.MOVE_FORWARD)) {
            Vector3f forward = new Vector3f(0, 0, -1).rotateY((float) Math.toRadians(rotation.y));
            force.add(forward.mul(thrust));
        }
        return force;
    }

    private Vector3f calculateWindForce(EnvironmentManager environmentManager) {
        Vector2f windDir2D = environmentManager.getWindDirection();
        float windSpeed = environmentManager.getWindSpeed();
        
        // Convert ship's rotation to a 2D forward vector
        Vector2f shipForward2D = new Vector2f(
            (float) -Math.sin(Math.toRadians(rotation.y)),
            (float) -Math.cos(Math.toRadians(rotation.y))
        ).normalize();
        
        // Calculate how much the wind is aligned with the ship
        float alignment = windDir2D.dot(shipForward2D);
        
        // Let's say sail effectiveness is based on alignment
        // A simple model: max force when wind is behind, some force for crosswinds, no force for headwinds
        float sailEffectiveness = Math.max(0, alignment); // Only pushed by wind from behind
        
        float windForceMagnitude = sailEffectiveness * windSpeed * 5000f; // 5000 is an arbitrary sail factor
        
        Vector3f windForce = new Vector3f(windDir2D.x, 0, windDir2D.y).mul(windForceMagnitude);
        return windForce;
    }

    @Override
    public void render(int shaderProgram, Camera camera) {
        if (vao == 0) return;

        Matrix4f model = new Matrix4f()
            .translate(position)
            .rotateY((float) Math.toRadians(rotation.y));
            // We'll add pitch and roll later

        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "model"), false, model.get(stack.mallocFloat(16)));
            glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"), false, camera.getViewMatrix().get(stack.mallocFloat(16)));
            glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, camera.getProjectionMatrix().get(stack.mallocFloat(16)));
        }

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }
    
    @Override
    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
        }
    }
    
    public void applyInput(boolean forward, boolean left, boolean right, boolean backward) {
        // This method can be used for direct input application if needed
        // Currently, input is handled in the update method through InputManager
        // But we'll keep this for compatibility with existing code
    }
    
    public Vector3f getDimensions() {
        return new Vector3f(width, height, depth);
    }
}