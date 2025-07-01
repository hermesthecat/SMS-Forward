# SMS Forward - Development Roadmap

## 📋 Project Overview

SMS Forward is a minimal, efficient Android application for forwarding SMS messages across multiple platforms. This document outlines future development suggestions and improvements.

**Current Version**: 1.14.0
**Package Name**: `com.keremgok.smsforward`
**Target**: Production-ready SMS forwarding solution

---

## 🚀 Development Roadmap

This roadmap outlines the planned features and improvements for upcoming versions.

### **v1.15.0: Enhanced Security & Reliability**

- **Number Whitelist**: Only forward SMS from specific, user-defined numbers.
- **Secure Storage**: Encrypt sensitive settings like API keys and passwords.
- **Input Validation**: Stricter validation for all user-configurable settings.
- **Better Error Messages**: Provide user-friendly and actionable error descriptions.
- **Last Status Display**: Show the result of the last forwarding attempt in the main UI.

### **v1.16.0: Platform Expansion & Customization**

- **Discord Integration**: Forward SMS via Discord webhooks.
- **Slack Integration**: Forward SMS using the Slack Bot API.
- **Microsoft Teams Integration**: Support for forwarding to MS Teams channels.
- **Custom Message Templates**: Allow users to define their own message format using variables (e.g., `{from}`, `{content}`, `{timestamp}`).

### **v1.17.0: Advanced Features & User Experience**

- **Quiet Hours**: A "do not disturb" mode to disable forwarding during specific times.
- **Smart Notifications**: Group multiple forwarding notifications to reduce notification spam.
- **Keyword-based Routing**: Send messages to different platforms based on keywords in the SMS content.
- **Initial Test Coverage**: Begin adding Unit and Integration tests for critical components.
- **Code Documentation**: Add JavaDoc comments to public APIs.

### **v2.0.0: Architecture Overhaul**

- **Modern Android Architecture**:
  - Migrate to a modern architecture (MVVM, Repository Pattern).
  - Implement Dependency Injection (Hilt or Dagger).
  - Refactor async operations using Kotlin Coroutines.
- **Database Migration**: Consolidate data access logic, potentially moving to Room.
- **Performance Optimization**: Profile and significantly reduce memory/battery usage.
- **Comprehensive Testing**: Aim for high test coverage across the application.

### **Future & Innovation Ideas (Post-v2.0.0)**

- **Cloud Sync**: Sync settings across multiple devices.
- **Centralized Management**: An admin dashboard for enterprise use cases.
- **Advanced Security**: Audit logs, geofencing, and other enterprise-grade security features.
- **API Gateway**: A REST API for remote management and integration.

---

This roadmap is a living document and is subject to change.
For a detailed history of changes, see the [CHANGELOG.md](CHANGELOG.md) file.
