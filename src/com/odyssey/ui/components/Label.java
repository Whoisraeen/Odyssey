package com.odyssey.ui.components;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.rendering.ui.TextRenderer;

/**
 * A text display component with various styling options
 */
public class Label extends UIComponent {
    
    private String text = "";
    private int textColor = 0xFFFFFFFF; // White
    private TextRenderer.TextStyle textStyle;
    private boolean autoSize = true;
    private boolean wordWrap = false;
    private int maxLines = Integer.MAX_VALUE;
    private String ellipsis = "...";
    
    // Text alignment within the label bounds
    private TextRenderer.TextAlign horizontalAlignment = TextRenderer.TextAlign.LEFT;
    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;
    
    public enum VerticalAlignment {
        TOP, CENTER, BOTTOM
    }
    
    public Label() {
        super();
        initializeLabel();
    }
    
    public Label(String text) {
        super();
        this.text = text;
        initializeLabel();
    }
    
    private void initializeLabel() {
        setFocusable(false);
        setClickable(false);
        
        // Create default text style
        textStyle = new TextRenderer.TextStyle()
            .setColor(textColor)
            .setAlignment(horizontalAlignment);
        
        // Calculate initial size if auto-sizing
        if (autoSize) {
            updateSize();
        }
    }
    
    public void setText(String text) {
        if (text == null) text = "";
        
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
        textStyle.setColor(color);
        invalidate();
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void setHorizontalAlignment(TextRenderer.TextAlign alignment) {
        this.horizontalAlignment = alignment;
        textStyle.setAlignment(alignment);
        invalidate();
    }
    
    public TextRenderer.TextAlign getHorizontalAlignment() {
        return horizontalAlignment;
    }
    
    public void setVerticalAlignment(VerticalAlignment alignment) {
        this.verticalAlignment = alignment;
        invalidate();
    }
    
    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
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
    
    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
        if (autoSize) {
            updateSize();
        }
        invalidate();
    }
    
    public boolean isWordWrap() {
        return wordWrap;
    }
    
    public void setMaxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        if (autoSize) {
            updateSize();
        }
        invalidate();
    }
    
    public int getMaxLines() {
        return maxLines;
    }
    
    public void setEllipsis(String ellipsis) {
        this.ellipsis = ellipsis != null ? ellipsis : "";
        invalidate();
    }
    
    public String getEllipsis() {
        return ellipsis;
    }
    
    private void updateSize() {
        if (text.isEmpty()) {
            setSize(0, 0);
            return;
        }
        
        // Calculate text dimensions (simplified - in real implementation use TextRenderer)
        int charWidth = 8; // Approximate character width
        int lineHeight = 16; // Approximate line height
        
        if (wordWrap && (int) getWidth() > 0) {
            // Calculate wrapped text size
            int availableWidth = (int) getWidth() - (int) getPaddingLeft() - (int) getPaddingRight();
            int charsPerLine = Math.max(1, availableWidth / charWidth);
            int lines = Math.min(maxLines, (int) Math.ceil((double) text.length() / charsPerLine));
            
            int textWidth = Math.min(text.length() * charWidth, availableWidth);
            int textHeight = lines * lineHeight;
            
            setSize(textWidth + (int) getPaddingLeft() + (int) getPaddingRight(),
                   textHeight + (int) getPaddingTop() + (int) getPaddingBottom());
        } else {
            // Single line or no wrapping
            int textWidth = text.length() * charWidth;
            int textHeight = lineHeight;
            
            setSize(textWidth + (int) getPaddingLeft() + (int) getPaddingRight(),
                   textHeight + (int) getPaddingTop() + (int) getPaddingBottom());
        }
    }
    
    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        if (wordWrap && autoSize) {
            updateSize();
        }
    }
    
    @Override
    public void render(UIRenderer renderer, float deltaTime) {
        if (!isVisible() || text.isEmpty()) return;
        
        // Draw background if set
        if (getBackgroundColor() != 0) {
            if (getCornerRadius() > 0) {
                renderer.drawRoundedRect(
                    (int) getLeft(), (int) getTop(), (int) getRight(), (int) getBottom(),
                    (int) getCornerRadius(), getBackgroundColor()
                );
            } else {
                renderer.drawRect(
                    (int) getLeft(), (int) getTop(), (int) getRight(), (int) getBottom(),
                    getBackgroundColor()
                );
            }
        }
        
        // Calculate text position
        int contentLeft = (int) getLeft() + (int) getPaddingLeft();
        int contentTop = (int) getTop() + (int) getPaddingTop();
        int contentWidth = (int) getWidth() - (int) getPaddingLeft() - (int) getPaddingRight();
        int contentHeight = (int) getHeight() - (int) getPaddingTop() - (int) getPaddingBottom();
        
        // Calculate text position based on alignment
        int textX = contentLeft;
        int textY = contentTop;
        
        // Horizontal alignment
        switch (horizontalAlignment) {
            case CENTER:
                textX = contentLeft + contentWidth / 2;
                break;
            case RIGHT:
                textX = contentLeft + contentWidth;
                break;
            case LEFT:
            default:
                textX = contentLeft;
                break;
        }
        
        // Vertical alignment (simplified)
        int textHeight = 16; // Approximate line height
        switch (verticalAlignment) {
            case CENTER:
                textY = contentTop + (contentHeight - textHeight) / 2;
                break;
            case BOTTOM:
                textY = contentTop + contentHeight - textHeight;
                break;
            case TOP:
            default:
                textY = contentTop;
                break;
        }
        
        // Prepare text for rendering
        String renderText = prepareTextForRendering(contentWidth);
        
        // Note: In a real implementation, you'd use the actual TextRenderer
        // renderer.drawText(renderText, textX, textY, textStyle);
    }
    
    private String prepareTextForRendering(int availableWidth) {
        if (!wordWrap) {
            // Check if text fits, if not apply ellipsis
            int charWidth = 8; // Approximate character width
            int maxChars = availableWidth / charWidth;
            
            if (text.length() <= maxChars) {
                return text;
            } else {
                int ellipsisChars = ellipsis.length();
                int textChars = Math.max(0, maxChars - ellipsisChars);
                return text.substring(0, textChars) + ellipsis;
            }
        } else {
            // Word wrapping logic (simplified)
            int charWidth = 8;
            int charsPerLine = Math.max(1, availableWidth / charWidth);
            
            if (text.length() <= charsPerLine) {
                return text;
            }
            
            StringBuilder wrapped = new StringBuilder();
            String[] words = text.split(" ");
            int currentLineLength = 0;
            int lines = 0;
            
            for (String word : words) {
                if (lines >= maxLines) break;
                
                if (currentLineLength + word.length() + 1 > charsPerLine) {
                    if (currentLineLength > 0) {
                        wrapped.append("\n");
                        lines++;
                        currentLineLength = 0;
                    }
                }
                
                if (currentLineLength > 0) {
                    wrapped.append(" ");
                    currentLineLength++;
                }
                
                wrapped.append(word);
                currentLineLength += word.length();
            }
            
            return wrapped.toString();
        }
    }
    
    /**
     * Get the preferred size of this label based on its text content
     */
    public void getPreferredSize(int[] outSize) {
        if (text.isEmpty()) {
            outSize[0] = (int) getPaddingLeft() + (int) getPaddingRight();
            outSize[1] = (int) getPaddingTop() + (int) getPaddingBottom();
            return;
        }
        
        // Calculate text dimensions (simplified)
        int charWidth = 8;
        int lineHeight = 16;
        
        if (wordWrap && (int) getWidth() > 0) {
            int availableWidth = (int) getWidth() - (int) getPaddingLeft() - (int) getPaddingRight();
            int charsPerLine = Math.max(1, availableWidth / charWidth);
            int lines = Math.min(maxLines, (int) Math.ceil((double) text.length() / charsPerLine));
            
            outSize[0] = Math.min(text.length() * charWidth, availableWidth) + (int) getPaddingLeft() + (int) getPaddingRight();
            outSize[1] = lines * lineHeight + (int) getPaddingTop() + (int) getPaddingBottom();
        } else {
            outSize[0] = text.length() * charWidth + (int) getPaddingLeft() + (int) getPaddingRight();
            outSize[1] = lineHeight + (int) getPaddingTop() + (int) getPaddingBottom();
        }
    }
    
    /**
     * Builder pattern for easy label creation
     */
    public static class Builder {
        private Label label;
        
        public Builder() {
            label = new Label();
        }
        
        public Builder(String text) {
            label = new Label(text);
        }
        
        public Builder setText(String text) {
            label.setText(text);
            return this;
        }
        
        public Builder setTextColor(int color) {
            label.setTextColor(color);
            return this;
        }
        
        public Builder setHorizontalAlignment(TextRenderer.TextAlign alignment) {
            label.setHorizontalAlignment(alignment);
            return this;
        }
        
        public Builder setVerticalAlignment(VerticalAlignment alignment) {
            label.setVerticalAlignment(alignment);
            return this;
        }
        
        public Builder setAutoSize(boolean autoSize) {
            label.setAutoSize(autoSize);
            return this;
        }
        
        public Builder setWordWrap(boolean wordWrap) {
            label.setWordWrap(wordWrap);
            return this;
        }
        
        public Builder setMaxLines(int maxLines) {
            label.setMaxLines(maxLines);
            return this;
        }
        
        public Builder setSize(int width, int height) {
            label.setSize(width, height);
            label.setAutoSize(false);
            return this;
        }
        
        public Builder setBackgroundColor(int color) {
            label.setBackgroundColor(color);
            return this;
        }
        
        public Builder setCornerRadius(float radius) {
            label.setCornerRadius(radius);
            return this;
        }
        
        public Builder setPadding(int padding) {
            label.setPadding(padding, padding, padding, padding);
            return this;
        }
        
        public Builder setPadding(int left, int top, int right, int bottom) {
            label.setPadding(left, top, right, bottom);
            return this;
        }
        
        public Label build() {
            return label;
        }
    }
}