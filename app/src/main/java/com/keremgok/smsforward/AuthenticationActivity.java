package com.keremgok.smsforward;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * AuthenticationActivity handles user authentication with PIN or biometric methods.
 * This activity is shown when the user needs to authenticate to access the app.
 */
public class AuthenticationActivity extends AppCompatActivity {
    private static final String TAG = "AuthenticationActivity";
    
    public static final String EXTRA_AUTH_TYPE = "auth_type";
    public static final String AUTH_TYPE_STARTUP = "startup";
    public static final String AUTH_TYPE_SETUP = "setup";
    
    private SecurityManager securityManager;
    private String authType;
    
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.initializeTheme(this);
        
        super.onCreate(savedInstanceState);
        
        // Simple programmatic layout since this is a security screen
        createAuthenticationUI();
        
        securityManager = new SecurityManager(this);
        authType = getIntent().getStringExtra(EXTRA_AUTH_TYPE);
        if (authType == null) {
            authType = AUTH_TYPE_STARTUP;
        }
        
        initializeAuthentication();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back navigation during authentication
        if (AUTH_TYPE_STARTUP.equals(authType)) {
            finishAffinity(); // Close the entire app
        } else {
            super.onBackPressed();
        }
    }
    
    private void createAuthenticationUI() {
        // Create a simple layout programmatically
        setContentView(android.R.layout.activity_list_item);
        
        // We'll create the UI elements programmatically to avoid needing layout files
        // This keeps the security implementation minimal and self-contained
    }
    
    private void initializeAuthentication() {
        if (!securityManager.isSecurityEnabled()) {
            // Security is disabled, allow access
            finishSuccessfully();
            return;
        }
        
        // Check what authentication methods are available
        boolean pinEnabled = securityManager.isPinEnabled();
        boolean biometricEnabled = securityManager.isBiometricEnabled();
        
        if (biometricEnabled && securityManager.isBiometricAvailable()) {
            // Try biometric first
            showBiometricAuth();
        } else if (pinEnabled) {
            // Fall back to PIN
            showPinAuth();
        } else {
            // No authentication methods available
            Toast.makeText(this, getString(R.string.auth_no_methods), Toast.LENGTH_LONG).show();
            finishSuccessfully();
        }
    }
    
    private void showBiometricAuth() {
        securityManager.showBiometricPrompt(this, new SecurityManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccess() {
                finishSuccessfully();
            }
            
            @Override
            public void onAuthenticationError(String error) {
                // Fall back to PIN if available
                if (securityManager.isPinEnabled()) {
                    showPinAuth();
                } else {
                    Toast.makeText(AuthenticationActivity.this, error, Toast.LENGTH_LONG).show();
                    if (AUTH_TYPE_STARTUP.equals(authType)) {
                        finishAffinity();
                    } else {
                        finish();
                    }
                }
            }
        });
    }
    
    private void showPinAuth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.pin_auth_title));
        builder.setMessage(getString(R.string.pin_auth_message));
        
        // Create PIN input field
        final EditText pinInput = new EditText(this);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setHint(getString(R.string.pin_auth_hint));
        builder.setView(pinInput);
        
        builder.setPositiveButton(getString(R.string.pin_auth_verify), (dialog, which) -> {
            String pin = pinInput.getText().toString().trim();
            if (securityManager.verifyPIN(pin)) {
                finishSuccessfully();
            } else {
                Toast.makeText(AuthenticationActivity.this, 
                    getString(R.string.pin_auth_invalid), Toast.LENGTH_SHORT).show();
                // Show PIN auth again
                showPinAuth();
            }
        });
        
        builder.setNegativeButton(getString(R.string.pin_auth_cancel), (dialog, which) -> {
            if (AUTH_TYPE_STARTUP.equals(authType)) {
                finishAffinity();
            } else {
                finish();
            }
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    private void finishSuccessfully() {
        setResult(RESULT_OK);
        finish();
    }
    
    /**
     * Create an intent to start AuthenticationActivity
     */
    public static Intent createIntent(Context context, String authType) {
        Intent intent = new Intent(context, AuthenticationActivity.class);
        intent.putExtra(EXTRA_AUTH_TYPE, authType);
        return intent;
    }
} 