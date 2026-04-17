package com.spytube.app.api

import com.spytube.app.models.HiCineItem
import com.spytube.app.models.HiCinePaginatedResponse
import com.spytube.app.models.HiCineTokenResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit


interface HiCineService {

    // ── Search ─────────────────────────────────────────────────────
    @GET("search/{query}")
    suspend fun search(@Path("query") query: String): List<HiCineItem>

    /** Blocking version for Java interop — use .execute() from a background thread */
    @GET("search/{query}")
    fun searchBlocking(@Path("query") query: String): retrofit2.Call<List<HiCineItem>>

    // ── Browse by Category (paginated) ─────────────────────────────
    @GET("trending")
    suspend fun getTrending(): List<HiCineItem>

    @GET("recent")
    suspend fun getRecent(): List<HiCineItem>

    @GET("hollywood_movies")
    suspend fun getHollywoodMovies(@Query("offset") offset: Int = 0): HiCinePaginatedResponse

    @GET("bollywood_movies")
    suspend fun getBollywoodMovies(@Query("offset") offset: Int = 0): HiCinePaginatedResponse

    @GET("hollywood_series")
    suspend fun getHollywoodSeries(@Query("offset") offset: Int = 0): HiCinePaginatedResponse

    @GET("bollywood_series")
    suspend fun getBollywoodSeries(@Query("offset") offset: Int = 0): HiCinePaginatedResponse

    @GET("anime")
    suspend fun getAnime(@Query("offset") offset: Int = 0): HiCinePaginatedResponse

    // ── Detail by Slug ─────────────────────────────────────────────
    @GET("movies/{slug}")
    suspend fun getMovieDetail(@Path("slug") slug: String): HiCineItem

    @GET("series/{slug}")
    suspend fun getSeriesDetail(@Path("slug") slug: String): HiCineItem
}


interface HiCineWorkerService {

    @GET
    suspend fun getTokens(@Url url: String): HiCineTokenResponse
}


object HiCineClient {
    private const val BASE_URL = "https://api.hicine.info/api/"

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: HiCineService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HiCineService::class.java)
    }

    /** Worker client — no base URL since we use @Url for dynamic worker domains */
    val workerService: HiCineWorkerService by lazy {
        Retrofit.Builder()
            .baseUrl("https://example.com/") // Required by Retrofit but overridden by @Url
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HiCineWorkerService::class.java)
    }
}
