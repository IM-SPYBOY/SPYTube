package com.spytube.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.spytube.app.models.HiCineDownloadManager
import kotlinx.coroutines.delay
import java.io.File

// Premium palette
private val BgDeep = Color(0xFF050510)
private val SurfaceGlass = Color(0xFF12122A)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentRed = Color(0xFFE50914)
private val GlassBorder = Color(0x1AFFFFFF)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextSecondary = Color(0xB3F0F0FF)
private val TextMuted = Color(0x66F0F0FF)
private val BadgeBg = Color(0xFF1E1E2E)
private val ProgressBg = Color(0xFF1A1A2E)
private val ProgressSuccess = Color(0xFF46D369)


private data class DownloadItem(
    val downloadId: Long,
    val title: String,
    val quality: String,
    val size: String,
    val fileName: String,
    val timestamp: Long,
    val status: Int,
    val progress: Int,         // 0-100
    val downloadedBytes: Long,
    val totalBytes: Long,
    val localUri: String?,     // File URI for completed downloads
    val posterUrl: String?     // Poster URL for thumbnail
)

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }

    // Refresh download states every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            downloads = loadDownloads(context)
            delay(2000)
        }
    }

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
            // Title
            Text(
                text = "Downloads",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(TextPrimary, AccentCyan.copy(alpha = 0.8f))
                    )
                ),
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 16.dp)
            )

            if (downloads.isEmpty()) {
                // Empty state
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
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = AccentCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No downloads yet",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Active downloads first
                    val (active, rest) = downloads.partition {
                        it.status == DownloadManager.STATUS_RUNNING || it.status == DownloadManager.STATUS_PENDING
                    }
                    val (completed, failed) = rest.partition {
                        it.status == DownloadManager.STATUS_SUCCESSFUL
                    }

                    if (active.isNotEmpty()) {
                        item {
                            Text(
                                "DOWNLOADING",
                                color = AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(active, key = { it.downloadId }) { item ->
                            DownloadCard(
                                item = item,
                                onPlay = null, // Can't play while downloading
                                onRemove = {
                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    dm.remove(item.downloadId)
                                    HiCineDownloadManager.removeDownload(context, item.downloadId)
                                    downloads = loadDownloads(context)
                                }
                            )
                        }
                    }

                    if (completed.isNotEmpty()) {
                        item {
                            Text(
                                "READY TO PLAY",
                                color = ProgressSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(completed, key = { it.downloadId }) { item ->
                            DownloadCard(
                                item = item,
                                onPlay = { playDownloadedFile(context, item) },
                                onRemove = {
                                    deleteDownloadedFile(context, item)
                                    downloads = loadDownloads(context)
                                }
                            )
                        }
                    }

                    if (failed.isNotEmpty()) {
                        item {
                            Text(
                                "FAILED",
                                color = AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(failed, key = { it.downloadId }) { item ->
                            DownloadCard(
                                item = item,
                                onPlay = null,
                                onRemove = {
                                    HiCineDownloadManager.removeDownload(context, item.downloadId)
                                    downloads = loadDownloads(context)
                                }
                            )
                        }
                    }

                    // Bottom padding for nav bar
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onPlay: (() -> Unit)?,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .build()
    }
    val isRunning = item.status == DownloadManager.STATUS_RUNNING
    val isPending = item.status == DownloadManager.STATUS_PENDING
    val isComplete = item.status == DownloadManager.STATUS_SUCCESSFUL
    val isFailed = item.status == DownloadManager.STATUS_FAILED

    // Pulse animation for active downloads
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGlass)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp))
            .then(
                if (isComplete && onPlay != null) {
                    Modifier.clickable(onClick = onPlay)
                } else Modifier
            )
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Status icon / Thumbnail
        Box(
            modifier = Modifier
                .width(106.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isComplete -> AccentRed.copy(alpha = 0.2f)
                        isFailed -> AccentRed.copy(alpha = 0.15f)
                        else -> AccentCyan.copy(alpha = 0.1f)
                    }
                )
                .then(
                    if (isComplete && onPlay != null) {
                        Modifier.clickable(onClick = onPlay)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Image if available
            if (!item.posterUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(item.posterUrl)
                        .crossfade(300)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Dark overlay for visibility similar to DetailActivity
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }

            Icon(
                imageVector = when {
                    isComplete -> Icons.Rounded.PlayArrow
                    isFailed -> Icons.Rounded.Error
                    isPending -> Icons.Rounded.HourglassTop
                    else -> Icons.Rounded.Download
                },
                contentDescription = when {
                    isComplete -> "Play"
                    isFailed -> "Failed"
                    else -> "Downloading"
                },
                tint = when {
                    isComplete -> Color.White.copy(alpha = 0.85f)
                    isFailed -> AccentRed
                    else -> AccentCyan.copy(alpha = if (isRunning) pulseAlpha else 0.7f)
                },
                modifier = Modifier.size(if (isComplete) 32.dp else 28.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            var cleanTitle = item.title
            var seBadge = ""
            
            val m1 = Regex("^S(\\d+)\\s*E(\\d+)\\s*•\\s*(.*)$").find(item.title)
            val m2 = Regex("^(.*)\\s*S(\\d+)E(\\d+)$").find(item.title)
            if (m1 != null) {
                seBadge = "S${m1.groupValues[1]} E${m1.groupValues[2]}"
                cleanTitle = m1.groupValues[3].trim()
            } else if (m2 != null) {
                seBadge = "S${m2.groupValues[2]} E${m2.groupValues[3]}"
                cleanTitle = m2.groupValues[1].trim()
            }

            // Title
            Text(
                text = cleanTitle,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(4.dp))

            // Quality badge + size + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // S/E Badge
                if (seBadge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = seBadge,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Quality pill
                if (item.quality.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.quality,
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Size
                if (item.size.isNotEmpty()) {
                    Text(
                        text = item.size,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // Status text
                when {
                    isRunning && item.progress > 0 -> {
                        Text(
                            text = "${item.progress}%",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    isPending -> {
                        Text(
                            text = "Waiting...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    isComplete -> {
                        // Omit details like "Tap to play" as the larger thumbnail and play button handles this visually
                    }
                    isFailed -> {
                        Text(
                            text = "Failed",
                            color = AccentRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Progress bar for active downloads
            if (isRunning || isPending) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ProgressBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = item.progress / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentRed, AccentCyan)
                                )
                            )
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Delete button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Play downloaded file ───────────────────────────────────────────


private fun playDownloadedFile(context: Context, item: DownloadItem) {
    try {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            item.fileName
        )
        if (file.exists()) {
            val intent = Intent(context, com.spytube.app.CinefyPlayerActivity::class.java).apply {
                putExtra("title", item.title)
                putExtra("localUri", file.toURI().toString())
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "File not found — may have been deleted", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
    }
}

// ── Delete downloaded file + tracking record ───────────────────────


private fun deleteDownloadedFile(context: Context, item: DownloadItem) {
    try {
        // Remove from DownloadManager
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(item.downloadId)

        // Also delete the physical file if it still exists
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            item.fileName
        )
        if (file.exists()) file.delete()
    } catch (_: Exception) {}

    // Remove tracking record
    HiCineDownloadManager.removeDownload(context, item.downloadId)
    Toast.makeText(context, "Deleted: ${item.title}", Toast.LENGTH_SHORT).show()
}

// ── Load Downloads ─────────────────────────────────────────────────


private fun loadDownloads(context: Context): List<DownloadItem> {
    val ids = HiCineDownloadManager.getDownloadIds(context)
    if (ids.isEmpty()) return emptyList()

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return ids.mapNotNull { downloadId ->
        val meta = HiCineDownloadManager.getDownloadMeta(context, downloadId)
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = try { dm.query(query) } catch (_: Exception) { null }

        val status: Int
        val progress: Int
        val downloadedBytes: Long
        val totalBytes: Long
        val localUri: String?

        if (cursor != null && cursor.moveToFirst()) {
            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
            localUri = try { cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) } catch (_: Exception) { null }
            cursor.close()
        } else {
            cursor?.close()
            // Check if file still exists on disk
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                meta["file"] ?: ""
            )
            if (file.exists()) {
                status = DownloadManager.STATUS_SUCCESSFUL
                progress = 100
                downloadedBytes = file.length()
                totalBytes = file.length()
                localUri = file.toURI().toString()
            } else {
                HiCineDownloadManager.removeDownload(context, downloadId)
                return@mapNotNull null
            }
        }

        DownloadItem(
            downloadId = downloadId,
            title = meta["title"] ?: "Unknown",
            quality = meta["quality"] ?: "",
            size = meta["size"] ?: "",
            fileName = meta["file"] ?: "",
            timestamp = meta["time"]?.toLongOrNull() ?: 0,
            status = status,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            localUri = localUri,
            posterUrl = meta["poster"]
        )
    }
}
