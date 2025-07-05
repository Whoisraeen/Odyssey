package com.odyssey.ui;

import com.odyssey.rendering.ui.UIRenderer;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WorldCreationMenu {
    private final UIRenderer uiRenderer;
    private final List<MenuButton> buttons;
    private final GameStateManager gameStateManager;
    
    private static final float BUTTON_WIDTH = 200;
    private static final float BUTTON_HEIGHT = 40;
    private static final float INPUT_WIDTH = 300;
    private static final float INPUT_HEIGHT = 30;
    
    private String worldName = "New World";
    private String seedInput = "";
    private boolean editingWorldName = false;
    private boolean editingSeed = false;
    
    public WorldCreationMenu(UIRenderer uiRenderer, GameStateManager gameStateManager) {
        this.uiRenderer = uiRenderer;
        this.gameStateManager = gameStateManager;
        this.buttons = new ArrayList<>();
        initializeButtons();
    }
    
    private void initializeButtons() {
        float centerX = 640; // Assuming 1280 width, center at 640
        float startY = 450;
        
        // Create World button
        buttons.add(new MenuButton("Create World", 
            centerX - BUTTON_WIDTH/2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> {
                gameStateManager.setPendingWorldName(worldName);
                if (!seedInput.isEmpty()) {
                    try {
                        long seed = Long.parseLong(seedInput);
                        gameStateManager.setPendingWorldSeed(seed);
                    } catch (NumberFormatException e) {
                        // Use string hash as seed
                        gameStateManager.setPendingWorldSeed(seedInput.hashCode());
                    }
                }
                gameStateManager.setState(GameState.IN_GAME);
            }));
        
        // Back button
        buttons.add(new MenuButton("Back", 
            centerX - BUTTON_WIDTH/2, startY + 60, BUTTON_WIDTH, BUTTON_HEIGHT,
            () -> gameStateManager.setState(GameState.MAIN_MENU)));
    }
    
    public void render(int windowWidth, int windowHeight) {
        float centerX = windowWidth / 2.0f;
        
        // Draw title
        String title = "Create New World";
        float titleX = (windowWidth - title.length() * 16) / 2;
        uiRenderer.drawText(title, titleX, 150, 2.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        
        // World Name input
        uiRenderer.drawText("World Name:", centerX - INPUT_WIDTH/2, 250, 1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        drawInputField(centerX - INPUT_WIDTH/2, 280, INPUT_WIDTH, INPUT_HEIGHT, worldName, editingWorldName);
        
        // Seed input
        uiRenderer.drawText("Seed (optional):", centerX - INPUT_WIDTH/2, 330, 1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        String seedDisplay = seedInput.isEmpty() ? "Random" : seedInput;
        drawInputField(centerX - INPUT_WIDTH/2, 360, INPUT_WIDTH, INPUT_HEIGHT, seedDisplay, editingSeed);
        
        // Render buttons
        for (MenuButton button : buttons) {
            button.render(uiRenderer);
        }
    }
    
    private void drawInputField(float x, float y, float width, float height, String text, boolean active) {
        // Draw input field background (simple rectangle)
        Vector3f bgColor = active ? new Vector3f(0.3f, 0.3f, 0.4f) : new Vector3f(0.2f, 0.2f, 0.2f);
        // TODO: Implement proper rectangle drawing in UIRenderer
        
        // Draw text
        Vector3f textColor = new Vector3f(1.0f, 1.0f, 1.0f);
        uiRenderer.drawText(text, x + 5, y + 5, 1.0f, textColor);
        
        // Draw cursor if active
        if (active) {
            float cursorX = x + 5 + text.length() * 8; // Rough character width
            uiRenderer.drawText("|", cursorX, y + 5, 1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
        }
    }
    
    public void handleMouseInput(double mouseX, double mouseY, boolean leftClick) {
        float centerX = 640;
        
        // Check input field clicks
        if (leftClick) {
            // World name field
            if (mouseX >= centerX - INPUT_WIDTH/2 && mouseX <= centerX + INPUT_WIDTH/2 &&
                mouseY >= 280 && mouseY <= 280 + INPUT_HEIGHT) {
                editingWorldName = true;
                editingSeed = false;
            }
            // Seed field
            else if (mouseX >= centerX - INPUT_WIDTH/2 && mouseX <= centerX + INPUT_WIDTH/2 &&
                     mouseY >= 360 && mouseY <= 360 + INPUT_HEIGHT) {
                editingSeed = true;
                editingWorldName = false;
            }
            else {
                editingWorldName = false;
                editingSeed = false;
            }
        }
        
        // Handle button clicks
        for (MenuButton button : buttons) {
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
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            if (editingWorldName) {
                handleTextInput(key, true);
            } else if (editingSeed) {
                handleTextInput(key, false);
            }
        }
        
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            gameStateManager.setState(GameState.MAIN_MENU);
        }
    }
    
    private void handleTextInput(int key, boolean isWorldName) {
        String target = isWorldName ? worldName : seedInput;
        
        if (key == GLFW.GLFW_KEY_BACKSPACE && !target.isEmpty()) {
            target = target.substring(0, target.length() - 1);
        } else if (key >= 32 && key <= 126) { // Printable ASCII characters
            char c = (char) key;
            if (target.length() < 20) { // Limit input length
                target += c;
            }
        }
        
        if (isWorldName) {
            worldName = target;
        } else {
            seedInput = target;
        }
    }
}