package com.odyssey.ui.animation;

import com.odyssey.ui.UIComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages animations for UI components
 * Handles animation scheduling and updates
 */
public class AnimationManager {
    
    private final List<Animator> activeAnimations = new ArrayList<>();
    private final List<Animator> pendingAnimations = new ArrayList<>();
    private final List<Animator> finishedAnimations = new ArrayList<>();
    
    private long lastUpdateTime = 0;
    private boolean isUpdating = false;
    
    /**
     * Add an animation to be managed
     */
    public void addAnimation(Animator animator) {
        if (animator == null) {
            return;
        }
        
        synchronized (pendingAnimations) {
            pendingAnimations.add(animator);
        }
    }
    
    /**
     * Remove an animation from management
     */
    public void removeAnimation(Animator animator) {
        if (animator == null) {
            return;
        }
        
        synchronized (activeAnimations) {
            activeAnimations.remove(animator);
        }
        synchronized (pendingAnimations) {
            pendingAnimations.remove(animator);
        }
        synchronized (finishedAnimations) {
            finishedAnimations.add(animator);
        }
    }
    
    /**
     * Start an animation on a component
     */
    public void startAnimation(UIComponent component, Animator animator) {
        if (component == null || animator == null) {
            return;
        }
        
        animator.setTarget(component);
        addAnimation(animator);
        animator.start();
    }
    
    /**
     * Cancel all animations on a component
     */
    public void cancelAnimations(UIComponent component) {
        if (component == null) {
            return;
        }
        
        synchronized (activeAnimations) {
            Iterator<Animator> iterator = activeAnimations.iterator();
            while (iterator.hasNext()) {
                Animator animator = iterator.next();
                if (animator.getTarget() == component) {
                    animator.cancel();
                    iterator.remove();
                }
            }
        }
    }
    
    /**
     * Update all active animations
     * Should be called every frame
     */
    public void update(long currentTime) {
        if (isUpdating) {
            return; // Prevent recursive updates
        }
        
        isUpdating = true;
        lastUpdateTime = currentTime;
        
        try {
            // Add pending animations
            synchronized (pendingAnimations) {
                if (!pendingAnimations.isEmpty()) {
                    synchronized (activeAnimations) {
                        activeAnimations.addAll(pendingAnimations);
                    }
                    pendingAnimations.clear();
                }
            }
            
            // Update active animations
            synchronized (activeAnimations) {
                Iterator<Animator> iterator = activeAnimations.iterator();
                while (iterator.hasNext()) {
                    Animator animator = iterator.next();
                    
                    if (animator.isRunning()) {
                        animator.update(currentTime);
                    } else if (animator.state == Animator.State.ENDED || 
                              animator.state == Animator.State.CANCELLED) {
                        iterator.remove();
                        synchronized (finishedAnimations) {
                            finishedAnimations.add(animator);
                        }
                    }
                }
            }
            
            // Clean up finished animations
            synchronized (finishedAnimations) {
                finishedAnimations.clear();
            }
            
        } finally {
            isUpdating = false;
        }
    }
    
    /**
     * Get the number of active animations
     */
    public int getActiveAnimationCount() {
        synchronized (activeAnimations) {
            return activeAnimations.size();
        }
    }
    
    /**
     * Check if there are any active animations
     */
    public boolean hasActiveAnimations() {
        synchronized (activeAnimations) {
            return !activeAnimations.isEmpty();
        }
    }
    
    /**
     * Clear all animations
     */
    public void clear() {
        // Cancel all active animations
        synchronized (activeAnimations) {
            for (Animator animator : activeAnimations) {
                animator.cancel();
            }
            activeAnimations.clear();
        }
        synchronized (pendingAnimations) {
            pendingAnimations.clear();
        }
        synchronized (finishedAnimations) {
            finishedAnimations.clear();
        }
    }
    
    /**
     * Create a simple alpha animation
     */
    public static Animator createAlphaAnimation(float fromAlpha, float toAlpha, long duration) {
        return new AlphaAnimator(fromAlpha, toAlpha, duration);
    }
    
    /**
     * Create a simple translation animation
     */
    public static Animator createTranslationAnimation(float fromX, float fromY, 
                                                     float toX, float toY, long duration) {
        return new TranslationAnimator(fromX, fromY, toX, toY, duration);
    }
    
    /**
     * Create a simple scale animation
     */
    public static Animator createScaleAnimation(float fromScale, float toScale, long duration) {
        return new ScaleAnimator(fromScale, toScale, duration);
    }
    
    /**
     * Simple alpha animator implementation
     */
    private static class AlphaAnimator extends Animator {
        private final float fromAlpha;
        private final float toAlpha;
        private long startTime;
        
        public AlphaAnimator(float fromAlpha, float toAlpha, long duration) {
            this.fromAlpha = fromAlpha;
            this.toAlpha = toAlpha;
            this.duration = duration;
        }
        
        @Override
        protected void onStart() {
            startTime = System.currentTimeMillis();
            if (target != null) {
                target.setAlpha(fromAlpha);
            }
        }
        
        @Override
        public void update(long currentTime) {
            if (target == null || state != State.RUNNING) {
                return;
            }
            
            long elapsed = currentTime - startTime;
            if (elapsed >= duration) {
                target.setAlpha(toAlpha);
                end();
            } else {
                float progress = (float) elapsed / duration;
                float currentAlpha = fromAlpha + (toAlpha - fromAlpha) * progress;
                target.setAlpha(currentAlpha);
            }
        }
    }
    
    /**
     * Simple translation animator implementation
     */
    private static class TranslationAnimator extends Animator {
        private final float fromX, fromY, toX, toY;
        private long startTime;
        
        public TranslationAnimator(float fromX, float fromY, float toX, float toY, long duration) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.duration = duration;
        }
        
        @Override
        protected void onStart() {
            startTime = System.currentTimeMillis();
            if (target != null) {
                target.setPosition(fromX, fromY);
            }
        }
        
        @Override
        public void update(long currentTime) {
            if (target == null || state != State.RUNNING) {
                return;
            }
            
            long elapsed = currentTime - startTime;
            if (elapsed >= duration) {
                target.setPosition(toX, toY);
                end();
            } else {
                float progress = (float) elapsed / duration;
                float currentX = fromX + (toX - fromX) * progress;
                float currentY = fromY + (toY - fromY) * progress;
                target.setPosition(currentX, currentY);
            }
        }
    }
    
    /**
     * Simple scale animator implementation
     */
    private static class ScaleAnimator extends Animator {
        private final float fromScale, toScale;
        private long startTime;
        
        public ScaleAnimator(float fromScale, float toScale, long duration) {
            this.fromScale = fromScale;
            this.toScale = toScale;
            this.duration = duration;
        }
        
        @Override
        protected void onStart() {
            startTime = System.currentTimeMillis();
            if (target != null) {
                target.setScale(fromScale);
            }
        }
        
        @Override
        public void update(long currentTime) {
            if (target == null || state != State.RUNNING) {
                return;
            }
            
            long elapsed = currentTime - startTime;
            if (elapsed >= duration) {
                target.setScale(toScale);
                end();
            } else {
                float progress = (float) elapsed / duration;
                float currentScale = fromScale + (toScale - fromScale) * progress;
                target.setScale(currentScale);
            }
        }
    }
}