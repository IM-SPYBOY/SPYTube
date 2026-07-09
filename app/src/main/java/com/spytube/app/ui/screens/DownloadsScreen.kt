package com.spytube.app.ui.screens

import com.downloader.PRDownloader
import com.downloader.Status
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import coil.compose.AsyncImage
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spytube.app.models.HiCineDownloadManager
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

// Premium palette
private val BgDeep = Color(0xFF050510)
private val SurfaceGlass = Color(0xFF12122A)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentPurple = Color(0xFF9C6AFF)
private val AccentRed = Color(0xFFE50914)
private val AccentAmber = Color(0xFFFFB74D)
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
    val status: Status,
    val progress: Int,         // 0-100
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadSpeed: Long = 0L, // Bytes per second
    val localUri: String?,     // File URI for completed downloads
    val posterUrl: String?     // Poster URL for thumbnail
)

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    var downloads by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }

    var previousBytesMap by remember { mutableStateOf(mapOf<Long, Long>()) }

    // Refresh download states every 1 second for accurate speed measurement
    LaunchedEffect(Unit) {
        while (true) {
            val nextDownloads = loadDownloads(context)
            val newPreviousBytes = mutableMapOf<Long, Long>()
            
            downloads = nextDownloads.map { item ->
                val prevBytes = previousBytesMap[item.downloadId] ?: item.downloadedBytes
                val speed = if (item.status == Status.RUNNING) {
                    item.downloadedBytes - prevBytes
                } else {
                    0L
                }
                newPreviousBytes[item.downloadId] = item.downloadedBytes
                item.copy(downloadSpeed = java.lang.Long.max(0L, speed))
            }
            previousBytesMap = newPreviousBytes
            delay(1000)
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
            // Title with count badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downloads",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(TextPrimary, AccentCyan.copy(alpha = 0.8f))
                        )
                    )
                )
                if (downloads.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${downloads.size}",
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloaded movies will appear here",
                            color = TextMuted,
                            fontSize = 13.sp
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
                        it.status == Status.RUNNING || it.status == Status.QUEUED || it.status == Status.PAUSED
                    }
                    val (completed, failed) = rest.partition {
                        it.status == Status.COMPLETED
                    }

                    if (active.isNotEmpty()) {
                        item {
                            SectionHeader("DOWNLOADING", AccentCyan, active.size)
                        }
                        items(active, key = { it.downloadId }) { item ->
                            DownloadCard(
                                item = item,
                                onPlay = null,
                                onRemove = {
                                    PRDownloader.cancel(item.downloadId.toInt())
                                    HiCineDownloadManager.removeDownload(context, item.downloadId)
                                    downloads = loadDownloads(context)
                                },
                                onPause = {
                                    PRDownloader.pause(item.downloadId.toInt())
                                    downloads = loadDownloads(context)
                                },
                                onResume = {
                                    val realStatus = PRDownloader.getStatus(item.downloadId.toInt())
                                    if (item.status == Status.FAILED) {
                                        android.widget.Toast.makeText(context, "Refreshing link...", android.widget.Toast.LENGTH_SHORT).show()
                                        HiCineDownloadManager.refreshDownloadLink(context, item.downloadId)
                                    } else if (realStatus == Status.UNKNOWN) {
                                        val resumed = HiCineDownloadManager.resumeOldLink(context, item.downloadId)
                                        if (!resumed) {
                                            android.widget.Toast.makeText(context, "Refreshing link...", android.widget.Toast.LENGTH_SHORT).show()
                                            HiCineDownloadManager.refreshDownloadLink(context, item.downloadId)
                                        } else {
                                            android.widget.Toast.makeText(context, "Resuming...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        PRDownloader.resume(item.downloadId.toInt())
                                    }
                                    downloads = loadDownloads(context)
                                }
                            )
                        }
                    }

                    if (completed.isNotEmpty()) {
                        item {
                            SectionHeader("READY TO PLAY", ProgressSuccess, completed.size)
                        }
                        items(completed, key = { it.downloadId }) { item ->
                            SwipeToDeleteCard(
                                onDelete = {
                                    deleteDownloadedFile(context, item)
                                    downloads = loadDownloads(context)
                                }
                            ) {
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
                    }

                    if (failed.isNotEmpty()) {
                        item {
                            SectionHeader("FAILED", AccentRed, failed.size)
                        }
                        items(failed, key = { it.downloadId }) { item ->
                            DownloadCard(
                                item = item,
                                onPlay = null,
                                onRemove = {
                                    HiCineDownloadManager.removeDownload(context, item.downloadId)
                                    downloads = loadDownloads(context)
                                },
                                onResume = {
                                    android.widget.Toast.makeText(context, "Refreshing link...", android.widget.Toast.LENGTH_SHORT).show()
                                    HiCineDownloadManager.refreshDownloadLink(context, item.downloadId)
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
private fun SectionHeader(title: String, color: Color, count: Int) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$count",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "swipe"
    )
    val deleteThreshold = -200f

    Box(modifier = Modifier.fillMaxWidth()) {
        // Delete background
        if (offsetX < -20f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Delete",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < deleteThreshold) {
                                onDelete()
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-300f, 0f)
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onPlay: (() -> Unit)?,
    onRemove: () -> Unit,
    onPause: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .build()
    }
    val isRunning = item.status == Status.RUNNING
    val isPending = item.status == Status.QUEUED
    val isComplete = item.status == Status.COMPLETED
    val isFailed = item.status == Status.FAILED || item.status == Status.CANCELLED || item.status == Status.UNKNOWN
    val isPaused = item.status == Status.PAUSED

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

    // Animated gradient offset for progress bar
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGlass)
            .border(
                0.5.dp,
                if (isRunning) AccentCyan.copy(alpha = 0.15f * pulseAlpha)
                else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .then(
                if (isComplete && onPlay != null) {
                    Modifier.clickable(onClick = onPlay)
                } else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Status icon / Thumbnail
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.posterUrl != null) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(item.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        imageLoader = imageLoader
                    )
                }

                // Dark overlay for completed items
                if (isComplete) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                // Status Icon centered on thumbnail
                Icon(
                    imageVector = when {
                        isComplete -> Icons.Rounded.PlayArrow
                        isFailed -> Icons.Rounded.ErrorOutline
                        isPaused -> Icons.Rounded.Pause
                        else -> Icons.Rounded.Download
                    },
                    contentDescription = when {
                        isComplete -> "Play"
                        isFailed -> "Failed"
                        isPaused -> "Paused"
                        else -> "Downloading"
                    },
                    tint = when {
                        isComplete -> Color.White.copy(alpha = 0.85f)
                        isFailed -> AccentRed
                        isPaused -> AccentAmber
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

                Spacer(Modifier.height(6.dp))

                // Metadata row (Quality, Size, S/E, Status)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status badge for active downloads
                    if (!isComplete) {
                        val (statusText, statusColor) = when {
                            isRunning -> "LIVE" to AccentCyan
                            isPaused -> "PAUSED" to AccentAmber
                            isFailed -> "FAILED" to AccentRed
                            isPending -> "QUEUED" to TextMuted
                            else -> "" to TextMuted
                        }
                        if (statusText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

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
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }

        // Active downloads section (Progress bar + Buttons)
        if (isRunning || isPending || isPaused || isFailed) {
            Spacer(Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress area
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Downloaded / Total bytes
                        val downloadedStr = formatBytes(item.downloadedBytes)
                        val totalStr = if (item.totalBytes > 0) formatBytes(item.totalBytes) else item.size
                        Text(
                            text = if (item.totalBytes > 0 || item.downloadedBytes > 0) "$downloadedStr / $totalStr" else "",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        // Speed + ETA
                        if (isRunning && item.downloadSpeed > 0) {
                            val speedStr = if (item.downloadSpeed > 1024 * 1024) {
                                String.format("%.1f MB/s", item.downloadSpeed / (1024f * 1024f))
                            } else {
                                String.format("%.0f KB/s", item.downloadSpeed / 1024f)
                            }
                            val remaining = item.totalBytes - item.downloadedBytes
                            val etaStr = if (remaining > 0 && item.downloadSpeed > 0) {
                                val etaSec = remaining / item.downloadSpeed
                                when {
                                    etaSec < 60 -> " · ${etaSec}s"
                                    etaSec < 3600 -> " · ${etaSec / 60}m ${etaSec % 60}s"
                                    else -> " · ${etaSec / 3600}h ${(etaSec % 3600) / 60}m"
                                }
                            } else ""
                            Text(
                                text = "$speedStr$etaStr",
                                color = AccentCyan.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isPaused) {
                            Text(
                                text = "${item.progress}%",
                                color = AccentAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isFailed) {
                            Text(
                                text = "Tap refresh to retry",
                                color = AccentRed.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Progress bar with animated gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ProgressBg)
                    ) {
                        val progressGradient = if (isRunning) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AccentPurple,
                                    AccentCyan,
                                    AccentPurple
                                ),
                                startX = -200f + gradientOffset * 600f,
                                endX = 200f + gradientOffset * 600f
                            )
                        } else if (isPaused) {
                            Brush.horizontalGradient(
                                colors = listOf(AccentAmber.copy(alpha = 0.7f), AccentAmber)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(AccentRed.copy(alpha = 0.5f), AccentRed)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = item.progress / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(progressGradient)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Action Buttons
                if (isRunning && onPause != null) {
                    IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Pause",
                            tint = AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (isPaused && onResume != null) {
                    IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume",
                            tint = ProgressSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (isFailed && onResume != null) {
                    IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Retry",
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } 
        
        // Completed downloads section (Export + Delete buttons)
        else if (isComplete) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File size on the left
                Text(
                    text = if (item.downloadedBytes > 0) formatBytes(item.downloadedBytes) else item.size,
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Row {
                    IconButton(
                        onClick = { 
                            try {
                                @Suppress("DEPRECATION")
                                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), item.fileName)
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Video"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot export file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Export",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Format Bytes ───────────────────────────────────────────────────

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.0f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

// ── Play downloaded file ───────────────────────────────────────────


private fun playDownloadedFile(context: Context, item: DownloadItem) {
    try {
        @Suppress("DEPRECATION")
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
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
        // Remove from PRDownloader
        PRDownloader.cancel(item.downloadId.toInt())

        // Also delete the physical file if it still exists
        @Suppress("DEPRECATION")
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            item.fileName
        )
        if (file.exists()) file.delete()
    } catch (_: Exception) {}

    // Remove tracking record
    HiCineDownloadManager.removeDownload(context, item.downloadId)
    Toast.makeText(context, "Deleted: ${item.title}", Toast.LENGTH_SHORT).show()
}

// ── Parse Size to Bytes ────────────────────────────────────────────

private fun parseSizeToBytes(sizeStr: String): Long {
    val cleanStr = sizeStr.trim().uppercase()
    val value = cleanStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return 0L
    return when {
        cleanStr.contains("GB") -> (value * 1024 * 1024 * 1024).toLong()
        cleanStr.contains("MB") -> (value * 1024 * 1024).toLong()
        cleanStr.contains("KB") -> (value * 1024).toLong()
        else -> value.toLong()
    }
}

// ── Load Downloads ─────────────────────────────────────────────────

private fun loadDownloads(context: Context): List<DownloadItem> {
    val ids = HiCineDownloadManager.getDownloadIds(context)
    if (ids.isEmpty()) return emptyList()

    return ids.mapNotNull { downloadId ->
        val meta = HiCineDownloadManager.getDownloadMeta(context, downloadId)
        
        var status = PRDownloader.getStatus(downloadId.toInt())
        val totalBytes = parseSizeToBytes(meta["size"] ?: "")

        @Suppress("DEPRECATION")
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            meta["file"] ?: ""
        )
        @Suppress("DEPRECATION")
        val tempFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            (meta["file"] ?: "") + ".temp"
        )
        val downloadedBytes = if (file.exists()) file.length() else if (tempFile.exists()) tempFile.length() else 0L

        if (status == Status.UNKNOWN) {
            if (file.exists() && downloadedBytes >= totalBytes * 0.99) {
                status = Status.COMPLETED
            } else if (tempFile.exists()) {
                status = Status.PAUSED
            } else if (!file.exists()) {
                HiCineDownloadManager.removeDownload(context, downloadId)
                return@mapNotNull null
            }
        }

        val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

        DownloadItem(
            downloadId = downloadId,
            title = meta["title"] ?: "Unknown",
            quality = meta["quality"] ?: "",
            size = meta["size"] ?: "",
            fileName = meta["file"] ?: "",
            timestamp = meta["time"]?.toLongOrNull() ?: 0,
            status = status,
            progress = java.lang.Integer.min(100, progress),
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            localUri = if (status == Status.COMPLETED) file.toURI().toString() else null,
            posterUrl = meta["poster"]
        )
    }
}
