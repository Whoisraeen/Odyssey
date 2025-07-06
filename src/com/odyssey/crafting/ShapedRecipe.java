package com.odyssey.crafting;

import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.*;

public class ShapedRecipe extends CraftingRecipe {
    private final String[] pattern;
    private final Map<Character, ItemType> ingredients;
    private final int width;
    private final int height;
    
    public ShapedRecipe(String name, CraftingSystem.CraftingStationType stationType, 
                       String[] pattern, Map<Character, ItemType> ingredients, ItemStack result) {
        super(name, stationType, result);
        this.pattern = pattern.clone();
        this.ingredients = new HashMap<>(ingredients);
        this.height = pattern.length;
        this.width = pattern.length > 0 ? pattern[0].length() : 0;
    }
    
    @Override
    public boolean matches(ItemStack[][] craftingGrid) {
        int gridHeight = craftingGrid.length;
        int gridWidth = craftingGrid[0].length;
        
        // Try all possible positions in the grid
        for (int startRow = 0; startRow <= gridHeight - height; startRow++) {
            for (int startCol = 0; startCol <= gridWidth - width; startCol++) {
                if (matchesAt(craftingGrid, startRow, startCol)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean matchesAt(ItemStack[][] craftingGrid, int startRow, int startCol) {
        // Check if the pattern matches at the specified position
        for (int row = 0; row < craftingGrid.length; row++) {
            for (int col = 0; col < craftingGrid[0].length; col++) {
                ItemStack gridItem = craftingGrid[row][col];
                
                // Calculate pattern position
                int patternRow = row - startRow;
                int patternCol = col - startCol;
                
                if (patternRow >= 0 && patternRow < height && patternCol >= 0 && patternCol < width) {
                    // We're inside the pattern area
                    char patternChar = pattern[patternRow].charAt(patternCol);
                    
                    if (patternChar == ' ') {
                        // Pattern expects empty space
                        if (gridItem != null && gridItem.getCount() > 0) {
                            return false;
                        }
                    } else {
                        // Pattern expects specific item
                        ItemType expectedType = ingredients.get(patternChar);
                        if (expectedType == null || !itemMatches(gridItem, expectedType)) {
                            return false;
                        }
                    }
                } else {
                    // We're outside the pattern area - should be empty
                    if (gridItem != null && gridItem.getCount() > 0) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean canCraftWith(List<ItemStack> availableItems) {
        Map<ItemType, Integer> required = getRequiredItemCounts();
        
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
        Map<ItemType, Integer> itemCounts = getRequiredItemCounts();
        List<ItemStack> ingredients = new ArrayList<>();
        
        for (Map.Entry<ItemType, Integer> entry : itemCounts.entrySet()) {
            ingredients.add(new ItemStack(entry.getKey(), entry.getValue()));
        }
        
        return ingredients;
    }
    
    @Override
    public String[] getPattern() {
        return pattern.clone();
    }
    
    private Map<ItemType, Integer> getRequiredItemCounts() {
        Map<ItemType, Integer> counts = new HashMap<>();
        
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && ingredients.containsKey(c)) {
                    ItemType itemType = ingredients.get(c);
                    counts.put(itemType, counts.getOrDefault(itemType, 0) + 1);
                }
            }
        }
        
        return counts;
    }
    
    /**
     * Get the ingredient mapping for display purposes
     */
    public Map<Character, ItemType> getIngredientMapping() {
        return new HashMap<>(ingredients);
    }
    
    /**
     * Get the pattern dimensions
     */
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}