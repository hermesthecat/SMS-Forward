# Source Code for SMS Forwarder (`com.keremgok.smsforward`)

This directory contains the core Java source code for the SMS Forwarder application. The application is designed to automatically forward incoming SMS messages to various other platforms like another phone number, Telegram, a generic web hook, or an email address.

## Architecture Overview

The application follows a modular and resilient architecture. Here's a breakdown of the key components:

1. **UI and Configuration (`MainActivity.java`)**: The main screen of the app, allowing users to configure forwarding rules, view stats, and check the message history. It uses Android's `PreferenceFragmentCompat` for the settings UI.
2. **SMS Reception (`SmsReceiver.java`)**: A `BroadcastReceiver` that listens for incoming SMS messages. When a message is received, it triggers the forwarding process.
3. **Forwarding Logic (`Forwarder` interface and implementations)**:
    * `Forwarder.java`: A simple interface defining the contract for all forwarders.
    * `SmsForwarder.java`: Forwards messages as an SMS to another number using `SmsManager`.
    * `AbstractWebForwarder.java`: An abstract base class for forwarders that use HTTP requests.
    * `TelegramForwarder.java`: Extends `AbstractWebForwarder` to send messages to the Telegram Bot API.
    * `JsonWebForwarder.java`: Extends `AbstractWebForwarder` to send messages as a JSON payload to a user-defined webhook.
    * `EmailForwarder.java`: Uses the `jakarta.mail` library to send messages as emails via an SMTP server.
4. **Resilience and Retries**:
    * `RetryableForwarder.java`: A decorator that wraps any `Forwarder`. It adds an in-memory retry mechanism with exponential backoff for transient failures.
    * `MessageQueueDbHelper.java`: A SQLite database that stores messages that have failed all initial retry attempts.
    * `MessageQueueProcessor.java`: A background service that periodically attempts to re-send the messages stored in the message queue database when network connectivity is restored.
5. **Data Persistence and Analytics**:
    * `MessageHistoryDbHelper.java`: A SQLite database that keeps a log of the last 100 forwarding attempts (both successful and failed) for user visibility.
    * `MessageStatsDbHelper.java`: A SQLite database for analytics, tracking daily and total counts of forwarded messages, successes, and failures for each platform.
6. **Utility and Manager Classes**:
    * `NetworkStatusManager.java`: Monitors the device's network state to decide if forwarding is possible.
    * `RateLimiter.java`: Prevents spam by limiting the number of messages that can be forwarded in a given time window.
    * `LanguageManager.java`: Manages the application's display language.
    * `ThemeManager.java`: Manages the application's theme (Light/Dark/System).
    * `SettingsBackupManager.java`: Handles exporting and importing of the application's settings to a file.
    * `SmsForwardApplication.java`: The main `Application` class, used for application-level initialization.
