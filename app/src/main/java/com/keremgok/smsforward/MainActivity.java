package com.keremgok.smsforward;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SecurityManager securityManager;
    private static final int REQUEST_CODE_AUTHENTICATION = 1001;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        
        // Initialize security manager
        securityManager = new SecurityManager(this);
        
        // Check if authentication is required
        if (securityManager.needsAuthentication()) {
            // Start authentication activity
            Intent authIntent = AuthenticationActivity.createIntent(this, AuthenticationActivity.AUTH_TYPE_STARTUP);
            startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
            return; // Don't continue with normal initialization until authenticated
        }
        
        // Continue with normal initialization
        initializeMainActivity();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_AUTHENTICATION) {
            if (resultCode == RESULT_OK) {
                // Authentication successful, continue with initialization
                initializeMainActivity();
            } else {
                // Authentication failed or cancelled, close the app
                finishAffinity();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check authentication when app comes back to foreground
        if (securityManager != null && securityManager.needsAuthentication()) {
            Intent authIntent = AuthenticationActivity.createIntent(this, AuthenticationActivity.AUTH_TYPE_STARTUP);
            startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
        }
    }
    
    private void initializeMainActivity() {
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, 0);

        if (getSupportFragmentManager().findFragmentById(R.id.settings) == null) {
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
        private LanguageManager languageManager;
        private SettingsBackupManager backupManager;
        private MessageHistoryDbHelper historyDbHelper;
        private SecurityManager securityManager;

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

            // Initialize language manager
            languageManager = new LanguageManager(getContext());

            // Initialize backup manager
            backupManager = new SettingsBackupManager(getContext());

            // Initialize message history helper
            historyDbHelper = new MessageHistoryDbHelper(getContext());
            
            // Initialize security manager
            securityManager = new SecurityManager(getContext());

            // Initialize file operation launchers
            initializeFileLaunchers();

            // Set up security preferences
            setupSecurityPreferences();

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

            // Set up language preference listener
            androidx.preference.ListPreference languagePreference = findPreference(getString(R.string.key_language));
            if (languagePreference != null) {
                // Set initial summary
                updateLanguageSummary(languagePreference);

                languagePreference
                        .setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(androidx.preference.Preference preference,
                                    Object newValue) {
                                String newLanguage = (String) newValue;

                                // Save the new language setting
                                languageManager.setLanguage(newLanguage);

                                // Show restart dialog
                                showLanguageRestartDialog();

                                return true;
                            }
                        });
            }

            // Set up theme preference listener
            androidx.preference.ListPreference themePreference = findPreference(getString(R.string.key_theme_mode));
            if (themePreference != null) {
                // Set initial summary
                updateThemeSummary(themePreference);

                themePreference
                        .setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(androidx.preference.Preference preference,
                                    Object newValue) {
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

            // Set up content filter preference listener
            androidx.preference.EditTextPreference filterKeywordsPreference = findPreference(getString(R.string.key_filter_keywords));
            if (filterKeywordsPreference != null) {
                // Set initial summary
                updateFilterKeywordsSummary(filterKeywordsPreference);

                filterKeywordsPreference
                        .setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(androidx.preference.Preference preference,
                                    Object newValue) {
                                String newKeywords = (String) newValue;

                                // Clean and validate keywords
                                String cleanedKeywords = SmsContentFilter.cleanFilterKeywords(newKeywords);

                                // Update the actual preference value with cleaned keywords
                                ((androidx.preference.EditTextPreference) preference).setText(cleanedKeywords);

                                // Update summary to show active filters
                                updateFilterKeywordsSummary((androidx.preference.EditTextPreference) preference);

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

            // Set up message history
            Preference messageHistoryPreference = findPreference(getString(R.string.key_message_history));
            if (messageHistoryPreference != null) {
                messageHistoryPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showMessageHistory();
                        return true;
                    }
                });

                // Update message history summary
                updateMessageHistorySummary(messageHistoryPreference);
            }

            // Set up clear history
            Preference clearHistoryPreference = findPreference(getString(R.string.key_clear_history));
            if (clearHistoryPreference != null) {
                clearHistoryPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showClearHistoryConfirmation();
                        return true;
                    }
                });
            }

            // Set up about
            Preference aboutPreference = findPreference(getString(R.string.key_about));
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showAboutDialog();
                        return true;
                    }
                });
            }

            // Set up number whitelist preference listener
            setupWhitelistPreferences();
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
        public void onDestroy() {
            super.onDestroy();
            // Cleanup to prevent memory leaks
            if (networkStatusManager != null) {
                networkStatusManager.removeListener(this);
                networkStatusManager.stopMonitoring();
            }

            // Close database helpers to free resources
            if (historyDbHelper != null) {
                historyDbHelper.close();
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
                List<RetryableForwarder> retryableForwarders = new ArrayList<>();

                for (Forwarder forwarder : forwarders) {
                    try {
                        forwarder.forward(testPhoneNumber, testMessage, currentTime);
                        successCount++;

                        // Record test success in stats (use actual forwarder name)
                        String forwarderName = (forwarder instanceof RetryableForwarder)
                                ? ((RetryableForwarder) forwarder).getDelegateName()
                                : forwarder.getClass().getSimpleName();
                        statsHelper.recordForwardSuccess(forwarderName);

                        // Collect RetryableForwarders for later cleanup
                        if (forwarder instanceof RetryableForwarder) {
                            retryableForwarders.add((RetryableForwarder) forwarder);
                        }

                    } catch (Exception e) {
                        String forwarderName = (forwarder instanceof RetryableForwarder)
                                ? ((RetryableForwarder) forwarder).getDelegateName()
                                : forwarder.getClass().getSimpleName();
                        errorMessages.append(forwarderName)
                                .append(": ").append(e.getMessage()).append("\n");

                        // Record test failure in stats
                        statsHelper.recordForwardFailure(forwarderName);

                        // Collect RetryableForwarders for later cleanup
                        if (forwarder instanceof RetryableForwarder) {
                            retryableForwarders.add((RetryableForwarder) forwarder);
                        }
                    }
                }

                // Cleanup RetryableForwarders to prevent memory leaks
                for (RetryableForwarder retryableForwarder : retryableForwarders) {
                    retryableForwarder.shutdown();
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
            MessageQueueDbHelper dbHelper = null;
            try {
                dbHelper = new MessageQueueDbHelper(getContext());
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
            } finally {
                // Close database helper to prevent memory leaks
                if (dbHelper != null) {
                    dbHelper.close();
                }
            }
        }

        private void updateQueueStatusSummary(Preference preference) {
            MessageQueueDbHelper dbHelper = null;
            try {
                dbHelper = new MessageQueueDbHelper(getContext());
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
            } finally {
                // Close database helper to prevent memory leaks
                if (dbHelper != null) {
                    dbHelper.close();
                }
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

                    if (todayStats.smsCount > 0)
                        message.append(String.format("  📱 SMS: %d\n", todayStats.smsCount));
                    if (todayStats.telegramCount > 0)
                        message.append(String.format("  📢 Telegram: %d\n", todayStats.telegramCount));
                    if (todayStats.emailCount > 0)
                        message.append(String.format("  📧 Email: %d\n", todayStats.emailCount));
                    if (todayStats.webCount > 0)
                        message.append(String.format("  🌐 Web API: %d\n", todayStats.webCount));
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
                    if (totalStats.smsCount > 0)
                        message.append(String.format("  📱 SMS: %d\n", totalStats.smsCount));
                    if (totalStats.telegramCount > 0)
                        message.append(String.format("  📢 Telegram: %d\n", totalStats.telegramCount));
                    if (totalStats.emailCount > 0)
                        message.append(String.format("  📧 Email: %d\n", totalStats.emailCount));
                    if (totalStats.webCount > 0)
                        message.append(String.format("  🌐 Web API: %d\n", totalStats.webCount));
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

        private void updateLanguageSummary(androidx.preference.ListPreference preference) {
            if (languageManager == null) {
                return;
            }

            try {
                String currentLanguage = languageManager.getSelectedLanguage();
                String[] languageEntries = getResources().getStringArray(R.array.language_entries);
                String[] languageValues = getResources().getStringArray(R.array.language_values);

                for (int i = 0; i < languageValues.length; i++) {
                    if (languageValues[i].equals(currentLanguage)) {
                        preference.setSummary(languageEntries[i]);
                        break;
                    }
                }
            } catch (Exception e) {
                preference.setSummary("Error reading language setting");
            }
        }

        private void showLanguageRestartDialog() {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.language_restart_title)
                    .setMessage(R.string.language_restart_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Recreate activity to apply language change
                            if (getActivity() != null) {
                                getActivity().recreate();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
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

        private void updateFilterKeywordsSummary(androidx.preference.EditTextPreference preference) {
            try {
                String keywords = preference.getText();
                String summary = SmsContentFilter.getFilterSummary(keywords);
                
                if (summary != null) {
                    preference.setSummary(String.format(getString(R.string.filter_active_summary), summary));
                } else {
                    preference.setSummary(getString(R.string.filter_inactive_summary));
                }
            } catch (Exception e) {
                preference.setSummary("Error reading filter settings");
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
                    });

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
                    });
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
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "application/json", "text/plain" });

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
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "application/json", "text/plain" });

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

                // Update content filter summary
                androidx.preference.EditTextPreference filterKeywordsPreference = findPreference(getString(R.string.key_filter_keywords));
                if (filterKeywordsPreference != null) {
                    updateFilterKeywordsSummary(filterKeywordsPreference);
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

                // Update message history summary
                Preference messageHistoryPreference = findPreference(getString(R.string.key_message_history));
                if (messageHistoryPreference != null) {
                    updateMessageHistorySummary(messageHistoryPreference);
                }

                // Update number whitelist summary
                SwitchPreferenceCompat enableWhitelistPref = findPreference(getString(R.string.key_enable_number_whitelist));
                EditTextPreference whitelistPref = findPreference(getString(R.string.key_number_whitelist));
                if (enableWhitelistPref != null && whitelistPref != null) {
                    updateWhitelistSummary(whitelistPref, enableWhitelistPref.isChecked(), whitelistPref.getText());
                }

            } catch (Exception e) {
                // Ignore errors during summary refresh
            }
        }

        /**
         * Show message history dialog
         */
        private void showMessageHistory() {
            try {
                List<MessageHistoryDbHelper.HistoryRecord> history = historyDbHelper.getMessageHistory(100);

                if (history.isEmpty()) {
                    Toast.makeText(getContext(), getString(R.string.history_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Build message history display
                StringBuilder historyText = new StringBuilder();
                MessageHistoryDbHelper.HistoryStats stats = historyDbHelper.getHistoryStats();

                // Add header with statistics
                historyText.append("📊 History Statistics:\n");
                historyText.append(String.format(getString(R.string.history_stats_format),
                        stats.totalCount, stats.successCount, stats.getSuccessRate(), stats.failedCount));
                historyText.append("\n");
                historyText.append("📅 Time span: ").append(stats.getTimeSpanDescription()).append("\n\n");

                // Add recent messages (show last 20 for better readability)
                historyText.append("📋 Recent Messages (Last 20):\n\n");

                int displayCount = Math.min(history.size(), 20);
                for (int i = 0; i < displayCount; i++) {
                    MessageHistoryDbHelper.HistoryRecord record = history.get(i);

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

                    historyText.append("Time: ").append(record.getFormattedForwardTimestamp()).append("\n");

                    if (record.isFailed() && record.errorMessage != null) {
                        historyText.append("Error: ").append(record.errorMessage).append("\n");
                    }

                    historyText.append("\n");
                }

                if (history.size() > 20) {
                    historyText.append("... and ").append(history.size() - 20).append(" more messages\n");
                }

                // Show in dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Message History")
                        .setMessage(historyText.toString())
                        .setPositiveButton("OK", null)
                        .setNeutralButton("View All", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showFullMessageHistory();
                            }
                        })
                        .show();

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error loading message history: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Show full message history in a separate dialog
         */
        private void showFullMessageHistory() {
            try {
                List<MessageHistoryDbHelper.HistoryRecord> history = historyDbHelper.getMessageHistory(100);

                StringBuilder fullHistoryText = new StringBuilder();
                fullHistoryText.append("📋 Complete Message History (Last 100):\n\n");

                for (MessageHistoryDbHelper.HistoryRecord record : history) {
                    fullHistoryText.append(record.getStatusEmoji()).append(" ")
                            .append(record.getPlatformEmoji()).append(" ")
                            .append(record.platform.toUpperCase()).append(" - ")
                            .append(record.getFormattedForwardTimestamp()).append("\n");

                    fullHistoryText.append("From: ").append(record.fromNumber).append("\n");
                    fullHistoryText.append("Content: ").append(record.messageContent).append("\n");

                    if (record.isFailed() && record.errorMessage != null) {
                        fullHistoryText.append("Error: ").append(record.errorMessage).append("\n");
                    }

                    fullHistoryText.append("────────────────────\n");
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Complete Message History")
                        .setMessage(fullHistoryText.toString())
                        .setPositiveButton("OK", null)
                        .show();

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error loading full history: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Show confirmation dialog before clearing history
         */
        private void showClearHistoryConfirmation() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Clear Message History")
                    .setMessage(getString(R.string.clear_history_confirmation))
                    .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clearMessageHistory();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        /**
         * Clear all message history
         */
        private void clearMessageHistory() {
            try {
                historyDbHelper.clearHistory();
                Toast.makeText(getContext(), getString(R.string.history_cleared), Toast.LENGTH_SHORT).show();

                // Update summary after clearing
                Preference messageHistoryPreference = findPreference(getString(R.string.key_message_history));
                if (messageHistoryPreference != null) {
                    updateMessageHistorySummary(messageHistoryPreference);
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error clearing history: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Update message history preference summary
         */
        private void updateMessageHistorySummary(Preference preference) {
            try {
                MessageHistoryDbHelper.HistoryStats stats = historyDbHelper.getHistoryStats();

                String summary;
                if (stats.totalCount > 0) {
                    summary = String.format("Last %d messages | Success: %.1f%% | %s",
                            stats.totalCount, stats.getSuccessRate(), stats.getTimeSpanDescription());
                } else {
                    summary = "No message history available";
                }

                preference.setSummary(summary);

            } catch (Exception e) {
                preference.setSummary("Error reading message history");
            }
        }

        /**
         * Show about dialog with app information
         */
        private void showAboutDialog() {
            try {
                // Get app version info
                android.content.pm.PackageManager pm = getContext().getPackageManager();
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(getContext().getPackageName(), 0);
                String versionName = packageInfo.versionName;
                int versionCode = packageInfo.versionCode;

                // Build about content
                StringBuilder aboutContent = new StringBuilder();

                // App name and version
                aboutContent.append("📱 ").append(getString(R.string.about_app_name)).append("\n");
                aboutContent.append(getString(R.string.about_version_title)).append(": ");
                aboutContent.append(String.format(getString(R.string.about_version_format), versionName, versionCode));
                aboutContent.append("\n\n");

                // Description
                aboutContent.append("📄 ").append(getString(R.string.about_description_title)).append(":\n");
                aboutContent.append(getString(R.string.about_description_text)).append("\n\n");

                // Key features
                aboutContent.append("⭐ ").append(getString(R.string.about_features_title)).append(":\n");
                aboutContent.append(getString(R.string.about_features_text)).append("\n\n");

                // Developer info
                aboutContent.append("👨‍💻 ").append(getString(R.string.about_developer_title)).append(": ");
                aboutContent.append(getString(R.string.about_developer_name)).append("\n\n");

                // License
                aboutContent.append("📜 ").append(getString(R.string.about_license_title)).append(": ");
                aboutContent.append(getString(R.string.about_license_name)).append("\n\n");

                // Build information
                aboutContent.append("🔧 ").append(getString(R.string.about_build_info_title)).append(":\n");
                aboutContent.append(String.format(getString(R.string.about_package_name),
                        getContext().getPackageName())).append("\n");
                aboutContent.append(String.format(getString(R.string.about_target_sdk),
                        android.os.Build.VERSION.SDK_INT)).append("\n");
                aboutContent.append(String.format(getString(R.string.about_min_sdk), 25)).append("\n");

                // Show build time if available
                try {
                    android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(getContext().getPackageName(),
                            0);
                    java.io.File apkFile = new java.io.File(appInfo.sourceDir);
                    long buildTime = apkFile.lastModified();
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault());
                    aboutContent.append(String.format(getString(R.string.about_build_time),
                            dateFormat.format(new java.util.Date(buildTime)))).append("\n");
                } catch (Exception e) {
                    // Ignore build time if unavailable
                }

                // Show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getString(R.string.about_title))
                        .setMessage(aboutContent.toString())
                        .setPositiveButton("OK", null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error showing about information: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }

        private void setupSecurityPreferences() {
            // Set up security enable/disable
            androidx.preference.SwitchPreferenceCompat securityEnabledPreference = 
                findPreference(getString(R.string.key_security_enabled));
            if (securityEnabledPreference != null) {
                securityEnabledPreference.setChecked(securityManager.isSecurityEnabled());
                securityEnabledPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    securityManager.setSecurityEnabled(enabled);
                    updateSecurityPreferenceSummaries();
                    return true;
                });
            }

            // Set up PIN setup
            Preference pinSetupPreference = findPreference(getString(R.string.key_pin_setup));
            if (pinSetupPreference != null) {
                pinSetupPreference.setOnPreferenceClickListener(preference -> {
                    showPinSetupDialog();
                    return true;
                });
                updatePinSetupSummary(pinSetupPreference);
            }

            // Set up biometric enable/disable
            androidx.preference.SwitchPreferenceCompat biometricPreference = 
                findPreference(getString(R.string.key_biometric_enabled));
            if (biometricPreference != null) {
                biometricPreference.setChecked(securityManager.isBiometricEnabled());
                biometricPreference.setEnabled(securityManager.isBiometricAvailable());
                
                if (!securityManager.isBiometricAvailable()) {
                    biometricPreference.setSummary(securityManager.getBiometricStatusMessage());
                }
                
                biometricPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        if (securityManager.enableBiometric()) {
                            Toast.makeText(getContext(), getString(R.string.biometric_enabled_success), 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.biometric_enable_failed), 
                                Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    } else {
                        securityManager.disableBiometric();
                        Toast.makeText(getContext(), getString(R.string.biometric_disabled), 
                            Toast.LENGTH_SHORT).show();
                    }
                    updateSecurityPreferenceSummaries();
                    return true;
                });
            }

            // Set up authentication timeout
            androidx.preference.ListPreference timeoutPreference = 
                findPreference(getString(R.string.key_auth_timeout));
            if (timeoutPreference != null) {
                updateAuthTimeoutSummary(timeoutPreference);
                timeoutPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String timeoutValue = (String) newValue;
                    long timeoutMs = Long.parseLong(timeoutValue);
                    securityManager.setAuthTimeout(timeoutMs);
                    updateAuthTimeoutSummary((androidx.preference.ListPreference) preference);
                    return true;
                });
            }

            // Set up security test
            Preference securityTestPreference = findPreference(getString(R.string.key_security_test));
            if (securityTestPreference != null) {
                securityTestPreference.setOnPreferenceClickListener(preference -> {
                    testSecurityAuthentication();
                    return true;
                });
            }

            // Update all summaries
            updateSecurityPreferenceSummaries();
        }

        private void showPinSetupDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            
            if (securityManager.isPinEnabled()) {
                // PIN is already set, offer to change or remove
                builder.setTitle(getString(R.string.pin_setup_existing_title));
                builder.setMessage(getString(R.string.pin_setup_existing_message));
                builder.setPositiveButton(getString(R.string.pin_setup_change), (dialog, which) -> showChangePinDialog());
                builder.setNegativeButton(getString(R.string.pin_setup_remove), (dialog, which) -> showRemovePinDialog());
                builder.setNeutralButton(getString(R.string.pin_setup_cancel), null);
            } else {
                // No PIN set, offer to create one
                builder.setTitle(getString(R.string.pin_setup_new_title));
                builder.setMessage(getString(R.string.pin_setup_new_message));
                builder.setPositiveButton(getString(R.string.pin_setup_create), (dialog, which) -> showCreatePinDialog());
                builder.setNegativeButton(getString(R.string.pin_setup_cancel), null);
            }
            
            builder.show();
        }

        private void showCreatePinDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.pin_create_title));
            builder.setMessage(getString(R.string.pin_create_message));
            
            final EditText pinInput = new EditText(getContext());
            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinInput.setHint(getString(R.string.pin_create_hint));
            builder.setView(pinInput);
            
            builder.setPositiveButton(getString(R.string.pin_create_confirm), (dialog, which) -> {
                String pin = pinInput.getText().toString().trim();
                if (pin.length() < 4) {
                    Toast.makeText(getContext(), getString(R.string.pin_create_too_short), 
                        Toast.LENGTH_SHORT).show();
                    showCreatePinDialog(); // Show again
                } else {
                    showConfirmPinDialog(pin);
                }
            });
            
            builder.setNegativeButton(getString(R.string.pin_create_cancel), null);
            builder.show();
        }

        private void showConfirmPinDialog(String originalPin) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.pin_confirm_title));
            builder.setMessage(getString(R.string.pin_confirm_message));
            
            final EditText pinInput = new EditText(getContext());
            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinInput.setHint(getString(R.string.pin_confirm_hint));
            builder.setView(pinInput);
            
            builder.setPositiveButton(getString(R.string.pin_confirm_verify), (dialog, which) -> {
                String confirmedPin = pinInput.getText().toString().trim();
                if (originalPin.equals(confirmedPin)) {
                    if (securityManager.setPIN(originalPin)) {
                        Toast.makeText(getContext(), getString(R.string.pin_create_success), 
                            Toast.LENGTH_SHORT).show();
                        updateSecurityPreferenceSummaries();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.pin_create_failed), 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.pin_confirm_mismatch), 
                        Toast.LENGTH_SHORT).show();
                    showConfirmPinDialog(originalPin); // Show again
                }
            });
            
            builder.setNegativeButton(getString(R.string.pin_confirm_cancel), null);
            builder.show();
        }

        private void showChangePinDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.pin_change_title));
            builder.setMessage(getString(R.string.pin_change_message));
            
            final EditText currentPinInput = new EditText(getContext());
            currentPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            currentPinInput.setHint(getString(R.string.pin_change_current_hint));
            builder.setView(currentPinInput);
            
            builder.setPositiveButton(getString(R.string.pin_change_verify), (dialog, which) -> {
                String currentPin = currentPinInput.getText().toString().trim();
                if (securityManager.verifyPIN(currentPin)) {
                    showCreatePinDialog(); // Show new PIN creation
                } else {
                    Toast.makeText(getContext(), getString(R.string.pin_change_wrong_current), 
                        Toast.LENGTH_SHORT).show();
                }
            });
            
            builder.setNegativeButton(getString(R.string.pin_change_cancel), null);
            builder.show();
        }

        private void showRemovePinDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.pin_remove_title));
            builder.setMessage(getString(R.string.pin_remove_message));
            
            final EditText pinInput = new EditText(getContext());
            pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pinInput.setHint(getString(R.string.pin_remove_hint));
            builder.setView(pinInput);
            
            builder.setPositiveButton(getString(R.string.pin_remove_confirm), (dialog, which) -> {
                String pin = pinInput.getText().toString().trim();
                if (securityManager.verifyPIN(pin)) {
                    securityManager.clearPinData();
                    Toast.makeText(getContext(), getString(R.string.pin_remove_success), 
                        Toast.LENGTH_SHORT).show();
                    updateSecurityPreferenceSummaries();
                } else {
                    Toast.makeText(getContext(), getString(R.string.pin_remove_wrong), 
                        Toast.LENGTH_SHORT).show();
                }
            });
            
            builder.setNegativeButton(getString(R.string.pin_remove_cancel), null);
            builder.show();
        }

        private void testSecurityAuthentication() {
            if (!securityManager.isSecurityEnabled()) {
                Toast.makeText(getContext(), getString(R.string.security_test_disabled), 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            Intent authIntent = AuthenticationActivity.createIntent(getContext(), 
                AuthenticationActivity.AUTH_TYPE_SETUP);
            startActivity(authIntent);
        }

        private void updateSecurityPreferenceSummaries() {
            // Update PIN setup summary
            Preference pinSetupPreference = findPreference(getString(R.string.key_pin_setup));
            if (pinSetupPreference != null) {
                updatePinSetupSummary(pinSetupPreference);
            }
            
            // Update authentication timeout summary
            androidx.preference.ListPreference timeoutPreference = 
                findPreference(getString(R.string.key_auth_timeout));
            if (timeoutPreference != null) {
                updateAuthTimeoutSummary(timeoutPreference);
            }
        }

        private void updatePinSetupSummary(Preference preference) {
            if (securityManager.isPinEnabled()) {
                preference.setSummary(getString(R.string.pin_setup_summary_enabled));
            } else {
                preference.setSummary(getString(R.string.pin_setup_summary_disabled));
            }
        }

        private void updateAuthTimeoutSummary(androidx.preference.ListPreference preference) {
            long timeoutMs = securityManager.getAuthTimeout();
            String[] timeoutEntries = getResources().getStringArray(R.array.auth_timeout_entries);
            String[] timeoutValues = getResources().getStringArray(R.array.auth_timeout_values);
            
            for (int i = 0; i < timeoutValues.length; i++) {
                if (timeoutValues[i].equals(String.valueOf(timeoutMs))) {
                    preference.setSummary(timeoutEntries[i]);
                    break;
                }
            }
        }

        private void setupWhitelistPreferences() {
            SwitchPreferenceCompat enableWhitelistPref = findPreference(getString(R.string.key_enable_number_whitelist));
            EditTextPreference whitelistPref = findPreference(getString(R.string.key_number_whitelist));

            if (enableWhitelistPref != null && whitelistPref != null) {
                enableWhitelistPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    updateWhitelistSummary(whitelistPref, (Boolean) newValue, whitelistPref.getText());
                    return true;
                });

                whitelistPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String cleanedValue = SmsNumberFilter.cleanWhitelist((String) newValue);
                    if (!newValue.equals(cleanedValue)) {
                        ((EditTextPreference) preference).setText(cleanedValue);
                    }
                    updateWhitelistSummary((EditTextPreference) preference, enableWhitelistPref.isChecked(), cleanedValue);
                    return true;
                });

                // Set initial summary
                updateWhitelistSummary(whitelistPref, enableWhitelistPref.isChecked(), whitelistPref.getText());
            }
        }

        private void updateWhitelistSummary(EditTextPreference preference, boolean enabled, String whitelist) {
            if (!enabled) {
                preference.setSummary(getString(R.string.whitelist_inactive_summary));
                return;
            }

            String summary = SmsNumberFilter.getWhitelistSummary(whitelist);
            if (summary != null) {
                preference.setSummary(String.format(getString(R.string.whitelist_active_summary), summary));
            } else {
                preference.setSummary(getString(R.string.whitelist_enabled_empty_summary));
            }
        }
    }
}