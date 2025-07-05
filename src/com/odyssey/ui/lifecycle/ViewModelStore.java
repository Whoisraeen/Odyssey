package com.odyssey.ui.lifecycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ViewModelStore for storing and managing ViewModels
 * Inspired by Android's ViewModelStore
 */
public class ViewModelStore {
    private final Map<String, ViewModel> map = new ConcurrentHashMap<>();
    
    /**
     * Put a ViewModel in the store
     */
    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = map.put(key, viewModel);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
    }
    
    /**
     * Get a ViewModel from the store
     */
    final ViewModel get(String key) {
        return map.get(key);
    }
    
    /**
     * Get all keys
     */
    Set<String> keys() {
        return new HashSet<>(map.keySet());
    }
    
    /**
     * Clear all ViewModels
     */
    public final void clear() {
        for (ViewModel vm : map.values()) {
            vm.onCleared();
        }
        map.clear();
    }
}



/**
 * ViewModelLazy for lazy initialization of ViewModels
 */
class ViewModelLazy<VM extends ViewModel> {
    private final Class<VM> viewModelClass;
    private final ViewModelStoreOwner storeOwner;
    private final ViewModelProvider.Factory factory;
    private VM cached;
    
    public ViewModelLazy(Class<VM> viewModelClass, ViewModelStoreOwner storeOwner) {
        this(viewModelClass, storeOwner, null);
    }
    
    public ViewModelLazy(Class<VM> viewModelClass, ViewModelStoreOwner storeOwner, 
                        ViewModelProvider.Factory factory) {
        this.viewModelClass = viewModelClass;
        this.storeOwner = storeOwner;
        this.factory = factory;
    }
    
    public VM getValue() {
        if (cached == null) {
            ViewModelProvider.Factory f = factory;
            if (f == null) {
                f = storeOwner instanceof HasDefaultViewModelProviderFactory ?
                    ((HasDefaultViewModelProviderFactory) storeOwner).getDefaultViewModelProviderFactory() :
                    new ViewModelProvider.NewInstanceFactory();
            }
            cached = new ViewModelProvider(storeOwner, f).get(viewModelClass);
        }
        return cached;
    }
    
    public boolean isInitialized() {
        return cached != null;
    }
}