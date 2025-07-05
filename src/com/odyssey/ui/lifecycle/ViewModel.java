package com.odyssey.ui.lifecycle;

/**
 * Base ViewModel class
 */
public abstract class ViewModel {
    private volatile boolean cleared = false;
    
    /**
     * Called when the ViewModel is no longer used and will be destroyed
     */
    protected void onCleared() {
        // Override in subclasses
    }
    
    /**
     * Mark as cleared
     */
    final void clear() {
        cleared = true;
        onCleared();
    }
    
    /**
     * Check if cleared
     */
    public final boolean isCleared() {
        return cleared;
    }
}