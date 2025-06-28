package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Manages application language settings and locale configuration.
 * Provides functionality to change app language at runtime.
 */
public class LanguageManager {
    private static final String TAG = "LanguageManager";

    private final Context context;
    private final SharedPreferences preferences;

    public LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    /**
     * Apply the selected language to the application context
     */
    public void applyLanguage() {
        String languageCode = getSelectedLanguage();

        if ("system".equals(languageCode)) {
            // Use system default language
            return;
        }

        Locale locale = new Locale(languageCode);
        setLocale(locale);
    }

    /**
     * Get the currently selected language code
     */
    public String getSelectedLanguage() {
        return preferences.getString(context.getString(R.string.key_language), "system");
    }

    /**
     * Set the app language
     */
    public void setLanguage(String languageCode) {
        preferences.edit()
                .putString(context.getString(R.string.key_language), languageCode)
                .apply();

        if (!"system".equals(languageCode)) {
            Locale locale = new Locale(languageCode);
            setLocale(locale);
        }
    }

    /**
     * Set the locale for the application
     */
    private void setLocale(Locale locale) {
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, displayMetrics);

        // Also set the default locale for the JVM
        Locale.setDefault(locale);
    }

    /**
     * Get the display name for a language code
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "en":
                return "English";
            case "tr":
                return "Türkçe";
            case "system":
                return context.getString(R.string.language_title) + " (System)";
            default:
                return languageCode;
        }
    }

    /**
     * Check if the current language is the system default
     */
    public boolean isSystemLanguage() {
        return "system".equals(getSelectedLanguage());
    }

    /**
     * Get the current locale
     */
    public Locale getCurrentLocale() {
        String languageCode = getSelectedLanguage();

        if ("system".equals(languageCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return context.getResources().getConfiguration().getLocales().get(0);
            } else {
                return context.getResources().getConfiguration().locale;
            }
        }

        return new Locale(languageCode);
    }
}