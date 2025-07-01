package com.keremgok.smsforward;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Custom Application class to initialize language settings at app startup
 * and perform cleanup on termination
 */
public class SmsForwardApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageManager.wrapContext(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Also apply language to the base context
        LanguageManager languageManager = new LanguageManager(this);
        languageManager.applyLanguage();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Language is now initialized in attachBaseContext.
        // The rest of the app initialization can go here.
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Cleanup global resources to prevent memory leaks
        // Note: onTerminate() is not guaranteed to be called on all devices,
        // but it's good practice to include cleanup code here

        // Stop network monitoring if it's still active
        try {
            NetworkStatusManager networkManager = NetworkStatusManager.getInstance(this);
            networkManager.stopMonitoring();
        } catch (Exception e) {
            // Ignore errors during cleanup
        }
    }
}