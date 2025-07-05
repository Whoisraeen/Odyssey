package com.odyssey.ui.lifecycle;

/**
 * Simple lifecycle owner implementation
 */
public class SimpleLifecycleOwner implements LifecycleOwner {
    private final Lifecycle.LifecycleRegistry lifecycleRegistry;
    
    public SimpleLifecycleOwner() {
        lifecycleRegistry = new Lifecycle.LifecycleRegistry(this);
    }
    
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
    
    /**
     * Get the lifecycle registry for state management
     */
    public Lifecycle.LifecycleRegistry getLifecycleRegistry() {
        return lifecycleRegistry;
    }
    
    /**
     * Mark the lifecycle state
     */
    public void markState(Lifecycle.State state) {
        lifecycleRegistry.markState(state);
    }
    
    /**
     * Handle lifecycle event
     */
    public void handleLifecycleEvent(Lifecycle.Event event) {
        lifecycleRegistry.handleLifecycleEvent(event);
    }
}