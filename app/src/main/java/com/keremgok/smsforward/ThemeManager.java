package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Manages application theme switching between light, dark, and system default
 * modes.
 * Uses AppCompatDelegate to apply themes system-wide.
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";

    // Theme mode constants
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    public static final String DEFAULT_THEME = THEME_SYSTEM;

    private final Context context;
    private final SharedPreferences preferences;

    public ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    /**
     * Apply the currently selected theme
     */
    public void applyTheme() {
        String themeMode = getCurrentThemeMode();
        applyTheme(themeMode);
    }

    /**
     * Apply a specific theme mode
     * 
     * @param themeMode Theme mode: "light", "dark", or "system"
     */
    public void applyTheme(String themeMode) {
        int nightMode;

        switch (themeMode) {
            case THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case THEME_SYSTEM:
            default:
                // Use system default (requires API 29+, fallback to battery saver for older
                // versions)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                } else {
                    // For older devices, follow battery saver mode
                    nightMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
                }
                break;
        }

        // Apply the theme
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /**
     * Get the currently selected theme mode from preferences
     * 
     * @return Current theme mode string
     */
    public String getCurrentThemeMode() {
        String themeKey = context.getString(R.string.key_theme_mode);
        return preferences.getString(themeKey, DEFAULT_THEME);
    }

    /**
     * Set the theme mode and apply it
     * 
     * @param themeMode Theme mode to set and apply
     */
    public void setThemeMode(String themeMode) {
        // Save preference
        String themeKey = context.getString(R.string.key_theme_mode);
        preferences.edit().putString(themeKey, themeMode).apply();

        // Apply immediately
        applyTheme(themeMode);
    }

    /**
     * Check if the current theme is dark mode
     * Note: This checks the actual applied theme, not just the preference
     * 
     * @return true if dark mode is currently active
     */
    public boolean isDarkMode() {
        int nightMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Get a user-friendly description of the current theme
     * 
     * @return Human-readable theme description
     */
    public String getCurrentThemeDescription() {
        String currentMode = getCurrentThemeMode();
        switch (currentMode) {
            case THEME_LIGHT:
                return "🌞 Light mode";
            case THEME_DARK:
                return "🌙 Dark mode";
            case THEME_SYSTEM:
                if (isDarkMode()) {
                    return "🔄 System (Dark)";
                } else {
                    return "🔄 System (Light)";
                }
            default:
                return "Unknown";
        }
    }

    /**
     * Initialize theme on application startup
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     */
    public static void initializeTheme(Context context) {
        ThemeManager themeManager = new ThemeManager(context);
        themeManager.applyTheme();
    }

    /**
     * Check if system dark mode is supported on this device
     * 
     * @return true if system dark mode detection is supported
     */
    public static boolean isSystemDarkModeSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    /**
     * Get the fallback behavior description for older devices
     * 
     * @return Description of fallback behavior
     */
    public static String getFallbackDescription() {
        if (isSystemDarkModeSupported()) {
            return "Follows system theme setting";
        } else {
            return "Follows battery saver mode (Android 9 and below)";
        }
    }
}