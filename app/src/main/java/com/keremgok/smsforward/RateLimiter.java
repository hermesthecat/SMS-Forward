package com.keremgok.smsforward;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Rate limiter to prevent spam by limiting the number of SMS forwards per minute.
 * Uses a sliding window approach to track forwarding attempts.
 * Singleton pattern ensures consistent rate limiting across all components.
 */
public class RateLimiter {
    private static final String TAG = "RateLimiter";
    private static final int MAX_SMS_PER_MINUTE = 10;
    private static final long ONE_MINUTE_MS = 60 * 1000; // 60 seconds in milliseconds
    
    private static volatile RateLimiter instance;
    private final Queue<Long> forwardingTimestamps;
    private final Object lock = new Object();
    
    private RateLimiter() {
        this.forwardingTimestamps = new LinkedList<>();
    }
    
    /**
     * Get the singleton instance of RateLimiter.
     * 
     * @return the singleton RateLimiter instance
     */
    public static RateLimiter getInstance() {
        if (instance == null) {
            synchronized (RateLimiter.class) {
                if (instance == null) {
                    instance = new RateLimiter();
                }
            }
        }
        return instance;
    }
    
    /**
     * Check if a new SMS forwarding attempt is allowed based on rate limiting rules.
     * 
     * @return true if forwarding is allowed, false if rate limit is exceeded
     */
    public boolean isForwardingAllowed() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            
            // Remove timestamps older than 1 minute
            while (!forwardingTimestamps.isEmpty() && 
                   (currentTime - forwardingTimestamps.peek()) > ONE_MINUTE_MS) {
                forwardingTimestamps.poll();
            }
            
            // Check if we're under the limit
            if (forwardingTimestamps.size() < MAX_SMS_PER_MINUTE) {
                return true;
            }
            
            Log.w(TAG, String.format("Rate limit exceeded: %d SMS forwarded in the last minute. Maximum allowed: %d", 
                    forwardingTimestamps.size(), MAX_SMS_PER_MINUTE));
            return false;
        }
    }
    
    /**
     * Record a successful SMS forwarding attempt.
     * This should be called after a successful forward operation.
     */
    public void recordForwarding() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            forwardingTimestamps.offer(currentTime);
            
            Log.d(TAG, String.format("Recorded SMS forward. Total in last minute: %d/%d", 
                    forwardingTimestamps.size(), MAX_SMS_PER_MINUTE));
        }
    }
    
    /**
     * Get the number of SMS forwards in the current minute window.
     * 
     * @return current count of forwards in the sliding window
     */
    public int getCurrentForwardCount() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            
            // Clean up old timestamps
            while (!forwardingTimestamps.isEmpty() && 
                   (currentTime - forwardingTimestamps.peek()) > ONE_MINUTE_MS) {
                forwardingTimestamps.poll();
            }
            
            return forwardingTimestamps.size();
        }
    }
    
    /**
     * Get the time until the next SMS forwarding slot becomes available.
     * 
     * @return milliseconds until next available slot, or 0 if slot is immediately available
     */
    public long getTimeUntilNextSlot() {
        synchronized (lock) {
            if (forwardingTimestamps.size() < MAX_SMS_PER_MINUTE) {
                return 0; // Slot available immediately
            }
            
            // Find the oldest timestamp that's still within the window
            long oldestTimestamp = forwardingTimestamps.peek();
            long currentTime = System.currentTimeMillis();
            
            return ONE_MINUTE_MS - (currentTime - oldestTimestamp);
        }
    }
    
    /**
     * Reset the rate limiter (mainly for testing purposes).
     */
    public void reset() {
        synchronized (lock) {
            forwardingTimestamps.clear();
            Log.d(TAG, "Rate limiter reset");
        }
    }
} 