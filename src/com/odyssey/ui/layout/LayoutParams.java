package com.odyssey.ui.layout;

/**
 * Base class for layout parameters used by layout managers
 */
public class LayoutParams {
    
    /**
     * Special value for the height or width requested by a View.
     * MATCH_PARENT means that the view wants to be as big as its parent,
     * minus the parent's padding, if any.
     */
    public static final int MATCH_PARENT = -1;
    
    /**
     * Special value for the height or width requested by a View.
     * WRAP_CONTENT means that the view wants to be just large enough to fit
     * its own internal content, taking its own padding into account.
     */
    public static final int WRAP_CONTENT = -2;
    
    /**
     * The width of the view in pixels
     */
    public int width;
    
    /**
     * The height of the view in pixels
     */
    public int height;
    
    /**
     * Weight for layout distribution
     */
    public float weight = 0.0f;
    
    // Margins - space outside the view
    public int marginLeft = 0;
    public int marginTop = 0;
    public int marginRight = 0;
    public int marginBottom = 0;
    
    // Padding - space inside the view
    public int paddingLeft = 0;
    public int paddingTop = 0;
    public int paddingRight = 0;
    public int paddingBottom = 0;
    
    /**
     * Creates a new set of layout parameters with the specified width and height.
     */
    public LayoutParams(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Creates a new set of layout parameters with WRAP_CONTENT dimensions.
     */
    public LayoutParams() {
        this(WRAP_CONTENT, WRAP_CONTENT);
    }
    
    /**
     * Copy constructor
     */
    public LayoutParams(LayoutParams source) {
        this.width = source.width;
        this.height = source.height;
        this.weight = source.weight;
        this.marginLeft = source.marginLeft;
        this.marginTop = source.marginTop;
        this.marginRight = source.marginRight;
        this.marginBottom = source.marginBottom;
        this.paddingLeft = source.paddingLeft;
        this.paddingTop = source.paddingTop;
        this.paddingRight = source.paddingRight;
        this.paddingBottom = source.paddingBottom;
    }
    
    /**
     * Set all margins to the same value
     */
    public LayoutParams setMargins(int margin) {
        return setMargins(margin, margin, margin, margin);
    }
    
    /**
     * Set margins
     */
    public LayoutParams setMargins(int left, int top, int right, int bottom) {
        this.marginLeft = left;
        this.marginTop = top;
        this.marginRight = right;
        this.marginBottom = bottom;
        return this;
    }
    
    /**
     * Set all padding to the same value
     */
    public LayoutParams setPadding(int padding) {
        return setPadding(padding, padding, padding, padding);
    }
    
    /**
     * Set padding
     */
    public LayoutParams setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        return this;
    }
    
    /**
     * Set weight for layout distribution
     */
    public LayoutParams setWeight(float weight) {
        this.weight = weight;
        return this;
    }
    
    /**
     * Get total horizontal margin
     */
    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }
    
    /**
     * Get total vertical margin
     */
    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }
    
    /**
     * Get total horizontal padding
     */
    public int getHorizontalPadding() {
        return paddingLeft + paddingRight;
    }
    
    /**
     * Get total vertical padding
     */
    public int getVerticalPadding() {
        return paddingTop + paddingBottom;
    }
    
    /**
     * Check if width is exactly specified
     */
    public boolean isWidthExact() {
        return width >= 0;
    }
    
    /**
     * Check if height is exactly specified
     */
    public boolean isHeightExact() {
        return height >= 0;
    }
    
    /**
     * Check if width should match parent
     */
    public boolean isWidthMatchParent() {
        return width == MATCH_PARENT;
    }
    
    /**
     * Check if height should match parent
     */
    public boolean isHeightMatchParent() {
        return height == MATCH_PARENT;
    }
    
    /**
     * Check if width should wrap content
     */
    public boolean isWidthWrapContent() {
        return width == WRAP_CONTENT;
    }
    
    /**
     * Check if height should wrap content
     */
    public boolean isHeightWrapContent() {
        return height == WRAP_CONTENT;
    }
    
    @Override
    public String toString() {
        return "LayoutParams{" +
                "width=" + width +
                ", height=" + height +
                ", weight=" + weight +
                ", margins=[" + marginLeft + ", " + marginTop + ", " + marginRight + ", " + marginBottom + "]" +
                ", padding=[" + paddingLeft + ", " + paddingTop + ", " + paddingRight + ", " + paddingBottom + "]" +
                '}';
    }
}