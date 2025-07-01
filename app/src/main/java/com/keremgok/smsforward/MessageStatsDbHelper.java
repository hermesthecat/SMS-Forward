package com.keremgok.smsforward;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite database helper for tracking message forwarding statistics.
 * Stores daily and total counts for monitoring and analytics.
 */
public class MessageStatsDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "MessageStatsDbHelper";
    private static final String DATABASE_NAME = "sms_forward_stats.db";
    private static final int DATABASE_VERSION = 2;

    // Table name and columns
    private static final String TABLE_DAILY_STATS = "daily_stats";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_SMS_COUNT = "sms_count";
    private static final String COLUMN_TELEGRAM_COUNT = "telegram_count";
    private static final String COLUMN_EMAIL_COUNT = "email_count";
    private static final String COLUMN_WEB_COUNT = "web_count";
    private static final String COLUMN_TOTAL_COUNT = "total_count";
    private static final String COLUMN_SUCCESS_COUNT = "success_count";
    private static final String COLUMN_FAILED_COUNT = "failed_count";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    // ✅ Performance optimization - Index creation statements
    private static final String[] INDEX_CREATION_STATEMENTS = {
        // Primary date index for fast date lookups
        "CREATE INDEX IF NOT EXISTS idx_date ON " + TABLE_DAILY_STATS + 
        "(" + COLUMN_DATE + " DESC)",
        
        // Total count index for aggregate queries
        "CREATE INDEX IF NOT EXISTS idx_total_count ON " + TABLE_DAILY_STATS + 
        "(" + COLUMN_TOTAL_COUNT + " DESC)",
        
        // Success count index for success rate calculations
        "CREATE INDEX IF NOT EXISTS idx_success_count ON " + TABLE_DAILY_STATS + 
        "(" + COLUMN_SUCCESS_COUNT + " DESC)",
        
        // Composite index for date range queries
        "CREATE INDEX IF NOT EXISTS idx_date_total ON " + TABLE_DAILY_STATS + 
        "(" + COLUMN_DATE + " DESC, " + COLUMN_TOTAL_COUNT + " DESC)",
        
        // Updated timestamp index for recent data queries
        "CREATE INDEX IF NOT EXISTS idx_updated_at ON " + TABLE_DAILY_STATS + 
        "(" + COLUMN_UPDATED_AT + " DESC)"
    };

    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_DAILY_STATS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_DATE + " TEXT UNIQUE NOT NULL," +
            COLUMN_SMS_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_TELEGRAM_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_EMAIL_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_WEB_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_TOTAL_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_SUCCESS_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_FAILED_COUNT + " INTEGER DEFAULT 0," +
            COLUMN_CREATED_AT + " INTEGER NOT NULL," +
            COLUMN_UPDATED_AT + " INTEGER NOT NULL" +
            ")";

    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_DAILY_STATS;

    // Date format for daily stats
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public MessageStatsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating message stats database v" + DATABASE_VERSION);
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
     * Record a successful message forward
     */
    public void recordForwardSuccess(String forwarderType) {
        recordForward(forwarderType, true);
    }

    /**
     * Record a failed message forward
     */
    public void recordForwardFailure(String forwarderType) {
        recordForward(forwarderType, false);
    }

    /**
     * Record a message forward attempt
     */
    private void recordForward(String forwarderType, boolean success) {
        String today = getTodayDateString();
        long currentTime = System.currentTimeMillis();

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Get or create today's record
            DailyStats todayStats = getTodayStats();
            if (todayStats == null) {
                todayStats = createTodayStats();
            }

            // Update counters
            ContentValues values = new ContentValues();

            // Increment forwarder-specific counter
            String forwarderColumn = getForwarderColumn(forwarderType);
            if (forwarderColumn != null) {
                values.put(forwarderColumn, todayStats.getForwarderCount(forwarderType) + 1);
            }

            // Update total and success/failed counters
            values.put(COLUMN_TOTAL_COUNT, todayStats.totalCount + 1);
            if (success) {
                values.put(COLUMN_SUCCESS_COUNT, todayStats.successCount + 1);
            } else {
                values.put(COLUMN_FAILED_COUNT, todayStats.failedCount + 1);
            }
            values.put(COLUMN_UPDATED_AT, currentTime);

            // Update the record
            String whereClause = COLUMN_DATE + " = ?";
            String[] whereArgs = { today };

            int rowsUpdated = db.update(TABLE_DAILY_STATS, values, whereClause, whereArgs);

            if (rowsUpdated > 0) {
                Log.d(TAG, "Updated " + forwarderType + " stats for " + today +
                        " (success: " + success + ")");
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Error recording forward stats", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * ✅ Performance optimization - Batch record multiple forwards at once
     */
    public void recordBatchForwards(List<BatchForwardRecord> batchRecords) {
        if (batchRecords == null || batchRecords.isEmpty()) {
            return;
        }

        String today = getTodayDateString();
        long currentTime = System.currentTimeMillis();

        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Get or create today's record
            DailyStats todayStats = getTodayStats();
            if (todayStats == null) {
                todayStats = createTodayStats();
            }

            // Calculate batch updates
            int smsIncrease = 0, telegramIncrease = 0, emailIncrease = 0, webIncrease = 0;
            int successIncrease = 0, failedIncrease = 0, totalIncrease = 0;

            for (BatchForwardRecord record : batchRecords) {
                // Count by forwarder type
                switch (record.forwarderType.toLowerCase()) {
                    case "sms":
                        smsIncrease++;
                        break;
                    case "telegram":
                        telegramIncrease++;
                        break;
                    case "email":
                        emailIncrease++;
                        break;
                    case "web":
                    case "webhook":
                    case "json":
                        webIncrease++;
                        break;
                }

                // Count by result
                totalIncrease++;
                if (record.success) {
                    successIncrease++;
                } else {
                    failedIncrease++;
                }
            }

            // Apply batch updates
            ContentValues values = new ContentValues();
            values.put(COLUMN_SMS_COUNT, todayStats.smsCount + smsIncrease);
            values.put(COLUMN_TELEGRAM_COUNT, todayStats.telegramCount + telegramIncrease);
            values.put(COLUMN_EMAIL_COUNT, todayStats.emailCount + emailIncrease);
            values.put(COLUMN_WEB_COUNT, todayStats.webCount + webIncrease);
            values.put(COLUMN_TOTAL_COUNT, todayStats.totalCount + totalIncrease);
            values.put(COLUMN_SUCCESS_COUNT, todayStats.successCount + successIncrease);
            values.put(COLUMN_FAILED_COUNT, todayStats.failedCount + failedIncrease);
            values.put(COLUMN_UPDATED_AT, currentTime);

            String whereClause = COLUMN_DATE + " = ?";
            String[] whereArgs = { today };

            int rowsUpdated = db.update(TABLE_DAILY_STATS, values, whereClause, whereArgs);
            
            if (rowsUpdated > 0) {
                Log.d(TAG, "Batch updated stats for " + today + ": " + batchRecords.size() + 
                        " records (" + successIncrease + " success, " + failedIncrease + " failed)");
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Error recording batch forward stats", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * ✅ Performance optimization - Optimized daily stats query with caching
     */
    private DailyStats cachedTodayStats = null;
    private String cachedStatsDate = null;

    public DailyStats getTodayStats() {
        String today = getTodayDateString();
        
        // Check cache first
        if (cachedTodayStats != null && today.equals(cachedStatsDate)) {
            return cachedTodayStats;
        }
        
        // Query from database
        DailyStats stats = getStatsForDate(today);
        
        // Update cache
        cachedTodayStats = stats;
        cachedStatsDate = today;
        
        return stats;
    }

    /**
     * ✅ Performance optimization - Clear cache when stats are updated
     */
    private void clearStatsCache() {
        cachedTodayStats = null;
        cachedStatsDate = null;
    }

    /**
     * ✅ Performance optimization - Batch forward record for batch processing
     */
    public static class BatchForwardRecord {
        public final String forwarderType;
        public final boolean success;
        public final long timestamp;

        public BatchForwardRecord(String forwarderType, boolean success, long timestamp) {
            this.forwarderType = forwarderType;
            this.success = success;
            this.timestamp = timestamp;
        }

        public BatchForwardRecord(String forwarderType, boolean success) {
            this(forwarderType, success, System.currentTimeMillis());
        }
    }

    /**
     * Get statistics for a specific date
     */
    public DailyStats getStatsForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_DATE + " = ?";
        String[] selectionArgs = { date };

        Cursor cursor = db.query(TABLE_DAILY_STATS, null, selection, selectionArgs,
                null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return createDailyStatsFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * Get total statistics (all time)
     */
    public TotalStats getTotalStats() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " +
                "SUM(" + COLUMN_SMS_COUNT + ") as total_sms, " +
                "SUM(" + COLUMN_TELEGRAM_COUNT + ") as total_telegram, " +
                "SUM(" + COLUMN_EMAIL_COUNT + ") as total_email, " +
                "SUM(" + COLUMN_WEB_COUNT + ") as total_web, " +
                "SUM(" + COLUMN_TOTAL_COUNT + ") as total_all, " +
                "SUM(" + COLUMN_SUCCESS_COUNT + ") as total_success, " +
                "SUM(" + COLUMN_FAILED_COUNT + ") as total_failed, " +
                "COUNT(*) as total_days " +
                "FROM " + TABLE_DAILY_STATS;

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (cursor.moveToFirst()) {
                TotalStats stats = new TotalStats();
                stats.smsCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_sms"));
                stats.telegramCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_telegram"));
                stats.emailCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_email"));
                stats.webCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_web"));
                stats.totalCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_all"));
                stats.successCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_success"));
                stats.failedCount = cursor.getInt(cursor.getColumnIndexOrThrow("total_failed"));
                stats.activeDays = cursor.getInt(cursor.getColumnIndexOrThrow("total_days"));
                return stats;
            }
        } finally {
            cursor.close();
        }

        return new TotalStats(); // Return empty stats if no data
    }

    /**
     * Get recent daily statistics (last N days)
     */
    public Map<String, DailyStats> getRecentStats(int days) {
        Map<String, DailyStats> recentStats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderBy = COLUMN_DATE + " DESC LIMIT " + days;
        Cursor cursor = db.query(TABLE_DAILY_STATS, null, null, null,
                null, null, orderBy);

        try {
            while (cursor.moveToNext()) {
                DailyStats stats = createDailyStatsFromCursor(cursor);
                recentStats.put(stats.date, stats);
            }
        } finally {
            cursor.close();
        }

        return recentStats;
    }

    /**
     * Clean up old statistics (older than specified days)
     */
    public void cleanupOldStats(int keepDays) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Calculate cutoff date
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);
        String cutoffDate = DATE_FORMAT.format(new Date(cutoffTime));

        String whereClause = COLUMN_DATE + " < ?";
        String[] whereArgs = { cutoffDate };

        int rowsDeleted = db.delete(TABLE_DAILY_STATS, whereClause, whereArgs);
        if (rowsDeleted > 0) {
            Log.d(TAG, "Cleaned up " + rowsDeleted + " old daily stats records");
        }
    }

    /**
     * Create today's stats record if it doesn't exist
     */
    private DailyStats createTodayStats() {
        String today = getTodayDateString();
        long currentTime = System.currentTimeMillis();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_DATE, today);
        values.put(COLUMN_SMS_COUNT, 0);
        values.put(COLUMN_TELEGRAM_COUNT, 0);
        values.put(COLUMN_EMAIL_COUNT, 0);
        values.put(COLUMN_WEB_COUNT, 0);
        values.put(COLUMN_TOTAL_COUNT, 0);
        values.put(COLUMN_SUCCESS_COUNT, 0);
        values.put(COLUMN_FAILED_COUNT, 0);
        values.put(COLUMN_CREATED_AT, currentTime);
        values.put(COLUMN_UPDATED_AT, currentTime);

        long id = db.insert(TABLE_DAILY_STATS, null, values);
        Log.d(TAG, "Created new daily stats record for " + today + " (ID: " + id + ")");

        return getTodayStats();
    }

    /**
     * Create DailyStats object from cursor
     */
    private DailyStats createDailyStatsFromCursor(Cursor cursor) {
        DailyStats stats = new DailyStats();
        stats.date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
        stats.smsCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SMS_COUNT));
        stats.telegramCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TELEGRAM_COUNT));
        stats.emailCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EMAIL_COUNT));
        stats.webCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEB_COUNT));
        stats.totalCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_COUNT));
        stats.successCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUCCESS_COUNT));
        stats.failedCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_COUNT));
        stats.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        stats.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
        return stats;
    }

    /**
     * Get column name for forwarder type
     */
    private String getForwarderColumn(String forwarderType) {
        switch (forwarderType.toLowerCase()) {
            case "smsforwarder":
                return COLUMN_SMS_COUNT;
            case "telegramforwarder":
                return COLUMN_TELEGRAM_COUNT;
            case "emailforwarder":
                return COLUMN_EMAIL_COUNT;
            case "jsonwebforwarder":
                return COLUMN_WEB_COUNT;
            default:
                Log.w(TAG, "Unknown forwarder type: " + forwarderType);
                return null;
        }
    }

    /**
     * Get today's date as string
     */
    private String getTodayDateString() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * Daily statistics data class
     */
    public static class DailyStats {
        public String date;
        public int smsCount;
        public int telegramCount;
        public int emailCount;
        public int webCount;
        public int totalCount;
        public int successCount;
        public int failedCount;
        public long createdAt;
        public long updatedAt;

        public int getForwarderCount(String forwarderType) {
            switch (forwarderType.toLowerCase()) {
                case "smsforwarder":
                    return smsCount;
                case "telegramforwarder":
                    return telegramCount;
                case "emailforwarder":
                    return emailCount;
                case "jsonwebforwarder":
                    return webCount;
                default:
                    return 0;
            }
        }

        public double getSuccessRate() {
            if (totalCount == 0)
                return 0.0;
            return (double) successCount / totalCount * 100.0;
        }
    }

    /**
     * Total statistics data class
     */
    public static class TotalStats {
        public int smsCount;
        public int telegramCount;
        public int emailCount;
        public int webCount;
        public int totalCount;
        public int successCount;
        public int failedCount;
        public int activeDays;

        public double getSuccessRate() {
            if (totalCount == 0)
                return 0.0;
            return (double) successCount / totalCount * 100.0;
        }

        public double getAveragePerDay() {
            if (activeDays == 0)
                return 0.0;
            return (double) totalCount / activeDays;
        }
    }
}