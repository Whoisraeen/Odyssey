package com.odyssey.ui.components;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.rendering.ui.TextRenderer;

/**
 * A text input field component with cursor, selection, and editing capabilities
 */
public class TextField extends UIComponent {
    
    private StringBuilder text;
    private String placeholder = "";
    private int textColor = 0xFFFFFFFF; // White
    private int placeholderColor = 0xFF808080; // Gray
    private int selectionColor = 0xFF0078D4; // Blue
    private int cursorColor = 0xFFFFFFFF; // White
    
    private int backgroundColor = 0xFF2D2D30; // Dark gray
    private int borderColor = 0xFF3F3F46; // Lighter gray
    private int borderColorFocused = 0xFF0078D4; // Blue when focused
    private int borderWidth = 1;
    
    // Text editing state
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean isSelecting = false;
    
    // Display properties
    private int scrollOffset = 0;
    private int maxLength = Integer.MAX_VALUE;
    private boolean isPassword = false;
    private char passwordChar = '*';
    private boolean readOnly = false;
    
    // Cursor blinking
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;
    private static final long CURSOR_BLINK_RATE = 500; // milliseconds
    
    // Text style
    private TextRenderer.TextStyle textStyle;
    
    // Input validation
    private InputFilter inputFilter;
    
    public interface InputFilter {
        boolean isValid(char character);
        String filter(String input);
    }
    
    // Event listeners
    public interface OnTextChangeListener {
        void onTextChanged(TextField textField, String oldText, String newText);
    }
    
    private OnTextChangeListener onTextChangeListener;
    
    public TextField() {
        super();
        initializeTextField();
    }
    
    public TextField(String initialText) {
        super();
        this.text = new StringBuilder(initialText != null ? initialText : "");
        this.cursorPosition = this.text.length();
        initializeTextField();
    }
    
    private void initializeTextField() {
        if (text == null) {
            text = new StringBuilder();
        }
        
        setFocusable(true);
        setClickable(true);
        
        // Create text style
        textStyle = new TextRenderer.TextStyle()
            .setColor(textColor)
            .setAlignment(TextRenderer.TextAlign.LEFT);
        
        // Set default appearance
        setBackgroundColor(backgroundColor);
        setCornerRadius(4.0f);
        setPadding(8, 6, 8, 6);
        
        // Set default size
        setSize(200, 32);
    }
    
    public void setText(String text) {
        String oldText = this.text.toString();
        String newText = text != null ? text : "";
        
        if (inputFilter != null) {
            newText = inputFilter.filter(newText);
        }
        
        if (newText.length() > maxLength) {
            newText = newText.substring(0, maxLength);
        }
        
        this.text.setLength(0);
        this.text.append(newText);
        
        // Update cursor position
        cursorPosition = Math.min(cursorPosition, this.text.length());
        clearSelection();
        updateScrollOffset();
        
        if (onTextChangeListener != null && !oldText.equals(newText)) {
            onTextChangeListener.onTextChanged(this, oldText, newText);
        }
        
        invalidate();
    }
    
    public String getText() {
        return text.toString();
    }
    
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        invalidate();
    }
    
    public String getPlaceholder() {
        return placeholder;
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
        textStyle.setColor(color);
        invalidate();
    }
    
    public void setPlaceholderColor(int color) {
        this.placeholderColor = color;
        invalidate();
    }
    
    public void setSelectionColor(int color) {
        this.selectionColor = color;
        invalidate();
    }
    
    public void setCursorColor(int color) {
        this.cursorColor = color;
        invalidate();
    }
    
    public void setBorderColorFocused(int color) {
        this.borderColorFocused = color;
        invalidate();
    }
    
    public void setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (text.length() > this.maxLength) {
            setText(text.substring(0, this.maxLength));
        }
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public void setPassword(boolean isPassword) {
        this.isPassword = isPassword;
        invalidate();
    }
    
    public boolean isPassword() {
        return isPassword;
    }
    
    public void setPasswordChar(char passwordChar) {
        this.passwordChar = passwordChar;
        if (isPassword) {
            invalidate();
        }
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setInputFilter(InputFilter filter) {
        this.inputFilter = filter;
    }
    
    public void setOnTextChangeListener(OnTextChangeListener listener) {
        this.onTextChangeListener = listener;
    }
    
    public void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
        invalidate();
    }
    
    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        isSelecting = false;
        invalidate();
    }
    
    public boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }
    
    public String getSelectedText() {
        if (!hasSelection()) return "";
        
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }
    
    public void deleteSelection() {
        if (!hasSelection()) return;
        
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        String oldText = text.toString();
        text.delete(start, end);
        cursorPosition = start;
        clearSelection();
        updateScrollOffset();
        
        if (onTextChangeListener != null) {
            onTextChangeListener.onTextChanged(this, oldText, text.toString());
        }
        
        invalidate();
    }
    
    private void insertText(String insertText) {
        if (readOnly || insertText == null || insertText.isEmpty()) return;
        
        // Apply input filter
        if (inputFilter != null) {
            insertText = inputFilter.filter(insertText);
            if (insertText.isEmpty()) return;
        }
        
        String oldText = text.toString();
        
        // Delete selection if any
        if (hasSelection()) {
            deleteSelection();
        }
        
        // Check length limit
        if (text.length() + insertText.length() > maxLength) {
            insertText = insertText.substring(0, maxLength - text.length());
        }
        
        if (!insertText.isEmpty()) {
            text.insert(cursorPosition, insertText);
            cursorPosition += insertText.length();
            updateScrollOffset();
            
            if (onTextChangeListener != null) {
                onTextChangeListener.onTextChanged(this, oldText, text.toString());
            }
            
            invalidate();
        }
    }
    
    private void updateScrollOffset() {
        // Simplified scroll calculation
        int charWidth = 8; // Approximate character width
        int contentWidth = (int)getWidth() - (int)getPaddingLeft() - (int)getPaddingRight();
        int cursorX = cursorPosition * charWidth;
        
        // Scroll to keep cursor visible
        if (cursorX < scrollOffset) {
            scrollOffset = cursorX;
        } else if (cursorX > scrollOffset + contentWidth) {
            scrollOffset = cursorX - contentWidth;
        }
        
        scrollOffset = Math.max(0, scrollOffset);
    }
    
    private String getDisplayText() {
        if (text.length() == 0) {
            return "";
        }
        
        if (isPassword) {
            return new String(new char[text.length()]).replace('\0', passwordChar);
        }
        
        return text.toString();
    }
    
    @Override
    public void render(UIRenderer renderer, float deltaTime) {
        if (!isVisible()) return;
        
        // Update cursor blink
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorBlink > CURSOR_BLINK_RATE) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = currentTime;
        }
        
        // Draw background
        renderer.drawRoundedRect(
            getLeft(), getTop(), getRight(), getBottom(),
            getCornerRadius(), getBackgroundColor()
        );
        
        // Draw border
        int currentBorderColor = isFocused() ? borderColorFocused : borderColor;
        if (borderWidth > 0) {
            renderer.drawRoundedRectOutline(
                getLeft(), getTop(), getRight(), getBottom(),
                getCornerRadius(), currentBorderColor, borderWidth
            );
        }
        
        // Calculate text area
        int textLeft = (int) getLeft() + (int) getPaddingLeft();
        int textTop = (int) getTop() + (int) getPaddingTop();
        int textWidth = (int) getWidth() - (int) getPaddingLeft() - (int) getPaddingRight();
        int textHeight = (int) getHeight() - (int) getPaddingTop() - (int) getPaddingBottom();
        
        // Set up clipping for text area
        renderer.pushClip(textLeft, textTop, textLeft + textWidth, textTop + textHeight);
        
        // Draw text or placeholder
        String displayText = getDisplayText();
        if (displayText.isEmpty() && !placeholder.isEmpty() && !isFocused()) {
            // Draw placeholder
            TextRenderer.TextStyle placeholderStyle = new TextRenderer.TextStyle()
                .setColor(placeholderColor)
                .setAlignment(TextRenderer.TextAlign.LEFT);
            
            // Note: In real implementation, use actual TextRenderer
            // renderer.drawText(placeholder, textLeft - scrollOffset, textTop + textHeight / 2, placeholderStyle);
        } else if (!displayText.isEmpty()) {
            // Draw selection background
            if (hasSelection()) {
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                
                int charWidth = 8; // Approximate
                int selStartX = textLeft + start * charWidth - scrollOffset;
                int selEndX = textLeft + end * charWidth - scrollOffset;
                
                renderer.drawRect(selStartX, textTop, selEndX, textTop + textHeight, selectionColor);
            }
            
            // Draw text
            // Note: In real implementation, use actual TextRenderer
            // renderer.drawText(displayText, textLeft - scrollOffset, textTop + textHeight / 2, textStyle);
        }
        
        // Draw cursor
        if (isFocused() && cursorVisible && !hasSelection()) {
            int charWidth = 8; // Approximate
            int cursorX = textLeft + cursorPosition * charWidth - scrollOffset;
            
            renderer.drawRect(cursorX, textTop + 2, cursorX + 1, textTop + textHeight - 2, cursorColor);
        }
        
        // Restore clipping
        renderer.popClip();
    }
    
    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (!isEnabled() || !isVisible()) {
            return false;
        }
        
        if (contains((int)mouseX, (int)mouseY)) {
            requestFocus();
            
            // Calculate cursor position from mouse click
            int textLeft = (int) getLeft() + (int) getPaddingLeft();
            int relativeX = (int)mouseX - textLeft + scrollOffset;
            int charWidth = 8; // Approximate
            int newCursorPos = Math.max(0, Math.min(text.length(), relativeX / charWidth));
            
            cursorPosition = newCursorPos;
            clearSelection();
            resetCursorBlink();
            invalidate();
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isEnabled() || !isVisible() || !isFocused()) {
            return false;
        }
        
        boolean ctrl = (modifiers & 2) != 0; // GLFW_MOD_CONTROL
        boolean shift = (modifiers & 1) != 0; // GLFW_MOD_SHIFT
        
        switch (keyCode) {
            case 259: // GLFW_KEY_BACKSPACE
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    String oldText = text.toString();
                    text.deleteCharAt(cursorPosition - 1);
                    cursorPosition--;
                    updateScrollOffset();
                    
                    if (onTextChangeListener != null) {
                        onTextChangeListener.onTextChanged(this, oldText, text.toString());
                    }
                    
                    invalidate();
                }
                resetCursorBlink();
                return true;
                
            case 261: // GLFW_KEY_DELETE
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    String oldText = text.toString();
                    text.deleteCharAt(cursorPosition);
                    updateScrollOffset();
                    
                    if (onTextChangeListener != null) {
                        onTextChangeListener.onTextChanged(this, oldText, text.toString());
                    }
                    
                    invalidate();
                }
                resetCursorBlink();
                return true;
                
            case 262: // GLFW_KEY_RIGHT
                if (cursorPosition < text.length()) {
                    cursorPosition++;
                    if (!shift) clearSelection();
                    updateScrollOffset();
                    resetCursorBlink();
                    invalidate();
                }
                return true;
                
            case 263: // GLFW_KEY_LEFT
                if (cursorPosition > 0) {
                    cursorPosition--;
                    if (!shift) clearSelection();
                    updateScrollOffset();
                    resetCursorBlink();
                    invalidate();
                }
                return true;
                
            case 268: // GLFW_KEY_HOME
                cursorPosition = 0;
                if (!shift) clearSelection();
                updateScrollOffset();
                resetCursorBlink();
                invalidate();
                return true;
                
            case 269: // GLFW_KEY_END
                cursorPosition = text.length();
                if (!shift) clearSelection();
                updateScrollOffset();
                resetCursorBlink();
                invalidate();
                return true;
                
            case 65: // GLFW_KEY_A
                if (ctrl) {
                    selectAll();
                    resetCursorBlink();
                    return true;
                }
                break;
        }
        
        return super.onKeyPress(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean onCharInput(int codepoint) {
        char character = (char) codepoint;
        if (!isEnabled() || !isVisible() || !isFocused() || readOnly) {
            return false;
        }
        
        // Filter out control characters
        if (character < 32 || character == 127) {
            return false;
        }
        
        // Apply input filter
        if (inputFilter != null && !inputFilter.isValid(character)) {
            return false;
        }
        
        insertText(String.valueOf(character));
        resetCursorBlink();
        
        return true;
    }
    
    private void resetCursorBlink() {
        cursorVisible = true;
        lastCursorBlink = System.currentTimeMillis();
    }
    
    protected void onFocusChanged(boolean focused) {
        if (focused) {
            resetCursorBlink();
        } else {
            clearSelection();
        }
        invalidate();
    }
    
    /**
     * Common input filters
     */
    public static class InputFilters {
        public static final InputFilter NUMERIC = new InputFilter() {
            @Override
            public boolean isValid(char character) {
                return Character.isDigit(character) || character == '.' || character == '-';
            }
            
            @Override
            public String filter(String input) {
                return input.replaceAll("[^0-9.-]", "");
            }
        };
        
        public static final InputFilter ALPHA = new InputFilter() {
            @Override
            public boolean isValid(char character) {
                return Character.isLetter(character);
            }
            
            @Override
            public String filter(String input) {
                return input.replaceAll("[^a-zA-Z]", "");
            }
        };
        
        public static final InputFilter ALPHANUMERIC = new InputFilter() {
            @Override
            public boolean isValid(char character) {
                return Character.isLetterOrDigit(character);
            }
            
            @Override
            public String filter(String input) {
                return input.replaceAll("[^a-zA-Z0-9]", "");
            }
        };
    }
}