package com.odyssey.ui.animation;

import com.odyssey.ui.UIComponent;

/**
 * Base class for UI animations
 * Inspired by Android's Animator system
 */
public abstract class Animator {
    
    public enum State {
        IDLE,
        RUNNING,
        PAUSED,
        CANCELLED,
        ENDED
    }
    
    protected UIComponent target;
    protected long duration = 300; // Default 300ms
    protected long startDelay = 0;
    protected State state = State.IDLE;
    protected boolean reversed = false;
    
    // Animation listeners
    protected AnimatorListener listener;
    
    /**
     * Interface for animation event callbacks
     */
    public interface AnimatorListener {
        void onAnimationStart(Animator animation);
        void onAnimationEnd(Animator animation);
        void onAnimationCancel(Animator animation);
        void onAnimationRepeat(Animator animation);
    }
    
    /**
     * Set the target component for this animation
     */
    public void setTarget(UIComponent target) {
        this.target = target;
    }
    
    /**
     * Get the target component
     */
    public UIComponent getTarget() {
        return target;
    }
    
    /**
     * Set the duration of the animation
     */
    public Animator setDuration(long duration) {
        this.duration = duration;
        return this;
    }
    
    /**
     * Get the duration of the animation
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Set the start delay
     */
    public Animator setStartDelay(long startDelay) {
        this.startDelay = startDelay;
        return this;
    }
    
    /**
     * Get the start delay
     */
    public long getStartDelay() {
        return startDelay;
    }
    
    /**
     * Set the animation listener
     */
    public void setListener(AnimatorListener listener) {
        this.listener = listener;
    }
    
    /**
     * Start the animation
     */
    public void start() {
        if (state == State.RUNNING) {
            return;
        }
        
        state = State.RUNNING;
        if (listener != null) {
            listener.onAnimationStart(this);
        }
        
        onStart();
    }
    
    /**
     * Cancel the animation
     */
    public void cancel() {
        if (state != State.RUNNING && state != State.PAUSED) {
            return;
        }
        
        state = State.CANCELLED;
        if (listener != null) {
            listener.onAnimationCancel(this);
        }
        
        onCancel();
    }
    
    /**
     * End the animation
     */
    public void end() {
        if (state == State.ENDED) {
            return;
        }
        
        state = State.ENDED;
        if (listener != null) {
            listener.onAnimationEnd(this);
        }
        
        onEnd();
    }
    
    /**
     * Pause the animation
     */
    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            onPause();
        }
    }
    
    /**
     * Resume the animation
     */
    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            onResume();
        }
    }
    
    /**
     * Check if the animation is running
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }
    
    /**
     * Check if the animation is started
     */
    public boolean isStarted() {
        return state != State.IDLE;
    }
    
    /**
     * Update the animation with the current time
     * @param currentTime current time in milliseconds
     */
    public abstract void update(long currentTime);
    
    /**
     * Called when the animation starts
     */
    protected void onStart() {
        // Override in subclasses
    }
    
    /**
     * Called when the animation is cancelled
     */
    protected void onCancel() {
        // Override in subclasses
    }
    
    /**
     * Called when the animation ends
     */
    protected void onEnd() {
        // Override in subclasses
    }
    
    /**
     * Called when the animation is paused
     */
    protected void onPause() {
        // Override in subclasses
    }
    
    /**
     * Called when the animation is resumed
     */
    protected void onResume() {
        // Override in subclasses
    }
}