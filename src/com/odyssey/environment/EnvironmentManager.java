package com.odyssey.environment;

import com.odyssey.audio.SoundManager;
import com.odyssey.audio.WeatherAudioPlayer;
import com.odyssey.core.VoxelEngine;
import com.odyssey.effects.ParticleSystem;
import com.odyssey.player.Player;
import com.odyssey.world.BlockType;
import com.odyssey.world.biome.Biome;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Random;

public class EnvironmentManager {

    private final WorldClock worldClock;
    private final ParticleSystem particleSystem;
    private Season currentSeason = Season.SPRING;
    private WeatherType currentWeather = WeatherType.CLEAR;
    private int daysPerSeason = 28; // In-game days
    private float seasonTransition = 0.0f;
    private final Random random = new Random();

    // Wind
    private Vector2f windDirection = new Vector2f(1, 0);
    private float windSpeed = 5.0f; // In m/s for example
    private float timeSinceLastWindChange = 0f;
    private float nextWindChangeTime;

    // Timers for weather transitions
    private float timeInCurrentWeather = 0f;
    private float nextWeatherChangeTime;

    // Lightning
    private float timeSinceLastLightning = 0f;
    private float nextLightningTime;
    private float lightningFlashIntensity = 0.0f;
    private final float LIGHTNING_DECAY_RATE = 5.0f;

    // Wetness
    private float wetness = 0.0f;
    private final float WETNESS_TRANSITION_SPEED = 0.1f; // How fast it gets wet/dries

    private final WeatherAudioPlayer weatherAudioPlayer;

    public EnvironmentManager(SoundManager soundManager, VoxelEngine voxelEngine) {
        this.worldClock = new WorldClock();
        this.particleSystem = new ParticleSystem(voxelEngine);
        this.weatherAudioPlayer = new WeatherAudioPlayer(soundManager, this);
        setNextWeatherChangeTime();
        setNextWindChangeTime();
        setNextLightningTime();
    }

    public void update(float deltaTime, VoxelEngine engine, Player player) {
        worldClock.update(deltaTime);
        updateSeason();
        updateWeatherEffects(deltaTime, engine, player);
        updateWind(deltaTime);
        particleSystem.update(deltaTime);
        weatherAudioPlayer.update(deltaTime);

        timeInCurrentWeather += deltaTime;
        if (timeInCurrentWeather >= nextWeatherChangeTime) {
            Biome playerBiome = engine.getBiomeAt((int)player.getPosition().x, (int)player.getPosition().z);
            transitionWeather(playerBiome);
            timeInCurrentWeather = 0;
            setNextWeatherChangeTime();
        }

        // Decay lightning flash
        if (lightningFlashIntensity > 0) {
            lightningFlashIntensity -= LIGHTNING_DECAY_RATE * deltaTime;
            lightningFlashIntensity = Math.max(0, lightningFlashIntensity);
        }

        if (currentWeather == WeatherType.STORM) {
            handleLightning(deltaTime, engine, player);
        }

        updateWetness(deltaTime);
    }

    private void updateWeatherEffects(float deltaTime, VoxelEngine engine, Player player) {
        switch (currentWeather) {
            case RAIN:
                emitParticles(200, player.getPosition(), new Vector3f(0, -15f, 0), new Vector3f(1.0f, 1.0f, 1.0f));
                break;
            case SNOW:
                emitParticles(100, player.getPosition(), new Vector3f(0, -2f, 0), new Vector3f(1.0f, 1.0f, 1.0f));
                accumulateSnow(engine, player.getPosition());
                break;
            case SANDSTORM:
                Vector3f windVec = new Vector3f(getWindDirection().x, 0, getWindDirection().y);
                emitParticles(400, player.getPosition(), windVec.mul(getWindSpeed() * 0.5f), new Vector3f(0.8f, 0.7f, 0.5f));
                break;
            default:
                break;
        }
    }

    private void emitParticles(int count, Vector3f center, Vector3f velocity, Vector3f color) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            float radius = random.nextFloat() * 30; // 30 block radius
            float x = center.x + radius * (float) Math.cos(angle);
            float z = center.z + radius * (float) Math.sin(angle);
            float y = center.y + random.nextFloat() * 10; // Swirling sand

            Vector3f randomVelocity = new Vector3f(velocity).add(
                (random.nextFloat() - 0.5f) * 15, 
                (random.nextFloat() - 0.5f) * 10, 
                (random.nextFloat() - 0.5f) * 15);

            particleSystem.emit(new Vector3f(x, y, z), randomVelocity, color);
        }
    }

    private void updateSeason() {
        int dayOfYear = worldClock.getCurrentDay() % (daysPerSeason * 4);
        int dayOfSeason = dayOfYear % daysPerSeason;
        
        seasonTransition = (float)dayOfSeason / (float)daysPerSeason;

        if (dayOfYear < daysPerSeason) {
            currentSeason = Season.SPRING;
        } else if (dayOfYear < daysPerSeason * 2) {
            currentSeason = Season.SUMMER;
        } else if (dayOfYear < daysPerSeason * 3) {
            currentSeason = Season.AUTUMN;
        } else {
            currentSeason = Season.WINTER;
        }
    }
    
    private void transitionWeather(Biome biome) {
        float value = random.nextFloat();

        switch (biome) {
            case DESERT:
                if (value < 0.8f) currentWeather = WeatherType.CLEAR;
                else if (value < 0.95f) currentWeather = WeatherType.WINDY;
                else currentWeather = WeatherType.SANDSTORM; // New weather type!
                break;
            case FOREST:
                if (value < 0.5f) currentWeather = WeatherType.CLEAR;
                else if (value < 0.8f) currentWeather = WeatherType.RAIN;
                else currentWeather = WeatherType.STORM;
                break;
            case TUNDRA:
                if (value < 0.5f) currentWeather = WeatherType.CLOUDY;
                else if (value < 0.9f) currentWeather = WeatherType.SNOW;
                else currentWeather = WeatherType.FOG;
                break;
            case SWAMP:
                if (value < 0.3f) currentWeather = WeatherType.FOG;
                else if (value < 0.7f) currentWeather = WeatherType.RAIN;
                else currentWeather = WeatherType.STORM;
                break;
            case MOUNTAINS:
                if (value < 0.4f) currentWeather = WeatherType.CLEAR;
                else if (value < 0.7f) currentWeather = WeatherType.SNOW;
                else currentWeather = WeatherType.STORM;
                break;
            case OCEAN:
            default:
                if (value < 0.6f) currentWeather = WeatherType.CLEAR;
                else if (value < 0.85f) currentWeather = WeatherType.RAIN;
                else currentWeather = WeatherType.STORM;
                break;
        }
    }
    
    private void setNextWeatherChangeTime() {
        // Weather changes every 5-15 minutes (real-time)
        this.nextWeatherChangeTime = 300 + random.nextFloat() * 600;
    }

    private void updateWind(float deltaTime) {
        timeSinceLastWindChange += deltaTime;
        if (timeSinceLastWindChange > nextWindChangeTime) {
            // Change wind direction and speed
            float angle = random.nextFloat() * 2 * (float) Math.PI;
            windDirection.set((float) Math.cos(angle), (float) Math.sin(angle)).normalize();
            
            // Wind speed is higher during storms
            if (currentWeather == WeatherType.STORM || currentWeather == WeatherType.WINDY) {
                windSpeed = 15.0f + random.nextFloat() * 10f; // 15-25
            } else {
                windSpeed = 3.0f + random.nextFloat() * 7f; // 3-10
            }

            timeSinceLastWindChange = 0;
            setNextWindChangeTime();
            System.out.printf("Wind changed! Direction: (%.2f, %.2f), Speed: %.2f\n", windDirection.x, windDirection.y, windSpeed);
        }
    }

    private void setNextWindChangeTime() {
        // Wind changes every 2-5 minutes (real-time)
        this.nextWindChangeTime = 120 + random.nextFloat() * 180;
    }

    private void accumulateSnow(VoxelEngine engine, Vector3f center) {
        // Perform a few random ticks each update to accumulate snow
        for (int i = 0; i < 50; i++) {
            int x = (int)center.x + random.nextInt(64) - 32;
            int z = (int)center.z + random.nextInt(64) - 32;
            
            // Find the highest block at this x,z
            for (int y = 255; y >= 0; y--) {
                BlockType block = engine.getBlock(x, y, z);
                if (block != BlockType.AIR && !block.isTransparent()) {
                    // Found the top solid block. Check if we can place snow on it.
                    BlockType blockAbove = engine.getBlock(x, y + 1, z);
                    if (blockAbove == BlockType.AIR) {
                        engine.setBlock(x, y + 1, z, BlockType.SNOW_LAYER);
                    }
                    break; // Move to next random column
                }
            }
        }
    }

    private void handleLightning(float deltaTime, VoxelEngine engine, Player player) {
        timeSinceLastLightning += deltaTime;
        if (timeSinceLastLightning >= nextLightningTime) {
            // Strike!
            Vector3f playerPos = player.getPosition();
            int strikeX = (int)playerPos.x + random.nextInt(128) - 64;
            int strikeZ = (int)playerPos.z + random.nextInt(128) - 64;
            
            // Find highest block
            for (int y = com.odyssey.core.GameConstants.MAX_HEIGHT - 1; y > 0; y--) {
                BlockType block = engine.getBlock(strikeX, y, strikeZ);
                if (block != BlockType.AIR && !block.isTransparent()) {
                    int impactY = y + 1;
                    
                    // TODO: Add visual effect for lightning bolt
                    // For now, just set fire
                    
                    BlockType belowBlock = engine.getBlock(strikeX, y, strikeZ);
                    if (isFlammable(belowBlock)) {
                        engine.setBlock(strikeX, impactY, strikeZ, BlockType.FIRE);
                    }
                    
                    // TODO: Play dedicated lightning strike sound
                    
                    // TODO: Damage entities in radius
                    
                    break;
                }
            }
            
            // Trigger the flash
            this.lightningFlashIntensity = 1.0f;
            
            timeSinceLastLightning = 0;
            setNextLightningTime();
        }
    }
    
    private boolean isFlammable(BlockType blockType) {
        return blockType == BlockType.WOOD || blockType == BlockType.LEAVES;
    }
    
    private void setNextLightningTime() {
        this.nextLightningTime = 5f + random.nextFloat() * 10f; // Every 5-15 seconds
    }

    private void updateWetness(float deltaTime) {
        boolean isRaining = (currentWeather == WeatherType.RAIN || currentWeather == WeatherType.STORM);
        
        if (isRaining) {
            if (wetness < 1.0f) {
                wetness += WETNESS_TRANSITION_SPEED * deltaTime;
                wetness = Math.min(wetness, 1.0f);
            }
        } else {
            if (wetness > 0.0f) {
                wetness -= WETNESS_TRANSITION_SPEED * deltaTime;
                wetness = Math.max(wetness, 0.0f);
            }
        }
    }

    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }

    public WorldClock getWorldClock() {
        return worldClock;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public WeatherType getCurrentWeather() {
        return currentWeather;
    }
    
    public Vector2f getWindDirection() {
        return windDirection;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public void cleanup() {
        particleSystem.clear();
    }

    public float getCropGrowthChance() {
        if (currentWeather == WeatherType.RAIN) {
            return 0.75f; // 75% chance to grow during rain
        }
        // Could also add modifiers for season, biome, etc. later
        return 0.1f; // 10% base chance
    }

    public float getWetness() {
        return this.wetness;
    }

    public float getCloudCoverage() {
        switch(currentWeather) {
            case CLEAR: return 0.2f;
            case CLOUDY: return 0.7f;
            case RAIN: return 0.9f;
            case STORM: return 1.0f;
            case FOG: return 0.8f;
            default: return 0.4f;
        }
    }

    public float getCloudDensity() {
        switch(currentWeather) {
            case CLEAR: return 0.1f;
            case CLOUDY: return 0.4f;
            case RAIN: return 0.8f;
            case STORM: return 1.0f;
            case FOG: return 0.6f;
            default: return 0.2f;
        }
    }

    public float getLightningFlashIntensity() {
        return lightningFlashIntensity;
    }

    public int getCurrentSeasonId() {
        return currentSeason.ordinal();
    }
    
    public float getSeasonTransition() {
        return seasonTransition;
    }
}