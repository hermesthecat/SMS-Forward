package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.Executor;

/**
 * SecurityManager handles PIN and biometric authentication for the application.
 * Provides secure storage for PIN hashes and biometric key management.
 */
public class SecurityManager {
    private static final String TAG = "SecurityManager";
    
    // Preference keys
    private static final String PREF_SECURITY_ENABLED = "security_enabled";
    private static final String PREF_PIN_ENABLED = "pin_enabled";
    private static final String PREF_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String PREF_PIN_HASH = "pin_hash";
    private static final String PREF_PIN_SALT = "pin_salt";
    private static final String PREF_LAST_AUTH_TIME = "last_auth_time";
    private static final String PREF_AUTH_TIMEOUT = "auth_timeout";
    
    // Keystore
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String BIOMETRIC_KEY_ALIAS = "sms_forward_biometric_key";
    
    // Default timeout (5 minutes)
    private static final long DEFAULT_AUTH_TIMEOUT_MS = 5 * 60 * 1000;
    
    private final Context context;
    private final SharedPreferences preferences;
    
    public SecurityManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
    }
    
    /**
     * Check if any security method is enabled
     */
    public boolean isSecurityEnabled() {
        return preferences.getBoolean(PREF_SECURITY_ENABLED, false);
    }
    
    /**
     * Check if PIN authentication is enabled
     */
    public boolean isPinEnabled() {
        return preferences.getBoolean(PREF_PIN_ENABLED, false);
    }
    
    /**
     * Check if biometric authentication is enabled
     */
    public boolean isBiometricEnabled() {
        return preferences.getBoolean(PREF_BIOMETRIC_ENABLED, false);
    }
    
    /**
     * Enable or disable security completely
     */
    public void setSecurityEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_SECURITY_ENABLED, enabled);
        
        if (!enabled) {
            // Disable all security methods
            editor.putBoolean(PREF_PIN_ENABLED, false);
            editor.putBoolean(PREF_BIOMETRIC_ENABLED, false);
            clearPinData();
            clearBiometricKey();
        }
        
        editor.apply();
    }
    
    /**
     * Set a new PIN
     */
    public boolean setPIN(String pin) {
        if (pin == null || pin.length() < 4) {
            return false; // PIN must be at least 4 digits
        }
        
        try {
            // Generate a random salt
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            
            // Hash the PIN with salt
            String hashedPin = hashPinWithSalt(pin, salt);
            
            // Store hash and salt
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_PIN_HASH, hashedPin);
            editor.putString(PREF_PIN_SALT, Base64.encodeToString(salt, Base64.DEFAULT));
            editor.putBoolean(PREF_PIN_ENABLED, true);
            editor.putBoolean(PREF_SECURITY_ENABLED, true);
            editor.apply();
            
            Log.i(TAG, "PIN set successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set PIN", e);
            return false;
        }
    }
    
    /**
     * Verify a PIN
     */
    public boolean verifyPIN(String pin) {
        if (!isPinEnabled()) {
            return false;
        }
        
        try {
            String storedHash = preferences.getString(PREF_PIN_HASH, "");
            String storedSalt = preferences.getString(PREF_PIN_SALT, "");
            
            if (storedHash.isEmpty() || storedSalt.isEmpty()) {
                return false;
            }
            
            byte[] salt = Base64.decode(storedSalt, Base64.DEFAULT);
            String inputHash = hashPinWithSalt(pin, salt);
            
            boolean isValid = storedHash.equals(inputHash);
            if (isValid) {
                recordSuccessfulAuth();
            }
            
            return isValid;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify PIN", e);
            return false;
        }
    }
    
    /**
     * Clear PIN data
     */
    public void clearPinData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_PIN_HASH);
        editor.remove(PREF_PIN_SALT);
        editor.putBoolean(PREF_PIN_ENABLED, false);
        editor.apply();
        
        Log.i(TAG, "PIN data cleared");
    }
    
    /**
     * Check if biometric authentication is available on device
     */
    public boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }
    
    /**
     * Get biometric availability status message
     */
    public String getBiometricStatusMessage() {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        
        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return context.getString(R.string.biometric_available);
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return context.getString(R.string.biometric_no_hardware);
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return context.getString(R.string.biometric_hw_unavailable);
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return context.getString(R.string.biometric_none_enrolled);
            default:
                return context.getString(R.string.biometric_unavailable);
        }
    }
    
    /**
     * Enable biometric authentication
     */
    public boolean enableBiometric() {
        if (!isBiometricAvailable()) {
            return false;
        }
        
        try {
            // Generate a key for biometric encryption
            generateBiometricKey();
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_BIOMETRIC_ENABLED, true);
            editor.putBoolean(PREF_SECURITY_ENABLED, true);
            editor.apply();
            
            Log.i(TAG, "Biometric authentication enabled");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable biometric authentication", e);
            return false;
        }
    }
    
    /**
     * Disable biometric authentication
     */
    public void disableBiometric() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_BIOMETRIC_ENABLED, false);
        editor.apply();
        
        clearBiometricKey();
        Log.i(TAG, "Biometric authentication disabled");
    }
    
    /**
     * Show biometric prompt
     */
    public void showBiometricPrompt(FragmentActivity activity, AuthenticationCallback callback) {
        if (!isBiometricEnabled() || !isBiometricAvailable()) {
            callback.onAuthenticationError("Biometric authentication not available");
            return;
        }
        
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.biometric_prompt_title))
                .setSubtitle(context.getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(context.getString(R.string.biometric_prompt_cancel))
                .build();
        
        Executor executor = ContextCompat.getMainExecutor(context);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, 
                new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onAuthenticationError(errString.toString());
            }
            
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                recordSuccessfulAuth();
                callback.onAuthenticationSuccess();
            }
            
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onAuthenticationError(context.getString(R.string.biometric_auth_failed));
            }
        });
        
        biometricPrompt.authenticate(promptInfo);
    }
    
    /**
     * Check if user needs to authenticate (based on timeout)
     */
    public boolean needsAuthentication() {
        if (!isSecurityEnabled()) {
            return false;
        }
        
        long lastAuthTime = preferences.getLong(PREF_LAST_AUTH_TIME, 0);
        long authTimeout = preferences.getLong(PREF_AUTH_TIMEOUT, DEFAULT_AUTH_TIMEOUT_MS);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastAuthTime) > authTimeout;
    }
    
    /**
     * Record successful authentication
     */
    public void recordSuccessfulAuth() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREF_LAST_AUTH_TIME, System.currentTimeMillis());
        editor.apply();
        
        Log.i(TAG, "Successful authentication recorded");
    }
    
    /**
     * Set authentication timeout
     */
    public void setAuthTimeout(long timeoutMs) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREF_AUTH_TIMEOUT, timeoutMs);
        editor.apply();
    }
    
    /**
     * Get authentication timeout
     */
    public long getAuthTimeout() {
        return preferences.getLong(PREF_AUTH_TIMEOUT, DEFAULT_AUTH_TIMEOUT_MS);
    }
    
    /**
     * Hash PIN with salt using SHA-256
     */
    private String hashPinWithSalt(String pin, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        byte[] hashedBytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT);
    }
    
    /**
     * Generate biometric key for encryption
     */
    private void generateBiometricKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .build();
        
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }
    
    /**
     * Clear biometric key
     */
    private void clearBiometricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS);
            Log.i(TAG, "Biometric key cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear biometric key", e);
        }
    }
    
    /**
     * Authentication callback interface
     */
    public interface AuthenticationCallback {
        void onAuthenticationSuccess();
        void onAuthenticationError(String error);
    }
} 