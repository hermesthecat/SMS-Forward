# SMS Forward - Development Roadmap

## 📋 Project Overview

SMS Forward is a minimal, efficient Android application for forwarding SMS messages across multiple platforms. This document outlines future development suggestions and improvements.

**Current Version**: 1.14.0
**Package Name**: `com.keremgok.smsforward`
**Target**: Production-ready SMS forwarding solution

---

## 🚀 Development Roadmap

This roadmap is divided into priorities. Features move from Future Ideas to higher priorities based on user feedback and development capacity.

### 🔥 **Next Release (High Priority)**

These features are planned for the next major release (v1.15.0).

- **Number Whitelist**: Only forward SMS from specific, user-defined numbers.
- **Better Error Messages**: Provide user-friendly and actionable error descriptions.
- **Last Status Display**: Show the result of the last forwarding attempt in the main UI.
- **Input Validation**: Stricter validation for all user-configurable settings.
- **Secure Storage**: Encrypt sensitive settings like API keys and passwords stored in preferences.

### ⚡ **Upcoming Features (Medium Priority)**

These features are planned for subsequent releases.

#### New Platforms & Integrations

- **Discord Integration**: Forward SMS via Discord webhooks.
- **Slack Integration**: Forward SMS using the Slack Bot API.
- **Microsoft Teams Integration**: Support for forwarding to MS Teams channels via webhooks.

#### Feature Enhancements

- **Custom Message Templates**: Allow users to define their own message format using variables (e.g., `{from}`, `{content}`, `{timestamp}`).
- **Quiet Hours**: A "do not disturb" mode to disable forwarding during specific times (e.g., at night).
- **Smart Notifications**: Group multiple forwarding notifications to reduce notification spam.
- **Keyword-based Routing**: Send messages to different platforms based on keywords in the SMS content.

### 🏗️ **Architecture & Technical Debt**

Improving the foundation of the app.

- **Unit & Integration Tests**: Increase test coverage to improve reliability.
- **Code Documentation**: Add comprehensive JavaDoc comments to public APIs.
- **Performance Optimization**: Profile and reduce memory/battery usage further.
- **Modern Android Architecture**:
  - Migrate to a modern architecture (MVVM, Repository Pattern).
  - Use Dependency Injection (Hilt or Dagger).
  - Refactor async operations using Kotlin Coroutines.
- **Database Migration**: Consolidate data access logic and potentially move to Room.

### 🌟 **Future & Innovation Ideas**

Long-term ideas that are being considered.

- **Cloud Sync**: Sync settings across multiple devices using a cloud service.
- **Centralized Management**: An admin dashboard for enterprise use cases.
- **Advanced Security**: Audit logs, geofencing, and other enterprise-grade security features.
- **API Gateway**: A REST API for remote management and integration.

---

This roadmap is a living document and is subject to change.
For a detailed history of changes, see the [CHANGELOG.md](CHANGELOG.md) file.
