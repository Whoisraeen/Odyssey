package com.odyssey.ui;

import com.odyssey.core.VoxelEngine;
import com.odyssey.rendering.ui.UIRenderer;
import com.odyssey.input.InputManager;
import com.odyssey.audio.SoundManager;

public class GameStateManager {
    private GameState currentState;
    private final UIRenderer uiRenderer;
    
    // Menu screens
    private MainMenu mainMenu;
    private PauseMenu pauseMenu;
    private WorldCreationMenu worldCreationMenu;
    private WorldSelectionMenu worldSelectionMenu;
    
    // Game components
    private VoxelEngine voxelEngine;
    private final SoundManager soundManager;
    
    // World creation data
    private String pendingWorldName = "New World";
    private long pendingWorldSeed = System.currentTimeMillis();
    
    private final int windowWidth;
    private final int windowHeight;
    
    public GameStateManager(UIRenderer uiRenderer, SoundManager soundManager, int windowWidth, int windowHeight) {
        this.uiRenderer = uiRenderer;
        this.soundManager = soundManager;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.currentState = GameState.IN_GAME; // Temporarily bypass menu for testing
        
        initializeMenus();
    }
    
    private void initializeMenus() {
        mainMenu = new MainMenu(uiRenderer, this);
        pauseMenu = new PauseMenu(uiRenderer, this);
        worldCreationMenu = new WorldCreationMenu(uiRenderer, this);
        worldSelectionMenu = new WorldSelectionMenu(uiRenderer, this);
    }
    
    public void setState(GameState newState) {
        GameState previousState = currentState;
        currentState = newState;
        
        // Handle state transitions
        switch (newState) {
            case IN_GAME:
                if (voxelEngine == null) {
                    // Create new world
                    voxelEngine = new VoxelEngine(soundManager, windowWidth, windowHeight);
                    // TODO: Apply world name and seed
                }
                break;
            case MAIN_MENU:
                // Clean up game if returning to main menu
                if (voxelEngine != null && previousState == GameState.IN_GAME) {
                    // TODO: Save world before cleanup
                    voxelEngine.cleanup();
                    voxelEngine = null;
                }
                break;
        }
    }
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public void render(int windowWidth, int windowHeight) {
        switch (currentState) {
            case MAIN_MENU:
                mainMenu.render(windowWidth, windowHeight);
                break;
            case PAUSE_MENU:
                pauseMenu.render(windowWidth, windowHeight);
                break;
            case WORLD_CREATION:
                worldCreationMenu.render(windowWidth, windowHeight);
                break;
            case WORLD_SELECTION:
                worldSelectionMenu.render(windowWidth, windowHeight);
                break;
            case IN_GAME:
                // Game rendering is handled elsewhere
                break;
        }
    }
    
    public void handleMouseInput(double mouseX, double mouseY, boolean leftClick) {
        switch (currentState) {
            case MAIN_MENU:
                mainMenu.handleMouseInput(mouseX, mouseY, leftClick);
                break;
            case PAUSE_MENU:
                pauseMenu.handleMouseInput(mouseX, mouseY, leftClick);
                break;
            case WORLD_CREATION:
                worldCreationMenu.handleMouseInput(mouseX, mouseY, leftClick);
                break;
            case WORLD_SELECTION:
                worldSelectionMenu.handleMouseInput(mouseX, mouseY, leftClick);
                break;
        }
    }
    
    public void handleKeyInput(int key, int action) {
        switch (currentState) {
            case MAIN_MENU:
                mainMenu.handleKeyInput(key, action);
                break;
            case PAUSE_MENU:
                pauseMenu.handleKeyInput(key, action);
                break;
            case WORLD_CREATION:
                worldCreationMenu.handleKeyInput(key, action);
                break;
            case WORLD_SELECTION:
                worldSelectionMenu.handleKeyInput(key, action);
                break;
            case IN_GAME:
                // Handle in-game key inputs (like ESC for pause)
                break;
        }
    }
    
    public VoxelEngine getVoxelEngine() {
        return voxelEngine;
    }
    
    public void setVoxelEngine(VoxelEngine voxelEngine) {
        this.voxelEngine = voxelEngine;
    }
    
    public String getPendingWorldName() {
        return pendingWorldName;
    }
    
    public void setPendingWorldName(String worldName) {
        this.pendingWorldName = worldName;
    }
    
    public long getPendingWorldSeed() {
        return pendingWorldSeed;
    }
    
    public void setPendingWorldSeed(long seed) {
        this.pendingWorldSeed = seed;
    }
}