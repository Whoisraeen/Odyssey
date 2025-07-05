package com.odyssey.ui.lifecycle;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lifecycle class for managing component lifecycle states
 * Inspired by Android's Lifecycle Architecture Component
 */
public abstract class Lifecycle {
    
    /**
     * Lifecycle states
     */
    public enum State {
        DESTROYED,
        INITIALIZED,
        CREATED,
        STARTED,
        RESUMED;
        
        /**
         * Check if this state is at least the given state
         */
        public boolean isAtLeast(State state) {
            return compareTo(state) >= 0;
        }
    }
    
    /**
     * Lifecycle events
     */
    public enum Event {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY,
        ON_ANY;
        
        /**
         * Get target state after this event
         */
        public State getTargetState() {
            switch (this) {
                case ON_CREATE:
                case ON_STOP:
                    return State.CREATED;
                case ON_START:
                case ON_PAUSE:
                    return State.STARTED;
                case ON_RESUME:
                    return State.RESUMED;
                case ON_DESTROY:
                    return State.DESTROYED;
                default:
                    throw new IllegalArgumentException(this + " has no target state");
            }
        }
        
        /**
         * Get event that moves down from the given state
         */
        public static Event downFrom(State state) {
            switch (state) {
                case CREATED:
                    return ON_DESTROY;
                case STARTED:
                    return ON_STOP;
                case RESUMED:
                    return ON_PAUSE;
                default:
                    return null;
            }
        }
        
        /**
         * Get event that moves up to the given state
         */
        public static Event upTo(State state) {
            switch (state) {
                case CREATED:
                    return ON_CREATE;
                case STARTED:
                    return ON_START;
                case RESUMED:
                    return ON_RESUME;
                default:
                    return null;
            }
        }
    }
    
    /**
     * Add a lifecycle observer
     */
    public abstract void addObserver(LifecycleObserver observer);
    
    /**
     * Remove a lifecycle observer
     */
    public abstract void removeObserver(LifecycleObserver observer);
    
    /**
     * Get current state
     */
    public abstract State getCurrentState();
    
    /**
     * Default implementation of Lifecycle
     */
    public static class LifecycleRegistry extends Lifecycle {
        private final WeakHashMap<LifecycleObserver, ObserverWithState> observerMap = new WeakHashMap<>();
        private State currentState = State.INITIALIZED;
        private final LifecycleOwner lifecycleOwner;
        private boolean addingObserverCounter = false;
        private boolean handlingEvent = false;
        private boolean newEventOccurred = false;
        
        /**
         * Observer with state tracking
         */
        private static class ObserverWithState {
            State state;
            LifecycleEventObserver lifecycleObserver;
            
            ObserverWithState(LifecycleObserver observer, State initialState) {
                lifecycleObserver = Lifecycling.lifecycleEventObserver(observer);
                state = initialState;
            }
            
            void dispatchEvent(LifecycleOwner owner, Event event) {
                State newState = event.getTargetState();
                state = min(state, newState);
                lifecycleObserver.onStateChanged(owner, event);
                state = newState;
            }
        }
        
        /**
         * Constructor
         */
        public LifecycleRegistry(LifecycleOwner provider) {
            lifecycleOwner = new WeakReference<>(provider).get();
        }
        
        /**
         * Mark state and notify observers
         */
        public void markState(State state) {
            setCurrentState(state);
        }
        
        /**
         * Handle lifecycle event
         */
        public void handleLifecycleEvent(Event event) {
            State next = event.getTargetState();
            setCurrentState(next);
        }
        
        private void setCurrentState(State state) {
            if (currentState == state) {
                return;
            }
            currentState = state;
            if (handlingEvent || addingObserverCounter) {
                newEventOccurred = true;
                return;
            }
            handlingEvent = true;
            sync();
            handlingEvent = false;
        }
        
        private boolean isSynced() {
            if (observerMap.size() == 0) {
                return true;
            }
            State eldestObserverState = observerMap.values().iterator().next().state;
            State newestObserverState = eldestObserverState;
            for (ObserverWithState observer : observerMap.values()) {
                if (observer.state.compareTo(eldestObserverState) < 0) {
                    eldestObserverState = observer.state;
                }
                if (observer.state.compareTo(newestObserverState) > 0) {
                    newestObserverState = observer.state;
                }
            }
            return newestObserverState == eldestObserverState && currentState == newestObserverState;
        }
        
        private void forwardPass(LifecycleOwner lifecycleOwner) {
            Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> ascendingIterator =
                    observerMap.entrySet().iterator();
            while (ascendingIterator.hasNext() && !newEventOccurred) {
                Map.Entry<LifecycleObserver, ObserverWithState> entry = ascendingIterator.next();
                ObserverWithState observer = entry.getValue();
                while ((observer.state.compareTo(currentState) < 0 && !newEventOccurred
                        && observerMap.containsKey(entry.getKey()))) {
                    pushParentState(observer.state);
                    Event event = Event.upTo(observer.state);
                    if (event == null) {
                        throw new IllegalStateException("no event up from " + observer.state);
                    }
                    observer.dispatchEvent(lifecycleOwner, event);
                    popParentState();
                }
            }
        }
        
        private void backwardPass(LifecycleOwner lifecycleOwner) {
            List<Map.Entry<LifecycleObserver, ObserverWithState>> entries = 
                    new ArrayList<>(observerMap.entrySet());
            for (int i = entries.size() - 1; i >= 0 && !newEventOccurred; i--) {
                Map.Entry<LifecycleObserver, ObserverWithState> entry = entries.get(i);
                ObserverWithState observer = entry.getValue();
                while ((observer.state.compareTo(currentState) > 0 && !newEventOccurred
                        && observerMap.containsKey(entry.getKey()))) {
                    Event event = Event.downFrom(observer.state);
                    if (event == null) {
                        throw new IllegalStateException("no event down from " + observer.state);
                    }
                    pushParentState(event.getTargetState());
                    observer.dispatchEvent(lifecycleOwner, event);
                    popParentState();
                }
            }
        }
        
        private void sync() {
            LifecycleOwner lifecycleOwner = this.lifecycleOwner;
            if (lifecycleOwner == null) {
                throw new IllegalStateException("LifecycleOwner of this LifecycleRegistry is"
                        + "already garbage collected. It is too late to change lifecycle state.");
            }
            while (!isSynced()) {
                newEventOccurred = false;
                if (currentState.compareTo(observerMap.values().iterator().next().state) < 0) {
                    backwardPass(lifecycleOwner);
                }
                Map.Entry<LifecycleObserver, ObserverWithState> newest = newestEntry();
                if (!newEventOccurred && newest != null
                        && currentState.compareTo(newest.getValue().state) > 0) {
                    forwardPass(lifecycleOwner);
                }
            }
            newEventOccurred = false;
        }
        
        private Map.Entry<LifecycleObserver, ObserverWithState> newestEntry() {
            Map.Entry<LifecycleObserver, ObserverWithState> newest = null;
            for (Map.Entry<LifecycleObserver, ObserverWithState> entry : observerMap.entrySet()) {
                if (newest == null || newest.getValue().state.compareTo(entry.getValue().state) < 0) {
                    newest = entry;
                }
            }
            return newest;
        }
        
        private static State min(State state1, State state2) {
            return state2 != null && state2.compareTo(state1) < 0 ? state2 : state1;
        }
        
        private void pushParentState(State state) {
            // Implementation for parent state tracking if needed
        }
        
        private void popParentState() {
            // Implementation for parent state tracking if needed
        }
        
        @Override
        public void addObserver(LifecycleObserver observer) {
            State initialState = currentState == State.DESTROYED ? State.DESTROYED : State.INITIALIZED;
            ObserverWithState statefulObserver = new ObserverWithState(observer, initialState);
            ObserverWithState previous = observerMap.get(observer);
            if (previous == null) {
                observerMap.put(observer, statefulObserver);
            }
            
            if (previous != null) {
                return;
            }
            LifecycleOwner lifecycleOwner = this.lifecycleOwner;
            if (lifecycleOwner == null) {
                return;
            }
            
            boolean isReentrant = addingObserverCounter || handlingEvent;
            State targetState = calculateTargetState(observer);
            addingObserverCounter = true;
            while ((statefulObserver.state.compareTo(targetState) < 0
                    && observerMap.containsKey(observer))) {
                pushParentState(statefulObserver.state);
                Event event = Event.upTo(statefulObserver.state);
                if (event == null) {
                    throw new IllegalStateException("no event up from " + statefulObserver.state);
                }
                statefulObserver.dispatchEvent(lifecycleOwner, event);
                popParentState();
                targetState = calculateTargetState(observer);
            }
            
            if (!isReentrant) {
                sync();
            }
            addingObserverCounter = false;
        }
        
        @Override
        public void removeObserver(LifecycleObserver observer) {
            observerMap.remove(observer);
        }
        
        @Override
        public State getCurrentState() {
            return currentState;
        }
        
        private State calculateTargetState(LifecycleObserver observer) {
            State siblingState = null;
            // Find the previous observer's state
            boolean foundCurrent = false;
            for (Map.Entry<LifecycleObserver, ObserverWithState> entry : observerMap.entrySet()) {
                if (entry.getKey() == observer) {
                    foundCurrent = true;
                    break;
                }
                siblingState = entry.getValue().state;
            }
            State parentState = null; // Would be from parent if this was a child lifecycle
            return min(min(currentState, siblingState), parentState);
        }
        
        /**
         * Get observer count
         */
        public int getObserverCount() {
            return observerMap.size();
        }
    }
    
    /**
     * Weak reference to lifecycle owner
     */
    private static class WeakReference<T> {
        private final T reference;
        
        WeakReference(T reference) {
            this.reference = reference;
        }
        
        T get() {
            return reference;
        }
    }
}