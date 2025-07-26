package com.keremgok.smsforward;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Fragment for Platform configuration containing:
 * - SMS forwarding settings (enable, target number)
 * - Telegram forwarding settings (enable, target ID, API key)
 * - Email forwarding settings (enable, SMTP configuration)
 * - Web forwarding settings (enable, target URL)
 */
public class PlatformsFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.platforms_preferences, rootKey);

        // Set up all platform preferences
        setupSmsPreferences();
        setupTelegramPreferences();
        setupEmailPreferences();
        setupWebPreferences();
    }

    @Override
    protected void updatePreferenceSummaries() {
        updateSmsPreferenceSummaries();
        updateTelegramPreferenceSummaries();
        updateEmailPreferenceSummaries();
        updateWebPreferenceSummaries();
    }

    @Override
    protected void updatePreferenceSummaryForKey(String key) {
        if (context == null) return;

        // SMS keys
        String smsEnableKey = context.getString(R.string.key_enable_sms);
        String smsTargetKey = context.getString(R.string.key_target_sms);

        // Telegram keys
        String telegramEnableKey = context.getString(R.string.key_enable_telegram);
        String telegramTargetKey = context.getString(R.string.key_target_telegram);
        String telegramApiKey = context.getString(R.string.key_telegram_apikey);

        // Email keys
        String emailEnableKey = context.getString(R.string.key_enable_email);
        String emailFromKey = context.getString(R.string.key_email_from_address);
        String emailToKey = context.getString(R.string.key_email_to_address);
        String emailHostKey = context.getString(R.string.key_email_submit_host);
        String emailPortKey = context.getString(R.string.key_email_submit_port);
        String emailUsernameStyleKey = context.getString(R.string.key_email_username_style);
        String emailPasswordKey = context.getString(R.string.key_email_submit_password);

        // Web keys
        String webEnableKey = context.getString(R.string.key_enable_web);
        String webTargetKey = context.getString(R.string.key_target_web);

        // Update appropriate summaries based on changed key
        if (smsEnableKey.equals(key) || smsTargetKey.equals(key)) {
            updateSmsPreferenceSummaries();
        } else if (telegramEnableKey.equals(key) || telegramTargetKey.equals(key) || telegramApiKey.equals(key)) {
            updateTelegramPreferenceSummaries();
        } else if (emailEnableKey.equals(key) || emailFromKey.equals(key) || emailToKey.equals(key) ||
                   emailHostKey.equals(key) || emailPortKey.equals(key) || emailUsernameStyleKey.equals(key) ||
                   emailPasswordKey.equals(key)) {
            updateEmailPreferenceSummaries();
        } else if (webEnableKey.equals(key) || webTargetKey.equals(key)) {
            updateWebPreferenceSummaries();
        }
    }

    @Override
    protected void cleanupResources() {
        // No specific cleanup needed for Platforms fragment
        // Base class handles SharedPreferences cleanup
    }

    /**
     * Set up SMS forwarding preferences
     */
    private void setupSmsPreferences() {
        SwitchPreferenceCompat smsEnablePreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_sms);
        EditTextPreference smsTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_sms);

        if (smsEnablePreference != null) {
            smsEnablePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateSmsPreferenceSummaries();
                return true;
            });
        }

        if (smsTargetPreference != null) {
            updateSmsTargetSummary(smsTargetPreference);
            smsTargetPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String target = (String) newValue;
                updateSmsTargetSummary((EditTextPreference) preference, target);
                return true;
            });
        }
    }

    /**
     * Set up Telegram forwarding preferences
     */
    private void setupTelegramPreferences() {
        SwitchPreferenceCompat telegramEnablePreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_telegram);
        EditTextPreference telegramTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_telegram);
        EditTextPreference telegramApiPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_telegram_apikey);

        if (telegramEnablePreference != null) {
            telegramEnablePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateTelegramPreferenceSummaries();
                return true;
            });
        }

        if (telegramTargetPreference != null) {
            updateTelegramTargetSummary(telegramTargetPreference);
            telegramTargetPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String target = (String) newValue;
                updateTelegramTargetSummary((EditTextPreference) preference, target);
                return true;
            });
        }

        if (telegramApiPreference != null) {
            updateTelegramApiSummary(telegramApiPreference);
            telegramApiPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String apiKey = (String) newValue;
                updateTelegramApiSummary((EditTextPreference) preference, apiKey);
                return true;
            });
        }
    }

    /**
     * Set up Email forwarding preferences
     */
    private void setupEmailPreferences() {
        SwitchPreferenceCompat emailEnablePreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_email);
        EditTextPreference emailFromPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_from_address);
        EditTextPreference emailToPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_to_address);
        EditTextPreference emailHostPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_host);
        EditTextPreference emailPortPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_port);
        ListPreference emailUsernameStylePreference = 
                (ListPreference) getPreferenceByStringRes(R.string.key_email_username_style);
        EditTextPreference emailPasswordPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_password);

        if (emailEnablePreference != null) {
            emailEnablePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateEmailPreferenceSummaries();
                return true;
            });
        }

        setupEmailFieldPreference(emailFromPreference, "email address");
        setupEmailFieldPreference(emailToPreference, "email address");
        setupEmailFieldPreference(emailHostPreference, "SMTP server");
        setupEmailFieldPreference(emailPortPreference, "port number");
        setupEmailFieldPreference(emailPasswordPreference, "password");

        if (emailUsernameStylePreference != null) {
            updateEmailUsernameStyleSummary(emailUsernameStylePreference);
            emailUsernameStylePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateEmailUsernameStyleSummary((ListPreference) preference);
                return true;
            });
        }
    }

    /**
     * Set up Web forwarding preferences
     */
    private void setupWebPreferences() {
        SwitchPreferenceCompat webEnablePreference = 
                (SwitchPreferenceCompat) getPreferenceByStringRes(R.string.key_enable_web);
        EditTextPreference webTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_web);

        if (webEnablePreference != null) {
            webEnablePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateWebPreferenceSummaries();
                return true;
            });
        }

        if (webTargetPreference != null) {
            updateWebTargetSummary(webTargetPreference);
            webTargetPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String target = (String) newValue;
                updateWebTargetSummary((EditTextPreference) preference, target);
                return true;
            });
        }
    }

    /**
     * Helper method to set up email field preferences
     */
    private void setupEmailFieldPreference(EditTextPreference preference, String fieldType) {
        if (preference != null) {
            updateEmailFieldSummary(preference, fieldType);
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                String value = (String) newValue;
                updateEmailFieldSummary((EditTextPreference) pref, fieldType, value);
                return true;
            });
        }
    }

    /**
     * Update all SMS preference summaries
     */
    private void updateSmsPreferenceSummaries() {
        EditTextPreference smsTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_sms);
        if (smsTargetPreference != null) {
            updateSmsTargetSummary(smsTargetPreference);
        }
    }

    /**
     * Update SMS target summary
     */
    private void updateSmsTargetSummary(EditTextPreference preference) {
        updateSmsTargetSummary(preference, preference.getText());
    }

    private void updateSmsTargetSummary(EditTextPreference preference, String target) {
        if (target == null || target.trim().isEmpty()) {
            preference.setSummary("Not configured");
        } else {
            preference.setSummary("Target: " + target);
        }
    }

    /**
     * Update all Telegram preference summaries
     */
    private void updateTelegramPreferenceSummaries() {
        EditTextPreference telegramTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_telegram);
        EditTextPreference telegramApiPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_telegram_apikey);

        if (telegramTargetPreference != null) {
            updateTelegramTargetSummary(telegramTargetPreference);
        }
        if (telegramApiPreference != null) {
            updateTelegramApiSummary(telegramApiPreference);
        }
    }

    /**
     * Update Telegram target summary
     */
    private void updateTelegramTargetSummary(EditTextPreference preference) {
        updateTelegramTargetSummary(preference, preference.getText());
    }

    private void updateTelegramTargetSummary(EditTextPreference preference, String target) {
        if (target == null || target.trim().isEmpty()) {
            preference.setSummary("Not configured");
        } else {
            preference.setSummary("Target ID: " + target);
        }
    }

    /**
     * Update Telegram API key summary
     */
    private void updateTelegramApiSummary(EditTextPreference preference) {
        updateTelegramApiSummary(preference, preference.getText());
    }

    private void updateTelegramApiSummary(EditTextPreference preference, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            preference.setSummary("Not configured");
        } else {
            // Show only first few characters for security
            String maskedKey = apiKey.length() > 10 ? 
                    apiKey.substring(0, 10) + "..." : 
                    apiKey.substring(0, Math.min(apiKey.length(), 6)) + "...";
            preference.setSummary("API Key: " + maskedKey);
        }
    }

    /**
     * Update all Email preference summaries
     */
    private void updateEmailPreferenceSummaries() {
        EditTextPreference emailFromPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_from_address);
        EditTextPreference emailToPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_to_address);
        EditTextPreference emailHostPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_host);
        EditTextPreference emailPortPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_port);
        EditTextPreference emailPasswordPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_email_submit_password);
        ListPreference emailUsernameStylePreference = 
                (ListPreference) getPreferenceByStringRes(R.string.key_email_username_style);

        updateEmailFieldSummary(emailFromPreference, "email address");
        updateEmailFieldSummary(emailToPreference, "email address");
        updateEmailFieldSummary(emailHostPreference, "SMTP server");
        updateEmailFieldSummary(emailPortPreference, "port number");
        updateEmailFieldSummary(emailPasswordPreference, "password");
        updateEmailUsernameStyleSummary(emailUsernameStylePreference);
    }

    /**
     * Update email field summary
     */
    private void updateEmailFieldSummary(EditTextPreference preference, String fieldType) {
        if (preference != null) {
            updateEmailFieldSummary(preference, fieldType, preference.getText());
        }
    }

    private void updateEmailFieldSummary(EditTextPreference preference, String fieldType, String value) {
        if (value == null || value.trim().isEmpty()) {
            preference.setSummary("Not configured");
        } else {
            if (fieldType.equals("password")) {
                preference.setSummary("Password: " + "●".repeat(Math.min(value.length(), 8)));
            } else {
                preference.setSummary(value);
            }
        }
    }

    /**
     * Update email username style summary
     */
    private void updateEmailUsernameStyleSummary(ListPreference preference) {
        if (preference != null) {
            CharSequence entry = preference.getEntry();
            if (entry != null) {
                preference.setSummary(entry);
            } else {
                preference.setSummary("Full RFC822 address");
            }
        }
    }

    /**
     * Update all Web preference summaries
     */
    private void updateWebPreferenceSummaries() {
        EditTextPreference webTargetPreference = 
                (EditTextPreference) getPreferenceByStringRes(R.string.key_target_web);
        if (webTargetPreference != null) {
            updateWebTargetSummary(webTargetPreference);
        }
    }

    /**
     * Update Web target summary
     */
    private void updateWebTargetSummary(EditTextPreference preference) {
        updateWebTargetSummary(preference, preference.getText());
    }

    private void updateWebTargetSummary(EditTextPreference preference, String target) {
        if (target == null || target.trim().isEmpty()) {
            preference.setSummary("Not configured");
        } else {
            preference.setSummary("URL: " + target);
        }
    }
}