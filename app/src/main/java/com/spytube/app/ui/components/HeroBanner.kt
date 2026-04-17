package com.spytube.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.mutableStateOf
import com.spytube.app.R
import com.spytube.app.api.ApiClient
import com.spytube.app.models.ImagesResponse
import com.spytube.app.models.MediaItem
import com.spytube.app.ui.theme.LocalSpyTubeColors
import com.spytube.app.ui.theme.White
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


import androidx.compose.ui.draw.blur
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.spytube.app.ui.components.safeLayerBackdrop
import com.spytube.app.ui.components.safeGlass

val sfProDisplayFamily = FontFamily(
    Font(R.font.sf_pro_display_black, FontWeight.Black),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold)
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(
    items: List<MediaItem>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPlayClick: (MediaItem) -> Unit,
    onInfoClick: (MediaItem) -> Unit
) {
    val configuration = LocalConfiguration.current
    val heroHeight = (configuration.screenHeightDp * 0.75f).dp

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(Color.DarkGray)
        )
        return
    }

    // Auto-slide every 5 seconds
    LaunchedEffect(pagerState, items.size) {
        if (items.size > 1) {
            while (true) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val themeColors = LocalSpyTubeColors.current
    val context = LocalContext.current

    // Pre-fetch ALL logos upfront for every banner item to eliminate per-page delay
    val logoCache = remember { mutableStateOf(mutableMapOf<Int, String?>()) }
    val checkedIds = remember { mutableStateOf(mutableSetOf<Int>()) }

    LaunchedEffect(items) {
        items.forEach { item ->
            if (item.id !in checkedIds.value) {
                val type = if (item.isTv) "tv" else "movie"
                ApiClient.getTmdbService().getMediaImages(type, item.id, ApiClient.getApiKey(context)).enqueue(object : Callback<ImagesResponse> {
                    override fun onResponse(call: Call<ImagesResponse>, response: Response<ImagesResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val logos = response.body()!!.logos
                            if (logos != null && logos.isNotEmpty()) {
                                val bestLogo = logos.firstOrNull { "en" == it.language } ?: logos[0]
                                logoCache.value = logoCache.value.toMutableMap().apply {
                                    put(item.id, "https://image.tmdb.org/t/p/original" + bestLogo.filePath)
                                }
                            }
                        }
                        checkedIds.value = checkedIds.value.toMutableSet().apply { add(item.id) }
                    }
                    override fun onFailure(call: Call<ImagesResponse>, t: Throwable) {
                        checkedIds.value = checkedIds.value.toMutableSet().apply { add(item.id) }
                    }
                })
            }
        }
    }

    val heroBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxWidth().height(heroHeight)) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
                .safeLayerBackdrop(heroBackdrop as com.kyant.backdrop.backdrops.LayerBackdrop),
            beyondViewportPageCount = 1 // Pre-load adjacent pages for smooth scroll
        ) { page ->
            val item = items[page]
            val context = LocalContext.current
            // Full quality hero image with caching
            val imageModel = remember(item.backdropPath) {
                ImageRequest.Builder(context)
                    .data("https://image.tmdb.org/t/p/original" + item.backdropPath)
                    .crossfade(300)
                    .memoryCacheKey("hero_" + item.id)
                    .build()
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = true } // Hardware layer for smooth pager
            ) {
                // Background Image
                Image(
                    painter = rememberAsyncImagePainter(model = imageModel),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.3f to Color.Black.copy(alpha = 0.2f),
                        0.6f to Color.Black.copy(alpha = 0.5f),
                        0.85f to Color.Black.copy(alpha = 0.8f),
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        // Floating Foreground Content (Title, Meta, Buttons, Dots)
        val currentItem = items[pagerState.currentPage]
        
        // Lookup logo from pre-fetched cache
        val cachedLogoUrl = logoCache.value[currentItem.id]
        val hasChecked = currentItem.id in checkedIds.value
        
        // Smooth fade-in animation for the title area
        val titleAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (cachedLogoUrl != null || (hasChecked && cachedLogoUrl == null)) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 500,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "titleFade"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(bottom = 8.dp)
                    .graphicsLayer { alpha = titleAlpha },
                contentAlignment = Alignment.Center
            ) {
                if (cachedLogoUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(cachedLogoUrl)
                                .crossfade(400)
                                .memoryCacheKey("logo_${currentItem.id}")
                                .build()
                        ),
                        contentDescription = currentItem.displayTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    )
                }

                // Show text fallback ONLY if we confirmed no logo exists
                if (hasChecked && cachedLogoUrl == null) {
                    Text(
                        text = currentItem.displayTitle ?: "",
                        color = White,
                        fontSize = 44.sp,
                        fontFamily = sfProDisplayFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 46.sp,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Apple TV metadata centered
            Text(
                text = currentItem.getMediaTypeLabel() + " · " + currentItem.year,
                color = Color(0xFFCCCCCC),
                fontFamily = sfProDisplayFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Apple TV centered floating Play + Add buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(44.dp)
                        .width(160.dp)
                        .safeGlass(
                            backdrop = heroBackdrop,
                            shape = RoundedCornerShape(24.dp),
                            surfaceColor = Color(0xFF1C1C1E).copy(alpha = 0.38f),
                            blurRadius = 16.dp,
                            lensAmount = 24.dp
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onPlayClick(currentItem) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // "+" Add Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .safeGlass(
                            backdrop = heroBackdrop,
                            shape = CircleShape,
                            surfaceColor = Color(0xFF1C1C1E).copy(alpha = 0.38f),
                            blurRadius = 16.dp,
                            lensAmount = 24.dp
                        )
                        .clip(CircleShape)
                        .clickable { onInfoClick(currentItem) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pagination dots (Exact Apple TV style)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isActive = index == pagerState.currentPage
                    
                    // Animate dot width for smooth Apple TV transitions
                    val dotWidth by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isActive) 24.dp else 6.dp,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isActive)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}
