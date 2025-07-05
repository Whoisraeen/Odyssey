package com.odyssey.ui;

import com.odyssey.rendering.ui.FontManager;
import com.odyssey.rendering.ui.TextRenderer;
import com.odyssey.ui.threading.UIThread;
import com.odyssey.ui.threading.Handler;
import com.odyssey.ui.threading.Message;
import com.odyssey.ui.layout.LayoutManager;
import com.odyssey.ui.animation.AnimationManager;
import com.odyssey.input.InputManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UI Manager for Odyssey Game with multi-threaded architecture
 * Inspired by ModernUI-MC architecture
 * Manages the entire UI system including rendering, layout, and event handling using dedicated UI thread
 */
public class UIManager {
    private static UIManager instance;
    
    // Threading components
    private UIThread uiThread;
    private Handler uiHandler;
    
    // UI Components
    private final List<UIComponent> components = new CopyOnWriteArrayList<>();
    private final List<UIScreen> screenStack = new ArrayList<>();
    private UIScreen currentScreen;
    
    // Rendering
    private TextRenderer textRenderer;
    private UIRenderer uiRenderer;
    private boolean initialized = false;
    
    // Layout and Animation
    private final LayoutManager layoutManager;
    private final AnimationManager animationManager;
    
    // Input handling
    private InputManager inputManager;
    
    // Theme and styling
    private UITheme currentTheme;
    
    // Message types for UI thread communication
    private static final int MSG_RENDER = 1;
    private static final int MSG_LAYOUT = 2;
    private static final int MSG_INPUT = 3;
    private static final int MSG_ANIMATION = 4;
    
    private UIManager() {
        this.layoutManager = new com.odyssey.ui.layout.LinearLayout(null);
        this.animationManager = new AnimationManager();
        this.inputManager = null; // Will be set later via setInputManager
        this.currentTheme = new UITheme();
    }
    
    public void setInputManager(InputManager inputManager) {
        this.inputManager = inputManager;
    }
    
    public static UIManager getInstance() {
        if (instance == null) {
            instance = new UIManager();
        }
        return instance;
    }
    
    public void initialize() {
        if (initialized) {
            System.out.println("UIManager already initialized");
            return;
        }
        
        System.out.println("UIManager initializing with multi-threaded architecture...");
        
        try {
            // Start UI thread
            uiThread = UIThread.getInstance();
            uiThread.start();
            uiHandler = uiThread.getHandler();
            
            // Post initialization to UI thread
            uiHandler.post(() -> initializeOnUIThread());
            
            initialized = true;
            System.out.println("UIManager initialized successfully with UI thread");
            
        } catch (Exception e) {
            System.err.println("UIManager: Failed to initialize - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("UIManager initialization failed", e);
        }
    }
    
    /**
     * Initialize components on the UI thread
     */
    private void initializeOnUIThread() {
        try {
            // Initialize text rendering
            this.textRenderer = FontManager.getInstance().getTextRenderer();
            if (textRenderer == null) {
                System.err.println("UIManager: Failed to initialize text renderer");
                return;
            }
            
            // Initialize UI renderer
            this.uiRenderer = new UIRenderer(textRenderer);
            
            // Setup OpenGL state for UI rendering
            setupOpenGLState();
            
            System.out.println("UI components initialized on UI thread");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize UI components on UI thread: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupOpenGLState() {
        // Enable blending for transparency
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Enable depth testing for proper layering
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }
    
    public void render(float deltaTime) {
        if (!initialized || uiHandler == null) return;
        
        // Post render task to UI thread
        uiHandler.post(() -> renderOnUIThread(deltaTime));
    }
    
    /**
     * Perform rendering on the UI thread
     */
    private void renderOnUIThread(float deltaTime) {
        if (uiRenderer == null) return;
        
        // Update animations
        animationManager.update(deltaTime);
        
        // Perform layout if needed
        layoutManager.performLayout();
        
        // Render current screen
        if (currentScreen != null) {
            uiRenderer.beginFrame();
            currentScreen.render(uiRenderer, deltaTime);
            uiRenderer.endFrame();
        }
        
        // Render floating components (tooltips, modals, etc.)
        renderFloatingComponents(deltaTime);
    }
    
    private void renderFloatingComponents(float deltaTime) {
        for (UIComponent component : components) {
            if (component.isFloating() && component.isVisible()) {
                component.render(uiRenderer, deltaTime);
            }
        }
    }
    
    public void pushScreen(UIScreen screen) {
        if (currentScreen != null) {
            screenStack.add(currentScreen);
            currentScreen.onPause();
        }
        
        currentScreen = screen;
        screen.onResume();
        layoutManager.requestLayout();
    }
    
    public void popScreen() {
        if (currentScreen != null) {
            currentScreen.onPause();
        }
        
        if (!screenStack.isEmpty()) {
            currentScreen = screenStack.remove(screenStack.size() - 1);
            currentScreen.onResume();
        } else {
            currentScreen = null;
        }
        
        layoutManager.requestLayout();
    }
    
    public void setScreen(UIScreen screen) {
        if (currentScreen != null) {
            currentScreen.onPause();
        }
        
        screenStack.clear();
        currentScreen = screen;
        
        if (screen != null) {
            screen.onResume();
        }
        
        layoutManager.requestLayout();
    }
    
    public void addComponent(UIComponent component) {
        components.add(component);
        layoutManager.requestLayout();
    }
    
    public void removeComponent(UIComponent component) {
        components.remove(component);
        layoutManager.requestLayout();
    }
    
    // Input handling - post to UI thread
    public boolean handleMouseClick(double x, double y, int button) {
        if (uiHandler != null && inputManager != null) {
            uiHandler.post(() -> inputManager.handleMouseClick(x, y, button, currentScreen, components));
            return true;
        }
        return false;
    }
    
    public boolean handleMouseMove(double x, double y) {
        if (uiHandler != null && inputManager != null) {
            uiHandler.post(() -> inputManager.handleMouseMove(x, y, currentScreen, components));
            return true;
        }
        return false;
    }
    
    public boolean handleKeyPress(int key, int scancode, int mods) {
        if (uiHandler != null && inputManager != null) {
            uiHandler.post(() -> inputManager.handleKeyPress(key, scancode, mods, currentScreen, components));
            return true;
        }
        return false;
    }
    
    public boolean handleCharInput(int codepoint) {
        if (uiHandler != null && inputManager != null) {
            uiHandler.post(() -> inputManager.handleCharInput(codepoint, currentScreen, components));
            return true;
        }
        return false;
    }
    
    public boolean handleScroll(double xOffset, double yOffset) {
        if (uiHandler != null && inputManager != null) {
            uiHandler.post(() -> inputManager.handleScroll(xOffset, yOffset, currentScreen, components));
            return true;
        }
        return false;
    }
    
    // Getters
    public UIScreen getCurrentScreen() {
        return currentScreen;
    }
    
    public TextRenderer getTextRenderer() {
        return textRenderer;
    }
    
    public UIRenderer getUIRenderer() {
        return uiRenderer;
    }
    
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
    
    public InputManager getInputManager() {
        return inputManager;
    }
    
    public UITheme getCurrentTheme() {
        return currentTheme;
    }
    
    public void setTheme(UITheme theme) {
        this.currentTheme = theme;
        layoutManager.requestLayout();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void cleanup() {
        if (uiHandler != null) {
            uiHandler.post(() -> {
                if (uiRenderer != null) {
                    uiRenderer.cleanup();
                }
                
                components.clear();
                screenStack.clear();
                currentScreen = null;
            });
        }
        
        // Stop UI thread
        if (uiThread != null) {
            uiThread.quit();
        }
        
        initialized = false;
    }
}