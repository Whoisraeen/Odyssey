package com.odyssey.world;

import org.joml.Vector3f;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

import static com.odyssey.core.VoxelEngine.*;

/**
 * Advanced Chunk Management with memory optimization
 */
public class ChunkManager {
    private final Map<ChunkPosition, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final Map<ChunkPosition, Future<Chunk>> loadingChunks = new ConcurrentHashMap<>();
    private final ExecutorService chunkLoadingPool;
    private final WorldGenerator worldGenerator;
    
    // Memory management
    private final int maxLoadedChunks = (RENDER_DISTANCE * 2) * (RENDER_DISTANCE * 2) * WORLD_HEIGHT;
    private final Queue<ChunkPosition> chunksToUnload = new ConcurrentLinkedQueue<>();
    
    public ChunkManager(ExecutorService chunkLoadingPool) {
        this.chunkLoadingPool = chunkLoadingPool;
        this.worldGenerator = new WorldGenerator();
    }
    
    public void update(Vector3f playerPos) {
        ChunkPosition playerChunk = new ChunkPosition(
            (int) Math.floor(playerPos.x / CHUNK_SIZE),
            (int) Math.floor(playerPos.y / CHUNK_SIZE),
            (int) Math.floor(playerPos.z / CHUNK_SIZE)
        );
        
        // Load chunks around player
        loadChunksAroundPlayer(playerChunk);
        
        // Unload distant chunks
        unloadDistantChunks(playerChunk);
        
        // Process completed chunk loading
        processCompletedLoading();
    }
    
    private void loadChunksAroundPlayer(ChunkPosition playerChunk) {
        for (int x = -RENDER_DISTANCE; x <= RENDER_DISTANCE; x++) {
            for (int z = -RENDER_DISTANCE; z <= RENDER_DISTANCE; z++) {
                for (int y = 0; y < WORLD_HEIGHT; y++) {
                    ChunkPosition pos = new ChunkPosition(
                        playerChunk.x + x, y, playerChunk.z + z
                    );
                    
                    if (!loadedChunks.containsKey(pos) && !loadingChunks.containsKey(pos)) {
                        // Start loading chunk asynchronously
                        Future<Chunk> future = chunkLoadingPool.submit(() -> {
                            Chunk chunk = new Chunk(pos);
                            worldGenerator.generateChunk(chunk);
                            return chunk;
                        });
                        loadingChunks.put(pos, future);
                    }
                }
            }
        }
    }
    
    private void unloadDistantChunks(ChunkPosition playerChunk) {
        Iterator<Map.Entry<ChunkPosition, Chunk>> iterator = loadedChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPosition, Chunk> entry = iterator.next();
            ChunkPosition pos = entry.getKey();
            
            double distance = Math.sqrt(
                Math.pow(pos.x - playerChunk.x, 2) + 
                Math.pow(pos.z - playerChunk.z, 2)
            );
            
            if (distance > RENDER_DISTANCE + 2) { // Hysteresis to prevent thrashing
                entry.getValue().cleanup();
                iterator.remove();
            }
        }
    }
    
    private void processCompletedLoading() {
        Iterator<Map.Entry<ChunkPosition, Future<Chunk>>> iterator = loadingChunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPosition, Future<Chunk>> entry = iterator.next();
            Future<Chunk> future = entry.getValue();
            
            if (future.isDone()) {
                try {
                    Chunk chunk = future.get();
                    loadedChunks.put(entry.getKey(), chunk);
                    iterator.remove();
                } catch (Exception e) {
                    e.printStackTrace();
                    iterator.remove();
                }
            }
        }
    }
    
    public Collection<Chunk> getVisibleChunks() {
        return loadedChunks.values();
    }
    
    public void cleanup() {
        loadedChunks.values().forEach(Chunk::cleanup);
        loadedChunks.clear();
        loadingChunks.values().forEach(future -> future.cancel(true));
        loadingChunks.clear();
    }
}