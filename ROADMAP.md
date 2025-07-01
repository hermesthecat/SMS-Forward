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
- **Input Validation**: Stricter validation for all user-configurable settings.
- **Better Error Messages**: Provide user-friendly and actionable error descriptions.
- **Last Status Display**: Show the result of the last forwarding attempt in the main UI.

### **v1.16.0: Platform Expansion & Customization**

- **Custom Message Templates**: Allow users to define their own message format using variables (e.g., `{from}`, `{content}`, `{timestamp}`).

### **v1.17.0: Advanced User Experience (QoL)**

- **Setup Wizard**: A step-by-step guide for first-time users to grant permissions and configure a platform.
- **Contact Name Integration**: Display sender's name from the device's contacts in forwarded messages. _(Requires new permissions)_
- **Home Screen Widget**: A widget to show app status, message counts, and quick toggles.

### **v1.18.0: Advanced Logic & Control**

- **Dual SIM Support**: Allow users to select which SIM card's messages to forward.
- **Regex-based Filtering**: Add support for regular expressions for advanced content filtering.
- **Keyword-based Routing**: Send messages to different platforms based on keywords in the SMS content.

### **v2.0.0: Architecture & Core Overhaul**

- **Performance Optimization**: Profile and significantly reduce memory/battery usage.
- **Code Documentation**: Add comprehensive JavaDoc comments to public APIs.

### **Future & Innovation Ideas (Post-v2.0.0)**

- **Cloud Sync**: Sync settings across multiple devices.

---

This roadmap is a living document and is subject to change.
For a detailed history of changes, see the [CHANGELOG.md](CHANGELOG.md) file.
