# Value Resources (Default)

This directory contains the default value resource files for the application. These files define fundamental values such as strings, colors, styles, and arrays that are used throughout the app. These are the base resources, which can be overridden by more specific configurations (e.g., `values-night` for dark mode or `values-tr` for the Turkish language).

## Files

### `strings.xml`

This is the default string resource file (in English). It contains all the user-facing text for the application's UI, including:

- Labels, titles, and summaries for the settings screen.
- Informational text for dialogs, toasts, and notifications.
- Content-formatting strings used by the forwarding logic.
- Descriptive text for the "About" screen.
- String keys (marked with `translatable="false"`) for `Preference` items.

### `colors.xml`

This file defines the application's color palette. It specifies a full set of [Material Design 3](https://m3.material.io/) colors for both a light theme and a dark theme. These named colors are referenced in the `styles.xml` file to build the application's themes.

### `styles.xml`

This file defines the base application theme (`AppTheme`), which inherits from `Theme.Material3.Light`. It maps the named colors from `colors.xml` to the standard Android theme attributes, ensuring a consistent and modern look and feel for the light mode of the application.

### `arrays.xml`

This file contains string arrays used to populate `ListPreference` widgets in the settings screen. It defines both the user-visible text (e.g., "Light", "Dark") and the corresponding internal values (e.g., "light", "dark") for settings like:

- SMTP Username Style
- Theme Mode (Light/Dark/System)
- Application Language (English/Turkish/System)
