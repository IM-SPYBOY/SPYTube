# Changelog

## v1.5
*Released: August 2026*

### New Features
- **Peachify Player**: New primary streaming player (`peachify.top/embed/`) with resume position tracking, dub/sub preferences, and `postMessage` timeupdate events.
- **4 Streaming Servers**: Peachify (primary), ZXCStream, Videasy, VidLink — all iframe-based embed players using TMDB IDs.
- **4KHDHub Downloads**: New download source with HubCloud → Gamerxyt → pyramid.surf/gpdl bypass chain for 4K/1080p/720p content.
- **3-Way Parallel Download Fetch**: VidVault + 4KHDHub + HiCine fetched simultaneously for maximum coverage.
- **ISP Bypass Client**: Unified 3-layer bypass — DNS-over-HTTPS (Cloudflare → Google → Quad9), TLS 1.3 enforcement, Cloudflare Worker proxy fallback.
- **Download Manager**: Full PRDownloader-based download tracking with pause/resume/refresh/delete, quality picker dialog, and re-search on expired links.
- **Local File Playback**: Play downloaded MKV files directly in CinefyPlayer with resume position.
- **CinefyCache**: Provider/mediaId/position caching in SharedPreferences for instant replay.

### Technical
- Added `FourKHDHubClient.kt` — JSoup-based HTML scraper with HubCloud bypass.
- Added `IspBypassClient.kt` — shared OkHttpClient with DoH + TLS 1.3 + Worker proxy.
- Updated `DohTunnel.kt` — multi-server DoH fallback chain with unsafe TrustManager for cert-pinned domains.
- Updated `HiCineDownloadManager.kt` — 3-way parallel search, PRDownloader integration, link refresh/resume.
- Updated `CinefyPlayerActivity.kt` — Peachify embed, JS bridge (close, fallback, cacheResult, saveProgress), local file playback.
- Updated `PlayerActivity.java` — ZXCStream/Videasy/VidLink server selection with `buildApiUrl()`.
- Updated `DetailActivity.java` — 4-server selector UI, Cinefy search/load/episodes resolution.
- Added `CinefyCache.kt` — provider/mediaId/position persistence.
- Added `cinefy_player.html` — minimal iframe shell with Peachify embed + local video fallback.
- Added JSoup dependency (`org.jsoup:jsoup:1.17.2`).
- Added PRDownloader dependency (`com.github.MindorksOpenSource:PRDownloader:0.6.0`).

## v1.4
*Released: June 2026*

### New Features
- **VidVault Integration**: Token-based download proxy for direct MKV downloads.
- **HiCine RPC Search**: New `/rpc/search/` endpoint with API key authentication.
- **Download Quality Picker**: Bottom sheet dialog for selecting download quality (4K/1080p/720p/480p).

### Improved
- **Downloads Screen**: Complete rewrite with progress tracking, file size display, and delete confirmation.
- **Player Gestures**: Brightness (left edge swipe) and volume (right edge swipe) with long-press 2x speed.

## v1.3
*Released: March 11, 2026*

### Improved
- **120fps Support**: Added support for high refresh rate displays (90Hz/120Hz).
- **Silky Smooth Scrolling**: Hardware layer caching and Coil memory caching to eliminate stuttering.
- **Improved Glass UI Visibility**: Dark gradient background for clearer liquid glass effects.
- **Reduced Battery & Memory Usage**: Optimized image loading and background rendering.
- **R8 Minification**: Smaller APK size and faster launch times.

## v1.2
*Released: March 1, 2026*

### New Features
- **Live TV:** Added full Live TV player with 9000+ channels from M3U playlists.
- **Universal Channel Search:** Search across all channels globally, not limited to selected category.
- **Movies/Series Merged:** Combined Movies and Series into a single tab with Netflix-style glass toggle.
- **Modern Live Player:** Custom player UI with play/pause, live badge, and VLC-style screen resize (Fit/Zoom/Stretch/Fixed Width).
- **30 Channel Categories:** Sports, News, Entertainment, Music, Movies, Kids, Education, and more.
- **Channel Logos:** Displays channel logos with country flag fallback.

### Improved
- **Navigation:** Streamlined to 4 tabs — Movies/Series, Anime, Live TV, My List.
- **Category Chips:** Cleaned up labels (removed emojis) for a cleaner look.
- **Network:** Added cleartext traffic support for HTTP IPTV streams.
- **Caching:** 30-minute cache per category for faster browsing.

### Technical
- Added `IptvRepository.kt` with M3U playlist parser.
- Added `IptvChannel.kt` data models.
- Added `LiveTvScreen.kt` with category filtering and universal search.
- Added `LivePlayerActivity.java` with custom ExoPlayer controls.
- Added `MoviesSeriesScreen.kt` with animated toggle.
## v1.1
*Released: February 15, 2026*

- **New:** Implemented robust in-app update system with remote configuration.
- **Improved:** Reduced navigation bar height by 15% and optimized splash screen timeout.
- **Fixed:** Resolved black screen issue on Android 15/16 (Hybrid Player).
- **Fixed:** Corrected infinite loading state during failed downloads.
- **Fixed:** Addressed `WindowInsetsController` crash in PlayerActivity.
- **Web:** Updated version display logic and repository links.
