package com.spytube.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spytube.app.models.MediaItem
import com.spytube.app.models.UpdateManager
import com.spytube.app.ui.components.GlassBottomNavigation
import com.spytube.app.ui.components.NavItem
import com.spytube.app.ui.components.safeLayerBackdrop
import com.spytube.app.ui.screens.*
import com.spytube.app.ui.theme.SpyTubeTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        enable120fps()
        
        setContent {
            val gyroAngle = com.spytube.app.ui.components.rememberGyroAngle()
            androidx.compose.runtime.CompositionLocalProvider(
                com.spytube.app.ui.components.LocalGyroAngle provides gyroAngle
            ) {
            SpyTubeTheme {
                // Update check state
                var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var isDownloading by remember { mutableStateOf(false) }
                val context = LocalContext.current

                // Check for updates on launch
                LaunchedEffect(Unit) {
                    UpdateManager.checkForUpdate(context) { info ->
                        updateInfo = info
                        if (info.state != UpdateManager.UpdateState.NO_UPDATE) {
                            showUpdateDialog = true
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        onMediaClick = { mediaItem ->
                            val intent = Intent(this@MainActivity, DetailActivity::class.java)
                            intent.putExtra("media", mediaItem)
                            startActivity(intent)
                        },
                        onPlayClick = { mediaItem ->
                            com.spytube.app.models.WatchHistoryManager.addToHistory(this@MainActivity, mediaItem)
                            val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                            intent.putExtra("media", mediaItem)
                            startActivity(intent)
                        }
                    )

                    // Update Dialog
                    if (showUpdateDialog && updateInfo != null) {
                        UpdateDialog(
                            info = updateInfo!!,
                            isDownloading = isDownloading,
                            onUpgradeNow = {
                                isDownloading = true
                                Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
                                UpdateManager.downloadAndInstall(context, updateInfo!!.updateUrl) { success ->
                                    isDownloading = false
                                }
                            },
                            onDownloadFromWebsite = {
                                UpdateManager.openWebsite(context)
                            },
                            onDismiss = {
                                if (updateInfo?.state != UpdateManager.UpdateState.FORCE_UPDATE) {
                                    showUpdateDialog = false
                                }
                            }
                        )
                    }
                }
            }
            }
        }
    }
    
    
    @Suppress("DEPRECATION")
    private fun enable120fps() {
        // Keep screen on for video app
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 1. Force preferred display mode to highest refresh rate
                val display = display ?: windowManager.defaultDisplay
                val modes = display.supportedModes
                val bestMode = modes.maxByOrNull { it.refreshRate }
                
                if (bestMode != null) {
                    window.attributes = window.attributes.also { params ->
                        params.preferredDisplayModeId = bestMode.modeId
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // API 23+: preferred display mode fallback
                val display = windowManager.defaultDisplay
                val modes = display.supportedModes
                val bestMode = modes.maxByOrNull { it.refreshRate }
                if (bestMode != null) {
                    window.attributes = window.attributes.also { params ->
                        params.preferredDisplayModeId = bestMode.modeId
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fail on unsupported devices
        }
    }
}


@Composable
fun UpdateDialog(
    info: UpdateManager.UpdateInfo,
    isDownloading: Boolean,
    onUpgradeNow: () -> Unit,
    onDownloadFromWebsite: () -> Unit,
    onDismiss: () -> Unit
) {
    val isForce = info.state == UpdateManager.UpdateState.FORCE_UPDATE

    Dialog(
        onDismissRequest = { if (!isForce) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isForce,
            dismissOnClickOutside = !isForce
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6C63FF),
                                    Color(0xFF3F51B5)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isForce) Icons.Default.Warning else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = if (isForce) "Update Required" else "Update Available",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6C63FF).copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${info.latestVersionName}",
                        color = Color(0xFF6C63FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = if (isForce) {
                        info.forceMessage.ifEmpty {
                            "This version is no longer supported. Please update to continue using SPYTube."
                        }
                    } else {
                        "A new version of SPYTube is available with improvements and bug fixes."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Upgrade Now button
                Button(
                    onClick = onUpgradeNow,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C63FF),
                        disabledContainerColor = Color(0xFF6C63FF).copy(alpha = 0.5f)
                    )
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Now", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Download from Website button
                OutlinedButton(
                    onClick = onDownloadFromWebsite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download from Website", fontWeight = FontWeight.Medium)
                }

                // "Maybe Later" for optional updates
                if (!isForce) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Maybe Later",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AppNavHost(
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit
) {
    val currentTab by com.spytube.app.models.GlobalNavState.currentTab.collectAsState()
    val backdrop = rememberLayerBackdrop()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("SPYTubePrefs", android.content.Context.MODE_PRIVATE) }
    var showWelcome by remember { mutableStateOf(prefs.getBoolean("is_first_launch_v1_3", true)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF0A0A12),
                        Color(0xFF060608)
                    )
                )
            )
    ) {
        // Content area with smooth crossfade transition between tabs
        androidx.compose.animation.Crossfade(
            targetState = currentTab,
            animationSpec = androidx.compose.animation.core.tween(200),
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                0 -> MainScreen(
                    backdrop = backdrop,
                    onMediaClick = onMediaClick,
                    onPlayClick = onPlayClick,
                    onSearchClick = { com.spytube.app.models.GlobalNavState.currentTab.value = 4 }
                )
                1 -> Box(Modifier.fillMaxSize().safeLayerBackdrop(backdrop)) { MoviesSeriesScreen(onMediaClick = onMediaClick) }
                2 -> Box(Modifier.fillMaxSize().safeLayerBackdrop(backdrop)) { LiveTvScreen() }
                3 -> Box(Modifier.fillMaxSize().safeLayerBackdrop(backdrop)) { DownloadsScreen() }
                4 -> Box(Modifier.fillMaxSize()) { SearchScreen(backdrop = backdrop, onMediaClick = onMediaClick) }
            }
        }
        GlassBottomNavigation(
            backdrop = backdrop,
            mainItems = listOf(
                NavItem("Home", if (currentTab == 0) Icons.Rounded.Home else Icons.Outlined.Home, currentTab == 0) { com.spytube.app.models.GlobalNavState.currentTab.value = 0 },
                NavItem("Movies", if (currentTab == 1) Icons.Rounded.Movie else Icons.Outlined.Movie, currentTab == 1) { com.spytube.app.models.GlobalNavState.currentTab.value = 1 },
                NavItem("Live TV", if (currentTab == 2) Icons.Rounded.LiveTv else Icons.Outlined.LiveTv, currentTab == 2) { com.spytube.app.models.GlobalNavState.currentTab.value = 2 },
                NavItem("Offline", if (currentTab == 3) Icons.Rounded.Download else Icons.Outlined.Download, currentTab == 3) { com.spytube.app.models.GlobalNavState.currentTab.value = 3 }
            ),
            searchItem = NavItem("Search", if (currentTab == 4) Icons.Rounded.Search else Icons.Outlined.Search, currentTab == 4) { com.spytube.app.models.GlobalNavState.currentTab.value = 4 },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Show Welcome Dialog on First Launch
        if (showWelcome) {
            com.spytube.app.ui.components.WelcomeDialogOverlay(
                backdrop = backdrop,
                onDismiss = {
                    prefs.edit().putBoolean("is_first_launch_v1_3", false).apply()
                    showWelcome = false
                }
            )
        }
    }
}
