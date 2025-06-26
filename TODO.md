# SMS Forward - TODO List

## 🚨 **High Priority (Next Release)**

### Critical Features

- [x] **Retry Mechanism** - Auto-retry failed forwards (3 attempts) ✅ *Completed in v1.3.0*
- [x] **Test Message Button** - Quick functionality test ✅ *Completed in v1.2.0*
- [x] **Connection Status Indicator** - Show online/offline status ✅ *Completed in v1.5.0*
- [x] **Offline Message Queue** - Store failed messages for retry ✅ *Completed in v1.4.0*
- [ ] **Number Whitelist** - Only forward from specific numbers

### UI Improvements

- [ ] **Message Counter** - Show daily forwarded count
- [ ] **Last Status Display** - Show last forward result
- [ ] **Better Error Messages** - User-friendly error descriptions
- [ ] **Dark Mode Support** - System theme following

## 🔧 **Medium Priority**

### Security

- [ ] **PIN Protection** - Lock app with PIN/biometric
- [ ] **Rate Limiting** - Prevent spam (max 10 SMS/minute)
- [ ] **Secure Storage** - Encrypt sensitive settings
- [ ] **Input Validation** - Validate all user inputs

### Features

- [ ] **Statistics Dashboard** - Daily/weekly stats
- [ ] **Export/Import Settings** - Backup configuration
- [ ] **Message History** - Last 50 forwarded messages
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
- [ ] **Error Handling** - Improve exception handling
- [ ] **Code Documentation** - Add JavaDoc comments
- [ ] **Performance Optimization** - Reduce memory usage

### Architecture

- [ ] **Database Migration** - Add SQLite for data storage
- [ ] **Dependency Injection** - Use modern DI framework
- [ ] **Background Processing** - Use JobScheduler/WorkManager
- [ ] **Memory Leaks** - Fix potential memory issues

## 🚀 **Quick Wins (Easy Implementation)**

1. **Test Message Button** - 1 day
2. **Message Counter** - 1 day  
3. **Connection Status** - 2 days
4. **Dark Mode** - 1 day
5. **Better Error Messages** - 2 days

## 📝 **Implementation Notes**

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

---

**Last Updated**: December 26, 2024  
**Priority Review**: Weekly basis
