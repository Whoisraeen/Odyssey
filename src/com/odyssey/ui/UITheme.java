package com.odyssey.ui;

/**
 * Theme system for the Odyssey UI
 * Manages colors, fonts, and styling properties
 */
public class UITheme {
    
    // Color constants
    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int GRAY = 0xFF808080;
    public static final int LIGHT_GRAY = 0xFFC0C0C0;
    public static final int DARK_GRAY = 0xFF404040;
    
    // Primary colors
    private int primaryColor = 0xFF2196F3;      // Blue
    private int primaryDarkColor = 0xFF1976D2;   // Dark Blue
    private int primaryLightColor = 0xFFBBDEFB;  // Light Blue
    private int accentColor = 0xFFFF4081;        // Pink
    
    // Background colors
    private int backgroundColor = 0xFF121212;     // Dark background
    private int surfaceColor = 0xFF1E1E1E;       // Surface color
    private int cardColor = 0xFF2D2D2D;          // Card background
    
    // Text colors
    private int textColorPrimary = 0xFFFFFFFF;   // White text
    private int textColorSecondary = 0xB3FFFFFF; // Semi-transparent white
    private int textColorHint = 0x61FFFFFF;      // Hint text
    
    // Border and divider colors
    private int borderColor = 0xFF424242;
    private int dividerColor = 0xFF303030;
    
    // State colors
    private int hoverColor = 0x1AFFFFFF;         // Hover overlay
    private int pressedColor = 0x33FFFFFF;       // Pressed overlay
    private int focusedColor = 0xFF64B5F6;       // Focus indicator
    private int disabledColor = 0x61FFFFFF;      // Disabled overlay
    
    // Error and warning colors
    private int errorColor = 0xFFE53935;
    private int warningColor = 0xFFFF9800;
    private int successColor = 0xFF4CAF50;
    
    // Font properties
    private String defaultFontFamily = "Arial";
    private float defaultFontSize = 14.0f;
    private float smallFontSize = 12.0f;
    private float largeFontSize = 18.0f;
    private float titleFontSize = 24.0f;
    
    // Spacing and dimensions
    private float defaultPadding = 8.0f;
    private float smallPadding = 4.0f;
    private float largePadding = 16.0f;
    private float defaultMargin = 8.0f;
    private float defaultCornerRadius = 4.0f;
    private float cardCornerRadius = 8.0f;
    
    // Animation durations
    private long shortAnimationDuration = 150;
    private long mediumAnimationDuration = 300;
    private long longAnimationDuration = 500;
    
    // Theme variants
    public enum ThemeVariant {
        DARK,
        LIGHT,
        AUTO
    }
    
    private ThemeVariant currentVariant = ThemeVariant.DARK;
    
    /**
     * Default constructor with dark theme
     */
    public UITheme() {
        applyDarkTheme();
    }
    
    /**
     * Constructor with specified variant
     */
    public UITheme(ThemeVariant variant) {
        setThemeVariant(variant);
    }
    
    /**
     * Apply dark theme colors
     */
    public void applyDarkTheme() {
        backgroundColor = 0xFF121212;
        surfaceColor = 0xFF1E1E1E;
        cardColor = 0xFF2D2D2D;
        textColorPrimary = 0xFFFFFFFF;
        textColorSecondary = 0xB3FFFFFF;
        textColorHint = 0x61FFFFFF;
        borderColor = 0xFF424242;
        dividerColor = 0xFF303030;
        currentVariant = ThemeVariant.DARK;
    }
    
    /**
     * Apply light theme colors
     */
    public void applyLightTheme() {
        backgroundColor = 0xFFFAFAFA;
        surfaceColor = 0xFFFFFFFF;
        cardColor = 0xFFFFFFFF;
        textColorPrimary = 0xFF212121;
        textColorSecondary = 0x8A000000;
        textColorHint = 0x61000000;
        borderColor = 0xFFE0E0E0;
        dividerColor = 0xFFEEEEEE;
        currentVariant = ThemeVariant.LIGHT;
    }
    
    /**
     * Set the theme variant
     */
    public void setThemeVariant(ThemeVariant variant) {
        switch (variant) {
            case DARK:
                applyDarkTheme();
                break;
            case LIGHT:
                applyLightTheme();
                break;
            case AUTO:
                // For now, default to dark theme
                // In a real implementation, this would check system preferences
                applyDarkTheme();
                break;
        }
    }
    
    // Color getters
    public int getPrimaryColor() { return primaryColor; }
    public int getPrimaryDarkColor() { return primaryDarkColor; }
    public int getPrimaryLightColor() { return primaryLightColor; }
    public int getAccentColor() { return accentColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getSurfaceColor() { return surfaceColor; }
    public int getCardColor() { return cardColor; }
    public int getTextColorPrimary() { return textColorPrimary; }
    public int getTextColorSecondary() { return textColorSecondary; }
    public int getTextColorHint() { return textColorHint; }
    public int getBorderColor() { return borderColor; }
    public int getDividerColor() { return dividerColor; }
    public int getHoverColor() { return hoverColor; }
    public int getPressedColor() { return pressedColor; }
    public int getFocusedColor() { return focusedColor; }
    public int getDisabledColor() { return disabledColor; }
    public int getErrorColor() { return errorColor; }
    public int getWarningColor() { return warningColor; }
    public int getSuccessColor() { return successColor; }
    
    // Color setters
    public void setPrimaryColor(int color) { this.primaryColor = color; }
    public void setPrimaryDarkColor(int color) { this.primaryDarkColor = color; }
    public void setPrimaryLightColor(int color) { this.primaryLightColor = color; }
    public void setAccentColor(int color) { this.accentColor = color; }
    public void setBackgroundColor(int color) { this.backgroundColor = color; }
    public void setSurfaceColor(int color) { this.surfaceColor = color; }
    public void setCardColor(int color) { this.cardColor = color; }
    
    // Font getters
    public String getDefaultFontFamily() { return defaultFontFamily; }
    public float getDefaultFontSize() { return defaultFontSize; }
    public float getSmallFontSize() { return smallFontSize; }
    public float getLargeFontSize() { return largeFontSize; }
    public float getTitleFontSize() { return titleFontSize; }
    
    // Font setters
    public void setDefaultFontFamily(String fontFamily) { this.defaultFontFamily = fontFamily; }
    public void setDefaultFontSize(float fontSize) { this.defaultFontSize = fontSize; }
    public void setSmallFontSize(float fontSize) { this.smallFontSize = fontSize; }
    public void setLargeFontSize(float fontSize) { this.largeFontSize = fontSize; }
    public void setTitleFontSize(float fontSize) { this.titleFontSize = fontSize; }
    
    // Spacing getters
    public float getDefaultPadding() { return defaultPadding; }
    public float getSmallPadding() { return smallPadding; }
    public float getLargePadding() { return largePadding; }
    public float getDefaultMargin() { return defaultMargin; }
    public float getDefaultCornerRadius() { return defaultCornerRadius; }
    public float getCardCornerRadius() { return cardCornerRadius; }
    
    // Animation duration getters
    public long getShortAnimationDuration() { return shortAnimationDuration; }
    public long getMediumAnimationDuration() { return mediumAnimationDuration; }
    public long getLongAnimationDuration() { return longAnimationDuration; }
    
    // Theme variant getter
    public ThemeVariant getCurrentVariant() { return currentVariant; }
    
    /**
     * Create a color with alpha
     */
    public static int withAlpha(int color, float alpha) {
        int alphaInt = (int) (alpha * 255);
        return (color & 0x00FFFFFF) | (alphaInt << 24);
    }
    
    /**
     * Blend two colors
     */
    public static int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1.0f - ratio;
        
        int a = (int) ((color1 >>> 24) * inverseRatio + (color2 >>> 24) * ratio);
        int r = (int) (((color1 >> 16) & 0xFF) * inverseRatio + ((color2 >> 16) & 0xFF) * ratio);
        int g = (int) (((color1 >> 8) & 0xFF) * inverseRatio + ((color2 >> 8) & 0xFF) * ratio);
        int b = (int) ((color1 & 0xFF) * inverseRatio + (color2 & 0xFF) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Get a lighter version of a color
     */
    public static int lighten(int color, float factor) {
        return blendColors(color, WHITE, factor);
    }
    
    /**
     * Get a darker version of a color
     */
    public static int darken(int color, float factor) {
        return blendColors(color, BLACK, factor);
    }
}