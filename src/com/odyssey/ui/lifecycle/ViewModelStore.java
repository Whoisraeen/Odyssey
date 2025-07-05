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
 * Base ViewModel class
 */
abstract class ViewModel {
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

/**
 * ViewModelStoreOwner interface
 */
interface ViewModelStoreOwner {
    /**
     * Returns the ViewModelStore associated with this owner
     */
    ViewModelStore getViewModelStore();
}

/**
 * ViewModelProvider for creating and retrieving ViewModels
 */
class ViewModelProvider {
    private final Factory factory;
    private final ViewModelStore store;
    
    /**
     * Factory interface for creating ViewModels
     */
    public interface Factory {
        <T extends ViewModel> T create(Class<T> modelClass);
    }
    
    /**
     * Default factory implementation
     */
    public static class NewInstanceFactory implements Factory {
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            try {
                return modelClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }
    
    /**
     * Constructor
     */
    public ViewModelProvider(ViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), new NewInstanceFactory());
    }
    
    /**
     * Constructor with factory
     */
    public ViewModelProvider(ViewModelStoreOwner owner, Factory factory) {
        this(owner.getViewModelStore(), factory);
    }
    
    /**
     * Constructor with store and factory
     */
    public ViewModelProvider(ViewModelStore store, Factory factory) {
        this.factory = factory;
        this.store = store;
    }
    
    /**
     * Get a ViewModel
     */
    public <T extends ViewModel> T get(Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }
    
    /**
     * Get a ViewModel with key
     */
    public <T extends ViewModel> T get(String key, Class<T> modelClass) {
        ViewModel viewModel = store.get(key);
        
        if (modelClass.isInstance(viewModel)) {
            return (T) viewModel;
        }
        
        viewModel = factory.create(modelClass);
        store.put(key, viewModel);
        return (T) viewModel;
    }
    
    private static final String DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey";
}

/**
 * Simple ViewModelStoreOwner implementation
 */
class SimpleViewModelStoreOwner implements ViewModelStoreOwner {
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

/**
 * HasDefaultViewModelProviderFactory interface
 */
interface HasDefaultViewModelProviderFactory {
    /**
     * Returns the default ViewModelProvider.Factory
     */
    ViewModelProvider.Factory getDefaultViewModelProviderFactory();
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