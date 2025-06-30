package com.keremgok.smsforward;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.button.MaterialButton;

/**
 * Utility class for adding help functionality to any view or fragment.
 * Provides convenient methods to add help buttons and show contextual help.
 */
public class HelpProvider {
    private final Context context;
    private final HelpManager helpManager;
    
    public HelpProvider(Context context) {
        this.context = context;
        this.helpManager = new HelpManager(context);
    }
    
    /**
     * Add a help button to any ViewGroup
     */
    public void addHelpButton(ViewGroup parent, HelpManager.HelpType helpType) {
        MaterialButton helpButton = new MaterialButton(context);
        helpButton.setIconResource(R.drawable.ic_help);
        helpButton.setContentDescription(context.getString(R.string.help_button_desc));
        
        // Set button style
        helpButton.setBackgroundColor(android.R.color.transparent);
        helpButton.setStrokeWidth(0);
        
        // Set click listener
        helpButton.setOnClickListener(v -> showHelp(helpType));
        
        // Add to parent
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        parent.addView(helpButton, params);
    }
    
    /**
     * Show help for specific type
     */
    public void showHelp(HelpManager.HelpType helpType) {
        helpManager.showHelpAlways(helpType);
    }
    
    /**
     * Show contextual help (only first time)
     */
    public void showContextualHelp(HelpManager.HelpType helpType) {
        helpManager.showHelp(helpType);
    }
    
    /**
     * Check if first app launch
     */
    public boolean isFirstLaunch() {
        return helpManager.isFirstLaunch();
    }
    
    /**
     * Show welcome help
     */
    public void showWelcome() {
        helpManager.showWelcome();
    }
    
    /**
     * Reset all help preferences
     */
    public void resetAllHelp() {
        helpManager.resetAllHelp();
    }
    
    /**
     * Setup help button listener for existing button
     */
    public void setupHelpButton(View helpButton, HelpManager.HelpType helpType) {
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> showHelp(helpType));
        }
    }
} 