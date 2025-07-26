# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Development
```bash
# Build debug APK using gradle wrapper
gradlew.bat assembleDebug

# Build release APK
gradlew.bat assembleRelease

# Clean build
gradlew.bat clean assembleDebug

# Run using convenient batch files
build.bat                    # Build debug APK
clean-build.bat             # Clean then build debug
create-signed-apk.bat       # Create signed release APK (requires keystore setup)
```

### APK Output Locations
- Debug: `app\build\outputs\apk\debug\app-debug.apk`
- Release: `app\build\outputs\apk\release\app-release.apk`

### Signing Setup for Release
1. Create keystore: `keytool -genkey -v -keystore app\keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward`
2. Update `gradle.properties` with keystore passwords (file is gitignored)
3. Use `create-signed-apk.bat` for automated signed builds

## Architecture Overview

### Core Components

**SmsReceiver.java** - Central SMS processing hub
- BroadcastReceiver that handles incoming SMS_RECEIVED intents
- Parses multi-part SMS messages and forwards to appropriate platforms
- Implements memory leak prevention by avoiding static contexts
- Coordinates with rate limiting, content filtering, and retry mechanisms

**Forwarder Interface Pattern**
- `Forwarder.java` - Abstract interface for all forwarding implementations
- `RetryableForwarder.java` - Decorator pattern wrapper adding exponential backoff retry logic
- Platform implementations: `SmsForwarder`, `TelegramForwarder`, `EmailForwarder`, `JsonWebForwarder`

**Resilience & Reliability Architecture**
- `MessageQueueDbHelper.java` + `MessageQueueProcessor.java` - SQLite-based offline queue with background processing
- `RateLimiter.java` - Singleton implementing sliding window rate limiting (10 SMS/minute)
- `RetryableForwarder.java` - Automatic retry with exponential backoff (3 attempts)

**Security Framework**
- `SecurityManager.java` - PIN and biometric authentication coordinator
- `AuthenticationActivity.java` - Dedicated security screen with fallback systems
- AndroidKeystore integration for secure key storage

**Data Management**
- `MessageHistoryDbHelper.java` - Last 100 forwarded messages with full metadata
- `MessageStatsDbHelper.java` - Daily/total statistics and success rates
- `SettingsBackupManager.java` - JSON export/import for configuration migration

### Key Design Patterns

1. **Decorator Pattern**: `RetryableForwarder` wraps any `Forwarder` to add retry logic
2. **Strategy Pattern**: Multiple `Forwarder` implementations for different platforms
3. **Singleton Pattern**: `RateLimiter` for global rate limiting coordination
4. **Observer Pattern**: Preferences-based configuration with real-time updates

### Memory Management
- No static context references to prevent memory leaks
- Database helpers instantiated per operation, not cached
- Proper cleanup in background threads and executors
- Resource management in network operations

### Multi-Language Support
- `LanguageManager.java` - Runtime language switching (Turkish/English)
- `SmsForwardApplication.java` - Application-level language initialization
- Localized message formatting per platform

## Development Notes

### Dependencies
- AndroidX AppCompat, Material Design, Preferences
- AndroidX Biometric for fingerprint/face authentication
- Jakarta Mail (Eclipse Angus) for SMTP email functionality
- Minimum SDK: API 25 (Android 7.0), Target SDK: API 34 (Android 14)

### Code Conventions
- Java 8+ features with stream API usage
- SQLite for local data persistence with proper transaction management
- SharedPreferences for user settings with AndroidX Preference framework
- Background processing using Executors, not deprecated AsyncTask

### Testing Strategy
- Built-in test message functionality in settings
- Real-time connection status monitoring
- Message history with success/failure tracking
- Rate limit status monitoring
- Platform-specific connection testing

### Security Considerations
- PIN authentication with salted SHA-256 hashing
- Biometric authentication with AndroidKeystore
- No sensitive data in logs or exported settings
- Content filtering to prevent spam/malicious message forwarding