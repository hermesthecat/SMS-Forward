package com.keremgok.smsforward;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite database helper for storing message forwarding history.
 * Maintains the last 100 forwarded messages for user review.
 */
public class MessageHistoryDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "MessageHistoryDbHelper";
    private static final String DATABASE_NAME = "sms_forward_history.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and columns
    private static final String TABLE_MESSAGE_HISTORY = "message_history";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_FROM_NUMBER = "from_number";
    private static final String COLUMN_MESSAGE_CONTENT = "message_content";
    private static final String COLUMN_PLATFORM = "platform";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_ERROR_MESSAGE = "error_message";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_FORWARD_TIMESTAMP = "forward_timestamp";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Status values
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";

    // Maximum number of history records to keep
    private static final int MAX_HISTORY_RECORDS = 100;

    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_MESSAGE_HISTORY + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_FROM_NUMBER + " TEXT NOT NULL," +
            COLUMN_MESSAGE_CONTENT + " TEXT NOT NULL," +
            COLUMN_PLATFORM + " TEXT NOT NULL," +
            COLUMN_STATUS + " TEXT NOT NULL," +
            COLUMN_ERROR_MESSAGE + " TEXT," +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL," +
            COLUMN_FORWARD_TIMESTAMP + " INTEGER NOT NULL," +
            COLUMN_CREATED_AT + " INTEGER NOT NULL" +
            ")";

    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_MESSAGE_HISTORY;

    public MessageHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating message history database");
        db.execSQL(SQL_CREATE_TABLE);

        // Create index for better query performance
        db.execSQL("CREATE INDEX idx_forward_timestamp ON " + TABLE_MESSAGE_HISTORY +
                "(" + COLUMN_FORWARD_TIMESTAMP + " DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    /**
     * Record a successful message forward
     */
    public void recordForwardSuccess(String fromNumber, String messageContent, String platform,
            long originalTimestamp) {
        recordForward(fromNumber, messageContent, platform, STATUS_SUCCESS, null, originalTimestamp);
    }

    /**
     * Record a failed message forward
     */
    public void recordForwardFailure(String fromNumber, String messageContent, String platform,
            String errorMessage, long originalTimestamp) {
        recordForward(fromNumber, messageContent, platform, STATUS_FAILED, errorMessage, originalTimestamp);
    }

    /**
     * Record a message forward attempt
     */
    private void recordForward(String fromNumber, String messageContent, String platform,
            String status, String errorMessage, long originalTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        long currentTime = System.currentTimeMillis();

        try {
            db.beginTransaction();

            // Insert new record
            ContentValues values = new ContentValues();
            values.put(COLUMN_FROM_NUMBER, fromNumber);
            values.put(COLUMN_MESSAGE_CONTENT, truncateMessage(messageContent));
            values.put(COLUMN_PLATFORM, platform);
            values.put(COLUMN_STATUS, status);
            values.put(COLUMN_ERROR_MESSAGE, errorMessage);
            values.put(COLUMN_TIMESTAMP, originalTimestamp);
            values.put(COLUMN_FORWARD_TIMESTAMP, currentTime);
            values.put(COLUMN_CREATED_AT, currentTime);

            long newId = db.insert(TABLE_MESSAGE_HISTORY, null, values);

            if (newId != -1) {
                Log.d(TAG, "Recorded message history: " + platform + " (" + status + ")");

                // Cleanup old records to maintain limit
                cleanupOldRecords(db);

                db.setTransactionSuccessful();
            } else {
                Log.e(TAG, "Failed to insert message history record");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error recording message history", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Get message history (last N records)
     */
    public List<HistoryRecord> getMessageHistory(int limit) {
        List<HistoryRecord> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderBy = COLUMN_FORWARD_TIMESTAMP + " DESC";
        String limitStr = String.valueOf(Math.min(limit, MAX_HISTORY_RECORDS));

        Cursor cursor = db.query(TABLE_MESSAGE_HISTORY, null, null, null,
                null, null, orderBy, limitStr);

        try {
            while (cursor.moveToNext()) {
                HistoryRecord record = createHistoryRecordFromCursor(cursor);
                history.add(record);
            }
        } finally {
            cursor.close();
        }

        Log.d(TAG, "Retrieved " + history.size() + " history records");
        return history;
    }

    /**
     * Get message history for a specific platform
     */
    public List<HistoryRecord> getMessageHistoryByPlatform(String platform, int limit) {
        List<HistoryRecord> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_PLATFORM + " = ?";
        String[] selectionArgs = { platform };
        String orderBy = COLUMN_FORWARD_TIMESTAMP + " DESC";
        String limitStr = String.valueOf(Math.min(limit, MAX_HISTORY_RECORDS));

        Cursor cursor = db.query(TABLE_MESSAGE_HISTORY, null, selection, selectionArgs,
                null, null, orderBy, limitStr);

        try {
            while (cursor.moveToNext()) {
                HistoryRecord record = createHistoryRecordFromCursor(cursor);
                history.add(record);
            }
        } finally {
            cursor.close();
        }

        return history;
    }

    /**
     * Get recent message history statistics
     */
    public HistoryStats getHistoryStats() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " +
                "COUNT(*) as total_count, " +
                "SUM(CASE WHEN " + COLUMN_STATUS + " = ? THEN 1 ELSE 0 END) as success_count, " +
                "SUM(CASE WHEN " + COLUMN_STATUS + " = ? THEN 1 ELSE 0 END) as failed_count, " +
                "COUNT(DISTINCT " + COLUMN_PLATFORM + ") as platform_count, " +
                "MIN(" + COLUMN_FORWARD_TIMESTAMP + ") as oldest_timestamp, " +
                "MAX(" + COLUMN_FORWARD_TIMESTAMP + ") as newest_timestamp " +
                "FROM " + TABLE_MESSAGE_HISTORY;

        String[] args = { STATUS_SUCCESS, STATUS_FAILED };

        Cursor cursor = db.rawQuery(query, args);

        try {
            if (cursor.moveToFirst()) {
                HistoryStats stats = new HistoryStats();
                stats.totalCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_count"));
                stats.successCount = cursor.getInt(cursor.getColumnIndexOrThrow("success_count"));
                stats.failedCount = cursor.getInt(cursor.getColumnIndexOrThrow("failed_count"));
                stats.platformCount = cursor.getInt(cursor.getColumnIndexOrThrow("platform_count"));
                stats.oldestTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("oldest_timestamp"));
                stats.newestTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("newest_timestamp"));
                return stats;
            }
        } finally {
            cursor.close();
        }

        return new HistoryStats(); // Return empty stats if no data
    }

    /**
     * Clear all message history
     */
    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int deletedCount = db.delete(TABLE_MESSAGE_HISTORY, null, null);
            Log.i(TAG, "Cleared " + deletedCount + " history records");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing message history", e);
        }
    }

    /**
     * Cleanup old records to maintain the maximum limit
     */
    private void cleanupOldRecords(SQLiteDatabase db) {
        try {
            // Count current records
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MESSAGE_HISTORY, null);
            int currentCount = 0;
            if (cursor.moveToFirst()) {
                currentCount = cursor.getInt(0);
            }
            cursor.close();

            // Delete excess records if over limit
            if (currentCount > MAX_HISTORY_RECORDS) {
                int recordsToDelete = currentCount - MAX_HISTORY_RECORDS;

                String deleteQuery = "DELETE FROM " + TABLE_MESSAGE_HISTORY +
                        " WHERE " + COLUMN_ID + " IN (" +
                        "SELECT " + COLUMN_ID + " FROM " + TABLE_MESSAGE_HISTORY +
                        " ORDER BY " + COLUMN_FORWARD_TIMESTAMP + " ASC LIMIT " + recordsToDelete + ")";

                db.execSQL(deleteQuery);
                Log.d(TAG, "Cleaned up " + recordsToDelete + " old history records");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    /**
     * Truncate message content to prevent very long messages from consuming too
     * much space
     */
    private String truncateMessage(String message) {
        if (message == null)
            return "";

        final int MAX_MESSAGE_LENGTH = 500; // Store up to 500 characters
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return message;
    }

    /**
     * Create HistoryRecord from cursor
     */
    private HistoryRecord createHistoryRecordFromCursor(Cursor cursor) {
        HistoryRecord record = new HistoryRecord();
        record.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        record.fromNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FROM_NUMBER));
        record.messageContent = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CONTENT));
        record.platform = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLATFORM));
        record.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
        record.errorMessage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ERROR_MESSAGE));
        record.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
        record.forwardTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FORWARD_TIMESTAMP));
        record.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        return record;
    }

    /**
     * Represents a single message history record
     */
    public static class HistoryRecord {
        public long id;
        public String fromNumber;
        public String messageContent;
        public String platform;
        public String status;
        public String errorMessage;
        public long timestamp; // Original SMS timestamp
        public long forwardTimestamp; // When it was forwarded
        public long createdAt;

        public boolean isSuccess() {
            return STATUS_SUCCESS.equals(status);
        }

        public boolean isFailed() {
            return STATUS_FAILED.equals(status);
        }

        public String getFormattedTimestamp() {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            return formatter.format(new Date(timestamp));
        }

        public String getFormattedForwardTimestamp() {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            return formatter.format(new Date(forwardTimestamp));
        }

        public String getStatusEmoji() {
            switch (status) {
                case STATUS_SUCCESS:
                    return "✅";
                case STATUS_FAILED:
                    return "❌";
                case STATUS_PENDING:
                    return "⏳";
                default:
                    return "❓";
            }
        }

        public String getPlatformEmoji() {
            switch (platform.toLowerCase()) {
                case "sms":
                    return "📱";
                case "telegram":
                    return "📢";
                case "email":
                    return "📧";
                case "web":
                case "webhook":
                    return "🌐";
                default:
                    return "📤";
            }
        }
    }

    /**
     * Represents message history statistics
     */
    public static class HistoryStats {
        public int totalCount = 0;
        public int successCount = 0;
        public int failedCount = 0;
        public int platformCount = 0;
        public long oldestTimestamp = 0;
        public long newestTimestamp = 0;

        public double getSuccessRate() {
            if (totalCount == 0)
                return 0.0;
            return (double) successCount / totalCount * 100.0;
        }

        public String getTimeSpanDescription() {
            if (oldestTimestamp == 0 || newestTimestamp == 0) {
                return "No history available";
            }

            long spanMs = newestTimestamp - oldestTimestamp;
            long spanDays = spanMs / (24 * 60 * 60 * 1000);

            if (spanDays == 0) {
                return "Today";
            } else if (spanDays == 1) {
                return "Last 2 days";
            } else {
                return "Last " + (spanDays + 1) + " days";
            }
        }
    }
}