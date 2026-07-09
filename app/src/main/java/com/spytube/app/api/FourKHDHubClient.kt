package com.spytube.app.api

import android.util.Log
import com.spytube.app.models.HiCineDownloadLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import android.util.Base64

object FourKHDHubClient {
    private const val TAG = "FourKHDHubClient"
    private const val BASE_URL = "https://4khdhub.one/"

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun fetchLinks(
        searchTitle: String,
        tmdbId: String,
        isTv: Boolean,
        season: Int?,
        episode: Int?
    ): List<HiCineDownloadLink> = withContext(Dispatchers.IO) {
        val links = mutableListOf<HiCineDownloadLink>()
        try {
            val query = URLEncoder.encode(searchTitle, "UTF-8")
            val searchUrl = "${BASE_URL}?s=$query"
            
            Log.d(TAG, "Searching 4KHDHub: $searchUrl")
            val searchResponse = fetchHtml(searchUrl) ?: return@withContext emptyList()
            
            val doc = Jsoup.parse(searchResponse)
            val results = doc.select("a.movie-card")
            
            fun normalizeTitle(t: String): String {
                val words = t.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim().split(Regex("\\s+"))
                return words.joinToString("") { word ->
                    when (word) {
                        "i" -> "1"
                        "ii" -> "2"
                        "iii" -> "3"
                        "iv" -> "4"
                        "v" -> "5"
                        "vi" -> "6"
                        "vii" -> "7"
                        "viii" -> "8"
                        "ix" -> "9"
                        "x" -> "10"
                        else -> word
                    }
                }
            }
            
            var matchUrl: String? = null
            for (result in results) {
                val titleElem = result.selectFirst(".movie-card-title")
                val title = titleElem?.text() ?: ""
                
                val normTitle = normalizeTitle(title)
                val normSearch = normalizeTitle(searchTitle)
                
                // Match if the normalized title contains the normalized search title
                // We avoid normSearch.contains(normTitle) because "Mortal Kombat 2".contains("Mortal Kombat")
                if (normTitle.contains(normSearch)) {
                    // Check if format matches tv vs movie
                    val format = result.selectFirst(".movie-card-format")?.text() ?: ""
                    val isResultTv = format.lowercase().contains("series")
                    
                    if (isTv == isResultTv) {
                        matchUrl = result.attr("href")
                        break
                    }
                }
            }
            
            if (matchUrl == null) {
                Log.d(TAG, "No matching title found on 4KHDHub")
                return@withContext emptyList()
            }
            
            if (!matchUrl.startsWith("http")) {
                matchUrl = "https://4khdhub.one" + matchUrl
            }
            
            Log.d(TAG, "Found match URL: $matchUrl")
            val movieHtml = fetchHtml(matchUrl) ?: return@withContext emptyList()
            val movieDoc = Jsoup.parse(movieHtml)
            
            // Extract download links
            val downloadHeaders = movieDoc.select(".download-header")
            
            for (header in downloadHeaders) {
                val fileId = header.attr("data-file-id")
                val titleElem = header.selectFirst(".flex-1.text-left")
                var rawTitle = titleElem?.text() ?: ""
                
                // For TV Series, 4khdhub uses "S01", "S02" etc.
                if (isTv && season != null) {
                    val seasonMatch = Regex("S0?(\\d+)").find(rawTitle)
                    val sNum = seasonMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (sNum != season) {
                        continue
                    }
                    rawTitle += " (Season Pack ZIP)"
                }
                
                // Content div
                val contentDiv = movieDoc.selectFirst("#content-$fileId") ?: continue
                
                // Find hubcloud link
                val hubcloudLink = contentDiv.select("a[href*='hubcloud.cx/drive']").firstOrNull()?.attr("href")
                if (hubcloudLink != null) {
                    // Start bypass
                    val directUrl = bypassHubCloud(hubcloudLink)
                    if (directUrl != null) {
                        // Extract quality
                        var quality = "Unknown"
                        if (rawTitle.contains("2160p") || rawTitle.contains("4k") || rawTitle.contains("4K")) quality = "4K"
                        else if (rawTitle.contains("1080p")) quality = "1080p"
                        else if (rawTitle.contains("720p")) quality = "720p"
                        else if (rawTitle.contains("480p")) quality = "480p"
                        
                        val sizeBadge = header.select(".badge").firstOrNull { it.text().contains("GB") || it.text().contains("MB") }
                        val size = sizeBadge?.text() ?: "Unknown"
                        
                        links.add(HiCineDownloadLink(
                            workerUrl = directUrl,
                            vcloudUrl = "",
                            quality = quality,
                            size = size,
                            description = "4KHDHub - $rawTitle",
                            source = "4KHDHub"
                        ))
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from 4KHDHub", e)
        }
        
        return@withContext links
    }
    
    private fun bypassHubCloud(hubcloudUrl: String): String? {
        try {
            Log.d(TAG, "Bypassing HubCloud: $hubcloudUrl")
            val hubCloudHtml = fetchHtml(hubcloudUrl) ?: return null
            
            val gamerxytMatch = Regex("href=\"(https://gamerxyt\\.com/hubcloud\\.php\\?[^\"]+)\"").find(hubCloudHtml)
            if (gamerxytMatch == null) {
                Log.d(TAG, "Gamerxyt link not found")
                return null
            }
            val gamerxytLink = gamerxytMatch.groupValues[1].replace("&amp;", "&")
            
            Log.d(TAG, "Bypassing Gamerxyt: $gamerxytLink")
            val gamerxytHtml = fetchHtml(gamerxytLink) ?: return null
            
            // Try new domains first (hub.pyramid.surf, gpdl.hubcloud.cx)
            val pyramidMatch = Regex("href=\"(https://hub\\.pyramid\\.surf/[^\"]+)\"").find(gamerxytHtml)
            if (pyramidMatch != null) {
                return pyramidMatch.groupValues[1].replace("&amp;", "&")
            }
            
            val gpdlMatch = Regex("href=\"(https://gpdl\\.hubcloud\\.cx/[^\"]+)\"").find(gamerxytHtml)
            if (gpdlMatch != null) {
                return gpdlMatch.groupValues[1].replace("&amp;", "&")
            }

            // Fallback: old cloudflare R2
            val r2Match = Regex("href=\"([^\"]+\\.cloudflarestorage\\.com[^\"]+)\"").find(gamerxytHtml)
            if (r2Match != null) {
                return r2Match.groupValues[1].replace("&amp;", "&")
            }
            
            // Fallback: buzz server
            val buzzMatch = Regex("href=\"(https://bzzhr\\.co/[^\"]+)\"").find(gamerxytHtml)
            if (buzzMatch != null) {
                return buzzMatch.groupValues[1].replace("&amp;", "&")
            }
            
            // Fallback: fastserver
            val fslMatch = Regex("href=\"([^\"]+\\.fastserver[^\"]+)\"").find(gamerxytHtml)
            if (fslMatch != null) {
                return fslMatch.groupValues[1].replace("&amp;", "&")
            }

            // Generic fallback: look for any large file download link
            val genericMatch = Regex("href=\"(https://[^\"]+)\"").findAll(gamerxytHtml)
                .map { it.groupValues[1].replace("&amp;", "&") }
                .filter { url ->
                    !url.contains("fontawesome") && !url.contains("bootstrap") &&
                    !url.contains("googleapis") && !url.contains("favicon") &&
                    !url.contains("hubcloud.cx/drive") && !url.contains("google.com/search") &&
                    !url.contains("t.me/") && !url.contains("snvhost") &&
                    (url.contains("token") || url.contains("?id="))
                }
                .firstOrNull()
            if (genericMatch != null) {
                Log.d(TAG, "Generic bypass match: $genericMatch")
                return genericMatch
            }
            
            Log.d(TAG, "No bypass download URL found in gamerxyt response")
        } catch (e: Exception) {
            Log.e(TAG, "Error during bypass", e)
        }
        return null
    }
    
    private fun fetchHtml(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
            
        okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }
}
