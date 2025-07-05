package com.odyssey.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block palette for memory-efficient storage
 */
public class BlockPalette {
    private final List<BlockType> blocks = new ArrayList<>();
    private final Map<BlockType, Byte> blockToIndex = new HashMap<>();
    
    public BlockPalette() {
        // Always start with AIR at index 0
        blocks.add(BlockType.AIR);
        blockToIndex.put(BlockType.AIR, (byte) 0);
    }
    
    public byte getOrAdd(BlockType block) {
        Byte index = blockToIndex.get(block);
        if (index == null) {
            if (blocks.size() >= 256) {
                throw new RuntimeException("Chunk palette overflow - too many unique blocks");
            }
            index = (byte) blocks.size();
            blocks.add(block);
            blockToIndex.put(block, index);
        }
        return index;
    }
    
    public BlockType getBlock(byte index) {
        if (index < 0 || index >= blocks.size()) {
            return BlockType.AIR;
        }
        return blocks.get(index & 0xFF); // Treat as unsigned
    }
} 