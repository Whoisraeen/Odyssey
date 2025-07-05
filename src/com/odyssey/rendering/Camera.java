package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;
    private Vector3f worldUp;
    
    private float yaw;
    private float pitch;
    
    private float fov;
    private float aspectRatio;
    private float nearPlane;
    private float farPlane;
    
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    
    public Camera() {
        this.position = new Vector3f(0.0f, 70.0f, 0.0f);
        this.worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        this.fov = 45.0f;
        this.aspectRatio = 1920.0f / 1080.0f;
        this.nearPlane = 0.1f;
        this.farPlane = 1000.0f;
        
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        updateCameraVectors();
        updateMatrices();
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        updateMatrices();
    }
    
    public void setPosition(Vector3f position) {
        this.position.set(position);
        updateMatrices();
    }
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Vector3f getFront() {
        return new Vector3f(front);
    }
    
    public Vector3f getRight() {
        return new Vector3f(right);
    }
    
    public Vector3f getUp() {
        return new Vector3f(up);
    }
    
    public void setYaw(float yaw) {
        this.yaw = yaw;
        updateCameraVectors();
        updateMatrices();
    }
    
    public void setPitch(float pitch) {
        this.pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        updateCameraVectors();
        updateMatrices();
    }
    
    public void rotate(float deltaYaw, float deltaPitch) {
        this.yaw += deltaYaw;
        this.pitch += deltaPitch;
        this.pitch = Math.max(-89.0f, Math.min(89.0f, this.pitch));
        updateCameraVectors();
        updateMatrices();
    }
    
    public void move(Vector3f direction, float distance) {
        position.add(new Vector3f(direction).mul(distance));
        updateMatrices();
    }
    
    public void moveForward(float distance) {
        position.add(new Vector3f(front).mul(distance));
        updateMatrices();
    }
    
    public void moveBackward(float distance) {
        position.sub(new Vector3f(front).mul(distance));
        updateMatrices();
    }
    
    public void moveLeft(float distance) {
        position.sub(new Vector3f(right).mul(distance));
        updateMatrices();
    }
    
    public void moveRight(float distance) {
        position.add(new Vector3f(right).mul(distance));
        updateMatrices();
    }
    
    public void moveUp(float distance) {
        position.add(new Vector3f(up).mul(distance));
        updateMatrices();
    }
    
    public void moveDown(float distance) {
        position.sub(new Vector3f(up).mul(distance));
        updateMatrices();
    }
    
    public Matrix4f getViewMatrix() {
        return new Matrix4f(viewMatrix);
    }
    
    public Matrix4f getProjectionMatrix() {
        return new Matrix4f(projectionMatrix);
    }
    
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateMatrices();
    }
    
    public void setFov(float fov) {
        this.fov = fov;
        updateMatrices();
    }
    
    private void updateCameraVectors() {
        Vector3f frontTemp = new Vector3f();
        frontTemp.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        frontTemp.y = (float) Math.sin(Math.toRadians(pitch));
        frontTemp.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        
        this.front = frontTemp.normalize();
        this.right = new Vector3f(front).cross(worldUp).normalize();
        this.up = new Vector3f(right).cross(front).normalize();
    }
    
    private void updateMatrices() {
        // Update view matrix
        viewMatrix.identity().lookAt(position, new Vector3f(position).add(front), up);
        
        // Update projection matrix
        projectionMatrix.identity().perspective(
            (float) Math.toRadians(fov),
            aspectRatio,
            nearPlane,
            farPlane
        );
    }
}