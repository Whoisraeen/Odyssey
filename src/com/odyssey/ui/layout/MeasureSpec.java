package com.odyssey.ui.layout;

/**
 * A MeasureSpec encapsulates the layout requirements passed from parent to child.
 * Each MeasureSpec represents a requirement for either the width or the height.
 * A MeasureSpec is comprised of a size and a mode.
 */
public class MeasureSpec {
    
    private static final int MODE_SHIFT = 30;
    private static final int MODE_MASK = 0x3 << MODE_SHIFT;
    
    /**
     * Measure specification mode: The parent has not imposed any constraint
     * on the child. It can be whatever size it wants.
     */
    public static final int UNSPECIFIED = 0 << MODE_SHIFT;
    
    /**
     * Measure specification mode: The parent has determined an exact size
     * for the child. The child is going to be given those bounds regardless
     * of how big it wants to be.
     */
    public static final int EXACTLY = 1 << MODE_SHIFT;
    
    /**
     * Measure specification mode: The child can be as large as it wants up
     * to the specified size.
     */
    public static final int AT_MOST = 2 << MODE_SHIFT;
    
    /**
     * Creates a measure specification based on the supplied size and mode.
     *
     * @param size the size of the measure specification
     * @param mode the mode of the measure specification
     * @return the measure specification based on size and mode
     */
    public static int makeMeasureSpec(int size, int mode) {
        return (size & ~MODE_MASK) | (mode & MODE_MASK);
    }
    
    /**
     * Extracts the mode from the supplied measure specification.
     *
     * @param measureSpec the measure specification to extract the mode from
     * @return the mode stored in the supplied measure specification
     */
    public static int getMode(int measureSpec) {
        return (measureSpec & MODE_MASK);
    }
    
    /**
     * Extracts the size from the supplied measure specification.
     *
     * @param measureSpec the measure specification to extract the size from
     * @return the size stored in the supplied measure specification
     */
    public static int getSize(int measureSpec) {
        return (measureSpec & ~MODE_MASK);
    }
    
    /**
     * Returns a String representation of the specified measure
     * specification.
     *
     * @param measureSpec the measure specification to convert to a String
     * @return a String with the following format: "MeasureSpec: MODE SIZE"
     */
    public static String toString(int measureSpec) {
        int mode = getMode(measureSpec);
        int size = getSize(measureSpec);
        
        StringBuilder sb = new StringBuilder("MeasureSpec: ");
        
        if (mode == UNSPECIFIED) {
            sb.append("UNSPECIFIED ");
        } else if (mode == EXACTLY) {
            sb.append("EXACTLY ");
        } else if (mode == AT_MOST) {
            sb.append("AT_MOST ");
        } else {
            sb.append(mode).append(" ");
        }
        
        sb.append(size);
        return sb.toString();
    }
    
    /**
     * Utility method to create an EXACTLY measure spec
     */
    public static int exactly(int size) {
        return makeMeasureSpec(size, EXACTLY);
    }
    
    /**
     * Utility method to create an AT_MOST measure spec
     */
    public static int atMost(int size) {
        return makeMeasureSpec(size, AT_MOST);
    }
    
    /**
     * Utility method to create an UNSPECIFIED measure spec
     */
    public static int unspecified() {
        return makeMeasureSpec(0, UNSPECIFIED);
    }
    
    /**
     * Utility method to create an UNSPECIFIED measure spec with a size hint
     */
    public static int unspecified(int size) {
        return makeMeasureSpec(size, UNSPECIFIED);
    }
    
    /**
     * Check if the measure spec is exactly specified
     */
    public static boolean isExactly(int measureSpec) {
        return getMode(measureSpec) == EXACTLY;
    }
    
    /**
     * Check if the measure spec is at most specified
     */
    public static boolean isAtMost(int measureSpec) {
        return getMode(measureSpec) == AT_MOST;
    }
    
    /**
     * Check if the measure spec is unspecified
     */
    public static boolean isUnspecified(int measureSpec) {
        return getMode(measureSpec) == UNSPECIFIED;
    }
    
    /**
     * Resolve a size based on the measure spec and desired size
     */
    public static int resolveSize(int size, int measureSpec) {
        int specMode = getMode(measureSpec);
        int specSize = getSize(measureSpec);
        
        switch (specMode) {
            case UNSPECIFIED:
                return size;
            case AT_MOST:
                return Math.min(size, specSize);
            case EXACTLY:
                return specSize;
            default:
                return size;
        }
    }
    
    /**
     * Get the default size for a view based on the measure spec
     */
    public static int getDefaultSize(int size, int measureSpec) {
        int result = size;
        int specMode = getMode(measureSpec);
        int specSize = getSize(measureSpec);
        
        switch (specMode) {
            case UNSPECIFIED:
                result = size;
                break;
            case AT_MOST:
            case EXACTLY:
                result = specSize;
                break;
        }
        
        return result;
    }
    
    /**
     * Adjust a measure spec based on layout parameters
     */
    public static int adjustMeasureSpec(int measureSpec, int layoutParam) {
        int specMode = getMode(measureSpec);
        int specSize = getSize(measureSpec);
        
        if (layoutParam == LayoutParams.MATCH_PARENT) {
            return makeMeasureSpec(specSize, EXACTLY);
        } else if (layoutParam == LayoutParams.WRAP_CONTENT) {
            return makeMeasureSpec(specSize, AT_MOST);
        } else if (layoutParam >= 0) {
            return makeMeasureSpec(layoutParam, EXACTLY);
        }
        
        return measureSpec;
    }
    
    /**
     * Create a child measure spec based on parent spec and child layout params
     */
    public static int getChildMeasureSpec(int parentSpec, int padding, int childDimension) {
        int specMode = getMode(parentSpec);
        int specSize = getSize(parentSpec);
        
        int size = Math.max(0, specSize - padding);
        
        int resultSize = 0;
        int resultMode = 0;
        
        switch (specMode) {
            case EXACTLY:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = AT_MOST;
                }
                break;
                
            case AT_MOST:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = AT_MOST;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    resultMode = AT_MOST;
                }
                break;
                
            case UNSPECIFIED:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = 0;
                    resultMode = UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = 0;
                    resultMode = UNSPECIFIED;
                }
                break;
        }
        
        return makeMeasureSpec(resultSize, resultMode);
    }
}