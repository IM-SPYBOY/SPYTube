package com.spytube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spytube.app.api.ApiClient
import com.spytube.app.models.ContentRow
import com.spytube.app.models.MediaItem
import com.spytube.app.models.TmdbResponse
import com.spytube.app.ui.components.GlassMediaCard
import com.spytube.app.ui.theme.White
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun SeriesScreen(
    onMediaClick: (MediaItem) -> Unit
) {
    var contentRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val tmdbService = ApiClient.getTmdbService()
        val apiKey = ApiClient.getApiKey(context)
        val rows = mutableListOf<ContentRow>()

        fun fetch(call: Call<TmdbResponse>, title: String) {
            call.enqueue(object : Callback<TmdbResponse> {
                override fun onResponse(call: Call<TmdbResponse>, response: Response<TmdbResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.results?.let { allItems ->
                            val items = allItems.filter { !it.posterPath.isNullOrEmpty() }
                            if (items.isNotEmpty()) {
                                rows.add(ContentRow(title, items))
                                contentRows = rows.toList()
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {}
            })
        }

        fetch(tmdbService.getPopularTv(apiKey), "Popular Series")
        fetch(tmdbService.getTopRatedTv(apiKey), "Top Rated")
        fetch(tmdbService.getOnTheAirTv(apiKey), "On The Air")
        fetch(tmdbService.getAiringTodayTv(apiKey), "Airing Today")
        // Genre-based TV: Action=10759, Comedy=35, Drama=18, SciFi=10765, Crime=80, Mystery=9648, Reality=10764, Documentary=99
        fetch(tmdbService.discoverTv(apiKey, "10759", "popularity.desc"), "Action & Adventure")
        fetch(tmdbService.discoverTv(apiKey, "35", "popularity.desc"), "Comedy")
        fetch(tmdbService.discoverTv(apiKey, "18", "popularity.desc"), "Drama")
        fetch(tmdbService.discoverTv(apiKey, "10765", "popularity.desc"), "Sci-Fi & Fantasy")
        fetch(tmdbService.discoverTv(apiKey, "80", "popularity.desc"), "Crime")
        fetch(tmdbService.discoverTv(apiKey, "9648", "popularity.desc"), "Mystery")
        fetch(tmdbService.discoverTv(apiKey, "10764", "popularity.desc"), "Reality")
        fetch(tmdbService.discoverTv(apiKey, "99", "popularity.desc"), "Documentaries")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A20), Color(0xFF050510), Color(0xFF050514))
                )
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp)
        ) {
            item {
                Text(
                    text = "TV Series",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFF0F0FF), Color(0xFF00D4FF).copy(alpha = 0.8f))
                        )
                    ),
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 16.dp)
                )
            }

            items(
                count = contentRows.size,
                key = { contentRows[it].title }
            ) { index ->
                ContentFaceRow(row = contentRows[index], onMediaClick = onMediaClick)
            }
        }
    }
}
