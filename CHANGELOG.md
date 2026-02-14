# Changelog

## v1.1
*Released: February 15, 2026*

### ✨ New Features
- **In-App Updates:** Added a robust auto-update system. The app now checks for updates on launch and supports both optional and forced updates via Firebase Remote Config.
- **Experimental 120Hz Support:** Enabled high refresh rate support for smoother scrolling and animations on supported devices.

### 🐛 Bug Fixes
- **Android 15/16 Black Screen:** Fixed a critical issue where the video player would show a black screen on Android 15 and 16 devices. This was resolved by implementing a hybrid player system (VideoView for A14+, ExoPlayer for older versions).
- **Infinite Loading:** Resolved an issue where the loading spinner would spin indefinitely if a download failed.
- **Splash Screen:** Fixed a potential crash on splash screen and reduced the safety timeout to 3 seconds for a faster startup.
- **Player Crash:** Fixed a `NullPointerException` related to `WindowInsetsController` in `PlayerActivity`.
- **Download 404:** Corrected the update URL to point to the valid GitHub release path.

### 🎨 UI/UX Improvements
- **Glass Navbar:** Reduced the height of the bottom navigation bar by 15% (to 51dp) for a sleeker look, while maintaining icon touch targets.
- **Website:** Updated the landing page to correctly display the version as "v1.0" (removing trailing zeros) and fixed repository links.
