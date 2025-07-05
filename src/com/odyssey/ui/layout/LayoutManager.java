package com.odyssey.ui.layout;

import com.odyssey.ui.UIComponent;

import java.util.List;

/**
 * Base class for layout managers that handle positioning and sizing of UI components
 * Inspired by Android's layout system and ModernUI's layout engine
 */
public abstract class LayoutManager {
    
    public static class LayoutParams {
        public static final int MATCH_PARENT = -1;
        public static final int WRAP_CONTENT = -2;
        
        public int width = WRAP_CONTENT;
        public int height = WRAP_CONTENT;
        public float weight = 0.0f;
        
        // Margins
        public int marginLeft = 0;
        public int marginTop = 0;
        public int marginRight = 0;
        public int marginBottom = 0;
        
        // Padding
        public int paddingLeft = 0;
        public int paddingTop = 0;
        public int paddingRight = 0;
        public int paddingBottom = 0;
        
        public LayoutParams() {}
        
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public LayoutParams margin(int left, int top, int right, int bottom) {
            this.marginLeft = left;
            this.marginTop = top;
            this.marginRight = right;
            this.marginBottom = bottom;
            return this;
        }
        
        public LayoutParams margin(int margin) {
            return margin(margin, margin, margin, margin);
        }
        
        public LayoutParams padding(int left, int top, int right, int bottom) {
            this.paddingLeft = left;
            this.paddingTop = top;
            this.paddingRight = right;
            this.paddingBottom = bottom;
            return this;
        }
        
        public LayoutParams padding(int padding) {
            return padding(padding, padding, padding, padding);
        }
        
        public LayoutParams weight(float weight) {
            this.weight = weight;
            return this;
        }
        
        public int getTotalMarginHorizontal() {
            return marginLeft + marginRight;
        }
        
        public int getTotalMarginVertical() {
            return marginTop + marginBottom;
        }
        
        public int getTotalPaddingHorizontal() {
            return paddingLeft + paddingRight;
        }
        
        public int getTotalPaddingVertical() {
            return paddingTop + paddingBottom;
        }
    }
    
    public static class MeasureSpec {
        public static final int UNSPECIFIED = 0;
        public static final int EXACTLY = 1;
        public static final int AT_MOST = 2;
        
        public static int makeMeasureSpec(int size, int mode) {
            return (size & ~(3 << 30)) | (mode << 30);
        }
        
        public static int getMode(int measureSpec) {
            return (measureSpec >> 30) & 3;
        }
        
        public static int getSize(int measureSpec) {
            return measureSpec & ~(3 << 30);
        }
    }
    
    protected UIComponent parent;
    
    public LayoutManager(UIComponent parent) {
        this.parent = parent;
    }
    
    /**
     * Measure all child components and determine their sizes
     */
    public abstract void measureChildren(int widthMeasureSpec, int heightMeasureSpec);
    
    /**
     * Layout all child components within the parent bounds
     */
    public abstract void layoutChildren(int left, int top, int right, int bottom);
    
    /**
     * Calculate the minimum size needed to contain all children
     */
    public abstract void calculateMinimumSize();
    
    /**
     * Helper method to measure a child component
     */
    protected void measureChild(UIComponent child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        LayoutParams lp = child.getLayoutParams();
        
        int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                parent.getPaddingLeft() + parent.getPaddingRight() + lp.getTotalMarginHorizontal(),
                lp.width);
        
        int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                parent.getPaddingTop() + parent.getPaddingBottom() + lp.getTotalMarginVertical(),
                lp.height);
        
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    
    /**
     * Calculate child measure spec based on parent constraints and child layout params
     */
    protected int getChildMeasureSpec(int parentMeasureSpec, int padding, int childDimension) {
        int parentMode = MeasureSpec.getMode(parentMeasureSpec);
        int parentSize = MeasureSpec.getSize(parentMeasureSpec);
        
        int size = Math.max(0, parentSize - padding);
        
        int resultSize = 0;
        int resultMode = 0;
        
        switch (parentMode) {
            case MeasureSpec.EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
                
            case MeasureSpec.AT_MOST:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
                
            case MeasureSpec.UNSPECIFIED:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else {
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
        }
        
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }
    
    /**
     * Helper method to resolve size based on measure spec
     */
    protected int resolveSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        
        return result;
    }
    
    /**
     * Get all visible children of the parent component
     */
    protected List<UIComponent> getVisibleChildren() {
        return parent.getChildren().stream()
                .filter(child -> child.getVisibility() != UIComponent.Visibility.GONE)
                .toList();
    }
    
    /**
     * Calculate the total weight of all children with weight > 0
     */
    protected float getTotalWeight() {
        return (float) getVisibleChildren().stream()
                .mapToDouble(child -> child.getLayoutParams().weight)
                .sum();
    }
    
    /**
     * Apply margins to a component's position
     */
    protected void applyMargins(UIComponent child, int left, int top, int right, int bottom) {
        LayoutParams lp = child.getLayoutParams();
        child.layout(
                left + lp.marginLeft,
                top + lp.marginTop,
                right - lp.marginRight,
                bottom - lp.marginBottom
        );
    }
    
    /**
     * Get the content area of the parent (excluding padding)
     */
    protected void getContentBounds(int[] bounds) {
        bounds[0] = parent.getPaddingLeft(); // left
        bounds[1] = parent.getPaddingTop(); // top
        bounds[2] = parent.getWidth() - parent.getPaddingRight(); // right
        bounds[3] = parent.getHeight() - parent.getPaddingBottom(); // bottom
    }
    
    /**
     * Calculate available space for children
     */
    protected int getAvailableWidth() {
        return parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
    }
    
    protected int getAvailableHeight() {
        return parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
    }
    
    /**
     * Request a layout pass for the parent component
     */
    public void requestLayout() {
        // Mark the parent as needing layout
        // In a real implementation, this would queue a layout pass
        if (parent != null) {
            parent.invalidate();
        }
    }
}