package com.odyssey.ui;

import com.odyssey.rendering.ui.UIRenderer;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldSelectionMenu {
    private final UIRenderer uiRenderer;
    private final List<MenuButton> buttons;
    private final GameStateManager gameStateManager;
    private final List<String> worldList;
    
    private static final float BUTTON_WIDTH = 300;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 50;
    private static final String WORLDS_DIRECTORY = "saves";
    
    private int selectedWorldIndex = -1;
    
    public WorldSelectionMenu(UIRenderer uiRenderer, GameStateManager gameStateManager) {
        this.uiRenderer = uiRenderer;
        this.gameStateManager = gameStateManager;
        this.buttons = new ArrayList<>();
        this.worldList = new ArrayList<>();
        
        loadWorldList();
        initializeButtons();
    }
    
    private void loadWorldList() {
        worldList.clear();
        
        File savesDir = new File(WORLDS_DIRECTORY);
        if (savesDir.exists() && savesDir.isDirectory()) {
            File[] worldDirs = savesDir.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    worldList.add(worldDir.getName());
                }
            }
        }
        
        // Add some example worlds if no saves exist
        if (worldList.isEmpty()) {
            worldList.add("No saved worlds found");
        }
    }
    
    private void initializeButtons() {
        buttons.clear();
        float centerX = 640; // Assuming 1280 width, center at 640
        float startY = 250;
        
        // World selection buttons
        for (int i = 0; i < Math.min(worldList.size(), 5); i++) { // Show max 5 worlds
            final int worldIndex = i;
            final String worldName = worldList.get(i);
            
            if (!worldName.equals("No saved worlds found")) {
                buttons.add(new MenuButton(worldName, 
                    centerX - BUTTON_WIDTH/2, startY + i * BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
                    () -> {
                        selectedWorldIndex = worldIndex;
                        loadSelectedWorld();
                    }));
            }
        }
        
        // Control buttons
        float controlY = startY + Math.min(worldList.size(), 5) * BUTTON_SPACING + 50;
        
        // Load World button (only if a world is available)
        if (!worldList.isEmpty() && !worldList.get(0).equals("No saved worlds found")) {
            buttons.add(new MenuButton("Load Selected World", 
                centerX - BUTTON_WIDTH/2, controlY, BUTTON_WIDTH, BUTTON_HEIGHT,
                () -> {
                    if (selectedWorldIndex >= 0) {
                        loadSelectedWorld();
                    }
                }));
            controlY += BUTTON_SPACING;
        }
        
        // Delete World button
        if (!worldList.isEmpty() && !worldList.get(0).equals("No saved worlds found")) {
            buttons.add(new MenuButton("Delete Selected World", 
                centerX - BUTTON_WIDTH/2, controlY, BUTTON_WIDTH, BUTTON_HEIGHT,
                () -> {
                    if (selectedWorldIndex >= 0) {
                        deleteSelectedWorld();
                    }
                }));
            controlY += BUTTON_SPACING;
        }
        
        // Back button
        buttons.add(new MenuButton("Back", 
            centerX - BUTTON_WIDTH/2, controlY, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.MAIN_MENU)));
    }
    
    private void loadSelectedWorld() {
        if (selectedWorldIndex >= 0 && selectedWorldIndex < worldList.size()) {
            String worldName = worldList.get(selectedWorldIndex);
            // TODO: Implement actual world loading
            gameStateManager.setPendingWorldName(worldName);
            gameStateManager.setState(GameState.IN_GAME);
        }
    }
    
    private void deleteSelectedWorld() {
        if (selectedWorldIndex >= 0 && selectedWorldIndex < worldList.size()) {
            String worldName = worldList.get(selectedWorldIndex);
            // TODO: Implement world deletion with confirmation dialog
            File worldDir = new File(WORLDS_DIRECTORY, worldName);
            if (worldDir.exists()) {
                deleteDirectory(worldDir);
                loadWorldList(); // Refresh the list
                initializeButtons(); // Rebuild buttons
                selectedWorldIndex = -1;
            }
        }
    }
    
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    
    public void render(int windowWidth, int windowHeight) {
        float centerX = windowWidth / 2.0f;
        
        // Draw title
        String title = "Select World";
        float titleX = (windowWidth - title.length() * 16) / 2;
        uiRenderer.drawText(title, titleX, 150, 2.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        
        // Draw instructions
        if (worldList.isEmpty() || worldList.get(0).equals("No saved worlds found")) {
            String noWorlds = "No saved worlds found. Create a new world first.";
            float noWorldsX = (windowWidth - noWorlds.length() * 8) / 2;
            uiRenderer.drawText(noWorlds, noWorldsX, 200, 1.0f, new Vector3f(0.8f, 0.8f, 0.8f));
        } else {
            String instructions = "Click on a world to select it, then click Load or Delete";
            float instructionsX = (windowWidth - instructions.length() * 8) / 2;
            uiRenderer.drawText(instructions, instructionsX, 200, 1.0f, new Vector3f(0.8f, 0.8f, 0.8f));
        }
        
        // Render buttons with selection highlighting
        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            
            // Highlight selected world
            if (i < worldList.size() && i == selectedWorldIndex) {
                // TODO: Draw selection background
            }
            
            button.render(uiRenderer);
        }
    }
    
    public void handleMouseInput(double mouseX, double mouseY, boolean leftClick) {
        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            boolean isHovered = button.isPointInside((float)mouseX, (float)mouseY);
            button.setHovered(isHovered);
            
            if (leftClick && isHovered) {
                button.setPressed(true);
                
                // If this is a world selection button, update selected index
                if (i < worldList.size() && !worldList.get(i).equals("No saved worlds found")) {
                    selectedWorldIndex = i;
                }
            } else if (!leftClick && button.isPressed()) {
                button.setPressed(false);
            }
        }
    }
    
    public void handleKeyInput(int key, int action) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            gameStateManager.setState(GameState.MAIN_MENU);
        }
        
        // Arrow key navigation
        if (action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_UP && selectedWorldIndex > 0) {
                selectedWorldIndex--;
            } else if (key == GLFW.GLFW_KEY_DOWN && selectedWorldIndex < worldList.size() - 1) {
                selectedWorldIndex++;
            } else if (key == GLFW.GLFW_KEY_ENTER && selectedWorldIndex >= 0) {
                loadSelectedWorld();
            }
        }
    }
}