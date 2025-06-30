package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enhanced backup manager for comprehensive app data management.
 * Supports unified backup of settings, filters, and selective operations.
 */
public class EnhancedBackupManager {
    private static final String TAG = "EnhancedBackupManager";
    
    // Enhanced backup version for new features
    private static final int ENHANCED_BACKUP_VERSION = 2;
    
    // Metadata keys
    private static final String KEY_BACKUP_VERSION = "_backup_version";
    private static final String KEY_EXPORT_TIMESTAMP = "_export_timestamp";
    private static final String KEY_APP_VERSION = "_app_version";
    private static final String KEY_BACKUP_TYPE = "_backup_type";
    private static final String KEY_INCLUDED_COMPONENTS = "_included_components";
    private static final String KEY_TOTAL_SIZE = "_total_size";
    private static final String KEY_DEVICE_INFO = "_device_info";
    
    // Data section keys
    private static final String SECTION_SETTINGS = "settings";
    private static final String SECTION_FILTERS = "filters";
    private static final String SECTION_STATISTICS = "statistics";
    
    public enum BackupType {
        FULL("Full Backup"),
        SETTINGS_ONLY("Settings Only"),
        FILTERS_ONLY("Filters Only"),
        SELECTIVE("Selective Backup");
        
        private final String displayName;
        
        BackupType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum BackupComponent {
        FORWARDER_SETTINGS("Forwarder Settings", "SMS, Telegram, Email, Webhook configurations"),
        APPEARANCE_SETTINGS("Appearance", "Theme, language, and UI preferences"),
        SECURITY_SETTINGS("Security", "Rate limiting and content filtering"),
        ADVANCED_FILTERS("Advanced Filters", "Custom message filtering rules"),
        APP_STATISTICS("Statistics", "Usage analytics and performance data");
        
        private final String displayName;
        private final String description;
        
        BackupComponent(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final Context context;
    private final SharedPreferences preferences;
    private final SettingsBackupManager legacyBackupManager;
    private final AdvancedContentFilter filterManager;
    
    public EnhancedBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.legacyBackupManager = new SettingsBackupManager(context);
        this.filterManager = new AdvancedContentFilter(context);
    }
    
    /**
     * Create enhanced backup with selective components
     */
    public String createBackup(BackupType type, List<BackupComponent> components) throws JSONException {
        JSONObject backup = new JSONObject();
        
        // Add enhanced metadata
        addEnhancedMetadata(backup, type, components);
        
        // Add selected components
        for (BackupComponent component : components) {
            switch (component) {
                case FORWARDER_SETTINGS:
                case APPEARANCE_SETTINGS:
                case SECURITY_SETTINGS:
                    addSettingsData(backup, component);
                    break;
                case ADVANCED_FILTERS:
                    addFiltersData(backup);
                    break;
                case APP_STATISTICS:
                    addStatisticsData(backup);
                    break;
            }
        }
        
        // Calculate and add total size
        String jsonString = backup.toString();
        backup.put(KEY_TOTAL_SIZE, jsonString.length());
        
        Log.i(TAG, "Created " + type.getDisplayName() + " with " + components.size() + " components");
        return backup.toString(2);
    }
    
    /**
     * Create full backup with all components
     */
    public String createFullBackup() throws JSONException {
        List<BackupComponent> allComponents = new ArrayList<>();
        for (BackupComponent component : BackupComponent.values()) {
            allComponents.add(component);
        }
        return createBackup(BackupType.FULL, allComponents);
    }
    
    /**
     * Analyze backup file and return information
     */
    public BackupInfo analyzeBackup(String jsonData) throws JSONException {
        JSONObject backup = new JSONObject(jsonData);
        
        // Extract metadata
        int version = backup.optInt(KEY_BACKUP_VERSION, 1);
        long timestamp = backup.optLong(KEY_EXPORT_TIMESTAMP, 0);
        String appVersion = backup.optString(KEY_APP_VERSION, "Unknown");
        String backupType = backup.optString(KEY_BACKUP_TYPE, "Legacy");
        int totalSize = backup.optInt(KEY_TOTAL_SIZE, jsonData.length());
        
        // Analyze components
        List<BackupComponent> components = new ArrayList<>();
        JSONArray includedComponents = backup.optJSONArray(KEY_INCLUDED_COMPONENTS);
        
        if (includedComponents != null) {
            for (int i = 0; i < includedComponents.length(); i++) {
                String componentName = includedComponents.getString(i);
                try {
                    BackupComponent component = BackupComponent.valueOf(componentName);
                    components.add(component);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Unknown backup component: " + componentName);
                }
            }
        } else {
            // Legacy backup - analyze what's available
            components.addAll(analyzeLegacyBackup(backup));
        }
        
        // Count items in each section
        Map<String, Integer> sectionCounts = new HashMap<>();
        if (backup.has(SECTION_SETTINGS)) {
            JSONObject settings = backup.getJSONObject(SECTION_SETTINGS);
            sectionCounts.put(SECTION_SETTINGS, settings.length());
        }
        if (backup.has(SECTION_FILTERS)) {
            JSONArray filters = backup.getJSONArray(SECTION_FILTERS);
            sectionCounts.put(SECTION_FILTERS, filters.length());
        }
        if (backup.has(SECTION_STATISTICS)) {
            JSONObject stats = backup.getJSONObject(SECTION_STATISTICS);
            sectionCounts.put(SECTION_STATISTICS, stats.length());
        }
        
        return new BackupInfo(version, timestamp, appVersion, backupType, 
                             totalSize, components, sectionCounts);
    }
    
    /**
     * Import backup with selective components
     */
    public ImportResult importBackup(String jsonData, List<BackupComponent> componentsToImport) {
        try {
            BackupInfo info = analyzeBackup(jsonData);
            
            // Validate version compatibility
            if (info.version > ENHANCED_BACKUP_VERSION) {
                return new ImportResult(false, 
                    "Backup is from a newer app version. Please update the app.", 0);
            }
            
            JSONObject backup = new JSONObject(jsonData);
            int importedCount = 0;
            
            // Import selected components
            for (BackupComponent component : componentsToImport) {
                if (info.components.contains(component)) {
                    switch (component) {
                        case FORWARDER_SETTINGS:
                        case APPEARANCE_SETTINGS:
                        case SECURITY_SETTINGS:
                            importedCount += importSettingsComponent(backup, component);
                            break;
                        case ADVANCED_FILTERS:
                            importedCount += importFiltersComponent(backup);
                            break;
                        case APP_STATISTICS:
                            importedCount += importStatisticsComponent(backup);
                            break;
                    }
                }
            }
            
            String message = "Successfully imported " + importedCount + " items from " + 
                           info.backupType + " created on " + formatDate(info.timestamp);
            
            Log.i(TAG, message);
            return new ImportResult(true, message, importedCount);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to import backup", e);
            return new ImportResult(false, "Import failed: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Export backup to file
     */
    public void exportToFile(Uri uri, BackupType type, List<BackupComponent> components) 
            throws IOException, JSONException {
        String backupData = createBackup(type, components);
        
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open output stream");
            }
            outputStream.write(backupData.getBytes("UTF-8"));
            outputStream.flush();
        }
        
        Log.i(TAG, "Exported " + type.getDisplayName() + " to " + uri);
    }
    
    /**
     * Import backup from file
     */
    public ImportResult importFromFile(Uri uri, List<BackupComponent> componentsToImport) {
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
            
            return importBackup(jsonBuilder.toString(), componentsToImport);
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to read backup file", e);
            return new ImportResult(false, "Failed to read backup file: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Generate enhanced backup filename
     */
    public String generateBackupFilename(BackupType type) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String typePrefix = type.name().toLowerCase();
        return "sms_forward_" + typePrefix + "_" + timestamp + ".json";
    }
    
    // Private helper methods
    
    private void addEnhancedMetadata(JSONObject backup, BackupType type, 
                                   List<BackupComponent> components) throws JSONException {
        backup.put(KEY_BACKUP_VERSION, ENHANCED_BACKUP_VERSION);
        backup.put(KEY_EXPORT_TIMESTAMP, System.currentTimeMillis());
        backup.put(KEY_APP_VERSION, BuildConfig.VERSION_NAME);
        backup.put(KEY_BACKUP_TYPE, type.getDisplayName());
        
        // Add included components
        JSONArray componentArray = new JSONArray();
        for (BackupComponent component : components) {
            componentArray.put(component.name());
        }
        backup.put(KEY_INCLUDED_COMPONENTS, componentArray);
        
        // Add device info
        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("android_version", android.os.Build.VERSION.RELEASE);
        deviceInfo.put("device_model", android.os.Build.MODEL);
        deviceInfo.put("app_package", context.getPackageName());
        backup.put(KEY_DEVICE_INFO, deviceInfo);
    }
    
    private void addSettingsData(JSONObject backup, BackupComponent component) throws JSONException {
        if (!backup.has(SECTION_SETTINGS)) {
            backup.put(SECTION_SETTINGS, new JSONObject());
        }
        
        JSONObject settings = backup.getJSONObject(SECTION_SETTINGS);
        Map<String, ?> allPrefs = preferences.getAll();
        
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (shouldIncludeSettingForComponent(key, component)) {
                if (value instanceof Boolean) {
                    settings.put(key, (Boolean) value);
                } else if (value instanceof String) {
                    settings.put(key, (String) value);
                } else if (value instanceof Integer) {
                    settings.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    settings.put(key, (Long) value);
                } else if (value instanceof Float) {
                    settings.put(key, (Float) value);
                }
            }
        }
    }
    
    private void addFiltersData(JSONObject backup) throws JSONException {
        String filtersJson = filterManager.exportFilters();
        JSONArray filtersArray = new JSONArray(filtersJson);
        backup.put(SECTION_FILTERS, filtersArray);
    }
    
    private void addStatisticsData(JSONObject backup) throws JSONException {
        JSONObject statistics = new JSONObject();
        
        // Add app usage statistics
        statistics.put("total_messages_forwarded", getTotalMessagesForwarded());
        statistics.put("active_forwarders_count", getActiveForwardersCount());
        statistics.put("filter_rules_count", filterManager.getFilterRules().size());
        statistics.put("app_install_date", getAppInstallDate());
        statistics.put("last_backup_date", System.currentTimeMillis());
        
        backup.put(SECTION_STATISTICS, statistics);
    }
    
    private boolean shouldIncludeSettingForComponent(String key, BackupComponent component) {
        switch (component) {
            case FORWARDER_SETTINGS:
                return isForwarderSetting(key);
            case APPEARANCE_SETTINGS:
                return isAppearanceSetting(key);
            case SECURITY_SETTINGS:
                return isSecuritySetting(key);
            default:
                return false;
        }
    }
    
    private boolean isForwarderSetting(String key) {
        return key.contains("enable_") || key.contains("target_") || 
               key.contains("telegram") || key.contains("email") || key.contains("web");
    }
    
    private boolean isAppearanceSetting(String key) {
        return key.contains("theme") || key.contains("language");
    }
    
    private boolean isSecuritySetting(String key) {
        return key.contains("rate_limiting") || key.contains("filter") || 
               key.contains("security") || key.contains("pin") || key.contains("biometric");
    }
    
    private List<BackupComponent> analyzeLegacyBackup(JSONObject backup) {
        List<BackupComponent> components = new ArrayList<>();
        
        Iterator<String> keys = backup.keys();
        boolean hasForwarders = false, hasAppearance = false, hasSecurity = false;
        
        while (keys.hasNext()) {
            String key = keys.next();
            if (isForwarderSetting(key)) hasForwarders = true;
            if (isAppearanceSetting(key)) hasAppearance = true;
            if (isSecuritySetting(key)) hasSecurity = true;
        }
        
        if (hasForwarders) components.add(BackupComponent.FORWARDER_SETTINGS);
        if (hasAppearance) components.add(BackupComponent.APPEARANCE_SETTINGS);
        if (hasSecurity) components.add(BackupComponent.SECURITY_SETTINGS);
        
        return components;
    }
    
    private int importSettingsComponent(JSONObject backup, BackupComponent component) {
        try {
            if (!backup.has(SECTION_SETTINGS)) return 0;
            
            JSONObject settings = backup.getJSONObject(SECTION_SETTINGS);
            SharedPreferences.Editor editor = preferences.edit();
            int count = 0;
            
            Iterator<String> keys = settings.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (shouldIncludeSettingForComponent(key, component)) {
                    Object value = settings.get(key);
                    
                    if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                        count++;
                    } else if (value instanceof String) {
                        editor.putString(key, (String) value);
                        count++;
                    } else if (value instanceof Integer) {
                        editor.putInt(key, (Integer) value);
                        count++;
                    } else if (value instanceof Long) {
                        editor.putLong(key, (Long) value);
                        count++;
                    } else if (value instanceof Double) {
                        editor.putFloat(key, ((Double) value).floatValue());
                        count++;
                    }
                }
            }
            
            editor.apply();
            return count;
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to import settings component: " + component, e);
            return 0;
        }
    }
    
    private int importFiltersComponent(JSONObject backup) {
        try {
            if (!backup.has(SECTION_FILTERS)) return 0;
            
            JSONArray filters = backup.getJSONArray(SECTION_FILTERS);
            filterManager.importFilters(filters.toString());
            return filters.length();
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to import filters component", e);
            return 0;
        }
    }
    
    private int importStatisticsComponent(JSONObject backup) {
        try {
            if (!backup.has(SECTION_STATISTICS)) return 0;
            
            JSONObject statistics = backup.getJSONObject(SECTION_STATISTICS);
            // Statistics are read-only for now, just log what was available
            Log.i(TAG, "Statistics from backup: " + statistics.toString());
            return statistics.length();
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to import statistics component", e);
            return 0;
        }
    }
    
    // Utility methods for statistics
    private long getTotalMessagesForwarded() {
        return preferences.getLong("total_messages_forwarded", 0);
    }
    
    private int getActiveForwardersCount() {
        int count = 0;
        if (preferences.getBoolean(context.getString(R.string.key_enable_sms), false)) count++;
        if (preferences.getBoolean(context.getString(R.string.key_enable_telegram), false)) count++;
        if (preferences.getBoolean(context.getString(R.string.key_enable_email), false)) count++;
        if (preferences.getBoolean(context.getString(R.string.key_enable_web), false)) count++;
        return count;
    }
    
    private long getAppInstallDate() {
        return preferences.getLong("app_install_date", System.currentTimeMillis());
    }
    
    private String formatDate(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date(timestamp));
    }
    
    // Data classes
    public static class BackupInfo {
        public final int version;
        public final long timestamp;
        public final String appVersion;
        public final String backupType;
        public final int totalSize;
        public final List<BackupComponent> components;
        public final Map<String, Integer> sectionCounts;
        
        public BackupInfo(int version, long timestamp, String appVersion, String backupType,
                         int totalSize, List<BackupComponent> components, 
                         Map<String, Integer> sectionCounts) {
            this.version = version;
            this.timestamp = timestamp;
            this.appVersion = appVersion;
            this.backupType = backupType;
            this.totalSize = totalSize;
            this.components = components;
            this.sectionCounts = sectionCounts;
        }
        
        public String getFormattedSize() {
            if (totalSize < 1024) return totalSize + " B";
            if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
            return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
        }
        
        public String getFormattedDate() {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date(timestamp));
        }
    }
    
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