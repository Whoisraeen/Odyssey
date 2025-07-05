package com.odyssey.rendering;

import com.odyssey.rendering.scene.Camera;
import com.odyssey.world.Chunk;
import java.util.Collection;

public class RenderManager {
    public void render(Camera camera, Collection<Chunk> chunks) {
        // Render all visible chunks
        for (Chunk chunk : chunks) {
            chunk.render();
        }
    }
    
    public void cleanup() {
        // Cleanup render resources
    }
} 