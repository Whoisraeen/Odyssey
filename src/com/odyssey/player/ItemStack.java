package com.odyssey.player;

import com.odyssey.world.BlockType;

public class ItemStack {
    public BlockType type;
    public int quantity;

    public ItemStack(BlockType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public ItemStack copy() {
        return new ItemStack(this.type, this.quantity);
    }
} 