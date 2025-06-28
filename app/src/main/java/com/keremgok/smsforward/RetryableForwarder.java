package com.keremgok.smsforward;

import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper class that adds retry functionality to any Forwarder
 * implementation.
 * Provides automatic retry with exponential backoff for failed forward
 * operations.
 */
public class RetryableForwarder implements Forwarder {
    private static final String TAG = "RetryableForwarder";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0; // Exponential backoff

    private final Forwarder delegate;
    private final ScheduledExecutorService retryExecutor;
    private final MessageQueueProcessor queueProcessor;
    private MessageStatsDbHelper statsHelper;
    private MessageHistoryDbHelper historyHelper;

    public RetryableForwarder(Forwarder delegate) {
        this.delegate = delegate;
        this.retryExecutor = Executors.newScheduledThreadPool(2);
        this.queueProcessor = null; // Will be set later via constructor or setter
    }

    public RetryableForwarder(Forwarder delegate, MessageQueueProcessor queueProcessor) {
        this.delegate = delegate;
        this.retryExecutor = Executors.newScheduledThreadPool(2);
        this.queueProcessor = queueProcessor;
    }

    /**
     * Set the stats helper for tracking forwarding statistics
     */
    public void setStatsHelper(MessageStatsDbHelper statsHelper) {
        this.statsHelper = statsHelper;
    }

    /**
     * Set the history helper for tracking message history
     */
    public void setHistoryHelper(MessageHistoryDbHelper historyHelper) {
        this.historyHelper = historyHelper;
    }

    @Override
    public void forward(String fromNumber, String content) throws Exception {
        forward(fromNumber, content, System.currentTimeMillis());
    }

    @Override
    public void forward(String fromNumber, String content, long timestamp) throws Exception {
        attemptForwardWithRetry(fromNumber, content, timestamp, 1);
    }

    /**
     * Attempts to forward a message with automatic retry on failure.
     * Uses exponential backoff between retry attempts.
     * 
     * @param fromNumber The sender's phone number
     * @param content    The message content
     * @param timestamp  The message timestamp
     * @param attempt    Current attempt number (1-based)
     */
    private void attemptForwardWithRetry(String fromNumber, String content, long timestamp, int attempt) {
        try {
            // Try to forward the message
            delegate.forward(fromNumber, content, timestamp);

            // Success - record in stats and history
            if (statsHelper != null) {
                statsHelper.recordForwardSuccess(delegate.getClass().getSimpleName());
            }

            if (historyHelper != null) {
                historyHelper.recordForwardSuccess(fromNumber, content,
                        delegate.getClass().getSimpleName(), timestamp);
            }

            if (attempt > 1) {
                Log.i(TAG, String.format("Forward succeeded on attempt %d for %s via %s",
                        attempt, fromNumber, delegate.getClass().getSimpleName()));
            }

        } catch (Exception e) {
            Log.w(TAG, String.format("Forward attempt %d failed for %s via %s: %s",
                    attempt, fromNumber, delegate.getClass().getSimpleName(), e.getMessage()));

            if (attempt < MAX_RETRY_ATTEMPTS) {
                // Schedule retry with exponential backoff
                long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));

                Log.i(TAG, String.format("Scheduling retry %d/%d in %d ms for %s via %s",
                        attempt + 1, MAX_RETRY_ATTEMPTS, delay, fromNumber, delegate.getClass().getSimpleName()));

                retryExecutor.schedule(() -> {
                    attemptForwardWithRetry(fromNumber, content, timestamp, attempt + 1);
                }, delay, TimeUnit.MILLISECONDS);

            } else {
                // All retry attempts exhausted - record failure in stats and history
                if (statsHelper != null) {
                    statsHelper.recordForwardFailure(delegate.getClass().getSimpleName());
                }

                if (historyHelper != null) {
                    historyHelper.recordForwardFailure(fromNumber, content,
                            delegate.getClass().getSimpleName(), e.getMessage(), timestamp);
                }

                Log.e(TAG, String.format("All %d retry attempts failed for %s via %s. Final error: %s",
                        MAX_RETRY_ATTEMPTS, fromNumber, delegate.getClass().getSimpleName(), e.getMessage()));

                if (queueProcessor != null) {
                    try {
                        String forwarderType = delegate.getClass().getSimpleName();
                        String forwarderConfig = createForwarderConfig();
                        queueProcessor.enqueueFailedMessage(fromNumber, content, timestamp,
                                forwarderType, forwarderConfig);
                        Log.i(TAG, "Added failed message to offline queue for later retry");
                    } catch (Exception queueError) {
                        Log.e(TAG, "Failed to add message to offline queue: " + queueError.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Get the underlying forwarder implementation
     */
    public Forwarder getDelegate() {
        return delegate;
    }

    /**
     * Get the class name of the underlying forwarder for identification
     */
    public String getDelegateName() {
        return delegate.getClass().getSimpleName();
    }

    /**
     * Create configuration JSON for the delegate forwarder
     */
    private String createForwarderConfig() {
        // For now, return empty config - the MessageQueueProcessor will create proper
        // config
        // when processing from SmsReceiver context
        return "{}";
    }

    /**
     * Shutdown the retry executor service.
     * Call this when the application is being destroyed to clean up resources.
     */
    public void shutdown() {
        if (retryExecutor != null && !retryExecutor.isShutdown()) {
            retryExecutor.shutdown();
            try {
                if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    retryExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                retryExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}