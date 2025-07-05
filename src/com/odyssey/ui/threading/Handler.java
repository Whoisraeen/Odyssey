package com.odyssey.ui.threading;

/**
 * Handler class for sending and processing messages and runnables
 * Inspired by Android's Handler system
 */
public class Handler {
    private final MessageQueue mQueue;
    private final Looper mLooper;
    private final Callback mCallback;
    
    /**
     * Callback interface for handling messages
     */
    public interface Callback {
        /**
         * Handle a message
         * @param msg The message to handle
         * @return true if the message was handled, false otherwise
         */
        boolean handleMessage(Message msg);
    }
    
    /**
     * Default constructor - uses the current thread's Looper
     */
    public Handler() {
        this(null, false);
    }
    
    /**
     * Constructor with callback
     */
    public Handler(Callback callback) {
        this(callback, false);
    }
    
    /**
     * Constructor with callback and async flag
     */
    public Handler(Callback callback, boolean async) {
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread " + Thread.currentThread()
                        + " that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
    }
    
    /**
     * Constructor with specific Looper
     */
    public Handler(Looper looper) {
        this(looper, null, false);
    }
    
    /**
     * Constructor with specific Looper and callback
     */
    public Handler(Looper looper, Callback callback) {
        this(looper, callback, false);
    }
    
    /**
     * Constructor with specific Looper, callback, and async flag
     */
    public Handler(Looper looper, Callback callback, boolean async) {
        mLooper = looper;
        mQueue = looper.mQueue;
        mCallback = callback;
    }
    
    /**
     * Handle a message - override this method to handle messages
     */
    public void handleMessage(Message msg) {
        // Default implementation does nothing
    }
    
    /**
     * Dispatch a message
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
    
    /**
     * Handle callback message
     */
    private static void handleCallback(Message message) {
        message.callback.run();
    }
    
    /**
     * Get the Looper associated with this handler
     */
    public final Looper getLooper() {
        return mLooper;
    }
    
    /**
     * Get the MessageQueue associated with this handler
     */
    public final MessageQueue getMessageQueue() {
        return mQueue;
    }
    
    /**
     * Post a runnable to be executed
     */
    public final boolean post(Runnable r) {
        return sendMessageDelayed(getPostMessage(r), 0);
    }
    
    /**
     * Post a runnable to be executed at a specific time
     */
    public final boolean postAtTime(Runnable r, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r), uptimeMillis);
    }
    
    /**
     * Post a runnable to be executed at a specific time with a token
     */
    public final boolean postAtTime(Runnable r, Object token, long uptimeMillis) {
        return sendMessageAtTime(getPostMessage(r, token), uptimeMillis);
    }
    
    /**
     * Post a runnable to be executed after a delay
     */
    public final boolean postDelayed(Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r), delayMillis);
    }
    
    /**
     * Post a runnable to be executed after a delay with a token
     */
    public final boolean postDelayed(Runnable r, Object token, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, token), delayMillis);
    }
    
    /**
     * Remove callbacks
     */
    public final void removeCallbacks(Runnable r) {
        mQueue.removeCallbacks(this, r);
    }
    
    /**
     * Remove callbacks with token
     */
    public final void removeCallbacks(Runnable r, Object token) {
        mQueue.removeCallbacks(this, r);
    }
    
    /**
     * Send an empty message
     */
    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what, 0);
    }
    
    /**
     * Send an empty message with delay
     */
    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageDelayed(msg, delayMillis);
    }
    
    /**
     * Send an empty message at a specific time
     */
    public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageAtTime(msg, uptimeMillis);
    }
    
    /**
     * Send a message
     */
    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg, 0);
    }
    
    /**
     * Send a message with delay
     */
    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, System.currentTimeMillis() + delayMillis);
    }
    
    /**
     * Send a message at a specific time
     */
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
    
    /**
     * Send a message to the front of the queue
     */
    public final boolean sendMessageAtFrontOfQueue(Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return enqueueMessage(queue, msg, 0);
    }
    
    /**
     * Enqueue a message
     */
    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        msg.setTarget(this);
        return queue.enqueueMessage(msg, uptimeMillis);
    }
    
    /**
     * Remove all messages with the specified what value
     */
    public final void removeMessages(int what) {
        mQueue.removeMessages(this, what);
    }
    
    /**
     * Remove all messages with the specified what value and object
     */
    public final void removeMessages(int what, Object object) {
        mQueue.removeMessages(this, what);
    }
    
    /**
     * Remove all callbacks and messages
     */
    public final void removeCallbacksAndMessages(Object token) {
        mQueue.removeMessages(this);
    }
    
    /**
     * Check if there are any pending posts with the specified runnable
     */
    public final boolean hasMessages(int what) {
        return hasMessages(what, null);
    }
    
    /**
     * Check if there are any pending posts with the specified runnable and object
     */
    public final boolean hasMessages(int what, Object object) {
        // For simplicity, we'll return false here
        // In a full implementation, this would check the message queue
        return false;
    }
    
    /**
     * Check if there are any pending posts with the specified runnable
     */
    public final boolean hasCallbacks(Runnable r) {
        // For simplicity, we'll return false here
        // In a full implementation, this would check the message queue
        return false;
    }
    
    /**
     * Get a message for posting a runnable
     */
    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
    
    /**
     * Get a message for posting a runnable with a token
     */
    private static Message getPostMessage(Runnable r, Object token) {
        Message m = Message.obtain();
        m.obj = token;
        m.callback = r;
        return m;
    }
    
    /**
     * Dump the handler state
     */
    public final void dump(String prefix) {
        System.out.println(prefix + this + " @ " + System.currentTimeMillis());
        if (mLooper != null) {
            mLooper.dump(prefix + "  ");
        } else {
            System.out.println(prefix + "looper uninitialized");
        }
    }
    
    @Override
    public String toString() {
        return "Handler (" + getClass().getName() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}