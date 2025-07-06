package com.odyssey.services;

import com.odyssey.environment.WeatherType;
import com.odyssey.effects.ParticleSystem;
import com.odyssey.player.Player;
import org.joml.Vector3f;

/**
 * Service interface for environment and weather management
 */
public interface EnvironmentService {
    
    /**
     * Update environment systems
     */
    void update(float deltaTime, Player player);
    
    /**
     * Get current weather type
     */
    WeatherType getCurrentWeather();
    
    /**
     * Get wetness level (0.0 to 1.0)
     */
    float getWetness();
    
    /**
     * Get cloud coverage (0.0 to 1.0)
     */
    float getCloudCoverage();
    
    /**
     * Get cloud density (0.0 to 1.0)
     */
    float getCloudDensity();
    
    /**
     * Get lightning flash intensity (0.0 to 1.0)
     */
    float getLightningFlashIntensity();
    
    /**
     * Get crop growth chance modifier
     */
    float getCropGrowthChance();
    
    /**
     * Get time of day (0-24000)
     */
    float getTimeOfDay();
    
    /**
     * Get particle system for environment effects
     */
    ParticleSystem getParticleSystem();
    
    /**
     * Get sky color based on current conditions
     */
    Vector3f getSkyColor();
    
    /**
     * Get the underlying environment manager
     */
    com.odyssey.environment.EnvironmentManager getEnvironmentManager();
    
    /**
     * Cleanup environment resources
     */
    void cleanup();
}