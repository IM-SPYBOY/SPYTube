package com.spytube.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spytube.app.models.FavoritesManager
import com.spytube.app.models.MediaItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassMediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Skip rendering if no poster image available
    if (mediaItem.posterPath.isNullOrEmpty()) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.375f, stiffness = 400f),
        label = "bounce"
    )
    // Animated elevation + border glow on press
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 4.dp,
        animationSpec = spring(stiffness = 400f), label = "elev"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 0.1f,
        animationSpec = spring(stiffness = 400f), label = "border"
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .padding(end = 12.dp, bottom = 12.dp)
            .graphicsLayer {
                clip = true
                scaleX = scale.value
                scaleY = scale.value
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onClick() },
                onLongClick = {
                    val added = FavoritesManager.toggleFavorite(context, mediaItem)
                    val msg = if (added) "Added to My List" else "Removed from My List"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        // Poster Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .shadow(elevation = elevation, shape = RoundedCornerShape(12.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(12.dp))
                .refractiveEdge(cornerRadius = 12.dp)
        ) {
            // Full quality poster with caching
            val imageModel = remember(mediaItem.posterPath) {
                ImageRequest.Builder(context)
                    .data("https://image.tmdb.org/t/p/w500" + mediaItem.posterPath)
                    .crossfade(true)
                    .memoryCacheKey("poster_" + mediaItem.id)
                    .build()
            }

            AsyncImage(
                model = imageModel,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay at bottom of poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x80000000))
                        )
                    )
            )
        }

        // Rating & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        ) {
            Text(
                text = "★",
                color = Color(0xFFFFD700),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = mediaItem.rating,
                color = Color(0xFFF0F0FF), // Bright white rating
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.1).sp
            )
            Text(
                text = " • ${mediaItem.year}",
                color = Color(0xFFB3B3CC), // Apple muted secondary
                fontSize = 12.sp,
                letterSpacing = (-0.1).sp
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Top10NumberedCard(
    mediaItem: MediaItem,
    rank: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Skip rendering if no poster image available
    if (mediaItem.posterPath.isNullOrEmpty()) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.375f, stiffness = 400f),
        label = "bounce"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 4.dp,
        animationSpec = spring(stiffness = 400f), label = "elev"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 0.1f,
        animationSpec = spring(stiffness = 400f), label = "border"
    )

    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(end = 12.dp, bottom = 4.dp)
            .graphicsLayer {
                clip = true
                scaleX = scale.value
                scaleY = scale.value
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onClick() },
                onLongClick = {
                    val added = FavoritesManager.toggleFavorite(context, mediaItem)
                    val msg = if (added) "Added to My List" else "Removed from My List"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        // Poster with number INSIDE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .shadow(elevation = elevation, shape = RoundedCornerShape(12.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(12.dp))
                .refractiveEdge(cornerRadius = 14.dp)
        ) {
            val imageModel = remember(mediaItem.posterPath) {
                ImageRequest.Builder(context)
                    .data("https://image.tmdb.org/t/p/w500" + mediaItem.posterPath)
                    .crossfade(true)
                    .memoryCacheKey("poster_" + mediaItem.id)
                    .build()
            }

            AsyncImage(
                model = imageModel,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Bottom gradient for title text contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Text(
                text = rank.toString(),
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 2f),
                        blurRadius = 6f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 8.dp)
            )

            // Title inside card at bottom-start (Apple TV exact style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = mediaItem.displayTitle?.take(18) ?: "",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }

        // Rating & Year below card (matching regular cards)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        ) {
            Text(
                text = "★",
                color = Color(0xFFFFD700),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = mediaItem.rating,
                color = Color(0xFFF0F0FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.1).sp
            )
            Text(
                text = " • ${mediaItem.year}",
                color = Color(0xFFB3B3CC),
                fontSize = 12.sp,
                letterSpacing = (-0.1).sp
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    if (mediaItem.backdropPath.isNullOrEmpty() && mediaItem.posterPath.isNullOrEmpty()) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.375f, stiffness = 400f),
        label = "cwBounce"
    )

    Column(
        modifier = Modifier
            .width(200.dp)
            .padding(end = 12.dp, bottom = 8.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onClick() },
                onLongClick = {
                    val added = FavoritesManager.toggleFavorite(context, mediaItem)
                    val msg = if (added) "Added to My List" else "Removed from My List"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        // Landscape backdrop container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(12.dp))
                .refractiveEdge(cornerRadius = 12.dp, strokeWidth = 0.5.dp)
        ) {
            val imgPath = mediaItem.backdropPath ?: mediaItem.posterPath
            val imageModel = remember(imgPath) {
                ImageRequest.Builder(context)
                    .data("https://image.tmdb.org/t/p/w780" + imgPath)
                    .crossfade(true)
                    .memoryCacheKey("cw_" + mediaItem.id)
                    .build()
            }

            AsyncImage(
                model = imageModel,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient for title contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .refractiveEdge(isCircle = true, strokeWidth = 1.5.dp, peakAlpha = 0.7f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onPlayClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // Title overlay at bottom
            Text(
                text = mediaItem.displayTitle?.take(22) ?: "",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )

            // Progress bar at very bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f) // 35% default progress
                        .fillMaxHeight()
                        .background(Color(0xFFE50914)) // Netflix/Apple red
                )
            }
        }
    }
}
