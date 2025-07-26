# Changelog

All notable changes to this project will be documented in this file.

### Version 1.15.0 - Sender Whitelist & Reliability

✅ **Number Whitelist**:
- ✅ **Sender Filtering**: Only forward SMS from specific, user-defined numbers.
- ✅ **Whitelist Management**: Enable/disable the whitelist and manage the list of numbers in settings.
- ✅ **UI Integration**: New "Sender Filter" section in preferences for easy configuration.
- ✅ **Backup & Restore**: Whitelist settings are included in the backup/restore functionality.
- ✅ **Multi-language Support**: Full localization for English and Turkish.

### Version 1.14.0 - PIN/Biometric Lock Security (Latest)

🔒 **Application Security:**

- ✅ **PIN Authentication**: 4+ digit PIN protection with secure salted SHA-256 hashing
- ✅ **Biometric Authentication**: Fingerprint and face recognition using Android Biometric API
- ✅ **Android Keystore Integration**: Secure biometric key storage and management
- ✅ **Authentication Timeout**: Configurable timeout from 1 minute to never expire
- ✅ **Fallback System**: Biometric authentication gracefully falls back to PIN if unavailable
- ✅ **Secure Storage**: PIN hashes stored with random salt, no plaintext PIN storage

🛡️ **Security Features:**

- ✅ **AuthenticationActivity**: Dedicated security screen preventing back navigation during auth
- ✅ **MainActivity Integration**: Authentication checks on app startup and when resuming
- ✅ **Security Testing**: Built-in functionality to test authentication methods
- ✅ **Device Compatibility**: Automatic detection of biometric hardware availability
- ✅ **Memory Security**: Proper cleanup of sensitive data and prevention of memory leaks

🎨 **User Interface:**

- ✅ **Security Settings**: Complete "Security & Privacy" section in app preferences
- ✅ **PIN Management**: Create, change, and remove PIN with confirmation dialogs
- ✅ **Biometric Toggle**: Enable/disable biometric authentication with status messages
- ✅ **Timeout Configuration**: Six timeout options from 1 minute to never expire
- ✅ **Real-time Status**: Live security status and availability indicators

🌐 **Multi-language Support:**

- ✅ **English Localization**: 50+ security-related strings with comprehensive messaging
- ✅ **Turkish Localization**: Complete Turkish translation with cultural adaptations
- ✅ **Dynamic Summaries**: Live preference summaries showing current security status
- ✅ **Error Messages**: Localized error handling and user guidance

🔧 **Technical Implementation:**

- ✅ **SecurityManager Class**: Centralized security management with comprehensive API
- ✅ **Thread Safety**: Synchronized operations for concurrent access protection
- ✅ **Permission Handling**: Android Biometric permission integration
- ✅ **Lifecycle Management**: Proper integration with Android Activity lifecycle
- ✅ **Settings Backup**: Security preferences appropriately excluded from backup files

### Version 1.13.0 - SMS Content Filter & Security

🛡️ **Content Filtering:**

- ✅ **Keyword-based Filtering**: Block messages containing specific keywords
- ✅ **Case-insensitive Matching**: SPAM = spam = Spam for flexible filtering
- ✅ **Comma-separated Lists**: Multiple keywords: "spam,advertisement,promotion"
- ✅ **Real-time Summary**: Live preview of active filters in settings
- ✅ **Input Validation**: Automatic keyword cleaning and formatting

🔒 **Security Enhancements:**

- ✅ **Spam Protection**: Proactive message blocking before forwarding
- ✅ **User Control**: Complete control over filtered content
- ✅ **Performance Optimized**: Lightweight string matching with early exit
- ✅ **Integration Point**: Pre-filtering in SmsReceiver before any forwarding

🌐 **Multi-language Support:**

- ✅ **Turkish & English**: Complete localization for filter settings
- ✅ **Settings Backup**: Filter keywords included in backup/restore
- ✅ **Smart UI**: Dynamic summaries showing active vs inactive filters
- ✅ **User-friendly**: Clear instructions and help text

🛠️ **Technical Implementation:**

- ✅ **SmsContentFilter Class**: Static utility class for efficient filtering
- ✅ **Memory Efficient**: No unnecessary object creation during filtering
- ✅ **Thread Safe**: Safe for concurrent access from SmsReceiver
- ✅ **Logging**: Detailed logs for debugging blocked messages

### Version 1.12.0 - Memory Leak Fixes & Performance

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

- **v1.7.0**: Dark mode support and Material Design 3 theming
- **v1.6.0**: Statistics dashboard with daily/total counters and success rates
- **v1.5.0**: Real-time connection status monitoring and network quality indicators
- **v1.4.0**: Offline message queue with SQLite storage and background processing
- **v1.3.0**: Automatic retry mechanism with exponential backoff
- **v1.2.0**: Test message functionality for setup verification
- **v1.1.0**: Enhanced message formatting with timestamps
- **v1.0.0**: Initial release with basic SMS, Telegram, Email, and Web forwarding
