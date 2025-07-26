package com.keremgok.smsforward;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

/**
 * Fragment for About section containing:
 * - Appearance settings (language, theme)
 * - Backup & restore functionality
 * - App information and about dialog
 */
public class AboutFragment extends BasePreferenceFragment {

    private ThemeManager themeManager;
    private LanguageManager languageManager;
    private SettingsBackupManager backupManager;

    // Activity result launchers for file operations
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.about_preferences, rootKey);

        // Initialize managers
        themeManager = new ThemeManager(context);
        languageManager = new LanguageManager(context);
        backupManager = new SettingsBackupManager(context);

        // Initialize file operation launchers
        initializeFileLaunchers();

        // Set up all preferences
        setupAppearancePreferences();
        setupBackupPreferences();
        setupAboutPreferences();
    }

    @Override
    protected void updatePreferenceSummaries() {
        updateLanguageSummary();
        updateThemeSummary();
        // About and backup preferences don't need summary updates
    }

    @Override
    protected void updatePreferenceSummaryForKey(String key) {
        if (context == null) return;

        String languageKey = context.getString(R.string.key_language);
        String themeKey = context.getString(R.string.key_theme_mode);

        if (languageKey.equals(key)) {
            updateLanguageSummary();
        } else if (themeKey.equals(key)) {
            updateThemeSummary();
        }
    }

    @Override
    protected void cleanupResources() {
        // No specific cleanup needed for About fragment
        // Base class handles SharedPreferences cleanup
    }

    /**
     * Set up appearance related preferences
     */
    private void setupAppearancePreferences() {
        // Language preference
        ListPreference languagePreference = (ListPreference) getPreferenceByStringRes(R.string.key_language);
        if (languagePreference != null) {
            updateLanguageSummary();
            languagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String newLanguage = (String) newValue;
                languageManager.setLanguage(newLanguage);
                showLanguageRestartDialog();
                return true;
            });
        }

        // Theme preference
        ListPreference themePreference = (ListPreference) getPreferenceByStringRes(R.string.key_theme_mode);
        if (themePreference != null) {
            updateThemeSummary();
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String newTheme = (String) newValue;
                themeManager.setThemeMode(newTheme);
                updateThemeSummary();
                
                // Recreate activity to apply theme immediately
                if (getActivity() != null) {
                    getActivity().recreate();
                }
                return true;
            });
        }
    }

    /**
     * Set up backup and restore preferences
     */
    private void setupBackupPreferences() {
        // Export settings
        Preference exportPreference = getPreferenceByStringRes(R.string.key_export_settings);
        if (exportPreference != null) {
            exportPreference.setOnPreferenceClickListener(preference -> {
                exportSettings();
                return true;
            });
        }

        // Import settings
        Preference importPreference = getPreferenceByStringRes(R.string.key_import_settings);
        if (importPreference != null) {
            importPreference.setOnPreferenceClickListener(preference -> {
                importSettings();
                return true;
            });
        }
    }

    /**
     * Set up about preference
     */
    private void setupAboutPreferences() {
        Preference aboutPreference = getPreferenceByStringRes(R.string.key_about);
        if (aboutPreference != null) {
            aboutPreference.setOnPreferenceClickListener(preference -> {
                showAboutDialog();
                return true;
            });
        }
    }

    /**
     * Update language preference summary
     */
    private void updateLanguageSummary() {
        if (languageManager == null || context == null) return;

        ListPreference languagePreference = (ListPreference) getPreferenceByStringRes(R.string.key_language);
        if (languagePreference != null) {
            try {
                String currentLanguage = languageManager.getSelectedLanguage();
                String[] languageEntries = getResources().getStringArray(R.array.language_entries);
                String[] languageValues = getResources().getStringArray(R.array.language_values);

                for (int i = 0; i < languageValues.length; i++) {
                    if (languageValues[i].equals(currentLanguage)) {
                        languagePreference.setSummary(languageEntries[i]);
                        break;
                    }
                }
            } catch (Exception e) {
                languagePreference.setSummary("Error reading language setting");
            }
        }
    }

    /**
     * Update theme preference summary
     */
    private void updateThemeSummary() {
        if (themeManager == null || context == null) return;

        ListPreference themePreference = (ListPreference) getPreferenceByStringRes(R.string.key_theme_mode);
        if (themePreference != null) {
            try {
                String description = themeManager.getCurrentThemeDescription();
                themePreference.setSummary(description);
            } catch (Exception e) {
                themePreference.setSummary("Error reading theme setting");
            }
        }
    }

    /**
     * Show language restart dialog
     */
    private void showLanguageRestartDialog() {
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle(R.string.language_restart_title)
                .setMessage(R.string.language_restart_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Initialize file operation launchers for export/import
     */
    private void initializeFileLaunchers() {
        // Export launcher - creates a new file
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExport(uri);
                        }
                    }
                });

        // Import launcher - opens an existing file
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performImport(uri);
                        }
                    }
                });
    }

    /**
     * Start the export settings process
     */
    private void exportSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, backupManager.generateBackupFilename());
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});

            exportLauncher.launch(intent);

        } catch (Exception e) {
            showToast(String.format(context.getString(R.string.export_error), e.getMessage()),
                    Toast.LENGTH_LONG);
        }
    }

    /**
     * Start the import settings process
     */
    private void importSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});

            importLauncher.launch(intent);

        } catch (Exception e) {
            showToast(String.format(context.getString(R.string.import_error), e.getMessage()),
                    Toast.LENGTH_LONG);
        }
    }

    /**
     * Perform the actual export to the selected file
     */
    private void performExport(Uri uri) {
        try {
            backupManager.exportToFile(uri);
            showToast(context.getString(R.string.export_success), Toast.LENGTH_SHORT);

        } catch (Exception e) {
            showToast(String.format(context.getString(R.string.export_error), e.getMessage()),
                    Toast.LENGTH_LONG);
        }
    }

    /**
     * Perform the actual import from the selected file
     */
    private void performImport(Uri uri) {
        try {
            SettingsBackupManager.ImportResult result = backupManager.importFromFile(uri);

            if (result.success) {
                showToast(String.format(context.getString(R.string.import_success), result.message),
                        Toast.LENGTH_LONG);

                // Refresh preference summaries to reflect imported values
                updatePreferenceSummaries();

                // If theme was changed, recreate activity
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } else {
                showToast(String.format(context.getString(R.string.import_error), result.message),
                        Toast.LENGTH_LONG);
            }

        } catch (Exception e) {
            showToast(String.format(context.getString(R.string.import_error), e.getMessage()),
                    Toast.LENGTH_LONG);
        }
    }

    /**
     * Show about dialog with app information
     */
    private void showAboutDialog() {
        if (context == null) return;

        try {
            // Get app version info
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;

            // Build about content
            StringBuilder aboutContent = new StringBuilder();

            // App name and version
            aboutContent.append("📱 ").append(context.getString(R.string.about_app_name)).append("\n");
            aboutContent.append(context.getString(R.string.about_version_title)).append(": ");
            aboutContent.append(String.format(context.getString(R.string.about_version_format), versionName, versionCode));
            aboutContent.append("\n\n");

            // Description
            aboutContent.append("📄 ").append(context.getString(R.string.about_description_title)).append(":\n");
            aboutContent.append(context.getString(R.string.about_description_text)).append("\n\n");

            // Key features
            aboutContent.append("⭐ ").append(context.getString(R.string.about_features_title)).append(":\n");
            aboutContent.append(context.getString(R.string.about_features_text)).append("\n\n");

            // Developer info
            aboutContent.append("👨‍💻 ").append(context.getString(R.string.about_developer_title)).append(": ");
            aboutContent.append(context.getString(R.string.about_developer_name)).append("\n\n");

            // License
            aboutContent.append("📜 ").append(context.getString(R.string.about_license_title)).append(": ");
            aboutContent.append(context.getString(R.string.about_license_name)).append("\n\n");

            // Build information
            aboutContent.append("🔧 ").append(context.getString(R.string.about_build_info_title)).append(":\n");
            aboutContent.append(String.format(context.getString(R.string.about_package_name),
                    context.getPackageName())).append("\n");
            aboutContent.append(String.format(context.getString(R.string.about_target_sdk),
                    android.os.Build.VERSION.SDK_INT)).append("\n");
            aboutContent.append(String.format(context.getString(R.string.about_min_sdk), 25)).append("\n");

            // Show build time if available
            try {
                android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
                java.io.File apkFile = new java.io.File(appInfo.sourceDir);
                long buildTime = apkFile.lastModified();
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault());
                aboutContent.append(String.format(context.getString(R.string.about_build_time),
                        dateFormat.format(new java.util.Date(buildTime)))).append("\n");
            } catch (Exception e) {
                // Ignore build time if unavailable
            }

            // Show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.about_title))
                    .setMessage(aboutContent.toString())
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();

        } catch (Exception e) {
            showToast("Error showing about information: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }
}