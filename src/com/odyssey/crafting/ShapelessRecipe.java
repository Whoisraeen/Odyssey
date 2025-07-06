package com.odyssey.crafting;

import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.*;

public class ShapelessRecipe extends CraftingRecipe {
    private final List<ItemType> ingredients;
    
    public ShapelessRecipe(String name, CraftingSystem.CraftingStationType stationType, 
                          List<ItemType> ingredients, ItemStack result) {
        super(name, stationType, result);
        this.ingredients = new ArrayList<>(ingredients);
    }
    
    @Override
    public boolean matches(ItemStack[][] craftingGrid) {
        // Collect all non-empty items from the grid
        List<ItemType> gridItems = new ArrayList<>();
        for (ItemStack[] row : craftingGrid) {
            for (ItemStack item : row) {
                if (item != null && item.getCount() > 0) {
                    gridItems.add(item.getType());
                }
            }
        }
        
        // Check if grid items match required ingredients (order doesn't matter)
        if (gridItems.size() != ingredients.size()) {
            return false;
        }
        
        // Count occurrences of each item type in both lists
        Map<ItemType, Integer> requiredCounts = getItemCounts(ingredients);
        Map<ItemType, Integer> gridCounts = getItemCounts(gridItems);
        
        return requiredCounts.equals(gridCounts);
    }
    
    @Override
    public boolean canCraftWith(List<ItemStack> availableItems) {
        Map<ItemType, Integer> required = getItemCounts(ingredients);
        
        for (Map.Entry<ItemType, Integer> entry : required.entrySet()) {
            ItemType itemType = entry.getKey();
            int requiredCount = entry.getValue();
            int availableCount = countAvailableItems(availableItems, itemType);
            
            if (availableCount < requiredCount) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<ItemStack> getRequiredIngredients() {
        Map<ItemType, Integer> itemCounts = getItemCounts(ingredients);
        List<ItemStack> result = new ArrayList<>();
        
        for (Map.Entry<ItemType, Integer> entry : itemCounts.entrySet()) {
            result.add(new ItemStack(entry.getKey(), entry.getValue()));
        }
        
        return result;
    }
    
    @Override
    public String[] getPattern() {
        // Shapeless recipes don't have a specific pattern
        // Return a representation showing the ingredients
        List<String> patternList = new ArrayList<>();
        Map<ItemType, Integer> counts = getItemCounts(ingredients);
        
        for (Map.Entry<ItemType, Integer> entry : counts.entrySet()) {
            String itemName = entry.getKey().name();
            int count = entry.getValue();
            if (count > 1) {
                patternList.add(count + "x " + itemName);
            } else {
                patternList.add(itemName);
            }
        }
        
        return patternList.toArray(new String[0]);
    }
    
    private Map<ItemType, Integer> getItemCounts(List<ItemType> items) {
        Map<ItemType, Integer> counts = new HashMap<>();
        for (ItemType item : items) {
            counts.put(item, counts.getOrDefault(item, 0) + 1);
        }
        return counts;
    }
    
    /**
     * Get the list of ingredients for this recipe
     */
    public List<ItemType> getIngredients() {
        return new ArrayList<>(ingredients);
    }
}