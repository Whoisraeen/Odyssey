package com.odyssey.ui.threading;

/**
 * Message class for thread communication in the UI system
 * Inspired by Android's Message system
 */
public class Message {
    public static final int WHAT_LAYOUT = 1;
    public static final int WHAT_RENDER = 2;
    public static final int WHAT_INPUT = 3;
    public static final int WHAT_LIFECYCLE = 4;
    
    public int what;
    public int arg1;
    public int arg2;
    public Object obj;
    public Runnable callback;
    
    private Handler target;
    private long when;
    private boolean inUse;
    
    // Message pool for recycling
    private static final Object sPoolSync = new Object();
    private static Message sPool;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 50;
    
    private Message next;
    
    public Message() {
    }
    
    /**
     * Obtain a Message from the global pool
     */
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.inUse = false;
                sPoolSize--;
                return m;
            }
        }
        return new Message();
    }
    
    /**
     * Obtain a Message with specified what value
     */
    public static Message obtain(int what) {
        Message m = obtain();
        m.what = what;
        return m;
    }
    
    /**
     * Obtain a Message with Handler target
     */
    public static Message obtain(Handler h) {
        Message m = obtain();
        m.target = h;
        return m;
    }
    
    /**
     * Obtain a Message with Handler target and what value
     */
    public static Message obtain(Handler h, int what) {
        Message m = obtain(h);
        m.what = what;
        return m;
    }
    
    /**
     * Obtain a Message with Handler target, what value, and object
     */
    public static Message obtain(Handler h, int what, Object obj) {
        Message m = obtain(h, what);
        m.obj = obj;
        return m;
    }
    
    /**
     * Obtain a Message with Handler target, what value, and args
     */
    public static Message obtain(Handler h, int what, int arg1, int arg2) {
        Message m = obtain(h, what);
        m.arg1 = arg1;
        m.arg2 = arg2;
        return m;
    }
    
    /**
     * Obtain a Message with Handler target, what value, args, and object
     */
    public static Message obtain(Handler h, int what, int arg1, int arg2, Object obj) {
        Message m = obtain(h, what, arg1, arg2);
        m.obj = obj;
        return m;
    }
    
    /**
     * Obtain a Message with callback
     */
    public static Message obtain(Handler h, Runnable callback) {
        Message m = obtain(h);
        m.callback = callback;
        return m;
    }
    
    /**
     * Recycle this message back to the global pool
     */
    public void recycle() {
        if (inUse) {
            throw new IllegalStateException("This message is still in use.");
        }
        recycleUnchecked();
    }
    
    void recycleUnchecked() {
        // Clear all fields
        inUse = false;
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        callback = null;
        target = null;
        when = 0;
        
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }
    
    /**
     * Send this message to its target Handler
     */
    public void sendToTarget() {
        if (target == null) {
            throw new IllegalStateException("Message has no target Handler");
        }
        target.sendMessage(this);
    }
    
    /**
     * Set when this message should be delivered
     */
    public void setWhen(long when) {
        this.when = when;
    }
    
    /**
     * Get when this message should be delivered
     */
    public long getWhen() {
        return when;
    }
    
    /**
     * Set the target Handler
     */
    public void setTarget(Handler target) {
        this.target = target;
    }
    
    /**
     * Get the target Handler
     */
    public Handler getTarget() {
        return target;
    }
    
    /**
     * Mark this message as in use
     */
    void markInUse() {
        inUse = true;
    }
    
    /**
     * Check if this message is in use
     */
    boolean isInUse() {
        return inUse;
    }
    
    /**
     * Get the next message in the queue
     */
    Message getNext() {
        return next;
    }
    
    /**
     * Set the next message in the queue
     */
    void setNext(Message next) {
        this.next = next;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "what=" + what +
                ", arg1=" + arg1 +
                ", arg2=" + arg2 +
                ", obj=" + obj +
                ", when=" + when +
                "}";
    }
}