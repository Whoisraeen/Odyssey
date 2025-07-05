package com.odyssey.rendering.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Simple camera for testing
 */
public class Camera {
    private Vector3f position = new Vector3f(0, 100, 0);
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projectionMatrix = new Matrix4f();
    
    public void update(float deltaTime) {
        // Simple camera movement would go here
        updateViewMatrix();
    }
    
    private void updateViewMatrix() {
        viewMatrix.identity()
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateY((float) Math.toRadians(rotation.y))
            .rotateZ((float) Math.toRadians(rotation.z))
            .translate(-position.x, -position.y, -position.z);
    }
    
    public Vector3f getPosition() { return position; }
    public Matrix4f getViewMatrix() { return viewMatrix; }
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
} 