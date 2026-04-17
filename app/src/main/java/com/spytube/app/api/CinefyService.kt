package com.spytube.app.api

import com.spytube.app.models.CinefyDetailResponse
import com.spytube.app.models.CinefyEpisodesResponse
import com.spytube.app.models.CinefyHomeResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


interface CinefyService {

    @GET("{provider}/home")
    suspend fun getHome(@Path("provider") provider: String): CinefyHomeResponse

    @GET("{provider}/load/{id}")
    suspend fun getDetail(
        @Path("provider") provider: String,
        @Path("id") id: String
    ): CinefyDetailResponse

    @GET("{provider}/episodes/{titleId}/{seasonId}")
    suspend fun getEpisodes(
        @Path("provider") provider: String,
        @Path("titleId") titleId: String,
        @Path("seasonId") seasonId: String
    ): CinefyEpisodesResponse

    @GET("{provider}/search")
    suspend fun search(
        @Path("provider") provider: String,
        @Query("q") query: String
    ): CinefyHomeResponse   // Same shape: { success, data: [ { name, results }] }
}


object CinefyClient {
    private const val BASE_URL = "https://cinefy.lol/api/"

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: CinefyService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CinefyService::class.java)
    }

    /** Build the embeddable player URL for a Cinefy media item */
    fun buildPlayerUrl(provider: String, mediaId: String, title: String? = null): String {
        val encoded = java.net.URLEncoder.encode(title ?: "", "UTF-8")
        return "${BASE_URL}${provider}/player/${mediaId}?title=$encoded"
    }
}
