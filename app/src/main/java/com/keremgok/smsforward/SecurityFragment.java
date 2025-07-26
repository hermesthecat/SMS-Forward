package com.keremgok.smsforward;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Fragment for Security settings containing:
 * - Authentication settings (PIN, biometric, timeout)
 * - Rate limiting configuration
 * - Content filtering (keyword blacklist)
 * - Sender filtering (number whitelist)
 */
public class SecurityFragment extends BasePreferenceFragment {

    private SecurityManager securityManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.security_preferences, rootKey);

        // Initialize security manager
        securityManager = new SecurityManager(context);

        // Set up all security preferences
        setupAuthenticationPreferences();
        setupRateLimitingPreferences();
        setupContentFilterPreferences();
        setupSenderFilterPreferences();
    }

    @Override
    protected void updatePreferenceSummaries() {
        updateAuthenticationSummaries();
        updateRateLimitingSummaries();
        updateContentFilterSummaries();
        updateSenderFilterSummaries();
    }

    @Override
    protected void updatePreferenceSummaryForKey(String key) {
        if (context == null) return;

        // Authentication keys
        String securityEnabledKey = context.getString(R.string.key_security_enabled);
        String pinSetupKey = context.getString(R.string.key_pin_setup);
        String biometricEnabledKey = context.getString(R.string.key_biometric_enabled);
        String authTimeoutKey = context.getString(R.string.key_auth_timeout);

        // Rate limiting keys
        String rateLimitEnabledKey = context.getString(R.string.key_enable_rate_limiting);
        String rateLimitStatusKey = context.getString(R.string.key_rate_limit_status);

        // Content filter keys
        String filterKeywordsKey = context.getString(R.string.key_filter_keywords);

        // Sender filter keys
        String whitelistEnabledKey = context.getString(R.string.key_enable_number_whitelist);
        String whitelistKey = context.getString(R.string.key_number_whitelist);

        // Update appropriate summaries based on changed key
        if (securityEnabledKey.equals(key) || pinSetupKey.equals(key) || 
            biometricEnabledKey.equals(key) || authTimeoutKey.equals(key)) {
            updateAuthenticationSummaries();
        } else if (rateLimitEnabledKey.equals(key) || rateLimitStatusKey.equals(key)) {
            updateRateLimitingSummaries();
        } else if (filterKeywordsKey.equals(key)) {
            updateContentFilterSummaries();
        } else if (whitelistEnabledKey.equals(key) || whitelistKey.equals(key)) {
            updateSenderFilterSummaries();
        }
    }

    @Override
    protected void cleanupResources() {
        // Clean up SecurityManager resources
        if (securityManager != null) {
            // SecurityManager doesn't currently have cleanup method, but good practice
            securityManager = null;
        }
    }

    /**
     * Set up authentication related preferences
     */
    private void setupAuthenticationPreferences() {
        // Security enable/disable
        SwitchPreferenceCompat securityEnabledPreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_security_enabled);
        if (securityEnabledPreference != null) {
            securityEnabledPreference.setChecked(securityManager.isSecurityEnabled());
            securityEnabledPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                securityManager.setSecurityEnabled(enabled);
                updateAuthenticationSummaries();
                return true;
            });
        }

        // PIN setup
        Preference pinSetupPreference = getPreferenceByStringRes(R.string.key_pin_setup);
        if (pinSetupPreference != null) {
            pinSetupPreference.setOnPreferenceClickListener(preference -> {
                showPinSetupDialog();
                return true;
            });
            updatePinSetupSummary(pinSetupPreference);
        }

        // Biometric enable/disable
        SwitchPreferenceCompat biometricPreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_biometric_enabled);
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
                        showToast(context.getString(R.string.biometric_enabled_success), 
                            Toast.LENGTH_SHORT);
                    } else {
                        showToast(context.getString(R.string.biometric_enable_failed), 
                            Toast.LENGTH_SHORT);
                        return false;
                    }
                } else {
                    securityManager.disableBiometric();
                    showToast(context.getString(R.string.biometric_disabled), 
                        Toast.LENGTH_SHORT);
                }
                updateAuthenticationSummaries();
                return true;
            });
        }

        // Authentication timeout
        ListPreference timeoutPreference = 
                (ListPreference) getPreferenceByStringRes(R.string.key_auth_timeout);
        if (timeoutPreference != null) {
            updateAuthTimeoutSummary(timeoutPreference);
            timeoutPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String timeoutValue = (String) newValue;
                long timeoutMs = Long.parseLong(timeoutValue);
                securityManager.setAuthTimeout(timeoutMs);
                updateAuthTimeoutSummary((ListPreference) preference);
                return true;
            });
        }

        // Security test
        Preference securityTestPreference = getPreferenceByStringRes(R.string.key_security_test);
        if (securityTestPreference != null) {
            securityTestPreference.setOnPreferenceClickListener(preference -> {
                testSecurityAuthentication();
                return true;
            });
        }
    }

    /**
     * Set up rate limiting preferences
     */
    private void setupRateLimitingPreferences() {
        SwitchPreferenceCompat rateLimitEnabledPreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_rate_limiting);
        if (rateLimitEnabledPreference != null) {
            rateLimitEnabledPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateRateLimitingSummaries();
                return true;
            });
        }

        Preference rateLimitStatusPreference = getPreferenceByStringRes(R.string.key_rate_limit_status);
        if (rateLimitStatusPreference != null) {
            rateLimitStatusPreference.setOnPreferenceClickListener(preference -> {
                showRateLimitStatus();
                return true;
            });
            updateRateLimitStatusSummary(rateLimitStatusPreference);
        }
    }

    /**
     * Set up content filter preferences
     */
    private void setupContentFilterPreferences() {
        EditTextPreference filterKeywordsPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_filter_keywords);
        if (filterKeywordsPreference != null) {
            updateFilterKeywordsSummary(filterKeywordsPreference);
            filterKeywordsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String newKeywords = (String) newValue;
                String cleanedKeywords = SmsContentFilter.cleanFilterKeywords(newKeywords);
                
                if (!newKeywords.equals(cleanedKeywords)) {
                    ((EditTextPreference) preference).setText(cleanedKeywords);
                }
                
                updateFilterKeywordsSummary((EditTextPreference) preference);
                return true;
            });
        }
    }

    /**
     * Set up sender filter preferences
     */
    private void setupSenderFilterPreferences() {
        SwitchPreferenceCompat enableWhitelistPreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_number_whitelist);
        EditTextPreference whitelistPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_number_whitelist);

        if (enableWhitelistPreference != null && whitelistPreference != null) {
            enableWhitelistPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateWhitelistSummary(whitelistPreference, (Boolean) newValue, whitelistPreference.getText());
                return true;
            });

            whitelistPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String cleanedValue = SmsNumberFilter.cleanWhitelist((String) newValue);
                if (!newValue.equals(cleanedValue)) {
                    ((EditTextPreference) preference).setText(cleanedValue);
                }
                updateWhitelistSummary((EditTextPreference) preference, 
                        enableWhitelistPreference.isChecked(), cleanedValue);
                return true;
            });

            // Set initial summary
            updateWhitelistSummary(whitelistPreference, 
                    enableWhitelistPreference.isChecked(), whitelistPreference.getText());
        }
    }

    /**
     * Update all authentication summaries
     */
    private void updateAuthenticationSummaries() {
        Preference pinSetupPreference = getPreferenceByStringRes(R.string.key_pin_setup);
        if (pinSetupPreference != null) {
            updatePinSetupSummary(pinSetupPreference);
        }
        
        ListPreference timeoutPreference = 
                (ListPreference) getPreferenceByStringRes(R.string.key_auth_timeout);
        if (timeoutPreference != null) {
            updateAuthTimeoutSummary(timeoutPreference);
        }
    }

    /**
     * Update PIN setup summary
     */
    private void updatePinSetupSummary(Preference preference) {
        if (securityManager.isPinEnabled()) {
            preference.setSummary(context.getString(R.string.pin_setup_summary_enabled));
        } else {
            preference.setSummary(context.getString(R.string.pin_setup_summary_disabled));
        }
    }

    /**
     * Update authentication timeout summary
     */
    private void updateAuthTimeoutSummary(ListPreference preference) {
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

    /**
     * Update all rate limiting summaries
     */
    private void updateRateLimitingSummaries() {
        Preference rateLimitStatusPreference = getPreferenceByStringRes(R.string.key_rate_limit_status);
        if (rateLimitStatusPreference != null) {
            updateRateLimitStatusSummary(rateLimitStatusPreference);
        }
    }

    /**
     * Update rate limit status summary
     */
    private void updateRateLimitStatusSummary(Preference preference) {
        try {
            RateLimiter rateLimiter = RateLimiter.getInstance();
            int currentCount = rateLimiter.getCurrentForwardCount();
            long timeUntilNext = rateLimiter.getTimeUntilNextSlot();

            String nextSlotText;
            if (timeUntilNext > 0) {
                long seconds = timeUntilNext / 1000;
                nextSlotText = String.format(context.getString(R.string.rate_limit_seconds), seconds);
            } else {
                nextSlotText = context.getString(R.string.rate_limit_available_now);
            }

            String summary = String.format(context.getString(R.string.rate_limit_status_format),
                    currentCount, nextSlotText);
            preference.setSummary(summary);

        } catch (Exception e) {
            preference.setSummary("Error reading rate limit status");
        }
    }

    /**
     * Update all content filter summaries
     */
    private void updateContentFilterSummaries() {
        EditTextPreference filterKeywordsPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_filter_keywords);
        if (filterKeywordsPreference != null) {
            updateFilterKeywordsSummary(filterKeywordsPreference);
        }
    }

    /**
     * Update filter keywords summary
     */
    private void updateFilterKeywordsSummary(EditTextPreference preference) {
        try {
            String keywords = preference.getText();
            String summary = SmsContentFilter.getFilterSummary(keywords);
            
            if (summary != null) {
                preference.setSummary(String.format(context.getString(R.string.filter_active_summary), summary));
            } else {
                preference.setSummary(context.getString(R.string.filter_inactive_summary));
            }
        } catch (Exception e) {
            preference.setSummary("Error reading filter settings");
        }
    }

    /**
     * Update all sender filter summaries
     */
    private void updateSenderFilterSummaries() {
        SwitchPreferenceCompat enableWhitelistPreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_number_whitelist);
        EditTextPreference whitelistPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_number_whitelist);
        
        if (enableWhitelistPreference != null && whitelistPreference != null) {
            updateWhitelistSummary(whitelistPreference, 
                    enableWhitelistPreference.isChecked(), whitelistPreference.getText());
        }
    }

    /**
     * Update whitelist summary
     */
    private void updateWhitelistSummary(EditTextPreference preference, boolean enabled, String whitelist) {
        if (!enabled) {
            preference.setSummary(context.getString(R.string.whitelist_inactive_summary));
            return;
        }

        String summary = SmsNumberFilter.getWhitelistSummary(whitelist);
        if (summary != null) {
            preference.setSummary(String.format(context.getString(R.string.whitelist_active_summary), summary));
        } else {
            preference.setSummary(context.getString(R.string.whitelist_enabled_empty_summary));
        }
    }

    /**
     * Show PIN setup dialog (reusing logic from MainActivity)
     */
    private void showPinSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        if (securityManager.isPinEnabled()) {
            // PIN is already set, offer to change or remove
            builder.setTitle(context.getString(R.string.pin_setup_existing_title));
            builder.setMessage(context.getString(R.string.pin_setup_existing_message));
            builder.setPositiveButton(context.getString(R.string.pin_setup_change), 
                    (dialog, which) -> showChangePinDialog());
            builder.setNegativeButton(context.getString(R.string.pin_setup_remove), 
                    (dialog, which) -> showRemovePinDialog());
            builder.setNeutralButton(context.getString(R.string.pin_setup_cancel), null);
        } else {
            // No PIN set, offer to create one
            builder.setTitle(context.getString(R.string.pin_setup_new_title));
            builder.setMessage(context.getString(R.string.pin_setup_new_message));
            builder.setPositiveButton(context.getString(R.string.pin_setup_create), 
                    (dialog, which) -> showCreatePinDialog());
            builder.setNegativeButton(context.getString(R.string.pin_setup_cancel), null);
        }
        
        builder.show();
    }

    /**
     * Show create PIN dialog
     */
    private void showCreatePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.pin_create_title));
        builder.setMessage(context.getString(R.string.pin_create_message));
        
        final EditText pinInput = new EditText(context);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setHint(context.getString(R.string.pin_create_hint));
        builder.setView(pinInput);
        
        builder.setPositiveButton(context.getString(R.string.pin_create_confirm), (dialog, which) -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.length() < 4) {
                showToast(context.getString(R.string.pin_create_too_short), Toast.LENGTH_SHORT);
                showCreatePinDialog(); // Show again
            } else {
                showConfirmPinDialog(pin);
            }
        });
        
        builder.setNegativeButton(context.getString(R.string.pin_create_cancel), null);
        builder.show();
    }

    /**
     * Show confirm PIN dialog
     */
    private void showConfirmPinDialog(String originalPin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.pin_confirm_title));
        builder.setMessage(context.getString(R.string.pin_confirm_message));
        
        final EditText pinInput = new EditText(context);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setHint(context.getString(R.string.pin_confirm_hint));
        builder.setView(pinInput);
        
        builder.setPositiveButton(context.getString(R.string.pin_confirm_verify), (dialog, which) -> {
            String confirmedPin = pinInput.getText().toString().trim();
            if (originalPin.equals(confirmedPin)) {
                if (securityManager.setPIN(originalPin)) {
                    showToast(context.getString(R.string.pin_create_success), Toast.LENGTH_SHORT);
                    updateAuthenticationSummaries();
                } else {
                    showToast(context.getString(R.string.pin_create_failed), Toast.LENGTH_SHORT);
                }
            } else {
                showToast(context.getString(R.string.pin_confirm_mismatch), Toast.LENGTH_SHORT);
                showConfirmPinDialog(originalPin); // Show again
            }
        });
        
        builder.setNegativeButton(context.getString(R.string.pin_confirm_cancel), null);
        builder.show();
    }

    /**
     * Show change PIN dialog
     */
    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.pin_change_title));
        builder.setMessage(context.getString(R.string.pin_change_message));
        
        final EditText currentPinInput = new EditText(context);
        currentPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        currentPinInput.setHint(context.getString(R.string.pin_change_current_hint));
        builder.setView(currentPinInput);
        
        builder.setPositiveButton(context.getString(R.string.pin_change_verify), (dialog, which) -> {
            String currentPin = currentPinInput.getText().toString().trim();
            if (securityManager.verifyPIN(currentPin)) {
                showCreatePinDialog(); // Show new PIN creation
            } else {
                showToast(context.getString(R.string.pin_change_wrong_current), Toast.LENGTH_SHORT);
            }
        });
        
        builder.setNegativeButton(context.getString(R.string.pin_change_cancel), null);
        builder.show();
    }

    /**
     * Show remove PIN dialog
     */
    private void showRemovePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.pin_remove_title));
        builder.setMessage(context.getString(R.string.pin_remove_message));
        
        final EditText pinInput = new EditText(context);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | 
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setHint(context.getString(R.string.pin_remove_hint));
        builder.setView(pinInput);
        
        builder.setPositiveButton(context.getString(R.string.pin_remove_confirm), (dialog, which) -> {
            String pin = pinInput.getText().toString().trim();
            if (securityManager.verifyPIN(pin)) {
                securityManager.clearPinData();
                showToast(context.getString(R.string.pin_remove_success), Toast.LENGTH_SHORT);
                updateAuthenticationSummaries();
            } else {
                showToast(context.getString(R.string.pin_remove_wrong), Toast.LENGTH_SHORT);
            }
        });
        
        builder.setNegativeButton(context.getString(R.string.pin_remove_cancel), null);
        builder.show();
    }

    /**
     * Test security authentication
     */
    private void testSecurityAuthentication() {
        if (!securityManager.isSecurityEnabled()) {
            showToast(context.getString(R.string.security_test_disabled), Toast.LENGTH_SHORT);
            return;
        }

        Intent authIntent = AuthenticationActivity.createIntent(context, 
            AuthenticationActivity.AUTH_TYPE_SETUP);
        startActivity(authIntent);
    }

    /**
     * Show rate limit status dialog
     */
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
            boolean rateLimitEnabled = getBooleanPreference(
                    context.getString(R.string.key_enable_rate_limiting), true);

            if (!rateLimitEnabled) {
                message.append("\n\n⚠️ Rate limiting is currently DISABLED in settings.");
            }

            showToast(message.toString(), Toast.LENGTH_LONG);

            // Update the preference summary
            Preference rateLimitStatusPreference = getPreferenceByStringRes(R.string.key_rate_limit_status);
            if (rateLimitStatusPreference != null) {
                updateRateLimitStatusSummary(rateLimitStatusPreference);
            }

        } catch (Exception e) {
            showToast("Error reading rate limit status: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }
}