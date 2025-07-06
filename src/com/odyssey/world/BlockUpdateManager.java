package com.odyssey.world;

import com.odyssey.services.WorldService;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockUpdateManager {
    private final WorldService worldService;
    private final ChunkManager chunkManager;
    private final Queue<BlockUpdate> updateQueue;
    private final Set<BlockPosition> scheduledUpdates;
    
    public BlockUpdateManager(WorldService worldService, ChunkManager chunkManager) {
        this.worldService = worldService;
        this.chunkManager = chunkManager; // Can be null if WorldService manages chunks directly
        this.updateQueue = new ConcurrentLinkedQueue<>();
        this.scheduledUpdates = new HashSet<>();
    }
    
    public void scheduleBlockUpdate(int x, int y, int z, int delay) {
        BlockPosition pos = new BlockPosition(x, y, z);
        if (!scheduledUpdates.contains(pos)) {
            updateQueue.offer(new BlockUpdate(x, y, z, System.currentTimeMillis() + delay));
            scheduledUpdates.add(pos);
        }
    }
    
    public void onBlockChanged(int x, int y, int z, BlockType oldBlock, BlockType newBlock) {
        // Handle the block change event
        notifyNeighbors(x, y, z, oldBlock, newBlock);
        
        // Update lighting if transparency changed
        if (oldBlock.isTransparent() != newBlock.isTransparent()) {
            updateLighting(x, y, z);
        }
    }
    
    public void notifyNeighbors(int x, int y, int z, BlockType oldBlock, BlockType newBlock) {
        // Notify all 6 adjacent neighbors
        int[][] neighbors = {
            {x + 1, y, z}, {x - 1, y, z},
            {x, y + 1, z}, {x, y - 1, z},
            {x, y, z + 1}, {x, y, z - 1}
        };
        
        for (int[] neighbor : neighbors) {
            onNeighborChanged(neighbor[0], neighbor[1], neighbor[2], x, y, z, oldBlock, newBlock);
        }
    }
    
    private void onNeighborChanged(int x, int y, int z, int changedX, int changedY, int changedZ, 
                                  BlockType oldBlock, BlockType newBlock) {
        BlockType currentBlock = worldService.getBlock(x, y, z);
        
        // Handle water flow
        if (currentBlock == BlockType.WATER) {
            handleWaterFlow(x, y, z);
        }
        
        // Handle falling blocks (sand, gravel)
        if (currentBlock == BlockType.SAND && changedY == y - 1 && newBlock == BlockType.AIR) {
            scheduleBlockUpdate(x, y, z, 100); // Schedule falling after 100ms
        }
        
        // Handle light updates
        if (oldBlock.isTransparent() != newBlock.isTransparent()) {
            updateLighting(x, y, z);
        }
        
        // Handle farmland moisture updates
        if (currentBlock == BlockType.FARMLAND) {
            updateFarmlandMoisture(x, y, z);
        }
    }
    
    private void handleWaterFlow(int x, int y, int z) {
        // Water flows to adjacent air blocks at same level or below
        int[][] flowDirections = {
            {x + 1, y, z}, {x - 1, y, z}, {x, y, z + 1}, {x, y, z - 1}, // Horizontal
            {x, y - 1, z} // Downward
        };
        
        for (int[] dir : flowDirections) {
            BlockType neighborBlock = worldService.getBlock(dir[0], dir[1], dir[2]);
            if (neighborBlock == BlockType.AIR) {
                // Schedule water placement
                scheduleWaterPlacement(dir[0], dir[1], dir[2]);
            }
        }
    }
    
    private void scheduleWaterPlacement(int x, int y, int z) {
        // Only place water if it's not already scheduled
        BlockPosition pos = new BlockPosition(x, y, z);
        if (!scheduledUpdates.contains(pos)) {
            updateQueue.offer(new BlockUpdate(x, y, z, System.currentTimeMillis() + 200, BlockType.WATER));
            scheduledUpdates.add(pos);
        }
    }
    
    private void updateLighting(int x, int y, int z) {
        // Propagate light changes to surrounding blocks
        // This is a simplified lighting system
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    int nx = x + dx, ny = y + dy, nz = z + dz;
                    Chunk chunk = chunkManager.getChunk(nx >> 4, nz >> 4);
                    if (chunk != null) {
                        chunk.markForRebuild();
                    }
                }
            }
        }
    }
    
    private void updateFarmlandMoisture(int x, int y, int z) {
        // Check for nearby water sources
        boolean hasWater = false;
        for (int dx = -4; dx <= 4 && !hasWater; dx++) {
            for (int dz = -4; dz <= 4 && !hasWater; dz++) {
                for (int dy = -1; dy <= 1 && !hasWater; dy++) {
                    if (worldService.getBlock(x + dx, y + dy, z + dz) == BlockType.WATER) {
                        hasWater = true;
                    }
                }
            }
        }
        
        if (!hasWater) {
            // Schedule farmland to turn back to dirt
            scheduleBlockUpdate(x, y, z, 30000); // 30 seconds
        }
    }
    
    public void processUpdates() {
        long currentTime = System.currentTimeMillis();
        
        while (!updateQueue.isEmpty()) {
            BlockUpdate update = updateQueue.peek();
            if (update.getExecutionTime() <= currentTime) {
                updateQueue.poll();
                scheduledUpdates.remove(new BlockPosition(update.getX(), update.getY(), update.getZ()));
                
                executeBlockUpdate(update);
            } else {
                break; // No more updates ready to execute
            }
        }
    }
    
    private void executeBlockUpdate(BlockUpdate update) {
        int x = update.getX(), y = update.getY(), z = update.getZ();
        BlockType currentBlock = worldService.getBlock(x, y, z);
        
        if (update.getNewBlockType() != null) {
            // Scheduled block placement (e.g., water flow)
            worldService.setBlock(x, y, z, update.getNewBlockType());
        } else {
            // Handle block physics
            if (currentBlock == BlockType.SAND) {
                handleFallingBlock(x, y, z);
            } else if (currentBlock == BlockType.FARMLAND) {
                // Convert farmland back to dirt if no water nearby
                worldService.setBlock(x, y, z, BlockType.DIRT);
            }
        }
    }
    
    private void handleFallingBlock(int x, int y, int z) {
        // Check if block below is air
        if (worldService.getBlock(x, y - 1, z) == BlockType.AIR) {
            // Move block down
            worldService.setBlock(x, y, z, BlockType.AIR);
            worldService.setBlock(x, y - 1, z, BlockType.SAND);
            
            // Schedule another fall check
            scheduleBlockUpdate(x, y - 1, z, 100);
        }
    }
    
    // Helper classes
    private static class BlockUpdate {
        private final int x, y, z;
        private final long executionTime;
        private final BlockType newBlockType;
        
        public BlockUpdate(int x, int y, int z, long executionTime) {
            this(x, y, z, executionTime, null);
        }
        
        public BlockUpdate(int x, int y, int z, long executionTime, BlockType newBlockType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.executionTime = executionTime;
            this.newBlockType = newBlockType;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public long getExecutionTime() { return executionTime; }
        public BlockType getNewBlockType() { return newBlockType; }
    }
    
    private static class BlockPosition {
        private final int x, y, z;
        
        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockPosition)) return false;
            BlockPosition other = (BlockPosition) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}