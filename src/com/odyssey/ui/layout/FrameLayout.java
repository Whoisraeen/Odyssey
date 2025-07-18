package com.odyssey.ui.layout;

import com.odyssey.ui.UIComponent;

import java.util.List;

/**
 * Frame layout manager that stacks children on top of each other
 * Children can be positioned using gravity within the frame
 */
public class FrameLayout extends LayoutManager {
    
    public enum Gravity {
        TOP_LEFT(0),
        TOP_CENTER(1),
        TOP_RIGHT(2),
        CENTER_LEFT(3),
        CENTER(4),
        CENTER_RIGHT(5),
        BOTTOM_LEFT(6),
        BOTTOM_CENTER(7),
        BOTTOM_RIGHT(8);
        
        private final int value;
        
        Gravity(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public boolean isLeft() {
            return this == TOP_LEFT || this == CENTER_LEFT || this == BOTTOM_LEFT;
        }
        
        public boolean isRight() {
            return this == TOP_RIGHT || this == CENTER_RIGHT || this == BOTTOM_RIGHT;
        }
        
        public boolean isTop() {
            return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
        }
        
        public boolean isBottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
        }
        
        public boolean isCenterHorizontal() {
            return this == TOP_CENTER || this == CENTER || this == BOTTOM_CENTER;
        }
        
        public boolean isCenterVertical() {
            return this == CENTER_LEFT || this == CENTER || this == CENTER_RIGHT;
        }
    }
    
    public static class FrameLayoutParams extends LayoutManager.LayoutParams {
        public Gravity gravity = Gravity.TOP_LEFT;
        
        public FrameLayoutParams() {
            super();
        }
        
        public FrameLayoutParams(int width, int height) {
            super(width, height);
        }
        
        public FrameLayoutParams(int width, int height, Gravity gravity) {
            super(width, height);
            this.gravity = gravity;
        }
        
        public FrameLayoutParams setGravity(Gravity gravity) {
            this.gravity = gravity;
            return this;
        }
    }
    
    private Gravity defaultGravity = Gravity.TOP_LEFT;
    
    public FrameLayout(UIComponent parent) {
        super(parent);
    }
    
    public FrameLayout(UIComponent parent, Gravity defaultGravity) {
        super(parent);
        this.defaultGravity = defaultGravity;
    }
    
    public void setDefaultGravity(Gravity gravity) {
        this.defaultGravity = gravity;
        parent.requestLayout();
    }
    
    public Gravity getDefaultGravity() {
        return defaultGravity;
    }
    
    @Override
    public void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        List<UIComponent> children = getVisibleChildren();
        
        int maxWidth = 0;
        int maxHeight = 0;
        
        // Measure all children
        for (UIComponent child : children) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            
            LayoutParams lp = child.getLayoutParams();
            int childWidth = (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal();
            int childHeight = (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical();
            
            maxWidth = Math.max(maxWidth, childWidth);
            maxHeight = Math.max(maxHeight, childHeight);
        }
        
        // Add padding
        maxWidth += (int)(parent.getPaddingLeft() + parent.getPaddingRight());
        maxHeight += (int)(parent.getPaddingTop() + parent.getPaddingBottom());
        
        // Set measured dimensions
        parent.setMeasuredDimension(
            resolveSize(maxWidth, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        );
    }
    
    @Override
    public void layoutChildren(int left, int top, int right, int bottom) {
        List<UIComponent> children = getVisibleChildren();
        if (children.isEmpty()) return;
        
        int paddingLeft = (int)parent.getPaddingLeft();
        int paddingTop = (int)parent.getPaddingTop();
        int paddingRight = (int)parent.getPaddingRight();
        int paddingBottom = (int)parent.getPaddingBottom();
        
        int parentWidth = right - left;
        int parentHeight = bottom - top;
        int contentWidth = parentWidth - paddingLeft - paddingRight;
        int contentHeight = parentHeight - paddingTop - paddingBottom;
        
        // Layout each child according to its gravity
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            Gravity gravity = defaultGravity;
            
            if (lp instanceof FrameLayoutParams) {
                FrameLayoutParams flp = (FrameLayoutParams) lp;
                gravity = flp.gravity;
            }
            
            int childWidth = (int)child.getMeasuredWidth();
            int childHeight = (int)child.getMeasuredHeight();
            
            // Calculate available space for positioning
            int availableWidth = contentWidth - (int)lp.getTotalMarginHorizontal();
            int availableHeight = contentHeight - (int)lp.getTotalMarginVertical();
            
            // Calculate horizontal position
            int childLeft;
            if (gravity.isLeft()) {
                childLeft = paddingLeft + (int)lp.marginLeft;
            } else if (gravity.isRight()) {
                childLeft = paddingLeft + availableWidth - childWidth + (int)lp.marginLeft;
            } else { // center horizontal
                childLeft = paddingLeft + (int)lp.marginLeft + (availableWidth - childWidth) / 2;
            }
            
            // Calculate vertical position
            int childTop;
            if (gravity.isTop()) {
                childTop = paddingTop + (int)lp.marginTop;
            } else if (gravity.isBottom()) {
                childTop = paddingTop + availableHeight - childHeight + (int)lp.marginTop;
            } else { // center vertical
                childTop = paddingTop + (int)lp.marginTop + (availableHeight - childHeight) / 2;
            }
            
            // Ensure child stays within bounds
            childLeft = Math.max(paddingLeft + (int)lp.marginLeft, 
                       Math.min(childLeft, paddingLeft + contentWidth - childWidth - (int)lp.marginRight));
            childTop = Math.max(paddingTop + (int)lp.marginTop, 
                      Math.min(childTop, paddingTop + contentHeight - childHeight - (int)lp.marginBottom));
            
            // Layout the child
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }
    
    @Override
    public void calculateMinimumSize() {
        List<UIComponent> children = getVisibleChildren();
        
        int minWidth = (int)(parent.getPaddingLeft() + parent.getPaddingRight());
        int minHeight = (int)(parent.getPaddingTop() + parent.getPaddingBottom());
        
        // Frame layout minimum size is the maximum of all children
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            minWidth = Math.max(minWidth, (int)(child.getMinWidth() + lp.getTotalMarginHorizontal() + 
                               parent.getPaddingLeft() + parent.getPaddingRight()));
            minHeight = Math.max(minHeight, (int)(child.getMinHeight() + lp.getTotalMarginVertical() + 
                                parent.getPaddingTop() + parent.getPaddingBottom()));
        }
        
        parent.setMinWidth((float)minWidth);
        parent.setMinHeight((float)minHeight);
    }
    
    /**
     * Helper method to create frame layout params with gravity
     */
    public static FrameLayoutParams createParams(int width, int height, Gravity gravity) {
        return new FrameLayoutParams(width, height, gravity);
    }
    
    /**
     * Helper method to create frame layout params with margins and gravity
     */
    public static FrameLayoutParams createParams(int width, int height, Gravity gravity, 
                                               int marginLeft, int marginTop, int marginRight, int marginBottom) {
        FrameLayoutParams params = new FrameLayoutParams(width, height, gravity);
        params.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        return params;
    }
    
    /**
     * Helper method to create frame layout params with uniform margins and gravity
     */
    public static FrameLayoutParams createParams(int width, int height, Gravity gravity, int margin) {
        return createParams(width, height, gravity, margin, margin, margin, margin);
    }
}