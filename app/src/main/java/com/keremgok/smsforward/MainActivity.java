package com.keremgok.smsforward;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET
        }, 0);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            
            // Set up test message button
            Preference testMessagePreference = findPreference(getString(R.string.key_test_message));
            if (testMessagePreference != null) {
                testMessagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        sendTestMessage();
                        return true;
                    }
                });
            }
        }
        
        private void sendTestMessage() {
            try {
                // Get preferences
                android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                
                // Check if any forwarder is enabled
                boolean smsEnabled = prefs.getBoolean(getString(R.string.key_enable_sms), false);
                boolean telegramEnabled = prefs.getBoolean(getString(R.string.key_enable_telegram), false);
                boolean webEnabled = prefs.getBoolean(getString(R.string.key_enable_web), false);
                boolean emailEnabled = prefs.getBoolean(getString(R.string.key_enable_email), false);
                
                if (!smsEnabled && !telegramEnabled && !webEnabled && !emailEnabled) {
                    Toast.makeText(getContext(), getString(R.string.test_message_no_forwarders), 
                                 Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Create test message data
                String testPhoneNumber = "+1234567890";
                String testMessage = "This is a test message from SMS Forward app. " +
                                   "If you receive this, your forwarding setup is working correctly!";
                long currentTime = System.currentTimeMillis();
                
                // Create forwarders list
                List<Forwarder> forwarders = new ArrayList<>();
                
                if (smsEnabled) {
                    String target = prefs.getString(getString(R.string.key_target_sms), "");
                    if (!target.isEmpty()) {
                        forwarders.add(new SmsForwarder(target));
                    }
                }
                
                if (telegramEnabled) {
                    String targetId = prefs.getString(getString(R.string.key_target_telegram), "");
                    String apiKey = prefs.getString(getString(R.string.key_telegram_apikey), "");
                    if (!targetId.isEmpty() && !apiKey.isEmpty()) {
                        forwarders.add(new TelegramForwarder(targetId, apiKey));
                    }
                }
                
                if (webEnabled) {
                    String targetUrl = prefs.getString(getString(R.string.key_target_web), "");
                    if (!targetUrl.isEmpty()) {
                        forwarders.add(new JsonWebForwarder(targetUrl));
                    }
                }
                
                if (emailEnabled) {
                    String fromAddress = prefs.getString(getString(R.string.key_email_from_address), "");
                    String toAddress = prefs.getString(getString(R.string.key_email_to_address), "");
                    String host = prefs.getString(getString(R.string.key_email_submit_host), "");
                    String port = prefs.getString(getString(R.string.key_email_submit_port), "587");
                    String password = prefs.getString(getString(R.string.key_email_submit_password), "");
                    String usernameStyle = prefs.getString(getString(R.string.key_email_username_style), "full");
                    
                    if (!fromAddress.isEmpty() && !toAddress.isEmpty() && 
                        !host.isEmpty() && !password.isEmpty()) {
                        try {
                            int portInt = Integer.parseInt(port);
                            
                            // Create InternetAddress objects as required by EmailForwarder constructor
                            jakarta.mail.internet.InternetAddress from = new jakarta.mail.internet.InternetAddress(fromAddress);
                            jakarta.mail.internet.InternetAddress[] to = {new jakarta.mail.internet.InternetAddress(toAddress)};
                            
                            // Determine username based on style
                            String username = usernameStyle.equals("full") ? fromAddress : 
                                            (fromAddress.contains("@") ? fromAddress.substring(0, fromAddress.indexOf("@")) : fromAddress);
                            
                            forwarders.add(new EmailForwarder(from, to, host, 
                                                            (short) portInt, username, password));
                        } catch (NumberFormatException e) {
                            // Skip email forwarder if port is invalid
                        } catch (jakarta.mail.internet.AddressException e) {
                            // Skip email forwarder if address format is invalid
                        }
                    }
                }
                
                if (forwarders.isEmpty()) {
                    Toast.makeText(getContext(), "Please complete the configuration for enabled forwarders", 
                                 Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Send test message through all enabled forwarders
                int successCount = 0;
                StringBuilder errorMessages = new StringBuilder();
                
                for (Forwarder forwarder : forwarders) {
                    try {
                        forwarder.forward(testPhoneNumber, testMessage, currentTime);
                        successCount++;
                    } catch (Exception e) {
                        errorMessages.append(forwarder.getClass().getSimpleName())
                                   .append(": ").append(e.getMessage()).append("\n");
                    }
                }
                
                // Show result
                if (successCount > 0) {
                    String message = getString(R.string.test_message_sent) + 
                                   " (" + successCount + "/" + forwarders.size() + " forwarders)";
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    
                    if (errorMessages.length() > 0) {
                        Toast.makeText(getContext(), "Some errors occurred:\n" + errorMessages.toString(), 
                                     Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMsg = String.format(getString(R.string.test_message_error), 
                                                  errorMessages.toString());
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
                
            } catch (Exception e) {
                String errorMsg = String.format(getString(R.string.test_message_error), e.getMessage());
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
} 