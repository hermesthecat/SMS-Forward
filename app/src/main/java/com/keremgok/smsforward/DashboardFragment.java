package com.keremgok.smsforward;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard Fragment showing real-time status overview and quick actions.
 * Unlike other fragments, this is not preference-based but displays live data.
 */
public class DashboardFragment extends Fragment implements 
        NetworkStatusManager.NetworkStatusListener, 
        SharedPreferences.OnSharedPreferenceChangeListener {

    // UI Components
    private TextView connectionStatusText;
    private TextView todayMessagesText;
    private TextView rateLimitText;
    private TextView smsStatusText;
    private TextView telegramStatusText;
    private TextView emailStatusText;
    private TextView webStatusText;
    private MaterialButton sendTestButton;
    private MaterialButton viewHistoryButton;
    private MaterialButton platformSettingsButton;

    // Core components
    private NetworkStatusManager networkStatusManager;
    private SharedPreferences sharedPreferences;
    private MessageHistoryDbHelper historyDbHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        initializeUIComponents(view);

        // Initialize core components
        initializeCoreComponents();

        // Set up button listeners
        setupButtonListeners();

        // Update all status displays
        updateAllStatuses();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Start network monitoring
        if (networkStatusManager != null) {
            networkStatusManager.startMonitoring();
            networkStatusManager.addListener(this);
        }

        // Register for preference changes
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        // Update all status displays
        updateAllStatuses();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Stop network monitoring
        if (networkStatusManager != null) {
            networkStatusManager.removeListener(this);
        }

        // Unregister preference changes
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (networkStatusManager != null) {
            networkStatusManager.removeListener(this);
            networkStatusManager.stopMonitoring();
        }

        if (historyDbHelper != null) {
            historyDbHelper.close();
        }
    }

    @Override
    public void onNetworkStatusChanged(boolean isConnected, String connectionType) {
        // Update UI on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateConnectionStatus);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update relevant status when preferences change
        if (getContext() != null) {
            String smsKey = getContext().getString(R.string.key_enable_sms);
            String telegramKey = getContext().getString(R.string.key_enable_telegram);
            String emailKey = getContext().getString(R.string.key_enable_email);
            String webKey = getContext().getString(R.string.key_enable_web);

            if (smsKey.equals(key) || telegramKey.equals(key) || 
                emailKey.equals(key) || webKey.equals(key)) {
                updatePlatformStatuses();
            }
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeUIComponents(View view) {
        // Status displays
        connectionStatusText = view.findViewById(R.id.connection_status_text);
        todayMessagesText = view.findViewById(R.id.today_messages_text);
        rateLimitText = view.findViewById(R.id.rate_limit_text);
        
        // Platform status displays
        smsStatusText = view.findViewById(R.id.sms_status_text);
        telegramStatusText = view.findViewById(R.id.telegram_status_text);
        emailStatusText = view.findViewById(R.id.email_status_text);
        webStatusText = view.findViewById(R.id.web_status_text);
        
        // Action buttons
        sendTestButton = view.findViewById(R.id.send_test_button);
        viewHistoryButton = view.findViewById(R.id.view_history_button);
        platformSettingsButton = view.findViewById(R.id.platform_settings_button);
    }

    /**
     * Initialize core components
     */
    private void initializeCoreComponents() {
        if (getContext() != null) {
            networkStatusManager = NetworkStatusManager.getInstance(getContext());
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            historyDbHelper = new MessageHistoryDbHelper(getContext());
        }
    }

    /**
     * Set up button click listeners
     */
    private void setupButtonListeners() {
        if (sendTestButton != null) {
            sendTestButton.setOnClickListener(v -> sendTestMessage());
        }

        if (viewHistoryButton != null) {
            viewHistoryButton.setOnClickListener(v -> showMessageHistory());
        }

        if (platformSettingsButton != null) {
            platformSettingsButton.setOnClickListener(v -> navigateToPlatformSettings());
        }
    }

    /**
     * Update all status displays
     */
    private void updateAllStatuses() {
        updateConnectionStatus();
        updateTodayMessages();
        updateRateLimitStatus();
        updatePlatformStatuses();
    }

    /**
     * Update connection status display
     */
    private void updateConnectionStatus() {
        if (connectionStatusText == null || networkStatusManager == null) return;

        try {
            String emoji = networkStatusManager.getStatusEmoji();
            String connectionType = networkStatusManager.getConnectionType();
            boolean isConnected = networkStatusManager.isConnected();

            String status;
            if (isConnected) {
                status = emoji + " Online (" + connectionType + ")";
            } else {
                status = "🔴 Offline";
            }

            connectionStatusText.setText(status);
        } catch (Exception e) {
            connectionStatusText.setText("❓ Unknown");
        }
    }

    /**
     * Update today's message count
     */
    private void updateTodayMessages() {
        if (todayMessagesText == null || getContext() == null) return;

        try {
            MessageStatsDbHelper statsHelper = new MessageStatsDbHelper(getContext());
            MessageStatsDbHelper.DailyStats todayStats = statsHelper.getTodayStats();
            
            int count = (todayStats != null) ? todayStats.totalCount : 0;
            todayMessagesText.setText(String.valueOf(count));
        } catch (Exception e) {
            todayMessagesText.setText("0");
        }
    }

    /**
     * Update rate limit status
     */
    private void updateRateLimitStatus() {
        if (rateLimitText == null) return;

        try {
            RateLimiter rateLimiter = RateLimiter.getInstance();
            int currentCount = rateLimiter.getCurrentForwardCount();
            rateLimitText.setText(currentCount + "/10");
        } catch (Exception e) {
            rateLimitText.setText("0/10");
        }
    }

    /**
     * Update platform enable/disable statuses
     */
    private void updatePlatformStatuses() {
        if (sharedPreferences == null || getContext() == null) return;

        // SMS Status
        boolean smsEnabled = sharedPreferences.getBoolean(
                getContext().getString(R.string.key_enable_sms), false);
        if (smsStatusText != null) {
            smsStatusText.setText(smsEnabled ? "✅ Enabled" : "❌ Disabled");
        }

        // Telegram Status
        boolean telegramEnabled = sharedPreferences.getBoolean(
                getContext().getString(R.string.key_enable_telegram), false);
        if (telegramStatusText != null) {
            telegramStatusText.setText(telegramEnabled ? "✅ Enabled" : "❌ Disabled");
        }

        // Email Status
        boolean emailEnabled = sharedPreferences.getBoolean(
                getContext().getString(R.string.key_enable_email), false);
        if (emailStatusText != null) {
            emailStatusText.setText(emailEnabled ? "✅ Enabled" : "❌ Disabled");
        }

        // Web Status
        boolean webEnabled = sharedPreferences.getBoolean(
                getContext().getString(R.string.key_enable_web), false);
        if (webStatusText != null) {
            webStatusText.setText(webEnabled ? "✅ Enabled" : "❌ Disabled");
        }
    }

    /**
     * Send test message using the same logic as in MainActivity
     */
    private void sendTestMessage() {
        if (getContext() == null) return;

        try {
            // Check if any forwarder is enabled
            boolean smsEnabled = sharedPreferences.getBoolean(
                    getContext().getString(R.string.key_enable_sms), false);
            boolean telegramEnabled = sharedPreferences.getBoolean(
                    getContext().getString(R.string.key_enable_telegram), false);
            boolean webEnabled = sharedPreferences.getBoolean(
                    getContext().getString(R.string.key_enable_web), false);
            boolean emailEnabled = sharedPreferences.getBoolean(
                    getContext().getString(R.string.key_enable_email), false);

            if (!smsEnabled && !telegramEnabled && !webEnabled && !emailEnabled) {
                Toast.makeText(getContext(), 
                        getContext().getString(R.string.test_message_no_forwarders),
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Create test message data
            String testPhoneNumber = "+1234567890";
            String testMessage = "This is a test message from SMS Forward app. " +
                    "If you receive this, your forwarding setup is working correctly!";
            long currentTime = System.currentTimeMillis();

            // Create forwarders list
            List<Forwarder> forwarders = new ArrayList<>();

            if (smsEnabled) {
                String target = sharedPreferences.getString(
                        getContext().getString(R.string.key_target_sms), "");
                if (!target.isEmpty()) {
                    forwarders.add(new RetryableForwarder(new SmsForwarder(target)));
                }
            }

            if (telegramEnabled) {
                String targetId = sharedPreferences.getString(
                        getContext().getString(R.string.key_target_telegram), "");
                String apiKey = sharedPreferences.getString(
                        getContext().getString(R.string.key_telegram_apikey), "");
                if (!targetId.isEmpty() && !apiKey.isEmpty()) {
                    forwarders.add(new RetryableForwarder(new TelegramForwarder(targetId, apiKey)));
                }
            }

            if (webEnabled) {
                String targetUrl = sharedPreferences.getString(
                        getContext().getString(R.string.key_target_web), "");
                if (!targetUrl.isEmpty()) {
                    forwarders.add(new RetryableForwarder(new JsonWebForwarder(targetUrl)));
                }
            }

            if (emailEnabled) {
                String fromAddress = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_from_address), "");
                String toAddress = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_to_address), "");
                String host = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_submit_host), "");
                String port = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_submit_port), "587");
                String password = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_submit_password), "");
                String usernameStyle = sharedPreferences.getString(
                        getContext().getString(R.string.key_email_username_style), "full");

                if (!fromAddress.isEmpty() && !toAddress.isEmpty() &&
                        !host.isEmpty() && !password.isEmpty()) {
                    try {
                        int portInt = Integer.parseInt(port);

                        jakarta.mail.internet.InternetAddress from = 
                                new jakarta.mail.internet.InternetAddress(fromAddress);
                        jakarta.mail.internet.InternetAddress[] to = {
                                new jakarta.mail.internet.InternetAddress(toAddress)};

                        String username = usernameStyle.equals("full") ? fromAddress
                                : (fromAddress.contains("@") ? 
                                   fromAddress.substring(0, fromAddress.indexOf("@")) : fromAddress);

                        forwarders.add(new RetryableForwarder(new EmailForwarder(from, to, host,
                                (short) portInt, username, password)));
                    } catch (Exception e) {
                        // Skip email forwarder if configuration is invalid
                    }
                }
            }

            if (forwarders.isEmpty()) {
                Toast.makeText(getContext(), 
                        "Please complete the configuration for enabled forwarders",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Send test message through all enabled forwarders
            MessageStatsDbHelper statsHelper = new MessageStatsDbHelper(getContext());
            int successCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (Forwarder forwarder : forwarders) {
                try {
                    forwarder.forward(testPhoneNumber, testMessage, currentTime);
                    successCount++;

                    String forwarderName = (forwarder instanceof RetryableForwarder)
                            ? ((RetryableForwarder) forwarder).getDelegateName()
                            : forwarder.getClass().getSimpleName();
                    statsHelper.recordForwardSuccess(forwarderName);

                } catch (Exception e) {
                    String forwarderName = (forwarder instanceof RetryableForwarder)
                            ? ((RetryableForwarder) forwarder).getDelegateName()
                            : forwarder.getClass().getSimpleName();
                    errorMessages.append(forwarderName)
                            .append(": ").append(e.getMessage()).append("\n");

                    statsHelper.recordForwardFailure(forwarderName);
                }
            }

            // Cleanup RetryableForwarders
            for (Forwarder forwarder : forwarders) {
                if (forwarder instanceof RetryableForwarder) {
                    ((RetryableForwarder) forwarder).shutdown();
                }
            }

            // Show result and update UI
            if (successCount > 0) {
                String message = getContext().getString(R.string.test_message_sent) +
                        " (" + successCount + "/" + forwarders.size() + " forwarders)";
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                // Update today's message count
                updateTodayMessages();
            } else {
                String errorMsg = String.format(
                        getContext().getString(R.string.test_message_error),
                        errorMessages.toString());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), 
                    "Error sending test message: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show message history dialog
     */
    private void showMessageHistory() {
        if (getContext() == null || historyDbHelper == null) return;

        try {
            List<MessageHistoryDbHelper.HistoryRecord> history = 
                    historyDbHelper.getMessageHistory(20);

            if (history.isEmpty()) {
                Toast.makeText(getContext(), 
                        getContext().getString(R.string.history_empty), 
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Build message history display
            StringBuilder historyText = new StringBuilder();
            MessageHistoryDbHelper.HistoryStats stats = historyDbHelper.getHistoryStats();

            // Add header with statistics
            historyText.append("📊 Recent Messages (Last 20):\n");
            historyText.append(String.format("Total: %d | Success: %.1f%% | Failed: %d\n\n",
                    stats.totalCount, stats.getSuccessRate(), stats.failedCount));

            // Add recent messages
            for (MessageHistoryDbHelper.HistoryRecord record : history) {
                historyText.append(record.getStatusEmoji()).append(" ")
                        .append(record.getPlatformEmoji()).append(" ")
                        .append(record.platform.toUpperCase()).append("\n");

                historyText.append("From: ").append(record.fromNumber).append("\n");

                // Truncate long messages for display
                String displayMessage = record.messageContent;
                if (displayMessage.length() > 100) {
                    displayMessage = displayMessage.substring(0, 100) + "...";
                }
                historyText.append("Message: ").append(displayMessage).append("\n");
                historyText.append("Time: ").append(record.getFormattedForwardTimestamp()).append("\n\n");
            }

            // Show in dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Message History")
                    .setMessage(historyText.toString())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(getContext(), 
                    "Error loading message history: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Navigate to platform settings (will be implemented when navigation is set up)
     */
    private void navigateToPlatformSettings() {
        // TODO: Implement navigation to Platforms fragment when MainActivity navigation is ready
        Toast.makeText(getContext(), "Platform settings navigation will be implemented with MainActivity integration", 
                Toast.LENGTH_SHORT).show();
    }
}