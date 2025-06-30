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
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.RetryableForwarder;
import com.keremgok.smsforward.SmsForwarder;

/**
 * Fragment for SMS forwarder configuration
 */
public class SmsForwarderTabFragment extends Fragment {
    private static final String TAG = "SmsForwarderTabFragment";
    
    private SwitchMaterial switchEnableSms;
    private MaterialCardView cardSmsConfig;
    private TextInputEditText editTargetSms;
    private MaterialButton btnTestSms;
    
    private SharedPreferences preferences;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_forwarder_sms, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        loadPreferences();
    }
    
    private void initViews(View view) {
        switchEnableSms = view.findViewById(R.id.switch_enable_sms);
        cardSmsConfig = view.findViewById(R.id.card_sms_config);
        editTargetSms = view.findViewById(R.id.edit_target_sms);
        btnTestSms = view.findViewById(R.id.btn_test_sms);
    }
    
    private void setupListeners() {
        switchEnableSms.setOnCheckedChangeListener((button, isChecked) -> {
            preferences.edit()
                .putBoolean(getString(R.string.key_enable_sms), isChecked)
                .apply();
            updateConfigCardVisibility();
        });
        
        editTargetSms.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String target = editTargetSms.getText().toString().trim();
                preferences.edit()
                    .putString(getString(R.string.key_target_sms), target)
                    .apply();
            }
        });
        
        btnTestSms.setOnClickListener(v -> sendTestMessage());
    }
    
    private void loadPreferences() {
        boolean enableSms = preferences.getBoolean(getString(R.string.key_enable_sms), false);
        String targetSms = preferences.getString(getString(R.string.key_target_sms), "");
        
        switchEnableSms.setChecked(enableSms);
        editTargetSms.setText(targetSms);
        
        updateConfigCardVisibility();
    }
    
    private void updateConfigCardVisibility() {
        boolean isEnabled = switchEnableSms.isChecked();
        cardSmsConfig.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        btnTestSms.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }
    
    private void sendTestMessage() {
        String targetNumber = editTargetSms.getText().toString().trim();
        
        if (targetNumber.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a target phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String testMessage = "Test message from SMS Forward app. SMS forwarding is working correctly!";
            SmsForwarder forwarder = new SmsForwarder(targetNumber, getContext());
            
            // Use RetryableForwarder for better reliability
            RetryableForwarder retryableForwarder = new RetryableForwarder(forwarder);
            retryableForwarder.forward("+1234567890", testMessage);
            
            Toast.makeText(getContext(), "Test SMS sent successfully!", Toast.LENGTH_SHORT).show();
            
            // Cleanup
            retryableForwarder.shutdown();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send test SMS: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Save current values when fragment goes to background
        String target = editTargetSms.getText().toString().trim();
        preferences.edit()
            .putString(getString(R.string.key_target_sms), target)
            .apply();
    }
} 