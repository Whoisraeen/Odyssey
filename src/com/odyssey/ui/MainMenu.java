package com.odyssey.ui;

import com.odyssey.rendering.ui.UIRenderer;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MainMenu {
    private final UIRenderer uiRenderer;
    private final List<MenuButton> buttons;
    private final GameStateManager gameStateManager;
    
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 40;
    private static final float BUTTON_SPACING = 60;
    
    public MainMenu(UIRenderer uiRenderer, GameStateManager gameStateManager) {
        this.uiRenderer = uiRenderer;
        this.gameStateManager = gameStateManager;
        this.buttons = new ArrayList<>();
        initializeButtons();
    }
    
    private void initializeButtons() {
        float centerX = 640; // Assuming 1280 width, center at 640
        float startY = 300;
        
        // Start New World button
        buttons.add(new MenuButton("Start New World", 
            centerX - BUTTON_WIDTH/2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.WORLD_CREATION)));
        
        // Load World button
        buttons.add(new MenuButton("Load World", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.WORLD_SELECTION)));
        
        // Settings button
        buttons.add(new MenuButton("Settings", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.SETTINGS)));
        
        // Exit button
        buttons.add(new MenuButton("Exit", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> System.exit(0)));
    }
    
    public void render(int windowWidth, int windowHeight) {
        // Draw title
        String title = "ODYSSEY";
        float titleX = (windowWidth - title.length() * 24) / 2; // Rough centering for larger text
        uiRenderer.drawText(title, titleX, 150, 3.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        
        // Draw subtitle
        String subtitle = "Voxel Adventure Game";
        float subtitleX = (windowWidth - subtitle.length() * 8) / 2;
        uiRenderer.drawText(subtitle, subtitleX, 200, 1.0f, new Vector3f(0.8f, 0.8f, 0.8f));
        
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
        // Handle keyboard navigation if needed
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            System.exit(0);
        }
    }
}