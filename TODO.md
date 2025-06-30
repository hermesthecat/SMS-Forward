# SMS Forward - TODO List

## 🚨 **High Priority (Next Release)**

### Critical Features

- [x] **Retry Mechanism** - Auto-retry failed forwards (3 attempts) ✅ _Completed in v1.3.0_
- [x] **Test Message Button** - Quick functionality test ✅ _Completed in v1.2.0_
- [x] **Connection Status Indicator** - Show online/offline status ✅ _Completed in v1.5.0_
- [x] **Offline Message Queue** - Store failed messages for retry ✅ _Completed in v1.4.0_
- [ ] **Number Whitelist** - Only forward from specific numbers
- [x] **SMS Content Filter** - Block messages containing specific keywords (comma-separated list) ✅ _Completed in v1.13.0_

### UI Improvements

- [x] **Message Counter** - Show daily forwarded count ✅ _Completed in v1.6.0_
- [x] **Dark Mode Support** - System theme following ✅ _Completed in v1.7.0_
- [x] **Multi-language Support** - Turkish and English localization ✅ _Completed in v1.11.0_
- [x] **About Page** - App information, version, and developer details ✅ _Completed in v1.11.0_
- [ ] **Last Status Display** - Show last forward result
- [ ] **Better Error Messages** - User-friendly error descriptions

## 🔧 **Medium Priority**

### Security

- [ ] **PIN Protection** - Lock app with PIN/biometric
- [x] **Rate Limiting** - Prevent spam (max 10 SMS/minute) ✅ _Completed in v1.8.0_
- [ ] **Secure Storage** - Encrypt sensitive settings
- [ ] **Input Validation** - Validate all user inputs

### Features

- [x] **Statistics Dashboard** - Daily/weekly stats ✅ _Basic version completed in v1.6.0_
- [x] **Export/Import Settings** - Backup configuration ✅ _Completed in v1.9.0_
- [x] **Message History** - Last 100 forwarded messages ✅ _Completed in v1.10.0_
- [ ] **Custom Message Templates** - Personalize message format

### New Platforms

- [ ] **Discord Integration** - Webhook support
- [ ] **Slack Integration** - Bot API support
- [ ] **Microsoft Teams** - Webhook support

## 🌟 **Future Ideas**

### Advanced Features

- [ ] **Smart Notifications** - Group similar notifications
- [ ] **Quiet Hours** - Disable forwarding at night
- [ ] **Keyword Routing** - Route to different platforms by content
- [ ] **AI Spam Detection** - Machine learning spam filter

### Enterprise Features

- [ ] **Multiple Device Sync** - Cloud configuration
- [ ] **Centralized Management** - Admin dashboard
- [ ] **Audit Logs** - Compliance logging
- [ ] **API Gateway** - REST API for management

## 📋 **Technical Debt**

### Code Quality

- [ ] **Unit Tests** - Add comprehensive test coverage
- [x] **Memory Leaks** - Fix potential memory issues ✅ _Completed in v1.12.0_
- [ ] **Code Documentation** - Add JavaDoc comments
- [ ] **Performance Optimization** - Reduce memory usage

### Architecture

- [ ] **Database Migration** - Add SQLite for data storage
- [ ] **Dependency Injection** - Use modern DI framework
- [ ] **Background Processing** - Use JobScheduler/WorkManager
- [x] **Resource Management** - Proper cleanup of executors and connections ✅ _Completed in v1.12.0_

## 🚀 **Quick Wins (Easy Implementation)**

1. ~~**Test Message Button** - 1 day~~ ✅ _Completed_
2. ~~**Message Counter** - 1 day~~ ✅ _Completed_
3. ~~**Connection Status** - 2 days~~ ✅ _Completed_
4. ~~**Dark Mode** - 1 day~~ ✅ _Completed_
5. ~~**Message History** - 2 days~~ ✅ _Completed_
6. ~~**Memory Leak Fixes** - 2 days~~ ✅ _Completed_
7. **Better Error Messages** - 2 days

## 📝 **Implementation Notes**

### Memory Leak Fixes ✅ COMPLETED

```java
// Fixed static context references in SmsReceiver
public class SmsReceiver extends BroadcastReceiver {
    // Removed static fields that held context references
    // Initialize instances per onReceive call to avoid leaks
    
    @Override
    public void onReceive(Context context, Intent intent) {
        MessageQueueProcessor queueProcessor = new MessageQueueProcessor(context);
        MessageStatsDbHelper statsDbHelper = new MessageStatsDbHelper(context);
        MessageHistoryDbHelper historyDbHelper = new MessageHistoryDbHelper(context);
        // ... rest of implementation
    }
}

// Added proper cleanup in MainActivity
@Override
public void onDestroy() {
    super.onDestroy();
    // Cleanup to prevent memory leaks
    if (networkStatusManager != null) {
        networkStatusManager.removeListener(this);
        networkStatusManager.stopMonitoring();
    }
    
    // Close database helpers to free resources
    if (historyDbHelper != null) {
        historyDbHelper.close();
    }
}

// Added RetryableForwarder cleanup in test methods
for (RetryableForwarder retryableForwarder : retryableForwarders) {
    retryableForwarder.shutdown();
}
```

### Retry Mechanism

```java
// Pseudo-code for retry system
public class RetryManager {
    private static final int MAX_RETRIES = 3;

    public void scheduleRetry(FailedMessage message) {
        // Use AlarmManager or WorkManager
        // Exponential backoff: 5s, 10s, 20s
    }
}
```

### Message Queue

```sql
-- SQLite table for offline queue
CREATE TABLE message_queue (
    id INTEGER PRIMARY KEY,
    from_number TEXT NOT NULL,
    content TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    platform TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0
);
```

### Statistics

```java
// Daily stats tracking
public class StatsManager {
    public void recordForward(String platform, boolean success);
    public DailyStats getToday();
    public WeeklyStats getThisWeek();
}
```

### Message History

```java
// Message history tracking ✅ COMPLETED
public class MessageHistoryDbHelper {
    private static final int MAX_HISTORY_RECORDS = 100;

    public void recordForwardSuccess(String fromNumber, String messageContent,
                                   String platform, long originalTimestamp) {
        // Store successful forward with full details
    }

    public void recordForwardFailure(String fromNumber, String messageContent,
                                   String platform, String errorMessage, long originalTimestamp) {
        // Store failed forward with error details
    }

    public List<HistoryRecord> getMessageHistory(int limit) {
        // Retrieve last N messages with status, platform, timestamps
        // Automatic cleanup maintains 100 record limit
    }

    public HistoryStats getHistoryStats() {
        // Get success rate, time span, platform distribution
    }
}
```

### SMS Content Filter

```java
// Content filtering system
public class SmsContentFilter {
    public static boolean shouldBlockMessage(String messageContent, String filterKeywords) {
        if (filterKeywords == null || filterKeywords.trim().isEmpty()) {
            return false; // No filters defined
        }
        
        String[] keywords = filterKeywords.split(",");
        String contentLowerCase = messageContent.toLowerCase();
        
        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim().toLowerCase();
            if (!trimmedKeyword.isEmpty() && contentLowerCase.contains(trimmedKeyword)) {
                return true; // Block message
            }
        }
        
        return false; // Allow message
    }
}

// Implementation in SmsReceiver:
String filterKeywords = preferences.getString(
    context.getString(R.string.key_filter_keywords), "");

if (SmsContentFilter.shouldBlockMessage(messageContent, filterKeywords)) {
    Log.i(TAG, "Message blocked by content filter: " + senderNumber);
    return; // Don't forward the message
}

// Settings UI:
// - EditTextPreference for keyword list
// - Summary showing current filters
// - Help text explaining comma separation
// - Case-insensitive matching
```

---

## ✅ **Recently Completed**

### Version 1.13.0 - SMS Content Filter

- [x] **SmsContentFilter Class** - Static utility class for content filtering
- [x] **Keyword-based Filtering** - Comma-separated keyword list with case-insensitive matching
- [x] **SmsReceiver Integration** - Pre-filtering messages before forwarding
- [x] **User Interface** - EditTextPreference with dynamic summary updates
- [x] **Settings Backup Support** - Filter keywords included in backup/restore
- [x] **Multi-language Support** - Complete Turkish and English localization
- [x] **Input Validation** - Automatic keyword cleaning and formatting

Implementation details:

- **Content Filtering**: Messages containing any configured keyword are blocked
- **Case Insensitive**: Filtering ignores capitalization (SPAM = spam = Spam)
- **Comma Separation**: Users can enter multiple keywords: "spam,promotion,advertisement"
- **Real-time Updates**: Summary shows active filters with live preview
- **Integration Point**: Filtering occurs in SmsReceiver before any forwarding
- **Performance**: Lightweight string matching with early exit optimization

Features:

- **Smart Summary**: Shows "Active filters: spam, promotion" or "No content filters active"
- **Input Cleaning**: Automatically removes empty keywords and normalizes spacing
- **Backup Compatible**: Filter settings included in settings export/import
- **Logging**: Blocked messages are logged for debugging with keyword identification

### Version 1.12.0 - Memory Leak Fixes

- [x] **Static Context References** - Removed static fields in SmsReceiver that held context references
- [x] **Database Helper Cleanup** - Added proper close() calls for database helpers
- [x] **NetworkStatusManager Cleanup** - Added stopMonitoring() calls in onDestroy lifecycle
- [x] **ExecutorService Shutdown** - Added shutdown() calls for RetryableForwarder executors
- [x] **Application Cleanup** - Added onTerminate() cleanup in Application class
- [x] **Resource Management** - Proper cleanup of all background threads and connections

Memory leak fixes details:

- **SmsReceiver**: Removed static MessageQueueProcessor, MessageStatsDbHelper, MessageHistoryDbHelper, and RateLimiter instances
- **MainActivity**: Added onDestroy() method with proper cleanup of NetworkStatusManager and database helpers
- **Test Methods**: Added cleanup of RetryableForwarder instances after test message sending
- **Database Operations**: Added try-with-resources pattern for database helpers in UI methods
- **Application Class**: Added onTerminate() method for global resource cleanup

### Version 1.11.0 - Multi-language Support & About Page

- [x] **Turkish Localization** - Complete Turkish translation of all UI elements
- [x] **English Localization** - Refined English strings with proper formatting
- [x] **Language Manager** - Runtime language switching functionality
- [x] **Message Format Localization** - SMS, Telegram, and Email message formats in both languages
- [x] **Preference Integration** - Language selection in app settings
- [x] **Application Class** - Proper language initialization at app startup
- [x] **Context-aware Forwarders** - All forwarder classes support localized messages
- [x] **About Page** - Comprehensive app information dialog with version, features, and build details

Implementation details:

- **Complete i18n support**: All user-facing strings moved to resources
- **Runtime language switching**: Users can change language without reinstalling
- **Message localization**: SMS/Email/Telegram messages respect selected language
- **Backward compatibility**: All existing functionality preserved
- **System integration**: Respects system language as default option

Language support:

- **Turkish (TR)**: Complete translation with cultural adaptations
- **English (EN)**: Default language with improved messaging
- **System default**: Follows device language settings

### Version 1.10.0 - Message History System

- [x] **MessageHistoryDbHelper Class** - SQLite-based message history storage
- [x] **Last 100 Messages** - Automatic storage and retrieval of forwarded messages
- [x] **Success/Failure Tracking** - Complete status and error message logging
- [x] **Platform Identification** - Track which platform each message was sent via
- [x] **User Interface Integration** - View history and clear history options in settings
- [x] **Automatic Cleanup** - Maintains 100 record limit with oldest-first deletion
- [x] **Rich Statistics** - Success rates, time spans, platform distribution
- [x] **Detailed Display** - Emoji status indicators, formatted timestamps

Implementation details:

- **SQLite storage**: Persistent message history with indexed queries
- **Automatic logging**: All RetryableForwarder operations recorded
- **Smart UI**: Separate views for recent (20) and complete (100) history
- **Memory efficient**: Message content truncated to 500 characters
- **Thread safe**: Database operations with proper transaction handling

Integration points:

- **RetryableForwarder**: Records success/failure for all forwarding attempts
- **SmsReceiver**: Initializes history helper and passes to all forwarders
- **MainActivity**: History viewing, statistics display, and clear functionality

### Version 1.8.0 - Rate Limiting System

- [x] **RateLimiter Class** - Singleton pattern with sliding window algorithm
- [x] **SMS Forward Rate Control** - Maximum 10 SMS per minute
- [x] **User Interface Integration** - Rate limit status display in settings
- [x] **Preference Control** - Enable/disable toggle for rate limiting
- [x] **Queue Integration** - Rate-limited messages queued for later processing
- [x] **Real-time Monitoring** - Live usage tracking and status updates
- [x] **Thread Safety** - Synchronized access for concurrent operations

Implementation details:

- **Sliding window**: 60-second rolling time window
- **Graceful handling**: Excess messages queued instead of dropped
- **User control**: Can be completely disabled via preferences
- **Integration points**: SmsReceiver, MessageQueueProcessor, MainActivity

---

**Priority Review**: Weekly basis
