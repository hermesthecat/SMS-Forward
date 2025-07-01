package com.keremgok.smsforward;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database helper for managing offline message queue.
 * Stores failed messages for later retry when connection is restored.
 */
public class MessageQueueDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "MessageQueueDbHelper";
    private static final String DATABASE_NAME = "sms_forward_queue.db";
    private static final int DATABASE_VERSION = 2;

    // Table name and columns
    private static final String TABLE_MESSAGE_QUEUE = "message_queue";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_FROM_NUMBER = "from_number";
    private static final String COLUMN_MESSAGE_CONTENT = "message_content";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_FORWARDER_TYPE = "forwarder_type";
    private static final String COLUMN_FORWARDER_CONFIG = "forwarder_config";
    private static final String COLUMN_RETRY_COUNT = "retry_count";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_LAST_RETRY_AT = "last_retry_at";
    private static final String COLUMN_STATUS = "status";

    // Status values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SUCCESS = "SUCCESS";

    // ✅ Performance optimization - Index creation statements
    private static final String[] INDEX_CREATION_STATEMENTS = {
        // Primary status index for queue processing
        "CREATE INDEX IF NOT EXISTS idx_status ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_STATUS + ")",
        
        // Created timestamp index for chronological processing
        "CREATE INDEX IF NOT EXISTS idx_created_at ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_CREATED_AT + " ASC)",
        
        // Composite index for pending message queries (most important for performance)
        "CREATE INDEX IF NOT EXISTS idx_status_created ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_STATUS + ", " + COLUMN_CREATED_AT + " ASC)",
        
        // Retry count index for retry logic
        "CREATE INDEX IF NOT EXISTS idx_retry_count ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_RETRY_COUNT + ")",
        
        // Forwarder type index for type-based queries
        "CREATE INDEX IF NOT EXISTS idx_forwarder_type ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_FORWARDER_TYPE + ")",
        
        // Last retry timestamp index for retry timing
        "CREATE INDEX IF NOT EXISTS idx_last_retry_at ON " + TABLE_MESSAGE_QUEUE + 
        "(" + COLUMN_LAST_RETRY_AT + " DESC)"
    };

    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_MESSAGE_QUEUE + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_FROM_NUMBER + " TEXT NOT NULL," +
            COLUMN_MESSAGE_CONTENT + " TEXT NOT NULL," +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL," +
            COLUMN_FORWARDER_TYPE + " TEXT NOT NULL," +
            COLUMN_FORWARDER_CONFIG + " TEXT NOT NULL," +
            COLUMN_RETRY_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_CREATED_AT + " INTEGER NOT NULL," +
            COLUMN_LAST_RETRY_AT + " INTEGER," +
            COLUMN_STATUS + " TEXT DEFAULT '" + STATUS_PENDING + "'" +
            ")";

    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_MESSAGE_QUEUE;

    public MessageQueueDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating message queue database v" + DATABASE_VERSION);
        db.execSQL(SQL_CREATE_TABLE);
        
        // ✅ Create performance indexes
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        if (oldVersion < 2) {
            // ✅ Performance optimization v1.21.0 - Add missing indexes
            Log.d(TAG, "Adding performance indexes for v1.21.0");
            createIndexes(db);
        }
        
        // Note: We don't drop the table anymore to preserve data during upgrades
    }

    /**
     * ✅ Performance optimization - Create all performance indexes
     */
    private void createIndexes(SQLiteDatabase db) {
        for (String indexStatement : INDEX_CREATION_STATEMENTS) {
            try {
                db.execSQL(indexStatement);
                Log.d(TAG, "Created index: " + indexStatement.substring(0, Math.min(50, indexStatement.length())) + "...");
            } catch (Exception e) {
                Log.e(TAG, "Error creating index: " + indexStatement, e);
            }
        }
    }

    /**
     * Add a failed message to the queue for later retry
     */
    public long enqueueMessage(String fromNumber, String messageContent, long timestamp,
            String forwarderType, String forwarderConfig) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FROM_NUMBER, fromNumber);
        values.put(COLUMN_MESSAGE_CONTENT, messageContent);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_FORWARDER_TYPE, forwarderType);
        values.put(COLUMN_FORWARDER_CONFIG, forwarderConfig);
        values.put(COLUMN_RETRY_COUNT, 0);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        values.put(COLUMN_STATUS, STATUS_PENDING);

        long id = db.insert(TABLE_MESSAGE_QUEUE, null, values);
        Log.d(TAG, "Enqueued message with ID: " + id + " from " + fromNumber + " via " + forwarderType);

        return id;
    }

    /**
     * Get all pending messages that need to be retried
     */
    public List<QueuedMessage> getPendingMessages() {
        List<QueuedMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_STATUS + " IN (?, ?)";
        String[] selectionArgs = { STATUS_PENDING, STATUS_FAILED };
        String orderBy = COLUMN_CREATED_AT + " ASC";

        Cursor cursor = db.query(TABLE_MESSAGE_QUEUE, null, selection, selectionArgs,
                null, null, orderBy);

        try {
            while (cursor.moveToNext()) {
                QueuedMessage message = new QueuedMessage();
                message.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                message.fromNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FROM_NUMBER));
                message.messageContent = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CONTENT));
                message.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                message.forwarderType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORWARDER_TYPE));
                message.forwarderConfig = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORWARDER_CONFIG));
                message.retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RETRY_COUNT));
                message.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));

                int lastRetryIndex = cursor.getColumnIndex(COLUMN_LAST_RETRY_AT);
                if (lastRetryIndex != -1 && !cursor.isNull(lastRetryIndex)) {
                    message.lastRetryAt = cursor.getLong(lastRetryIndex);
                }

                message.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                messages.add(message);
            }
        } finally {
            cursor.close();
        }

        Log.d(TAG, "Retrieved " + messages.size() + " pending messages from queue");
        return messages;
    }

    /**
     * Update message status and retry information
     */
    public void updateMessage(long id, String status, int retryCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_RETRY_COUNT, retryCount);
        values.put(COLUMN_LAST_RETRY_AT, System.currentTimeMillis());

        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = { String.valueOf(id) };

        int rowsUpdated = db.update(TABLE_MESSAGE_QUEUE, values, whereClause, whereArgs);
        Log.d(TAG, "Updated message " + id + " status to " + status + " (retry " + retryCount + ")");
    }

    /**
     * Mark message as successfully processed and remove from queue
     */
    public void markMessageSuccess(long id) {
        updateMessage(id, STATUS_SUCCESS, -1);
        deleteMessage(id);
    }

    /**
     * Mark message as failed after all retries exhausted
     */
    public void markMessageFailed(long id, int finalRetryCount) {
        updateMessage(id, STATUS_FAILED, finalRetryCount);
    }

    /**
     * Delete a message from the queue
     */
    public void deleteMessage(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_ID + " = ?";
        String[] whereArgs = { String.valueOf(id) };

        int rowsDeleted = db.delete(TABLE_MESSAGE_QUEUE, whereClause, whereArgs);
        Log.d(TAG, "Deleted " + rowsDeleted + " message(s) with ID " + id);
    }

    /**
     * Get queue statistics
     */
    public QueueStats getQueueStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        QueueStats stats = new QueueStats();

        // Count by status
        String[] statuses = { STATUS_PENDING, STATUS_PROCESSING, STATUS_FAILED, STATUS_SUCCESS };
        for (String status : statuses) {
            String selection = COLUMN_STATUS + " = ?";
            String[] selectionArgs = { status };

            Cursor cursor = db.query(TABLE_MESSAGE_QUEUE, new String[] { "COUNT(*)" },
                    selection, selectionArgs, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    int count = cursor.getInt(0);
                    switch (status) {
                        case STATUS_PENDING:
                            stats.pendingCount = count;
                            break;
                        case STATUS_PROCESSING:
                            stats.processingCount = count;
                            break;
                        case STATUS_FAILED:
                            stats.failedCount = count;
                            break;
                        case STATUS_SUCCESS:
                            stats.successCount = count;
                            break;
                    }
                }
            } finally {
                cursor.close();
            }
        }

        // Get oldest pending message age
        String selection = COLUMN_STATUS + " IN (?, ?)";
        String[] selectionArgs = { STATUS_PENDING, STATUS_FAILED };
        String orderBy = COLUMN_CREATED_AT + " ASC LIMIT 1";

        Cursor cursor = db.query(TABLE_MESSAGE_QUEUE, new String[] { COLUMN_CREATED_AT },
                selection, selectionArgs, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                long oldestTime = cursor.getLong(0);
                stats.oldestPendingAge = System.currentTimeMillis() - oldestTime;
            }
        } finally {
            cursor.close();
        }

        stats.totalCount = stats.pendingCount + stats.processingCount + stats.failedCount + stats.successCount;
        return stats;
    }

    /**
     * Clean up old successful messages (older than 24 hours)
     */
    public void cleanupOldMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago

        String whereClause = COLUMN_STATUS + " = ? AND " + COLUMN_CREATED_AT + " < ?";
        String[] whereArgs = { STATUS_SUCCESS, String.valueOf(cutoffTime) };

        int rowsDeleted = db.delete(TABLE_MESSAGE_QUEUE, whereClause, whereArgs);
        if (rowsDeleted > 0) {
            Log.d(TAG, "Cleaned up " + rowsDeleted + " old successful messages");
        }
    }

    /**
     * Represents a queued message
     */
    public static class QueuedMessage {
        public long id;
        public String fromNumber;
        public String messageContent;
        public long timestamp;
        public String forwarderType;
        public String forwarderConfig;
        public int retryCount;
        public long createdAt;
        public long lastRetryAt;
        public String status;
    }

    /**
     * Queue statistics
     */
    public static class QueueStats {
        public int totalCount;
        public int pendingCount;
        public int processingCount;
        public int failedCount;
        public int successCount;
        public long oldestPendingAge;
    }
}