# SMS Forward

Sometimes we have more phone numbers than SIM card slots in the phone we use most. You do not want to miss any call and text message so you carry multiple phones everyday, what a pain!

Calls can be forwarded to a single phone thanks to carriers' call forwarding service. What about SMS messages? This app can handle them. It forwards them between an Android phone and a target phone.

## Features

✅ **Multiple forwarding methods:**

- SMS forwarding to another phone number
- Telegram Bot API integration
- Email forwarding via SMTP
- HTTP webhook integration (JSON POST)

✅ **Bidirectional messaging:**

- Forward incoming SMS to target destinations
- Send SMS remotely through the Android device

✅ **Enhanced message information:**

- Includes original SMS timestamp
- Shows exact received date/time
- Formatted as "Received at: dd/MM/yyyy HH:mm:ss"

✅ **Reliability and resilience:**

- Automatic retry mechanism (3 attempts)
- Exponential backoff between retries
- Offline message queue with SQLite storage
- Automatic reprocessing when connectivity restored
- Rate limiting to prevent spam (10 SMS/minute)
- Detailed logging for troubleshooting

✅ **Testing and debugging:**

- Built-in test message feature
- Real-time connection status indicator
- Daily and total message counter with success rates
- Rate limiting status monitor with real-time usage
- Verify forwarding setup instantly
- Debug each platform individually

✅ **Modern user interface:**

- Material Design 3 theming
- Dark mode support with system theme following
- Multi-language support (Turkish and English)
- Runtime language switching
- Automatic theme switching based on system settings
- Elegant light and dark color schemes

✅ **Backup and configuration management:**

- Export/Import settings in JSON format
- Complete configuration backup with metadata
- Device migration and settings sharing
- Version compatibility protection
- Automatic preference refresh after import

✅ **Message history and tracking:**

- Complete forwarding history (last 100 messages)
- Success/failure status tracking
- Rich metadata (timestamps, platforms, error messages)
- Memory-optimized display with automatic cleanup
- Integrated statistics dashboard

✅ **App information and about:**

- Comprehensive about page with app details
- Version information and build details
- Developer information and license details
- Feature list and technical specifications
- Package information and system requirements

✅ **Security and spam prevention:**

- Rate limiting prevents spam (10 SMS/minute maximum)
- Sliding window algorithm for precise control
- User-configurable enable/disable toggle
- Rate-limited messages queued for later processing

✅ **Memory management and performance:**

- Memory leak prevention and proper resource cleanup
- Static context reference elimination
- Database connection management
- Background thread lifecycle management
- Optimized long-term stability

✅ **Minimal and efficient:**

- Small APK size (~1.9MB)
- Low battery consumption
- No unnecessary permissions
- Enterprise-grade memory efficiency

## Supported Platforms

- **SMS**: Direct SMS forwarding to phone numbers
- **Telegram**: Send messages via Telegram Bot API
- **Email**: SMTP email forwarding (supports TLS/SSL)
- **Web API**: HTTP POST requests with JSON payload

## Supported Languages

- **🇺🇸 English**: Default language with comprehensive messaging
- **🇹🇷 Turkish**: Complete localization with cultural adaptations
- **🌐 System Default**: Automatically follows device language settings

### Language Features

- **Runtime switching**: Change language without reinstalling
- **Message localization**: SMS, Telegram, and Email content in selected language
- **UI localization**: Complete interface translation
- **Cultural adaptations**: Proper formatting and expressions for each language

## Build Instructions

### Prerequisites

- Android SDK (API Level 25+)
- Java 8+ (tested with Java 21)
- Gradle (included via wrapper)

### Quick Build

Use the provided batch files for easy building:

```batch
# Debug APK
build.bat

# Release APK
build-release.bat

# Clean and build
clean-build.bat
```

### Manual Build

```batch
# Set Android SDK paths (if not set system-wide)
set ANDROID_SDK_ROOT=C:\Users\[username]\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\[username]\AppData\Local\Android\Sdk

# Build debug APK
gradlew.bat assembleDebug

# Build release APK
gradlew.bat assembleRelease
```

### Output Location

- Debug APK: `app\build\outputs\apk\debug\app-debug.apk`
- Release APK: `app\build\outputs\apk\release\app-release.apk`

## Installation & Setup

1. **Install the APK:**

   - Download from releases or build from source
   - Install: `adb install app-debug.apk` or manually transfer to device

2. **Grant Permissions:**

   - SMS receive and send permissions
   - Internet permission (for web forwarding)

3. **Configure Appearance (Optional):**

   - Go to "Appearance" section in settings
   - Choose language: English, Turkish, or System default
   - Choose theme: Light, Dark, or System default
   - System default automatically follows device theme/language settings

4. **Configure Forwarding Methods:**

   - **SMS**: Enter target phone number (e.g., +1234567890)
   - **Telegram**: Set Bot Token and Chat ID
   - **Email**: Configure SMTP server settings
   - **Web**: Set webhook URL endpoint

5. **Backup & Restore (Optional):**

   - Go to "Backup & Restore" section in settings
   - **Export Settings**: Backup your configuration to a JSON file
   - **Import Settings**: Restore configuration from a backup file
   - Use for device migration or sharing settings between devices
   - Exported files include all platform settings, theme preferences, and rate limiting config

6. **Test Your Setup:**

   - Go to "Test & Debug" section in settings
   - Check "Connection Status" to view real-time network connectivity
   - Tap "Send Test Message" to verify configuration
   - Check "Message Queue Status" to view offline queue statistics
   - Check "Message Counter" to view daily/total forwarding statistics
   - Check "Rate Limit Status" to view current usage and spam protection
   - Check "View Message History" to see recent forwarding activity
   - Check if test message arrives on your target platforms

7. **View App Information:**

   - Go to "About" section in settings
   - View app version, developer information, and license details
   - See comprehensive feature list and technical specifications
   - Check package information and system requirements

8. **Usage:**
   - Keep the Android phone charged and connected
   - Incoming SMS will be forwarded automatically
   - Send reverse SMS with format: `To [number]:\n[message]`

## Usage Examples

### Automatic Retry Mechanism

All forwarding attempts automatically retry on failure:

```bash
# First attempt fails -> Retry in 1 second
# Second attempt fails -> Retry in 2 seconds
# Third attempt fails -> Give up and log error
```

Retry timing follows exponential backoff:

- Attempt 1: Immediate
- Attempt 2: 1 second delay
- Attempt 3: 2 second delay
- After 3 failures: Message stored in offline queue

### Offline Message Queue

Failed messages are automatically stored and retried:

```bash
# Message fails after 3 retry attempts -> Stored in SQLite database
# Queue processor runs every 30 seconds checking connectivity
# When online -> Messages reprocessed automatically
# Up to 5 additional queue retry attempts per message
```

Queue processing features:

- Persistent SQLite storage survives app restarts
- Automatic connectivity detection
- Background processing every 30 seconds
- Statistics available in "Test & Debug" section
- Automatic cleanup of old successful messages (24h)

### Backup & Restore Settings

Complete configuration management for easy device migration:

#### Export Settings Example

```json
{
  "_backup_version": 1,
  "_export_timestamp": 1704045612345,
  "_app_version": "1.9.0",
  "key_enable_sms": true,
  "key_sms_target": "+1234567890",
  "key_enable_telegram": true,
  "key_target_telegram": "123456789",
  "key_telegram_apikey": "1234567890:ABCDEfghijk...",
  "key_enable_web": false,
  "key_target_web": "",
  "key_enable_email": false,
  "key_email_from_address": "",
  "key_email_to_address": "",
  "key_email_submit_host": "",
  "key_email_submit_port": "587",
  "key_email_submit_password": "",
  "key_email_username_style": "full",
  "key_enable_rate_limiting": true,
  "theme_mode": "system"
}
```

#### Export Process

```bash
# User initiates export
Backup & Restore → Export Settings
↓
File picker opens with suggested name
"sms_forward_backup_20241226_143052.json"
↓
User selects save location
↓
All configuration exported with metadata
✅ Settings exported successfully!
```

#### Import Process

```bash
# User initiates import
Backup & Restore → Import Settings
↓
File picker opens for JSON file selection
↓
File validation and version checking
↓
Compatible backup found → Import 15 settings
✅ Successfully imported 15 settings from backup created on 26/12/2024 14:30:52
↓
UI refreshed with new settings
Theme applied if changed
```

#### Use Cases

**Device Migration:**

```bash
Old Phone: Export → sms_forward_backup_20241226.json
New Phone: Import → Instant setup with same configuration
```

**Settings Sharing:**

```bash
Team Lead: Configure → Export → Share backup file
Team Members: Import → Identical setup across team
```

**Backup Before Changes:**

```bash
Before experimenting: Export current settings
After testing: Import to restore original configuration
```

#### Security Features

- **Version Validation**: Prevents importing incompatible backups
- **Key Whitelisting**: Only known settings are imported
- **Metadata Tracking**: Shows backup creation date and app version
- **Error Handling**: Detailed feedback for failed imports

### Message History & Tracking

Complete forwarding history system with persistent storage:

```bash
# Recent Message History (Last 20)
📨 Recent Messages:
  15:34:21 ✅ +1234567890 → Telegram (120 chars)
  15:33:45 ❌ +5555555555 → Email (Failed: Invalid SMTP)
  15:32:10 ✅ +9876543210 → SMS (85 chars)
  15:30:55 ✅ +1111111111 → Web API (200 chars)

# Complete Message History (Last 100)
📊 Message Statistics:
  Total Records: 87
  Success Rate: 94.3% (82/87)
  Failed Messages: 5

  Platform Distribution:
  📱 SMS: 45 messages (91.1% success)
  📢 Telegram: 28 messages (96.4% success)
  📧 Email: 10 messages (90.0% success)
  🌐 Web API: 4 messages (100% success)

  Time Span: Last 7 days
  Average: 12.4 messages/day
```

#### Message History Features

- **Persistent SQLite Storage**: Messages saved between app restarts
- **100 Message Limit**: Automatic cleanup of oldest records
- **Rich Metadata**: Status, platform, timestamps, error details
- **Smart Views**: Recent (20) vs complete (100) display modes
- **Memory Optimized**: Message content truncated to 500 characters
- **Success/Failure Tracking**: Visual indicators with status emojis
- **Error Logging**: Detailed failure reasons for troubleshooting

#### Message History Examples

**Successful Forward:**

```bash
✅ 15:34:21 | +1234567890 → Telegram
"Hello from customer support team..."
Platform: Telegram Bot API
Status: Success (HTTP 200)
```

**Failed Forward:**

```bash
❌ 15:33:45 | +5555555555 → Email
"Urgent: Server maintenance tonight..."
Platform: SMTP Email
Status: Failed - Authentication failed (535)
Error: Invalid username or password
```

#### Clearing Message History

- **Manual Clear**: "Clear Message History" preference
- **Confirmation Dialog**: Prevents accidental deletion
- **Immediate Effect**: History cleared instantly
- **Statistics Reset**: Success rates recalculated
- **Fresh Start**: Useful for testing or privacy

#### History Integration

- **Automatic Logging**: All forwards logged automatically
- **Real-time Updates**: New messages appear instantly
- **Thread-Safe Operations**: Concurrent access protection
- **Performance Optimized**: Fast database queries with indexing
- **Background Processing**: No UI blocking during logging

### Connection Status Monitoring

Real-time network connectivity tracking:

```bash
# Connection Status Examples
🟢 Online (WiFi)     # WiFi connection - good quality
🟡 Online (Mobile)   # Mobile data - moderate quality
🔵 Online (Ethernet) # Ethernet connection - good quality
🔴 Offline           # No internet connection
```

Status indicators:

- Real-time connectivity monitoring
- Connection type detection (WiFi/Mobile/Ethernet)
- Network quality assessment
- Forwarding capability status
- Automatic UI updates on network changes

### Message Counter Statistics

Track daily and total forwarding performance:

```bash
# Daily Statistics Example
📊 Today's Messages:
  Total: 15
  Success: 14 (93.3%)
  Failed: 1
  📱 SMS: 8
  📢 Telegram: 4
  📧 Email: 2
  🌐 Web API: 1

# All Time Statistics
📈 All Time:
  Total: 1,247
  Success: 1,201 (96.3%)
  Failed: 46
  Active Days: 28
  Daily Avg: 44.5

  Platform Breakdown:
  📱 SMS: 890
  📢 Telegram: 245
  📧 Email: 78
  🌐 Web API: 34
```

Features:

- Daily and total message counts
- Success rate percentages
- Platform-specific breakdowns
- Active days tracking
- Daily average calculations
- Automatic data retention (90 days)
- Real-time updates during forwarding

### Rate Limiting and Spam Prevention

Automatic protection against SMS forwarding abuse:

```bash
# Rate Limiting Status Examples
🚦 Current usage: 3/10 SMS per minute
✅ Slots available immediately

🚦 Current usage: 8/10 SMS per minute
⚠️ Approaching rate limit. Be careful not to exceed 10 SMS per minute.

🚦 Current usage: 10/10 SMS per minute
⚠️ Rate limit reached! SMS forwarding temporarily blocked.
Next slot available in: 45 seconds
```

Protection features:

- **Sliding window algorithm**: Tracks last 60 seconds of activity
- **10 SMS per minute limit**: Prevents spam and abuse
- **Graceful handling**: Rate-limited messages queued for later
- **User control**: Can be disabled in settings if not needed
- **Real-time monitoring**: View current usage anytime
- **Thread-safe**: Works reliably with multiple simultaneous messages

Rate limiting behavior:

```bash
# Normal operation
SMS 1-10: ✅ Forwarded immediately
SMS 11+:  ⏳ Queued until rate limit window resets

# After 60 seconds
Oldest SMS timestamp expires → New slot available
Queued messages processed automatically
```

### Dark Mode and Theme Support

Automatic theme switching with modern Material Design 3:

```bash
# Theme Options
🌞 Light Mode     # Always light theme
🌙 Dark Mode      # Always dark theme
🔄 System Default # Follows device setting

# Automatic Features
✓ Instant theme switching
✓ Material Design 3 colors
✓ System theme detection
✓ Battery saver fallback (Android 9-)
```

Theme behavior:

- **Android 10+**: Full system theme detection
- **Android 9 and below**: Uses battery saver mode as fallback
- **Instant switching**: No app restart required
- **Persistent setting**: Remembers user preference
- **Elegant colors**: Professional light and dark color schemes

### Incoming Message Forwarding

When SMS is received on Android phone:

```bash
Original: "Hello from John"
Forwarded: "From +1234567890:
Hello from John
Received at: 26/06/2025 20:29:15"
```

### Reverse Message Sending

Send to Android phone:

```bash
"To +9876543210:\nMeeting at 3pm"
```

Android phone will send "Meeting at 3pm" to +9876543210

### Platform-Specific Formats

Messages are automatically localized based on selected language:

#### SMS Forwarding

**English:**

```cmd
From +1234567890:
Hello from John
Received at: 26/06/2025 20:29:15
```

**Turkish:**

```cmd
+1234567890'den:
Hello from John
Alındığı zaman: 26/06/2025 20:29:15
```

#### Telegram Forwarding

**English:**

```cmd
Message from +1234567890:
Hello from John
Received at: 26/06/2025 20:29:15
```

**Turkish:**

```cmd
+1234567890'den mesaj:
Hello from John
Alındığı zaman: 26/06/2025 20:29:15
```

#### Email Forwarding

**English:**

```cmd
Subject: SMS from: +1234567890
Body:
Hello from John

Received at: 26/06/2025 20:29:15
```

**Turkish:**

```cmd
Subject: SMS: +1234567890
Body:
Hello from John

Alındığı zaman: 26/06/2025 20:29:15
```

#### Web API Format

HTTP POST to configured webhook:

```json
{
  "from": "+1234567890",
  "message": "Hello from John",
  "received_at": "26/06/2025 20:29:15",
  "timestamp": 1735224555000
}
```

## Technical Details

- **Package Name**: `com.keremgok.smsforward`
- **Minimum Android**: API Level 25 (Android 7.0)
- **Target Android**: API Level 34 (Android 14)
- **App Version**: 1.12.0
- **Languages**: Turkish, English (with system default)
- **Architecture**: Java with Android Gradle Plugin 8.7.3

## Project Structure

```bash
app/src/main/java/com/keremgok/smsforward/
├── SmsForwardApplication.java # Application class for language initialization
├── MainActivity.java          # Settings UI with stats/queue/connection/rate limit status
├── SmsReceiver.java           # SMS broadcast receiver with memory leak fixes
├── LanguageManager.java       # Multi-language support and runtime switching
├── RateLimiter.java           # Rate limiting and spam prevention system
├── SettingsBackupManager.java # Export/Import settings in JSON format
├── MessageHistoryDbHelper.java # SQLite database for forwarding history
├── Forwarder.java             # Interface for all forwarders
├── RetryableForwarder.java    # Retry mechanism wrapper with proper cleanup
├── MessageQueueDbHelper.java  # SQLite database for offline queue
├── MessageQueueProcessor.java # Background queue processing service
├── MessageStatsDbHelper.java  # SQLite database for message statistics
├── NetworkStatusManager.java  # Real-time network connectivity monitor
├── ThemeManager.java          # Dark mode and theme switching manager
├── SmsForwarder.java          # SMS forwarding implementation
├── TelegramForwarder.java     # Telegram Bot API integration
├── EmailForwarder.java        # SMTP email forwarding
├── JsonWebForwarder.java      # HTTP webhook integration
└── AbstractWebForwarder.java  # Base class for web APIs
```

## Changelog

### Version 1.12.0 - Memory Leak Fixes & Performance (Latest)

🔧 **Memory Management:**

- ✅ **Static Context References**: Eliminated memory leaks in SmsReceiver by removing static field dependencies
- ✅ **Resource Cleanup**: Added proper lifecycle management with onDestroy() cleanup in MainActivity
- ✅ **Database Management**: Implemented proper close() calls for all database helpers
- ✅ **Background Thread Cleanup**: Added shutdown() methods for ExecutorService instances
- ✅ **Application Cleanup**: Global resource cleanup in Application.onTerminate()

🚀 **Performance Improvements:**

- ✅ **Memory Efficiency**: Reduced memory footprint and eliminated memory leaks
- ✅ **Long-term Stability**: Optimized for continuous operation without memory degradation
- ✅ **Resource Management**: Proper cleanup of all background threads and connections
- ✅ **Enterprise-grade**: Production-ready memory management standards

🛠️ **Technical Enhancements:**

- ✅ **Code Quality**: Improved code formatting and consistency
- ✅ **Error Prevention**: Proactive memory leak prevention measures
- ✅ **Lifecycle Management**: Proper Android component lifecycle handling
- ✅ **Thread Safety**: Enhanced concurrent access protection

### Version 1.11.0 - Multi-language Support & About Page

🌐 **Localization:**

- ✅ **Turkish Translation**: Complete Turkish localization with cultural adaptations
- ✅ **Runtime Language Switching**: Change language without app restart
- ✅ **Message Localization**: SMS, Telegram, Email formats in both languages
- ✅ **System Integration**: Respects device language settings

📱 **User Interface:**

- ✅ **About Page**: Comprehensive app information with version details
- ✅ **Language Manager**: Centralized language management system
- ✅ **Context-aware Forwarders**: All message types support localization

### Version 1.10.0 - Message History System

📊 **History Tracking:**

- ✅ **SQLite Storage**: Persistent message history (last 100 messages)
- ✅ **Rich Metadata**: Status, platform, timestamps, error messages
- ✅ **Smart UI**: Recent (20) vs complete (100) view modes
- ✅ **Statistics Integration**: Success rates and platform distribution

### Version 1.9.0 - Settings Backup & Restore

💾 **Configuration Management:**

- ✅ **Export/Import**: JSON format with metadata and version compatibility
- ✅ **Device Migration**: Complete configuration transfer between devices
- ✅ **File Operations**: Modern Android Storage Access Framework
- ✅ **Security Validation**: Whitelist-based preference filtering

### Version 1.8.0 - Rate Limiting System

🚦 **Spam Prevention:**

- ✅ **Rate Limiting**: 10 SMS per minute with sliding window algorithm
- ✅ **Queue Integration**: Rate-limited messages queued for later
- ✅ **Real-time Monitoring**: Live usage tracking and status display
- ✅ **User Control**: Enable/disable toggle in preferences

### Version 1.7.0 - Dark Mode & Theming

🎨 **Visual Enhancements:**

- ✅ **Dark Mode**: System theme following with Material Design 3
- ✅ **Theme Manager**: Automatic switching and persistence
- ✅ **Modern UI**: Elegant light and dark color schemes

### Previous Versions

- **v1.6.0**: Statistics dashboard with daily/total counters and success rates
- **v1.5.0**: Real-time connection status monitoring and network quality indicators
- **v1.4.0**: Offline message queue with SQLite storage and background processing
- **v1.3.0**: Automatic retry mechanism with exponential backoff
- **v1.2.0**: Test message functionality for setup verification
- **v1.1.0**: Enhanced message formatting with timestamps
- **v1.0.0**: Initial release with basic SMS, Telegram, Email, and Web forwarding

## Development

### Dependencies

- AndroidX AppCompat & Material Design
- AndroidX Preferences

### Build System

- Gradle 8.9
- Android Gradle Plugin 8.7.3
- Java toolchain support

## Compatibility

- **Tested on**: Android 7.0+ devices
- **Should work on**: Android 7.0 (API 25) and newer
- **Telephony feature**: Required (will not work on tablets without cellular)

## Notes

- This is a minimal implementation focused on core functionality
- Alternative commercial apps exist but are typically much larger
- Keep the Android device charged for continuous operation
- SMS forwarding works immediately; reverse messaging requires target number setup
- Memory-optimized for long-term continuous operation

## License

MIT License - Feel free to modify and distribute

## Contributing

This project welcomes contributions. Please ensure all changes maintain the minimal and efficient nature of the application.
