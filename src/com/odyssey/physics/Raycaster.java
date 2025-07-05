package com.odyssey.physics;

import com.odyssey.core.VoxelEngine;
import com.odyssey.world.BlockType;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Raycaster {

    public static class RaycastResult {
        public final Vector3i blockPos;
        public final Vector3i face;

        public RaycastResult(Vector3i blockPos, Vector3i face) {
            this.blockPos = blockPos;
            this.face = face;
        }
    }

    public static RaycastResult cast(VoxelEngine world, Vector3f start, Vector3f direction, float maxDistance) {
        Vector3i blockPos = new Vector3i((int) Math.floor(start.x), (int) Math.floor(start.y), (int) Math.floor(start.z));
        Vector3f rayPos = new Vector3f(start);
        
        Vector3i step = new Vector3i(
            (int) Math.signum(direction.x),
            (int) Math.signum(direction.y),
            (int) Math.signum(direction.z)
        );

        Vector3f deltaDist = new Vector3f(
            Math.abs(1f / direction.x),
            Math.abs(1f / direction.y),
            Math.abs(1f / direction.z)
        );

        Vector3f sideDist = new Vector3f();
        if (direction.x > 0) {
            sideDist.x = ((float) Math.floor(start.x) + 1 - start.x) * deltaDist.x;
        } else {
            sideDist.x = (start.x - (float) Math.floor(start.x)) * deltaDist.x;
        }
        if (direction.y > 0) {
            sideDist.y = ((float) Math.floor(start.y) + 1 - start.y) * deltaDist.y;
        } else {
            sideDist.y = (start.y - (float) Math.floor(start.y)) * deltaDist.y;
        }
        if (direction.z > 0) {
            sideDist.z = ((float) Math.floor(start.z) + 1 - start.z) * deltaDist.z;
        } else {
            sideDist.z = (start.z - (float) Math.floor(start.z)) * deltaDist.z;
        }

        Vector3i lastFace = new Vector3i();

        for (int i = 0; i < maxDistance * 2; i++) {
            if (world.getBlock(blockPos.x, blockPos.y, blockPos.z) != BlockType.AIR) {
                return new RaycastResult(blockPos, lastFace);
            }

            if (sideDist.x < sideDist.y) {
                if (sideDist.x < sideDist.z) {
                    sideDist.x += deltaDist.x;
                    blockPos.x += step.x;
                    lastFace.set(-step.x, 0, 0);
                } else {
                    sideDist.z += deltaDist.z;
                    blockPos.z += step.z;
                    lastFace.set(0, 0, -step.z);
                }
            } else {
                if (sideDist.y < sideDist.z) {
                    sideDist.y += deltaDist.y;
                    blockPos.y += step.y;
                    lastFace.set(0, -step.y, 0);
                } else {
                    sideDist.z += deltaDist.z;
                    blockPos.z += step.z;
                    lastFace.set(0, 0, -step.z);
                }
            }
        }
        
        return null;
    }
} 