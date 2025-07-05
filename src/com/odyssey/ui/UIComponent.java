package com.odyssey.ui;

import com.odyssey.ui.layout.LayoutManager;
import com.odyssey.ui.layout.MeasureSpec;
import com.odyssey.ui.animation.Animator;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all UI components in the modern UI system
 * Inspired by Android View system and ModernUI architecture
 */
public abstract class UIComponent {
    
    // Position and size
    protected float x, y;
    protected float width, height;
    protected float measuredWidth, measuredHeight;
    protected float minWidth = 0.0f, minHeight = 0.0f;
    
    // Layout
    protected LayoutManager.LayoutParams layoutParams;
    protected UIComponent parent;
    protected List<UIComponent> children = new ArrayList<>();
    
    // Visibility enum
    public enum Visibility {
        VISIBLE,
        INVISIBLE,
        GONE
    }
    
    // State
    protected boolean visible = true;
    protected Visibility visibility = Visibility.VISIBLE;
    protected boolean enabled = true;
    protected boolean focusable = false;
    protected boolean focused = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean floating = false;
    protected boolean clickable = false;
    
    // Styling
    protected float alpha = 1.0f;
    protected int backgroundColor = 0x00000000; // Transparent by default
    protected float cornerRadius = 0.0f;
    protected float elevation = 0.0f;
    protected float scaleX = 1.0f;
    protected float scaleY = 1.0f;
    
    // Animation
    protected List<Animator> animators = new ArrayList<>();
    
    // Padding and margin
    protected float paddingLeft, paddingTop, paddingRight, paddingBottom;
    protected float marginLeft, marginTop, marginRight, marginBottom;
    
    // Event listeners
    protected OnClickListener onClickListener;
    protected OnHoverListener onHoverListener;
    protected OnFocusChangeListener onFocusChangeListener;
    
    public UIComponent() {
        this.layoutParams = new LayoutManager.LayoutParams();
    }
    
    public UIComponent(float x, float y, float width, float height) {
        this.layoutParams = new LayoutManager.LayoutParams();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    // Abstract methods that subclasses must implement
    public abstract void render(UIRenderer renderer, float deltaTime);
    
    /**
     * Measure the component
     * @param widthSpec width constraint
     * @param heightSpec height constraint
     */
    public void measure(int widthSpec, int heightSpec) {
        measuredWidth = LayoutManager.MeasureSpec.getSize(widthSpec);
        measuredHeight = LayoutManager.MeasureSpec.getSize(heightSpec);
        
        onMeasure(widthSpec, heightSpec);
    }
    
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Layout the component and its children
     * @param left left position
     * @param top top position
     * @param right right position
     * @param bottom bottom position
     */
    public void layout(float left, float top, float right, float bottom) {
        boolean changed = (this.x != left || this.y != top || 
                          this.width != (right - left) || this.height != (bottom - top));
        
        this.x = left;
        this.y = top;
        this.width = right - left;
        this.height = bottom - top;
        
        if (changed) {
            onLayout(changed, left, top, right, bottom);
        }
    }
    
    protected void onLayout(boolean changed, float left, float top, float right, float bottom) {
        // Default implementation - subclasses can override
    }
    
    // Input handling
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!isPointInside(mouseX, mouseY) || !enabled || !visible) {
            return false;
        }
        
        // Check children first (reverse order for proper z-ordering)
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onMouseClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Handle click on this component
        if (button == 0) { // Left click
            setPressed(true);
            if (onClickListener != null) {
                onClickListener.onClick(this);
                return true;
            }
        }
        
        return false;
    }
    
    public boolean onMouseMove(double mouseX, double mouseY) {
        boolean wasHovered = hovered;
        boolean isInside = isPointInside(mouseX, mouseY) && visible;
        
        setHovered(isInside);
        
        if (isInside != wasHovered && onHoverListener != null) {
            onHoverListener.onHoverChange(this, isInside);
        }
        
        // Propagate to children
        for (UIComponent child : children) {
            child.onMouseMove(mouseX, mouseY);
        }
        
        return isInside;
    }
    
    public boolean onKeyPress(int key, int scancode, int mods) {
        if (!focused || !enabled || !visible) {
            return false;
        }
        
        // Check focused child first
        for (UIComponent child : children) {
            if (child.focused && child.onKeyPress(key, scancode, mods)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean onCharInput(int codepoint) {
        if (!focused || !enabled || !visible) {
            return false;
        }
        
        // Check focused child first
        for (UIComponent child : children) {
            if (child.focused && child.onCharInput(codepoint)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean onScroll(double xOffset, double yOffset) {
        if (!isPointInside(UIManager.getInstance().getInputManager().getMouseX(),
                UIManager.getInstance().getInputManager().getMouseY()) || 
            !enabled || !visible) {
            return false;
        }
        
        // Check children first
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onScroll(xOffset, yOffset)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Utility methods
    protected boolean isPointInside(double pointX, double pointY) {
        return pointX >= x && pointX <= x + width && 
               pointY >= y && pointY <= y + height;
    }
    
    public boolean contains(int x, int y) {
        return isPointInside(x, y);
    }
    
    public boolean contains(float x, float y) {
        return isPointInside(x, y);
    }
    
    // Child management
    public void addChild(UIComponent child) {
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        
        children.add(child);
        child.parent = this;
        requestLayout();
    }
    
    public void removeChild(UIComponent child) {
        if (children.remove(child)) {
            child.parent = null;
            requestLayout();
        }
    }
    
    public void removeAllChildren() {
        for (UIComponent child : children) {
            child.parent = null;
        }
        children.clear();
        requestLayout();
    }
    
    public List<UIComponent> getChildren() {
        return new ArrayList<>(children);
    }
    
    // Animation
    public void startAnimation(Animator animator) {
        animators.add(animator);
        animator.setTarget(this);
        animator.start();
    }
    
    public void clearAnimations() {
        for (Animator animator : animators) {
            animator.cancel();
        }
        animators.clear();
    }
    
    // Layout request
    public void requestLayout() {
        UIManager.getInstance().getLayoutManager().requestLayout();
    }
    
    public void invalidate() {
        // Mark for redraw - in a real implementation this would be more sophisticated
    }
    
    // Getters and setters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getMeasuredWidth() { return measuredWidth; }
    public float getMeasuredHeight() { return measuredHeight; }
    
    public void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;
    }
    
    // Position methods for layout
    public float getLeft() { return x; }
    public float getTop() { return y; }
    public float getRight() { return x + width; }
    public float getBottom() { return y + height; }
    
    public float getMinWidth() { return minWidth; }
    public float getMinHeight() { return minHeight; }
    
    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
        requestLayout();
    }
    
    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
        requestLayout();
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { 
        this.visible = visible;
        this.visibility = visible ? Visibility.VISIBLE : Visibility.INVISIBLE;
        invalidate();
    }
    
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
        this.visible = (visibility == Visibility.VISIBLE);
        invalidate();
    }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { 
        this.enabled = enabled;
        invalidate();
    }
    
    public boolean isFocusable() { return focusable; }
    public void setFocusable(boolean focusable) { this.focusable = focusable; }
    
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
            if (onFocusChangeListener != null) {
                onFocusChangeListener.onFocusChange(this, focused);
            }
            invalidate();
        }
    }
    
    public boolean isHovered() { return hovered; }
    protected void setHovered(boolean hovered) {
        this.hovered = hovered;
        invalidate();
    }
    
    public boolean isPressed() { return pressed; }
    protected void setPressed(boolean pressed) {
        this.pressed = pressed;
        invalidate();
    }
    
    public boolean isFloating() { return floating; }
    public void setFloating(boolean floating) { this.floating = floating; }
    
    public boolean isClickable() { return clickable; }
    public void setClickable(boolean clickable) { this.clickable = clickable; }
    
    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        invalidate();
    }
    
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }
    
    public float getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        invalidate();
    }
    
    public float getElevation() { return elevation; }
    public void setElevation(float elevation) {
        this.elevation = elevation;
        invalidate();
    }
    
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public void setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        invalidate();
    }
    
    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        invalidate();
    }
    
    // Padding
    public void setPadding(float padding) {
        setPadding(padding, padding, padding, padding);
    }
    
    public void setPadding(float left, float top, float right, float bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        requestLayout();
    }
    
    public float getPaddingLeft() { return paddingLeft; }
    public float getPaddingTop() { return paddingTop; }
    public float getPaddingRight() { return paddingRight; }
    public float getPaddingBottom() { return paddingBottom; }
    
    // Margin
    public void setMargin(float margin) {
        setMargin(margin, margin, margin, margin);
    }
    
    public void setMargin(float left, float top, float right, float bottom) {
        this.marginLeft = left;
        this.marginTop = top;
        this.marginRight = right;
        this.marginBottom = bottom;
        requestLayout();
    }
    
    // Event listeners
    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }
    
    public void setOnHoverListener(OnHoverListener listener) {
        this.onHoverListener = listener;
    }
    
    public void setOnFocusChangeListener(OnFocusChangeListener listener) {
        this.onFocusChangeListener = listener;
    }
    
    // Layout params
    public LayoutManager.LayoutParams getLayoutParams() { return layoutParams; }
    public void setLayoutParams(LayoutManager.LayoutParams layoutParams) {
        this.layoutParams = layoutParams;
        requestLayout();
    }
    
    // Event listener interfaces
    public interface OnClickListener {
        void onClick(UIComponent component);
    }
    
    public interface OnHoverListener {
        void onHoverChange(UIComponent component, boolean hovered);
    }
    
    public interface OnFocusChangeListener {
        void onFocusChange(UIComponent component, boolean focused);
    }
}