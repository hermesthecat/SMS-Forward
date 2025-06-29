# Value Resources (Night Mode)

This directory contains resource files that are used when the application is in "Night" or "Dark" mode. The Android system automatically selects these resources over the default ones in `res/values/` when the user has enabled Dark Theme on their device (or when it's otherwise activated by the system, e.g., through Battery Saver).

## Files

### `styles.xml`

This file provides the theme for the application's dark mode.

It defines the same `AppTheme` style as the default `styles.xml`, but it inherits from a dark theme base (`Theme.Material3.Dark`) and maps the corresponding dark color set (`md_theme_dark_*` from `colors.xml`) to the standard theme attributes. This ensures that the UI automatically adapts to a dark, eye-friendly color scheme when required.
