package com.odyssey.world;

/**
 * Block types enum
 */
public enum BlockType {
    AIR(0, true, "Air"),
    DIRT(1, false, "Dirt"),
    GRASS(2, false, "Grass Block"),
    STONE(3, false, "Stone"),
    COBBLESTONE(4, false, "Cobblestone"),
    SAND(5, false, "Sand"),
    WATER(6, true, "Water"),
    WOOD(7, false, "Wood"),
    LEAVES(8, true, "Leaves"),
    HELM(9, false, "Helm"),
    SNOW_LAYER(10, true, "Snow Layer"),
    
    // Farming Blocks
    FARMLAND(11, false, "Farmland"),
    WHEAT_0(12, true, "Wheat"),
    WHEAT_1(13, true, "Wheat"),
    WHEAT_2(14, true, "Wheat"),
    WHEAT_3(15, true, "Wheat"),
    WHEAT_4(16, true, "Wheat"),
    WHEAT_5(17, true, "Wheat"),
    WHEAT_6(18, true, "Wheat"),
    WHEAT_7(19, true, "Wheat"), // Final growth stage
    
    FIRE(20, true, "Fire");
    
    private final int id;
    private final boolean isTransparent;
    private final String name;
    
    BlockType(int id, boolean isTransparent, String name) {
        this.id = id;
        this.isTransparent = isTransparent;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public boolean isTransparent() {
        return isTransparent;
    }
    
    public String getName() {
        return name;
    }

    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AIR;
    }
} 