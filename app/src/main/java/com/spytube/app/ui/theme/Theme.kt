package com.spytube.app.ui.theme

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp

// ── Core Palette ───────────────────────────────────────────────────────
val Black = Color(0xFF000000)
val NetflixRed = Color(0xFFE50914)
val NetflixDarkRed = Color(0xFFB20710)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFB3B3B3)
val DarkGray = Color(0xFF141414)

// Dark Glass Colors
val GlassSurface = Color(0x26FFFFFF)
val GlassSurfaceLight = Color(0x33FFFFFF)
val GlassBorder = Color(0x1AFFFFFF)
val GlassNavBg = Color(0xCC141414)

// ── Dark Color Scheme ──────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = White,
    secondary = NetflixDarkRed,
    onSecondary = White,
    background = Color(0xFF050510),
    onBackground = White,
    surface = Color(0xFF12122A),
    onSurface = White,
    surfaceVariant = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFFB3B3CC),
    outline = Color(0x1AFFFFFF)
)

// ── Light Color Scheme ─────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = NetflixRed,
    onPrimary = White,
    secondary = NetflixDarkRed,
    onSecondary = White,
    background = Color(0xFFF5F5FA),
    onBackground = Color(0xFF1A1A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F0F8),
    onSurfaceVariant = Color(0xFF555566),
    outline = Color(0x18000000)
)

// ── Extended Colors (accessible via LocalSpyTubeColors) ────────────────
data class SpyTubeExtendedColors(
    val isDark: Boolean,
    val bgGradient: Brush,
    val cardBg: Color,
    val cardBgAlt: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val accentRed: Color,
    val accentRedGlow: Color,
    val accentBlue: Color,
    val accentCyan: Color,
    val accentGreen: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val heroBannerGradientEnd: Color,
    val linkBlue: Color,        // Apple bright blue for links on dark bg
    val linkBlueDim: Color      // System blue for subtle link use
)

val DarkExtendedColors = SpyTubeExtendedColors(
    isDark = true,
    bgGradient = Brush.verticalGradient(listOf(Color(0xFF0A0A0C), Color(0xFF070709), Color(0xFF040405))),
    cardBg = Color(0xFF141414),
    cardBgAlt = Color(0xFF1A1A1A),
    glassSurface = Color(0x80141414), // 50% opacity
    glassBorder = Color(0x14FFFFFF), // 8% opacity
    accentRed = NetflixRed,
    accentRedGlow = NetflixRed.copy(alpha = 0.4f),
    accentBlue = NetflixRed, // Standardized to red
    accentCyan = NetflixRed, // Standardized to red
    accentGreen = Color(0xFF00C853), // Keep green for Live indicator
    textPrimary = Color(0xFFF0F0FF),
    textSecondary = Color(0xCCF0F0FF),
    textMuted = Color(0xFFB3B3CC), // Apple muted secondary
    heroBannerGradientEnd = Color(0xFF0A0A0C),  // Must match bgGradient first stop
    linkBlue = Color(0xFF2997FF),      // Apple bright blue (dark bg links)
    linkBlueDim = Color(0xFF0A84FF)    // System blue
)

val LightExtendedColors = SpyTubeExtendedColors(
    isDark = false,
    bgGradient = Brush.verticalGradient(listOf(Color(0xFFF8F8FE), Color(0xFFF0F0F8), Color(0xFFEBEBF5))),
    cardBg = Color(0xFFFFFFFF),
    cardBgAlt = Color(0xFFF8F8FC),
    glassSurface = Color(0xFFF0F0F8),
    glassBorder = Color(0x15000000),
    accentRed = Color(0xFFE50914),
    accentRedGlow = Color(0x20E50914),
    accentBlue = Color(0xFF5A52E0),
    accentCyan = Color(0xFF0099CC),
    accentGreen = Color(0xFF00C853),
    textPrimary = Color(0xFF1A1A2E),
    textSecondary = Color(0xFF555566),
    textMuted = Color(0xFF999AAA),
    heroBannerGradientEnd = Color(0xFFF8F8FE),  // Must match bgGradient first stop
    linkBlue = Color(0xFF0066CC),      // Apple link blue (light bg)
    linkBlueDim = Color(0xFF0071E3)    // Apple primary blue
)

val LocalSpyTubeColors = staticCompositionLocalOf { DarkExtendedColors }
val ColorScheme = DarkColorScheme

@Composable
fun SpyTubeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalSpyTubeColors provides extendedColors) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// ── Shared UI Utilities ──────────────────────────────────────────────────


fun Modifier.frostedGlass(shape: Shape) = composed {
    this
        .clip(shape)
        .background(Color(0xFF1A1A2E).copy(alpha = 0.70f))
        .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
}
