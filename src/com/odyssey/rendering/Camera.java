package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Camera {
    private static final Logger logger = Logger.getLogger(Camera.class.getName());
    
    // Validation constants
    private static final float MIN_ASPECT_RATIO = 0.1f;
    private static final float MAX_ASPECT_RATIO = 10.0f;
    private static final float MIN_FOV = 1.0f;
    private static final float MAX_FOV = 179.0f;
    private static final float MIN_NEAR_PLANE = 0.001f;
    private static final float MAX_FAR_PLANE = 10000.0f;
    private static final float MIN_PITCH = -89.9f;
    private static final float MAX_PITCH = 89.9f;
    
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
    
    // Fallback values for invalid states
    private static final Vector3f FALLBACK_POSITION = new Vector3f(0.0f, 70.0f, 0.0f);
    private static final float FALLBACK_ASPECT_RATIO = 16.0f / 9.0f;
    private static final float FALLBACK_FOV = 45.0f;
    private static final float FALLBACK_NEAR_PLANE = 0.1f;
    private static final float FALLBACK_FAR_PLANE = 1000.0f;
    
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
        if (validateAspectRatio(aspectRatio)) {
            this.aspectRatio = aspectRatio;
        } else {
            logger.warning("Invalid aspect ratio: " + aspectRatio + ". Using fallback: " + FALLBACK_ASPECT_RATIO);
            this.aspectRatio = FALLBACK_ASPECT_RATIO;
        }
        updateMatrices();
    }
    
    public void setFov(float fov) {
        if (validateFov(fov)) {
            this.fov = fov;
        } else {
            logger.warning("Invalid FOV: " + fov + ". Using fallback: " + FALLBACK_FOV);
            this.fov = FALLBACK_FOV;
        }
        updateMatrices();
    }
    
    public void setNearPlane(float nearPlane) {
        if (validateNearPlane(nearPlane)) {
            this.nearPlane = nearPlane;
            updateMatrices();
        } else {
            logger.warning("Invalid near plane: " + nearPlane + ". Keeping current value: " + this.nearPlane);
        }
    }
    
    public void setFarPlane(float farPlane) {
        if (validateFarPlane(farPlane)) {
            this.farPlane = farPlane;
            updateMatrices();
        } else {
            logger.warning("Invalid far plane: " + farPlane + ". Keeping current value: " + this.farPlane);
        }
    }
    
    private void updateCameraVectors() {
        try {
            Vector3f frontTemp = new Vector3f();
            frontTemp.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
            frontTemp.y = (float) Math.sin(Math.toRadians(pitch));
            frontTemp.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
            
            // Validate calculated front vector before normalizing
            if (!validateFloat(frontTemp.x) || !validateFloat(frontTemp.y) || !validateFloat(frontTemp.z)) {
                logger.severe("Invalid front vector calculated: " + frontTemp + " (yaw=" + yaw + ", pitch=" + pitch + ")");
                // Use fallback front vector
                frontTemp.set(0.0f, 0.0f, -1.0f);
            }
            
            this.front = frontTemp.normalize();
            
            // Validate front vector after normalization
            if (!validateVector3f(this.front)) {
                logger.severe("Front vector became invalid after normalization: " + this.front);
                this.front = new Vector3f(0.0f, 0.0f, -1.0f);
            }
            
            this.right = new Vector3f(front).cross(worldUp).normalize();
            
            // Validate right vector
            if (!validateVector3f(this.right)) {
                logger.severe("Right vector became invalid: " + this.right);
                this.right = new Vector3f(1.0f, 0.0f, 0.0f);
            }
            
            this.up = new Vector3f(right).cross(front).normalize();
            
            // Validate up vector
            if (!validateVector3f(this.up)) {
                logger.severe("Up vector became invalid: " + this.up);
                this.up = new Vector3f(0.0f, 1.0f, 0.0f);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception in updateCameraVectors: " + e.getMessage(), e);
            // Set fallback vectors
            this.front = new Vector3f(0.0f, 0.0f, -1.0f);
            this.right = new Vector3f(1.0f, 0.0f, 0.0f);
            this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        }
    }
    
    private void updateMatrices() {
        try {
            // Validate all parameters before matrix updates
            if (!validateCameraState()) {
                logger.severe("Camera state validation failed. Using fallback values.");
                resetToFallbackState();
            }
            
            // Update view matrix with validation
            Vector3f target = new Vector3f(position).add(front);
            if (validateVector3f(position) && validateVector3f(target) && validateVector3f(up)) {
                viewMatrix.identity().lookAt(position, target, up);
                
                // Validate resulting view matrix
                if (!validateMatrix4f(viewMatrix)) {
                    logger.severe("View matrix contains invalid values. Resetting camera.");
                    resetToFallbackState();
                    viewMatrix.identity().lookAt(position, new Vector3f(position).add(front), up);
                }
            } else {
                logger.severe("Invalid camera vectors detected. Resetting camera.");
                resetToFallbackState();
                viewMatrix.identity().lookAt(position, new Vector3f(position).add(front), up);
            }
            
            // Update projection matrix with validation
            float fovRadians = (float) Math.toRadians(fov);
            if (validateProjectionParameters(fovRadians, aspectRatio, nearPlane, farPlane)) {
                projectionMatrix.identity().perspective(fovRadians, aspectRatio, nearPlane, farPlane);
                
                // Validate resulting projection matrix
                if (!validateMatrix4f(projectionMatrix)) {
                    logger.severe("Projection matrix contains invalid values. Using fallback parameters.");
                    projectionMatrix.identity().perspective(
                        (float) Math.toRadians(FALLBACK_FOV),
                        FALLBACK_ASPECT_RATIO,
                        nearPlane,
                        farPlane
                    );
                }
            } else {
                logger.severe("Invalid projection parameters. Using fallback values.");
                projectionMatrix.identity().perspective(
                    (float) Math.toRadians(FALLBACK_FOV),
                    FALLBACK_ASPECT_RATIO,
                    nearPlane,
                    farPlane
                );
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during matrix update: " + e.getMessage(), e);
            resetToFallbackState();
            // Attempt safe matrix update
            viewMatrix.identity().lookAt(position, new Vector3f(position).add(front), up);
            projectionMatrix.identity().perspective(
                (float) Math.toRadians(FALLBACK_FOV),
                FALLBACK_ASPECT_RATIO,
                nearPlane,
                farPlane
            );
        }
    }
    
    // Validation methods
    private boolean validateCameraState() {
        boolean positionValid = validateVector3f(position);
        boolean frontValid = validateVector3f(front);
        boolean upValid = validateVector3f(up);
        boolean rightValid = validateVector3f(right);
        boolean yawValid = validateFloat(yaw);
        boolean pitchValid = validateFloat(pitch);
        boolean aspectRatioValid = validateAspectRatio(aspectRatio);
        boolean fovValid = validateFov(fov);
        boolean nearPlaneValid = validateNearPlane(nearPlane);
        boolean farPlaneValid = validateFarPlane(farPlane);
        
        boolean allValid = positionValid && frontValid && upValid && rightValid &&
                          yawValid && pitchValid && aspectRatioValid && fovValid &&
                          nearPlaneValid && farPlaneValid;
        
        if (!allValid) {
            logger.severe("Camera validation details:");
            logger.severe("  Position valid: " + positionValid + " (" + position + ")");
            logger.severe("  Front valid: " + frontValid + " (" + front + ")");
            logger.severe("  Up valid: " + upValid + " (" + up + ")");
            logger.severe("  Right valid: " + rightValid + " (" + right + ")");
            logger.severe("  Yaw valid: " + yawValid + " (" + yaw + ")");
            logger.severe("  Pitch valid: " + pitchValid + " (" + pitch + ")");
            logger.severe("  Aspect ratio valid: " + aspectRatioValid + " (" + aspectRatio + ")");
            logger.severe("  FOV valid: " + fovValid + " (" + fov + ")");
            logger.severe("  Near plane valid: " + nearPlaneValid + " (" + nearPlane + ")");
            logger.severe("  Far plane valid: " + farPlaneValid + " (" + farPlane + ")");
        }
        
        return allValid;
    }
    
    private boolean validateVector3f(Vector3f vector) {
        return vector != null && 
               validateFloat(vector.x) && validateFloat(vector.y) && validateFloat(vector.z) &&
               vector.lengthSquared() > 0.0001f; // Prevent zero-length vectors
    }
    
    private boolean validateFloat(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
    
    private boolean validateMatrix4f(Matrix4f matrix) {
        if (matrix == null) return false;
        
        // Check all matrix elements for NaN/infinity
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float value = matrix.get(i, j);
                if (!validateFloat(value)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean validateAspectRatio(float aspectRatio) {
        return validateFloat(aspectRatio) && 
               aspectRatio >= MIN_ASPECT_RATIO && aspectRatio <= MAX_ASPECT_RATIO;
    }
    
    private boolean validateFov(float fov) {
        return validateFloat(fov) && fov >= MIN_FOV && fov <= MAX_FOV;
    }
    
    private boolean validateNearPlane(float nearPlane) {
        return validateFloat(nearPlane) && nearPlane >= MIN_NEAR_PLANE && nearPlane < farPlane;
    }
    
    private boolean validateFarPlane(float farPlane) {
        return validateFloat(farPlane) && farPlane <= MAX_FAR_PLANE && farPlane > nearPlane;
    }
    
    private boolean validateProjectionParameters(float fovRadians, float aspectRatio, float nearPlane, float farPlane) {
        return validateFloat(fovRadians) && validateAspectRatio(aspectRatio) &&
               validateNearPlane(nearPlane) && validateFarPlane(farPlane) &&
               (farPlane - nearPlane) > 0.001f; // Ensure reasonable depth range
    }
    
    private void resetToFallbackState() {
        this.position.set(FALLBACK_POSITION);
        this.worldUp.set(0.0f, 1.0f, 0.0f);
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        this.fov = FALLBACK_FOV;
        this.aspectRatio = FALLBACK_ASPECT_RATIO;
        this.nearPlane = FALLBACK_NEAR_PLANE;
        this.farPlane = FALLBACK_FAR_PLANE;
        updateCameraVectors();
    }
    
    // Getter methods for validation parameters
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getFov() { return fov; }
    public float getAspectRatio() { return aspectRatio; }
    public float getNearPlane() { return nearPlane; }
    public float getFarPlane() { return farPlane; }
    
    // Matrix validation status methods
    public boolean isViewMatrixValid() {
        return validateMatrix4f(viewMatrix);
    }
    
    public boolean isProjectionMatrixValid() {
        return validateMatrix4f(projectionMatrix);
    }
}