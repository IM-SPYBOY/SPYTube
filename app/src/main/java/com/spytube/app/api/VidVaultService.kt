package com.spytube.app.api

import com.spytube.app.models.VidVaultRequest
import com.spytube.app.models.VidVaultResponse
import com.spytube.app.models.VidVaultTokenResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface VidVaultService {
    @GET("get-token")
    suspend fun getToken(): VidVaultTokenResponse

    @GET("get-token")
    fun getTokenBlocking(): retrofit2.Call<VidVaultTokenResponse>

    @POST("download-proxy")
    suspend fun getDownloadProxy(
        @Header("x-request-token") token: String,
        @Body request: VidVaultRequest
    ): okhttp3.ResponseBody

    @POST("download-proxy")
    fun getDownloadProxyBlocking(
        @Header("x-request-token") token: String,
        @Body request: VidVaultRequest
    ): retrofit2.Call<VidVaultResponse>
}

object VidVaultClient {
    private const val BASE_URL = "https://vidvault.ru/api/"

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .addHeader("Referer", "https://vidvault.ru/")
                .addHeader("Origin", "https://vidvault.ru")
                .build()
            chain.proceed(request)
        }
        .build()

    val service: VidVaultService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VidVaultService::class.java)
    }

    private const val DOWNLOAD_WORKER = "https://dl.gemlelispe.workers.dev"

    var lastError: String = ""

    
    suspend fun resolveTmdbId(title: String, isMovie: Boolean): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tmdbService = ApiClient.getTmdbService()
                val apiKey = "54e00466a09676df57ba51c4ca30b1a6"
                val response = tmdbService.searchMulti(apiKey, title).execute()
                val results = response.body()?.results ?: return@withContext null
                val mediaType = if (isMovie) "movie" else "tv"
                val match = results.firstOrNull {
                    val name = (it.title ?: it.name ?: "").lowercase()
                    val type = it.mediaType ?: ""
                    name.contains(title.lowercase()) && type == mediaType
                } ?: results.firstOrNull {
                    val type = it.mediaType ?: ""
                    type == mediaType
                } ?: results.firstOrNull()

                match?.id?.toString()
            } catch (e: Exception) {
                android.util.Log.e("VidVaultFallback", "TMDB resolution failed", e)
                null
            }
        }
    }

    suspend fun fetchLinks(
        tmdbId: String,
        type: String = "movie",
        season: Int? = null,
        episode: Int? = null,
        title: String? = null
    ): List<com.spytube.app.models.HiCineDownloadLink> {
        val maxRetries = 3
        lastError = ""
        for (attempt in 1..maxRetries) {
            try {
                val token = service.getToken().token
                val req = VidVaultRequest(
                    type = type,
                    tmdbId = tmdbId,
                    season = season,
                    episode = episode
                )
                val res = service.getDownloadProxy(token, req)
                val jsonRaw = res.string()
                val gson = com.google.gson.Gson()
                val parsed: com.spytube.app.models.VidVaultResponse = gson.fromJson(jsonRaw, com.spytube.app.models.VidVaultResponse::class.java)

                val results = mutableListOf<com.spytube.app.models.HiCineDownloadLink>()

                val fileLabel = if (type == "tv" && season != null && episode != null) {
                    "${title ?: "Episode"} S${season}E${episode}"
                } else {
                    title ?: "Download"
                }
                val encodedName = java.net.URLEncoder.encode(fileLabel, "UTF-8")

                val wrapper = parsed.extractData?.dataWrapper
                // Check for 403 or other extraction errors
                if (wrapper?.code != 0 && wrapper?.code != null) {
                    lastError = "Proxy Code: ${wrapper.code} Msg: ${wrapper.message}"
                }
                if (parsed.extractData == null) {
                    lastError = "Response missing extractData. JSON length: ${jsonRaw.length}"
                }

                val streams = wrapper?.data?.streams ?: emptyList()
                val downloads = wrapper?.data?.downloads ?: emptyList()
                val allMedia = streams + downloads
                
                if (allMedia.isEmpty() && lastError.isEmpty()) {
                    lastError = "Valid JSON but 0 streams found. Raw Response: ${jsonRaw.take(100)}..."
                }

                for (dl in allMedia) {
                    val url = dl.url ?: continue
                    if (url.isBlank()) continue
                    val sizeBytes = dl.size?.toLongOrNull() ?: 0L
                    val humanSize = formatBytes(sizeBytes)
                    val resolution = dl.resolution ?: 720
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val proxyUrl = "$DOWNLOAD_WORKER/$encodedUrl?n=$encodedName"
                    results.add(
                        com.spytube.app.models.HiCineDownloadLink(
                            workerUrl = proxyUrl,
                            vcloudUrl = "",
                            quality = "${resolution}p",
                            size = humanSize,
                            description = "VidVault ${resolution}p",
                            source = "VidVault"
                        )
                    )
                }

                parsed.mkvData?.files?.forEach { file ->
                    val fileUrl = file.url
                    if (fileUrl != null && fileUrl.isNotBlank()) {
                        results.add(
                            com.spytube.app.models.HiCineDownloadLink(
                                workerUrl = fileUrl,
                                vcloudUrl = "",
                                quality = "MKV",
                                size = file.size ?: "Unknown",
                                description = "VidVault MKV",
                                source = "VidVault"
                            )
                        )
                    }
                }

                // If we got results, return immediately
                if (results.isNotEmpty()) return results
                // Only retry if it's not the last attempt
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(500L * attempt)
                    continue
                }
            } catch (e: Exception) {
                lastError = "Exception: ${e.message}"
                android.util.Log.e("VidVaultFallback", "fetchLinks loop exception: ${e.message}", e)
                if (attempt == maxRetries) {
                    // fall through to return emptyList
                } else {
                    kotlinx.coroutines.delay(500)
                }
            }
        }
        android.util.Log.e("VidVaultFallback", "All retries exhausted. lastError=$lastError")
        return emptyList()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.0f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
