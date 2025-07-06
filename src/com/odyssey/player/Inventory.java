package com.odyssey.player;

import com.odyssey.world.BlockType;
import com.odyssey.inventory.ItemStack;
import com.odyssey.inventory.ItemType;

import java.util.HashMap;
import java.util.Map;

public class Inventory {

    public static final int HOTBAR_SIZE = 9;

    private final Map<Integer, ItemStack> slots = new HashMap<>();
    private int selectedSlot = 0;

    public Inventory() {
        // Add some starting blocks for testing
        slots.put(0, new ItemStack(ItemType.STONE, 64));
        slots.put(1, new ItemStack(ItemType.DIRT, 64));
        slots.put(2, new ItemStack(ItemType.WOODEN_PLANKS, 64));
    }

    public boolean addItem(ItemStack itemStack) {
        // Very simple add logic: find first available empty slot.
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (!slots.containsKey(i)) {
                slots.put(i, itemStack.copy());
                return true;
            } else if (slots.get(i).getType() == itemStack.getType()) {
                // Stack with existing items
                int maxStack = itemStack.getType().getMaxStackSize();
                int currentCount = slots.get(i).getCount();
                int addAmount = Math.min(itemStack.getCount(), maxStack - currentCount);
                if (addAmount > 0) {
                    slots.get(i).addItems(addAmount);
                    itemStack.removeItems(addAmount);
                    if (itemStack.getCount() <= 0) {
                        return true;
                    }
                }
            }
        }
        return false; // Inventory full
    }

    public ItemStack getSelectedItem() {
        return slots.get(selectedSlot);
    }

    public void consumeSelectedItem() {
        ItemStack selected = getSelectedItem();
        if (selected != null) {
            selected.removeItems(1);
            if (selected.getCount() <= 0) {
                slots.remove(selectedSlot);
            }
        }
    }
    
    public boolean removeItem(BlockType blockType, int quantity) {
        // Convert BlockType to ItemType for compatibility
        ItemType itemType = convertBlockToItemType(blockType);
        return removeItem(itemType, quantity);
    }
    
    public boolean removeItem(ItemType itemType, int quantity) {
        for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.getType() == itemType) {
                if (stack.getCount() >= quantity) {
                    stack.removeItems(quantity);
                    if (stack.getCount() <= 0) {
                        slots.remove(entry.getKey());
                    }
                    return true;
                }
            }
        }
        return false; // Not enough items to remove
    }
    
    private ItemType convertBlockToItemType(BlockType blockType) {
        // Simple conversion - in a full implementation this would be more comprehensive
        switch (blockType) {
            case STONE: return ItemType.STONE;
            case DIRT: return ItemType.DIRT;
            case GRASS: return ItemType.DIRT; // Grass drops dirt when broken
            case COBBLESTONE: return ItemType.COBBLESTONE;
            case WOOD: return ItemType.WOOD;
            case LEAVES: return ItemType.LEAVES;
            case SAND: return ItemType.SAND;
            case WATER: return ItemType.WATER_BUCKET;
            case GLASS: return ItemType.GLASS;
            case COAL_ORE: return ItemType.COAL_ORE;
            case IRON_ORE: return ItemType.IRON_ORE;
            case GOLD_ORE: return ItemType.GOLD_ORE;
            case DIAMOND_ORE: return ItemType.DIAMOND_ORE;
            default: return ItemType.STONE; // Fallback
        }
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public Map<Integer, ItemStack> getHotbarItems() {
        return slots;
    }
}