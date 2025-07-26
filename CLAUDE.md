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

### UI Architecture (Multi-Screen Design)

**MainActivity.java** - Bottom navigation container with fragment management
- Material Design 3 BottomNavigationView with 5 main sections
- Fragment lifecycle management and navigation coordination
- Authentication flow integration (SecurityManager handoff)
- Memory-efficient fragment switching without recreation

**Fragment Architecture Pattern**
- `BasePreferenceFragment.java` - Common base class for all preference fragments
- `DashboardFragment.java` - Real-time status overview with quick actions (non-preference)
- `PlatformsFragment.java` - SMS/Telegram/Email/Web platform configuration
- `SecurityFragment.java` - Authentication, rate limiting, and content filtering
- `MonitorFragment.java` - Statistics, message history, and connection testing
- `AboutFragment.java` - Appearance, backup/restore, and app information

**Navigation Flow**
```
MainActivity (Bottom Navigation)
├─ 🏠 Dashboard - Status overview, quick actions, platform status
├─ ⚙️ Platforms - SMS/Telegram/Email/Web configurations  
├─ 🛡️ Security - Authentication, filters, rate limiting
├─ 📊 Monitor - Statistics, message history, test tools
└─ ℹ️ About - App info, appearance, backup/restore
```

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

1. **Fragment Pattern**: Modular UI components with shared base class and common functionality
2. **Decorator Pattern**: `RetryableForwarder` wraps any `Forwarder` to add retry logic
3. **Strategy Pattern**: Multiple `Forwarder` implementations for different platforms
4. **Singleton Pattern**: `RateLimiter` for global rate limiting coordination
5. **Observer Pattern**: Preferences-based configuration with real-time updates
6. **Template Method Pattern**: `BasePreferenceFragment` defines common lifecycle methods

### Memory Management
- No static context references to prevent memory leaks
- Database helpers instantiated per operation with explicit close() calls
- Fragment lifecycle cleanup in onDestroy() with resource release
- Proper cleanup in background threads and executors
- Resource management in network operations
- SharedPreferences listener registration/unregistration in fragment lifecycle

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

## UI Architecture Notes

### Multi-Screen Implementation (v1.15.0+)
The app was successfully refactored from a single-screen preferences activity to a modern multi-screen design:

**Before (v1.14.x):**
- Single `MainActivity` with embedded `SettingsFragment`
- All preferences in one scrollable `root_preferences.xml`
- Limited navigation and poor organization

**After (v1.15.0+):**
- Bottom navigation with 5 dedicated fragments
- Organized preference categories with focused functionality
- Real-time dashboard with status overview
- Improved user experience and maintainability

### Fragment Responsibilities

**DashboardFragment.java:**
- Real-time status monitoring (connection, rate limits, message counts)
- Platform enable/disable status overview
- Quick action buttons (test message, view history, platform settings)
- Navigation bridge to other fragments

**PlatformsFragment.java:**
- SMS forwarding configuration (target number)
- Telegram setup (API key, chat ID)
- Email SMTP configuration (host, port, credentials, TLS)
- Web webhook setup (target URL, JSON format)

**SecurityFragment.java:**
- Authentication setup (PIN creation, biometric enable)
- Rate limiting configuration and status
- Content filtering (keyword blacklist)
- Sender filtering (number whitelist)

**MonitorFragment.java:**
- Message statistics (daily/total counts, success rates)
- Message history viewer (last 100 forwarded messages)
- Connection testing for all platforms
- System information (permissions, queue status, logs)

**AboutFragment.java:**
- Language switching (Turkish/English)
- Theme selection (Light/Dark/System)
- Settings backup/restore (JSON export/import)
- App version and information