package com.odyssey.ui.lifecycle;

/**
 * ViewModelProvider for creating and retrieving ViewModels
 */
public class ViewModelProvider {
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