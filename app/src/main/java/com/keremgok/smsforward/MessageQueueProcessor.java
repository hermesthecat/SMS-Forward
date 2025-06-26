package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Processes offline message queue and retries failed messages when connectivity
 * is restored.
 * Runs as a background service with periodic queue processing.
 */
public class MessageQueueProcessor {
    private static final String TAG = "MessageQueueProcessor";
    private static final int QUEUE_PROCESS_INTERVAL_SECONDS = 30; // Process queue every 30 seconds
    private static final int MAX_QUEUE_RETRY_ATTEMPTS = 5; // Maximum queue retries (beyond normal retry mechanism)

    private final Context context;
    private final MessageQueueDbHelper dbHelper;
    private final ScheduledExecutorService queueExecutor;
    private final NetworkStatusManager networkStatusManager;
    private final MessageStatsDbHelper statsHelper;
    private final RateLimiter rateLimiter;
    private volatile boolean isRunning = false;

    public MessageQueueProcessor(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new MessageQueueDbHelper(context);
        this.queueExecutor = Executors.newSingleThreadScheduledExecutor();
        this.networkStatusManager = NetworkStatusManager.getInstance(context);
        this.statsHelper = new MessageStatsDbHelper(context);
        this.rateLimiter = RateLimiter.getInstance();
    }

    /**
     * Start the queue processor service
     */
    public void start() {
        if (isRunning) {
            Log.w(TAG, "Queue processor is already running");
            return;
        }

        isRunning = true;
        Log.i(TAG, "Starting message queue processor");

        // Process queue immediately on start
        queueExecutor.execute(this::processQueue);

        // Schedule periodic queue processing
        queueExecutor.scheduleWithFixedDelay(
                this::processQueue,
                QUEUE_PROCESS_INTERVAL_SECONDS,
                QUEUE_PROCESS_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        // Schedule periodic cleanup
        queueExecutor.scheduleWithFixedDelay(
                () -> {
                    dbHelper.cleanupOldMessages();
                    // Also cleanup old stats (keep 90 days)
                    statsHelper.cleanupOldStats(90);
                },
                1, // Initial delay of 1 hour
                24, // Cleanup every 24 hours
                TimeUnit.HOURS);
    }

    /**
     * Stop the queue processor service
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        Log.i(TAG, "Stopping message queue processor");

        queueExecutor.shutdown();
        try {
            if (!queueExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                queueExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            queueExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Add a failed message to the offline queue
     */
    public void enqueueFailedMessage(String fromNumber, String messageContent, long timestamp,
            String forwarderType, String forwarderConfig) {
        try {
            long messageId = dbHelper.enqueueMessage(fromNumber, messageContent, timestamp,
                    forwarderType, forwarderConfig);
            Log.i(TAG, "Enqueued failed message ID " + messageId + " for later retry");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue message: " + e.getMessage(), e);
        }
    }

    /**
     * Process all pending messages in the queue
     */
    private void processQueue() {
        if (!isRunning) {
            return;
        }

        try {
            List<MessageQueueDbHelper.QueuedMessage> pendingMessages = dbHelper.getPendingMessages();

            if (pendingMessages.isEmpty()) {
                return; // No pending messages
            }

            Log.d(TAG, "Processing " + pendingMessages.size() + " pending messages from queue");

            // Check connectivity before processing
            if (!networkStatusManager.canForwardMessages()) {
                Log.d(TAG, "No network connectivity available (" +
                        networkStatusManager.getConnectionStatus() + "), skipping queue processing");
                return;
            }

            // Check if rate limiting is enabled
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enableRateLimiting = prefs.getBoolean(context.getString(R.string.key_enable_rate_limiting), true);

            for (MessageQueueDbHelper.QueuedMessage queuedMessage : pendingMessages) {
                // Check rate limit before processing each message if enabled
                if (enableRateLimiting && !rateLimiter.isForwardingAllowed()) {
                    Log.d(TAG, String.format("Rate limit reached during queue processing. " +
                            "Current count: %d/10. Skipping remaining messages in this cycle.", 
                            rateLimiter.getCurrentForwardCount()));
                    break; // Stop processing this cycle, will retry in next cycle
                }
                processQueuedMessage(queuedMessage, enableRateLimiting);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing message queue: " + e.getMessage(), e);
        }
    }

    /**
     * Process a single queued message
     */
    private void processQueuedMessage(MessageQueueDbHelper.QueuedMessage queuedMessage, boolean enableRateLimiting) {
        try {
            // Mark as processing
            dbHelper.updateMessage(queuedMessage.id, MessageQueueDbHelper.STATUS_PROCESSING,
                    queuedMessage.retryCount);

            // Create forwarder from stored configuration
            Forwarder forwarder = createForwarderFromConfig(queuedMessage.forwarderType,
                    queuedMessage.forwarderConfig);

            if (forwarder == null) {
                Log.e(TAG, "Failed to create forwarder for type: " + queuedMessage.forwarderType);
                dbHelper.markMessageFailed(queuedMessage.id, queuedMessage.retryCount);
                return;
            }

            // Try to forward the message
            forwarder.forward(queuedMessage.fromNumber, queuedMessage.messageContent,
                    queuedMessage.timestamp);

            // Success - remove from queue, record stats, and update rate limiter if enabled
            dbHelper.markMessageSuccess(queuedMessage.id);
            statsHelper.recordForwardSuccess(queuedMessage.forwarderType);
            if (enableRateLimiting) {
                rateLimiter.recordForwarding();
            }
            Log.i(TAG, "Successfully processed queued message ID " + queuedMessage.id +
                    " via " + queuedMessage.forwarderType);

        } catch (Exception e) {
            // Failed - increment retry count
            int newRetryCount = queuedMessage.retryCount + 1;

            Log.w(TAG, "Failed to process queued message ID " + queuedMessage.id +
                    " (attempt " + newRetryCount + "): " + e.getMessage());

            if (newRetryCount >= MAX_QUEUE_RETRY_ATTEMPTS) {
                // Max retries reached - mark as permanently failed and record stats
                dbHelper.markMessageFailed(queuedMessage.id, newRetryCount);
                statsHelper.recordForwardFailure(queuedMessage.forwarderType);
                Log.e(TAG, "Message ID " + queuedMessage.id + " permanently failed after " +
                        newRetryCount + " queue retry attempts");
            } else {
                // Update retry count and try again later
                dbHelper.updateMessage(queuedMessage.id, MessageQueueDbHelper.STATUS_FAILED,
                        newRetryCount);
            }
        }
    }

    /**
     * Create a forwarder instance from stored configuration
     */
    private Forwarder createForwarderFromConfig(String forwarderType, String configJson) {
        try {
            JSONObject config = new JSONObject(configJson);

            switch (forwarderType) {
                case "SmsForwarder":
                    String targetNumber = config.getString("targetNumber");
                    return new SmsForwarder(targetNumber);

                case "TelegramForwarder":
                    String targetId = config.getString("targetId");
                    String apiKey = config.getString("apiKey");
                    return new TelegramForwarder(targetId, apiKey);

                case "JsonWebForwarder":
                    String targetUrl = config.getString("targetUrl");
                    return new JsonWebForwarder(targetUrl);

                case "EmailForwarder":
                    String fromAddress = config.getString("fromAddress");
                    String toAddress = config.getString("toAddress");
                    String host = config.getString("host");
                    int port = config.getInt("port");
                    String username = config.getString("username");
                    String password = config.getString("password");

                    InternetAddress from = new InternetAddress(fromAddress);
                    InternetAddress[] to = { new InternetAddress(toAddress) };

                    return new EmailForwarder(from, to, host, (short) port, username, password);

                default:
                    Log.e(TAG, "Unknown forwarder type: " + forwarderType);
                    return null;
            }

        } catch (JSONException | AddressException e) {
            Log.e(TAG, "Failed to parse forwarder config: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get current queue statistics
     */
    public MessageQueueDbHelper.QueueStats getQueueStats() {
        return dbHelper.getQueueStats();
    }

    /**
     * Create forwarder configuration JSON for storage
     */
    public static String createForwarderConfig(Forwarder forwarder, Context context) {
        try {
            JSONObject config = new JSONObject();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (forwarder instanceof RetryableForwarder) {
                forwarder = ((RetryableForwarder) forwarder).getDelegate();
            }

            if (forwarder instanceof SmsForwarder) {
                String targetNumber = prefs.getString(context.getString(R.string.key_target_sms), "");
                config.put("targetNumber", targetNumber);

            } else if (forwarder instanceof TelegramForwarder) {
                String targetId = prefs.getString(context.getString(R.string.key_target_telegram), "");
                String apiKey = prefs.getString(context.getString(R.string.key_telegram_apikey), "");
                config.put("targetId", targetId);
                config.put("apiKey", apiKey);

            } else if (forwarder instanceof JsonWebForwarder) {
                String targetUrl = prefs.getString(context.getString(R.string.key_target_web), "");
                config.put("targetUrl", targetUrl);

            } else if (forwarder instanceof EmailForwarder) {
                String fromAddress = prefs.getString(context.getString(R.string.key_email_from_address), "");
                String toAddress = prefs.getString(context.getString(R.string.key_email_to_address), "");
                String host = prefs.getString(context.getString(R.string.key_email_submit_host), "");
                String port = prefs.getString(context.getString(R.string.key_email_submit_port), "587");
                String password = prefs.getString(context.getString(R.string.key_email_submit_password), "");
                String usernameStyle = prefs.getString(context.getString(R.string.key_email_username_style), "full");

                String username = usernameStyle.equals("full") ? fromAddress
                        : (fromAddress.contains("@") ? fromAddress.substring(0, fromAddress.indexOf("@"))
                                : fromAddress);

                config.put("fromAddress", fromAddress);
                config.put("toAddress", toAddress);
                config.put("host", host);
                config.put("port", Integer.parseInt(port));
                config.put("username", username);
                config.put("password", password);
            }

            return config.toString();

        } catch (JSONException e) {
            Log.e(TAG, "Failed to create forwarder config: " + e.getMessage(), e);
            return "{}";
        }
    }
}