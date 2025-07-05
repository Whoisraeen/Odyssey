package com.odyssey.ui;

import com.odyssey.rendering.ui.UIRenderer;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PauseMenu {
    private final UIRenderer uiRenderer;
    private final List<MenuButton> buttons;
    private final GameStateManager gameStateManager;
    
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 60;
    
    public PauseMenu(UIRenderer uiRenderer, GameStateManager gameStateManager) {
        this.uiRenderer = uiRenderer;
        this.gameStateManager = gameStateManager;
        this.buttons = new ArrayList<>();
        initializeButtons();
    }
    
    private void initializeButtons() {
        float centerX = 640; // Assuming 1280 width, center at 640
        float startY = 300;
        
        // Resume button
        buttons.add(new MenuButton("Resume", 
            centerX - BUTTON_WIDTH/2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.IN_GAME)));
        
        // Save and Exit button
        buttons.add(new MenuButton("Save and Exit", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> {
                // TODO: Implement save functionality
                gameStateManager.setState(GameState.MAIN_MENU);
            }));
        
        // Settings button
        buttons.add(new MenuButton("Settings", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.SETTINGS)));
        
        // Quit to Main Menu button
        buttons.add(new MenuButton("Quit to Main Menu", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.MAIN_MENU)));
    }
    
    public void render(int windowWidth, int windowHeight) {
        // Draw semi-transparent background
        // TODO: Implement proper background overlay
        
        // Draw title
        String title = "PAUSED";
        float titleX = (windowWidth - title.length() * 16) / 2; // Rough centering
        uiRenderer.drawText(title, titleX, 200, 2.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        
        // Render buttons
        for (MenuButton button : buttons) {
            button.render(uiRenderer);
        }
    }
    
    public void handleMouseInput(double mouseX, double mouseY, boolean leftClick) {
        for (MenuButton button : buttons) {
            boolean wasHovered = button.isHovered();
            boolean isHovered = button.isPointInside((float)mouseX, (float)mouseY);
            
            button.setHovered(isHovered);
            
            if (leftClick && isHovered) {
                button.setPressed(true);
            } else if (!leftClick && button.isPressed()) {
                button.setPressed(false);
            }
        }
    }
    
    public void handleKeyInput(int key, int action) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            gameStateManager.setState(GameState.IN_GAME);
        }
    }
}