# 🎬 SPYTube - The Ultimate Streaming Experience

**SPYTube** is a cutting-edge Android streaming application designed for the modern era. Currently in **Active Development (Beta)**, it combines a futuristic **Glass UI** with a powerful, immersive video player to deliver Movies, TV Series, and Anime in stunning quality.

> **Note**: This project is for educational purposes only. It demonstrates advanced Android development techniques including native/web hybrid views, custom gesture handling, and glassmorphism design.

## ✨ Key Features

### 💎 **Glass UI Design**
Experience a truly modern interface.
*   **Real-time Blurs**: Beautiful frosted glass effects on navigation bars and overlays.
*   **Immersive Layouts**: Edge-to-edge content with no wasted space.
*   **Smooth Animations**: Fluid transitions and micro-interactions.

### 🎥 **Advanced Player**
Built for power users.
*   **Smart Gestures**: 
    *   **Swipe Right/Left** for Volume & Brightness (with 50px threshold to prevent accidental clicks).
    *   **Long Press** anywhere to speed up video to **2x**.
*   **Immersive Mode**: Hides system bars for a distraction-free experience.
*   **WebView Integration**: Seamlessly embeds advanced web players while retaining native control.

### 🌍 **Massive Content Library**
*   **Movies & TV**: Access to a vast database of global content.
*   **Anime**: Dedicated request handling for anime lovers.
*   **Search**: Instant search with rich metadata from TMDB.

### ⚙️ **Smart Features**
*   **Continue Watching**: Automatically remembers where you left off.
*   **Favorites**: Save your must-watch list.
*   **Dynamic Colors**: UI adapts to the content (coming soon).

## 🛠️ Tech Stack
*   **Language**: Kotlin & Java (Hybrid)
*   **Architecture**: MVVM
*   **UI**: XML Layouts + Jetpack Compose (Experimental) + Custom Views
*   **Network**: Retrofit + OkHttp
*   **Image Loading**: Glide + Coil
*   **Backdrop**: Liquid Glass effect libraries
*   **Analytics**: Firebase

## 📥/🔨 Installation

### Option 1: Download APK
Grab the latest release from the [Releases Page](https://github.com/IM-SPYBOY/spytube-app/releases).

### Option 2: Build from Source
1.  **Clone the repo**:
    ```bash
    git clone https://github.com/IM-SPYBOY/spytube-app.git
    ```
2.  **Add Firebase Entitlements**:
    *   You must provide your own `google-services.json`.
    *   Place it in `app/google-services.json`.
3.  **Build**:
    Open in Android Studio and run:
    ```bash
    ./gradlew assembleDebug
    ```

## 🤝 Contributing
Contributions are welcome!
1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ by [SPYBOY](https://github.com/IM-SPYBOY)*
