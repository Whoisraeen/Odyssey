package com.odyssey.rendering.scene;

import com.odyssey.rendering.mesh.Mesh;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class RenderObject {
    private Mesh mesh;
    private Material material;
    private Matrix4f modelMatrix;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private boolean visible;
    private boolean transparent;
    private float distanceFromCamera;
    
    public RenderObject(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f(0.0f);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1.0f);
        this.visible = true;
        this.transparent = false;
        this.distanceFromCamera = 0.0f;
        
        updateModelMatrix();
    }
    
    public RenderObject(Mesh mesh) {
        this(mesh, new Material());
    }
    
    public void setPosition(Vector3f position) {
        this.position.set(position);
        updateModelMatrix();
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        updateModelMatrix();
    }
    
    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        updateModelMatrix();
    }
    
    public void setRotation(float x, float y, float z) {
        this.rotation.rotationXYZ(x, y, z);
        updateModelMatrix();
    }
    
    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        updateModelMatrix();
    }
    
    public void setScale(float scale) {
        this.scale.set(scale);
        updateModelMatrix();
    }
    
    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        updateModelMatrix();
    }
    
    private void updateModelMatrix() {
        // Validate transformation components for NaN/infinity values
        if (!isValidVector3f(position)) {
            System.err.println("Warning: Invalid position in RenderObject: " + position);
            position.set(0.0f, 0.0f, 0.0f); // Reset to origin
        }
        if (!isValidQuaternion(rotation)) {
            System.err.println("Warning: Invalid rotation in RenderObject: " + rotation);
            rotation.identity(); // Reset to identity rotation
        }
        if (!isValidVector3f(scale)) {
            System.err.println("Warning: Invalid scale in RenderObject: " + scale);
            scale.set(1.0f, 1.0f, 1.0f); // Reset to unit scale
        }
        
        modelMatrix.identity()
            .translate(position)
            .rotate(rotation)
            .scale(scale);
        
        // Validate resulting model matrix
        if (!isValidMatrix4f(modelMatrix)) {
            System.err.println("Warning: Invalid model matrix generated in RenderObject");
            // Reset to identity matrix as fallback
            modelMatrix.identity();
        }
    }
    
    public void translate(Vector3f translation) {
        this.position.add(translation);
        updateModelMatrix();
    }
    
    public void rotate(float angle, Vector3f axis) {
        Quaternionf deltaRotation = new Quaternionf().rotateAxis(angle, axis);
        this.rotation.mul(deltaRotation);
        updateModelMatrix();
    }
    
    public void scaleBy(float factor) {
        this.scale.mul(factor);
        updateModelMatrix();
    }
    
    // Getters
    public Material getMaterial() {
        return material;
    }
    
    public Mesh getMesh() {
        return mesh;
    }
    
    public Matrix4f getModelMatrix() {
        return new Matrix4f(modelMatrix);
    }
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }
    
    public Vector3f getScale() {
        return new Vector3f(scale);
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public boolean isTransparent() {
        return transparent;
    }
    
    public float getDistanceFromCamera() {
        return distanceFromCamera;
    }
    
    // Setters
    public void setMaterial(Material material) {
        this.material = material;
    }
    
    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }
    
    public void setDistanceFromCamera(float distance) {
        this.distanceFromCamera = distance;
    }
    
    public void updateDistanceFromCamera(Vector3f cameraPosition) {
        this.distanceFromCamera = position.distance(cameraPosition);
    }
    
    // Utility methods
    public boolean isInFrustum(Matrix4f viewProjectionMatrix) {
        // Simple bounding sphere frustum culling
        // This is a simplified implementation - in practice you'd want proper AABB or OBB culling
        Vector3f center = new Vector3f(position);
        float radius = Math.max(Math.max(scale.x, scale.y), scale.z) * 2.0f; // Rough estimate
        
        // Transform center to clip space
        Vector3f clipSpace = new Vector3f();
        viewProjectionMatrix.transformPosition(center, clipSpace);
        
        // Check if sphere is within frustum bounds
        return Math.abs(clipSpace.x) <= 1.0f + radius &&
               Math.abs(clipSpace.y) <= 1.0f + radius &&
               clipSpace.z >= -1.0f - radius && clipSpace.z <= 1.0f + radius;
    }
    
    public void cleanup() {
        // Mesh cleanup is handled elsewhere
        // No cleanup needed for this object
    }
    
    // Validation helper methods
    private boolean isValidVector3f(Vector3f vector) {
        return Float.isFinite(vector.x) && Float.isFinite(vector.y) && Float.isFinite(vector.z);
    }
    
    private boolean isValidQuaternion(Quaternionf quaternion) {
        return Float.isFinite(quaternion.x) && Float.isFinite(quaternion.y) && 
               Float.isFinite(quaternion.z) && Float.isFinite(quaternion.w);
    }
    
    private boolean isValidMatrix4f(Matrix4f matrix) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (!Float.isFinite(matrix.get(i, j))) {
                    return false;
                }
            }
        }
        return true;
    }
}