package com.odyssey.ui.threading;

/**
 * Looper class for running a message loop in a thread
 * Inspired by Android's Looper system
 */
public final class Looper {
    private static final String TAG = "Looper";
    
    // Thread-local storage for the Looper
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    private static Looper sMainLooper;  // guarded by Looper.class
    
    final MessageQueue mQueue;
    final Thread mThread;
    
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
    
    /**
     * Initialize the current thread as a looper.
     * This gives you a chance to create handlers that then reference
     * this looper, before actually starting the loop. Be sure to call
     * {@link #loop()} after calling this method, and end it by calling
     * {@link #quit()}.
     */
    public static void prepare() {
        prepare(true);
    }
    
    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
    
    /**
     * Initialize the current thread as a looper, marking it as an
     * application's main looper. The main looper for your application
     * is created by the Android environment, so you should never need
     * to call this function yourself.  See also: {@link #prepare()}
     */
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
    
    /**
     * Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }
    
    /**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;
        
        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
            
            // This must be in a local variable, in case a UI event sets the logger
            final long start = System.currentTimeMillis();
            long end = 0;
            try {
                msg.getTarget().dispatchMessage(msg);
                end = System.currentTimeMillis();
            } catch (Exception exception) {
                System.err.println("Exception in message dispatch: " + exception.getMessage());
                exception.printStackTrace();
                end = System.currentTimeMillis();
                // Continue running instead of crashing the thread
            } finally {
                if (start == end) {
                    // Do nothing
                } else {
                    final long timeInMillis = end - start;
                    if (timeInMillis > 100) {
                        System.out.println("Slow dispatch took " + timeInMillis + "ms");
                    }
                }
            }
            
            msg.recycleUnchecked();
        }
    }
    
    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
     */
    public static Looper myLooper() {
        return sThreadLocal.get();
    }
    
    /**
     * Return the {@link MessageQueue} object associated with the current
     * thread.  This must be called from a thread running a Looper, or a
     * NullPointerException will be thrown.
     */
    public static MessageQueue myQueue() {
        return myLooper().mQueue;
    }
    
    /**
     * Returns true if the current thread is this looper's thread.
     */
    public boolean isCurrentThread() {
        return Thread.currentThread() == mThread;
    }
    
    /**
     * Quits the looper.
     * <p>
     * Causes the {@link #loop} method to terminate without processing any
     * more messages in the message queue.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p><p class="note">
     * Using this method may be unsafe because some messages may not be processed
     * before the looper terminates.  Consider using {@link #quitSafely} instead to ensure
     * that all pending work is completed in an orderly manner.
     * </p>
     *
     * @see #quitSafely
     */
    public void quit() {
        mQueue.quit(false);
    }
    
    /**
     * Quits the looper safely.
     * <p>
     * Causes the {@link #loop} method to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * However pending delayed messages with due times in the future will not be
     * delivered before the loop terminates.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p>
     */
    public void quitSafely() {
        mQueue.quit(true);
    }
    
    /**
     * Gets the Thread associated with this Looper.
     *
     * @return The looper's thread.
     */
    public Thread getThread() {
        return mThread;
    }
    
    /**
     * Gets this looper's message queue.
     *
     * @return The looper's message queue.
     */
    public MessageQueue getQueue() {
        return mQueue;
    }
    
    /**
     * Dumps the state of the looper for debugging purposes.
     *
     * @param prefix What to print before each line.
     */
    public void dump(String prefix) {
        System.out.println(prefix + toString());
        System.out.println(prefix + "mQueue=" + ((mQueue != null) ? mQueue : "(null)"));
        if (mQueue != null) {
            synchronized (mQueue) {
                System.out.println(prefix + "(queue state dump would go here)");
            }
        }
    }
    
    /** {@hide} */
    public void writeToProto(long fieldId) {
        // Proto writing would go here in a full implementation
    }
    
    @Override
    public String toString() {
        return "Looper (" + mThread.getName() + ", tid " + mThread.getId() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}