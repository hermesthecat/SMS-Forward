package com.keremgok.smsforward;

import android.app.Application;

/**
 * Custom Application class to initialize language settings at app startup
 */
public class SmsForwardApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize language settings
        LanguageManager languageManager = new LanguageManager(this);
        languageManager.applyLanguage();
    }
} 