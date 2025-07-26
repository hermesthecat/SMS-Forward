package com.keremgok.smsforward;

import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle SMS filtering based on a sender number whitelist.
 */
public class SmsNumberFilter {
    private static final String TAG = "SmsNumberFilter";

    /**
     * Checks if a message from a given number should be blocked based on a whitelist.
     * If the whitelist is enabled but empty, it will block all numbers.
     *
     * @param fromNumber      The sender's phone number.
     * @param numberWhitelist A comma-separated string of whitelisted numbers.
     * @return true if the number is NOT in the whitelist, otherwise false.
     */
    public static boolean shouldBlockNumber(String fromNumber, String numberWhitelist) {
        if (numberWhitelist == null || numberWhitelist.trim().isEmpty()) {
            // If the whitelist is empty, we block all messages.
            return true;
        }

        String[] whitelist = numberWhitelist.split(",");
        for (String whitelistedNumber : whitelist) {
            if (!TextUtils.isEmpty(whitelistedNumber) && PhoneNumberUtils.compare(fromNumber, whitelistedNumber.trim())) {
                // The number is in the whitelist, do not block.
                return false;
            }
        }

        // The number was not found in the whitelist, so it should be blocked.
        return true;
    }

    /**
     * Generates a summary of the active whitelist for display in the UI.
     *
     * @param numberWhitelist A comma-separated string of whitelisted numbers.
     * @return A truncated summary of the first few numbers in the list.
     */
    public static String getWhitelistSummary(String numberWhitelist) {
        if (numberWhitelist == null || numberWhitelist.trim().isEmpty()) {
            return null;
        }

        String[] numbers = numberWhitelist.split(",");
        List<String> cleanedNumbers = new ArrayList<>();
        for (String number : numbers) {
            if (!number.trim().isEmpty()) {
                cleanedNumbers.add(number.trim());
            }
        }

        if (cleanedNumbers.isEmpty()) {
            return null;
        }

        int count = cleanedNumbers.size();
        int displayCount = Math.min(count, 3);
        StringBuilder summary = new StringBuilder();

        for (int i = 0; i < displayCount; i++) {
            summary.append(cleanedNumbers.get(i));
            if (i < displayCount - 1) {
                summary.append(", ");
            }
        }

        if (count > displayCount) {
            summary.append("... (").append(count).append(" total)");
        }

        return summary.toString();
    }

    /**
     * Cleans and normalizes a comma-separated string of phone numbers.
     *
     * @param numberWhitelist The raw string from the settings preference.
     * @return A clean, comma-separated string with no extra spaces or empty items.
     */
    public static String cleanWhitelist(String numberWhitelist) {
        if (numberWhitelist == null || numberWhitelist.trim().isEmpty()) {
            return "";
        }

        String[] numbers = numberWhitelist.split(",");
        List<String> cleanedList = new ArrayList<>();
        for (String number : numbers) {
            String cleaned = number.trim();
            if (!cleaned.isEmpty()) {
                cleanedList.add(cleaned);
            }
        }
        return TextUtils.join(",", cleanedList);
    }
} 