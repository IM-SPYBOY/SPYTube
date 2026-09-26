<p align="center">
  <img src="1-SPYTube-crescent.png" width="200" alt="SPYTube gold crescent icon on a black background" />
</p>
<h1 align="center">SPYTube</h1>
<p align="center">
  <strong>Open-source Android streaming client</strong><br>
  Movies · Series · Anime · Live TV
</p>

<p align="center">
  <a href="https://github.com/IM-SPYBOY/SPYTube/releases/latest"><img src="https://img.shields.io/github/v/release/IM-SPYBOY/SPYTube?style=flat-square&color=2997ff" alt="Release" /></a>
  <a href="https://github.com/IM-SPYBOY/SPYTube/releases"><img src="https://img.shields.io/github/downloads/IM-SPYBOY/SPYTube/total?style=flat-square&color=34c759" alt="Downloads" /></a>
  <a href="https://github.com/IM-SPYBOY/SPYTube/blob/main/LICENSE"><img src="https://img.shields.io/github/license/IM-SPYBOY/SPYTube?style=flat-square" alt="License" /></a>
  <a href="https://t.me/SPYxTube"><img src="https://img.shields.io/badge/Telegram-Channel-0088cc?style=flat-square&logo=telegram" alt="Telegram" /></a>
</p>

---

## Overview

SPYTube is an Android streaming client built with Kotlin and Jetpack Compose. This repository also includes a web interface. The Android app brings movies, series, anime, and live TV sources into one interface. See the project files and release notes for current implementation details.

---

## Features

- **Glass UI** — Frosted glass panels rendered with AGSL shaders. Real-time blur on every card, navbar, and overlay.
- **Gesture Player** — Swipe for brightness/volume, long-press for 2x, tap to play/pause. Zero button clutter.
- **Resume Playback** — Watch history with per-title progress. Pick up exactly where you left off.
- **Offline Downloads** — Download movies and episodes for offline viewing via the built-in download manager.
- **Multi-Server** — Multiple streaming providers with automatic fallover. Switch sources instantly.
- **Live TV** — Stream live television channels directly in-app.
- **DNS-over-HTTPS** — All network traffic routed through Cloudflare DoH with connection pooling.
- **120fps Support** — Requests highest available display refresh rate on compatible devices.

---

## Screenshots

<p align="center">
  <img src="screenshots/home.png" width="200" />
  <img src="screenshots/detail.png" width="200" />
  <img src="screenshots/player.png" width="200" />
  <img src="screenshots/search.png" width="200" />
</p>

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin, Java |
| UI | Jetpack Compose, XML Views |
| Shaders | AGSL (Android Graphics Shading Language) |
| Networking | OkHttp + Retrofit, DNS-over-HTTPS |
| Player | WebView + HLS.js (bundled locally) |
| Image Loading | Coil |
| Data | SharedPreferences, TMDB API |
| Build | Gradle (Groovy DSL), JDK 17 |

---

## Build

```bash
# Clone
git clone https://github.com/IM-SPYBOY/SPYTube.git
cd SPYTube

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

> Build configuration currently uses JDK 17 and Android compile SDK 36. A local Android SDK is needed. The build commands above have not been tested as part of this README edit; the signing setup for release builds needs review before publishing.

---

## Project Structure

```
app/src/main/java/com/spytube/app/
├── api/              # Network layer (Retrofit, OkHttp, DoH tunnel)
├── models/           # Data models, caches, managers
├── adapters/         # RecyclerView adapters
├── ui/
│   ├── components/   # Glass cards, navbar, hero banner, shaders
│   ├── screens/      # Compose screens (Home, Search, Downloads, Live TV)
│   └── theme/        # Color system, typography
├── MainActivity.kt   # App entry point
├── DetailActivity.java
├── CinefyPlayerActivity.kt
└── SplashActivity.kt
```

---

## Download

Get the [latest published Android release](https://github.com/IM-SPYBOY/SPYTube/releases/latest) or visit [SPYTube Web](https://spytube.in). At the time of this draft, the latest published release is v1.5. Check the release page for current versions and assets.

---

## Feedback and status

Use [Issues](https://github.com/IM-SPYBOY/SPYTube/issues) for reproducible bugs and feature requests. Include the app version, Android version, expected behavior, and steps to reproduce. Some existing issues remain open; do not assume every advertised feature works on every device or source.

---

## Disclaimer

This application does not host, store, or distribute any media content. All streams are sourced from publicly available third-party APIs. All rights to the content belong to their respective owners.

SPYTube does not claim ownership or responsibility for any material displayed. The app functions solely as an aggregator of publicly accessible resources.

If you are a copyright holder and believe any content infringes your rights, please [contact us](https://t.me/Spyboi) for prompt removal.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  Built by <a href="https://github.com/IM-SPYBOY">SPYBOY</a>
</p>
