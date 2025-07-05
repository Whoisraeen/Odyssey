package com.odyssey.ui.fragment;

import java.util.*;

/**
 * FragmentTransaction for managing fragment operations
 * Inspired by Android's FragmentTransaction
 */
public class FragmentTransaction {
    private static final String TAG = "FragmentTransaction";
    
    private final FragmentManager fragmentManager;
    private final List<Op> operations = new ArrayList<>();
    private boolean addToBackStack = false;
    private String backStackName = null;
    private boolean committed = false;
    
    /**
     * Fragment operation
     */
    public static class Op {
        public enum Type {
            ADD, REMOVE, REPLACE, HIDE, SHOW, ATTACH, DETACH
        }
        
        private final Type type;
        private final Fragment fragment;
        private final int containerId;
        private final String tag;
        
        public Op(Type type, Fragment fragment, int containerId, String tag) {
            this.type = type;
            this.fragment = fragment;
            this.containerId = containerId;
            this.tag = tag;
        }
        
        public Type getType() {
            return type;
        }
        
        public Fragment getFragment() {
            return fragment;
        }
        
        public int getContainerId() {
            return containerId;
        }
        
        public String getTag() {
            return tag;
        }
        
        @Override
        public String toString() {
            return String.format("Op[%s, fragment=%s, container=%d, tag=%s]", 
                    type, fragment != null ? fragment.getClass().getSimpleName() : "null", 
                    containerId, tag);
        }
    }
    
    /**
     * Constructor
     */
    FragmentTransaction(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }
    
    /**
     * Add a fragment to the activity state
     */
    public FragmentTransaction add(int containerViewId, Fragment fragment) {
        return add(containerViewId, fragment, null);
    }
    
    /**
     * Add a fragment to the activity state with a tag
     */
    public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.ADD, fragment, containerViewId, tag));
        return this;
    }
    
    /**
     * Remove an existing fragment
     */
    public FragmentTransaction remove(Fragment fragment) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.REMOVE, fragment, fragment.getId(), fragment.getTag()));
        return this;
    }
    
    /**
     * Replace an existing fragment
     */
    public FragmentTransaction replace(int containerViewId, Fragment fragment) {
        return replace(containerViewId, fragment, null);
    }
    
    /**
     * Replace an existing fragment with a tag
     */
    public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.REPLACE, fragment, containerViewId, tag));
        return this;
    }
    
    /**
     * Hide an existing fragment
     */
    public FragmentTransaction hide(Fragment fragment) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.HIDE, fragment, fragment.getId(), fragment.getTag()));
        return this;
    }
    
    /**
     * Show a previously hidden fragment
     */
    public FragmentTransaction show(Fragment fragment) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.SHOW, fragment, fragment.getId(), fragment.getTag()));
        return this;
    }
    
    /**
     * Attach a fragment that was previously detached
     */
    public FragmentTransaction attach(Fragment fragment) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.ATTACH, fragment, fragment.getId(), fragment.getTag()));
        return this;
    }
    
    /**
     * Detach the given fragment from the UI
     */
    public FragmentTransaction detach(Fragment fragment) {
        checkNotCommitted();
        operations.add(new Op(Op.Type.DETACH, fragment, fragment.getId(), fragment.getTag()));
        return this;
    }
    
    /**
     * Add this transaction to the back stack
     */
    public FragmentTransaction addToBackStack(String name) {
        checkNotCommitted();
        addToBackStack = true;
        backStackName = name;
        return this;
    }
    
    /**
     * Set custom animations for this transaction
     */
    public FragmentTransaction setCustomAnimations(int enter, int exit) {
        return setCustomAnimations(enter, exit, 0, 0);
    }
    
    /**
     * Set custom animations for this transaction with pop animations
     */
    public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
        checkNotCommitted();
        // TODO: Implement animation support
        System.out.println("Custom animations set: enter=" + enter + ", exit=" + exit + 
                          ", popEnter=" + popEnter + ", popExit=" + popExit);
        return this;
    }
    
    /**
     * Set transition style for this transaction
     */
    public FragmentTransaction setTransition(int transit) {
        checkNotCommitted();
        // TODO: Implement transition support
        System.out.println("Transition set: " + transit);
        return this;
    }
    
    /**
     * Set transition style resource for this transaction
     */
    public FragmentTransaction setTransitionStyle(int styleRes) {
        checkNotCommitted();
        // TODO: Implement transition style support
        System.out.println("Transition style set: " + styleRes);
        return this;
    }
    
    /**
     * Commit the transaction
     */
    public int commit() {
        checkNotCommitted();
        committed = true;
        
        if (operations.isEmpty()) {
            System.out.println("Empty transaction committed");
            return -1;
        }
        
        System.out.println("Committing transaction with " + operations.size() + " operations");
        for (Op op : operations) {
            System.out.println("  " + op);
        }
        
        fragmentManager.executeTransaction(this);
        return hashCode(); // Use hashcode as transaction ID
    }
    
    /**
     * Commit the transaction allowing state loss
     */
    public int commitAllowingStateLoss() {
        // For now, same as commit()
        return commit();
    }
    
    /**
     * Commit the transaction now (synchronously)
     */
    public void commitNow() {
        commit();
        // In a real implementation, this would execute immediately
        // rather than being queued
    }
    
    /**
     * Commit the transaction now allowing state loss
     */
    public void commitNowAllowingStateLoss() {
        commitNow();
    }
    
    /**
     * Check if the transaction has been committed
     */
    public boolean isCommitted() {
        return committed;
    }
    
    /**
     * Check if empty
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }
    
    /**
     * Get operations (package-private for FragmentManager)
     */
    List<Op> getOperations() {
        return new ArrayList<>(operations);
    }
    
    /**
     * Check if should add to backstack (package-private for FragmentManager)
     */
    boolean isAddToBackStack() {
        return addToBackStack;
    }
    
    /**
     * Get backstack name (package-private for FragmentManager)
     */
    String getBackStackName() {
        return backStackName;
    }
    
    /**
     * Check if not committed
     */
    private void checkNotCommitted() {
        if (committed) {
            throw new IllegalStateException("Transaction has already been committed");
        }
    }
    
    /**
     * Disallow adding to backstack
     */
    public FragmentTransaction disallowAddToBackStack() {
        checkNotCommitted();
        if (addToBackStack) {
            throw new IllegalStateException("This transaction is already being added to the back stack");
        }
        // Set a flag to disallow future addToBackStack calls
        // For simplicity, we'll just log this
        System.out.println("Backstack addition disallowed for this transaction");
        return this;
    }
    
    /**
     * Set reordering allowed
     */
    public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) {
        checkNotCommitted();
        // TODO: Implement reordering support
        System.out.println("Reordering allowed: " + reorderingAllowed);
        return this;
    }
    
    /**
     * Run on commit
     */
    public FragmentTransaction runOnCommit(Runnable runnable) {
        checkNotCommitted();
        // TODO: Implement runOnCommit support
        System.out.println("RunOnCommit added: " + runnable);
        return this;
    }
    
    /**
     * Set bread crumb title
     */
    public FragmentTransaction setBreadCrumbTitle(String title) {
        checkNotCommitted();
        // TODO: Implement breadcrumb support
        System.out.println("Breadcrumb title set: " + title);
        return this;
    }
    
    /**
     * Set bread crumb title resource
     */
    public FragmentTransaction setBreadCrumbTitle(int res) {
        checkNotCommitted();
        // TODO: Implement breadcrumb support
        System.out.println("Breadcrumb title resource set: " + res);
        return this;
    }
    
    /**
     * Set bread crumb short title
     */
    public FragmentTransaction setBreadCrumbShortTitle(String title) {
        checkNotCommitted();
        // TODO: Implement breadcrumb support
        System.out.println("Breadcrumb short title set: " + title);
        return this;
    }
    
    /**
     * Set bread crumb short title resource
     */
    public FragmentTransaction setBreadCrumbShortTitle(int res) {
        checkNotCommitted();
        // TODO: Implement breadcrumb support
        System.out.println("Breadcrumb short title resource set: " + res);
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FragmentTransaction{");
        sb.append("operations=").append(operations.size());
        sb.append(", addToBackStack=").append(addToBackStack);
        sb.append(", backStackName='").append(backStackName).append("'");
        sb.append(", committed=").append(committed);
        sb.append('}');
        return sb.toString();
    }
}