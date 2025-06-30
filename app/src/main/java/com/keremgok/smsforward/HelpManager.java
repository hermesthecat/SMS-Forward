package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;

/**
 * Simple help manager providing contextual help dialogs and tooltips throughout the app.
 */
public class HelpManager {
    private static final String TAG = "HelpManager";
    
    private static final String PREF_HELP_SHOWN = "help_shown_";
    private static final String PREF_FIRST_LAUNCH = "first_launch";
    
    public enum HelpType {
        DASHBOARD,
        FORWARDERS,
        SECURITY,
        FILTERS,
        BACKUP,
        GENERAL
    }
    
    private final Context context;
    private final SharedPreferences preferences;
    
    public HelpManager(Context context) {
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     * Show contextual help dialog
     */
    public void showHelp(HelpType type) {
        String helpKey = type.name().toLowerCase();
        
        if (shouldShowHelp(helpKey)) {
            showHelpDialog(type);
            markHelpAsShown(helpKey);
        }
    }
    
    /**
     * Force show help dialog regardless of previous showing
     */
    public void showHelpAlways(HelpType type) {
        showHelpDialog(type);
    }
    
    /**
     * Show help dialog with content based on type
     */
    private void showHelpDialog(HelpType type) {
        String title = getHelpTitle(type);
        String message = getHelpMessage(type);
        int iconRes = getHelpIcon(type);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        // Create custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simple_help, null);
        
        ImageView iconView = dialogView.findViewById(R.id.help_icon);
        TextView titleView = dialogView.findViewById(R.id.help_title);
        TextView messageView = dialogView.findViewById(R.id.help_message);
        MaterialButton okButton = dialogView.findViewById(R.id.help_ok_button);
        
        iconView.setImageResource(iconRes);
        titleView.setText(title);
        messageView.setText(message);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        okButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Get help title based on type
     */
    private String getHelpTitle(HelpType type) {
        switch (type) {
            case DASHBOARD:
                return context.getString(R.string.help_dashboard_title);
            case FORWARDERS:
                return context.getString(R.string.help_forwarders_title);
            case SECURITY:
                return context.getString(R.string.help_security_title);
            case FILTERS:
                return context.getString(R.string.help_filters_title);
            case BACKUP:
                return context.getString(R.string.help_backup_title);
            case GENERAL:
            default:
                return context.getString(R.string.help_general_title);
        }
    }
    
    /**
     * Get help message based on type
     */
    private String getHelpMessage(HelpType type) {
        switch (type) {
            case DASHBOARD:
                return context.getString(R.string.help_dashboard_message);
            case FORWARDERS:
                return context.getString(R.string.help_forwarders_message);
            case SECURITY:
                return context.getString(R.string.help_security_message);
            case FILTERS:
                return context.getString(R.string.help_filters_message);
            case BACKUP:
                return context.getString(R.string.help_backup_message);
            case GENERAL:
            default:
                return context.getString(R.string.help_general_message);
        }
    }
    
    /**
     * Get help icon based on type
     */
    private int getHelpIcon(HelpType type) {
        switch (type) {
            case DASHBOARD:
                return R.drawable.ic_dashboard;
            case FORWARDERS:
                return R.drawable.ic_send;
            case SECURITY:
                return R.drawable.ic_security;
            case FILTERS:
                return R.drawable.ic_filter_list;
            case BACKUP:
                return R.drawable.ic_backup;
            case GENERAL:
            default:
                return R.drawable.ic_help;
        }
    }
    
    /**
     * Check if help should be shown (first time only)
     */
    private boolean shouldShowHelp(String helpKey) {
        return !preferences.getBoolean(PREF_HELP_SHOWN + helpKey, false);
    }
    
    /**
     * Mark help as shown
     */
    private void markHelpAsShown(String helpKey) {
        preferences.edit()
                .putBoolean(PREF_HELP_SHOWN + helpKey, true)
                .apply();
    }
    
    /**
     * Check if this is first app launch
     */
    public boolean isFirstLaunch() {
        boolean isFirst = preferences.getBoolean(PREF_FIRST_LAUNCH, true);
        if (isFirst) {
            preferences.edit()
                    .putBoolean(PREF_FIRST_LAUNCH, false)
                    .apply();
        }
        return isFirst;
    }
    
    /**
     * Show first launch welcome
     */
    public void showWelcome() {
        if (isFirstLaunch()) {
            showHelpDialog(HelpType.GENERAL);
        }
    }
    
    /**
     * Reset all help preferences
     */
    public void resetAllHelp() {
        SharedPreferences.Editor editor = preferences.edit();
        
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith(PREF_HELP_SHOWN)) {
                editor.remove(key);
            }
        }
        
        editor.putBoolean(PREF_FIRST_LAUNCH, true);
        editor.apply();
    }
} 