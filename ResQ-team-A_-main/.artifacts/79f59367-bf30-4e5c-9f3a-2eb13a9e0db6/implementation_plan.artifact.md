# Fix Dark/Light Mode Toggling

The goal is to make the theme toggle button functional and ensure the app correctly transitions between dark and light modes. Currently, the app theme is hardcoded to a light theme in the manifest, and the theme switching logic in `MainActivity` is not optimal.

## Proposed Changes

### [Component] Android Manifest
- [MODIFY] [AndroidManifest.xml](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/AndroidManifest.xml)
  - Change `android:theme` from `@style/Theme.AppCompat.Light.DarkActionBar` to `@style/Theme.Pheonixnet` to support DayNight themes.

### [Component] Main Activity
- [MODIFY] [MainActivity.java](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/java/com/phoenix/phoenixnet/MainActivity.java)
  - Move `AppCompatDelegate.setDefaultNightMode` calls to before `super.onCreate` to ensure the activity is initialized with the correct theme.
  - Update `theme_button` click listener to toggle the mode, save the preference, and update the button's text/icon.
  - Add logic to update the MapView color filter when in dark mode to provide a consistent dark experience.
  - Fix the button text to show the correct icon (☀️ for dark mode, 🌙 for light mode).

## Verification Plan

### Manual Verification
- Deploy the app to the emulator/device.
- Tap the "Mode" button.
- Verify that:
  1. The background color changes (Light -> Dark / Dark -> Light).
  2. The text colors remain readable.
  3. The "Mode" button text/icon updates.
  4. The MapView colors are inverted in dark mode.
  5. The theme persists after app restart.
