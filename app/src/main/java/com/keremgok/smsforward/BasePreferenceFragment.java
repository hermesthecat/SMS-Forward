package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * Base class for all preference fragments in the multi-screen UI.
 * Provides common functionality for preference management, SharedPreferences listening,
 * and UI updates that are shared across all fragments.
 */
public abstract class BasePreferenceFragment extends PreferenceFragmentCompat 
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected SharedPreferences sharedPreferences;
    protected Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        // Update all preference summaries when fragment becomes active
        updatePreferenceSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up any resources to prevent memory leaks
        cleanupResources();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update specific preference summary when its value changes
        updatePreferenceSummaryForKey(key);
    }

    /**
     * Update all preference summaries in this fragment.
     * Should be implemented by each fragment to update their specific preferences.
     */
    protected abstract void updatePreferenceSummaries();

    /**
     * Update preference summary for a specific key.
     * Should be implemented by each fragment to handle their specific preferences.
     * 
     * @param key The preference key that changed
     */
    protected abstract void updatePreferenceSummaryForKey(String key);

    /**
     * Clean up any resources (database connections, listeners, etc.)
     * Should be implemented by each fragment to clean up their specific resources.
     */
    protected abstract void cleanupResources();

    /**
     * Helper method to safely get string preference value
     * 
     * @param key The preference key
     * @param defaultValue Default value if preference doesn't exist
     * @return The preference value or default
     */
    protected String getStringPreference(String key, String defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Helper method to safely get boolean preference value
     * 
     * @param key The preference key  
     * @param defaultValue Default value if preference doesn't exist
     * @return The preference value or default
     */
    protected boolean getBooleanPreference(String key, boolean defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Helper method to safely get int preference value
     * 
     * @param key The preference key
     * @param defaultValue Default value if preference doesn't exist  
     * @return The preference value or default
     */
    protected int getIntPreference(String key, int defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Helper method to safely set preference summary
     * 
     * @param preferenceKey The preference key to find
     * @param summary The summary to set
     */
    protected void setPreferenceSummary(String preferenceKey, String summary) {
        if (context != null) {
            Preference preference = findPreference(preferenceKey);
            if (preference != null) {
                preference.setSummary(summary);
            }
        }
    }

    /**
     * Helper method to safely get preference by key using string resource
     * 
     * @param stringResId String resource ID for the preference key
     * @return The preference or null if not found
     */
    protected Preference getPreferenceByStringRes(int stringResId) {
        if (context != null) {
            String key = context.getString(stringResId);
            return findPreference(key);
        }
        return null;
    }

    /**
     * Helper method to update preference summary using string resource
     * 
     * @param stringResId String resource ID for the preference key
     * @param summary The summary to set
     */
    protected void updatePreferenceSummaryByStringRes(int stringResId, String summary) {
        if (context != null) {
            String key = context.getString(stringResId);
            setPreferenceSummary(key, summary);
        }
    }

    /**
     * Helper method to check if a preference is enabled
     * 
     * @param stringResId String resource ID for the preference key
     * @param defaultValue Default value if preference doesn't exist
     * @return True if preference is enabled, false otherwise
     */
    protected boolean isPreferenceEnabled(int stringResId, boolean defaultValue) {
        if (context != null) {
            String key = context.getString(stringResId);
            return getBooleanPreference(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Helper method to show a toast message safely
     * 
     * @param message The message to show
     * @param duration Toast duration (Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     */
    protected void showToast(String message, int duration) {
        if (context != null) {
            android.widget.Toast.makeText(context, message, duration).show();
        }
    }
}