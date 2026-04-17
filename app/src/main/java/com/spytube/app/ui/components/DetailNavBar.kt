package com.spytube.app.ui.components

import android.app.Activity
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.spytube.app.models.GlobalNavState
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.spytube.app.ui.theme.SpyTubeTheme
import androidx.compose.runtime.*

object DetailNavBar {

    @JvmStatic
    fun setupOpticsGlassRoot(activity: Activity, nativeRoot: View, onMounted: () -> Unit) {
        val composeRoot = ComposeView(activity)
        activity.setContentView(composeRoot)

        composeRoot.setContent {
            SpyTubeTheme {
                val backdrop = rememberLayerBackdrop()
                var isMounted by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Host the Native UI underneath, passing it through the backdrop filter
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeLayerBackdrop(backdrop),
                        factory = { nativeRoot },
                        update = {
                            if (!isMounted) {
                                isMounted = true
                                // Hand control back to the Activity to run logic
                                // Delay slightly to ensure layout pass if needed, but normally immediate is fine
                                it.post { onMounted() }
                            }
                        }
                    )
                    
                    // Render the real optics frosted glass navigation bar
                    GlassBottomNavigation(
                        backdrop = backdrop,
                        mainItems = listOf(
                            NavItem("Home", Icons.Outlined.Home, false) { navigate(activity, 0) },
                            NavItem("Movies", Icons.Outlined.Movie, false) { navigate(activity, 1) },
                            NavItem("Live TV", Icons.Outlined.LiveTv, false) { navigate(activity, 2) },
                            NavItem("Offline", Icons.Outlined.Download, false) { navigate(activity, 3) }
                        ),
                        searchItem = NavItem("Search", Icons.Outlined.Search, false) { navigate(activity, 4) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    private fun navigate(activity: Activity, tabIndex: Int) {
        GlobalNavState.currentTab.value = tabIndex
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }
}
