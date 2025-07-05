package com.odyssey.rendering.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Simple camera for testing
 */
public class Camera {
    private Vector3f position;
    private Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private Vector3f right;
    private Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

    // Camera options
    private float yaw;
    private float pitch;
    private static final float SENSITIVITY = 0.1f;

    private double lastX = 400, lastY = 300;
    private boolean firstMouse = true;
    
    // Projection parameters
    private float fov = 45.0f;
    private float aspectRatio = 1280.0f / 720.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;
    
    public Camera() {
        position = new Vector3f(0.0f, 0.0f, 3.0f);
        updateCameraVectors();
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }
    
    public void handleMouseInput(long window) {
        double xpos, ypos;
        DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xBuffer, yBuffer);
        xpos = xBuffer.get(0);
        ypos = yBuffer.get(0);

        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
        }

        float xoffset = (float)(xpos - lastX);
        float yoffset = (float)(lastY - ypos);
        lastX = xpos;
        lastY = ypos;

        xoffset *= SENSITIVITY;
        yoffset *= SENSITIVITY;

        yaw += xoffset;
        pitch += yoffset;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        updateCameraVectors();
    }

    private void updateCameraVectors() {
        // Calculate the new Front vector
        Vector3f newFront = new Vector3f();
        newFront.x = (float)(Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        newFront.y = (float)(Math.sin(Math.toRadians(pitch)));
        newFront.z = (float)(Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front = newFront.normalize();
        // Also re-calculate the Right and Up vector
        right = new Vector3f(front).cross(worldUp).normalize();
        up = new Vector3f(right).cross(front).normalize();
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
    
    public Vector3f getFront() {
        return front;
    }
    
    public Vector3f getRight() {
        return right;
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective((float)Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
    }
    
    public void setProjection(int width, int height) {
        this.aspectRatio = (float) width / (float) height;
    }
    
    public void setProjection(float fov, float aspectRatio, float nearPlane, float farPlane) {
        this.fov = fov;
        this.aspectRatio = aspectRatio;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }
}