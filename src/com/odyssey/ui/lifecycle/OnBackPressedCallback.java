package com.odyssey.ui.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract callback for handling back press events
 */
public abstract class OnBackPressedCallback {
    private boolean enabled;
    private final List<OnBackPressedDispatcher.OnBackPressedCancellable> cancellables = new ArrayList<>();
    
    /**
     * Constructor
     */
    public OnBackPressedCallback(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Set enabled state
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if enabled
     */
    public final boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Remove this callback
     */
    public final void remove() {
        for (OnBackPressedDispatcher.OnBackPressedCancellable cancellable : cancellables) {
            cancellable.cancel();
        }
    }
    
    /**
     * Handle back press event
     */
    public abstract void handleOnBackPressed();
    
    /**
     * Add cancellable (package-private)
     */
    void addCancellable(OnBackPressedDispatcher.OnBackPressedCancellable cancellable) {
        cancellables.add(cancellable);
    }
    
    /**
     * Remove cancellable (package-private)
     */
    void removeCancellable(OnBackPressedDispatcher.OnBackPressedCancellable cancellable) {
        cancellables.remove(cancellable);
    }
}