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
- Detailed logging for troubleshooting

✅ **Testing and debugging:**

- Built-in test message feature
- Real-time connection status indicator
- Daily and total message counter with success rates
- Verify forwarding setup instantly
- Debug each platform individually

✅ **Modern user interface:**

- Material Design 3 theming
- Dark mode support with system theme following
- Automatic theme switching based on system settings
- Elegant light and dark color schemes

✅ **Minimal and efficient:**

- Small APK size (~1.9MB)
- Low battery consumption
- No unnecessary permissions

## Supported Platforms

- **SMS**: Direct SMS forwarding to phone numbers
- **Telegram**: Send messages via Telegram Bot API
- **Email**: SMTP email forwarding (supports TLS/SSL)
- **Web API**: HTTP POST requests with JSON payload

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
   - Choose theme: Light, Dark, or System default
   - System default automatically follows device theme settings

4. **Configure Forwarding Methods:**
   - **SMS**: Enter target phone number (e.g., +1234567890)
   - **Telegram**: Set Bot Token and Chat ID
   - **Email**: Configure SMTP server settings
   - **Web**: Set webhook URL endpoint

5. **Test Your Setup:**
   - Go to "Test & Debug" section in settings
   - Check "Connection Status" to view real-time network connectivity
   - Tap "Send Test Message" to verify configuration
   - Check "Message Queue Status" to view offline queue statistics
   - Check "Message Counter" to view daily/total forwarding statistics
   - Check if test message arrives on your target platforms

6. **Usage:**
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

#### SMS Forwarding

```cmd
From +1234567890:
Hello from John
Received at: 26/06/2025 20:29:15
```

#### Telegram Forwarding

```cmd
Message from +1234567890:
Hello from John
Received at: 26/06/2025 20:29:15
```

#### Email Forwarding

```cmd
Subject: SMS from: +1234567890
Body:
Hello from John

Received at: 26/06/2025 20:29:15
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
- **App Version**: 1.7.0
- **Architecture**: Java with Android Gradle Plugin 8.7.3

## Project Structure

```bash
app/src/main/java/com/keremgok/smsforward/
├── MainActivity.java          # Settings UI with stats/queue/connection status
├── SmsReceiver.java           # SMS broadcast receiver
├── Forwarder.java             # Interface for all forwarders
├── RetryableForwarder.java    # Retry mechanism wrapper
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

## Development

### Dependencies

- AndroidX AppCompat & Material Design
- AndroidX Preferences
- Jakarta Mail API for email

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

## License

MIT License - Feel free to modify and distribute

## Contributing

This project welcomes contributions. Please ensure all changes maintain the minimal and efficient nature of the application.
