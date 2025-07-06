package com.odyssey.ui.components;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.ui.layout.LayoutManager;
import com.odyssey.ui.layout.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * A container component that can hold and layout other UI components
 */
public class Panel extends UIComponent {
    
    private LayoutManager layoutManager;
    private List<UIComponent> children;
    private boolean clipChildren = true;
    
    // Border properties
    private int borderColor = 0x00000000; // Transparent by default
    private int borderWidth = 0;
    
    public Panel() {
        super();
        initializePanel();
    }
    
    public Panel(LayoutManager layoutManager) {
        super();
        this.layoutManager = layoutManager;
        initializePanel();
    }
    
    private void initializePanel() {
        children = new ArrayList<>();
        
        // Default to FrameLayout if no layout manager specified
        if (layoutManager == null) {
            layoutManager = new FrameLayout(this);
        }
        
        setFocusable(false);
        setClickable(false);
    }
    
    public void setLayoutManager(LayoutManager layoutManager) {
        if (layoutManager == null) {
            throw new IllegalArgumentException("Layout manager cannot be null");
        }
        
        this.layoutManager = layoutManager;
        requestLayout();
    }
    
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    public void setClipChildren(boolean clipChildren) {
        this.clipChildren = clipChildren;
        invalidate();
    }
    
    public boolean isClipChildren() {
        return clipChildren;
    }
    
    public void setBorderColor(int color) {
        this.borderColor = color;
        invalidate();
    }
    
    public int getBorderColor() {
        return borderColor;
    }
    
    public void setBorderWidth(int width) {
        this.borderWidth = Math.max(0, width);
        invalidate();
    }
    
    public int getBorderWidth() {
        return borderWidth;
    }
    
    // Child management methods
    public void addChild(UIComponent child) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        
        if (child.getParent() != null) {
            child.getParent().removeChild(child);
        }
        
        children.add(child);
        child.setParent(this);
        requestLayout();
    }
    
    public void addChild(UIComponent child, LayoutManager.LayoutParams layoutParams) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        
        child.setLayoutParams(layoutParams);
        addChild(child);
    }
    
    public void removeChild(UIComponent child) {
        if (child != null && children.remove(child)) {
            child.setParent(null);
            requestLayout();
        }
    }
    
    public void removeChildAt(int index) {
        if (index >= 0 && index < children.size()) {
            UIComponent child = children.remove(index);
            child.setParent(null);
            requestLayout();
        }
    }
    
    public void removeAllChildren() {
        for (UIComponent child : children) {
            child.setParent(null);
        }
        children.clear();
        requestLayout();
    }
    
    public UIComponent getChild(int index) {
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }
    
    public int getChildCount() {
        return children.size();
    }
    
    public List<UIComponent> getChildren() {
        return new ArrayList<>(children);
    }
    
    public int indexOfChild(UIComponent child) {
        return children.indexOf(child);
    }
    
    @Override
    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
        if (layoutManager != null) {
            layoutManager.measureChildren(widthMeasureSpec, heightMeasureSpec);
        } else {
            // Fallback measurement
            setMeasuredDimension(
                LayoutManager.MeasureSpec.getSize(widthMeasureSpec),
                LayoutManager.MeasureSpec.getSize(heightMeasureSpec)
            );
        }
    }
    
    @Override
    public void layout(float left, float top, float right, float bottom) {
        super.layout(left, top, right, bottom);
        
        if (layoutManager != null) {
            layoutManager.layoutChildren((int)left, (int)top, (int)right, (int)bottom);
        }
    }
    
    @Override
    public void render(UIRenderer renderer, float deltaTime) {
        if (!isVisible()) return;
        
        // Draw background
        if (getBackgroundColor() != 0) {
            if (getCornerRadius() > 0) {
                renderer.drawRoundedRect(
                    getLeft(), getTop(), getRight(), getBottom(),
                    getCornerRadius(), getBackgroundColor()
                );
            } else {
                renderer.drawRect(
                    getLeft(), getTop(), getRight(), getBottom(),
                    getBackgroundColor()
                );
            }
        }
        
        // Draw border
        if (borderWidth > 0 && (borderColor & 0xFF000000) != 0) {
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
        
        // Set up clipping if enabled
        boolean wasClipping = false;
        if (clipChildren) {
            wasClipping = renderer.isClipping();
            renderer.pushClip(getLeft(), getTop(), getRight(), getBottom());
        }
        
        // Render children
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.render(renderer, deltaTime);
            }
        }
        
        // Restore clipping state
        if (clipChildren) {
            renderer.popClip();
        }
    }
    
    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }
        
        // Check children first (reverse order for proper z-ordering)
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.onMouseClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Handle panel click if clickable
        if (isClickable() && contains((int)mouseX, (int)mouseY)) {
            performClick();
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onMouseMove(double mouseX, double mouseY) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }
        
        boolean handled = false;
        
        // Update hover state for children
        for (UIComponent child : children) {
            if (child.onMouseMove(mouseX, mouseY)) {
                handled = true;
            }
        }
        
        // Update own hover state
        boolean wasHovered = isHovered();
        boolean nowHovered = contains((int)mouseX, (int)mouseY);
        
        if (wasHovered != nowHovered) {
            setHovered(nowHovered);
            if (getOnHoverListener() != null) {
                getOnHoverListener().onHoverChange(this, nowHovered);
            }
            handled = true;
        }
        
        return handled;
    }
    
    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }
        
        // Forward to focused child first
        for (UIComponent child : children) {
            if (child.isFocused() && child.onKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        return super.onKeyPress(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean onCharInput(int codepoint) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }

        // Forward to focused child first
        for (UIComponent child : children) {
            if (child.isFocused() && child.onCharInput(codepoint)) {
                return true;
            }
        }

        return super.onCharInput(codepoint);
    }
    
    @Override
    public boolean onScroll(double xOffset, double yOffset) {
        if (!isVisible() || !isEnabled()) {
            return false;
        }

        // Forward to children
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.onScroll(xOffset, yOffset)) {
                return true;
            }
        }

        return super.onScroll(xOffset, yOffset);
    }
    
    /**
     * Find the topmost child component at the given coordinates
     */
    public UIComponent findChildAt(int x, int y) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.contains(x, y)) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * Find all child components at the given coordinates
     */
    public List<UIComponent> findChildrenAt(int x, int y) {
        List<UIComponent> result = new ArrayList<>();
        for (UIComponent child : children) {
            if (child.isVisible() && child.contains(x, y)) {
                result.add(child);
            }
        }
        return result;
    }
    
    /**
     * Builder pattern for easy panel creation
     */
    public static class Builder {
        private Panel panel;
        
        public Builder() {
            panel = new Panel();
        }
        
        public Builder(LayoutManager layoutManager) {
            panel = new Panel(layoutManager);
        }
        
        public Builder setLayoutManager(LayoutManager layoutManager) {
            panel.setLayoutManager(layoutManager);
            return this;
        }
        
        public Builder setSize(float width, float height) {
            panel.setSize(width, height);
            return this;
        }
        
        public Builder setBackgroundColor(int color) {
            panel.setBackgroundColor(color);
            return this;
        }
        
        public Builder setBorderColor(int color) {
            panel.setBorderColor(color);
            return this;
        }
        
        public Builder setBorderWidth(int width) {
            panel.setBorderWidth(width);
            return this;
        }
        
        public Builder setCornerRadius(float radius) {
            panel.setCornerRadius(radius);
            return this;
        }
        
        public Builder setPadding(int padding) {
            panel.setPadding(padding, padding, padding, padding);
            return this;
        }
        
        public Builder setPadding(int left, int top, int right, int bottom) {
            panel.setPadding(left, top, right, bottom);
            return this;
        }
        
        public Builder setClipChildren(boolean clipChildren) {
            panel.setClipChildren(clipChildren);
            return this;
        }
        
        public Builder addChild(UIComponent child) {
            panel.addChild(child);
            return this;
        }
        
        public Builder addChild(UIComponent child, LayoutManager.LayoutParams layoutParams) {
            panel.addChild(child, layoutParams);
            return this;
        }
        
        public Panel build() {
            return panel;
        }
    }
}