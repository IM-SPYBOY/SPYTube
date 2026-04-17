package com.spytube.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spytube.app.ui.theme.NetflixRed
import com.kyant.backdrop.Backdrop

// ── Data ────────────────────────────────────────────────────────────

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)
private val InactiveGrey   = Color.White.copy(alpha = 0.55f)

// Selection bubble
private val BubbleColor = Color.White.copy(alpha = 0.14f)

// ── Glass Configuration Profiles ─────────────────────────────────────
// Toggle these values to switch between the two different glass styles

// ** 1. The NEW tuned iOS style (Reduced blur, soft edge) **
private val ActiveShadowElevation = 20.dp
private val ActiveShadowColor = Color.Black.copy(alpha = 0.55f)
private val ActiveBlurRadius = 16.dp // Maximum safe hardware limit
private val ActiveLensAmount = 24.dp

/*
// ** 2. The OLD heavy glass style (Heavy blur, prominent dark edge) **
// To use this backup profile, comment out the section above and uncomment this one:
private val ActiveShadowElevation = 12.dp
private val ActiveShadowColor = Color.Black.copy(alpha = 0.5f)
private val ActiveBlurRadius = 24.dp
private val ActiveLensAmount = 48.dp
*/

// Shared glass surface tint
private val GlassSurfaceColor = Color(0xFF1C1C1E).copy(alpha = 0.38f)

// ── Root Composable ─────────────────────────────────────────────────


@Composable
fun GlassBottomNavigation(
    backdrop: Backdrop,
    mainItems: List<NavItem>,
    searchItem: NavItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Navbar row on top of the gradient
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Component A: Main Pill (4 items) ────────────────────────
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .shadow(ActiveShadowElevation, CircleShape, ambientColor = Color.Transparent, spotColor = ActiveShadowColor)
                    .safeGlass(
                        backdrop = backdrop,
                        shape = CircleShape,
                        surfaceColor = GlassSurfaceColor,
                        blurRadius = ActiveBlurRadius,
                        lensAmount = ActiveLensAmount
                    )
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mainItems.forEach { item ->
                    IOSNavItem(item, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── Component B: Detached Search Circle ─────────────────────
            SearchCircle(backdrop = backdrop, item = searchItem)
        }
    }
}

// ── iOS Nav Item (Icon + Label + Capsule Bubble) ────────────────────

@Composable
private fun IOSNavItem(item: NavItem, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val isSelected = item.isSelected

    // Press-bounce
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_bounce"
    )

    // Color transition
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) NetflixRed else InactiveGrey,
        animationSpec = tween(200),
        label = "tint_color"
    )

    // Selection bubble alpha
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "bubble_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isSelected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                item.onClick()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // ── iOS Capsule Selection Bubble ─────────────────────────
        // Fills entire tab cell as a wide capsule
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 2.dp, vertical = 5.dp)
                    .graphicsLayer { alpha = bubbleAlpha }
                    .clip(CircleShape)
                    .background(BubbleColor)
            )
        }

        // ── Icon + Label ──────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            // ── Per-icon density calibration ──────────────────────
            // Material icons have inconsistent intrinsic padding/glyph weight.
            // We normalize them to sit on an identical visual baseline.
            val iconScale = when (item.title) {
                "Home" -> 1.0f
                "Movies" -> 1.05f    // Movie clapperboard is slightly narrow
                "Live TV" -> 1.0f
                "Offline" -> 1.1f    // Download arrow is thinner, scale up
                else -> 1f
            }
            val yOffset = when (item.title) {
                "Home" -> 0f
                "Movies" -> -0.3f
                "Live TV" -> -1.5f   // TV antenna pulls glyph center down
                "Offline" -> -0.2f
                else -> 0f
            }

            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = tintColor,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        translationY = yOffset * density
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.title,
                color = tintColor,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                lineHeight = 10.sp
            )
        }
    }
}

// ── Detached Search Circle ──────────────────────────────────────────

@Composable
private fun SearchCircle(
    backdrop: Backdrop,
    item: NavItem,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "search_bounce"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(58.dp)
            .shadow(ActiveShadowElevation, CircleShape, ambientColor = Color.Transparent, spotColor = ActiveShadowColor)
            .safeGlass(
                backdrop = backdrop,
                shape = CircleShape,
                surfaceColor = GlassSurfaceColor,
                blurRadius = ActiveBlurRadius,
                lensAmount = ActiveLensAmount
            )
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!item.isSelected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                item.onClick()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (item.isSelected) NetflixRed else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
