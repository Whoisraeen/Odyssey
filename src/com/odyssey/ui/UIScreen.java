package com.odyssey.ui;

import com.odyssey.ui.fragment.Fragment;
import com.odyssey.ui.fragment.FragmentManager;
import com.odyssey.ui.lifecycle.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for UI screens in the Odyssey UI system
 * Represents a full-screen UI state with lifecycle management
 */
public abstract class UIScreen implements LifecycleOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {
    
    private static final String TAG = "UIScreen";
    
    // Screen state
    public enum State {
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }
    
    // Screen properties
    protected String title = "";
    protected boolean visible = false;
    protected boolean focusable = true;
    protected State currentState = State.CREATED;
    
    // Lifecycle components
    private final SimpleLifecycleOwner lifecycleOwner = new SimpleLifecycleOwner();
    private final ViewModelStore viewModelStore = new ViewModelStore();
    private final OnBackPressedDispatcher onBackPressedDispatcher = new OnBackPressedDispatcher();
    
    // Fragment management
    private FragmentManager fragmentManager;
    private final List<Fragment> fragments = new ArrayList<>();
    
    // Root component
    protected UIComponent rootComponent;
    
    /**
     * Default constructor
     */
    public UIScreen() {
        this.fragmentManager = new FragmentManager(this);
    }
    
    /**
     * Constructor with title
     */
    public UIScreen(String title) {
        this();
        this.title = title;
    }
    
    // Lifecycle methods
    
    /**
     * Called when the screen is first created
     */
    public void onCreate() {
        currentState = State.CREATED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.CREATED);
        System.out.println("Screen onCreate: " + getClass().getSimpleName());
    }
    
    /**
     * Called to create the screen's UI
     */
    public abstract UIComponent onCreateView();
    
    /**
     * Called after the view is created
     */
    public void onViewCreated(UIComponent view) {
        this.rootComponent = view;
        System.out.println("Screen onViewCreated: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen becomes visible
     */
    public void onStart() {
        currentState = State.STARTED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.STARTED);
        visible = true;
        System.out.println("Screen onStart: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen becomes active
     */
    public void onResume() {
        currentState = State.RESUMED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.RESUMED);
        System.out.println("Screen onResume: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen becomes inactive
     */
    public void onPause() {
        currentState = State.PAUSED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.STARTED);
        System.out.println("Screen onPause: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen becomes invisible
     */
    public void onStop() {
        currentState = State.STOPPED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.CREATED);
        visible = false;
        System.out.println("Screen onStop: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen's view is destroyed
     */
    public void onDestroyView() {
        this.rootComponent = null;
        System.out.println("Screen onDestroyView: " + getClass().getSimpleName());
    }
    
    /**
     * Called when the screen is destroyed
     */
    public void onDestroy() {
        currentState = State.DESTROYED;
        lifecycleOwner.getLifecycleRegistry().setCurrentState(Lifecycle.State.DESTROYED);
        viewModelStore.clear();
        System.out.println("Screen onDestroy: " + getClass().getSimpleName());
    }
    
    // Input handling
    
    /**
     * Handle mouse click events
     */
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (rootComponent != null && visible) {
            return rootComponent.onMouseClick(mouseX, mouseY, button);
        }
        return false;
    }
    
    /**
     * Handle mouse move events
     */
    public boolean onMouseMove(double mouseX, double mouseY) {
        if (rootComponent != null && visible) {
            return rootComponent.onMouseMove(mouseX, mouseY);
        }
        return false;
    }
    
    /**
     * Handle key press events
     */
    public boolean onKeyPress(int key, int scancode, int mods) {
        if (rootComponent != null && visible && focusable) {
            return rootComponent.onKeyPress(key, scancode, mods);
        }
        return false;
    }
    
    /**
     * Handle character input events
     */
    public boolean onCharInput(int codepoint) {
        if (rootComponent != null && visible && focusable) {
            return rootComponent.onCharInput(codepoint);
        }
        return false;
    }
    
    /**
     * Handle scroll events
     */
    public boolean onScroll(double xOffset, double yOffset) {
        if (rootComponent != null && visible) {
            return rootComponent.onScroll(xOffset, yOffset);
        }
        return false;
    }
    
    /**
     * Handle back button press
     */
    public boolean onBackPressed() {
        onBackPressedDispatcher.onBackPressed();
        return true;
    }
    
    // Rendering
    
    /**
     * Render the screen
     */
    public void render(UIRenderer renderer, float deltaTime) {
        if (rootComponent != null && visible) {
            rootComponent.render(renderer, deltaTime);
        }
    }
    
    // Getters and setters
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public boolean isFocusable() {
        return focusable;
    }
    
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }
    
    public State getCurrentState() {
        return currentState;
    }
    
    public UIComponent getRootComponent() {
        return rootComponent;
    }
    
    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }
    
    // Lifecycle interface implementations
    
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
    
    // Fragment management helpers
    
    /**
     * Add a fragment to this screen
     */
    public void addFragment(Fragment fragment) {
        fragments.add(fragment);
        fragmentManager.beginTransaction()
            .add(fragment)
            .commit();
    }
    
    /**
     * Remove a fragment from this screen
     */
    public void removeFragment(Fragment fragment) {
        fragments.remove(fragment);
        fragmentManager.beginTransaction()
            .remove(fragment)
            .commit();
    }
    
    /**
     * Replace the current fragment
     */
    public void replaceFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
            .replace(fragment)
            .commit();
    }
    
    /**
     * Get all fragments in this screen
     */
    public List<Fragment> getFragments() {
        return new ArrayList<>(fragments);
    }
}