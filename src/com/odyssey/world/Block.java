package com.odyssey.world;

import org.joml.Vector3i;

/**
 * Utility class for block-related constants and operations.
 */
public class Block {
    
    // Face offsets for the 6 faces of a cube
    // Order: +X, -X, +Y, -Y, +Z, -Z
    public static final Vector3i[] FACE_OFFSETS = {
        new Vector3i(1, 0, 0),   // +X (right)
        new Vector3i(-1, 0, 0),  // -X (left)
        new Vector3i(0, 1, 0),   // +Y (up)
        new Vector3i(0, -1, 0),  // -Y (down)
        new Vector3i(0, 0, 1),   // +Z (forward)
        new Vector3i(0, 0, -1)   // -Z (backward)
    };
    
    /**
     * Face names corresponding to the FACE_OFFSETS array.
     */
    public static final String[] FACE_NAMES = {
        "TOP", "BOTTOM", "FRONT", "BACK", "RIGHT", "LEFT"
    };
    
    private Block() {
        // Utility class - prevent instantiation
    }
}