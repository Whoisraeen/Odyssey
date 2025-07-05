package com.odyssey.ui.components;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.rendering.ui.TextRenderer;

/**
 * A clickable button component with text and customizable appearance
 */
public class Button extends UIComponent {
    
    private String text = "";
    private int textColor = 0xFFFFFFFF; // White
    private int textColorHover = 0xFFE0E0E0; // Light gray
    private int textColorPressed = 0xFFC0C0C0; // Darker gray
    private int textColorDisabled = 0xFF808080; // Gray
    
    private int buttonColor = 0xFF404040; // Dark gray
    private int buttonColorHover = 0xFF505050; // Lighter gray
    private int buttonColorPressed = 0xFF303030; // Darker gray
    private int buttonColorDisabled = 0xFF202020; // Very dark gray
    
    private int borderColor = 0xFF606060; // Border color
    private int borderWidth = 1;
    
    private TextRenderer.TextStyle textStyle;
    private boolean autoSize = true;
    private int minButtonWidth = 80;
    private int minButtonHeight = 32;
    
    public Button() {
        super();
        initializeButton();
    }
    
    public Button(String text) {
        super();
        this.text = text;
        initializeButton();
    }
    
    private void initializeButton() {
        setFocusable(true);
        setClickable(true);
        
        // Create default text style
        textStyle = new TextRenderer.TextStyle()
            .setColor(textColor)
            .setAlignment(TextRenderer.TextStyle.Alignment.CENTER);
        
        // Set default corner radius for modern look
        setCornerRadius(4.0f);
        
        // Calculate initial size if auto-sizing
        if (autoSize) {
            updateSize();
        }
    }
    
    public void setText(String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            if (autoSize) {
                updateSize();
            }
            invalidate();
        }
    }
    
    public String getText() {
        return text;
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
        updateTextStyle();
        invalidate();
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void setTextColorHover(int color) {
        this.textColorHover = color;
        invalidate();
    }
    
    public void setTextColorPressed(int color) {
        this.textColorPressed = color;
        invalidate();
    }
    
    public void setTextColorDisabled(int color) {
        this.textColorDisabled = color;
        invalidate();
    }
    
    public void setButtonColor(int color) {
        this.buttonColor = color;
        invalidate();
    }
    
    public int getButtonColor() {
        return buttonColor;
    }
    
    public void setButtonColorHover(int color) {
        this.buttonColorHover = color;
        invalidate();
    }
    
    public void setButtonColorPressed(int color) {
        this.buttonColorPressed = color;
        invalidate();
    }
    
    public void setButtonColorDisabled(int color) {
        this.buttonColorDisabled = color;
        invalidate();
    }
    
    public void setBorderColor(int color) {
        this.borderColor = color;
        invalidate();
    }
    
    public void setBorderWidth(int width) {
        this.borderWidth = width;
        invalidate();
    }
    
    public void setAutoSize(boolean autoSize) {
        this.autoSize = autoSize;
        if (autoSize) {
            updateSize();
        }
    }
    
    public boolean isAutoSize() {
        return autoSize;
    }
    
    public void setMinButtonSize(int width, int height) {
        this.minButtonWidth = width;
        this.minButtonHeight = height;
        if (autoSize) {
            updateSize();
        }
    }
    
    private void updateSize() {
        if (text.isEmpty()) {
            setSize(minButtonWidth, minButtonHeight);
            return;
        }
        
        // Calculate text dimensions (simplified - in real implementation use TextRenderer)
        int textWidth = text.length() * 8; // Approximate character width
        int textHeight = 16; // Approximate character height
        
        // Add padding
        int padding = 16;
        int buttonWidth = Math.max(minButtonWidth, textWidth + padding * 2);
        int buttonHeight = Math.max(minButtonHeight, textHeight + padding);
        
        setSize(buttonWidth, buttonHeight);
    }
    
    private void updateTextStyle() {
        int currentTextColor = getCurrentTextColor();
        textStyle.setColor(currentTextColor);
    }
    
    private int getCurrentTextColor() {
        if (!isEnabled()) {
            return textColorDisabled;
        } else if (isPressed()) {
            return textColorPressed;
        } else if (isHovered()) {
            return textColorHover;
        } else {
            return textColor;
        }
    }
    
    private int getCurrentButtonColor() {
        if (!isEnabled()) {
            return buttonColorDisabled;
        } else if (isPressed()) {
            return buttonColorPressed;
        } else if (isHovered()) {
            return buttonColorHover;
        } else {
            return buttonColor;
        }
    }
    
    @Override
    public void render(UIRenderer renderer) {
        if (!isVisible()) return;
        
        int currentButtonColor = getCurrentButtonColor();
        
        // Draw button background
        if (getCornerRadius() > 0) {
            renderer.drawRoundedRect(
                getLeft(), getTop(), getRight(), getBottom(),
                getCornerRadius(), currentButtonColor
            );
        } else {
            renderer.drawRect(
                getLeft(), getTop(), getRight(), getBottom(),
                currentButtonColor
            );
        }
        
        // Draw border if specified
        if (borderWidth > 0) {
            if (getCornerRadius() > 0) {
                renderer.drawRoundedRectOutline(
                    getLeft(), getTop(), getRight(), getBottom(),
                    getCornerRadius(), borderColor, borderWidth
                );
            } else {
                renderer.drawRectOutline(
                    getLeft(), getTop(), getRight(), getBottom(),
                    borderColor, borderWidth
                );
            }
        }
        
        // Draw text
        if (!text.isEmpty()) {
            updateTextStyle();
            
            int textX = getLeft() + getWidth() / 2;
            int textY = getTop() + getHeight() / 2;
            
            // Note: In a real implementation, you'd use the actual TextRenderer
            // renderer.drawText(text, textX, textY, textStyle);
        }
        
        // Draw focus indicator
        if (isFocused()) {
            int focusColor = 0xFF00AAFF; // Blue focus color
            renderer.drawRectOutline(
                getLeft() - 2, getTop() - 2, getRight() + 2, getBottom() + 2,
                focusColor, 2
            );
        }
    }
    
    @Override
    protected void onStateChanged() {
        super.onStateChanged();
        invalidate(); // Redraw when state changes
    }
    
    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        if (!isEnabled() || !isVisible()) {
            return false;
        }
        
        if (contains(mouseX, mouseY)) {
            // Request focus
            requestFocus();
            
            // Trigger click
            performClick();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isVisible() || !isFocused()) {
            return false;
        }
        
        // Handle Enter and Space as click
        if (keyCode == 257 || keyCode == 32) { // GLFW_KEY_ENTER or GLFW_KEY_SPACE
            performClick();
            return true;
        }
        
        return super.onKeyPress(keyCode, scanCode, modifiers);
    }
    
    /**
     * Builder pattern for easy button creation
     */
    public static class Builder {
        private Button button;
        
        public Builder() {
            button = new Button();
        }
        
        public Builder(String text) {
            button = new Button(text);
        }
        
        public Builder setText(String text) {
            button.setText(text);
            return this;
        }
        
        public Builder setTextColor(int color) {
            button.setTextColor(color);
            return this;
        }
        
        public Builder setButtonColor(int color) {
            button.setButtonColor(color);
            return this;
        }
        
        public Builder setSize(int width, int height) {
            button.setSize(width, height);
            button.setAutoSize(false);
            return this;
        }
        
        public Builder setCornerRadius(float radius) {
            button.setCornerRadius(radius);
            return this;
        }
        
        public Builder setOnClickListener(OnClickListener listener) {
            button.setOnClickListener(listener);
            return this;
        }
        
        public Builder setEnabled(boolean enabled) {
            button.setEnabled(enabled);
            return this;
        }
        
        public Button build() {
            return button;
        }
    }
}