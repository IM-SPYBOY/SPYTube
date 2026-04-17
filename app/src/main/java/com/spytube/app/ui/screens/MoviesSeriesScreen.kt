package com.spytube.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spytube.app.ui.theme.frostedGlass
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun MoviesSeriesScreen(
    onMediaClick: (MediaItem) -> Unit
) {
    // 0 = Movies, 1 = Series, 2 = Anime
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    var movieRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    var seriesRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    var animeRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Load movies
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
                                movieRows = rows.toList()
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {}
            })
        }

        fetch(tmdbService.getPopularMovies(apiKey), "Popular Movies")
        fetch(tmdbService.getTopRatedMovies(apiKey), "Top Rated")
        fetch(tmdbService.getNowPlayingMovies(apiKey), "Now Playing")
        fetch(tmdbService.getUpcomingMovies(apiKey), "Upcoming")
        fetch(tmdbService.discoverMovies(apiKey, "28", "popularity.desc"), "Action & Adventure")
        fetch(tmdbService.discoverMovies(apiKey, "35", "popularity.desc"), "Comedy")
        fetch(tmdbService.discoverMovies(apiKey, "27", "popularity.desc"), "Horror")
        fetch(tmdbService.discoverMovies(apiKey, "878", "popularity.desc"), "Sci-Fi")
        fetch(tmdbService.discoverMovies(apiKey, "10749", "popularity.desc"), "Romance")
        fetch(tmdbService.discoverMovies(apiKey, "18", "popularity.desc"), "Drama")
        fetch(tmdbService.discoverMovies(apiKey, "53", "popularity.desc"), "Thriller")
        fetch(tmdbService.discoverMovies(apiKey, "80", "popularity.desc"), "Crime")
        fetch(tmdbService.discoverMovies(apiKey, "99", "popularity.desc"), "Documentaries")
        fetch(tmdbService.discoverMovies(apiKey, "10751", "popularity.desc"), "Family")
    }

    // Load series
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
                                seriesRows = rows.toList()
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
        fetch(tmdbService.discoverTv(apiKey, "10759", "popularity.desc"), "Action & Adventure")
        fetch(tmdbService.discoverTv(apiKey, "35", "popularity.desc"), "Comedy")
        fetch(tmdbService.discoverTv(apiKey, "18", "popularity.desc"), "Drama")
        fetch(tmdbService.discoverTv(apiKey, "10765", "popularity.desc"), "Sci-Fi & Fantasy")
        fetch(tmdbService.discoverTv(apiKey, "80", "popularity.desc"), "Crime")
        fetch(tmdbService.discoverTv(apiKey, "9648", "popularity.desc"), "Mystery")
        fetch(tmdbService.discoverTv(apiKey, "10764", "popularity.desc"), "Reality")
        fetch(tmdbService.discoverTv(apiKey, "99", "popularity.desc"), "Documentaries")
    }

    // Load Anime
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
                                animeRows = rows.toList()
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {}
            })
        }

        fetch(tmdbService.discoverTv(apiKey, "16", "popularity.desc"), "Popular Anime Series")
        fetch(tmdbService.discoverMovies(apiKey, "16", "popularity.desc"), "Popular Anime Movies")
        fetch(tmdbService.discoverTv(apiKey, "16", "vote_average.desc"), "Top Rated TV")
        fetch(tmdbService.discoverMovies(apiKey, "16", "vote_average.desc"), "Top Rated Movies")
        fetch(tmdbService.discoverTv(apiKey, "16,10759", "popularity.desc"), "Action Anime")
        fetch(tmdbService.discoverTv(apiKey, "16,35", "popularity.desc"), "Comedy Anime")
        fetch(tmdbService.discoverTv(apiKey, "16,10765", "popularity.desc"), "Sci-Fi Anime")
        fetch(tmdbService.discoverMovies(apiKey, "16,28", "popularity.desc"), "Action Anime Movies")
    }

    val contentRows = when (selectedTabIndex) {
        0 -> movieRows
        1 -> seriesRows
        else -> animeRows
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
            contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp)
        ) {
            // Glass toggle
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .frostedGlass(RoundedCornerShape(26.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleTab("Movies", selectedTabIndex == 0) { selectedTabIndex = 0 }
                        ToggleTab("Series", selectedTabIndex == 1) { selectedTabIndex = 1 }
                        ToggleTab("Anime", selectedTabIndex == 2) { selectedTabIndex = 2 }
                    }
                }
            }

            // Content rows with crossfade transition
            item {
                androidx.compose.animation.Crossfade(
                    targetState = selectedTabIndex,
                    animationSpec = androidx.compose.animation.core.tween(200)
                ) { tabIndex ->
                    val rows = when (tabIndex) {
                        0 -> movieRows
                        1 -> seriesRows
                        else -> animeRows
                    }
                    Column {
                        rows.forEach { row ->
                            ContentFaceRow(row = row, onMediaClick = onMediaClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFFE50914) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(250)
    )
    val borderColor by animateColorAsState(
        if (isSelected) Color(0xFFE50914).copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(250)
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(0.5.dp, borderColor, RoundedCornerShape(25.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 10.dp), // slightly reduced horizontal padding for 3 tabs
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}
