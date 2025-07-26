package com.keremgok.smsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final Pattern REVERSE_MESSAGE_PATTERN = Pattern.compile("To (\\+?\\d+?):\\n((.|\\n)*)");

    // Remove static fields to prevent memory leaks
    private final Executor forwarderExecutor = Executors.newCachedThreadPool();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))
            return;

        // Initialize instances per onReceive call to avoid static context references
        MessageQueueProcessor queueProcessor = new MessageQueueProcessor(context);
        queueProcessor.start();

        MessageStatsDbHelper statsDbHelper = new MessageStatsDbHelper(context);
        MessageHistoryDbHelper historyDbHelper = new MessageHistoryDbHelper(context);
        RateLimiter rateLimiter = RateLimiter.getInstance();

        // Large message might be broken into several parts.
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages.length == 0) {
            Log.wtf(TAG, "Got empty message");
            return;
        }
        final String fromNumber = messages[0].getDisplayOriginatingAddress();
        final String messageContent = Arrays.stream(messages)
                .map(SmsMessage::getDisplayMessageBody)
                .collect(Collectors.joining());
        final long timestamp = messages[0].getTimestampMillis();
        Log.d(TAG, String.format("Received SMS message from %s, content: %s", fromNumber, messageContent));

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableSms = preferences.getBoolean(context.getString(R.string.key_enable_sms), false);
        String targetNumber = preferences.getString(context.getString(R.string.key_target_sms), "");
        boolean enableTelegram = preferences.getBoolean(context.getString(R.string.key_enable_telegram), false);
        String targetTelegram = preferences.getString(context.getString(R.string.key_target_telegram), "");
        String telegramToken = preferences.getString(context.getString(R.string.key_telegram_apikey), "");
        boolean enableWeb = preferences.getBoolean(context.getString(R.string.key_enable_web), false);
        String targetWeb = preferences.getString(context.getString(R.string.key_target_web), "");
        boolean enableEmail = preferences.getBoolean(context.getString(R.string.key_enable_email), false);
        String fromEmailAddress = preferences.getString(context.getString(R.string.key_email_from_address), "");
        String toEmailAddress = preferences.getString(context.getString(R.string.key_email_to_address), "");
        String smtpHost = preferences.getString(context.getString(R.string.key_email_submit_host), "");
        short smtpPort = Short
                .parseShort(preferences.getString(context.getString(R.string.key_email_submit_port), "0"));
        String smtpPassword = preferences.getString(context.getString(R.string.key_email_submit_password), "");
        String smtpUsernameStyle = preferences.getString(context.getString(R.string.key_email_username_style), "full");
        boolean enableRateLimiting = preferences.getBoolean(context.getString(R.string.key_enable_rate_limiting), true);
        String filterKeywords = preferences.getString(context.getString(R.string.key_filter_keywords), "");

        // TODO: add a dedicated preference item for reverse forwarding
        // Disables reverse forwarding too if no forwarders is enabled.
        if (!enableSms && !enableTelegram && !enableWeb && !enableEmail)
            return;

        // Check content filter - block message if it contains filtered keywords
        if (SmsContentFilter.shouldBlockMessage(messageContent, filterKeywords)) {
            Log.i(TAG, String.format("Message from %s blocked by content filter", fromNumber));
            return; // Don't forward the message
        }

        // Number Whitelist Filtering
        boolean whitelistEnabled = preferences.getBoolean(context.getString(R.string.key_enable_number_whitelist), false);
        if (whitelistEnabled) {
            String numberWhitelist = preferences.getString(context.getString(R.string.key_number_whitelist), "");
            if (SmsNumberFilter.shouldBlockNumber(fromNumber, numberWhitelist)) {
                Log.d(TAG, "Message from " + fromNumber + " blocked by number whitelist.");
                return; // Stop processing, number not in whitelist
            }
        }

        ArrayList<Forwarder> forwarders = new ArrayList<>(1);
        if (enableSms && !targetNumber.isEmpty()) {
            SmsForwarder smsForwarder = new SmsForwarder(targetNumber, context);
            RetryableForwarder retryableForwarder = new RetryableForwarder(smsForwarder, queueProcessor);
            retryableForwarder.setStatsHelper(statsDbHelper);
            retryableForwarder.setHistoryHelper(historyDbHelper);
            forwarders.add(retryableForwarder);
        }
        if (enableTelegram && !targetTelegram.isEmpty() && !telegramToken.isEmpty()) {
            TelegramForwarder telegramForwarder = new TelegramForwarder(targetTelegram, telegramToken, context);
            RetryableForwarder retryableForwarder = new RetryableForwarder(telegramForwarder, queueProcessor);
            retryableForwarder.setStatsHelper(statsDbHelper);
            retryableForwarder.setHistoryHelper(historyDbHelper);
            forwarders.add(retryableForwarder);
        }
        if (enableWeb && !targetWeb.isEmpty()) {
            JsonWebForwarder webForwarder = new JsonWebForwarder(targetWeb);
            RetryableForwarder retryableForwarder = new RetryableForwarder(webForwarder, queueProcessor);
            retryableForwarder.setStatsHelper(statsDbHelper);
            retryableForwarder.setHistoryHelper(historyDbHelper);
            forwarders.add(retryableForwarder);
        }
        if (enableEmail && !fromEmailAddress.isEmpty() && !toEmailAddress.isEmpty() &&
                !smtpHost.isEmpty() && smtpPort != 0 && !smtpPassword.isEmpty()) {
            InternetAddress fromAddress;
            InternetAddress[] toAddresses;
            try {
                fromAddress = new InternetAddress(fromEmailAddress, true);
                toAddresses = InternetAddress.parse(toEmailAddress);
            } catch (AddressException e) {
                throw new IllegalArgumentException("Invalid email address", e);
            }
            String username = "full".equals(smtpUsernameStyle)
                    ? fromAddress.getAddress()
                    : fromAddress.getAddress().substring(0, fromAddress.getAddress().indexOf("@"));
            EmailForwarder emailForwarder = new EmailForwarder(
                    fromAddress,
                    toAddresses,
                    smtpHost,
                    smtpPort,
                    username,
                    smtpPassword,
                    context);
            RetryableForwarder retryableForwarder = new RetryableForwarder(emailForwarder, queueProcessor);
            retryableForwarder.setStatsHelper(statsDbHelper);
            retryableForwarder.setHistoryHelper(historyDbHelper);
            forwarders.add(retryableForwarder);
        }

        if (fromNumber.equals(targetNumber)) {
            // Reverse message - check rate limit first if enabled
            if (enableRateLimiting && !rateLimiter.isForwardingAllowed()) {
                Log.w(TAG, String.format("Rate limit exceeded for reverse SMS to %s. Current count: %d/10. " +
                        "Time until next slot: %d seconds",
                        fromNumber, rateLimiter.getCurrentForwardCount(),
                        rateLimiter.getTimeUntilNextSlot() / 1000));
                return; // Skip reverse forwarding due to rate limit
            }

            Matcher matcher = REVERSE_MESSAGE_PATTERN.matcher(messageContent);
            if (matcher.matches()) {
                String forwardNumber = matcher.replaceFirst("$1");
                String forwardContent = matcher.replaceFirst("$2");
                forwarderExecutor.execute(() -> {
                    try {
                        SmsForwarder.sendSmsTo(forwardNumber, forwardContent);
                        // Record successful reverse forwarding for rate limiting if enabled
                        if (enableRateLimiting) {
                            rateLimiter.recordForwarding();
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to send SMS", e);
                    }
                });
            }
        } else {
            // Normal message, forwarded - check rate limit first if enabled
            if (enableRateLimiting && !rateLimiter.isForwardingAllowed()) {
                Log.w(TAG, String.format("Rate limit exceeded for SMS from %s. Current count: %d/10. " +
                        "Time until next slot: %d seconds",
                        fromNumber, rateLimiter.getCurrentForwardCount(),
                        rateLimiter.getTimeUntilNextSlot() / 1000));

                // Optionally add to queue for later processing when rate limit resets
                if (queueProcessor != null) {
                    for (Forwarder forwarder : forwarders) {
                        if (forwarder instanceof RetryableForwarder) {
                            try {
                                RetryableForwarder retryableForwarder = (RetryableForwarder) forwarder;
                                String forwarderType = retryableForwarder.getDelegateName();
                                String forwarderConfig = MessageQueueProcessor.createForwarderConfig(
                                        retryableForwarder.getDelegate(), context);

                                queueProcessor.enqueueFailedMessage(fromNumber, messageContent,
                                        timestamp, forwarderType, forwarderConfig);
                                Log.i(TAG, "Added rate-limited message to offline queue via " + forwarderType);
                            } catch (Exception queueError) {
                                Log.e(TAG, "Failed to add rate-limited message to offline queue: "
                                        + queueError.getMessage());
                            }
                        }
                    }
                }
                return; // Skip forwarding due to rate limit
            }

            for (Forwarder forwarder : forwarders) {
                forwarderExecutor.execute(() -> {
                    try {
                        forwarder.forward(fromNumber, messageContent, timestamp);
                        // Record successful forwarding for rate limiting if enabled
                        if (enableRateLimiting) {
                            rateLimiter.recordForwarding();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to forward SMS", e);

                        // If this is a RetryableForwarder, try to add to offline queue
                        if (forwarder instanceof RetryableForwarder && queueProcessor != null) {
                            try {
                                RetryableForwarder retryableForwarder = (RetryableForwarder) forwarder;
                                String forwarderType = retryableForwarder.getDelegateName();
                                String forwarderConfig = MessageQueueProcessor.createForwarderConfig(
                                        retryableForwarder.getDelegate(), context);

                                queueProcessor.enqueueFailedMessage(fromNumber, messageContent,
                                        timestamp, forwarderType, forwarderConfig);
                                Log.i(TAG, "Added failed message to offline queue via " + forwarderType);
                            } catch (Exception queueError) {
                                Log.e(TAG, "Failed to add message to offline queue: " + queueError.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }
}