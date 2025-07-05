package com.odyssey.ui.lifecycle;

/**
 * Simple ViewModelStoreOwner implementation
 */
public class SimpleViewModelStoreOwner implements ViewModelStoreOwner {
    private final ViewModelStore viewModelStore = new ViewModelStore();
    
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
    
    /**
     * Clear the ViewModelStore
     */
    public void clearViewModelStore() {
        viewModelStore.clear();
    }
}