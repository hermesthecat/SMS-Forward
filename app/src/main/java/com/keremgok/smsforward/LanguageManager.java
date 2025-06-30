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
     * Wraps the given context with the selected language configuration.
     * This method should be called in `attachBaseContext` of Application and Activity classes.
     *
     * @param context The base context.
     * @return A new context with the updated locale.
     */
    public static Context wrapContext(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String languageCode = prefs.getString(context.getString(R.string.key_language), "system");

        if ("system".equals(languageCode)) {
            return context;
        }

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        return context.createConfigurationContext(config);
    }

    /**
     * Apply the selected language to the application context
     */
    public void applyLanguage() {
        String languageCode = getSelectedLanguage();
        Locale locale;

        if ("system".equals(languageCode)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locale = Resources.getSystem().getConfiguration().getLocales().get(0);
            } else {
                locale = Resources.getSystem().getConfiguration().locale;
            }
        } else {
            locale = new Locale(languageCode);
        }

        setLocale(locale);
    }

    /**
     * Get the currently selected language code
     */
    public String getSelectedLanguage() {
        return preferences.getString(context.getString(R.string.key_language), "system");
    }

    /**
     * Set the app language and apply it.
     */
    public void setLanguage(String languageCode) {
        preferences.edit()
                .putString(context.getString(R.string.key_language), languageCode)
                .apply();
    }

    /**
     * Set the locale for the application.
     * This method is primarily for runtime changes and might not be fully effective
     * without recreating the Activity. The recommended approach is using `wrapContext`.
     */
    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
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