package com.odyssey.ui;

import com.odyssey.rendering.ui.UIRenderer;
import org.joml.Vector3f;

public class MenuButton {
    private final String text;
    private final float x, y, width, height;
    private boolean hovered = false;
    private boolean pressed = false;
    private final Runnable onClick;
    
    private static final Vector3f NORMAL_COLOR = new Vector3f(0.3f, 0.3f, 0.3f);
    private static final Vector3f HOVER_COLOR = new Vector3f(0.5f, 0.5f, 0.5f);
    private static final Vector3f PRESSED_COLOR = new Vector3f(0.2f, 0.2f, 0.2f);
    private static final Vector3f TEXT_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);
    
    public MenuButton(String text, float x, float y, float width, float height, Runnable onClick) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
    }
    
    public void render(UIRenderer uiRenderer) {
        Vector3f buttonColor = NORMAL_COLOR;
        if (pressed) {
            buttonColor = PRESSED_COLOR;
        } else if (hovered) {
            buttonColor = HOVER_COLOR;
        }
        
        // Convert Vector3f color to int format (ARGB)
        int colorInt = (0xFF << 24) | // Alpha
                      ((int)(buttonColor.x * 255) << 16) | // Red
                      ((int)(buttonColor.y * 255) << 8) |  // Green
                      ((int)(buttonColor.z * 255));        // Blue
        
        // Draw button background
        uiRenderer.drawRect(x, y, width, height, colorInt);
        
        // Calculate text position to center it
        float textX = x + (width - text.length() * 8) / 2; // Rough text centering
        float textY = y + (height - 16) / 2; // Center vertically
        
        uiRenderer.drawText(text, textX, textY, 1.0f, TEXT_COLOR);
    }
    
    public boolean isPointInside(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }
    
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
        if (!pressed && hovered && onClick != null) {
            onClick.run();
        }
    }
    
    public boolean isHovered() {
        return hovered;
    }
    
    public boolean isPressed() {
        return pressed;
    }
}