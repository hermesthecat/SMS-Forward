package com.keremgok.smsforward;

import android.app.Application;

/**
 * Custom Application class to initialize language settings at app startup
 * and perform cleanup on termination
 */
public class SmsForwardApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize language settings
        LanguageManager languageManager = new LanguageManager(this);
        languageManager.applyLanguage();
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