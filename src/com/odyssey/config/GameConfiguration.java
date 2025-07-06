package com.odyssey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration management for the Odyssey game engine.
 * Uses Java 21 records for immutable configuration data.
 */
@Component
// @ConfigurationProperties(prefix = "odyssey") // Temporarily disabled due to Java records compatibility
public class GameConfiguration {
    
    private final WorldConfig world;
    private final RenderingConfig rendering;
    private final EntityConfig entity;
    private final PerformanceConfig performance;
    private final UIConfig ui;
    
    public GameConfiguration() {
        this.world = new WorldConfig();
        this.rendering = new RenderingConfig();
        this.entity = new EntityConfig();
        this.performance = new PerformanceConfig();
        this.ui = new UIConfig();
    }
    
    public WorldConfig world() { return world; }
    public RenderingConfig rendering() { return rendering; }
    public EntityConfig entity() { return entity; }
    public PerformanceConfig performance() { return performance; }
    public UIConfig ui() { return ui; }
    
    /**
     * World generation and chunk management configuration
     */
    public static record WorldConfig(
        int chunkSize,
        int chunkHeight,
        int renderDistance,
        int maxLoadedChunks,
        CaveConfig caves,
        LightingConfig lighting,
        SpawnConfig spawn
    ) {
        public WorldConfig() {
            this(16, 256, 16, 1024, new CaveConfig(), new LightingConfig(), new SpawnConfig());
        }
    }
    
    /**
     * Cave generation configuration
     */
    public static record CaveConfig(
        int minHeight,
        int maxHeight,
        double density,
        int maxTunnelLength
    ) {
        public CaveConfig() {
            this(5, 50, 0.3, 100);
        }
    }
    
    /**
     * Lighting system configuration
     */
    public static record LightingConfig(
        int maxLightLevel,
        int minLightLevel,
        boolean enableShadows,
        int shadowMapSize
    ) {
        public LightingConfig() {
            this(15, 0, true, 2048);
        }
    }
    
    /**
     * Spawn system configuration
     */
    public static record SpawnConfig(
        int maxAttempts,
        int searchRadius,
        double spawnRate
    ) {
        public SpawnConfig() {
            this(100, 32, 0.1);
        }
    }
    
    /**
     * Rendering system configuration
     */
    public static record RenderingConfig(
        CameraConfig camera,
        ParticleConfig particles,
        CloudConfig clouds,
        ShaderConfig shaders
    ) {
        public RenderingConfig() {
            this(new CameraConfig(), new ParticleConfig(), new CloudConfig(), new ShaderConfig());
        }
    }
    
    /**
     * Camera configuration
     */
    public static record CameraConfig(
        float minAspectRatio,
        float maxAspectRatio,
        float minFov,
        float maxFov,
        float minNearPlane,
        float maxFarPlane,
        float minPitch,
        float maxPitch,
        float defaultFov,
        float defaultNearPlane,
        float defaultFarPlane
    ) {
        public CameraConfig() {
            this(0.1f, 10.0f, 1.0f, 179.0f, 0.001f, 10000.0f, -89.9f, 89.9f, 70.0f, 0.1f, 1000.0f);
        }
    }
    
    /**
     * Particle system configuration
     */
    public static record ParticleConfig(
        int maxParticles,
        int maxParticlesPerFrame,
        float defaultLifetime,
        boolean enablePhysics
    ) {
        public ParticleConfig() {
            this(1000, 50, 5.0f, true);
        }
    }
    
    /**
     * Cloud rendering configuration
     */
    public static record CloudConfig(
        int maxMarchStepsLow,
        int maxMarchStepsMed,
        int maxMarchStepsHigh,
        float maxDistance,
        float minStepSize,
        float maxStepSize,
        float density,
        float coverage
    ) {
        public CloudConfig() {
            this(32, 64, 128, 50000.0f, 100.0f, 1000.0f, 0.5f, 0.6f);
        }
    }
    
    /**
     * Shader configuration
     */
    public static record ShaderConfig(
        int maxSpotLights,
        float maxReflectionLod,
        boolean enableDeferred,
        boolean enableSSAO,
        boolean enableBloom
    ) {
        public ShaderConfig() {
            this(16, 4.0f, true, true, true);
        }
    }
    
    /**
     * Entity system configuration
     */
    public static record EntityConfig(
        MobConfig mobs,
        ShipConfig ships
    ) {
        public EntityConfig() {
            this(new MobConfig(), new ShipConfig());
        }
    }
    
    /**
     * Mob spawning configuration
     */
    public static record MobConfig(
        int maxMobs,
        int maxSpawnAttempts,
        double spawnRate,
        int despawnDistance
    ) {
        public MobConfig() {
            this(50, 10, 0.05, 64);
        }
    }
    
    /**
     * Ship system configuration
     */
    public static record ShipConfig(
        int maxShipSize,
        int maxShips,
        boolean enablePhysics
    ) {
        public ShipConfig() {
            this(32, 10, true);
        }
    }
    
    /**
     * Performance and optimization configuration
     */
    public static record PerformanceConfig(
        ThreadingConfig threading,
        MemoryConfig memory,
        OptimizationConfig optimization,
        int randomTicksPerChunk
    ) {
        public PerformanceConfig() {
            this(new ThreadingConfig(), new MemoryConfig(), new OptimizationConfig(), 3);
        }
    }
    
    /**
     * Threading configuration
     */
    public static record ThreadingConfig(
        int workerThreads,
        int chunkLoadingThreads,
        int meshGenerationThreads,
        boolean enableAsyncLoading
    ) {
        public ThreadingConfig() {
            this(Runtime.getRuntime().availableProcessors(), 2, 4, true);
        }
    }
    
    /**
     * Memory management configuration
     */
    public static record MemoryConfig(
        int maxPoolSize,
        int initialPoolSize,
        boolean enableObjectPooling,
        long maxHeapSize
    ) {
        public MemoryConfig() {
            this(50, 10, true, Runtime.getRuntime().maxMemory());
        }
    }
    
    /**
     * Optimization configuration
     */
    public static record OptimizationConfig(
        boolean enableFrustumCulling,
        boolean enableOcclusionCulling,
        boolean enableLOD,
        int maxDrawCalls,
        float targetFrameRate
    ) {
        public OptimizationConfig() {
            this(true, true, true, 1000, 60.0f);
        }
    }
    
    /**
     * UI system configuration
     */
    public static record UIConfig(
        int maxQuads,
        boolean enableAntialiasing,
        float uiScale,
        int maxTextLength
    ) {
        public UIConfig() {
            this(1000, true, 1.0f, 1000);
        }
    }
}