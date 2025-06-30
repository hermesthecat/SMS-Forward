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
import com.keremgok.smsforward.TelegramForwarder;

/**
 * Fragment for Telegram forwarder configuration
 */
public class TelegramForwarderTabFragment extends Fragment {
    private static final String TAG = "TelegramForwarderTabFragment";
    
    private SwitchMaterial switchEnableTelegram;
    private MaterialCardView cardTelegramConfig;
    private TextInputEditText editTelegramToken;
    private TextInputEditText editTelegramChatId;
    private MaterialButton btnTestTelegram;
    
    private SharedPreferences preferences;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_forwarder_telegram, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        loadPreferences();
    }
    
    private void initViews(View view) {
        switchEnableTelegram = view.findViewById(R.id.switch_enable_telegram);
        cardTelegramConfig = view.findViewById(R.id.card_telegram_config);
        editTelegramToken = view.findViewById(R.id.edit_telegram_token);
        editTelegramChatId = view.findViewById(R.id.edit_telegram_chat_id);
        btnTestTelegram = view.findViewById(R.id.btn_test_telegram);
    }
    
    private void setupListeners() {
        switchEnableTelegram.setOnCheckedChangeListener((button, isChecked) -> {
            preferences.edit()
                .putBoolean(getString(R.string.key_enable_telegram), isChecked)
                .apply();
            updateConfigCardVisibility();
        });
        
        editTelegramToken.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String token = editTelegramToken.getText().toString().trim();
                preferences.edit()
                    .putString(getString(R.string.key_telegram_apikey), token)
                    .apply();
            }
        });
        
        editTelegramChatId.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String chatId = editTelegramChatId.getText().toString().trim();
                preferences.edit()
                    .putString(getString(R.string.key_target_telegram), chatId)
                    .apply();
            }
        });
        
        btnTestTelegram.setOnClickListener(v -> sendTestMessage());
    }
    
    private void loadPreferences() {
        boolean enableTelegram = preferences.getBoolean(getString(R.string.key_enable_telegram), false);
        String token = preferences.getString(getString(R.string.key_telegram_apikey), "");
        String chatId = preferences.getString(getString(R.string.key_target_telegram), "");
        
        switchEnableTelegram.setChecked(enableTelegram);
        editTelegramToken.setText(token);
        editTelegramChatId.setText(chatId);
        
        updateConfigCardVisibility();
    }
    
    private void updateConfigCardVisibility() {
        boolean isEnabled = switchEnableTelegram.isChecked();
        cardTelegramConfig.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        btnTestTelegram.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }
    
    private void sendTestMessage() {
        String token = editTelegramToken.getText().toString().trim();
        String chatId = editTelegramChatId.getText().toString().trim();
        
        if (token.isEmpty()) {
            Toast.makeText(getContext(), "Please enter bot token", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (chatId.isEmpty()) {
            Toast.makeText(getContext(), "Please enter chat ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String testMessage = "Test message from SMS Forward app. Telegram forwarding is working correctly!";
            TelegramForwarder forwarder = new TelegramForwarder(token, chatId, getContext());
            
            // Use RetryableForwarder for better reliability
            RetryableForwarder retryableForwarder = new RetryableForwarder(forwarder);
            retryableForwarder.forward("+1234567890", testMessage);
            
            Toast.makeText(getContext(), "Test Telegram message sent successfully!", Toast.LENGTH_SHORT).show();
            
            // Cleanup
            retryableForwarder.shutdown();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send test Telegram message: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Save current values when fragment goes to background
        String token = editTelegramToken.getText().toString().trim();
        String chatId = editTelegramChatId.getText().toString().trim();
        
        preferences.edit()
            .putString(getString(R.string.key_telegram_apikey), token)
            .putString(getString(R.string.key_target_telegram), chatId)
            .apply();
    }
} 