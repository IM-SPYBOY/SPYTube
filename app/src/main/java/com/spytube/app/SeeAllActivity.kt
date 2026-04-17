package com.spytube.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.spytube.app.models.MediaItem


class SeeAllActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val title = intent.getStringExtra("title") ?: "All Items"
        val items = try {
            intent.getSerializableExtra("items") as? ArrayList<MediaItem> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    SeeAllScreen(
                        title = title,
                        items = items,
                        onBack = { finish() },
                        onItemClick = { item ->
                            val i = Intent(this, DetailActivity::class.java)
                            i.putExtra("media", item)
                            startActivity(i)
                        }
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    title: String,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onItemClick: (MediaItem) -> Unit
) {
    val isTop10 = title.contains("Top 10")
    val isContinue = title.contains("Continue Watching") || title.contains("History")
    
    // Everything else defaults to landscape rows similar to Continue Watching but without status
    val useLandscapeRows = !isTop10

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp) // Accounts for bottom nav
        ) {
            itemsIndexed(items) { index, item ->
                if (isTop10) {
                    Top10ListRow(
                        item = item,
                        rank = index + 1,
                        onClick = { onItemClick(item) }
                    )
                } else if (isContinue) {
                    ContinueWatchingListRow(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                } else {
                    // Generic landscape row (like continue watching without the 'Continue' dot)
                    GenericLandscapeListRow(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
                
                // Separator line between rows (Apple TV style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isTop10) 80.dp else 168.dp, end = 16.dp)
                        .height(0.5.dp)
                        .background(Color(0x2AFFFFFF)) // Faint gray line
                )
            }
        }
    }
}

@Composable
fun Top10ListRow(
    item: MediaItem,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large Gray Number
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "$rank",
                color = Color(0xFFA0A0A5), // Apple TV light gray text
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Portrait Poster
        val posterPath = item.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
        AsyncImage(
            model = posterPath,
            contentDescription = item.title,
            modifier = Modifier
                .width(60.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Genre
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle ?: "Unknown",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.isTv) "TV Show" else "Movie", // Simplified genre for list
                color = Color(0xFFA0A0A5),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ContinueWatchingListRow(
    item: MediaItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Landscape Poster
        val backdropPath = item.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } 
            ?: item.posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            
        AsyncImage(
            model = backdropPath,
            contentDescription = item.title,
            modifier = Modifier
                .width(136.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle ?: "Unknown",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Continue", // Mocked status, could be "Next Season · S3"
                color = Color(0xFFA0A0A5),
                fontSize = 14.sp
            )
        }
        
        // Options '...' Icon
        IconButton(onClick = { /* Options menu */ }) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Options",
                tint = Color(0xFFA0A0A5)
            )
        }
    }
}

@Composable
fun GenericLandscapeListRow(
    item: MediaItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Landscape Poster
        val backdropPath = item.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } 
            ?: item.posterPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            
        AsyncImage(
            model = backdropPath,
            contentDescription = item.title,
            modifier = Modifier
                .width(136.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Overview snippet
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle ?: "Unknown",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (item.isTv) "TV Series • ${item.year}" else "Movie • ${item.year}",
                color = Color(0xFFA0A0A5),
                fontSize = 14.sp
            )
        }
    }
}
