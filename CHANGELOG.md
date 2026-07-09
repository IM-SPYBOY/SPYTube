# Changelog

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
