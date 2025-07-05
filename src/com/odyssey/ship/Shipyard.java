package com.odyssey.ship;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entity.Ship;
import com.odyssey.world.BlockType;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;

public class Shipyard {

    private static final int MAX_SHIP_SIZE = 32; // Max search radius

    public static Ship assembleShip(VoxelEngine world, Vector3i startPos) {
        Map<Vector3i, BlockType> shipBlocks = new HashMap<>();
        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        Vector3i min = new Vector3i(startPos);
        Vector3i max = new Vector3i(startPos);

        while (!queue.isEmpty()) {
            Vector3i current = queue.poll();

            // Check bounds to prevent massive ships
            if (Math.abs(current.x - startPos.x) > MAX_SHIP_SIZE ||
                Math.abs(current.y - startPos.y) > MAX_SHIP_SIZE ||
                Math.abs(current.z - startPos.z) > MAX_SHIP_SIZE) {
                continue;
            }

            BlockType type = world.getBlock(current.x, current.y, current.z);
            if (type != BlockType.AIR && type != BlockType.WATER) {
                shipBlocks.put(current, type);

                // Update bounds
                min.min(current);
                max.max(current);

                // Check neighbors
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Vector3i neighbor = new Vector3i(current.x + x, current.y + y, current.z + z);
                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        if (shipBlocks.isEmpty() || !shipBlocks.containsKey(startPos)) {
            return null; // Failed to assemble
        }

        // Convert world coordinates to local ship coordinates
        Map<Vector3f, BlockType> localBlocks = new HashMap<>();
        for(Map.Entry<Vector3i, BlockType> entry : shipBlocks.entrySet()) {
            Vector3i worldBlockPos = entry.getKey();
            localBlocks.put(new Vector3f(worldBlockPos.x - min.x, worldBlockPos.y - min.y, worldBlockPos.z - min.z), entry.getValue());
            // Remove the block from the world
            world.setBlock(worldBlockPos.x, worldBlockPos.y, worldBlockPos.z, BlockType.AIR);
        }

        int width = max.x - min.x + 1;
        int height = max.y - min.y + 1;
        int depth = max.z - min.z + 1;
        
        Vector3f shipPosition = new Vector3f(min.x, min.y, min.z);
        return new Ship(shipPosition, localBlocks, width, height, depth);
    }
} 