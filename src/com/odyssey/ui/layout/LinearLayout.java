package com.odyssey.ui.layout;

import com.odyssey.ui.UIComponent;

import java.util.List;

/**
 * Linear layout manager that arranges children in a single row or column
 * Supports weight distribution and gravity alignment
 */
public class LinearLayout extends LayoutManager {
    
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }
    
    public enum Gravity {
        START, CENTER, END, FILL
    }
    
    public static class LinearLayoutParams extends LayoutParams {
        public Gravity gravity = Gravity.START;
        
        public LinearLayoutParams() {
            super();
        }
        
        public LinearLayoutParams(int width, int height) {
            super(width, height);
        }
        
        public LinearLayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }
        
        public LinearLayoutParams gravity(Gravity gravity) {
            this.gravity = gravity;
            return this;
        }
    }
    
    private Orientation orientation = Orientation.VERTICAL;
    private Gravity gravity = Gravity.START;
    private boolean baselineAligned = true;
    
    public LinearLayout(UIComponent parent) {
        super(parent);
    }
    
    public LinearLayout(UIComponent parent, Orientation orientation) {
        super(parent);
        this.orientation = orientation;
    }
    
    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        parent.requestLayout();
    }
    
    public Orientation getOrientation() {
        return orientation;
    }
    
    public void setGravity(Gravity gravity) {
        this.gravity = gravity;
        parent.requestLayout();
    }
    
    public Gravity getGravity() {
        return gravity;
    }
    
    public void setBaselineAligned(boolean baselineAligned) {
        this.baselineAligned = baselineAligned;
        parent.requestLayout();
    }
    
    @Override
    public void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        List<UIComponent> children = getVisibleChildren();
        if (children.isEmpty()) {
            parent.setMeasuredDimension(
                resolveSize((int)(parent.getPaddingLeft() + parent.getPaddingRight()), widthMeasureSpec),
                resolveSize((int)(parent.getPaddingTop() + parent.getPaddingBottom()), heightMeasureSpec)
            );
            return;
        }
        
        if (orientation == Orientation.VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }
    
    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        List<UIComponent> children = getVisibleChildren();
        
        int totalHeight = 0;
        int maxWidth = 0;
        float totalWeight = 0;
        int weightedChildCount = 0;
        
        // First pass: measure children without weight
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            
            if (lp.weight > 0) {
                totalWeight += lp.weight;
                weightedChildCount++;
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                totalHeight += (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical();
                maxWidth = Math.max(maxWidth, (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal());
            }
        }
        
        // Add padding to dimensions
        totalHeight += (int)(parent.getPaddingTop() + parent.getPaddingBottom());
        maxWidth += (int)(parent.getPaddingLeft() + parent.getPaddingRight());
        
        // Calculate remaining space for weighted children
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        int remainingHeight = Math.max(0, parentHeight - totalHeight);
        
        // Second pass: measure weighted children
        if (totalWeight > 0 && remainingHeight > 0) {
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                
                if (lp.weight > 0) {
                    int childHeight = (int) (remainingHeight * (lp.weight / totalWeight));
                    
                    int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            (int)(parent.getPaddingLeft() + parent.getPaddingRight()) + (int)lp.getTotalMarginHorizontal(),
                            lp.width);
                    
                    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
                    
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    
                    totalHeight += (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical();
                    maxWidth = Math.max(maxWidth, (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal());
                }
            }
        }
        
        // Set measured dimensions
        parent.setMeasuredDimension(
            resolveSize(maxWidth, widthMeasureSpec),
            resolveSize(totalHeight, heightMeasureSpec)
        );
    }
    
    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        List<UIComponent> children = getVisibleChildren();
        
        int totalWidth = 0;
        int maxHeight = 0;
        float totalWeight = 0;
        int weightedChildCount = 0;
        
        // First pass: measure children without weight
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            
            if (lp.weight > 0) {
                totalWeight += lp.weight;
                weightedChildCount++;
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                totalWidth += (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal();
                maxHeight = Math.max(maxHeight, (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical());
            }
        }
        
        // Add padding to dimensions
        totalWidth += (int)(parent.getPaddingLeft() + parent.getPaddingRight());
        maxHeight += (int)(parent.getPaddingTop() + parent.getPaddingBottom());
        
        // Calculate remaining space for weighted children
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int remainingWidth = Math.max(0, parentWidth - totalWidth);
        
        // Second pass: measure weighted children
        if (totalWeight > 0 && remainingWidth > 0) {
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                
                if (lp.weight > 0) {
                    int childWidth = (int) (remainingWidth * (lp.weight / totalWeight));
                    
                    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                    
                    int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            (int)(parent.getPaddingTop() + parent.getPaddingBottom()) + (int)lp.getTotalMarginVertical(),
                            lp.height);
                    
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                    
                    totalWidth += (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal();
                    maxHeight = Math.max(maxHeight, (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical());
                }
            }
        }
        
        // Set measured dimensions
        parent.setMeasuredDimension(
            resolveSize(totalWidth, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        );
    }
    
    @Override
    public void layoutChildren(int left, int top, int right, int bottom) {
        List<UIComponent> children = getVisibleChildren();
        if (children.isEmpty()) return;
        
        if (orientation == Orientation.VERTICAL) {
            layoutVertical(left, top, right, bottom);
        } else {
            layoutHorizontal(left, top, right, bottom);
        }
    }
    
    private void layoutVertical(int left, int top, int right, int bottom) {
        List<UIComponent> children = getVisibleChildren();
        
        int paddingLeft = (int)parent.getPaddingLeft();
        int paddingTop = (int)parent.getPaddingTop();
        int paddingRight = (int)parent.getPaddingRight();
        int paddingBottom = (int)parent.getPaddingBottom();
        
        int width = right - left;
        int height = bottom - top;
        
        int childTop = paddingTop;
        int childLeft = paddingLeft;
        int childRight = width - paddingRight;
        
        // Apply gravity for the entire group
        if (gravity != Gravity.START) {
            int totalChildrenHeight = 0;
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                totalChildrenHeight += (int)child.getMeasuredHeight() + (int)lp.getTotalMarginVertical();
            }
            
            int remainingHeight = height - paddingTop - paddingBottom - totalChildrenHeight;
            
            switch (gravity) {
                case CENTER:
                    childTop += remainingHeight / 2;
                    break;
                case END:
                    childTop += remainingHeight;
                    break;
                case FILL:
                    // Distribute extra space among children
                    break;
            }
        }
        
        // Layout each child
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            
            int childWidth = (int)child.getMeasuredWidth();
            int childHeight = (int)child.getMeasuredHeight();
            
            // Apply horizontal gravity for individual child
            int childLeftPos = childLeft + lp.marginLeft;
            int childRightPos = childRight - lp.marginRight;
            
            if (lp instanceof LinearLayoutParams) {
                LinearLayoutParams llp = (LinearLayoutParams) lp;
                switch (llp.gravity) {
                    case CENTER:
                        int availableWidth = childRightPos - childLeftPos;
                        childLeftPos += (availableWidth - childWidth) / 2;
                        break;
                    case END:
                        childLeftPos = childRightPos - childWidth;
                        break;
                    case FILL:
                        childWidth = childRightPos - childLeftPos;
                        break;
                }
            }
            
            child.layout(
                childLeftPos,
                childTop + lp.marginTop,
                childLeftPos + childWidth,
                childTop + lp.marginTop + childHeight
            );
            
            childTop += childHeight + (int)lp.getTotalMarginVertical();
        }
    }
    
    private void layoutHorizontal(int left, int top, int right, int bottom) {
        List<UIComponent> children = getVisibleChildren();
        
        int paddingLeft = (int)parent.getPaddingLeft();
        int paddingTop = (int)parent.getPaddingTop();
        int paddingRight = (int)parent.getPaddingRight();
        int paddingBottom = (int)parent.getPaddingBottom();
        
        int width = right - left;
        int height = bottom - top;
        
        int childLeft = paddingLeft;
        int childTop = paddingTop;
        int childBottom = height - paddingBottom;
        
        // Apply gravity for the entire group
        if (gravity != Gravity.START) {
            int totalChildrenWidth = 0;
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                totalChildrenWidth += (int)child.getMeasuredWidth() + (int)lp.getTotalMarginHorizontal();
            }
            
            int remainingWidth = width - paddingLeft - paddingRight - totalChildrenWidth;
            
            switch (gravity) {
                case CENTER:
                    childLeft += remainingWidth / 2;
                    break;
                case END:
                    childLeft += remainingWidth;
                    break;
                case FILL:
                    // Distribute extra space among children
                    break;
            }
        }
        
        // Layout each child
        for (UIComponent child : children) {
            LayoutParams lp = child.getLayoutParams();
            
            int childWidth = (int)child.getMeasuredWidth();
            int childHeight = (int)child.getMeasuredHeight();
            
            // Apply vertical gravity for individual child
            int childTopPos = childTop + lp.marginTop;
            int childBottomPos = childBottom - lp.marginBottom;
            
            if (lp instanceof LinearLayoutParams) {
                LinearLayoutParams llp = (LinearLayoutParams) lp;
                switch (llp.gravity) {
                    case CENTER:
                        int availableHeight = childBottomPos - childTopPos;
                        childTopPos += (availableHeight - childHeight) / 2;
                        break;
                    case END:
                        childTopPos = childBottomPos - childHeight;
                        break;
                    case FILL:
                        childHeight = childBottomPos - childTopPos;
                        break;
                }
            }
            
            child.layout(
                childLeft + lp.marginLeft,
                childTopPos,
                childLeft + lp.marginLeft + childWidth,
                childTopPos + childHeight
            );
            
            childLeft += childWidth + (int)lp.getTotalMarginHorizontal();
        }
    }
    
    @Override
    public void calculateMinimumSize() {
        List<UIComponent> children = getVisibleChildren();
        
        int minWidth = (int)(parent.getPaddingLeft() + parent.getPaddingRight());
        int minHeight = (int)(parent.getPaddingTop() + parent.getPaddingBottom());
        
        if (orientation == Orientation.VERTICAL) {
            int maxChildWidth = 0;
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                minHeight += (int)child.getMinHeight() + (int)lp.getTotalMarginVertical();
                maxChildWidth = Math.max(maxChildWidth, (int)child.getMinWidth() + (int)lp.getTotalMarginHorizontal());
            }
            minWidth += maxChildWidth;
        } else {
            int maxChildHeight = 0;
            for (UIComponent child : children) {
                LayoutParams lp = child.getLayoutParams();
                minWidth += (int)child.getMinWidth() + (int)lp.getTotalMarginHorizontal();
                maxChildHeight = Math.max(maxChildHeight, (int)child.getMinHeight() + (int)lp.getTotalMarginVertical());
            }
            minHeight += maxChildHeight;
        }
        
        parent.setMinWidth(minWidth);
        parent.setMinHeight(minHeight);
    }
}