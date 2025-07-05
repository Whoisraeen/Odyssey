package com.odyssey.ui.threading;

/**
 * MessageQueue class for managing messages in the UI thread
 * Inspired by Android's MessageQueue system
 */
public class MessageQueue {
    private Message mMessages;
    private final Object mLock = new Object();
    private boolean mQuitting = false;
    private boolean mQuitAllowed = true;
    
    // Idle handlers
    private final java.util.ArrayList<IdleHandler> mIdleHandlers = new java.util.ArrayList<>();
    private IdleHandler[] mPendingIdleHandlers;
    
    /**
     * Interface for idle handlers
     */
    public interface IdleHandler {
        /**
         * Called when the message queue has run out of messages and will now wait for more.
         * @return true to keep the idle handler active, false to remove it
         */
        boolean queueIdle();
    }
    
    MessageQueue(boolean quitAllowed) {
        mQuitAllowed = quitAllowed;
    }
    
    /**
     * Get the next message from the queue, blocking if necessary
     */
    Message next() {
        long nextPollTimeoutMillis = 0;
        
        for (;;) {
            synchronized (mLock) {
                // Check if we're quitting
                if (mQuitting) {
                    return null;
                }
                
                final long now = System.currentTimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                
                // Find the next message to process
                while (msg != null && msg.getWhen() > now) {
                    prevMsg = msg;
                    msg = msg.getNext();
                }
                
                if (msg != null) {
                    // Found a message to process
                    if (prevMsg != null) {
                        prevMsg.setNext(msg.getNext());
                    } else {
                        mMessages = msg.getNext();
                    }
                    msg.setNext(null);
                    msg.markInUse();
                    return msg;
                }
                
                // No messages ready, calculate next poll timeout
                if (mMessages != null) {
                    nextPollTimeoutMillis = Math.max(0, mMessages.getWhen() - now);
                } else {
                    nextPollTimeoutMillis = -1; // Wait indefinitely
                }
                
                // Process idle handlers if we have time
                if (nextPollTimeoutMillis != 0) {
                    processIdleHandlers();
                }
            }
            
            // Wait for the next message
            if (nextPollTimeoutMillis > 0) {
                try {
                    Thread.sleep(nextPollTimeoutMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else if (nextPollTimeoutMillis < 0) {
                // Wait indefinitely
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
    }
    
    /**
     * Enqueue a message in the queue
     */
    boolean enqueueMessage(Message msg, long when) {
        if (msg.getTarget() == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException("Message is already in use.");
        }
        
        synchronized (mLock) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        "Trying to enqueue message on a quitting MessageQueue");
                msg.recycle();
                throw e;
            }
            
            msg.markInUse();
            msg.setWhen(when);
            
            Message p = mMessages;
            boolean needWake;
            
            if (p == null || when == 0 || when < p.getWhen()) {
                // New head, wake up the event queue if blocked
                msg.setNext(p);
                mMessages = msg;
                needWake = true;
            } else {
                // Inserted within the middle of the queue
                needWake = false;
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.getNext();
                    if (p == null || when < p.getWhen()) {
                        break;
                    }
                }
                msg.setNext(p);
                prev.setNext(msg);
            }
            
            // Wake up the queue if needed
            if (needWake) {
                mLock.notify();
            }
        }
        return true;
    }
    
    /**
     * Remove all messages with the specified Handler
     */
    void removeMessages(Handler h) {
        if (h == null) {
            return;
        }
        
        synchronized (mLock) {
            Message p = mMessages;
            
            // Remove from head
            while (p != null && p.getTarget() == h) {
                Message n = p.getNext();
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }
            
            // Remove from middle
            while (p != null) {
                Message n = p.getNext();
                if (n != null && n.getTarget() == h) {
                    Message nn = n.getNext();
                    n.recycleUnchecked();
                    p.setNext(nn);
                    continue;
                }
                p = n;
            }
        }
    }
    
    /**
     * Remove all messages with the specified Handler and what value
     */
    void removeMessages(Handler h, int what) {
        if (h == null) {
            return;
        }
        
        synchronized (mLock) {
            Message p = mMessages;
            
            // Remove from head
            while (p != null && p.getTarget() == h && p.what == what) {
                Message n = p.getNext();
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }
            
            // Remove from middle
            while (p != null) {
                Message n = p.getNext();
                if (n != null && n.getTarget() == h && n.what == what) {
                    Message nn = n.getNext();
                    n.recycleUnchecked();
                    p.setNext(nn);
                    continue;
                }
                p = n;
            }
        }
    }
    
    /**
     * Remove all callbacks with the specified Handler and Runnable
     */
    void removeCallbacks(Handler h, Runnable r) {
        if (h == null || r == null) {
            return;
        }
        
        synchronized (mLock) {
            Message p = mMessages;
            
            // Remove from head
            while (p != null && p.getTarget() == h && p.callback == r) {
                Message n = p.getNext();
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }
            
            // Remove from middle
            while (p != null) {
                Message n = p.getNext();
                if (n != null && n.getTarget() == h && n.callback == r) {
                    Message nn = n.getNext();
                    n.recycleUnchecked();
                    p.setNext(nn);
                    continue;
                }
                p = n;
            }
        }
    }
    
    /**
     * Add an idle handler
     */
    public void addIdleHandler(IdleHandler handler) {
        if (handler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        synchronized (mLock) {
            mIdleHandlers.add(handler);
        }
    }
    
    /**
     * Remove an idle handler
     */
    public void removeIdleHandler(IdleHandler handler) {
        synchronized (mLock) {
            mIdleHandlers.remove(handler);
        }
    }
    
    /**
     * Process idle handlers
     */
    private void processIdleHandlers() {
        final int pendingIdleHandlerCount;
        synchronized (mLock) {
            pendingIdleHandlerCount = mIdleHandlers.size();
            if (pendingIdleHandlerCount <= 0) {
                return;
            }
            
            if (mPendingIdleHandlers == null) {
                mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
            } else if (mPendingIdleHandlers.length < pendingIdleHandlerCount) {
                mPendingIdleHandlers = new IdleHandler[pendingIdleHandlerCount];
            }
            
            mIdleHandlers.toArray(mPendingIdleHandlers);
        }
        
        // Process idle handlers outside of the lock
        for (int i = 0; i < pendingIdleHandlerCount; i++) {
            final IdleHandler idler = mPendingIdleHandlers[i];
            mPendingIdleHandlers[i] = null; // release the reference to the handler
            
            boolean keep = false;
            try {
                keep = idler.queueIdle();
            } catch (Throwable t) {
                System.err.println("IdleHandler threw exception: " + t.getMessage());
                t.printStackTrace();
            }
            
            if (!keep) {
                synchronized (mLock) {
                    mIdleHandlers.remove(idler);
                }
            }
        }
    }
    
    /**
     * Quit the message queue
     */
    void quit(boolean safe) {
        if (!mQuitAllowed) {
            throw new IllegalStateException("Main thread not allowed to quit.");
        }
        
        synchronized (mLock) {
            if (mQuitting) {
                return;
            }
            mQuitting = true;
            
            if (safe) {
                removeAllFutureMessagesLocked();
            } else {
                removeAllMessagesLocked();
            }
            
            mLock.notifyAll();
        }
    }
    
    /**
     * Remove all messages
     */
    private void removeAllMessagesLocked() {
        Message p = mMessages;
        while (p != null) {
            Message n = p.getNext();
            p.recycleUnchecked();
            p = n;
        }
        mMessages = null;
    }
    
    /**
     * Remove all future messages (keep current ones)
     */
    private void removeAllFutureMessagesLocked() {
        final long now = System.currentTimeMillis();
        Message p = mMessages;
        if (p != null) {
            if (p.getWhen() > now) {
                removeAllMessagesLocked();
            } else {
                Message n;
                for (;;) {
                    n = p.getNext();
                    if (n == null) {
                        return;
                    }
                    if (n.getWhen() > now) {
                        break;
                    }
                    p = n;
                }
                p.setNext(null);
                do {
                    p = n;
                    n = p.getNext();
                    p.recycleUnchecked();
                } while (n != null);
            }
        }
    }
    
    /**
     * Check if the queue is quitting
     */
    boolean isQuitting() {
        synchronized (mLock) {
            return mQuitting;
        }
    }
}