package com.keremgok.smsforward.ui.fragments.forwarders;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.keremgok.smsforward.JsonWebForwarder;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.RetryableForwarder;

/**
 * Fragment for Webhook forwarder configuration
 */
public class WebhookForwarderTabFragment extends Fragment {
    private static final String TAG = "WebhookForwarderTabFragment";
    
    private SwitchMaterial switchEnableWebhook;
    private MaterialCardView cardWebhookConfig;
    private TextInputEditText editWebhookUrl;
    private MaterialButton btnTestWebhook;
    
    private SharedPreferences preferences;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_forwarder_webhook, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        loadPreferences();
    }
    
    private void initViews(View view) {
        switchEnableWebhook = view.findViewById(R.id.switch_enable_webhook);
        cardWebhookConfig = view.findViewById(R.id.card_webhook_config);
        editWebhookUrl = view.findViewById(R.id.edit_webhook_url);
        btnTestWebhook = view.findViewById(R.id.btn_test_webhook);
    }
    
    private void setupListeners() {
        switchEnableWebhook.setOnCheckedChangeListener((button, isChecked) -> {
            preferences.edit()
                .putBoolean(getString(R.string.key_enable_web), isChecked)
                .apply();
            updateConfigCardVisibility();
        });
        
        editWebhookUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String url = editWebhookUrl.getText().toString().trim();
                preferences.edit()
                    .putString(getString(R.string.key_target_web), url)
                    .apply();
            }
        });
        
        btnTestWebhook.setOnClickListener(v -> sendTestMessage());
    }
    
    private void loadPreferences() {
        boolean enableWebhook = preferences.getBoolean(getString(R.string.key_enable_web), false);
        String webhookUrl = preferences.getString(getString(R.string.key_target_web), "");
        
        switchEnableWebhook.setChecked(enableWebhook);
        editWebhookUrl.setText(webhookUrl);
        
        updateConfigCardVisibility();
    }
    
    private void updateConfigCardVisibility() {
        boolean isEnabled = switchEnableWebhook.isChecked();
        cardWebhookConfig.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        btnTestWebhook.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }
    
    private void sendTestMessage() {
        String webhookUrl = editWebhookUrl.getText().toString().trim();
        
        if (webhookUrl.isEmpty()) {
            Toast.makeText(getContext(), "Please enter webhook URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
            Toast.makeText(getContext(), "Please enter a valid HTTP/HTTPS URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String testMessage = "Test message from SMS Forward app. Webhook forwarding is working correctly!";
            JsonWebForwarder forwarder = new JsonWebForwarder(webhookUrl);
            
            // Use RetryableForwarder for better reliability
            RetryableForwarder retryableForwarder = new RetryableForwarder(forwarder);
            retryableForwarder.forward("+1234567890", testMessage);
            
            Toast.makeText(getContext(), "Test webhook request sent successfully!", Toast.LENGTH_SHORT).show();
            
            // Cleanup
            retryableForwarder.shutdown();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send test webhook request: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Save current values when fragment goes to background
        String url = editWebhookUrl.getText().toString().trim();
        preferences.edit()
            .putString(getString(R.string.key_target_web), url)
            .apply();
    }
} 