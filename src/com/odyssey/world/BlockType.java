package com.odyssey.world;

/**
 * Block types enum
 */
public enum BlockType {
    AIR(0, false),
    STONE(1, true),
    GRASS(2, true),
    DIRT(3, true),
    WOOD(4, true),
    LEAVES(5, true),
    WATER(6, false),
    GLASS(7, false);
    
    public final int id;
    public final boolean solid;
    
    BlockType(int id, boolean solid) {
        this.id = id;
        this.solid = solid;
    }
} 