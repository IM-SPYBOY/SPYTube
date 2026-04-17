package com.spytube.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.spytube.app.ui.components.safeLayerBackdrop
import com.spytube.app.api.ApiClient
import com.spytube.app.models.ContentPreloader
import com.spytube.app.models.ContentRow
import com.spytube.app.models.MediaItem
import com.spytube.app.models.TmdbResponse
import com.spytube.app.models.WatchHistoryManager
import com.spytube.app.ui.components.GlassMediaCard
import com.spytube.app.ui.components.Top10NumberedCard
import com.spytube.app.ui.components.ContinueWatchingCard
import com.spytube.app.ui.components.GlassTopBar
import com.spytube.app.ui.components.HeroBanner
import com.spytube.app.ui.theme.LocalSpyTubeColors
import com.spytube.app.ui.theme.White
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@Composable
fun MainScreen(
    backdrop: com.kyant.backdrop.Backdrop,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit
) {
    var contentRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    var heroItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var watchHistory by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    // Refresh watch history live on every resume (returning from player/detail)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                watchHistory = WatchHistoryManager.getHistory(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val tmdbService = ApiClient.getTmdbService()
        val apiKey = ApiClient.getApiKey(context)

        // Use preloaded data if available (from splash screen), otherwise fetch fresh
        if (ContentPreloader.isLoaded) {
            val rows = mutableListOf<ContentRow>()
            ContentPreloader.trendingItems?.let {
                val withPoster = it.filter { item -> !item.posterPath.isNullOrEmpty() }
                if (withPoster.isNotEmpty()) {
                    heroItems = withPoster.filter { item -> !item.backdropPath.isNullOrEmpty() }.shuffled().take(5)
                    rows.add(ContentRow("Trending Now", withPoster))
                }
            }
            ContentPreloader.popularMovies?.let {
                val withPoster = it.filter { item -> !item.posterPath.isNullOrEmpty() }
                if (withPoster.isNotEmpty()) rows.add(ContentRow("Popular Movies", withPoster))
            }
            ContentPreloader.popularTv?.let {
                val withPoster = it.filter { item -> !item.posterPath.isNullOrEmpty() }
                if (withPoster.isNotEmpty()) rows.add(ContentRow("Popular TV Shows", withPoster))
            }
            ContentPreloader.topRatedMovies?.let {
                val withPoster = it.filter { item -> !item.posterPath.isNullOrEmpty() }
                if (withPoster.isNotEmpty()) rows.add(ContentRow("Top Rated Movies", withPoster))
            }
            ContentPreloader.nowPlaying?.let {
                val withPoster = it.filter { item -> !item.posterPath.isNullOrEmpty() }
                if (withPoster.isNotEmpty()) rows.add(ContentRow("New Releases", withPoster))
            }
            contentRows = rows

            // Fetch additional categories dynamically
            // Using contentRows += to avoid race condition with shared mutable list
            fun fetchExtra(call: Call<TmdbResponse>, title: String) {
                call.enqueue(object : Callback<TmdbResponse> {
                    override fun onResponse(call: Call<TmdbResponse>, response: Response<TmdbResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.results?.let { allItems ->
                                val items = allItems.filter { !it.posterPath.isNullOrEmpty() }
                                if (items.isNotEmpty()) {
                                    contentRows = contentRows + ContentRow(title, items)
                                }
                            }
                        }
                    }
                    override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {}
                })
            }


            fetchExtra(tmdbService.getUpcomingMovies(apiKey), "Coming Soon")
            fetchExtra(tmdbService.getAiringTodayTv(apiKey), "Airing Today")
            fetchExtra(tmdbService.getOnTheAirTv(apiKey), "On The Air")
            // Genre-based: Action=28, Comedy=35, Horror=27, SciFi=878, Romance=10749, Thriller=53
            fetchExtra(tmdbService.discoverMovies(apiKey, "28", "popularity.desc"), "Action & Adventure")
            fetchExtra(tmdbService.discoverMovies(apiKey, "35", "popularity.desc"), "Comedy")
            fetchExtra(tmdbService.discoverMovies(apiKey, "27", "popularity.desc"), "Horror")
            fetchExtra(tmdbService.discoverMovies(apiKey, "878", "popularity.desc"), "Sci-Fi")
            fetchExtra(tmdbService.discoverMovies(apiKey, "10749", "popularity.desc"), "Romance")
            fetchExtra(tmdbService.discoverMovies(apiKey, "53", "popularity.desc"), "Thriller")
            fetchExtra(tmdbService.discoverMovies(apiKey, "99", "popularity.desc"), "Documentaries")
        } else {
            // Fallback: fresh fetch if preloader didn't run
            fun fetch(call: Call<TmdbResponse>, title: String, isHeroCandidate: Boolean = false) {
                call.enqueue(object : Callback<TmdbResponse> {
                    override fun onResponse(call: Call<TmdbResponse>, response: Response<TmdbResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.results?.let { allItems ->
                                val items = allItems.filter { !it.posterPath.isNullOrEmpty() }
                                if (items.isNotEmpty()) {
                                    if (isHeroCandidate && heroItems.isEmpty()) {
                                        heroItems = items.filter { !it.backdropPath.isNullOrEmpty() }.shuffled().take(5)
                                    }
                                    contentRows = contentRows + ContentRow(title, items)
                                }
                            }
                        }
                    }
                    override fun onFailure(call: Call<TmdbResponse>, t: Throwable) {}
                })
            }

            fetch(tmdbService.getTrending(apiKey), "Trending Now", true)
            fetch(tmdbService.getPopularMovies(apiKey), "Popular Movies")
            fetch(tmdbService.getPopularTv(apiKey), "Popular TV Shows")
            fetch(tmdbService.getTopRatedMovies(apiKey), "Top Rated Movies")
            fetch(tmdbService.getNowPlayingMovies(apiKey), "New Releases")
            fetch(tmdbService.getTrendingWeek(apiKey), "Trending This Week")
            fetch(tmdbService.getUpcomingMovies(apiKey), "Coming Soon")
            fetch(tmdbService.getAiringTodayTv(apiKey), "Airing Today")
            fetch(tmdbService.getOnTheAirTv(apiKey), "On The Air")
            fetch(tmdbService.discoverMovies(apiKey, "28", "popularity.desc"), "Action & Adventure")
            fetch(tmdbService.discoverMovies(apiKey, "35", "popularity.desc"), "Comedy")
            fetch(tmdbService.discoverMovies(apiKey, "27", "popularity.desc"), "Horror")
            fetch(tmdbService.discoverMovies(apiKey, "878", "popularity.desc"), "Sci-Fi")
            fetch(tmdbService.discoverMovies(apiKey, "10749", "popularity.desc"), "Romance")
            fetch(tmdbService.discoverMovies(apiKey, "53", "popularity.desc"), "Thriller")
            fetch(tmdbService.discoverMovies(apiKey, "99", "popularity.desc"), "Documentaries")
        }
    }

    // Shared pager state for hero banner
    val heroPagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { heroItems.size.coerceAtLeast(1) })

    val themeColors = LocalSpyTubeColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Content inside backdrop capture area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeLayerBackdrop(backdrop as com.kyant.backdrop.backdrops.LayerBackdrop) // Outer: Capture everything drawn inside
                .background(themeColors.bgGradient) // Inner: Draw background inside capture
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                // Hero Banner
                item(key = "hero") {
                    Box {
                        HeroBanner(
                            items = heroItems,
                            pagerState = heroPagerState,
                            onPlayClick = { item -> 
                                val intent = android.content.Intent(context, com.spytube.app.CinefyPlayerActivity::class.java)
                                val itemTitle = item.title ?: item.name ?: ""
                                intent.putExtra("searchTitle", itemTitle)
                                intent.putExtra("isTv", item.isTv())
                                if (item.isTv()) {
                                    intent.putExtra("season", 1)
                                    intent.putExtra("episode", 1)
                                }
                                intent.putExtra("resumePosition", com.spytube.app.models.CinefyCache.getPosition(context, itemTitle))
                                intent.putExtra("media", item)
                                context.startActivity(intent)
                            },
                            onInfoClick = { item -> onMediaClick(item) }
                        )
                    }
                }
                if (watchHistory.isNotEmpty()) {
                    item(key = "continue_watching") {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Continue Watching",
                                    color = Color(0xFFF0F0FF),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.4).sp
                                )
                                Text(
                                    text = "See All",
                                    color = Color(0xFF2997FF), // Apple bright blue
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.2).sp,
                                    modifier = Modifier.clickable {
                                        val intent = android.content.Intent(context, com.spytube.app.SeeAllActivity::class.java)
                                        intent.putExtra("title", "Continue Watching")
                                        intent.putExtra("items", java.util.ArrayList(watchHistory))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                items(
                                    items = watchHistory,
                                    key = { "cw_${it.id}" }
                                ) { item ->
                                    ContinueWatchingCard(
                                        mediaItem = item,
                                        onClick = { onMediaClick(item) },
                                        onPlayClick = {
                                            val itemTitle = item.title ?: item.name ?: ""
                                            val cached = com.spytube.app.models.CinefyCache.lookup(context, itemTitle)
                                            val resumePos = com.spytube.app.models.CinefyCache.getPosition(context, itemTitle)
                                            val pi = android.content.Intent(context, com.spytube.app.CinefyPlayerActivity::class.java)
                                            pi.putExtra("searchTitle", itemTitle)
                                            if (cached != null) {
                                                pi.putExtra("isTv", cached["isTv"] as? Boolean ?: false)
                                                pi.putExtra("season", (cached["season"] as? Int) ?: 0)
                                                pi.putExtra("episode", (cached["episode"] as? Int) ?: 0)
                                            } else {
                                                pi.putExtra("isTv", item.isTv())
                                            }
                                            pi.putExtra("resumePosition", resumePos)
                                            pi.putExtra("media", item)
                                            context.startActivity(pi)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Top 10 rows first (Popular Movies/TV) ──────
                val top10Rows = contentRows.filter { it.title == "Popular Movies" || it.title == "Popular TV Shows" }
                if (top10Rows.isNotEmpty()) {
                    items(
                        count = top10Rows.size,
                        key = { "top10_${top10Rows[it].title}" }
                    ) { index ->
                        ContentFaceRow(row = top10Rows[index], onMediaClick = onMediaClick)
                    }
                }


                // ── Remaining TMDB content rows (skip Top 10 + duplicates) ──────
                val skipTitles = setOf("Popular Movies", "Popular TV Shows", "Trending This Week")
                val remainingRows = contentRows.filter { it.title !in skipTitles }
                if (remainingRows.isEmpty() && contentRows.isEmpty()) {
                    // Show shimmer skeleton while loading
                    items(4) {
                        com.spytube.app.ui.components.ShimmerRow()
                    }
                } else {
                    items(
                        count = remainingRows.size,
                        key = { remainingRows[it].title }
                    ) { index ->
                        ContentFaceRow(row = remainingRows[index], onMediaClick = onMediaClick)
                    }
                }
            }
        }
    }
}


@Composable
fun ContentFaceRow(
    row: ContentRow,
    onMediaClick: (MediaItem) -> Unit
) {
    // Determine if this row should use the Apple TV Numbered style
    val isTop10Row = row.title == "Popular Movies" || row.title == "Popular TV Shows"
    val displayTitle = if (isTop10Row) {
        if (row.title.contains("Movies")) "Top 10 Movies" else "Top 10 TV Shows"
    } else {
        row.title
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayTitle,
                color = Color(0xFFF0F0FF),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "See All",
                color = Color(0xFF2997FF), // Apple bright blue
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.clickable {
                    val intent = android.content.Intent(context, com.spytube.app.SeeAllActivity::class.java)
                    intent.putExtra("title", displayTitle)
                    intent.putExtra("items", java.util.ArrayList(row.items))
                    context.startActivity(intent)
                }
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(
                items = if (isTop10Row) row.items.take(10) else row.items,
                key = { it.id }
            ) { item ->
                if (isTop10Row) {
                    Top10NumberedCard(
                        mediaItem = item,
                        rank = row.items.indexOf(item) + 1,
                        onClick = { onMediaClick(item) }
                    )
                } else {
                    GlassMediaCard(mediaItem = item, onClick = { onMediaClick(item) })
                }
            }
        }
    }
}


