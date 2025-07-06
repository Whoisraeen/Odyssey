package com.odyssey.ui;

import com.odyssey.ui.UIRenderer;
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
        // Buttons will be created dynamically in updateButtonPositions method
        // based on actual window size
    }
    
    public void render(int windowWidth, int windowHeight) {
        System.out.println("DEBUG: MainMenu.render() called with window size: " + windowWidth + "x" + windowHeight);
        System.out.println("DEBUG: UIRenderer text rendering available: " + uiRenderer.isTextRenderingAvailable());
        
        // Update button positions based on current window size
        updateButtonPositions(windowWidth, windowHeight);
        
        // Draw title
        String title = "ODYSSEY";
        float titleX = (windowWidth - title.length() * 24) / 2.0f; // Rough centering for larger text
        System.out.println("DEBUG: Drawing title '" + title + "' at (" + titleX + ", 150)");
        uiRenderer.drawText(title, titleX, 150.0f, 3.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        
        // Draw subtitle
        String subtitle = "Voxel Adventure Game";
        float subtitleX = (windowWidth - subtitle.length() * 8) / 2.0f;
        System.out.println("DEBUG: Drawing subtitle '" + subtitle + "' at (" + subtitleX + ", 200)");
        uiRenderer.drawText(subtitle, subtitleX, 200.0f, 1.0f, new Vector3f(0.8f, 0.8f, 0.8f));
        
        // Draw a test bright red rectangle to verify basic rendering
        System.out.println("DEBUG: Drawing test rectangle");
        uiRenderer.drawRect(100, 100, 200, 50, 0xFF0000); // Bright red rectangle
        
        // Render buttons
        System.out.println("DEBUG: Rendering " + buttons.size() + " buttons");
        for (MenuButton button : buttons) {
            button.render(uiRenderer);
        }
    }
    
    private void updateButtonPositions(int windowWidth, int windowHeight) {
        float centerX = windowWidth / 2.0f;
        float startY = windowHeight * 0.4f; // Start buttons at 40% down the screen
        
        // Clear existing buttons and recreate with correct positions
        buttons.clear();
        
        // Quick Play button (directly start game)
        buttons.add(new MenuButton("Quick Play", 
            centerX - BUTTON_WIDTH/2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> {
                System.out.println("DEBUG: Quick Play button clicked, switching to IN_GAME state");
                gameStateManager.setState(GameState.IN_GAME);
            }));
        
        // Start New World button
        buttons.add(new MenuButton("Start New World", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.WORLD_CREATION)));
        
        // Load World button
        buttons.add(new MenuButton("Load World", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.WORLD_SELECTION)));
        
        // Settings button
        buttons.add(new MenuButton("Settings", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.SETTINGS)));
        
        // Exit button
        buttons.add(new MenuButton("Exit", 
            centerX - BUTTON_WIDTH/2, startY + BUTTON_SPACING * 4, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> System.exit(0)));
    }
    
    public void handleMouseInput(double mouseX, double mouseY, boolean leftClick) {
        for (MenuButton button : buttons) {
            boolean wasHovered = button.isHovered();
            boolean isHovered = button.isPointInside((float)mouseX, (float)mouseY);
            
            button.setHovered(isHovered);
            
            if (leftClick && isHovered) {
                button.setPressed(true);
            } else if (!leftClick && button.isPressed()) {
                // This will trigger the onClick action if the button is hovered
                button.setPressed(false);
            }
        }
    }
    
    public void handleKeyInput(int key, int action) {
        // Handle keyboard navigation if needed
        // ESC key handling removed to prevent accidental game exit
        System.out.println("DEBUG: Key pressed: " + key + ", action: " + action);
    }
}