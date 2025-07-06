package com.odyssey.crafting;

import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.List;

public abstract class CraftingRecipe {
    protected final String name;
    protected final CraftingSystem.CraftingStationType stationType;
    protected final ItemStack result;
    
    public CraftingRecipe(String name, CraftingSystem.CraftingStationType stationType, ItemStack result) {
        this.name = name;
        this.stationType = stationType;
        this.result = result;
    }
    
    /**
     * Check if this recipe matches the provided crafting grid
     */
    public abstract boolean matches(ItemStack[][] craftingGrid);
    
    /**
     * Check if this recipe can be crafted with the available items
     */
    public abstract boolean canCraftWith(List<ItemStack> availableItems);
    
    /**
     * Get the ingredients required for this recipe
     */
    public abstract List<ItemStack> getRequiredIngredients();
    
    /**
     * Get the recipe pattern for display (if applicable)
     */
    public abstract String[] getPattern();
    
    // Getters
    public String getName() { return name; }
    public CraftingSystem.CraftingStationType getStationType() { return stationType; }
    public ItemStack getResult() { return result; }
    
    /**
     * Helper method to check if an ItemStack matches a required type
     */
    protected boolean itemMatches(ItemStack item, ItemType requiredType) {
        return item != null && item.getType() == requiredType && item.getCount() > 0;
    }
    
    /**
     * Helper method to count available items of a specific type
     */
    protected int countAvailableItems(List<ItemStack> availableItems, ItemType itemType) {
        int count = 0;
        for (ItemStack stack : availableItems) {
            if (stack != null && stack.getType() == itemType) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Helper method to get the dimensions of a crafting grid
     */
    protected int[] getGridDimensions(ItemStack[][] grid) {
        return new int[]{grid.length, grid[0].length};
    }
    
    /**
     * Helper method to check if a crafting grid is empty
     */
    protected boolean isGridEmpty(ItemStack[][] grid) {
        for (ItemStack[] row : grid) {
            for (ItemStack item : row) {
                if (item != null && item.getCount() > 0) {
                    return false;
                }
            }
        }
        return true;
    }
}