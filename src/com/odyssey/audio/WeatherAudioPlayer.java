package com.odyssey.audio;

import com.odyssey.environment.EnvironmentManager;
import com.odyssey.environment.WeatherType;

import java.util.Random;

public class WeatherAudioPlayer {

    private final SoundManager soundManager;
    private final EnvironmentManager environmentManager;

    private final int rainBuffer;
    private final int windBuffer;
    private final int thunderBuffer;

    private final int rainSource;
    private final int windSource;
    private final int thunderSource;

    private WeatherType previousWeather = WeatherType.CLEAR;
    private final Random random = new Random();
    private float timeSinceLastThunder = 0f;
    private float nextThunderTime = 10f + random.nextFloat() * 15f;

    public WeatherAudioPlayer(SoundManager soundManager, EnvironmentManager environmentManager) {
        this.soundManager = soundManager;
        this.environmentManager = environmentManager;

        // Load sounds - will return -1 for missing files
        this.rainBuffer = soundManager.loadSound("resources/sounds/rain.ogg");
        this.windBuffer = soundManager.loadSound("resources/sounds/wind.ogg");
        this.thunderBuffer = soundManager.loadSound("resources/sounds/thunder.ogg");

        // Create sources
        this.rainSource = soundManager.createSource(true);
        this.windSource = soundManager.createSource(true);
        this.thunderSource = soundManager.createSource(false);
        
        // Log which sounds are available
        System.out.println("WeatherAudioPlayer initialized:");
        System.out.println("  Rain sound: " + (soundManager.isValidBuffer(rainBuffer) ? "Available" : "Missing"));
        System.out.println("  Wind sound: " + (soundManager.isValidBuffer(windBuffer) ? "Available" : "Missing"));
        System.out.println("  Thunder sound: " + (soundManager.isValidBuffer(thunderBuffer) ? "Available" : "Missing"));
    }

    public void update(float deltaTime) {
        WeatherType currentWeather = environmentManager.getCurrentWeather();

        if (currentWeather != previousWeather) {
            stopAllLoops();
            switch (currentWeather) {
                case RAIN:
                case STORM:
                    if (soundManager.isValidBuffer(rainBuffer)) {
                        soundManager.playSound(rainSource, rainBuffer);
                    }
                    break;
                // Wind will be handled separately based on speed
            }
            previousWeather = currentWeather;
        }

        // Handle continuous sounds like wind
        updateWindSound();
        
        // Handle one-shot sounds like thunder
        if (currentWeather == WeatherType.STORM) {
            timeSinceLastThunder += deltaTime;
            if (timeSinceLastThunder >= nextThunderTime) {
                if (soundManager.isValidBuffer(thunderBuffer)) {
                    soundManager.playSound(thunderSource, thunderBuffer);
                }
                timeSinceLastThunder = 0;
                nextThunderTime = 10f + random.nextFloat() * 15f; // 10-25 seconds until next thunder
            }
        }
    }

    private void updateWindSound() {
        float windSpeed = environmentManager.getWindSpeed();
        if (windSpeed > 3.0f) {
            // Check if it's already playing
            // This is a simplification; a better system might check source state
            if (previousWeather != WeatherType.STORM && previousWeather != WeatherType.WINDY) {
                if (soundManager.isValidBuffer(windBuffer)) {
                    soundManager.playSound(windSource, windBuffer);
                }
            }
            float volume = Math.min(1.0f, (windSpeed - 3.0f) / 20.0f); // Normalize volume
            soundManager.setVolume(windSource, volume);
        } else {
            soundManager.stopSound(windSource);
        }
    }

    private void stopAllLoops() {
        soundManager.stopSound(rainSource);
        soundManager.stopSound(windSource);
    }
} 