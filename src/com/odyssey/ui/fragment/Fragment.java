package com.odyssey.ui.fragment;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.ui.lifecycle.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Base Fragment class for UI composition and lifecycle management
 * Inspired by Android's Fragment system
 */
public abstract class Fragment implements LifecycleOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {
    private static final String TAG = "Fragment";
    
    // Fragment states
    public enum State {
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }
    
    // Fragment lifecycle
    private State currentState = State.CREATED;
    private FragmentManager fragmentManager;
    private Fragment parentFragment;
    private final List<Fragment> childFragments = new ArrayList<>();
    
    // Lifecycle components
    private final SimpleLifecycleOwner lifecycleOwner = new SimpleLifecycleOwner();
    private final ViewModelStore viewModelStore = new ViewModelStore();
    private final OnBackPressedDispatcher onBackPressedDispatcher = new OnBackPressedDispatcher();
    
    // Fragment properties
    private String tag;
    private int id = -1;
    private boolean added = false;
    private boolean detached = false;
    private boolean hidden = false;
    private boolean removing = false;
    
    // Fragment view
    private UIComponent view;
    private FragmentContainerView container;
    
    // Fragment arguments
    private Bundle arguments;
    
    /**
     * Simple Bundle implementation for fragment arguments
     */
    public static class Bundle {
        private final java.util.Map<String, Object> data = new java.util.HashMap<>();
        
        public void putString(String key, String value) {
            data.put(key, value);
        }
        
        public void putInt(String key, int value) {
            data.put(key, value);
        }
        
        public void putFloat(String key, float value) {
            data.put(key, value);
        }
        
        public void putBoolean(String key, boolean value) {
            data.put(key, value);
        }
        
        public String getString(String key) {
            return getString(key, null);
        }
        
        public String getString(String key, String defaultValue) {
            Object value = data.get(key);
            return value instanceof String ? (String) value : defaultValue;
        }
        
        public int getInt(String key) {
            return getInt(key, 0);
        }
        
        public int getInt(String key, int defaultValue) {
            Object value = data.get(key);
            return value instanceof Integer ? (Integer) value : defaultValue;
        }
        
        public float getFloat(String key) {
            return getFloat(key, 0.0f);
        }
        
        public float getFloat(String key, float defaultValue) {
            Object value = data.get(key);
            return value instanceof Float ? (Float) value : defaultValue;
        }
        
        public boolean getBoolean(String key) {
            return getBoolean(key, false);
        }
        
        public boolean getBoolean(String key, boolean defaultValue) {
            Object value = data.get(key);
            return value instanceof Boolean ? (Boolean) value : defaultValue;
        }
        
        public boolean containsKey(String key) {
            return data.containsKey(key);
        }
        
        public void clear() {
            data.clear();
        }
    }
    
    /**
     * Default constructor
     */
    public Fragment() {
        // Empty constructor required
    }
    
    // Lifecycle methods
    
    /**
     * Called when the fragment is first created
     */
    public void onCreate(Bundle savedInstanceState) {
        currentState = State.CREATED;
        System.out.println("Fragment onCreate: " + getClass().getSimpleName());
    }
    
    /**
     * Called to create the fragment's view
     */
    public abstract UIComponent onCreateView(FragmentContainerView container, Bundle savedInstanceState);
    
    /**
     * Called after the view is created
     */
    public void onViewCreated(UIComponent view, Bundle savedInstanceState) {
        this.view = view;
        System.out.println("Fragment onViewCreated: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment becomes visible
     */
    public void onStart() {
        currentState = State.STARTED;
        System.out.println("Fragment onStart: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment becomes active
     */
    public void onResume() {
        currentState = State.RESUMED;
        System.out.println("Fragment onResume: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment becomes inactive
     */
    public void onPause() {
        currentState = State.PAUSED;
        System.out.println("Fragment onPause: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment becomes invisible
     */
    public void onStop() {
        currentState = State.STOPPED;
        System.out.println("Fragment onStop: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment's view is destroyed
     */
    public void onDestroyView() {
        this.view = null;
        System.out.println("Fragment onDestroyView: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment is destroyed
     */
    public void onDestroy() {
        currentState = State.DESTROYED;
        System.out.println("Fragment onDestroy: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the fragment is detached
     */
    public void onDetach() {
        this.fragmentManager = null;
        this.parentFragment = null;
        System.out.println("Fragment onDetach: " + getClass().getSimpleName());
    }
    
    // Fragment management methods
    
    /**
     * Get the fragment manager
     */
    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }
    
    /**
     * Get the child fragment manager
     */
    public FragmentManager getChildFragmentManager() {
        // TODO: Implement child fragment manager
        return null;
    }
    
    /**
     * Get the parent fragment
     */
    public Fragment getParentFragment() {
        return parentFragment;
    }
    
    /**
     * Get the fragment's view
     */
    public UIComponent getView() {
        return view;
    }
    
    /**
     * Get the fragment's container
     */
    public FragmentContainerView getContainer() {
        return container;
    }
    
    /**
     * Get the fragment's arguments
     */
    public Bundle getArguments() {
        return arguments;
    }
    
    /**
     * Set the fragment's arguments
     */
    public void setArguments(Bundle arguments) {
        if (currentState != State.CREATED) {
            throw new IllegalStateException("Fragment already created");
        }
        this.arguments = arguments;
    }
    
    /**
     * Get the fragment's tag
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * Get the fragment's ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Check if the fragment is added
     */
    public boolean isAdded() {
        return added;
    }
    
    /**
     * Check if the fragment is detached
     */
    public boolean isDetached() {
        return detached;
    }
    
    /**
     * Check if the fragment is hidden
     */
    public boolean isHidden() {
        return hidden;
    }
    
    /**
     * Check if the fragment is being removed
     */
    public boolean isRemoving() {
        return removing;
    }
    
    /**
     * Check if the fragment is visible
     */
    public boolean isVisible() {
        return added && !hidden && !detached && view != null;
    }
    
    /**
     * Check if the fragment is resumed
     */
    public boolean isResumed() {
        return currentState == State.RESUMED;
    }
    
    /**
     * Get the current state
     */
    public State getState() {
        return currentState;
    }
    
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleOwner.getLifecycle();
    }
    
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
    
    @Override
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return onBackPressedDispatcher;
    }
    
    // Internal methods for FragmentManager
    
    void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }
    
    void setParentFragment(Fragment parentFragment) {
        this.parentFragment = parentFragment;
    }
    
    void setTag(String tag) {
        this.tag = tag;
    }
    
    void setId(int id) {
        this.id = id;
    }
    
    void setAdded(boolean added) {
        this.added = added;
    }
    
    void setDetached(boolean detached) {
        this.detached = detached;
    }
    
    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    void setRemoving(boolean removing) {
        this.removing = removing;
    }
    
    void setContainer(FragmentContainerView container) {
        this.container = container;
    }
    
    void setState(State state) {
        this.currentState = state;
    }
    
    void performCreate(Bundle savedInstanceState) {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        onCreate(savedInstanceState);
    }
    
    void performCreateView(FragmentContainerView container, Bundle savedInstanceState) {
        this.container = container;
        UIComponent view = onCreateView(container, savedInstanceState);
        if (view != null) {
            onViewCreated(view, savedInstanceState);
        }
    }
    
    void performStart() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        onStart();
    }
    
    void performResume() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        onResume();
    }
    
    void performPause() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        onPause();
    }
    
    void performStop() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        onStop();
    }
    
    void performDestroyView() {
        onDestroyView();
    }
    
    void performDestroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        onDestroy();
        viewModelStore.clear();
    }
    
    void performDetach() {
        onDetach();
    }
    
    @Override
    public String toString() {
        return String.format("%s[id=%d, tag=%s, state=%s]", 
                getClass().getSimpleName(), id, tag, currentState);
    }
}