# Layout Resources

This directory contains the XML layout files that define the structure of the user interface for the application's activities and fragments.

## Files

### `activity_main.xml`

This is the main layout file for the `MainActivity`. It defines a simple `LinearLayout` that contains a single `FrameLayout` with the ID `@+id/settings`.

This `FrameLayout` acts as a container where the `SettingsFragment` is dynamically loaded at runtime. All the user-configurable settings and action buttons are displayed within this fragment, making the main activity just a host for the preferences UI.
