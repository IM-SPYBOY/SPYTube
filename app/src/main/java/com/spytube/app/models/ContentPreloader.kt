package com.spytube.app.models

import android.content.Context
import android.util.Log
import com.spytube.app.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger


object ContentPreloader {
    private const val TAG = "ContentPreloader"
    private const val TOTAL_CALLS = 5

    @Volatile var trendingItems: List<MediaItem>? = null
        private set
    @Volatile var popularMovies: List<MediaItem>? = null
        private set
    @Volatile var popularTv: List<MediaItem>? = null
        private set
    @Volatile var topRatedMovies: List<MediaItem>? = null
        private set
    @Volatile var nowPlaying: List<MediaItem>? = null
        private set
    @Volatile var isLoaded: Boolean = false
        private set

    private var onCompleteCallback: (() -> Unit)? = null

    fun preload(context: Context, onComplete: (() -> Unit)? = null) {
        onCompleteCallback = onComplete
        val completedCount = AtomicInteger(0)

        val tmdbService = ApiClient.getTmdbService()
        val apiKey = ApiClient.getApiKey(context)

        fun markDone() {
            val count = completedCount.incrementAndGet()
            Log.d(TAG, "Preload progress: $count/$TOTAL_CALLS")
            if (count >= TOTAL_CALLS) {
                isLoaded = true
                Log.d(TAG, "All preload calls complete")
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }
        }

        fun fetch(call: Call<TmdbResponse>, onResult: (List<MediaItem>) -> Unit) {
            call.enqueue(object : Callback<TmdbResponse> {
                override fun onResponse(call: Call<TmdbResponse>, response: Response<TmdbResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.results?.let { items ->
                            Log.d(TAG, "Preloaded ${items.size} items")
                            onResult(items)
                        }
                    }
                    markDone()
                }
                override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {
                    Log.e(TAG, "Preload failed", t)
                    markDone()
                }
            })
        }

        fetch(tmdbService.getTrending(apiKey)) { trendingItems = it }
        fetch(tmdbService.getPopularMovies(apiKey)) { popularMovies = it }
        fetch(tmdbService.getPopularTv(apiKey)) { popularTv = it }
        fetch(tmdbService.getTopRatedMovies(apiKey)) { topRatedMovies = it }
        fetch(tmdbService.getNowPlayingMovies(apiKey)) { nowPlaying = it }
    }
}
