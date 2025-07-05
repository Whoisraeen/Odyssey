package com.odyssey.ui.fragment;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FragmentManager for managing fragment transactions and backstack
 * Inspired by Android's FragmentManager
 */
public class FragmentManager {
    private static final String TAG = "FragmentManager";
    
    // Fragment storage
    private final List<Fragment> fragments = new CopyOnWriteArrayList<>();
    private final Map<String, Fragment> fragmentsByTag = new HashMap<>();
    private final Map<Integer, Fragment> fragmentsById = new HashMap<>();
    private final Map<Integer, FragmentContainerView> containers = new HashMap<>();
    
    // Backstack management
    private final List<BackStackEntry> backStack = new ArrayList<>();
    private final List<OnBackStackChangedListener> backStackListeners = new ArrayList<>();
    
    // Transaction management
    private boolean executingTransactions = false;
    private final List<FragmentTransaction> pendingTransactions = new ArrayList<>();
    
    // State management
    private boolean destroyed = false;
    private Fragment.State currentState = Fragment.State.CREATED;
    
    /**
     * Backstack entry
     */
    public static class BackStackEntry {
        private final String name;
        private final int id;
        private final List<FragmentTransaction.Op> operations;
        
        public BackStackEntry(String name, int id, List<FragmentTransaction.Op> operations) {
            this.name = name;
            this.id = id;
            this.operations = new ArrayList<>(operations);
        }
        
        public String getName() {
            return name;
        }
        
        public int getId() {
            return id;
        }
        
        public List<FragmentTransaction.Op> getOperations() {
            return new ArrayList<>(operations);
        }
        
        @Override
        public String toString() {
            return String.format("BackStackEntry[name=%s, id=%d, ops=%d]", name, id, operations.size());
        }
    }
    
    /**
     * Backstack change listener
     */
    public interface OnBackStackChangedListener {
        void onBackStackChanged();
    }
    
    /**
     * Begin a new transaction
     */
    public FragmentTransaction beginTransaction() {
        if (destroyed) {
            throw new IllegalStateException("FragmentManager has been destroyed");
        }
        return new FragmentTransaction(this);
    }
    
    /**
     * Execute a transaction
     */
    void executeTransaction(FragmentTransaction transaction) {
        if (destroyed) {
            throw new IllegalStateException("FragmentManager has been destroyed");
        }
        
        if (executingTransactions) {
            pendingTransactions.add(transaction);
            return;
        }
        
        executingTransactions = true;
        
        try {
            // Execute all operations in the transaction
            for (FragmentTransaction.Op op : transaction.getOperations()) {
                executeOperation(op);
            }
            
            // Add to backstack if requested
            if (transaction.isAddToBackStack()) {
                BackStackEntry entry = new BackStackEntry(
                    transaction.getBackStackName(),
                    backStack.size(),
                    transaction.getOperations()
                );
                backStack.add(entry);
                notifyBackStackChanged();
            }
            
            // Execute pending transactions
            while (!pendingTransactions.isEmpty()) {
                FragmentTransaction pending = pendingTransactions.remove(0);
                executeTransaction(pending);
            }
            
        } finally {
            executingTransactions = false;
        }
    }
    
    /**
     * Execute a single operation
     */
    private void executeOperation(FragmentTransaction.Op op) {
        switch (op.getType()) {
            case ADD:
                executeAdd(op);
                break;
            case REMOVE:
                executeRemove(op);
                break;
            case REPLACE:
                executeReplace(op);
                break;
            case HIDE:
                executeHide(op);
                break;
            case SHOW:
                executeShow(op);
                break;
            case ATTACH:
                executeAttach(op);
                break;
            case DETACH:
                executeDetach(op);
                break;
        }
    }
    
    private void executeAdd(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        int containerId = op.getContainerId();
        String tag = op.getTag();
        
        // Set fragment properties
        fragment.setFragmentManager(this);
        fragment.setId(containerId);
        fragment.setTag(tag);
        fragment.setAdded(true);
        
        // Add to collections
        fragments.add(fragment);
        if (tag != null) {
            fragmentsByTag.put(tag, fragment);
        }
        fragmentsById.put(containerId, fragment);
        
        // Get container
        FragmentContainerView container = containers.get(containerId);
        if (container != null) {
            container.addFragmentToStack(fragment);
            
            // Create view if needed
            if (fragment.getView() == null) {
                fragment.performCreateView(container, fragment.getArguments());
            }
            
            // Set as current fragment
            container.setFragment(fragment);
        }
        
        // Perform lifecycle
        moveFragmentToState(fragment, currentState);
        
        System.out.println("Fragment added: " + fragment);
    }
    
    private void executeRemove(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        
        // Remove from collections
        fragments.remove(fragment);
        if (fragment.getTag() != null) {
            fragmentsByTag.remove(fragment.getTag());
        }
        fragmentsById.remove(fragment.getId());
        
        // Remove from container
        FragmentContainerView container = containers.get(fragment.getId());
        if (container != null) {
            container.removeFragmentFromStack(fragment);
            if (container.getCurrentFragment() == fragment) {
                container.setFragment(null);
            }
        }
        
        // Perform lifecycle
        fragment.setRemoving(true);
        moveFragmentToState(fragment, Fragment.State.DESTROYED);
        fragment.performDetach();
        
        System.out.println("Fragment removed: " + fragment);
    }
    
    private void executeReplace(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        int containerId = op.getContainerId();
        
        // Remove existing fragment in container
        Fragment existing = fragmentsById.get(containerId);
        if (existing != null) {
            FragmentTransaction.Op removeOp = new FragmentTransaction.Op(
                FragmentTransaction.Op.Type.REMOVE, existing, containerId, null
            );
            executeRemove(removeOp);
        }
        
        // Add new fragment
        executeAdd(op);
    }
    
    private void executeHide(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        fragment.setHidden(true);
        
        FragmentContainerView container = containers.get(fragment.getId());
        if (container != null && container.getCurrentFragment() == fragment) {
            container.setFragment(null);
        }
        
        System.out.println("Fragment hidden: " + fragment);
    }
    
    private void executeShow(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        fragment.setHidden(false);
        
        FragmentContainerView container = containers.get(fragment.getId());
        if (container != null) {
            container.setFragment(fragment);
        }
        
        System.out.println("Fragment shown: " + fragment);
    }
    
    private void executeAttach(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        fragment.setDetached(false);
        
        // Create view if needed
        FragmentContainerView container = containers.get(fragment.getId());
        if (container != null && fragment.getView() == null) {
            fragment.performCreateView(container, fragment.getArguments());
        }
        
        moveFragmentToState(fragment, currentState);
        
        System.out.println("Fragment attached: " + fragment);
    }
    
    private void executeDetach(FragmentTransaction.Op op) {
        Fragment fragment = op.getFragment();
        fragment.setDetached(true);
        
        // Destroy view
        fragment.performDestroyView();
        
        System.out.println("Fragment detached: " + fragment);
    }
    
    /**
     * Move fragment to target state
     */
    private void moveFragmentToState(Fragment fragment, Fragment.State targetState) {
        Fragment.State currentFragmentState = fragment.getState();
        
        if (currentFragmentState == targetState) {
            return;
        }
        
        // Move up in lifecycle
        while (currentFragmentState.ordinal() < targetState.ordinal()) {
            switch (currentFragmentState) {
                case CREATED:
                    if (fragment.getView() == null) {
                        FragmentContainerView container = containers.get(fragment.getId());
                        if (container != null) {
                            fragment.performCreateView(container, fragment.getArguments());
                        }
                    }
                    fragment.performStart();
                    break;
                case STARTED:
                    fragment.performResume();
                    break;
            }
            currentFragmentState = Fragment.State.values()[currentFragmentState.ordinal() + 1];
        }
        
        // Move down in lifecycle
        while (currentFragmentState.ordinal() > targetState.ordinal()) {
            switch (currentFragmentState) {
                case RESUMED:
                    fragment.performPause();
                    break;
                case STARTED:
                    fragment.performStop();
                    break;
                case STOPPED:
                    fragment.performDestroyView();
                    break;
                case CREATED:
                    fragment.performDestroy();
                    break;
            }
            currentFragmentState = Fragment.State.values()[currentFragmentState.ordinal() - 1];
        }
    }
    
    /**
     * Register a container
     */
    public void registerContainer(FragmentContainerView container) {
        containers.put(container.getContainerId(), container);
        System.out.println("Container registered: " + container.getContainerId());
    }
    
    /**
     * Unregister a container
     */
    public void unregisterContainer(int containerId) {
        containers.remove(containerId);
        System.out.println("Container unregistered: " + containerId);
    }
    
    /**
     * Find fragment by tag
     */
    public Fragment findFragmentByTag(String tag) {
        return fragmentsByTag.get(tag);
    }
    
    /**
     * Find fragment by ID
     */
    public Fragment findFragmentById(int id) {
        return fragmentsById.get(id);
    }
    
    /**
     * Get all fragments
     */
    public List<Fragment> getFragments() {
        return new ArrayList<>(fragments);
    }
    
    /**
     * Pop backstack
     */
    public boolean popBackStack() {
        if (backStack.isEmpty()) {
            return false;
        }
        
        BackStackEntry entry = backStack.remove(backStack.size() - 1);
        
        // Reverse the operations
        List<FragmentTransaction.Op> operations = entry.getOperations();
        for (int i = operations.size() - 1; i >= 0; i--) {
            FragmentTransaction.Op op = operations.get(i);
            executeReverseOperation(op);
        }
        
        notifyBackStackChanged();
        return true;
    }
    
    /**
     * Pop backstack to specific name
     */
    public boolean popBackStack(String name, boolean inclusive) {
        int index = -1;
        for (int i = backStack.size() - 1; i >= 0; i--) {
            if (name.equals(backStack.get(i).getName())) {
                index = i;
                break;
            }
        }
        
        if (index == -1) {
            return false;
        }
        
        if (!inclusive) {
            index++;
        }
        
        // Pop entries
        while (backStack.size() > index) {
            popBackStack();
        }
        
        return true;
    }
    
    /**
     * Execute reverse operation for backstack
     */
    private void executeReverseOperation(FragmentTransaction.Op op) {
        switch (op.getType()) {
            case ADD:
                executeRemove(op);
                break;
            case REMOVE:
                executeAdd(op);
                break;
            case HIDE:
                executeShow(op);
                break;
            case SHOW:
                executeHide(op);
                break;
            case ATTACH:
                executeDetach(op);
                break;
            case DETACH:
                executeAttach(op);
                break;
            case REPLACE:
                // Replace is more complex to reverse
                // For now, just remove the fragment
                executeRemove(op);
                break;
        }
    }
    
    /**
     * Get backstack entry count
     */
    public int getBackStackEntryCount() {
        return backStack.size();
    }
    
    /**
     * Get backstack entry at index
     */
    public BackStackEntry getBackStackEntryAt(int index) {
        return backStack.get(index);
    }
    
    /**
     * Add backstack changed listener
     */
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (!backStackListeners.contains(listener)) {
            backStackListeners.add(listener);
        }
    }
    
    /**
     * Remove backstack changed listener
     */
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        backStackListeners.remove(listener);
    }
    
    /**
     * Notify backstack changed
     */
    private void notifyBackStackChanged() {
        for (OnBackStackChangedListener listener : backStackListeners) {
            try {
                listener.onBackStackChanged();
            } catch (Exception e) {
                System.err.println("Error in backstack listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set fragment manager state
     */
    public void setState(Fragment.State state) {
        this.currentState = state;
        
        // Move all fragments to new state
        for (Fragment fragment : fragments) {
            if (fragment.isAdded() && !fragment.isDetached()) {
                moveFragmentToState(fragment, state);
            }
        }
    }
    
    /**
     * Destroy the fragment manager
     */
    public void destroy() {
        if (destroyed) {
            return;
        }
        
        // Remove all fragments
        for (Fragment fragment : new ArrayList<>(fragments)) {
            FragmentTransaction.Op removeOp = new FragmentTransaction.Op(
                FragmentTransaction.Op.Type.REMOVE, fragment, fragment.getId(), null
            );
            executeRemove(removeOp);
        }
        
        // Clear collections
        fragments.clear();
        fragmentsByTag.clear();
        fragmentsById.clear();
        containers.clear();
        backStack.clear();
        backStackListeners.clear();
        pendingTransactions.clear();
        
        destroyed = true;
        System.out.println("FragmentManager destroyed");
    }
    
    /**
     * Check if destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }
    
    @Override
    public String toString() {
        return String.format("FragmentManager[fragments=%d, backstack=%d, state=%s]", 
                fragments.size(), backStack.size(), currentState);
    }
}