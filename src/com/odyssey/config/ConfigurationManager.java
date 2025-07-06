package com.odyssey.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages game configuration with support for runtime updates and property file loading.
 * Thread-safe implementation with read-write locks for performance.
 */
@Service
public class ConfigurationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String DEFAULT_CONFIG_FILE = "odyssey.properties";
    private static final String USER_CONFIG_FILE = "odyssey-user.properties";
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, Object> runtimeOverrides = new ConcurrentHashMap<>();
    
    private GameConfiguration configuration;
    private Properties properties;
    
    public ConfigurationManager() {
        loadConfiguration();
    }
    
    /**
     * Gets the current game configuration (thread-safe read)
     */
    public GameConfiguration getConfiguration() {
        lock.readLock().lock();
        try {
            return configuration;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Reloads configuration from files (thread-safe write)
     */
    public void reloadConfiguration() {
        lock.writeLock().lock();
        try {
            loadConfiguration();
            logger.info("Configuration reloaded successfully");
        } catch (Exception e) {
            logger.error("Failed to reload configuration", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Sets a runtime override for a configuration value
     */
    public void setRuntimeOverride(String key, Object value) {
        runtimeOverrides.put(key, value);
        logger.debug("Runtime override set: {} = {}", key, value);
    }
    
    /**
     * Removes a runtime override
     */
    public void removeRuntimeOverride(String key) {
        runtimeOverrides.remove(key);
        logger.debug("Runtime override removed: {}", key);
    }
    
    /**
     * Gets a configuration value with runtime override support
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, T defaultValue) {
        // Check runtime overrides first
        Object override = runtimeOverrides.get(key);
        if (override != null) {
            try {
                return (T) override;
            } catch (ClassCastException e) {
                logger.warn("Runtime override type mismatch for key: {}", key);
            }
        }
        
        // Check properties file
        String propertyValue = properties.getProperty(key);
        if (propertyValue != null) {
            try {
                return convertValue(propertyValue, defaultValue);
            } catch (Exception e) {
                logger.warn("Failed to convert property value for key: {}", key, e);
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Saves current configuration to user properties file
     */
    public void saveUserConfiguration() {
        try {
            Path userConfigPath = Paths.get(USER_CONFIG_FILE);
            Properties userProps = new Properties();
            
            // Add runtime overrides to user properties
            runtimeOverrides.forEach((key, value) -> 
                userProps.setProperty(key, String.valueOf(value)));
            
            try (var writer = Files.newBufferedWriter(userConfigPath)) {
                userProps.store(writer, "Odyssey User Configuration");
            }
            
            logger.info("User configuration saved to: {}", userConfigPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save user configuration", e);
        }
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        // Load default configuration from classpath
        loadPropertiesFromClasspath(DEFAULT_CONFIG_FILE);
        
        // Load user configuration if it exists
        loadPropertiesFromFile(USER_CONFIG_FILE);
        
        // Create configuration object with loaded properties
        configuration = createConfigurationFromProperties();
        
        logger.info("Configuration loaded successfully");
    }
    
    private void loadPropertiesFromClasspath(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                properties.load(is);
                logger.debug("Loaded properties from classpath: {}", filename);
            } else {
                logger.debug("Properties file not found in classpath: {}", filename);
            }
        } catch (IOException e) {
            logger.warn("Failed to load properties from classpath: {}", filename, e);
        }
    }
    
    private void loadPropertiesFromFile(String filename) {
        Path configPath = Paths.get(filename);
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                properties.load(is);
                logger.debug("Loaded properties from file: {}", configPath.toAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to load properties from file: {}", filename, e);
            }
        }
    }
    
    private GameConfiguration createConfigurationFromProperties() {
        // For now, return default configuration
        // In a full implementation, this would parse properties and create custom configuration
        return new GameConfiguration();
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convertValue(String value, T defaultValue) {
        if (defaultValue instanceof Integer) {
            return (T) Integer.valueOf(value);
        } else if (defaultValue instanceof Float) {
            return (T) Float.valueOf(value);
        } else if (defaultValue instanceof Double) {
            return (T) Double.valueOf(value);
        } else if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(value);
        } else if (defaultValue instanceof Long) {
            return (T) Long.valueOf(value);
        } else {
            return (T) value;
        }
    }
    
    /**
     * Validates configuration values and logs warnings for invalid settings
     */
    public void validateConfiguration() {
        GameConfiguration config = getConfiguration();
        
        // Validate world configuration
        if (config.world().chunkSize() <= 0 || config.world().chunkSize() > 64) {
            logger.warn("Invalid chunk size: {}. Should be between 1 and 64", config.world().chunkSize());
        }
        
        if (config.world().renderDistance() <= 0 || config.world().renderDistance() > 32) {
            logger.warn("Invalid render distance: {}. Should be between 1 and 32", config.world().renderDistance());
        }
        
        // Validate rendering configuration
        if (config.rendering().camera().defaultFov() < 1.0f || config.rendering().camera().defaultFov() > 179.0f) {
            logger.warn("Invalid FOV: {}. Should be between 1.0 and 179.0", config.rendering().camera().defaultFov());
        }
        
        // Validate performance configuration
        if (config.performance().threading().workerThreads() <= 0) {
            logger.warn("Invalid worker thread count: {}. Should be greater than 0", 
                config.performance().threading().workerThreads());
        }
        
        logger.info("Configuration validation completed");
    }
}