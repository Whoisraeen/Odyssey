package com.odyssey.ui.lifecycle;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import com.odyssey.ui.fragment.FragmentManager;

/**
 * OnBackPressedDispatcher for handling back button events
 * Inspired by Android's OnBackPressedDispatcher
 */
public class OnBackPressedDispatcher {
    private final List<OnBackPressedCallback> onBackPressedCallbacks = new CopyOnWriteArrayList<>();
    private boolean hasEnabledCallbacks = false;
    
    /**
     * Add a callback for handling back press events
     */
    public void addCallback(OnBackPressedCallback onBackPressedCallback) {
        addCallback(null, onBackPressedCallback);
    }
    
    /**
     * Add a callback with lifecycle owner
     */
    public void addCallback(LifecycleOwner owner, OnBackPressedCallback onBackPressedCallback) {
        if (owner != null) {
            Lifecycle lifecycle = owner.getLifecycle();
            if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
                return;
            }
            
            onBackPressedCallback.addCancellable(new LifecycleOnBackPressedCancellable(
                    lifecycle, onBackPressedCallback));
        }
        
        onBackPressedCallbacks.add(onBackPressedCallback);
        updateEnabledCallbacks();
        
        onBackPressedCallback.addCancellable(new OnBackPressedCancellableImpl(onBackPressedCallback));
    }
    
    /**
     * Handle back press event
     */
    public void onBackPressed() {
        // Iterate through callbacks in reverse order (most recent first)
        for (int i = onBackPressedCallbacks.size() - 1; i >= 0; i--) {
            OnBackPressedCallback callback = onBackPressedCallbacks.get(i);
            if (callback.isEnabled()) {
                callback.handleOnBackPressed();
                return;
            }
        }
        
        // If no callback handled the event, perform default action
        onBackPressedNotHandled();
    }
    
    /**
     * Called when back press is not handled by any callback
     */
    protected void onBackPressedNotHandled() {
        // Default implementation - can be overridden
        System.out.println("Back press not handled by any callback");
    }
    
    /**
     * Check if there are enabled callbacks
     */
    public boolean hasEnabledCallbacks() {
        return hasEnabledCallbacks;
    }
    
    /**
     * Update enabled callbacks flag
     */
    private void updateEnabledCallbacks() {
        boolean hadEnabledCallbacks = hasEnabledCallbacks;
        hasEnabledCallbacks = false;
        
        for (OnBackPressedCallback callback : onBackPressedCallbacks) {
            if (callback.isEnabled()) {
                hasEnabledCallbacks = true;
                break;
            }
        }
        
        if (hadEnabledCallbacks != hasEnabledCallbacks) {
            onHasEnabledCallbacksChanged();
        }
    }
    
    /**
     * Called when enabled callbacks state changes
     */
    protected void onHasEnabledCallbacksChanged() {
        // Can be overridden by subclasses
    }
    
    /**
     * Cancellable interface for removing callbacks
     */
    public interface OnBackPressedCancellable {
        void cancel();
    }
    
    /**
     * Implementation of OnBackPressedCancellable
     */
    private class OnBackPressedCancellableImpl implements OnBackPressedCancellable {
        private final OnBackPressedCallback callback;
        
        OnBackPressedCancellableImpl(OnBackPressedCallback callback) {
            this.callback = callback;
        }
        
        @Override
        public void cancel() {
            onBackPressedCallbacks.remove(callback);
            callback.removeCancellable(this);
            updateEnabledCallbacks();
        }
    }
    
    /**
     * Lifecycle-aware cancellable
     */
    private class LifecycleOnBackPressedCancellable implements OnBackPressedCancellable, LifecycleEventObserver {
        private final Lifecycle lifecycle;
        private final OnBackPressedCallback onBackPressedCallback;
        private OnBackPressedCancellable currentCancellable;
        
        LifecycleOnBackPressedCancellable(Lifecycle lifecycle, OnBackPressedCallback onBackPressedCallback) {
            this.lifecycle = lifecycle;
            this.onBackPressedCallback = onBackPressedCallback;
            lifecycle.addObserver(this);
        }
        
        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_START) {
                currentCancellable = new OnBackPressedCancellableImpl(onBackPressedCallback);
            } else if (event == Lifecycle.Event.ON_STOP) {
                if (currentCancellable != null) {
                    currentCancellable.cancel();
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                cancel();
            }
        }
        
        @Override
        public void cancel() {
            lifecycle.removeObserver(this);
            onBackPressedCallback.removeCancellable(this);
            if (currentCancellable != null) {
                currentCancellable.cancel();
                currentCancellable = null;
            }
        }
    }
}

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

/**
 * OnBackPressedDispatcherOwner interface
 */
public interface OnBackPressedDispatcherOwner {
    /**
     * Get the OnBackPressedDispatcher
     */
    OnBackPressedDispatcher getOnBackPressedDispatcher();
}

/**
 * Simple implementation of OnBackPressedDispatcherOwner
 */
public class SimpleOnBackPressedDispatcherOwner implements OnBackPressedDispatcherOwner {
    private final OnBackPressedDispatcher onBackPressedDispatcher = new OnBackPressedDispatcher();
    
    @Override
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return onBackPressedDispatcher;
    }
}

/**
 * Utility class for creating common back press callbacks
 */
class BackPressCallbacks {
    /**
     * Create a callback that finishes an activity
     */
    public static OnBackPressedCallback createFinishCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                System.out.println("Finishing activity due to back press");
                // In a real implementation, this would finish the activity
            }
        };
    }
    
    /**
     * Create a callback that pops the fragment back stack
     */
    public static OnBackPressedCallback createFragmentBackStackCallback(final FragmentManager fragmentManager) {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!fragmentManager.popBackStack()) {
                    // If no fragments to pop, disable this callback
                    setEnabled(false);
                }
            }
        };
    }
    
    /**
     * Create a custom callback with runnable
     */
    public static OnBackPressedCallback createCustomCallback(final Runnable action) {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                action.run();
            }
        };
    }
}