package com.odyssey.world;

import com.odyssey.core.VoxelEngine;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles fluid simulation for water and lava blocks
 */
public class FluidSimulation {
    private final VoxelEngine voxelEngine;
    private final Queue<FluidUpdate> fluidUpdates;
    private final Set<Vector3i> scheduledFluidUpdates;
    
    // Flow directions (6 neighbors + down priority)
    private static final Vector3i[] FLOW_DIRECTIONS = {
        new Vector3i(0, -1, 0),  // Down (highest priority)
        new Vector3i(1, 0, 0),   // East
        new Vector3i(-1, 0, 0),  // West
        new Vector3i(0, 0, 1),   // South
        new Vector3i(0, 0, -1),  // North
    };
    
    public FluidSimulation(VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
        this.fluidUpdates = new ConcurrentLinkedQueue<>();
        this.scheduledFluidUpdates = new HashSet<>();
    }
    
    /**
     * Schedule a fluid update at the given position
     */
    public void scheduleFluidUpdate(int x, int y, int z) {
        Vector3i pos = new Vector3i(x, y, z);
        if (!scheduledFluidUpdates.contains(pos)) {
            fluidUpdates.offer(new FluidUpdate(x, y, z));
            scheduledFluidUpdates.add(pos);
        }
    }
    
    /**
     * Process all pending fluid updates
     */
    public void processFluidUpdates() {
        int maxUpdatesPerFrame = 50; // Limit to prevent lag
        int processed = 0;
        
        while (!fluidUpdates.isEmpty() && processed < maxUpdatesPerFrame) {
            FluidUpdate update = fluidUpdates.poll();
            if (update != null) {
                Vector3i pos = new Vector3i(update.x, update.y, update.z);
                scheduledFluidUpdates.remove(pos);
                processFluidAt(update.x, update.y, update.z);
                processed++;
            }
        }
    }
    
    /**
     * Process fluid behavior at a specific position
     */
    private void processFluidAt(int x, int y, int z) {
        BlockType currentBlock = voxelEngine.getBlock(x, y, z);
        
        if (currentBlock == BlockType.WATER) {
            processWaterFlow(x, y, z);
        }
        // Future: Add lava flow processing here
    }
    
    /**
     * Handle water flow mechanics
     */
    private void processWaterFlow(int x, int y, int z) {
        // Check if water can flow down
        BlockType below = voxelEngine.getBlock(x, y - 1, z);
        if (canFluidFlowInto(below)) {
            voxelEngine.setBlock(x, y - 1, z, BlockType.WATER);
            scheduleFluidUpdate(x, y - 1, z);
            return; // Water flows down, don't spread horizontally
        }
        
        // If can't flow down, try to spread horizontally
        List<Vector3i> validFlowDirections = new ArrayList<>();
        
        for (int i = 1; i < FLOW_DIRECTIONS.length; i++) { // Skip down direction
            Vector3i dir = FLOW_DIRECTIONS[i];
            int newX = x + dir.x;
            int newY = y + dir.y;
            int newZ = z + dir.z;
            
            BlockType targetBlock = voxelEngine.getBlock(newX, newY, newZ);
            if (canFluidFlowInto(targetBlock)) {
                // Check if there's support below the target position
                BlockType supportBelow = voxelEngine.getBlock(newX, newY - 1, newZ);
                if (!canFluidFlowInto(supportBelow)) {
                    validFlowDirections.add(new Vector3i(newX, newY, newZ));
                }
            }
        }
        
        // Flow to valid directions (limit to prevent infinite spread)
        if (!validFlowDirections.isEmpty() && validFlowDirections.size() <= 3) {
            for (Vector3i flowPos : validFlowDirections) {
                voxelEngine.setBlock(flowPos.x, flowPos.y, flowPos.z, BlockType.WATER);
                scheduleFluidUpdate(flowPos.x, flowPos.y, flowPos.z);
            }
        }
    }
    
    /**
     * Check if fluid can flow into a block type
     */
    private boolean canFluidFlowInto(BlockType blockType) {
        return blockType == BlockType.AIR;
    }
    
    /**
     * Called when a block is placed or removed to trigger fluid updates
     */
    public void onBlockChanged(int x, int y, int z, BlockType oldType, BlockType newType) {
        // If water was removed, check neighbors for flow
        if (oldType == BlockType.WATER && newType != BlockType.WATER) {
            scheduleNeighborFluidUpdates(x, y, z);
        }
        
        // If a block was removed next to water, water might flow
        if (oldType != BlockType.AIR && newType == BlockType.AIR) {
            scheduleNeighborFluidUpdates(x, y, z);
        }
        
        // If water was placed, start flow simulation
        if (newType == BlockType.WATER) {
            scheduleFluidUpdate(x, y, z);
        }
    }
    
    /**
     * Schedule fluid updates for all neighbors of a position
     */
    private void scheduleNeighborFluidUpdates(int x, int y, int z) {
        for (Vector3i dir : FLOW_DIRECTIONS) {
            int neighborX = x + dir.x;
            int neighborY = y + dir.y;
            int neighborZ = z + dir.z;
            
            BlockType neighborBlock = voxelEngine.getBlock(neighborX, neighborY, neighborZ);
            if (neighborBlock == BlockType.WATER) {
                scheduleFluidUpdate(neighborX, neighborY, neighborZ);
            }
        }
    }
    
    /**
     * Represents a pending fluid update
     */
    private static class FluidUpdate {
        final int x, y, z;
        
        FluidUpdate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}