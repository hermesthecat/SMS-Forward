package com.keremgok.smsforward;

import android.util.Log;

/**
 * Content filtering system for SMS messages.
 * Provides functionality to block messages containing specific keywords.
 */
public class SmsContentFilter {
    private static final String TAG = "SmsContentFilter";

    /**
     * Check if a message should be blocked based on configured keywords.
     * 
     * @param messageContent The SMS message content to check
     * @param filterKeywords Comma-separated list of keywords to filter
     * @return true if the message should be blocked, false otherwise
     */
    public static boolean shouldBlockMessage(String messageContent, String filterKeywords) {
        // If no filter keywords are configured, allow all messages
        if (filterKeywords == null || filterKeywords.trim().isEmpty()) {
            return false;
        }

        // If message content is null or empty, don't block it
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return false;
        }

        // Split keywords by comma and check each one
        String[] keywords = filterKeywords.split(",");
        String contentLowerCase = messageContent.toLowerCase().trim();

        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim().toLowerCase();
            
            // Skip empty keywords
            if (trimmedKeyword.isEmpty()) {
                continue;
            }

            // Check if the message contains this keyword
            if (contentLowerCase.contains(trimmedKeyword)) {
                Log.i(TAG, String.format("Message blocked by keyword filter: '%s' found in message", trimmedKeyword));
                return true; // Block the message
            }
        }

        // No matching keywords found, allow the message
        return false;
    }

    /**
     * Get a formatted summary of active filter keywords.
     * 
     * @param filterKeywords Comma-separated list of keywords
     * @return Formatted string of active keywords, or null if none
     */
    public static String getFilterSummary(String filterKeywords) {
        if (filterKeywords == null || filterKeywords.trim().isEmpty()) {
            return null;
        }

        String[] keywords = filterKeywords.split(",");
        StringBuilder summary = new StringBuilder();
        
        for (int i = 0; i < keywords.length; i++) {
            String trimmedKeyword = keywords[i].trim();
            if (!trimmedKeyword.isEmpty()) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(trimmedKeyword);
            }
        }

        return summary.length() > 0 ? summary.toString() : null;
    }

    /**
     * Validate and clean filter keywords string.
     * 
     * @param filterKeywords Raw keyword string from user input
     * @return Cleaned keyword string with normalized spacing
     */
    public static String cleanFilterKeywords(String filterKeywords) {
        if (filterKeywords == null || filterKeywords.trim().isEmpty()) {
            return "";
        }

        String[] keywords = filterKeywords.split(",");
        StringBuilder cleaned = new StringBuilder();

        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim();
            if (!trimmedKeyword.isEmpty()) {
                if (cleaned.length() > 0) {
                    cleaned.append(",");
                }
                cleaned.append(trimmedKeyword);
            }
        }

        return cleaned.toString();
    }

    /**
     * Count the number of active filter keywords.
     * 
     * @param filterKeywords Comma-separated list of keywords
     * @return Number of active keywords
     */
    public static int getFilterCount(String filterKeywords) {
        if (filterKeywords == null || filterKeywords.trim().isEmpty()) {
            return 0;
        }

        String[] keywords = filterKeywords.split(",");
        int count = 0;

        for (String keyword : keywords) {
            if (!keyword.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }
} 