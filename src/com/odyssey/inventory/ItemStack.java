package com.odyssey.inventory;

import java.util.HashMap;
import java.util.Map;

public class ItemStack {
    private ItemType type;
    private int count;
    private int durability;
    private Map<String, Object> metadata;
    
    public ItemStack(ItemType type) {
        this(type, 1);
    }
    
    public ItemStack(ItemType type, int count) {
        this.type = type;
        this.count = Math.max(0, count);
        this.durability = type.getMaxDurability();
        this.metadata = new HashMap<>();
    }
    
    public ItemStack(ItemType type, int count, int durability) {
        this.type = type;
        this.count = Math.max(0, count);
        this.durability = Math.max(0, Math.min(durability, type.getMaxDurability()));
        this.metadata = new HashMap<>();
    }
    
    /**
     * Copy constructor
     */
    public ItemStack(ItemStack other) {
        this.type = other.type;
        this.count = other.count;
        this.durability = other.durability;
        this.metadata = new HashMap<>(other.metadata);
    }
    
    /**
     * Create a copy of this ItemStack
     */
    public ItemStack copy() {
        return new ItemStack(this);
    }
    
    /**
     * Check if this ItemStack is empty (count <= 0)
     */
    public boolean isEmpty() {
        return count <= 0 || type == ItemType.AIR;
    }
    
    /**
     * Check if this ItemStack can stack with another
     */
    public boolean canStackWith(ItemStack other) {
        if (other == null || isEmpty() || other.isEmpty()) {
            return false;
        }
        
        return type == other.type && 
               durability == other.durability &&
               metadata.equals(other.metadata);
    }
    
    /**
     * Add items to this stack, returns the amount that couldn't be added
     */
    public int addItems(int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int maxStack = type.getMaxStackSize();
        int canAdd = Math.min(amount, maxStack - count);
        count += canAdd;
        
        return amount - canAdd; // Return overflow
    }
    
    /**
     * Remove items from this stack, returns the amount actually removed
     */
    public int removeItems(int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int removed = Math.min(amount, count);
        count -= removed;
        
        if (count <= 0) {
            type = ItemType.AIR;
            count = 0;
        }
        
        return removed;
    }
    
    /**
     * Split this stack, returning a new stack with the specified amount
     */
    public ItemStack split(int amount) {
        if (amount <= 0 || amount >= count) {
            ItemStack result = copy();
            this.count = 0;
            this.type = ItemType.AIR;
            return result;
        }
        
        ItemStack result = new ItemStack(type, amount, durability);
        result.metadata = new HashMap<>(this.metadata);
        this.count -= amount;
        
        return result;
    }
    
    /**
     * Damage this item (for tools and armor)
     */
    public boolean damage(int amount) {
        if (type.getMaxDurability() <= 0) {
            return false; // Item doesn't have durability
        }
        
        durability -= amount;
        
        if (durability <= 0) {
            // Item is broken
            count = 0;
            type = ItemType.AIR;
            return true; // Item was destroyed
        }
        
        return false; // Item was damaged but not destroyed
    }
    
    /**
     * Repair this item
     */
    public void repair(int amount) {
        if (type.getMaxDurability() > 0) {
            durability = Math.min(type.getMaxDurability(), durability + amount);
        }
    }
    
    /**
     * Get the durability percentage (0.0 to 1.0)
     */
    public float getDurabilityPercentage() {
        int maxDurability = type.getMaxDurability();
        if (maxDurability <= 0) {
            return 1.0f; // Items without durability are always "full"
        }
        
        return (float) durability / maxDurability;
    }
    
    /**
     * Check if this item is damaged
     */
    public boolean isDamaged() {
        return type.getMaxDurability() > 0 && durability < type.getMaxDurability();
    }
    
    /**
     * Set metadata for this item
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get metadata for this item
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get metadata with default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        Object value = metadata.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Check if metadata exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Remove metadata
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }
    
    // Getters and setters
    public ItemType getType() {
        return type;
    }
    
    public void setType(ItemType type) {
        this.type = type;
        // Reset durability when type changes
        this.durability = type.getMaxDurability();
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = Math.max(0, Math.min(count, type.getMaxStackSize()));
        if (this.count <= 0) {
            this.type = ItemType.AIR;
        }
    }
    
    public int getDurability() {
        return durability;
    }
    
    public void setDurability(int durability) {
        this.durability = Math.max(0, Math.min(durability, type.getMaxDurability()));
    }
    
    public int getMaxStackSize() {
        return type.getMaxStackSize();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ItemStack itemStack = (ItemStack) obj;
        return count == itemStack.count &&
               durability == itemStack.durability &&
               type == itemStack.type &&
               metadata.equals(itemStack.metadata);
    }
    
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + count;
        result = 31 * result + durability;
        result = 31 * result + metadata.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(count).append("x ").append(type.name());
        
        if (isDamaged()) {
            sb.append(" (").append(durability).append("/").append(type.getMaxDurability()).append(")");
        }
        
        if (!metadata.isEmpty()) {
            sb.append(" ").append(metadata);
        }
        
        return sb.toString();
    }
}