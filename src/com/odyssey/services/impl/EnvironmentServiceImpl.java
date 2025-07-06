package com.odyssey.services.impl;

import com.odyssey.config.GameConfiguration;
import com.odyssey.services.EnvironmentService;
import com.odyssey.environment.*;
import com.odyssey.effects.ParticleSystem;
import com.odyssey.player.Player;
import com.odyssey.audio.SoundManager;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentServiceImpl implements EnvironmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentServiceImpl.class);
    
    @Autowired
    private GameConfiguration config;
    
    private EnvironmentManager environmentManager;
    private ParticleSystem particleSystem;
    private com.odyssey.core.VoxelEngine voxelEngine;
    
    public EnvironmentServiceImpl() {
        // EnvironmentManager will be initialized with SoundManager later
    }
    
    public void initialize(Object soundManager) {
        this.environmentManager = new EnvironmentManager((SoundManager) soundManager, voxelEngine);
        this.particleSystem = new ParticleSystem(voxelEngine);
        logger.info("Environment service initialized");
    }
    
    public void setVoxelEngine(com.odyssey.core.VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
    }
    
    public void setParticleSystem(ParticleSystem particleSystem) {
        this.particleSystem = particleSystem;
    }
    
    @Override
    public void update(float deltaTime, Player player) {
        if (environmentManager != null) {
            environmentManager.update(deltaTime, voxelEngine, player);
        }
        
        if (particleSystem != null) {
            particleSystem.update(deltaTime);
        }
        
        logger.debug("Environment updated - Weather: {}, Time: {}", 
                    getCurrentWeather(), getTimeOfDay());
    }
    
    @Override
    public WeatherType getCurrentWeather() {
        return environmentManager != null ? environmentManager.getCurrentWeather() : WeatherType.CLEAR;
    }
    
    @Override
    public float getWetness() {
        return environmentManager != null ? environmentManager.getWetness() : 0.0f;
    }
    
    @Override
    public float getCloudCoverage() {
        return environmentManager != null ? environmentManager.getCloudCoverage() : 0.0f;
    }
    
    @Override
    public float getCloudDensity() {
        return environmentManager != null ? environmentManager.getCloudDensity() : 0.0f;
    }
    
    @Override
    public float getLightningFlashIntensity() {
        return environmentManager != null ? environmentManager.getLightningFlashIntensity() : 0.0f;
    }
    
    @Override
    public float getCropGrowthChance() {
        return environmentManager != null ? environmentManager.getCropGrowthChance() : 0.1f;
    }
    
    @Override
    public float getTimeOfDay() {
        return environmentManager != null ? environmentManager.getWorldClock().getTimeOfDay() : 6000.0f;
    }
    
    @Override
    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }
    
    @Override
    public Vector3f getSkyColor() {
        Vector3f color = new Vector3f(0.5f, 0.8f, 1.0f); // Default sunny day color
        
        if (environmentManager == null) {
            return color;
        }
        
        WeatherType weather = getCurrentWeather();
        if (weather == WeatherType.RAIN || weather == WeatherType.STORM || weather == WeatherType.FOG) {
            color.lerp(new Vector3f(0.4f, 0.4f, 0.45f), 0.7f);
        } else if (weather == WeatherType.SNOW) {
            color.lerp(new Vector3f(0.6f, 0.6f, 0.7f), 0.7f);
        } else if (weather == WeatherType.SANDSTORM) {
            color.set(0.8f, 0.7f, 0.5f); // Sandy color
        }

        // Day/Night Cycle tint
        float time = getTimeOfDay();
        if (time > 12000 || time < 500) { // Night
            color.mul(0.4f);
        } else if (time > 10000) { // Sunset
            color.lerp(new Vector3f(1.0f, 0.5f, 0.2f), (time - 10000) / 2000f);
        } else if (time < 2000) { // Sunrise
            color.lerp(new Vector3f(1.0f, 0.5f, 0.2f), (2000 - time) / 2000f);
        }
        
        return color;
    }
    
    public EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }
    
    @Override
    public void cleanup() {
        if (environmentManager != null) {
            environmentManager.cleanup();
        }
        logger.info("Environment service cleaned up");
    }
}