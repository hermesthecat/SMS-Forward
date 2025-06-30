package com.keremgok.smsforward.ui.fragments.forwarders;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.keremgok.smsforward.EmailForwarder;
import com.keremgok.smsforward.R;
import com.keremgok.smsforward.RetryableForwarder;

import jakarta.mail.internet.InternetAddress;

/**
 * Fragment for Email forwarder configuration
 */
public class EmailForwarderTabFragment extends Fragment {
    private static final String TAG = "EmailForwarderTabFragment";
    
    private SwitchMaterial switchEnableEmail;
    private MaterialCardView cardEmailConfig;
    private TextInputEditText editEmailFrom;
    private TextInputEditText editEmailTo;
    private TextInputEditText editEmailHost;
    private TextInputEditText editEmailPort;
    private AutoCompleteTextView spinnerUsernameStyle;
    private TextInputEditText editEmailPassword;
    private MaterialButton btnTestEmail;
    
    private SharedPreferences preferences;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_forwarder_email, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupSpinner();
        setupListeners();
        loadPreferences();
    }
    
    private void initViews(View view) {
        switchEnableEmail = view.findViewById(R.id.switch_enable_email);
        cardEmailConfig = view.findViewById(R.id.card_email_config);
        editEmailFrom = view.findViewById(R.id.edit_email_from);
        editEmailTo = view.findViewById(R.id.edit_email_to);
        editEmailHost = view.findViewById(R.id.edit_email_host);
        editEmailPort = view.findViewById(R.id.edit_email_port);
        spinnerUsernameStyle = view.findViewById(R.id.spinner_username_style);
        editEmailPassword = view.findViewById(R.id.edit_email_password);
        btnTestEmail = view.findViewById(R.id.btn_test_email);
    }
    
    private void setupSpinner() {
        String[] usernameStyles = {"Full Email", "Username Only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
            android.R.layout.simple_dropdown_item_1line, usernameStyles);
        spinnerUsernameStyle.setAdapter(adapter);
    }
    
    private void setupListeners() {
        switchEnableEmail.setOnCheckedChangeListener((button, isChecked) -> {
            preferences.edit()
                .putBoolean(getString(R.string.key_enable_email), isChecked)
                .apply();
            updateConfigCardVisibility();
        });
        
        editEmailFrom.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveEmailSettings();
        });
        
        editEmailTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveEmailSettings();
        });
        
        editEmailHost.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveEmailSettings();
        });
        
        editEmailPort.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveEmailSettings();
        });
        
        editEmailPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveEmailSettings();
        });
        
        spinnerUsernameStyle.setOnItemClickListener((parent, view, position, id) -> {
            String selected = position == 0 ? "full" : "username";
            preferences.edit()
                .putString(getString(R.string.key_email_username_style), selected)
                .apply();
        });
        
        btnTestEmail.setOnClickListener(v -> sendTestMessage());
    }
    
    private void loadPreferences() {
        boolean enableEmail = preferences.getBoolean(getString(R.string.key_enable_email), false);
        String fromAddress = preferences.getString(getString(R.string.key_email_from_address), "");
        String toAddress = preferences.getString(getString(R.string.key_email_to_address), "");
        String host = preferences.getString(getString(R.string.key_email_submit_host), "");
        String port = preferences.getString(getString(R.string.key_email_submit_port), "587");
        String password = preferences.getString(getString(R.string.key_email_submit_password), "");
        String usernameStyle = preferences.getString(getString(R.string.key_email_username_style), "full");
        
        switchEnableEmail.setChecked(enableEmail);
        editEmailFrom.setText(fromAddress);
        editEmailTo.setText(toAddress);
        editEmailHost.setText(host);
        editEmailPort.setText(port);
        editEmailPassword.setText(password);
        spinnerUsernameStyle.setText(usernameStyle.equals("full") ? "Full Email" : "Username Only", false);
        
        updateConfigCardVisibility();
    }
    
    private void saveEmailSettings() {
        String fromAddress = editEmailFrom.getText().toString().trim();
        String toAddress = editEmailTo.getText().toString().trim();
        String host = editEmailHost.getText().toString().trim();
        String port = editEmailPort.getText().toString().trim();
        String password = editEmailPassword.getText().toString().trim();
        
        preferences.edit()
            .putString(getString(R.string.key_email_from_address), fromAddress)
            .putString(getString(R.string.key_email_to_address), toAddress)
            .putString(getString(R.string.key_email_submit_host), host)
            .putString(getString(R.string.key_email_submit_port), port)
            .putString(getString(R.string.key_email_submit_password), password)
            .apply();
    }
    
    private void updateConfigCardVisibility() {
        boolean isEnabled = switchEnableEmail.isChecked();
        cardEmailConfig.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        btnTestEmail.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }
    
    private void sendTestMessage() {
        String fromAddress = editEmailFrom.getText().toString().trim();
        String toAddress = editEmailTo.getText().toString().trim();
        String host = editEmailHost.getText().toString().trim();
        String port = editEmailPort.getText().toString().trim();
        String password = editEmailPassword.getText().toString().trim();
        String usernameStyleText = spinnerUsernameStyle.getText().toString().trim();
        
        if (fromAddress.isEmpty() || toAddress.isEmpty() || host.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int portInt = Integer.parseInt(port);
            String usernameStyle = usernameStyleText.equals("Full Email") ? "full" : "username";
            
            // Determine username based on style
            String username = usernameStyle.equals("full") ? fromAddress
                    : (fromAddress.contains("@") ? fromAddress.substring(0, fromAddress.indexOf("@"))
                            : fromAddress);
            
            InternetAddress from = new InternetAddress(fromAddress);
            InternetAddress[] to = {new InternetAddress(toAddress)};
            
            String testMessage = "Test message from SMS Forward app. Email forwarding is working correctly!";
            EmailForwarder forwarder = new EmailForwarder(from, to, host, (short) portInt, 
                username, password, getContext());
            
            // Use RetryableForwarder for better reliability
            RetryableForwarder retryableForwarder = new RetryableForwarder(forwarder);
            retryableForwarder.forward("+1234567890", testMessage);
            
            Toast.makeText(getContext(), "Test email sent successfully!", Toast.LENGTH_SHORT).show();
            
            // Cleanup
            retryableForwarder.shutdown();
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid port number", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send test email: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        saveEmailSettings();
    }
} 