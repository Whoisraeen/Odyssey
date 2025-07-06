package com.odyssey.core;

/**
 * 4x4 Matrix implementation for 3D transformations
 * Column-major order matrix for OpenGL compatibility
 */
public class Matrix4f {
    
    // Matrix data in column-major order
    private final float[] m = new float[16];
    
    /**
     * Create an identity matrix
     */
    public Matrix4f() {
        identity();
    }
    
    /**
     * Create a matrix with the given values
     */
    public Matrix4f(float[] values) {
        if (values.length != 16) {
            throw new IllegalArgumentException("Matrix4f requires 16 values");
        }
        System.arraycopy(values, 0, m, 0, 16);
    }
    
    /**
     * Copy constructor
     */
    public Matrix4f(Matrix4f other) {
        System.arraycopy(other.m, 0, m, 0, 16);
    }
    
    /**
     * Set this matrix to the identity matrix
     */
    public Matrix4f identity() {
        for (int i = 0; i < 16; i++) {
            m[i] = 0.0f;
        }
        m[0] = m[5] = m[10] = m[15] = 1.0f;
        return this;
    }
    
    /**
     * Set a matrix element
     */
    public void set(int row, int col, float value) {
        m[col * 4 + row] = value;
    }
    
    /**
     * Get a matrix element
     */
    public float get(int row, int col) {
        return m[col * 4 + row];
    }
    
    /**
     * Get the raw matrix data
     */
    public float[] getData() {
        return m.clone();
    }
    
    /**
     * Create an orthographic projection matrix
     */
    public Matrix4f ortho(float left, float right, float bottom, float top, float near, float far) {
        identity();
        
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        m[15] = 1.0f;
        
        return this;
    }
    
    /**
     * Create a perspective projection matrix
     */
    public Matrix4f perspective(float fovy, float aspect, float near, float far) {
        identity();
        
        float f = 1.0f / (float) Math.tan(fovy * 0.5f);
        
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1.0f;
        m[14] = (2.0f * far * near) / (near - far);
        m[15] = 0.0f;
        
        return this;
    }
    
    /**
     * Create a translation matrix
     */
    public Matrix4f translate(float x, float y, float z) {
        Matrix4f translation = new Matrix4f();
        translation.m[12] = x;
        translation.m[13] = y;
        translation.m[14] = z;
        
        return multiply(translation);
    }
    
    /**
     * Create a scaling matrix
     */
    public Matrix4f scale(float x, float y, float z) {
        Matrix4f scaling = new Matrix4f();
        scaling.m[0] = x;
        scaling.m[5] = y;
        scaling.m[10] = z;
        
        return multiply(scaling);
    }
    
    /**
     * Create a rotation matrix around the X axis
     */
    public Matrix4f rotateX(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        Matrix4f rotation = new Matrix4f();
        rotation.m[5] = cos;
        rotation.m[6] = sin;
        rotation.m[9] = -sin;
        rotation.m[10] = cos;
        
        return multiply(rotation);
    }
    
    /**
     * Create a rotation matrix around the Y axis
     */
    public Matrix4f rotateY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        Matrix4f rotation = new Matrix4f();
        rotation.m[0] = cos;
        rotation.m[2] = -sin;
        rotation.m[8] = sin;
        rotation.m[10] = cos;
        
        return multiply(rotation);
    }
    
    /**
     * Create a rotation matrix around the Z axis
     */
    public Matrix4f rotateZ(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        
        Matrix4f rotation = new Matrix4f();
        rotation.m[0] = cos;
        rotation.m[1] = sin;
        rotation.m[4] = -sin;
        rotation.m[5] = cos;
        
        return multiply(rotation);
    }
    
    /**
     * Multiply this matrix by another matrix
     */
    public Matrix4f multiply(Matrix4f other) {
        // Validate input matrix
        if (!isValidMatrix(other)) {
            System.err.println("Warning: Invalid matrix in multiply operation");
            return this;
        }
        
        float[] result = new float[16];
        
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                result[col * 4 + row] = 
                    m[0 * 4 + row] * other.m[col * 4 + 0] +
                    m[1 * 4 + row] * other.m[col * 4 + 1] +
                    m[2 * 4 + row] * other.m[col * 4 + 2] +
                    m[3 * 4 + row] * other.m[col * 4 + 3];
                    
                // Validate each result element
                if (!Float.isFinite(result[col * 4 + row])) {
                    System.err.println("Warning: Matrix multiplication produced invalid value at [" + row + "][" + col + "]: " + result[col * 4 + row]);
                    return this; // Keep original matrix
                }
            }
        }
        
        System.arraycopy(result, 0, m, 0, 16);
        return this;
    }
    
    /**
     * Transform a 3D point by this matrix
     */
    public Vector3f transform(Vector3f point) {
        // Validate input point
        if (!Float.isFinite(point.x) || !Float.isFinite(point.y) || !Float.isFinite(point.z)) {
            System.err.println("Warning: Invalid point in transform operation: " + point);
            return new Vector3f(0, 0, 0);
        }
        
        float x = point.x * m[0] + point.y * m[4] + point.z * m[8] + m[12];
        float y = point.x * m[1] + point.y * m[5] + point.z * m[9] + m[13];
        float z = point.x * m[2] + point.y * m[6] + point.z * m[10] + m[14];
        float w = point.x * m[3] + point.y * m[7] + point.z * m[11] + m[15];
        
        // Validate transformation results
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(w)) {
            System.err.println("Warning: Matrix transformation produced invalid values");
            return new Vector3f(0, 0, 0);
        }
        
        if (w != 0.0f && Float.isFinite(w)) {
            x /= w;
            y /= w;
            z /= w;
            
            // Validate division results
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                System.err.println("Warning: Perspective division produced invalid values");
                return new Vector3f(0, 0, 0);
            }
        }
        
        return new Vector3f(x, y, z);
    }
    
    /**
     * Calculate the determinant of this matrix
     */
    public float determinant() {
        float det = 
            m[0] * (m[5] * (m[10] * m[15] - m[11] * m[14]) - 
                   m[6] * (m[9] * m[15] - m[11] * m[13]) + 
                   m[7] * (m[9] * m[14] - m[10] * m[13])) -
            m[1] * (m[4] * (m[10] * m[15] - m[11] * m[14]) - 
                   m[6] * (m[8] * m[15] - m[11] * m[12]) + 
                   m[7] * (m[8] * m[14] - m[10] * m[12])) +
            m[2] * (m[4] * (m[9] * m[15] - m[11] * m[13]) - 
                   m[5] * (m[8] * m[15] - m[11] * m[12]) + 
                   m[7] * (m[8] * m[13] - m[9] * m[12])) -
            m[3] * (m[4] * (m[9] * m[14] - m[10] * m[13]) - 
                   m[5] * (m[8] * m[14] - m[10] * m[12]) + 
                   m[6] * (m[8] * m[13] - m[9] * m[12]));
        
        return det;
    }
    
    /**
     * Invert this matrix
     */
    public Matrix4f invert() {
        float det = determinant();
        if (!Float.isFinite(det) || Math.abs(det) < 1e-6f) {
            System.err.println("Warning: Matrix is not invertible (determinant: " + det + ")");
            return this; // Keep original matrix instead of throwing exception
        }
        
        float[] inv = new float[16];
        
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + 
                m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - 
                m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + 
                m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - 
                 m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - 
                m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + 
                m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - 
                m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + 
                 m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + 
                m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - 
                m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + 
                 m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - 
                 m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - 
                m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + 
                m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - 
                 m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + 
                 m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        
        float invDet = 1.0f / det;
        for (int i = 0; i < 16; i++) {
            float newValue = inv[i] * invDet;
            if (!Float.isFinite(newValue)) {
                System.err.println("Warning: Matrix inversion produced invalid value at index " + i + ": " + newValue);
                return this; // Keep original matrix
            }
            m[i] = newValue;
        }
        
        return this;
    }
    
    /**
     * Transpose this matrix
     */
    public Matrix4f transpose() {
        float[] temp = new float[16];
        
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                temp[row * 4 + col] = m[col * 4 + row];
            }
        }
        
        System.arraycopy(temp, 0, m, 0, 16);
        return this;
    }
    
    /**
     * Check if this matrix contains valid (finite) values
     */
    public boolean isValidMatrix() {
        for (int i = 0; i < 16; i++) {
            if (!Float.isFinite(m[i])) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if another matrix contains valid (finite) values
     */
    private boolean isValidMatrix(Matrix4f matrix) {
        if (matrix == null) return false;
        for (int i = 0; i < 16; i++) {
            if (!Float.isFinite(matrix.m[i])) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix4f:\n");
        for (int row = 0; row < 4; row++) {
            sb.append("[");
            for (int col = 0; col < 4; col++) {
                sb.append(String.format("%8.3f", m[col * 4 + row]));
                if (col < 3) sb.append(", ");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
    
    /**
     * Simple 3D vector class for matrix operations
     */
    public static class Vector3f {
        public float x, y, z;
        
        public Vector3f(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public String toString() {
            return String.format("Vector3f(%.3f, %.3f, %.3f)", x, y, z);
        }
    }
}