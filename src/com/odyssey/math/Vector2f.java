package com.odyssey.math;

public class Vector2f {
    public float x, y;

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f normalize() {
        float length = (float) Math.sqrt(x * x + y * y);
        if (length != 0) {
            x /= length;
            y /= length;
        }
        return this;
    }
} 