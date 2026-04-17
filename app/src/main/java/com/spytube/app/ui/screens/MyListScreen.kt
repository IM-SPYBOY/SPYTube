package com.spytube.app.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spytube.app.models.FavoritesManager
import com.spytube.app.models.MediaItem

// Premium palette
private val BgDeep = Color(0xFF050510)
private val SurfaceGlass = Color(0xFF12122A)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentRed = Color(0xFFE50914)
private val GlassBorder = Color(0x1AFFFFFF)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextSecondary = Color(0xB3F0F0FF)
private val TextMuted = Color(0x66F0F0FF)

@Composable
fun MyListScreen(
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoritesManager.getList(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A20), BgDeep, Color(0xFF050514))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Title with gradient text
            Text(
                text = "My List",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(TextPrimary, AccentCyan.copy(alpha = 0.8f))
                    )
                ),
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 16.dp)
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceGlass)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = AccentRed.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your list is empty",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Long press any title to add it here",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMediaClick(item) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 0.5.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x30FFFFFF),
                                                Color(0x08FFFFFF)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data("https://image.tmdb.org/t/p/w342" + item.posterPath)
                                        .crossfade(300)
                                        .build(),
                                    contentDescription = item.displayTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Gradient overlay at bottom
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0x80000000))
                                            )
                                        )
                                )

                                // Remove button with glass bg
                                IconButton(
                                    onClick = {
                                        FavoritesManager.removeFromList(context, item.id)
                                        favorites = FavoritesManager.getList(context)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x80000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkRemove,
                                        contentDescription = "Remove",
                                        tint = AccentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.displayTitle ?: "",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
