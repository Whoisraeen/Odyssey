package com.odyssey.world;

/**
 * Block types enum with texture atlas support
 */
public enum BlockType {
    // Basic blocks with texture atlas coordinates (16x16 atlas)
    AIR(0, true, "Air", 0, 0, 0, 0, 0, 0),
    DIRT(1, false, "Dirt", 2, 2, 2, 2, 2, 2),
    GRASS(2, false, "Grass Block", 0, 2, 3, 3, 3, 3), // top, bottom, sides
    STONE(3, false, "Stone", 1, 1, 1, 1, 1, 1),
    COBBLESTONE(4, false, "Cobblestone", 16, 16, 16, 16, 16, 16),
    SAND(5, false, "Sand", 18, 18, 18, 18, 18, 18),
    WATER(6, true, "Water", 205, 205, 205, 205, 205, 205),
    WOOD(7, false, "Wood", 21, 21, 20, 20, 20, 20), // top/bottom, sides
    LEAVES(8, true, "Leaves", 52, 52, 52, 52, 52, 52),
    HELM(9, false, "Helm", 1, 1, 1, 1, 1, 1),
    SNOW_LAYER(10, true, "Snow Layer", 66, 66, 66, 66, 66, 66),
    
    // Farming Blocks
    FARMLAND(11, false, "Farmland", 87, 2, 87, 87, 87, 87),
    WHEAT_0(12, true, "Wheat", 88, 88, 88, 88, 88, 88),
    WHEAT_1(13, true, "Wheat", 89, 89, 89, 89, 89, 89),
    WHEAT_2(14, true, "Wheat", 90, 90, 90, 90, 90, 90),
    WHEAT_3(15, true, "Wheat", 91, 91, 91, 91, 91, 91),
    WHEAT_4(16, true, "Wheat", 92, 92, 92, 92, 92, 92),
    WHEAT_5(17, true, "Wheat", 93, 93, 93, 93, 93, 93),
    WHEAT_6(18, true, "Wheat", 94, 94, 94, 94, 94, 94),
    WHEAT_7(19, true, "Wheat", 95, 95, 95, 95, 95, 95), // Final growth stage
    
    FIRE(20, true, "Fire", 31, 31, 31, 31, 31, 31),
    
    // Ore blocks
    COAL_ORE(21, false, "Coal Ore", 32, 32, 32, 32, 32, 32),
    IRON_ORE(22, false, "Iron Ore", 33, 33, 33, 33, 33, 33),
    GOLD_ORE(23, false, "Gold Ore", 34, 34, 34, 34, 34, 34),
    DIAMOND_ORE(24, false, "Diamond Ore", 35, 35, 35, 35, 35, 35),
    
    // Additional blocks
    GLASS(25, true, "Glass", 49, 49, 49, 49, 49, 49),
    LAVA(26, true, "Lava", 237, 237, 237, 237, 237, 237);
    
    private final int id;
    private final boolean isTransparent;
    private final String name;
    
    // Texture atlas indices for each face (16x16 atlas = 256 textures)
    private final int topTexture;
    private final int bottomTexture;
    private final int frontTexture;
    private final int backTexture;
    private final int leftTexture;
    private final int rightTexture;
    
    // Atlas configuration
    public static final int ATLAS_SIZE = 16; // 16x16 texture atlas
    public static final float TEXTURE_SIZE = 1.0f / ATLAS_SIZE; // Size of each texture in UV space
    
    BlockType(int id, boolean isTransparent, String name, 
              int topTexture, int bottomTexture, int frontTexture, 
              int backTexture, int leftTexture, int rightTexture) {
        this.id = id;
        this.isTransparent = isTransparent;
        this.name = name;
        this.topTexture = topTexture;
        this.bottomTexture = bottomTexture;
        this.frontTexture = frontTexture;
        this.backTexture = backTexture;
        this.leftTexture = leftTexture;
        this.rightTexture = rightTexture;
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
    
    /**
     * Get texture index for a specific face
     */
    public int getTextureIndex(String face) {
        switch (face.toUpperCase()) {
            case "TOP": return topTexture;
            case "BOTTOM": return bottomTexture;
            case "FRONT": return frontTexture;
            case "BACK": return backTexture;
            case "LEFT": return leftTexture;
            case "RIGHT": return rightTexture;
            default: return topTexture;
        }
    }
    
    /**
     * Convert texture index to UV coordinates
     */
    public float[] getTextureUV(String face) {
        int textureIndex = getTextureIndex(face);
        int x = textureIndex % ATLAS_SIZE;
        int y = textureIndex / ATLAS_SIZE;
        
        float u = x * TEXTURE_SIZE;
        float v = y * TEXTURE_SIZE;
        
        return new float[] { u, v, u + TEXTURE_SIZE, v + TEXTURE_SIZE };
    }
    
    // Getter methods
    public int getTopTexture() { return topTexture; }
    public int getBottomTexture() { return bottomTexture; }
    public int getFrontTexture() { return frontTexture; }
    public int getBackTexture() { return backTexture; }
    public int getLeftTexture() { return leftTexture; }
    public int getRightTexture() { return rightTexture; }
}