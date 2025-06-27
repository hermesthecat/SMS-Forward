package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Manages backup and restore of application settings.
 * Exports/imports configuration to/from JSON format.
 */
public class SettingsBackupManager {
    private static final String TAG = "SettingsBackupManager";
    
    // Version for backward compatibility
    private static final int BACKUP_VERSION = 1;
    
    // Metadata keys
    private static final String KEY_BACKUP_VERSION = "_backup_version";
    private static final String KEY_EXPORT_TIMESTAMP = "_export_timestamp";
    private static final String KEY_APP_VERSION = "_app_version";
    
    private final Context context;
    private final SharedPreferences preferences;
    
    public SettingsBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     * Export all settings to JSON format
     * 
     * @return JSON string containing all exportable settings
     * @throws JSONException if JSON creation fails
     */
    public String exportSettings() throws JSONException {
        JSONObject exportData = new JSONObject();
        
        // Add metadata
        exportData.put(KEY_BACKUP_VERSION, BACKUP_VERSION);
        exportData.put(KEY_EXPORT_TIMESTAMP, System.currentTimeMillis());
        exportData.put(KEY_APP_VERSION, BuildConfig.VERSION_NAME);
        
        // Export all preferences
        Map<String, ?> allPrefs = preferences.getAll();
        
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Only export user-configurable settings, skip internal state
            if (isExportableKey(key)) {
                if (value instanceof Boolean) {
                    exportData.put(key, (Boolean) value);
                } else if (value instanceof String) {
                    exportData.put(key, (String) value);
                } else if (value instanceof Integer) {
                    exportData.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    exportData.put(key, (Long) value);
                } else if (value instanceof Float) {
                    exportData.put(key, (Float) value);
                }
            }
        }
        
        Log.i(TAG, "Exported " + (exportData.length() - 3) + " settings"); // -3 for metadata
        return exportData.toString(2); // Pretty print with 2 space indentation
    }
    
    /**
     * Import settings from JSON format
     * 
     * @param jsonData JSON string containing settings to import
     * @return ImportResult with success status and details
     */
    public ImportResult importSettings(String jsonData) {
        try {
            JSONObject importData = new JSONObject(jsonData);
            
            // Validate backup version
            int backupVersion = importData.optInt(KEY_BACKUP_VERSION, -1);
            if (backupVersion == -1) {
                return new ImportResult(false, "Invalid backup file: missing version information", 0);
            }
            
            if (backupVersion > BACKUP_VERSION) {
                return new ImportResult(false, 
                    "Backup file is from a newer version (v" + backupVersion + "). Please update the app.", 0);
            }
            
            // Start importing preferences
            SharedPreferences.Editor editor = preferences.edit();
            int importedCount = 0;
            
            Iterator<String> keyIterator = importData.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                
                // Skip metadata keys
                if (key.startsWith("_")) {
                    continue;
                }
                
                // Only import known preference keys for security
                if (isExportableKey(key)) {
                    Object value = importData.get(key);
                    
                    if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                        importedCount++;
                    } else if (value instanceof String) {
                        editor.putString(key, (String) value);
                        importedCount++;
                    } else if (value instanceof Integer) {
                        editor.putInt(key, (Integer) value);
                        importedCount++;
                    } else if (value instanceof Long) {
                        editor.putLong(key, (Long) value);
                        importedCount++;
                    } else if (value instanceof Double) {
                        editor.putFloat(key, ((Double) value).floatValue());
                        importedCount++;
                    }
                }
            }
            
            // Apply all changes
            boolean success = editor.commit();
            
            if (success) {
                long exportTimestamp = importData.optLong(KEY_EXPORT_TIMESTAMP, 0);
                String exportDate = exportTimestamp > 0 ? 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        .format(new Date(exportTimestamp)) : "Unknown";
                
                Log.i(TAG, "Successfully imported " + importedCount + " settings");
                return new ImportResult(true, 
                    "Successfully imported " + importedCount + " settings from backup created on " + exportDate, 
                    importedCount);
            } else {
                return new ImportResult(false, "Failed to save imported settings", 0);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse import data", e);
            return new ImportResult(false, "Invalid backup file format: " + e.getMessage(), 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to import settings", e);
            return new ImportResult(false, "Import failed: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Write exported settings to a file URI
     * 
     * @param uri File URI to write to
     * @throws IOException if file write fails
     * @throws JSONException if export data creation fails
     */
    public void exportToFile(Uri uri) throws IOException, JSONException {
        String exportData = exportSettings();
        
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream for URI: " + uri);
            }
            
            outputStream.write(exportData.getBytes("UTF-8"));
            outputStream.flush();
        }
        
        Log.i(TAG, "Successfully exported settings to file: " + uri);
    }
    
    /**
     * Read and import settings from a file URI
     * 
     * @param uri File URI to read from
     * @return ImportResult with success status and details
     */
    public ImportResult importFromFile(Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return new ImportResult(false, "Failed to read backup file", 0);
            }
            
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line).append('\n');
                }
            }
            
            String jsonData = jsonBuilder.toString();
            ImportResult result = importSettings(jsonData);
            
            if (result.success) {
                Log.i(TAG, "Successfully imported settings from file: " + uri);
            } else {
                Log.e(TAG, "Failed to import settings from file: " + result.message);
            }
            
            return result;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to read import file", e);
            return new ImportResult(false, "Failed to read backup file: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Generate a suggested filename for backup export
     * 
     * @return Filename in format: sms_forward_backup_YYYYMMDD_HHMMSS.json
     */
    public String generateBackupFilename() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        return "sms_forward_backup_" + timestamp + ".json";
    }
    
    /**
     * Check if a preference key should be included in export/import
     * This excludes internal state and runtime data
     */
    private boolean isExportableKey(String key) {
        // Include all user-configurable preferences
        return key.equals(context.getString(R.string.key_enable_sms)) ||
               key.equals(context.getString(R.string.key_target_sms)) ||
               key.equals(context.getString(R.string.key_enable_telegram)) ||
               key.equals(context.getString(R.string.key_target_telegram)) ||
               key.equals(context.getString(R.string.key_telegram_apikey)) ||
               key.equals(context.getString(R.string.key_enable_web)) ||
               key.equals(context.getString(R.string.key_target_web)) ||
               key.equals(context.getString(R.string.key_enable_email)) ||
               key.equals(context.getString(R.string.key_email_from_address)) ||
               key.equals(context.getString(R.string.key_email_to_address)) ||
               key.equals(context.getString(R.string.key_email_submit_host)) ||
               key.equals(context.getString(R.string.key_email_submit_port)) ||
               key.equals(context.getString(R.string.key_email_submit_password)) ||
               key.equals(context.getString(R.string.key_email_username_style)) ||
               key.equals(context.getString(R.string.key_enable_rate_limiting)) ||
               key.equals(context.getString(R.string.key_theme_mode));
    }
    
    /**
     * Result class for import operations
     */
    public static class ImportResult {
        public final boolean success;
        public final String message;
        public final int importedCount;
        
        public ImportResult(boolean success, String message, int importedCount) {
            this.success = success;
            this.message = message;
            this.importedCount = importedCount;
        }
    }
} 