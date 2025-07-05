package com.odyssey.ui.threading;

/**
 * UIThread class for managing the dedicated UI thread
 * Provides a separate thread for UI operations with message queue support
 */
public class UIThread extends Thread {
    private static final String TAG = "UIThread";
    private static UIThread sInstance;
    private static Handler sHandler;
    private static final Object sLock = new Object();
    
    private volatile boolean mReady = false;
    private final Object mReadyLock = new Object();
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private UIThread() {
        super("UIThread");
        setDaemon(false); // Keep the thread alive
    }
    
    /**
     * Get the singleton instance of UIThread
     */
    public static UIThread getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new UIThread();
            }
            return sInstance;
        }
    }
    
    /**
     * Start the UI thread if not already started
     */
    public static void startUIThread() {
        UIThread thread = getInstance();
        if (!thread.isAlive()) {
            thread.start();
            thread.waitUntilReady();
        }
    }
    
    /**
     * Get the UI thread handler
     */
    public static Handler getHandler() {
        synchronized (sLock) {
            if (sHandler == null) {
                startUIThread();
            }
            return sHandler;
        }
    }
    
    /**
     * Check if the current thread is the UI thread
     */
    public static boolean isUIThread() {
        return Thread.currentThread() == getInstance();
    }
    
    /**
     * Post a runnable to the UI thread
     */
    public static void post(Runnable runnable) {
        getHandler().post(runnable);
    }
    
    /**
     * Post a runnable to the UI thread with delay
     */
    public static void postDelayed(Runnable runnable, long delayMillis) {
        getHandler().postDelayed(runnable, delayMillis);
    }
    
    /**
     * Remove callbacks from the UI thread
     */
    public static void removeCallbacks(Runnable runnable) {
        if (sHandler != null) {
            sHandler.removeCallbacks(runnable);
        }
    }
    
    /**
     * Send a message to the UI thread
     */
    public static void sendMessage(Message message) {
        getHandler().sendMessage(message);
    }
    
    /**
     * Send an empty message to the UI thread
     */
    public static void sendEmptyMessage(int what) {
        getHandler().sendEmptyMessage(what);
    }
    
    /**
     * Run on UI thread - if already on UI thread, run immediately, otherwise post
     */
    public static void runOnUIThread(Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
        } else {
            post(runnable);
        }
    }
    
    /**
     * Wait until the UI thread is ready
     */
    private void waitUntilReady() {
        synchronized (mReadyLock) {
            while (!mReady) {
                try {
                    mReadyLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
    
    /**
     * Mark the UI thread as ready
     */
    private void setReady() {
        synchronized (mReadyLock) {
            mReady = true;
            mReadyLock.notifyAll();
        }
    }
    
    @Override
    public void run() {
        try {
            // Prepare the looper for this thread
            Looper.prepare();
            
            // Create the handler for this thread
            synchronized (sLock) {
                sHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // Handle UI thread messages here
                        handleUIMessage(msg);
                    }
                };
            }
            
            // Mark as ready
            setReady();
            
            System.out.println("UIThread started and ready");
            
            // Start the message loop
            Looper.loop();
            
        } catch (Exception e) {
            System.err.println("UIThread error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            synchronized (sLock) {
                sHandler = null;
                sInstance = null;
            }
            System.out.println("UIThread terminated");
        }
    }
    
    /**
     * Handle UI thread messages
     */
    private void handleUIMessage(Message msg) {
        switch (msg.what) {
            case MSG_RENDER:
                handleRenderMessage(msg);
                break;
            case MSG_LAYOUT:
                handleLayoutMessage(msg);
                break;
            case MSG_INPUT:
                handleInputMessage(msg);
                break;
            case MSG_ANIMATION:
                handleAnimationMessage(msg);
                break;
            default:
                System.out.println("Unknown UI message: " + msg.what);
                break;
        }
    }
    
    /**
     * Handle render messages
     */
    private void handleRenderMessage(Message msg) {
        // Render message handling would go here
        // This could trigger UI rendering operations
    }
    
    /**
     * Handle layout messages
     */
    private void handleLayoutMessage(Message msg) {
        // Layout message handling would go here
        // This could trigger layout calculations
    }
    
    /**
     * Handle input messages
     */
    private void handleInputMessage(Message msg) {
        // Input message handling would go here
        // This could process input events
    }
    
    /**
     * Handle animation messages
     */
    private void handleAnimationMessage(Message msg) {
        // Animation message handling would go here
        // This could update animation states
    }
    
    /**
     * Quit the UI thread safely
     */
    public static void quitSafely() {
        UIThread thread = getInstance();
        if (thread.isAlive()) {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quitSafely();
            }
        }
    }
    
    /**
     * Quit the UI thread immediately
     */
    public static void quit() {
        UIThread thread = getInstance();
        if (thread.isAlive()) {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        }
    }
    
    // Message types for UI operations
    public static final int MSG_RENDER = 1;
    public static final int MSG_LAYOUT = 2;
    public static final int MSG_INPUT = 3;
    public static final int MSG_ANIMATION = 4;
    
    // Utility methods for sending specific message types
    
    /**
     * Request a render operation
     */
    public static void requestRender() {
        sendEmptyMessage(MSG_RENDER);
    }
    
    /**
     * Request a layout operation
     */
    public static void requestLayout() {
        sendEmptyMessage(MSG_LAYOUT);
    }
    
    /**
     * Send an input event
     */
    public static void sendInputEvent(Object inputEvent) {
        Message msg = Message.obtain();
        msg.what = MSG_INPUT;
        msg.obj = inputEvent;
        sendMessage(msg);
    }
    
    /**
     * Request animation update
     */
    public static void requestAnimationUpdate() {
        sendEmptyMessage(MSG_ANIMATION);
    }
    
    /**
     * Get thread statistics
     */
    public static String getThreadStats() {
        UIThread thread = getInstance();
        return String.format("UIThread[id=%d, name=%s, state=%s, alive=%b]", 
                thread.getId(), thread.getName(), thread.getState(), thread.isAlive());
    }
}