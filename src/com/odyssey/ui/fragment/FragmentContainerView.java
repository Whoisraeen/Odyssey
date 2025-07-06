package com.odyssey.ui.fragment;

import com.odyssey.ui.UIComponent;
import com.odyssey.ui.UIRenderer;
import com.odyssey.ui.layout.LayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Container view for hosting fragments
 * Manages fragment layout and rendering within a specific area
 */
public class FragmentContainerView extends UIComponent {
    private static final String TAG = "FragmentContainerView";
    
    // Container properties
    private int containerId;
    private Fragment currentFragment;
    private final List<Fragment> fragmentStack = new ArrayList<>();
    
    // Layout properties
    private boolean clipChildren = true;
    private boolean measureAllChildren = false;
    
    /**
     * Constructor
     */
    public FragmentContainerView(int containerId) {
        super();
        this.containerId = containerId;
        setFocusable(false); // Container itself is not focusable
    }
    
    /**
     * Constructor with position and size
     */
    public FragmentContainerView(int containerId, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.containerId = containerId;
        setFocusable(false);
    }
    
    /**
     * Get the container ID
     */
    public int getContainerId() {
        return containerId;
    }
    
    /**
     * Set the current fragment
     */
    public void setFragment(Fragment fragment) {
        if (currentFragment != null) {
            // Remove current fragment's view
            if (currentFragment.getView() != null) {
                removeChild(currentFragment.getView());
            }
        }
        
        this.currentFragment = fragment;
        
        if (fragment != null) {
            // Add new fragment's view
            if (fragment.getView() != null) {
                addChild(fragment.getView());
                // Make fragment view fill the container
                fragment.getView().setPosition(0, 0);
                fragment.getView().setSize(getWidth(), getHeight());
            }
        }
        
        requestLayout();
    }
    
    /**
     * Get the current fragment
     */
    public Fragment getCurrentFragment() {
        return currentFragment;
    }
    
    /**
     * Add fragment to stack
     */
    public void addFragmentToStack(Fragment fragment) {
        if (!fragmentStack.contains(fragment)) {
            fragmentStack.add(fragment);
            System.out.println("Fragment added to stack: " + fragment + " in container " + containerId);
        }
    }
    
    /**
     * Remove fragment from stack
     */
    public void removeFragmentFromStack(Fragment fragment) {
        if (fragmentStack.remove(fragment)) {
            System.out.println("Fragment removed from stack: " + fragment + " in container " + containerId);
        }
    }
    
    /**
     * Get fragment stack
     */
    public List<Fragment> getFragmentStack() {
        return new ArrayList<>(fragmentStack);
    }
    
    /**
     * Clear all fragments
     */
    public void clearFragments() {
        if (currentFragment != null) {
            if (currentFragment.getView() != null) {
                removeChild(currentFragment.getView());
            }
            currentFragment = null;
        }
        fragmentStack.clear();
        requestLayout();
    }
    
    /**
     * Set whether to clip children to container bounds
     */
    public void setClipChildren(boolean clipChildren) {
        this.clipChildren = clipChildren;
    }
    
    /**
     * Check if children are clipped
     */
    public boolean getClipChildren() {
        return clipChildren;
    }
    
    /**
     * Set whether to measure all children
     */
    public void setMeasureAllChildren(boolean measureAllChildren) {
        this.measureAllChildren = measureAllChildren;
    }
    
    /**
     * Check if all children are measured
     */
    public boolean getMeasureAllChildren() {
        return measureAllChildren;
    }
    
    @Override
    protected void onLayout(boolean changed, float left, float top, float right, float bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
        // Layout current fragment to fill container
        if (currentFragment != null && currentFragment.getView() != null) {
            UIComponent fragmentView = currentFragment.getView();
            fragmentView.setPosition(0, 0);
            fragmentView.setSize(getWidth(), getHeight());
            fragmentView.layout(0, 0, getWidth(), getHeight());
        }
    }
    
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Measure the container itself
        super.onMeasure(widthSpec, heightSpec);
        
        // Measure current fragment
        if (currentFragment != null && currentFragment.getView() != null) {
            UIComponent fragmentView = currentFragment.getView();
            int childWidthSpec = LayoutManager.MeasureSpec.makeMeasureSpec((int)getWidth(), LayoutManager.MeasureSpec.EXACTLY);
            int childHeightSpec = LayoutManager.MeasureSpec.makeMeasureSpec((int)getHeight(), LayoutManager.MeasureSpec.EXACTLY);
            fragmentView.measure(childWidthSpec, childHeightSpec);
        }
        
        // Optionally measure all fragments in stack
        if (measureAllChildren) {
            for (Fragment fragment : fragmentStack) {
                if (fragment.getView() != null) {
                    int childWidthSpec = LayoutManager.MeasureSpec.makeMeasureSpec((int)getWidth(), LayoutManager.MeasureSpec.EXACTLY);
                    int childHeightSpec = LayoutManager.MeasureSpec.makeMeasureSpec((int)getHeight(), LayoutManager.MeasureSpec.EXACTLY);
                    fragment.getView().measure(childWidthSpec, childHeightSpec);
                }
            }
        }
    }
    
    @Override
    public void render(UIRenderer renderer, float deltaTime) {
        if (!isVisible()) {
            return;
        }
        
        // Set up clipping if enabled
        if (clipChildren) {
            // TODO: Implement scissor test for clipping
            // GL11.glEnable(GL11.GL_SCISSOR_TEST);
            // GL11.glScissor((int)getAbsoluteX(), (int)getAbsoluteY(), (int)getWidth(), (int)getHeight());
        }
        
        // Render background
        if (getParent() != null) {
            // Call parent's render method properly
            // super.render(renderer, deltaTime);
        }
        
        // Render current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            currentFragment.getView().render(renderer, deltaTime);
        }
        
        // Restore clipping state
        if (clipChildren) {
            // TODO: Restore scissor test
            // GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    
    @Override
    public boolean onMouseClick(double x, double y, int button) {
        // Check if click is within container bounds
        if (!isPointInside(x, y)) {
            return false;
        }
        
        // Forward to current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            // Convert coordinates to fragment's local space
            double localX = x - getX();
            double localY = y - getY();
            
            if (currentFragment.getView().onMouseClick(localX, localY, button)) {
                return true;
            }
        }
        
        // Handle by container itself
        return super.onMouseClick(x, y, button);
    }
    
    @Override
    public boolean onMouseMove(double x, double y) {
        // Forward to current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            // Convert coordinates to fragment's local space
            double localX = x - getX();
            double localY = y - getY();
            
            if (currentFragment.getView().onMouseMove(localX, localY)) {
                return true;
            }
        }
        
        return super.onMouseMove(x, y);
    }
    
    @Override
    public boolean onKeyPress(int key, int scancode, int mods) {
        // Forward to current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            if (currentFragment.getView().onKeyPress(key, scancode, mods)) {
                return true;
            }
        }
        
        return super.onKeyPress(key, scancode, mods);
    }
    
    @Override
    public boolean onCharInput(int codepoint) {
        // Forward to current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            if (currentFragment.getView().onCharInput(codepoint)) {
                return true;
            }
        }
        
        return super.onCharInput(codepoint);
    }
    
    @Override
    public boolean onScroll(double xOffset, double yOffset) {
        // Forward to current fragment
        if (currentFragment != null && currentFragment.getView() != null && currentFragment.isVisible()) {
            if (currentFragment.getView().onScroll(xOffset, yOffset)) {
                return true;
            }
        }
        
        return super.onScroll(xOffset, yOffset);
    }
    
    /**
     * Find fragment by tag in the stack
     */
    public Fragment findFragmentByTag(String tag) {
        if (tag == null) {
            return null;
        }
        
        for (Fragment fragment : fragmentStack) {
            if (tag.equals(fragment.getTag())) {
                return fragment;
            }
        }
        
        return null;
    }
    
    /**
     * Find fragment by ID in the stack
     */
    public Fragment findFragmentById(int id) {
        for (Fragment fragment : fragmentStack) {
            if (fragment.getId() == id) {
                return fragment;
            }
        }
        
        return null;
    }
    
    /**
     * Check if container has any fragments
     */
    public boolean hasFragments() {
        return currentFragment != null || !fragmentStack.isEmpty();
    }
    
    /**
     * Get fragment count
     */
    public int getFragmentCount() {
        return fragmentStack.size();
    }
    
    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        
        // Update current fragment size
        if (currentFragment != null && currentFragment.getView() != null) {
            currentFragment.getView().setSize(width, height);
        }
    }
    
    @Override
    public String toString() {
        return String.format("FragmentContainerView[id=%d, fragments=%d, current=%s]", 
                containerId, fragmentStack.size(), 
                currentFragment != null ? currentFragment.getClass().getSimpleName() : "none");
    }
}