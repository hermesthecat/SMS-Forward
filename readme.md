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

✅ **Minimal and efficient:**

- Small APK size (~6MB)
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

3. **Configure Forwarding Methods:**
   - **SMS**: Enter target phone number (e.g., +1234567890)
   - **Telegram**: Set Bot Token and Chat ID
   - **Email**: Configure SMTP server settings
   - **Web**: Set webhook URL endpoint

4. **Usage:**
   - Keep the Android phone charged and connected
   - Incoming SMS will be forwarded automatically
   - Send reverse SMS with format: `To [number]:\n[message]`

## Usage Examples

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
- **App Version**: 1.1.0
- **Architecture**: Java with Android Gradle Plugin 8.7.3

## Project Structure

```bash
app/src/main/java/com/keremgok/smsforward/
├── MainActivity.java          # Settings UI
├── SmsReceiver.java           # SMS broadcast receiver
├── Forwarder.java             # Interface for all forwarders
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
