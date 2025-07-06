package com.odyssey.crafting;

import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.*;

public class CraftingSystem {
    private final Map<String, CraftingRecipe> recipes;
    private final Map<CraftingStationType, List<CraftingRecipe>> recipesByStation;
    
    public enum CraftingStationType {
        HAND_CRAFTING,    // 2x2 grid
        CRAFTING_TABLE,   // 3x3 grid
        FURNACE,          // Smelting
        ANVIL,            // Tool repair/enhancement
        ENCHANTING_TABLE  // Enchantments
    }
    
    public CraftingSystem() {
        this.recipes = new HashMap<>();
        this.recipesByStation = new HashMap<>();
        
        // Initialize recipe lists for each station type
        for (CraftingStationType station : CraftingStationType.values()) {
            recipesByStation.put(station, new ArrayList<>());
        }
        
        initializeRecipes();
    }
    
    private void initializeRecipes() {
        // Basic hand crafting recipes (2x2)
        addShapedRecipe("wooden_planks", CraftingStationType.HAND_CRAFTING,
            new String[]{
                "W ",
                "  "
            },
            Map.of('W', ItemType.WOOD),
            new ItemStack(ItemType.WOODEN_PLANKS, 4)
        );
        
        addShapedRecipe("sticks", CraftingStationType.HAND_CRAFTING,
            new String[]{
                "P ",
                "P "
            },
            Map.of('P', ItemType.WOODEN_PLANKS),
            new ItemStack(ItemType.STICK, 4)
        );
        
        // Crafting table recipes (3x3)
        addShapedRecipe("crafting_table", CraftingStationType.HAND_CRAFTING,
            new String[]{
                "PP",
                "PP"
            },
            Map.of('P', ItemType.WOODEN_PLANKS),
            new ItemStack(ItemType.CRAFTING_TABLE, 1)
        );
        
        addShapedRecipe("wooden_pickaxe", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "PPP",
                " S ",
                " S "
            },
            Map.of('P', ItemType.WOODEN_PLANKS, 'S', ItemType.STICK),
            new ItemStack(ItemType.WOODEN_PICKAXE, 1)
        );
        
        addShapedRecipe("wooden_axe", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "PP ",
                "PS ",
                " S "
            },
            Map.of('P', ItemType.WOODEN_PLANKS, 'S', ItemType.STICK),
            new ItemStack(ItemType.WOODEN_AXE, 1)
        );
        
        addShapedRecipe("wooden_sword", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                " P ",
                " P ",
                " S "
            },
            Map.of('P', ItemType.WOODEN_PLANKS, 'S', ItemType.STICK),
            new ItemStack(ItemType.WOODEN_SWORD, 1)
        );
        
        addShapedRecipe("stone_pickaxe", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "CCC",
                " S ",
                " S "
            },
            Map.of('C', ItemType.COBBLESTONE, 'S', ItemType.STICK),
            new ItemStack(ItemType.STONE_PICKAXE, 1)
        );
        
        addShapedRecipe("iron_pickaxe", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "III",
                " S ",
                " S "
            },
            Map.of('I', ItemType.IRON_INGOT, 'S', ItemType.STICK),
            new ItemStack(ItemType.IRON_PICKAXE, 1)
        );
        
        addShapedRecipe("furnace", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "CCC",
                "C C",
                "CCC"
            },
            Map.of('C', ItemType.COBBLESTONE),
            new ItemStack(ItemType.FURNACE, 1)
        );
        
        addShapedRecipe("chest", CraftingStationType.CRAFTING_TABLE,
            new String[]{
                "PPP",
                "P P",
                "PPP"
            },
            Map.of('P', ItemType.WOODEN_PLANKS),
            new ItemStack(ItemType.CHEST, 1)
        );
        
        // Furnace smelting recipes
        addSmeltingRecipe("coal", ItemType.COAL_ORE, ItemType.COAL, 200); // 10 seconds
        addSmeltingRecipe("iron_ingot", ItemType.IRON_ORE, ItemType.IRON_INGOT, 200); // 10 seconds
        addSmeltingRecipe("gold_ingot", ItemType.GOLD_ORE, ItemType.GOLD_INGOT, 200);
        addSmeltingRecipe("diamond", ItemType.DIAMOND_ORE, ItemType.DIAMOND, 200);
        addSmeltingRecipe("cooked_food", ItemType.RAW_FOOD, ItemType.COOKED_FOOD, 100); // 5 seconds
        addSmeltingRecipe("glass", ItemType.SAND, ItemType.GLASS, 200);
        
        // Shapeless recipes
        addShapelessRecipe("flint_and_steel", CraftingStationType.CRAFTING_TABLE,
            Arrays.asList(ItemType.IRON_INGOT, ItemType.FLINT),
            new ItemStack(ItemType.FLINT_AND_STEEL, 1)
        );
    }
    
    private void addShapedRecipe(String name, CraftingStationType station, String[] pattern, 
                                Map<Character, ItemType> ingredients, ItemStack result) {
        CraftingRecipe recipe = new ShapedRecipe(name, station, pattern, ingredients, result);
        recipes.put(name, recipe);
        recipesByStation.get(station).add(recipe);
    }
    
    private void addShapelessRecipe(String name, CraftingStationType station, 
                                   List<ItemType> ingredients, ItemStack result) {
        CraftingRecipe recipe = new ShapelessRecipe(name, station, ingredients, result);
        recipes.put(name, recipe);
        recipesByStation.get(station).add(recipe);
    }
    
    private void addSmeltingRecipe(String name, ItemType input, ItemType output, int smeltTime) {
        CraftingRecipe recipe = new SmeltingRecipe(name, input, output, smeltTime);
        recipes.put(name, recipe);
        recipesByStation.get(CraftingStationType.FURNACE).add(recipe);
    }
    
    /**
     * Attempt to craft an item using the provided crafting grid
     */
    public CraftingResult craft(CraftingStationType station, ItemStack[][] craftingGrid) {
        List<CraftingRecipe> availableRecipes = recipesByStation.get(station);
        
        for (CraftingRecipe recipe : availableRecipes) {
            if (recipe.matches(craftingGrid)) {
                return new CraftingResult(true, recipe.getResult().copy(), recipe);
            }
        }
        
        return new CraftingResult(false, null, null);
    }
    
    /**
     * Get all possible recipes for a crafting station
     */
    public List<CraftingRecipe> getRecipesForStation(CraftingStationType station) {
        return new ArrayList<>(recipesByStation.get(station));
    }
    
    /**
     * Get recipes that can be made with available items
     */
    public List<CraftingRecipe> getAvailableRecipes(CraftingStationType station, List<ItemStack> availableItems) {
        List<CraftingRecipe> available = new ArrayList<>();
        List<CraftingRecipe> stationRecipes = recipesByStation.get(station);
        
        for (CraftingRecipe recipe : stationRecipes) {
            if (recipe.canCraftWith(availableItems)) {
                available.add(recipe);
            }
        }
        
        return available;
    }
    
    /**
     * Get recipe by name
     */
    public CraftingRecipe getRecipe(String name) {
        return recipes.get(name);
    }
    
    /**
     * Check if a recipe exists
     */
    public boolean hasRecipe(String name) {
        return recipes.containsKey(name);
    }
    
    /**
     * Get all recipes
     */
    public Collection<CraftingRecipe> getAllRecipes() {
        return recipes.values();
    }
    
    /**
     * Result of a crafting attempt
     */
    public static class CraftingResult {
        private final boolean success;
        private final ItemStack result;
        private final CraftingRecipe recipe;
        
        public CraftingResult(boolean success, ItemStack result, CraftingRecipe recipe) {
            this.success = success;
            this.result = result;
            this.recipe = recipe;
        }
        
        public boolean isSuccess() { return success; }
        public ItemStack getResult() { return result; }
        public CraftingRecipe getRecipe() { return recipe; }
    }
}