package com.odyssey.rendering.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Simple camera for testing
 */
public class Camera {
    private Vector3f position = new Vector3f(8, 100, 8);
    private Vector3f rotation = new Vector3f(30, -45, 0); // Pitch, Yaw, Roll
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projectionMatrix = new Matrix4f();
    private float moveSpeed = 10.0f;
    private float turnSpeed = 90.0f;

    public void setProjection(int width, int height) {
        projectionMatrix.setPerspective((float) Math.toRadians(70.0f), (float)width / height, 0.1f, 1000.0f);
    }
    
    public void update(float deltaTime) {
        // Simple camera movement is handled by handleInput
        updateViewMatrix();
    }
    
    public void handleInput(int key, int action) {
        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            switch (key) {
                case GLFW_KEY_UP:
                    position.x += Math.sin(Math.toRadians(rotation.y)) * moveSpeed * 0.01f;
                    position.z -= Math.cos(Math.toRadians(rotation.y)) * moveSpeed * 0.01f;
                    break;
                case GLFW_KEY_DOWN:
                    position.x -= Math.sin(Math.toRadians(rotation.y)) * moveSpeed * 0.01f;
                    position.z += Math.cos(Math.toRadians(rotation.y)) * moveSpeed * 0.01f;
                    break;
                case GLFW_KEY_LEFT:
                    rotation.y -= turnSpeed * 0.1f;
                    break;
                case GLFW_KEY_RIGHT:
                    rotation.y += turnSpeed * 0.1f;
                    break;
                case GLFW_KEY_I:
                    rotation.x -= turnSpeed * 0.1f;
                    break;
                case GLFW_KEY_K:
                    rotation.x += turnSpeed * 0.1f;
                    break;
            }
        }
    }

    private void updateViewMatrix() {
        viewMatrix.identity()
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateY((float) Math.toRadians(rotation.y))
            .translate(-position.x, -position.y, -position.z);
    }
    
    public Vector3f getPosition() { return position; }
    public Matrix4f getViewMatrix() { return viewMatrix; }
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
} 