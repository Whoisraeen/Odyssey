package com.odyssey.player;

import com.odyssey.world.BlockType;

import java.util.HashMap;
import java.util.Map;

public class Inventory {

    public static final int HOTBAR_SIZE = 9;

    private final Map<Integer, ItemStack> slots = new HashMap<>();
    private int selectedSlot = 0;

    public Inventory() {
        // Add some starting blocks for testing
        slots.put(0, new ItemStack(BlockType.STONE, 64));
        slots.put(1, new ItemStack(BlockType.DIRT, 64));
        slots.put(2, new ItemStack(BlockType.GRASS, 64));
    }

    public boolean addItem(ItemStack itemStack) {
        // Very simple add logic: find first available empty slot.
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (!slots.containsKey(i)) {
                slots.put(i, itemStack.copy());
                return true;
            } else if (slots.get(i).type == itemStack.type) {
                // Stack with existing items
                slots.get(i).quantity += itemStack.quantity;
                return true;
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
            selected.quantity--;
            if (selected.quantity <= 0) {
                slots.remove(selectedSlot);
            }
        }
    }
    
    public boolean removeItem(BlockType blockType, int quantity) {
        for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.type == blockType) {
                if (stack.quantity >= quantity) {
                    stack.quantity -= quantity;
                    if (stack.quantity <= 0) {
                        slots.remove(entry.getKey());
                    }
                    return true;
                }
            }
        }
        return false; // Not enough items to remove
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