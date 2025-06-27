package com.keremgok.smsforward;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.initializeTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, 0);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements NetworkStatusManager.NetworkStatusListener {

        private NetworkStatusManager networkStatusManager;
        private Preference connectionStatusPreference;
        private ThemeManager themeManager;
        private SettingsBackupManager backupManager;
        
        // Activity result launchers for file operations
        private ActivityResultLauncher<Intent> exportLauncher;
        private ActivityResultLauncher<Intent> importLauncher;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Initialize network status manager
            networkStatusManager = NetworkStatusManager.getInstance(getContext());
            
            // Initialize theme manager
            themeManager = new ThemeManager(getContext());
            
            // Initialize backup manager
            backupManager = new SettingsBackupManager(getContext());
            
            // Initialize file operation launchers
            initializeFileLaunchers();

            // Set up test message button
            Preference testMessagePreference = findPreference(getString(R.string.key_test_message));
            if (testMessagePreference != null) {
                testMessagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        sendTestMessage();
                        return true;
                    }
                });
            }

            // Set up queue status display
            Preference queueStatusPreference = findPreference(getString(R.string.key_queue_status));
            if (queueStatusPreference != null) {
                queueStatusPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showQueueStatus();
                        return true;
                    }
                });

                // Update queue status summary
                updateQueueStatusSummary(queueStatusPreference);
            }

            // Set up connection status display
            connectionStatusPreference = findPreference(getString(R.string.key_connection_status));
            if (connectionStatusPreference != null) {
                connectionStatusPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showConnectionStatus();
                        return true;
                    }
                });

                // Update connection status summary
                updateConnectionStatusSummary();
            }

            // Set up message counter display
            Preference messageCounterPreference = findPreference(getString(R.string.key_message_counter));
            if (messageCounterPreference != null) {
                messageCounterPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showMessageCounter();
                        return true;
                    }
                });

                // Update message counter summary
                updateMessageCounterSummary(messageCounterPreference);
            }

            // Set up rate limit status display
            Preference rateLimitStatusPreference = findPreference(getString(R.string.key_rate_limit_status));
            if (rateLimitStatusPreference != null) {
                rateLimitStatusPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showRateLimitStatus();
                        return true;
                    }
                });

                // Update rate limit status summary
                updateRateLimitStatusSummary(rateLimitStatusPreference);
            }

            // Set up theme preference listener
            androidx.preference.ListPreference themePreference = findPreference(getString(R.string.key_theme_mode));
            if (themePreference != null) {
                // Set initial summary
                updateThemeSummary(themePreference);
                
                themePreference.setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(androidx.preference.Preference preference, Object newValue) {
                        String newTheme = (String) newValue;
                        
                        // Apply the new theme
                        themeManager.setThemeMode(newTheme);
                        
                        // Update summary
                        updateThemeSummary((androidx.preference.ListPreference) preference);
                        
                        // Recreate activity to apply theme immediately
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                        
                        return true;
                    }
                });
            }

            // Set up export settings
            Preference exportSettingsPreference = findPreference(getString(R.string.key_export_settings));
            if (exportSettingsPreference != null) {
                exportSettingsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        exportSettings();
                        return true;
                    }
                });
            }

            // Set up import settings
            Preference importSettingsPreference = findPreference(getString(R.string.key_import_settings));
            if (importSettingsPreference != null) {
                importSettingsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        importSettings();
                        return true;
                    }
                });
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (networkStatusManager != null) {
                networkStatusManager.startMonitoring();
                networkStatusManager.addListener(this);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (networkStatusManager != null) {
                networkStatusManager.removeListener(this);
            }
        }

        @Override
        public void onNetworkStatusChanged(boolean isConnected, String connectionType) {
            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateConnectionStatusSummary();
                });
            }
        }

        private void sendTestMessage() {
            try {
                // Get preferences
                android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

                // Check if any forwarder is enabled
                boolean smsEnabled = prefs.getBoolean(getString(R.string.key_enable_sms), false);
                boolean telegramEnabled = prefs.getBoolean(getString(R.string.key_enable_telegram), false);
                boolean webEnabled = prefs.getBoolean(getString(R.string.key_enable_web), false);
                boolean emailEnabled = prefs.getBoolean(getString(R.string.key_enable_email), false);

                if (!smsEnabled && !telegramEnabled && !webEnabled && !emailEnabled) {
                    Toast.makeText(getContext(), getString(R.string.test_message_no_forwarders),
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
                    String target = prefs.getString(getString(R.string.key_target_sms), "");
                    if (!target.isEmpty()) {
                        forwarders.add(new RetryableForwarder(new SmsForwarder(target)));
                    }
                }

                if (telegramEnabled) {
                    String targetId = prefs.getString(getString(R.string.key_target_telegram), "");
                    String apiKey = prefs.getString(getString(R.string.key_telegram_apikey), "");
                    if (!targetId.isEmpty() && !apiKey.isEmpty()) {
                        forwarders.add(new RetryableForwarder(new TelegramForwarder(targetId, apiKey)));
                    }
                }

                if (webEnabled) {
                    String targetUrl = prefs.getString(getString(R.string.key_target_web), "");
                    if (!targetUrl.isEmpty()) {
                        forwarders.add(new RetryableForwarder(new JsonWebForwarder(targetUrl)));
                    }
                }

                if (emailEnabled) {
                    String fromAddress = prefs.getString(getString(R.string.key_email_from_address), "");
                    String toAddress = prefs.getString(getString(R.string.key_email_to_address), "");
                    String host = prefs.getString(getString(R.string.key_email_submit_host), "");
                    String port = prefs.getString(getString(R.string.key_email_submit_port), "587");
                    String password = prefs.getString(getString(R.string.key_email_submit_password), "");
                    String usernameStyle = prefs.getString(getString(R.string.key_email_username_style), "full");

                    if (!fromAddress.isEmpty() && !toAddress.isEmpty() &&
                            !host.isEmpty() && !password.isEmpty()) {
                        try {
                            int portInt = Integer.parseInt(port);

                            // Create InternetAddress objects as required by EmailForwarder constructor
                            jakarta.mail.internet.InternetAddress from = new jakarta.mail.internet.InternetAddress(
                                    fromAddress);
                            jakarta.mail.internet.InternetAddress[] to = {
                                    new jakarta.mail.internet.InternetAddress(toAddress) };

                            // Determine username based on style
                            String username = usernameStyle.equals("full") ? fromAddress
                                    : (fromAddress.contains("@") ? fromAddress.substring(0, fromAddress.indexOf("@"))
                                            : fromAddress);

                            forwarders.add(new RetryableForwarder(new EmailForwarder(from, to, host,
                                    (short) portInt, username, password)));
                        } catch (NumberFormatException e) {
                            // Skip email forwarder if port is invalid
                        } catch (jakarta.mail.internet.AddressException e) {
                            // Skip email forwarder if address format is invalid
                        }
                    }
                }

                if (forwarders.isEmpty()) {
                    Toast.makeText(getContext(), "Please complete the configuration for enabled forwarders",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Initialize stats helper for test messages
                MessageStatsDbHelper statsHelper = new MessageStatsDbHelper(getContext());

                // Send test message through all enabled forwarders
                int successCount = 0;
                StringBuilder errorMessages = new StringBuilder();

                for (Forwarder forwarder : forwarders) {
                    try {
                        forwarder.forward(testPhoneNumber, testMessage, currentTime);
                        successCount++;
                        
                        // Record test success in stats (use actual forwarder name)
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
                                
                        // Record test failure in stats
                        statsHelper.recordForwardFailure(forwarderName);
                    }
                }

                // Show result
                if (successCount > 0) {
                    String message = getString(R.string.test_message_sent) +
                            " (" + successCount + "/" + forwarders.size() + " forwarders)";
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                    if (errorMessages.length() > 0) {
                        Toast.makeText(getContext(), "Some errors occurred:\n" + errorMessages.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = String.format(getString(R.string.test_message_error),
                            errorMessages.toString());
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                String errorMsg = String.format(getString(R.string.test_message_error), e.getMessage());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        }

        private void showQueueStatus() {
            try {
                MessageQueueDbHelper dbHelper = new MessageQueueDbHelper(getContext());
                MessageQueueDbHelper.QueueStats stats = dbHelper.getQueueStats();

                String message;
                if (stats.totalCount == 0) {
                    message = getString(R.string.queue_empty);
                } else {
                    message = String.format(getString(R.string.queue_stats_format),
                            stats.totalCount, stats.pendingCount, stats.failedCount);

                    if (stats.oldestPendingAge > 0) {
                        long hours = stats.oldestPendingAge / (1000 * 60 * 60);
                        long minutes = (stats.oldestPendingAge / (1000 * 60)) % 60;
                        message += String.format("\nOldest pending: %dh %dm ago", hours, minutes);
                    }
                }

                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                // Update the preference summary
                Preference queueStatusPreference = findPreference(getString(R.string.key_queue_status));
                if (queueStatusPreference != null) {
                    updateQueueStatusSummary(queueStatusPreference);
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error reading queue status: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        private void updateQueueStatusSummary(Preference preference) {
            try {
                MessageQueueDbHelper dbHelper = new MessageQueueDbHelper(getContext());
                MessageQueueDbHelper.QueueStats stats = dbHelper.getQueueStats();

                String summary;
                if (stats.totalCount == 0) {
                    summary = getString(R.string.queue_empty);
                } else {
                    summary = String.format(getString(R.string.queue_stats_format),
                            stats.totalCount, stats.pendingCount, stats.failedCount);
                }

                preference.setSummary(summary);

            } catch (Exception e) {
                preference.setSummary("Error reading queue status");
            }
        }

        private void showConnectionStatus() {
            if (networkStatusManager == null) {
                Toast.makeText(getContext(), "Network status manager not available",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            networkStatusManager.updateNetworkStatus();

            StringBuilder message = new StringBuilder();
            message.append("Status: ").append(networkStatusManager.getConnectionStatus()).append("\n");
            message.append("Quality: ").append(networkStatusManager.getNetworkQuality()).append("\n");

            if (networkStatusManager.canForwardMessages()) {
                message.append(getString(R.string.connection_can_forward));
            } else {
                message.append(getString(R.string.connection_cannot_forward));
            }

            Toast.makeText(getContext(), message.toString(), Toast.LENGTH_LONG).show();

            // Update the preference summary
            updateConnectionStatusSummary();
        }

        private void updateConnectionStatusSummary() {
            if (connectionStatusPreference == null || networkStatusManager == null) {
                return;
            }

            try {
                String emoji = networkStatusManager.getStatusEmoji();
                String connectionType = networkStatusManager.getConnectionType();
                boolean isConnected = networkStatusManager.isConnected();

                String summary;
                if (isConnected) {
                    summary = String.format(getString(R.string.connection_online), emoji, connectionType);
                } else {
                    summary = getString(R.string.connection_offline);
                }

                connectionStatusPreference.setSummary(summary);

            } catch (Exception e) {
                connectionStatusPreference.setSummary("Error reading connection status");
            }
        }

        private void showMessageCounter() {
            try {
                MessageStatsDbHelper statsHelper = new MessageStatsDbHelper(getContext());
                
                // Get today's stats
                MessageStatsDbHelper.DailyStats todayStats = statsHelper.getTodayStats();
                
                // Get total stats
                MessageStatsDbHelper.TotalStats totalStats = statsHelper.getTotalStats();
                
                StringBuilder message = new StringBuilder();
                
                // Today's statistics
                message.append("📊 Today's Messages:\n");
                if (todayStats != null && todayStats.totalCount > 0) {
                    message.append(String.format("  Total: %d\n", todayStats.totalCount));
                    message.append(String.format("  Success: %d (%.1f%%)\n", 
                            todayStats.successCount, todayStats.getSuccessRate()));
                    message.append(String.format("  Failed: %d\n", todayStats.failedCount));
                    
                    if (todayStats.smsCount > 0) message.append(String.format("  📱 SMS: %d\n", todayStats.smsCount));
                    if (todayStats.telegramCount > 0) message.append(String.format("  📢 Telegram: %d\n", todayStats.telegramCount));
                    if (todayStats.emailCount > 0) message.append(String.format("  📧 Email: %d\n", todayStats.emailCount));
                    if (todayStats.webCount > 0) message.append(String.format("  🌐 Web API: %d\n", todayStats.webCount));
                } else {
                    message.append("  No messages forwarded today\n");
                }
                
                message.append("\n");
                
                // Total statistics
                message.append("📈 All Time:\n");
                if (totalStats.totalCount > 0) {
                    message.append(String.format("  Total: %d\n", totalStats.totalCount));
                    message.append(String.format("  Success: %d (%.1f%%)\n", 
                            totalStats.successCount, totalStats.getSuccessRate()));
                    message.append(String.format("  Failed: %d\n", totalStats.failedCount));
                    message.append(String.format("  Active Days: %d\n", totalStats.activeDays));
                    message.append(String.format("  Daily Avg: %.1f\n", totalStats.getAveragePerDay()));
                    
                    message.append("\n  Platform Breakdown:\n");
                    if (totalStats.smsCount > 0) message.append(String.format("  📱 SMS: %d\n", totalStats.smsCount));
                    if (totalStats.telegramCount > 0) message.append(String.format("  📢 Telegram: %d\n", totalStats.telegramCount));
                    if (totalStats.emailCount > 0) message.append(String.format("  📧 Email: %d\n", totalStats.emailCount));
                    if (totalStats.webCount > 0) message.append(String.format("  🌐 Web API: %d\n", totalStats.webCount));
                } else {
                    message.append("  No messages forwarded yet\n");
                }
                
                Toast.makeText(getContext(), message.toString(), Toast.LENGTH_LONG).show();
                
                // Update the preference summary
                Preference messageCounterPreference = findPreference(getString(R.string.key_message_counter));
                if (messageCounterPreference != null) {
                    updateMessageCounterSummary(messageCounterPreference);
                }
                
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error reading message statistics: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        private void updateMessageCounterSummary(Preference preference) {
            try {
                MessageStatsDbHelper statsHelper = new MessageStatsDbHelper(getContext());
                MessageStatsDbHelper.DailyStats todayStats = statsHelper.getTodayStats();
                MessageStatsDbHelper.TotalStats totalStats = statsHelper.getTotalStats();
                
                String summary;
                if (todayStats != null && todayStats.totalCount > 0) {
                    summary = String.format("Today: %d | Total: %d (%.1f%% success)", 
                            todayStats.totalCount, totalStats.totalCount, totalStats.getSuccessRate());
                } else if (totalStats.totalCount > 0) {
                    summary = String.format("Today: 0 | Total: %d (%.1f%% success)", 
                            totalStats.totalCount, totalStats.getSuccessRate());
                } else {
                    summary = "No messages forwarded yet";
                }
                
                preference.setSummary(summary);
                
            } catch (Exception e) {
                preference.setSummary("Error reading statistics");
            }
        }

        private void updateThemeSummary(androidx.preference.ListPreference preference) {
            if (themeManager == null) {
                return;
            }
            
            try {
                String description = themeManager.getCurrentThemeDescription();
                preference.setSummary(description);
            } catch (Exception e) {
                preference.setSummary("Error reading theme setting");
            }
        }

        private void showRateLimitStatus() {
            try {
                RateLimiter rateLimiter = RateLimiter.getInstance();
                
                int currentCount = rateLimiter.getCurrentForwardCount();
                long timeUntilNext = rateLimiter.getTimeUntilNextSlot();
                
                StringBuilder message = new StringBuilder();
                message.append("🚦 Rate Limiting Status:\n\n");
                message.append(String.format("Current usage: %d/10 SMS per minute\n", currentCount));
                
                if (timeUntilNext > 0) {
                    long seconds = timeUntilNext / 1000;
                    message.append(String.format("Next slot available in: %d seconds\n", seconds));
                } else {
                    message.append("✅ Slots available immediately\n");
                }
                
                if (currentCount >= 10) {
                    message.append("\n⚠️ Rate limit reached! SMS forwarding temporarily blocked.");
                } else if (currentCount >= 7) {
                    message.append("\n⚠️ Approaching rate limit. Be careful not to exceed 10 SMS per minute.");
                } else {
                    message.append("\n✅ Within safe limits for SMS forwarding.");
                }
                
                // Check if rate limiting is enabled
                android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean rateLimitEnabled = prefs.getBoolean(getString(R.string.key_enable_rate_limiting), true);
                
                if (!rateLimitEnabled) {
                    message.append("\n\n⚠️ Rate limiting is currently DISABLED in settings.");
                }
                
                Toast.makeText(getContext(), message.toString(), Toast.LENGTH_LONG).show();
                
                // Update the preference summary
                Preference rateLimitStatusPreference = findPreference(getString(R.string.key_rate_limit_status));
                if (rateLimitStatusPreference != null) {
                    updateRateLimitStatusSummary(rateLimitStatusPreference);
                }
                
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error reading rate limit status: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        private void updateRateLimitStatusSummary(Preference preference) {
            try {
                RateLimiter rateLimiter = RateLimiter.getInstance();
                
                int currentCount = rateLimiter.getCurrentForwardCount();
                long timeUntilNext = rateLimiter.getTimeUntilNextSlot();
                
                String nextSlotText;
                if (timeUntilNext > 0) {
                    long seconds = timeUntilNext / 1000;
                    nextSlotText = String.format(getString(R.string.rate_limit_seconds), seconds);
                } else {
                    nextSlotText = getString(R.string.rate_limit_available_now);
                }
                
                String summary = String.format(getString(R.string.rate_limit_status_format), 
                        currentCount, nextSlotText);
                        
                preference.setSummary(summary);
                
            } catch (Exception e) {
                preference.setSummary("Error reading rate limit status");
            }
        }

        /**
         * Initialize file operation launchers for export/import
         */
        private void initializeFileLaunchers() {
            // Export launcher - creates a new file
            exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri);
                        }
                    }
                }
            );

            // Import launcher - opens an existing file
            importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                }
            );
        }

        /**
         * Start the export settings process
         */
        private void exportSettings() {
            try {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, backupManager.generateBackupFilename());
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
                
                exportLauncher.launch(intent);
                
            } catch (Exception e) {
                Toast.makeText(getContext(), 
                    String.format(getString(R.string.export_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Start the import settings process
         */
        private void importSettings() {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
                
                importLauncher.launch(intent);
                
            } catch (Exception e) {
                Toast.makeText(getContext(), 
                    String.format(getString(R.string.import_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Perform the actual export to the selected file
         */
        private void performExport(Uri uri) {
            try {
                backupManager.exportToFile(uri);
                Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                Toast.makeText(getContext(), 
                    String.format(getString(R.string.export_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Perform the actual import from the selected file
         */
        private void performImport(Uri uri) {
            try {
                SettingsBackupManager.ImportResult result = backupManager.importFromFile(uri);
                
                if (result.success) {
                    Toast.makeText(getContext(), 
                        String.format(getString(R.string.import_success), result.message),
                        Toast.LENGTH_LONG).show();
                    
                    // Refresh all preference summaries to reflect imported values
                    refreshPreferenceSummaries();
                    
                    // If theme was changed, recreate activity
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                } else {
                    Toast.makeText(getContext(), 
                        String.format(getString(R.string.import_error), result.message),
                        Toast.LENGTH_LONG).show();
                }
                
            } catch (Exception e) {
                Toast.makeText(getContext(), 
                    String.format(getString(R.string.import_error), e.getMessage()),
                    Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Refresh all preference summaries after import
         */
        private void refreshPreferenceSummaries() {
            try {
                // Update theme summary
                androidx.preference.ListPreference themePreference = findPreference(getString(R.string.key_theme_mode));
                if (themePreference != null) {
                    updateThemeSummary(themePreference);
                }
                
                // Update message counter summary
                Preference messageCounterPreference = findPreference(getString(R.string.key_message_counter));
                if (messageCounterPreference != null) {
                    updateMessageCounterSummary(messageCounterPreference);
                }
                
                // Update queue status summary
                Preference queueStatusPreference = findPreference(getString(R.string.key_queue_status));
                if (queueStatusPreference != null) {
                    updateQueueStatusSummary(queueStatusPreference);
                }
                
                // Update rate limit status summary
                Preference rateLimitStatusPreference = findPreference(getString(R.string.key_rate_limit_status));
                if (rateLimitStatusPreference != null) {
                    updateRateLimitStatusSummary(rateLimitStatusPreference);
                }
                
                // Update connection status summary
                updateConnectionStatusSummary();
                
            } catch (Exception e) {
                // Ignore errors during summary refresh
            }
        }
    }
}