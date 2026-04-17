package com.spytube.app.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spytube.app.ui.theme.GlassBorder
import com.spytube.app.ui.theme.GlassSurface


@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassSurface.copy(alpha = 0.85f)) // Higher alpha for visibility without blur
            .border(
                BorderStroke(1.dp, GlassBorder),
                RoundedCornerShape(cornerRadius)
            )
    ) {
        // Content Layer
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun VerticalGradientScrim(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(colors)
        )
    )
}
