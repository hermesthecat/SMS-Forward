package com.keremgok.smsforward;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for Monitoring and Testing tools containing:
 * - Statistics overview (today's and total message counts)
 * - Message history viewing and management
 * - Connection testing for all platforms
 * - System information and diagnostics
 */
public class MonitorFragment extends BasePreferenceFragment {

    private ExecutorService executorService;
    private MessageStatsDbHelper messageStatsDbHelper;
    private MessageHistoryDbHelper messageHistoryDbHelper;
    private MessageQueueDbHelper messageQueueDbHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.monitor_preferences, rootKey);

        // Initialize database helpers
        messageStatsDbHelper = new MessageStatsDbHelper(context);
        messageHistoryDbHelper = new MessageHistoryDbHelper(context);
        messageQueueDbHelper = new MessageQueueDbHelper(context);
        
        // Initialize executor service for background tasks
        executorService = Executors.newCachedThreadPool();

        // Set up all monitor preferences
        setupStatisticsPreferences();
        setupMessageHistoryPreferences();
        setupConnectionTestPreferences();
        setupSystemInfoPreferences();
    }

    @Override
    protected void updatePreferenceSummaries() {
        updateStatisticsSummaries();
        updateMessageHistorySummaries();
        updateConnectionTestSummaries();
        updateSystemInfoSummaries();
    }

    @Override
    protected void updatePreferenceSummaryForKey(String key) {
        if (context == null) return;

        // Statistics keys
        String statisticsOverviewKey = context.getString(R.string.key_statistics_overview);
        String statisticsTodayKey = context.getString(R.string.key_statistics_today);
        String statisticsTotalKey = context.getString(R.string.key_statistics_total);
        String statisticsSuccessRateKey = context.getString(R.string.key_statistics_success_rate);

        // Message history keys
        String messageHistoryViewKey = context.getString(R.string.key_message_history_view);

        // System info keys
        String systemQueueStatusKey = context.getString(R.string.key_system_queue_status);
        String systemPermissionsKey = context.getString(R.string.key_system_permissions);

        // Update appropriate summaries based on changed key
        if (statisticsOverviewKey.equals(key) || statisticsTodayKey.equals(key) || 
            statisticsTotalKey.equals(key) || statisticsSuccessRateKey.equals(key)) {
            updateStatisticsSummaries();
        } else if (messageHistoryViewKey.equals(key)) {
            updateMessageHistorySummaries();
        } else if (systemQueueStatusKey.equals(key) || systemPermissionsKey.equals(key)) {
            updateSystemInfoSummaries();
        }
    }

    @Override
    protected void cleanupResources() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (messageStatsDbHelper != null) {
            messageStatsDbHelper.close();
        }
        if (messageHistoryDbHelper != null) {
            messageHistoryDbHelper.close();
        }
        if (messageQueueDbHelper != null) {
            messageQueueDbHelper.close();
        }
    }

    /**
     * Set up statistics preferences
     */
    private void setupStatisticsPreferences() {
        Preference statisticsOverviewPreference = getPreferenceByStringRes(R.string.key_statistics_overview);
        if (statisticsOverviewPreference != null) {
            statisticsOverviewPreference.setOnPreferenceClickListener(preference -> {
                showStatisticsOverview();
                return true;
            });
        }

        Preference statisticsTodayPreference = getPreferenceByStringRes(R.string.key_statistics_today);
        if (statisticsTodayPreference != null) {
            statisticsTodayPreference.setOnPreferenceClickListener(preference -> {
                showTodayStatistics();
                return true;
            });
        }

        Preference statisticsTotalPreference = getPreferenceByStringRes(R.string.key_statistics_total);
        if (statisticsTotalPreference != null) {
            statisticsTotalPreference.setOnPreferenceClickListener(preference -> {
                showTotalStatistics();
                return true;
            });
        }

        Preference statisticsSuccessRatePreference = getPreferenceByStringRes(R.string.key_statistics_success_rate);
        if (statisticsSuccessRatePreference != null) {
            statisticsSuccessRatePreference.setOnPreferenceClickListener(preference -> {
                showSuccessRateStatistics();
                return true;
            });
        }
    }

    /**
     * Set up message history preferences
     */
    private void setupMessageHistoryPreferences() {
        Preference messageHistoryViewPreference = getPreferenceByStringRes(R.string.key_message_history_view);
        if (messageHistoryViewPreference != null) {
            messageHistoryViewPreference.setOnPreferenceClickListener(preference -> {
                showMessageHistory();
                return true;
            });
        }

        Preference messageHistoryClearPreference = getPreferenceByStringRes(R.string.key_message_history_clear);
        if (messageHistoryClearPreference != null) {
            messageHistoryClearPreference.setOnPreferenceClickListener(preference -> {
                showClearHistoryConfirmation();
                return true;
            });
        }

        Preference messageHistoryExportPreference = getPreferenceByStringRes(R.string.key_message_history_export);
        if (messageHistoryExportPreference != null) {
            messageHistoryExportPreference.setOnPreferenceClickListener(preference -> {
                exportMessageHistory();
                return true;
            });
        }
    }

    /**
     * Set up connection test preferences
     */
    private void setupConnectionTestPreferences() {
        Preference testSmsPreference = getPreferenceByStringRes(R.string.key_test_sms);
        if (testSmsPreference != null) {
            testSmsPreference.setOnPreferenceClickListener(preference -> {
                testSmsConnection();
                return true;
            });
        }

        Preference testTelegramPreference = getPreferenceByStringRes(R.string.key_test_telegram);
        if (testTelegramPreference != null) {
            testTelegramPreference.setOnPreferenceClickListener(preference -> {
                testTelegramConnection();
                return true;
            });
        }

        Preference testEmailPreference = getPreferenceByStringRes(R.string.key_test_email);
        if (testEmailPreference != null) {
            testEmailPreference.setOnPreferenceClickListener(preference -> {
                testEmailConnection();
                return true;
            });
        }

        Preference testWebPreference = getPreferenceByStringRes(R.string.key_test_web);
        if (testWebPreference != null) {
            testWebPreference.setOnPreferenceClickListener(preference -> {
                testWebConnection();
                return true;
            });
        }

        Preference testAllPlatformsPreference = getPreferenceByStringRes(R.string.key_test_all_platforms);
        if (testAllPlatformsPreference != null) {
            testAllPlatformsPreference.setOnPreferenceClickListener(preference -> {
                testAllPlatforms();
                return true;
            });
        }
    }

    /**
     * Set up system information preferences
     */
    private void setupSystemInfoPreferences() {
        Preference systemPermissionsPreference = getPreferenceByStringRes(R.string.key_system_permissions);
        if (systemPermissionsPreference != null) {
            systemPermissionsPreference.setOnPreferenceClickListener(preference -> {
                showSystemPermissions();
                return true;
            });
        }

        Preference systemQueueStatusPreference = getPreferenceByStringRes(R.string.key_system_queue_status);
        if (systemQueueStatusPreference != null) {
            systemQueueStatusPreference.setOnPreferenceClickListener(preference -> {
                showQueueStatus();
                return true;
            });
        }

        Preference systemLogsPreference = getPreferenceByStringRes(R.string.key_system_logs);
        if (systemLogsPreference != null) {
            systemLogsPreference.setOnPreferenceClickListener(preference -> {
                showSystemLogs();
                return true;
            });
        }
    }

    /**
     * Update all statistics summaries
     */
    private void updateStatisticsSummaries() {
        executorService.execute(() -> {
            try {
                MessageStatsDbHelper.DailyStats todayStats = messageStatsDbHelper.getTodayStats();
                MessageStatsDbHelper.TotalStats totalStats = messageStatsDbHelper.getTotalStats();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_statistics_overview, 
                            String.format("Today: %d, Total: %d", todayStats.totalCount, totalStats.totalCount));
                        
                        updateStatisticsSummary(R.string.key_statistics_today, 
                            String.format("%d messages (%d successful)", todayStats.totalCount, todayStats.successCount));
                        
                        updateStatisticsSummary(R.string.key_statistics_total, 
                            String.format("%d messages (%d successful)", totalStats.totalCount, totalStats.successCount));
                        
                        double successRate = totalStats.totalCount > 0 ? 
                            (double) totalStats.successCount / totalStats.totalCount * 100 : 0;
                        updateStatisticsSummary(R.string.key_statistics_success_rate, 
                            String.format("%.1f%% success rate", successRate));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_statistics_overview, "Error loading statistics");
                    });
                }
            }
        });
    }

    /**
     * Update message history summaries
     */
    private void updateMessageHistorySummaries() {
        executorService.execute(() -> {
            try {
                MessageHistoryDbHelper.HistoryStats historyStats = messageHistoryDbHelper.getHistoryStats();
                int historyCount = historyStats.totalCount;
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_message_history_view, 
                            String.format("%d messages in history", historyCount));
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_message_history_view, 
                            "Error loading history count");
                    });
                }
            }
        });
    }

    /**
     * Update connection test summaries
     */
    private void updateConnectionTestSummaries() {
        // Test summaries are updated when tests are run
        // Set initial status based on platform configurations
        updateTestSummary(R.string.key_test_sms, 
            getBooleanPreference(context.getString(R.string.key_enable_sms), false));
        updateTestSummary(R.string.key_test_telegram, 
            getBooleanPreference(context.getString(R.string.key_enable_telegram), false));
        updateTestSummary(R.string.key_test_email, 
            getBooleanPreference(context.getString(R.string.key_enable_email), false));
        updateTestSummary(R.string.key_test_web, 
            getBooleanPreference(context.getString(R.string.key_enable_web), false));
    }

    /**
     * Update system information summaries
     */
    private void updateSystemInfoSummaries() {
        executorService.execute(() -> {
            try {
                MessageQueueDbHelper.QueueStats queueStats = messageQueueDbHelper.getQueueStats();
                int queueCount = queueStats.pendingCount;
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_system_queue_status, 
                            String.format("%d messages in queue", queueCount));
                        
                        // Check permissions
                        boolean hasPermissions = checkRequiredPermissions();
                        updateStatisticsSummary(R.string.key_system_permissions, 
                            hasPermissions ? "All permissions granted" : "Missing permissions");
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateStatisticsSummary(R.string.key_system_queue_status, "Error reading queue");
                    });
                }
            }
        });
    }

    /**
     * Helper method to update statistics summary
     */
    private void updateStatisticsSummary(int keyResId, String summary) {
        Preference preference = getPreferenceByStringRes(keyResId);
        if (preference != null) {
            preference.setSummary(summary);
        }
    }

    /**
     * Helper method to update test summary
     */
    private void updateTestSummary(int keyResId, boolean enabled) {
        Preference preference = getPreferenceByStringRes(keyResId);
        if (preference != null) {
            preference.setSummary(enabled ? "Platform enabled" : "Platform disabled");
        }
    }

    /**
     * Show detailed statistics overview dialog
     */
    private void showStatisticsOverview() {
        executorService.execute(() -> {
            try {
                MessageStatsDbHelper.DailyStats todayStats = messageStatsDbHelper.getTodayStats();
                MessageStatsDbHelper.TotalStats totalStats = messageStatsDbHelper.getTotalStats();

                StringBuilder message = new StringBuilder();
                message.append("📊 SMS Forward Statistics\n\n");
                
                message.append("🗓️ Today:\n");
                message.append(String.format("  • Total messages: %d\n", todayStats.totalCount));
                message.append(String.format("  • Successful: %d\n", todayStats.successCount));
                message.append(String.format("  • Failed: %d\n", todayStats.totalCount - todayStats.successCount));
                
                if (todayStats.totalCount > 0) {
                    double todayRate = (double) todayStats.successCount / todayStats.totalCount * 100;
                    message.append(String.format("  • Success rate: %.1f%%\n", todayRate));
                }
                
                message.append("\n📈 All Time:\n");
                message.append(String.format("  • Total messages: %d\n", totalStats.totalCount));
                message.append(String.format("  • Successful: %d\n", totalStats.successCount));
                message.append(String.format("  • Failed: %d\n", totalStats.totalCount - totalStats.successCount));
                
                if (totalStats.totalCount > 0) {
                    double totalRate = (double) totalStats.successCount / totalStats.totalCount * 100;
                    message.append(String.format("  • Success rate: %.1f%%\n", totalRate));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Statistics Overview")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading statistics: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show today's statistics
     */
    private void showTodayStatistics() {
        executorService.execute(() -> {
            try {
                MessageStatsDbHelper.DailyStats stats = messageStatsDbHelper.getTodayStats();
                
                StringBuilder message = new StringBuilder();
                message.append("📅 Today's Statistics\n\n");
                message.append(String.format("Total messages: %d\n", stats.totalCount));
                message.append(String.format("Successful: %d\n", stats.successCount));
                message.append(String.format("Failed: %d\n", stats.totalCount - stats.successCount));
                
                if (stats.totalCount > 0) {
                    double successRate = (double) stats.successCount / stats.totalCount * 100;
                    message.append(String.format("Success rate: %.1f%%", successRate));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Today's Statistics")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading today's statistics: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show total statistics
     */
    private void showTotalStatistics() {
        executorService.execute(() -> {
            try {
                MessageStatsDbHelper.TotalStats stats = messageStatsDbHelper.getTotalStats();
                
                StringBuilder message = new StringBuilder();
                message.append("📊 Total Statistics\n\n");
                message.append(String.format("Total messages: %d\n", stats.totalCount));
                message.append(String.format("Successful: %d\n", stats.successCount));
                message.append(String.format("Failed: %d\n", stats.totalCount - stats.successCount));
                
                if (stats.totalCount > 0) {
                    double successRate = (double) stats.successCount / stats.totalCount * 100;
                    message.append(String.format("Success rate: %.1f%%", successRate));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Total Statistics")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading total statistics: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show success rate statistics
     */
    private void showSuccessRateStatistics() {
        executorService.execute(() -> {
            try {
                MessageStatsDbHelper.DailyStats todayStats = messageStatsDbHelper.getTodayStats();
                MessageStatsDbHelper.TotalStats totalStats = messageStatsDbHelper.getTotalStats();
                
                StringBuilder message = new StringBuilder();
                message.append("📈 Success Rate Analysis\n\n");
                
                if (todayStats.totalCount > 0) {
                    double todayRate = (double) todayStats.successCount / todayStats.totalCount * 100;
                    message.append(String.format("Today: %.1f%% (%d/%d)\n", todayRate, 
                        todayStats.successCount, todayStats.totalCount));
                } else {
                    message.append("Today: No messages sent\n");
                }
                
                if (totalStats.totalCount > 0) {
                    double totalRate = (double) totalStats.successCount / totalStats.totalCount * 100;
                    message.append(String.format("All time: %.1f%% (%d/%d)\n", totalRate, 
                        totalStats.successCount, totalStats.totalCount));
                    
                    if (totalRate >= 95) {
                        message.append("\n✅ Excellent success rate!");
                    } else if (totalRate >= 80) {
                        message.append("\n⚠️ Good success rate, consider checking configuration for failed messages.");
                    } else {
                        message.append("\n❌ Low success rate, please check platform configurations.");
                    }
                } else {
                    message.append("All time: No messages sent");
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Success Rate Statistics")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading success rate statistics: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show message history
     */
    private void showMessageHistory() {
        executorService.execute(() -> {
            try {
                List<MessageHistoryDbHelper.HistoryRecord> history = messageHistoryDbHelper.getMessageHistory(50);
                
                StringBuilder message = new StringBuilder();
                message.append("📧 Recent Message History\n");
                message.append("(Last 50 messages)\n\n");
                
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                
                if (history.isEmpty()) {
                    message.append("No messages in history.");
                } else {
                    for (MessageHistoryDbHelper.HistoryRecord entry : history) {
                        String timestamp = sdf.format(new Date(entry.timestamp));
                        String status = entry.isSuccess() ? "✅" : "❌";
                        String platform = entry.platform != null ? entry.platform : "Unknown";
                        
                        message.append(String.format("%s %s [%s] %s\n", 
                            status, timestamp, platform, 
                            entry.fromNumber != null ? entry.fromNumber : "Unknown"));
                        
                        if (entry.messageContent != null && entry.messageContent.length() > 50) {
                            message.append("   " + entry.messageContent.substring(0, 47) + "...\n");
                        } else if (entry.messageContent != null) {
                            message.append("   " + entry.messageContent + "\n");
                        }
                        
                        if (entry.isFailed() && entry.errorMessage != null) {
                            message.append("   Error: " + entry.errorMessage + "\n");
                        }
                        message.append("\n");
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Message History")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .setNegativeButton("Clear History", (dialog, which) -> showClearHistoryConfirmation())
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading message history: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show confirmation dialog for clearing history
     */
    private void showClearHistoryConfirmation() {
        new AlertDialog.Builder(context)
            .setTitle("Clear Message History")
            .setMessage("Are you sure you want to clear all message history? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> clearMessageHistory())
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Clear message history
     */
    private void clearMessageHistory() {
        executorService.execute(() -> {
            try {
                messageHistoryDbHelper.clearHistory();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Message history cleared", Toast.LENGTH_SHORT);
                        updateMessageHistorySummaries();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error clearing history: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Export message history
     */
    private void exportMessageHistory() {
        // Note: This is a simplified implementation
        // In a full implementation, you would use Android's storage access framework
        showToast("Message history export is not yet implemented", Toast.LENGTH_SHORT);
    }

    /**
     * Test SMS connection
     */
    private void testSmsConnection() {
        boolean smsEnabled = getBooleanPreference(context.getString(R.string.key_enable_sms), false);
        
        if (!smsEnabled) {
            showToast("SMS forwarding is disabled", Toast.LENGTH_SHORT);
            return;
        }
        
        String targetNumber = getStringPreference(context.getString(R.string.key_target_sms), "");
        
        if (targetNumber.trim().isEmpty()) {
            showToast("SMS target number not configured", Toast.LENGTH_SHORT);
            return;
        }
        
        // Simple validation
        if (targetNumber.length() < 10) {
            showToast("SMS target number appears to be invalid", Toast.LENGTH_SHORT);
            return;
        }
        
        showToast("SMS configuration appears valid", Toast.LENGTH_SHORT);
        updateTestSummary(R.string.key_test_sms, true);
    }

    /**
     * Test Telegram connection
     */
    private void testTelegramConnection() {
        boolean telegramEnabled = getBooleanPreference(context.getString(R.string.key_enable_telegram), false);
        
        if (!telegramEnabled) {
            showToast("Telegram forwarding is disabled", Toast.LENGTH_SHORT);
            return;
        }
        
        String apiKey = getStringPreference(context.getString(R.string.key_telegram_apikey), "");
        String targetId = getStringPreference(context.getString(R.string.key_target_telegram), "");
        
        if (apiKey.trim().isEmpty()) {
            showToast("Telegram API key not configured", Toast.LENGTH_SHORT);
            return;
        }
        
        if (targetId.trim().isEmpty()) {
            showToast("Telegram target ID not configured", Toast.LENGTH_SHORT);
            return;
        }
        
        showToast("Telegram configuration appears valid", Toast.LENGTH_SHORT);
        updateTestSummary(R.string.key_test_telegram, true);
    }

    /**
     * Test Email connection
     */
    private void testEmailConnection() {
        boolean emailEnabled = getBooleanPreference(context.getString(R.string.key_enable_email), false);
        
        if (!emailEnabled) {
            showToast("Email forwarding is disabled", Toast.LENGTH_SHORT);
            return;
        }
        
        String fromAddress = getStringPreference(context.getString(R.string.key_email_from_address), "");
        String toAddress = getStringPreference(context.getString(R.string.key_email_to_address), "");
        String smtpHost = getStringPreference(context.getString(R.string.key_email_submit_host), "");
        String smtpPassword = getStringPreference(context.getString(R.string.key_email_submit_password), "");
        
        if (fromAddress.trim().isEmpty() || toAddress.trim().isEmpty() || 
            smtpHost.trim().isEmpty() || smtpPassword.trim().isEmpty()) {
            showToast("Email configuration incomplete", Toast.LENGTH_SHORT);
            return;
        }
        
        showToast("Email configuration appears valid", Toast.LENGTH_SHORT);
        updateTestSummary(R.string.key_test_email, true);
    }

    /**
     * Test Web connection
     */
    private void testWebConnection() {
        boolean webEnabled = getBooleanPreference(context.getString(R.string.key_enable_web), false);
        
        if (!webEnabled) {
            showToast("Web forwarding is disabled", Toast.LENGTH_SHORT);
            return;
        }
        
        String targetUrl = getStringPreference(context.getString(R.string.key_target_web), "");
        
        if (targetUrl.trim().isEmpty()) {
            showToast("Web target URL not configured", Toast.LENGTH_SHORT);
            return;
        }
        
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            showToast("Web target URL should start with http:// or https://", Toast.LENGTH_SHORT);
            return;
        }
        
        showToast("Web configuration appears valid", Toast.LENGTH_SHORT);
        updateTestSummary(R.string.key_test_web, true);
    }

    /**
     * Test all platforms
     */
    private void testAllPlatforms() {
        StringBuilder results = new StringBuilder();
        results.append("🔧 Platform Configuration Test\n\n");
        
        // Test SMS
        boolean smsEnabled = getBooleanPreference(context.getString(R.string.key_enable_sms), false);
        if (smsEnabled) {
            String targetNumber = getStringPreference(context.getString(R.string.key_target_sms), "");
            results.append("📱 SMS: ");
            if (!targetNumber.trim().isEmpty() && targetNumber.length() >= 10) {
                results.append("✅ Configured\n");
            } else {
                results.append("❌ Invalid target number\n");
            }
        } else {
            results.append("📱 SMS: ⏸️ Disabled\n");
        }
        
        // Test Telegram
        boolean telegramEnabled = getBooleanPreference(context.getString(R.string.key_enable_telegram), false);
        if (telegramEnabled) {
            String apiKey = getStringPreference(context.getString(R.string.key_telegram_apikey), "");
            String targetId = getStringPreference(context.getString(R.string.key_target_telegram), "");
            results.append("💬 Telegram: ");
            if (!apiKey.trim().isEmpty() && !targetId.trim().isEmpty()) {
                results.append("✅ Configured\n");
            } else {
                results.append("❌ Missing API key or target ID\n");
            }
        } else {
            results.append("💬 Telegram: ⏸️ Disabled\n");
        }
        
        // Test Email
        boolean emailEnabled = getBooleanPreference(context.getString(R.string.key_enable_email), false);
        if (emailEnabled) {
            String fromAddress = getStringPreference(context.getString(R.string.key_email_from_address), "");
            String toAddress = getStringPreference(context.getString(R.string.key_email_to_address), "");
            String smtpHost = getStringPreference(context.getString(R.string.key_email_submit_host), "");
            String smtpPassword = getStringPreference(context.getString(R.string.key_email_submit_password), "");
            results.append("📧 Email: ");
            if (!fromAddress.trim().isEmpty() && !toAddress.trim().isEmpty() && 
                !smtpHost.trim().isEmpty() && !smtpPassword.trim().isEmpty()) {
                results.append("✅ Configured\n");
            } else {
                results.append("❌ Incomplete configuration\n");
            }
        } else {
            results.append("📧 Email: ⏸️ Disabled\n");
        }
        
        // Test Web
        boolean webEnabled = getBooleanPreference(context.getString(R.string.key_enable_web), false);
        if (webEnabled) {
            String targetUrl = getStringPreference(context.getString(R.string.key_target_web), "");
            results.append("🌐 Web: ");
            if (!targetUrl.trim().isEmpty() && 
                (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
                results.append("✅ Configured\n");
            } else {
                results.append("❌ Invalid URL\n");
            }
        } else {
            results.append("🌐 Web: ⏸️ Disabled\n");
        }
        
        results.append("\nNote: These are basic configuration checks. Actual connectivity may vary.");
        
        new AlertDialog.Builder(context)
            .setTitle("Platform Test Results")
            .setMessage(results.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Show system permissions status
     */
    private void showSystemPermissions() {
        StringBuilder message = new StringBuilder();
        message.append("🔐 System Permissions\n\n");
        
        // Check SMS permissions
        boolean sendSmsPermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        message.append("📱 SEND_SMS: ").append(sendSmsPermission ? "✅ Granted" : "❌ Denied").append("\n");
        
        boolean receiveSmsPermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        message.append("📥 RECEIVE_SMS: ").append(receiveSmsPermission ? "✅ Granted" : "❌ Denied").append("\n");
        
        // Check internet permissions
        boolean internetPermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        message.append("🌐 INTERNET: ").append(internetPermission ? "✅ Granted" : "❌ Denied").append("\n");
        
        boolean networkStatePermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
        message.append("📡 NETWORK_STATE: ").append(networkStatePermission ? "✅ Granted" : "❌ Denied").append("\n");
        
        if (!sendSmsPermission || !receiveSmsPermission || !internetPermission || !networkStatePermission) {
            message.append("\n⚠️ Some permissions are missing. The app may not function correctly.");
        } else {
            message.append("\n✅ All required permissions are granted.");
        }
        
        new AlertDialog.Builder(context)
            .setTitle("System Permissions")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Show message queue status
     */
    private void showQueueStatus() {
        executorService.execute(() -> {
            try {
                MessageQueueDbHelper.QueueStats queueStats = messageQueueDbHelper.getQueueStats();
                int pendingCount = queueStats.pendingCount;
                List<MessageQueueDbHelper.QueuedMessage> pendingMessages = messageQueueDbHelper.getPendingMessages();
                
                // Limit to 10 for display
                if (pendingMessages.size() > 10) {
                    pendingMessages = pendingMessages.subList(0, 10);
                }
                
                StringBuilder message = new StringBuilder();
                message.append("📬 Message Queue Status\n\n");
                message.append(String.format("Pending messages: %d\n\n", pendingCount));
                
                if (pendingMessages.isEmpty()) {
                    message.append("No messages in queue.");
                } else {
                    message.append("Recent pending messages:\n");
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                    
                    for (MessageQueueDbHelper.QueuedMessage entry : pendingMessages) {
                        String timestamp = sdf.format(new Date(entry.createdAt));
                        message.append(String.format("• %s [%s] Retry: %d\n", 
                            timestamp, entry.forwarderType, entry.retryCount));
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(context)
                            .setTitle("Queue Status")
                            .setMessage(message.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Error loading queue status: " + e.getMessage(), Toast.LENGTH_SHORT);
                    });
                }
            }
        });
    }

    /**
     * Show system logs
     */
    private void showSystemLogs() {
        // Note: This is a simplified implementation
        // In a full implementation, you would implement proper logging functionality
        new AlertDialog.Builder(context)
            .setTitle("System Logs")
            .setMessage("📋 System Logs\n\nSystem logging is not yet implemented.\n\nFor now, you can check:\n• Statistics for success/failure rates\n• Message history for recent activity\n• Queue status for pending messages")
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Check if all required permissions are granted
     */
    private boolean checkRequiredPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
    }
}