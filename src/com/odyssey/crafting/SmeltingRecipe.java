package com.odyssey.crafting;

import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.ArrayList;
import java.util.List;

public class SmeltingRecipe extends CraftingRecipe {
    private final ItemType input;
    private final int smeltTime; // Time in ticks (20 ticks = 1 second)
    
    public SmeltingRecipe(String name, ItemType input, ItemType output, int smeltTime) {
        super(name, CraftingSystem.CraftingStationType.FURNACE, new ItemStack(output, 1));
        this.input = input;
        this.smeltTime = smeltTime;
    }
    
    @Override
    public boolean matches(ItemStack[][] craftingGrid) {
        // For smelting, we expect a 1x1 "grid" representing the furnace input slot
        if (craftingGrid.length != 1 || craftingGrid[0].length != 1) {
            return false;
        }
        
        ItemStack inputItem = craftingGrid[0][0];
        return itemMatches(inputItem, input);
    }
    
    /**
     * Check if this recipe can smelt the given input item
     */
    public boolean canSmelt(ItemStack inputItem) {
        return itemMatches(inputItem, input);
    }
    
    @Override
    public boolean canCraftWith(List<ItemStack> availableItems) {
        return countAvailableItems(availableItems, input) > 0;
    }
    
    @Override
    public List<ItemStack> getRequiredIngredients() {
        List<ItemStack> ingredients = new ArrayList<>();
        ingredients.add(new ItemStack(input, 1));
        return ingredients;
    }
    
    @Override
    public String[] getPattern() {
        // Smelting recipes don't have a traditional pattern
        // Return a simple representation
        return new String[]{
            input.name() + " -> " + result.getType().name()
        };
    }
    
    /**
     * Get the input item type for this smelting recipe
     */
    public ItemType getInput() {
        return input;
    }
    
    /**
     * Get the output item type for this smelting recipe
     */
    public ItemType getOutput() {
        return result.getType();
    }
    
    /**
     * Get the time required to smelt this item (in ticks)
     */
    public int getSmeltTime() {
        return smeltTime;
    }
    
    /**
     * Get the time required to smelt this item (in seconds)
     */
    public float getSmeltTimeSeconds() {
        return smeltTime / 20.0f;
    }
}