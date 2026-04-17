package com.spytube.app.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.spytube.app.R
import com.spytube.app.api.ApiClient
import com.spytube.app.models.MediaItem
import com.spytube.app.ui.components.safeGlass
import com.spytube.app.ui.components.safeLayerBackdrop
import com.spytube.app.ui.theme.LocalSpyTubeColors
import com.spytube.app.ui.theme.NetflixRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    backdrop: Backdrop,
    onMediaClick: (MediaItem) -> Unit
) {
    val extColors = LocalSpyTubeColors.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    fun performSearch(text: String) {
        if (text.length < 2) {
            results = emptyList()
            hasSearched = false
            return
        }

        scope.launch {
            try {
                isLoading = true
                val items = withContext(Dispatchers.IO) {
                    val tmdbService = ApiClient.getTmdbService()
                    val apiKey = ApiClient.getApiKey(context)
                    val response = tmdbService.searchMulti(apiKey, text).execute()

                    if (response.isSuccessful) {
                        response.body()?.results?.filter {
                            it.posterPath != null && (it.mediaType == "movie" || it.mediaType == "tv")
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                results = items
                hasSearched = true
                isLoading = false
            } catch (e: Exception) {
                Log.e("SearchScreen", "Search error", e)
                isLoading = false
                hasSearched = true
            }
        }
    }

    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(500)
            performSearch(query)
        } else {
            results = emptyList()
            hasSearched = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ── Body ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeLayerBackdrop(backdrop as LayerBackdrop)
                .background(extColors.bgGradient)
        ) {
            val state = when {
                isLoading && results.isEmpty() -> SearchState.LOADING
                results.isNotEmpty() -> SearchState.RESULTS
                hasSearched && results.isEmpty() -> SearchState.EMPTY
                else -> SearchState.IDLE
            }

            when (state) {
                SearchState.IDLE -> IdleContent(extColors = extColors)
                SearchState.LOADING -> LoadingContent(extColors)
                SearchState.RESULTS -> ResultsList(results, extColors, onMediaClick)
                SearchState.EMPTY -> EmptyContent(query, extColors)
            }
        }

        // ── Bottom search bar (glass) ─────────────────────────────────
        BottomSearchBar(
            query = query,
            onQueryChange = { query = it },
            onClear = {
                query = ""
                results = emptyList()
                hasSearched = false
            },
            isLoading = isLoading,
            focusRequester = focusRequester,
            extColors = extColors,
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private enum class SearchState { IDLE, LOADING, RESULTS, EMPTY }

// ══════════════════════════════════════════════════════════════════════════════
// ── Bottom search bar (Netflix-style with reddish border) ────────────────────
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun BottomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    // Detect keyboard visibility
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    val glassShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isKeyboardVisible) {
                    // When keyboard is open: sit right above the keyboard
                    Modifier
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    // When keyboard is closed: clear the glass bottom nav
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 12.dp)
                        .padding(bottom = 72.dp)
                }
            )
            .height(52.dp)
            .shadow(
                elevation = 20.dp,
                shape = glassShape,
                ambientColor = Color.Transparent,
                spotColor = Color.Black.copy(alpha = 0.55f)
            )
            .safeGlass(
                backdrop = backdrop,
                shape = glassShape,
                surfaceColor = Color(0xFF1C1C1E).copy(alpha = 0.38f),
                blurRadius = 16.dp,
                lensAmount = 24.dp
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), glassShape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp)
        ) {

            // ── Text field ───────────────────────────────────────────
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = extColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                cursorBrush = SolidColor(NetflixRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* debounce handles it */ }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search movies & TV shows…",
                                color = extColors.textMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ── Trailing icon: Clear / loading / sparkle ─────────────
            if (query.isNotEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                    exit = fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = NetflixRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = extColors.textMuted,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onClear() }
                        )
                    }
                }
            } else {
                // Sparkle/AI icon when idle
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = "AI Search",
                    tint = extColors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── Idle: clean landing with just title, subtitle, toggle ────────────────────
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun IdleContent(
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Title ───────────────────────────────────────────────────
        Text(
            text = "Search",
            color = extColors.textPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Subtitle ────────────────────────────────────────────────
        Text(
            text = "Find movies & TV shows",
            color = extColors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── Loading ─────────────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoadingContent(extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(extColors.glassSurface.copy(alpha = shimmerAlpha))
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Title placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(extColors.glassSurface.copy(alpha = shimmerAlpha))
                    )
                    Spacer(Modifier.height(8.dp))
                    // Subtitle placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(extColors.glassSurface.copy(alpha = shimmerAlpha))
                    )
                    Spacer(Modifier.height(6.dp))
                    // Rating placeholder
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(extColors.glassSurface.copy(alpha = shimmerAlpha))
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── Results list ────────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ResultsList(
    results: List<MediaItem>,
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors,
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Dismiss keyboard when scrolling
    if (listState.isScrollInProgress) {
        focusManager.clearFocus()
    }

    // Split results by type
    val movies = results.filter { !it.isTv }
    val tvShows = results.filter { it.isTv }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 158.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Results count
        item {
            Text(
                text = "${results.size} result${if (results.size != 1) "s" else ""} found",
                color = extColors.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
            )
        }

        // Movies section (if present)
        if (movies.isNotEmpty()) {
            item {
                SectionLabel("Movies", movies.size, extColors)
            }
            items(movies.size) { index ->
                SearchResultCard(
                    item = movies[index],
                    extColors = extColors,
                    context = context,
                    onClick = { onMediaClick(movies[index]) }
                )
            }
        }

        // TV section (if present)
        if (tvShows.isNotEmpty()) {
            item {
                SectionLabel("TV Shows", tvShows.size, extColors)
            }
            items(tvShows.size) { index ->
                SearchResultCard(
                    item = tvShows[index],
                    extColors = extColors,
                    context = context,
                    onClick = { onMediaClick(tvShows[index]) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 16.dp)
    ) {
        Text(
            text = title,
            color = extColors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NetflixRed.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                color = NetflixRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    item: MediaItem,
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors,
    context: android.content.Context,
    onClick: () -> Unit
) {
    val backdropUrl = item.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
        ?: item.posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Landscape thumbnail
        Box(
            modifier = Modifier
                .width(136.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(extColors.glassSurface)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
            // Media type badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (item.isTv) Color(0xFF6C5CE7).copy(alpha = 0.85f)
                        else NetflixRed.copy(alpha = 0.85f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (item.isTv) "TV" else "FILM",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle ?: "Unknown",
                color = extColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(4.dp))

            // Year + type row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.year ?: "N/A",
                    color = extColors.textSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = " · ",
                    color = extColors.textMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = if (item.isTv) "TV Series" else "Movie",
                    color = extColors.textSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = item.rating,
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── Empty state ─────────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EmptyContent(
    query: String,
    extColors: com.spytube.app.ui.theme.SpyTubeExtendedColors
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = extColors.textMuted,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "No results found",
                color = extColors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Try a different search term",
                color = extColors.textMuted,
                fontSize = 14.sp
            )
        }
    }
}
