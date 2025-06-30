package com.keremgok.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Advanced content filtering system for SMS messages.
 * Supports multiple filter types: keywords, regex, sender patterns, and custom rules.
 */
public class AdvancedContentFilter {
    private static final String TAG = "AdvancedContentFilter";
    
    // Preference keys
    private static final String PREF_ADVANCED_FILTERS = "advanced_filters";
    private static final String PREF_FILTER_ENABLED = "advanced_filter_enabled";
    
    // Filter types
    public enum FilterType {
        KEYWORD,    // Simple keyword matching
        REGEX,      // Regular expression
        SENDER,     // Sender number pattern
        CONTAINS,   // Contains specific text
        STARTS_WITH, // Starts with text
        ENDS_WITH   // Ends with text
    }
    
    // Filter actions
    public enum FilterAction {
        BLOCK,      // Block the message completely
        SKIP,       // Skip forwarding but allow message
        TAG         // Tag with category for special handling
    }
    
    private final Context context;
    private final SharedPreferences preferences;
    private List<FilterRule> filterRules;
    
    public AdvancedContentFilter(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.filterRules = new ArrayList<>();
        loadFilters();
    }
    
    /**
     * Check if advanced filtering is enabled
     */
    public boolean isAdvancedFilteringEnabled() {
        return preferences.getBoolean(PREF_FILTER_ENABLED, false);
    }
    
    /**
     * Enable or disable advanced filtering
     */
    public void setAdvancedFilteringEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(PREF_FILTER_ENABLED, enabled)
                .apply();
    }
    
    /**
     * Process a message through all active filters
     */
    public FilterResult processMessage(String fromNumber, String messageContent) {
        if (!isAdvancedFilteringEnabled() || filterRules.isEmpty()) {
            return new FilterResult(FilterAction.SKIP, null, "No filters active");
        }
        
        for (FilterRule rule : filterRules) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            if (rule.matches(fromNumber, messageContent)) {
                Log.i(TAG, String.format("Message matched filter rule: %s", rule.getName()));
                return new FilterResult(rule.getAction(), rule, 
                    String.format("Matched rule: %s", rule.getName()));
            }
        }
        
        return new FilterResult(FilterAction.SKIP, null, "No matching filters");
    }
    
    /**
     * Add a new filter rule
     */
    public void addFilterRule(FilterRule rule) {
        filterRules.add(rule);
        saveFilters();
        Log.i(TAG, "Added filter rule: " + rule.getName());
    }
    
    /**
     * Remove a filter rule
     */
    public void removeFilterRule(FilterRule rule) {
        filterRules.remove(rule);
        saveFilters();
        Log.i(TAG, "Removed filter rule: " + rule.getName());
    }
    
    /**
     * Update a filter rule
     */
    public void updateFilterRule(int index, FilterRule rule) {
        if (index >= 0 && index < filterRules.size()) {
            filterRules.set(index, rule);
            saveFilters();
            Log.i(TAG, "Updated filter rule: " + rule.getName());
        }
    }
    
    /**
     * Get all filter rules
     */
    public List<FilterRule> getFilterRules() {
        return new ArrayList<>(filterRules);
    }
    
    /**
     * Get active filter rules count
     */
    public int getActiveFilterCount() {
        int count = 0;
        for (FilterRule rule : filterRules) {
            if (rule.isEnabled()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Clear all filter rules
     */
    public void clearAllFilters() {
        filterRules.clear();
        saveFilters();
        Log.i(TAG, "Cleared all filter rules");
    }
    
    /**
     * Test a message against all filters (for preview)
     */
    public List<FilterRule> testMessage(String fromNumber, String messageContent) {
        List<FilterRule> matchingRules = new ArrayList<>();
        
        for (FilterRule rule : filterRules) {
            if (rule.isEnabled() && rule.matches(fromNumber, messageContent)) {
                matchingRules.add(rule);
            }
        }
        
        return matchingRules;
    }
    
    /**
     * Import filters from JSON
     */
    public void importFilters(String jsonData) throws JSONException {
        JSONArray jsonArray = new JSONArray(jsonData);
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonRule = jsonArray.getJSONObject(i);
            FilterRule rule = FilterRule.fromJson(jsonRule);
            if (rule != null) {
                filterRules.add(rule);
            }
        }
        
        saveFilters();
        Log.i(TAG, "Imported " + jsonArray.length() + " filter rules");
    }
    
    /**
     * Export filters to JSON
     */
    public String exportFilters() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        
        for (FilterRule rule : filterRules) {
            jsonArray.put(rule.toJson());
        }
        
        return jsonArray.toString(2);
    }
    
    /**
     * Save filters to preferences
     */
    private void saveFilters() {
        try {
            String jsonData = exportFilters();
            preferences.edit()
                    .putString(PREF_ADVANCED_FILTERS, jsonData)
                    .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save filters", e);
        }
    }
    
    /**
     * Load filters from preferences
     */
    private void loadFilters() {
        String jsonData = preferences.getString(PREF_ADVANCED_FILTERS, "[]");
        try {
            importFilters(jsonData);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load filters", e);
            filterRules.clear();
        }
    }
    
    /**
     * Represents a single filter rule
     */
    public static class FilterRule {
        private String name;
        private String description;
        private FilterType type;
        private FilterAction action;
        private String pattern;
        private String category;
        private boolean enabled;
        private long createdAt;
        
        // Compiled regex pattern for performance
        private Pattern compiledPattern;
        
        public FilterRule(String name, FilterType type, FilterAction action, String pattern) {
            this.name = name;
            this.type = type;
            this.action = action;
            this.pattern = pattern;
            this.enabled = true;
            this.createdAt = System.currentTimeMillis();
            this.category = "General";
            this.description = "";
            
            compilePattern();
        }
        
        /**
         * Check if this rule matches the message
         */
        public boolean matches(String fromNumber, String messageContent) {
            if (!enabled) {
                return false;
            }
            
            try {
                switch (type) {
                    case KEYWORD:
                        return messageContent.toLowerCase().contains(pattern.toLowerCase());
                        
                    case REGEX:
                        return compiledPattern != null && 
                               compiledPattern.matcher(messageContent).find();
                        
                    case SENDER:
                        return fromNumber.contains(pattern);
                        
                    case CONTAINS:
                        return messageContent.contains(pattern);
                        
                    case STARTS_WITH:
                        return messageContent.startsWith(pattern);
                        
                    case ENDS_WITH:
                        return messageContent.endsWith(pattern);
                        
                    default:
                        return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error matching filter rule: " + name, e);
                return false;
            }
        }
        
        /**
         * Compile regex pattern for performance
         */
        private void compilePattern() {
            if (type == FilterType.REGEX) {
                try {
                    compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    Log.e(TAG, "Invalid regex pattern: " + pattern, e);
                    compiledPattern = null;
                }
            }
        }
        
        /**
         * Convert to JSON
         */
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("description", description);
            json.put("type", type.name());
            json.put("action", action.name());
            json.put("pattern", pattern);
            json.put("category", category);
            json.put("enabled", enabled);
            json.put("createdAt", createdAt);
            return json;
        }
        
        /**
         * Create from JSON
         */
        public static FilterRule fromJson(JSONObject json) {
            try {
                String name = json.getString("name");
                FilterType type = FilterType.valueOf(json.getString("type"));
                FilterAction action = FilterAction.valueOf(json.getString("action"));
                String pattern = json.getString("pattern");
                
                FilterRule rule = new FilterRule(name, type, action, pattern);
                rule.setDescription(json.optString("description", ""));
                rule.setCategory(json.optString("category", "General"));
                rule.setEnabled(json.optBoolean("enabled", true));
                rule.createdAt = json.optLong("createdAt", System.currentTimeMillis());
                
                return rule;
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse filter rule from JSON", e);
                return null;
            }
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public FilterType getType() { return type; }
        public void setType(FilterType type) { 
            this.type = type; 
            compilePattern();
        }
        
        public FilterAction getAction() { return action; }
        public void setAction(FilterAction action) { this.action = action; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { 
            this.pattern = pattern; 
            compilePattern();
        }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public long getCreatedAt() { return createdAt; }
        
        public String getTypeDisplayName() {
            switch (type) {
                case KEYWORD: return "Keyword";
                case REGEX: return "Regex";
                case SENDER: return "Sender";
                case CONTAINS: return "Contains";
                case STARTS_WITH: return "Starts With";
                case ENDS_WITH: return "Ends With";
                default: return type.name();
            }
        }
        
        public String getActionDisplayName() {
            switch (action) {
                case BLOCK: return "Block";
                case SKIP: return "Skip";
                case TAG: return "Tag";
                default: return action.name();
            }
        }
    }
    
    /**
     * Filter processing result
     */
    public static class FilterResult {
        private final FilterAction action;
        private final FilterRule matchedRule;
        private final String reason;
        
        public FilterResult(FilterAction action, FilterRule matchedRule, String reason) {
            this.action = action;
            this.matchedRule = matchedRule;
            this.reason = reason;
        }
        
        public FilterAction getAction() { return action; }
        public FilterRule getMatchedRule() { return matchedRule; }
        public String getReason() { return reason; }
        
        public boolean shouldBlock() { 
            return action == FilterAction.BLOCK; 
        }
        
        public boolean shouldSkip() { 
            return action == FilterAction.SKIP; 
        }
        
        public boolean hasTag() { 
            return action == FilterAction.TAG; 
        }
    }
} 