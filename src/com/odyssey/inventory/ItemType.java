package com.odyssey.inventory;

public enum ItemType {
    // Basic materials
    WOOD,
    STONE,
    COBBLESTONE,
    DIRT,
    GRASS_BLOCK,
    SAND,
    GRAVEL,
    CLAY,
    LEAVES,
    
    // Processed materials
    WOODEN_PLANKS,
    STICK,
    GLASS,
    BRICK,
    
    // Ores and ingots
    COAL_ORE,
    IRON_ORE,
    GOLD_ORE,
    DIAMOND_ORE,
    COAL,
    IRON_INGOT,
    GOLD_INGOT,
    DIAMOND,
    
    // Tools - Wooden
    WOODEN_PICKAXE,
    WOODEN_AXE,
    WOODEN_SHOVEL,
    WOODEN_HOE,
    WOODEN_SWORD,
    
    // Tools - Stone
    STONE_PICKAXE,
    STONE_AXE,
    STONE_SHOVEL,
    STONE_HOE,
    STONE_SWORD,
    
    // Tools - Iron
    IRON_PICKAXE,
    IRON_AXE,
    IRON_SHOVEL,
    IRON_HOE,
    IRON_SWORD,
    
    // Tools - Gold
    GOLD_PICKAXE,
    GOLD_AXE,
    GOLD_SHOVEL,
    GOLD_HOE,
    GOLD_SWORD,
    
    // Tools - Diamond
    DIAMOND_PICKAXE,
    DIAMOND_AXE,
    DIAMOND_SHOVEL,
    DIAMOND_HOE,
    DIAMOND_SWORD,
    
    // Armor - Leather
    LEATHER_HELMET,
    LEATHER_CHESTPLATE,
    LEATHER_LEGGINGS,
    LEATHER_BOOTS,
    
    // Armor - Iron
    IRON_HELMET,
    IRON_CHESTPLATE,
    IRON_LEGGINGS,
    IRON_BOOTS,
    
    // Armor - Gold
    GOLD_HELMET,
    GOLD_CHESTPLATE,
    GOLD_LEGGINGS,
    GOLD_BOOTS,
    
    // Armor - Diamond
    DIAMOND_HELMET,
    DIAMOND_CHESTPLATE,
    DIAMOND_LEGGINGS,
    DIAMOND_BOOTS,
    
    // Food
    APPLE,
    BREAD,
    RAW_FOOD,
    COOKED_FOOD,
    WHEAT,
    SEEDS,
    
    // Utility items
    FLINT,
    FLINT_AND_STEEL,
    BOW,
    ARROW,
    FISHING_ROD,
    BUCKET,
    WATER_BUCKET,
    LAVA_BUCKET,
    
    // Crafting stations and storage
    CRAFTING_TABLE,
    FURNACE,
    CHEST,
    ANVIL,
    ENCHANTING_TABLE,
    
    // Redstone and mechanisms
    REDSTONE,
    REDSTONE_TORCH,
    LEVER,
    BUTTON,
    PRESSURE_PLATE,
    
    // Decorative blocks
    TORCH,
    LADDER,
    FENCE,
    DOOR,
    SIGN,
    
    // Miscellaneous
    BOOK,
    PAPER,
    LEATHER,
    STRING,
    FEATHER,
    GUNPOWDER,
    BONE,
    
    // Special/placeholder
    AIR,
    UNKNOWN;
    
    /**
     * Check if this item type represents a tool
     */
    public boolean isTool() {
        return name().contains("PICKAXE") || name().contains("AXE") || 
               name().contains("SHOVEL") || name().contains("HOE") || 
               name().contains("SWORD");
    }
    
    /**
     * Check if this item type represents armor
     */
    public boolean isArmor() {
        return name().contains("HELMET") || name().contains("CHESTPLATE") || 
               name().contains("LEGGINGS") || name().contains("BOOTS");
    }
    
    /**
     * Check if this item type represents food
     */
    public boolean isFood() {
        return this == APPLE || this == BREAD || this == RAW_FOOD || 
               this == COOKED_FOOD || this == WHEAT;
    }
    
    /**
     * Check if this item type can be placed as a block
     */
    public boolean isPlaceable() {
        return this == WOOD || this == STONE || this == COBBLESTONE || 
               this == DIRT || this == GRASS_BLOCK || this == SAND || 
               this == GRAVEL || this == LEAVES || this == GLASS ||
               this == CRAFTING_TABLE || this == FURNACE || this == CHEST ||
               this == TORCH || this == LADDER;
    }
    
    /**
     * Get the maximum stack size for this item type
     */
    public int getMaxStackSize() {
        if (isTool() || isArmor()) {
            return 1; // Tools and armor don't stack
        }
        
        switch (this) {
            case BUCKET:
            case WATER_BUCKET:
            case LAVA_BUCKET:
            case FLINT_AND_STEEL:
            case BOW:
            case FISHING_ROD:
                return 1;
            
            case ARROW:
                return 64;
            
            default:
                return 64; // Default stack size
        }
    }
    
    /**
     * Get the durability for tools and armor (0 means no durability)
     */
    public int getMaxDurability() {
        if (name().startsWith("WOODEN_")) {
            return 60;
        } else if (name().startsWith("STONE_")) {
            return 132;
        } else if (name().startsWith("IRON_")) {
            return 251;
        } else if (name().startsWith("GOLD_")) {
            return 33;
        } else if (name().startsWith("DIAMOND_")) {
            return 1562;
        }
        
        // Special cases
        switch (this) {
            case BOW:
                return 385;
            case FISHING_ROD:
                return 65;
            case FLINT_AND_STEEL:
                return 65;
            default:
                return 0; // No durability
        }
    }
}