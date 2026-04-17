package com.spytube.app.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spytube.app.LivePlayerActivity
import com.spytube.app.models.IptvChannel
import com.spytube.app.models.IptvRepository
import com.spytube.app.ui.theme.frostedGlass
import kotlinx.coroutines.launch

// ── Premium Color Palette ──────────────────────────────────────────────
private val BgDeep = Color(0xFF050510)
private val BgCard = Color(0xFF0D0D1E)
private val BgCardAlt = Color(0xFF0A0A1A)
private val SurfaceGlass = Color(0xFF12122A)
private val AccentRed = Color(0xFFE50914)
private val AccentRedGlow = Color(0x40E50914)
private val AccentBlue = Color(0xFF6C63FF)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentGreen = Color(0xFF00E676)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextSecondary = Color(0xB3F0F0FF)
private val TextMuted = Color(0x66F0F0FF)
private val GlassBorder = Color(0x1AFFFFFF)

// Country code → flag emoji
private fun countryFlag(code: String): String {
    if (code.length != 2) return "📺"
    return try {
        val first = Character.toChars(0x1F1E6 + (code[0].uppercaseChar() - 'A'))
        val second = Character.toChars(0x1F1E6 + (code[1].uppercaseChar() - 'A'))
        String(first) + String(second)
    } catch (_: Exception) { "📺" }
}

@Composable
fun LiveTvScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories = remember { IptvRepository.getCategories() }

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("all") }
    var channels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Load channels when category changes
    LaunchedEffect(selectedCategory) {
        if (searchQuery.isBlank()) {
            isLoading = true
            error = null
            val result = IptvRepository.loadChannels(selectedCategory)
            result.fold(
                onSuccess = { channels = it },
                onFailure = { error = it.message ?: "Failed to load" }
            )
            isLoading = false
        }
    }

    // Universal search with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(300)
        val result = IptvRepository.searchAll(searchQuery)
        result.fold(
            onSuccess = { searchResults = it },
            onFailure = { searchResults = emptyList() }
        )
        isSearching = false
    }

    val isInSearch = searchQuery.isNotBlank()
    val displayChannels = if (isInSearch) searchResults else channels

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A20),
                        BgDeep,
                        Color(0xFF050514)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // ── Header ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = showSearch, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(SurfaceGlass)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(21.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text("Search all channels...", color = TextMuted, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(AccentCyan),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            scope.launch {
                                isLoading = true
                                error = null
                                val r = IptvRepository.loadChannels(selectedCategory)
                                r.fold(
                                    onSuccess = { channels = it },
                                    onFailure = { error = it.message }
                                )
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                        }
                    }
                }

                if (!showSearch) {
                    // Gradient title
                    Text(
                        "Live TV",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(TextPrimary, AccentCyan.copy(alpha = 0.8f))
                            )
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    // Search icon
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            Icons.Rounded.Search, "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Refresh icon
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val r = IptvRepository.loadChannels(selectedCategory, forceRefresh = true)
                            r.fold(
                                onSuccess = { channels = it },
                                onFailure = { error = it.message }
                            )
                            isLoading = false
                        }
                    }) {
                        Icon(
                            Icons.Rounded.Refresh, "Refresh",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ── Category Chips ─────────────────────────────────────────────
        AnimatedVisibility(visible = !isInSearch, enter = fadeIn() + slideInVertically(), exit = fadeOut()) {
            LazyRow(
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    CategoryChip(
                        label = cat.name,
                        isSelected = selectedCategory == cat.id
                    ) { selectedCategory = cat.id }
                }
            }
        }

        // ── Content ────────────────────────────────────────────────────
        when {
            isLoading || isSearching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pulsing loader
                        val infiniteTransition = rememberInfiniteTransition(label = "loader")
                        val pulse by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        CircularProgressIndicator(
                            color = AccentCyan.copy(alpha = pulse),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (isSearching) "Searching..." else "Loading channels...",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceGlass)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline, null,
                            tint = AccentRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Failed to load", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            error ?: "", color = TextMuted, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 3
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                error = null
                                scope.launch {
                                    isLoading = true
                                    val r = IptvRepository.loadChannels(selectedCategory, forceRefresh = true)
                                    r.fold(
                                        onSuccess = { channels = it },
                                        onFailure = { error = it.message }
                                    )
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            displayChannels.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.LiveTv, null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No channels found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            else -> {
                val infiniteTransition = rememberInfiniteTransition(label = "sharedLive")
                val liveDotAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sharedDotPulse"
                )

                // Channel count
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentCyan)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${displayChannels.size} channels available",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayChannels, key = { it.streamUrl }) { channel ->
                        ChannelCard(channel, liveDotAlpha) {
                            val intent = Intent(context, LivePlayerActivity::class.java)
                            intent.putExtra("channel", channel)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }
}

// ── Category Chip ──────────────────────────────────────────────────────
@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (isSelected) AccentRed else SurfaceGlass,
        animationSpec = tween(250), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) AccentRed.copy(alpha = 0.6f) else GlassBorder,
        animationSpec = tween(250), label = "chipBorder"
    )
    val textColor by animateColorAsState(
        if (isSelected) Color.White else TextSecondary,
        animationSpec = tween(250), label = "chipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isSelected) Modifier.shadow(8.dp, RoundedCornerShape(22.dp), spotColor = AccentRedGlow)
                else Modifier
            )
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ── Channel Card ───────────────────────────────────────────────────────
@Composable
private fun ChannelCard(channel: IptvChannel, liveDotAlpha: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(BgCard, BgCardAlt, Color(0xFF08081A))
                )
            )
            .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel logo
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1A1A3A), Color(0xFF12122A))
                    )
                )
                .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrEmpty()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logoUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = channel.name,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(text = countryFlag(channel.country), fontSize = 24.sp)
            }
        }

        Spacer(Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Country badge
                if (channel.country.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentBlue.copy(alpha = 0.12f))
                            .border(0.5.dp, AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${countryFlag(channel.country)} ${channel.country}",
                            color = AccentBlue.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Category badges
                channel.categories.take(2).forEach { cat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .frostedGlass(RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = cat.replaceFirstChar { it.uppercase() },
                            color = Color(0xFFF0F0FF), // Pure white for contrast
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // LIVE indicator + Play
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = liveDotAlpha))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "LIVE", color = AccentGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Icon(
                Icons.Rounded.PlayCircleFilled,
                contentDescription = "Play",
                tint = AccentCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
