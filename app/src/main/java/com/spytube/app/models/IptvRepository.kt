package com.spytube.app.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


object IptvRepository {
    private const val BASE = "https://iptv-org.github.io/iptv"
    private const val INDEX_URL = "$BASE/index.m3u"

    // Category-specific playlist URLs
    private fun categoryUrl(cat: String) = "$BASE/categories/$cat.m3u"
    private fun countryUrl(code: String) = "$BASE/countries/${code.lowercase()}.m3u"

    // Predefined categories
    val CATEGORIES = listOf(
        IptvCategory("all", "All"),
        IptvCategory("indian", "Indian"),
        IptvCategory("cricket", "Cricket"),
        IptvCategory("sports", "Sports"),
        IptvCategory("news", "News"),
        IptvCategory("entertainment", "Entertainment"),
        IptvCategory("music", "Music"),
        IptvCategory("movies", "Movies"),
        IptvCategory("kids", "Kids"),
        IptvCategory("education", "Education"),
        IptvCategory("comedy", "Comedy"),
        IptvCategory("cooking", "Cooking"),
        IptvCategory("documentary", "Documentary"),
        IptvCategory("lifestyle", "Lifestyle"),
        IptvCategory("religious", "Religious"),
        IptvCategory("science", "Science"),
        IptvCategory("shop", "Shopping"),
        IptvCategory("travel", "Travel"),
        IptvCategory("weather", "Weather"),
        IptvCategory("animation", "Animation"),
        IptvCategory("auto", "Auto"),
        IptvCategory("business", "Business"),
        IptvCategory("classic", "Classic"),
        IptvCategory("culture", "Culture"),
        IptvCategory("family", "Family"),
        IptvCategory("general", "General"),
        IptvCategory("legislative", "Legislative"),
        IptvCategory("outdoor", "Outdoor"),
        IptvCategory("relax", "Relax"),
        IptvCategory("series", "Series")
    )

    // Per-category cache (thread-safe for Dispatchers.IO pool)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L

    private data class CacheEntry(
        val channels: List<IptvChannel>,
        val timestamp: Long
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    
    suspend fun loadChannels(categoryId: String, forceRefresh: Boolean = false): Result<List<IptvChannel>> =
        withContext(Dispatchers.IO) {
            // Check cache
            if (!forceRefresh) {
                val cached = cache[categoryId]
                if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION_MS) {
                    return@withContext Result.success(cached.channels)
                }
            }

            try {
                val url = when (categoryId) {
                    "all" -> INDEX_URL
                    "indian" -> countryUrl("in")
                    "cricket" -> countryUrl("in") // Fetch Indian channels, then filter
                    else -> categoryUrl(categoryId)
                }

                val m3uContent = fetchText(url)
                var channels = parseM3U(m3uContent)

                // For cricket tab, filter to only sports/cricket channels
                if (categoryId == "cricket") {
                    val cricketKeywords = listOf(
                        "star sports", "sony sports", "dd sports",
                        "cricket", "sports ten", "sports 1", "sports 2", "sports 3",
                        "sports select", "jio cinema sport", "fancode",
                        "sports tamil", "sports telugu", "sports hindi",
                        "willow", "supersport"
                    )
                    channels = channels.filter { ch ->
                        val nameLower = ch.name.lowercase()
                        val isSportsCategory = ch.categories.any { it.lowercase() == "sports" }
                        isSportsCategory || cricketKeywords.any { kw -> nameLower.contains(kw) }
                    }
                }

                cache[categoryId] = CacheEntry(channels, System.currentTimeMillis())
                Result.success(channels)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    
    suspend fun searchAll(query: String): Result<List<IptvChannel>> = withContext(Dispatchers.IO) {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return@withContext Result.success(emptyList())

        try {
            // Ensure the full index is loaded
            val cached = cache["all"]
            val allChannels = if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION_MS) {
                cached.channels
            } else {
                val m3uContent = fetchText(INDEX_URL)
                val channels = parseM3U(m3uContent)
                cache["all"] = CacheEntry(channels, System.currentTimeMillis())
                channels
            }

            val results = allChannels.filter { ch ->
                ch.name.lowercase().contains(q) ||
                ch.country.lowercase().contains(q) ||
                ch.categories.any { it.lowercase().contains(q) }
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCategories(): List<IptvCategory> = CATEGORIES

    
    private fun parseM3U(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                // Parse EXTINF line
                val attrs = parseExtInf(line)
                val name = attrs["name"] ?: "Unknown Channel"
                val id = attrs["tvg-id"] ?: name.replace(" ", "")
                val country = attrs["tvg-country"] ?: ""
                val logo = attrs["tvg-logo"]
                val group = attrs["group-title"] ?: ""
                val categories = if (group.isNotEmpty()) group.split(";").map { it.trim() } else emptyList()

                // Next non-empty, non-comment line is the stream URL
                i++
                while (i < lines.size && (lines[i].isBlank() || lines[i].trim().startsWith("#"))) {
                    i++
                }
                if (i < lines.size) {
                    val streamUrl = lines[i].trim()
                    if (streamUrl.startsWith("http")) {
                        channels.add(
                            IptvChannel(
                                id = id,
                                name = name,
                                country = country,
                                categories = categories,
                                streamUrl = streamUrl,
                                quality = null,
                                referrer = null,
                                userAgent = null,
                                logoUrl = logo
                            )
                        )
                    }
                }
            }
            i++
        }

        return channels
    }

    
    private fun parseExtInf(line: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()

        // Extract key="value" pairs
        val attrRegex = """(\w[\w-]*)="([^"]*?)"""".toRegex()
        attrRegex.findAll(line).forEach { match ->
            attrs[match.groupValues[1]] = match.groupValues[2]
        }

        // Channel name is after the last comma
        val commaIdx = line.lastIndexOf(',')
        if (commaIdx >= 0 && commaIdx < line.length - 1) {
            attrs["name"] = line.substring(commaIdx + 1).trim()
        }

        return attrs
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            return response.body?.string() ?: throw Exception("Empty body")
        }
    }
}
