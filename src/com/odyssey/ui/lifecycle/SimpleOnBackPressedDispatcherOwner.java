package com.odyssey.ui.lifecycle;

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