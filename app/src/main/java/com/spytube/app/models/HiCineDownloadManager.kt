package com.spytube.app.models

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.spytube.app.api.HiCineClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


object HiCineDownloadManager {

    private const val TAG = "HiCineDownload"

    // ── Link Parsing ───────────────────────────────────────────────

    
    fun parseMovieLinks(rawLinks: String?): List<HiCineDownloadLink> {
        if (rawLinks.isNullOrBlank()) return emptyList()
        val results = mutableListOf<HiCineDownloadLink>()

        for (line in rawLinks.split("\n")) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            try {
                val parts = trimmed.split(", ")
                if (parts.isEmpty()) continue

                val fullUrl = parts[0].trim()
                if (!fullUrl.startsWith("http")) continue

                // Extract worker base URL and vcloud parameter
                val uri = Uri.parse(fullUrl)
                val workerUrl = "${uri.scheme}://${uri.host}"
                val vcloudUrl = uri.getQueryParameter("vcloud") ?: continue

                // Last part is file size, second-to-last is description
                val size = parts.lastOrNull()?.trim() ?: ""
                val description = if (parts.size >= 2) parts[parts.size - 2].trim() else ""

                // Extract quality from description
                val quality = extractQuality(description)

                results.add(
                    HiCineDownloadLink(
                        workerUrl = workerUrl,
                        vcloudUrl = vcloudUrl,
                        quality = quality,
                        size = size,
                        description = description
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse link line: $trimmed", e)
            }
        }
        return results
    }

    
    fun parseSeriesSeasonLinks(seasonData: String?): List<HiCineDownloadLink> {
        if (seasonData.isNullOrBlank()) return emptyList()
        val results = mutableListOf<HiCineDownloadLink>()

        val lines = seasonData.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // Match "Episode N :" pattern
            val epMatch = Regex("Episode\\s+(\\d+)\\s*:", RegexOption.IGNORE_CASE).find(trimmed) ?: continue
            val episodeNum = epMatch.groupValues[1].toIntOrNull() ?: continue

            // Split on " : " to get quality variants (skip first which is "Episode N")
            val epContent = trimmed.substring(epMatch.range.last + 1)
            val qualityParts = epContent.split(" : ")

            for (part in qualityParts) {
                val pt = part.trim()
                if (!pt.startsWith("http")) continue

                try {
                    // Format: {url}, {size},{quality}
                    val commaIdx = pt.indexOf(",")
                    if (commaIdx < 0) continue
                    val fullUrl = pt.substring(0, commaIdx).trim()
                    val remainder = pt.substring(commaIdx + 1).trim()

                    val uri = Uri.parse(fullUrl)
                    val workerUrl = "${uri.scheme}://${uri.host}"
                    val vcloudUrl = uri.getQueryParameter("vcloud") ?: continue

                    // remainder is like "284.07 MB,720p" or "497.76 MB,1080p"
                    val remParts = remainder.split(",")
                    val size = remParts.getOrElse(0) { "" }.trim()
                    val quality = remParts.getOrElse(1) { "" }.trim().ifEmpty { "Unknown" }

                    results.add(
                        HiCineDownloadLink(
                            workerUrl = workerUrl,
                            vcloudUrl = vcloudUrl,
                            quality = quality,
                            size = size,
                            description = "Episode $episodeNum - $quality",
                            episodeNumber = episodeNum
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse series link: $pt", e)
                }
            }
        }
        return results
    }

    
    private fun extractQuality(text: String): String {
        return when {
            text.contains("1080p", true) -> "1080p"
            text.contains("720p", true) -> "720p"
            text.contains("480p", true) -> "480p"
            text.contains("2160p", true) || text.contains("4K", true) -> "4K"
            text.contains("HEVC", true) -> "720p HEVC"
            else -> "Unknown"
        }
    }

    // ── Token Resolution ───────────────────────────────────────────

    
    suspend fun getTokens(link: HiCineDownloadLink): HiCineTokenResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedVcloud = URLEncoder.encode(link.vcloudUrl, "UTF-8")
                val tokenUrl = "${link.workerUrl}/api/links?vcloud=$encodedVcloud"
                Log.d(TAG, "Fetching tokens: $tokenUrl")
                HiCineClient.workerService.getTokens(tokenUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get tokens", e)
                null
            }
        }
    }

    
    suspend fun resolveDownloadUrl(
        link: HiCineDownloadLink,
        tokenResponse: HiCineTokenResponse,
        serverType: String = "fsl"
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenResponse.tokens?.get(serverType) ?: run {
                    // Fallback: try any available server
                    tokenResponse.tokens?.entries?.firstOrNull()?.value
                } ?: return@withContext null

                val encodedVcloud = URLEncoder.encode(link.vcloudUrl, "UTF-8")
                val goUrl = "${link.workerUrl}/go?type=$serverType" +
                        "&vcloud=$encodedVcloud" +
                        "&ts=${token.ts}" +
                        "&sig=${token.sig}"

                Log.d(TAG, "Resolving redirect: $goUrl")

                // Follow the 302 manually to extract Location header
                val conn = URL(goUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.connect()

                val location = conn.getHeaderField("Location")
                conn.disconnect()

                Log.d(TAG, "Resolved to: $location")
                location
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve download URL", e)
                null
            }
        }
    }

    // ── Android DownloadManager Integration ────────────────────────

    
    suspend fun startDownload(
        context: Context,
        link: HiCineDownloadLink,
        title: String,
        posterUrl: String? = null
    ): Long? {
        val downloadUrl: String

        if (link.source == "VidVault") {
            // VidVault already provides the direct MKV link in workerUrl
            downloadUrl = link.workerUrl
            if (downloadUrl.isBlank()) return null
        } else {
            // Step 1: Get tokens (HiCine)
            val tokenResponse = getTokens(link)
            if (tokenResponse == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to get download servers", Toast.LENGTH_SHORT).show()
                }
                return null
            }

            // Step 2: Pick best server
            val serverType = tokenResponse.tokens?.keys?.let { keys ->
                when {
                    "fsl" in keys -> "fsl"
                    "fsl2" in keys -> "fsl2"
                    else -> keys.firstOrNull()
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No download servers available", Toast.LENGTH_SHORT).show()
                }
                return null
            }

            // Step 3: Resolve final URL
            val resolved = resolveDownloadUrl(link, tokenResponse, serverType)
            if (resolved.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to resolve download link", Toast.LENGTH_SHORT).show()
                }
                return null
            }
            downloadUrl = resolved
        }

        // Step 4: Enqueue in Android DownloadManager
        return withContext(Dispatchers.Main) {
            try {
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9()\\-_ ]"), "").trim()
                val fileName = "${sanitizedTitle}_${link.quality}.mkv"

                val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                    setTitle("$title (${link.quality})")
                    setDescription("${link.size} — SPYTube Download")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalFilesDir(
                        context, Environment.DIRECTORY_DOWNLOADS, fileName
                    )
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(false)
                    // Cloudflare workers block "AndroidDownloadManager" user-agent, resulting in 403 Forbidden 
                    // and creating 0-byte empty files. Mocking a desktop browser fixes this immediately.
                    addRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    addRequestHeader("Referer", "https://vidvault.ru/")
                    addRequestHeader("Origin", "https://vidvault.ru")
                }

                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = dm.enqueue(request)

                // Save download info for tracking in DownloadsScreen
                saveDownloadInfo(context, downloadId, title, link.quality, link.size, fileName, posterUrl)

                Toast.makeText(context, "Download started: $title (${link.quality})", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Download enqueued: id=$downloadId file=$fileName")
                downloadId
            } catch (e: Exception) {
                Log.e(TAG, "DownloadManager enqueue failed", e)
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    
    @JvmStatic
    fun startDownloadBlocking(context: Context, link: HiCineDownloadLink, title: String, posterUrl: String? = null): Long? {
        return kotlinx.coroutines.runBlocking {
            startDownload(context, link, title, posterUrl)
        }
    }

    
    @JvmStatic
    fun searchDownloadsBlocking(searchTitle: String, tmdbId: String, isTv: Boolean, season: Int?, episode: Int?): List<HiCineDownloadLink> {
        return kotlinx.coroutines.runBlocking {
            val hicineJob = async(Dispatchers.IO) {
                try {
                    val results = HiCineClient.service.search(URLEncoder.encode(searchTitle, "UTF-8"))
                    val match = results.firstOrNull { it.title.lowercase().contains(searchTitle.lowercase()) } ?: results.firstOrNull()
                    if (match != null) {
                        if (isTv) {
                            parseSeriesSeasonLinks(match.getSeasonData(season ?: 1))
                                .filter { episode == null || it.episodeNumber == episode }
                        } else {
                            parseMovieLinks(match.links)
                        }
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val vidVaultJob = async(Dispatchers.IO) {
                com.spytube.app.api.VidVaultClient.fetchLinks(
                    tmdbId = tmdbId,
                    type = if (isTv) "tv" else "movie",
                    season = season,
                    episode = episode,
                    title = searchTitle
                )
            }

            val hicineLinks = hicineJob.await()
            val vidVaultLinks = vidVaultJob.await()
            vidVaultLinks + hicineLinks
        }
    }

    // ── Download Tracking (SharedPreferences) ──────────────────────

    private const val PREFS_NAME = "hicine_downloads"

    private fun saveDownloadInfo(
        context: Context,
        downloadId: Long,
        title: String,
        quality: String,
        size: String,
        fileName: String,
        posterUrl: String?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("download_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(downloadId.toString())
        prefs.edit()
            .putStringSet("download_ids", ids)
            .putString("dl_${downloadId}_title", title)
            .putString("dl_${downloadId}_quality", quality)
            .putString("dl_${downloadId}_size", size)
            .putString("dl_${downloadId}_file", fileName)
            .putLong("dl_${downloadId}_time", System.currentTimeMillis())
            .putString("dl_${downloadId}_poster", posterUrl ?: "")
            .apply()
    }

    /** Get all tracked download IDs */
    fun getDownloadIds(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("download_ids", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.sortedDescending()
            ?: emptyList()
    }

    /** Get metadata for a specific download */
    fun getDownloadMeta(context: Context, downloadId: Long): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "title" to (prefs.getString("dl_${downloadId}_title", "") ?: ""),
            "quality" to (prefs.getString("dl_${downloadId}_quality", "") ?: ""),
            "size" to (prefs.getString("dl_${downloadId}_size", "") ?: ""),
            "file" to (prefs.getString("dl_${downloadId}_file", "") ?: ""),
            "time" to (prefs.getLong("dl_${downloadId}_time", 0).toString()),
            "poster" to (prefs.getString("dl_${downloadId}_poster", "") ?: "")
        )
    }

    /** Remove a download record */
    fun removeDownload(context: Context, downloadId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("download_ids", mutableSetOf())?.toMutableSet() ?: return
        ids.remove(downloadId.toString())
        prefs.edit()
            .putStringSet("download_ids", ids)
            .remove("dl_${downloadId}_title")
            .remove("dl_${downloadId}_quality")
            .remove("dl_${downloadId}_size")
            .remove("dl_${downloadId}_file")
            .remove("dl_${downloadId}_time")
            .remove("dl_${downloadId}_poster")
            .apply()
    }
    // Helper to find a completed download by title
    @JvmStatic
    fun getCompletedDownloadMeta(context: Context, targetTitle: String): Map<String, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = getDownloadIds(context)
        for (id in ids) {
            val title = prefs.getString("dl_${id}_title", "")
            if (title == targetTitle) {
                val fileName = prefs.getString("dl_${id}_file", "")
                if (!fileName.isNullOrEmpty()) {
                    val file = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (file.exists()) {
                        val meta = getDownloadMeta(context, id).toMutableMap()
                        meta["downloadId"] = id.toString()
                        meta["localUri"] = file.toURI().toString()
                        return meta
                    }
                }
            }
        }
        return null
    }

    // Helper to completely delete a downloaded file and record from Java or Kotlin
    @JvmStatic
    fun removeDownloadAndFile(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(downloadId)
        val meta = getDownloadMeta(context, downloadId)
        val fileName = meta["file"] ?: ""
        if (fileName.isNotEmpty()) {
            val file = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (file.exists()) file.delete()
        }
        removeDownload(context, downloadId)
    }
}
