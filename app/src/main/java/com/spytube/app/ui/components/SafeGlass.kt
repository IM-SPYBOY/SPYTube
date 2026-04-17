package com.spytube.app.ui.components

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.LayerBackdrop


@Composable
fun Modifier.safeGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(16.dp),
    width: Dp? = null,
    height: Dp? = null,
    surfaceColor: Color = Color(0xFF0A0A0A).copy(alpha = 0.55f),
    blurRadius: Dp = 24.dp,
    lensHeight: Dp = 24.dp,
    lensAmount: Dp = 48.dp,
    onDrawSurface: () -> Unit = {}
): Modifier {
    return this
        .then(if (width != null) Modifier.width(width) else Modifier)
        .then(if (height != null) Modifier.height(height) else Modifier)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                // True iOS UIBlurEffectStyle.dark: High blur (colorful clouds, no sharp edges), MAX vibrancy
                vibrancy()
                blur(blurRadius.toPx())
                lens(lensHeight.toPx(), lensAmount.toPx())
            },
            onDrawSurface = {
                // Draw the tinted surface for readability (per official tutorial)
                drawRect(surfaceColor)
                onDrawSurface()
            }
        )
}


@Composable
fun Modifier.safeLayerBackdrop(backdrop: LayerBackdrop): Modifier {
    return this.layerBackdrop(backdrop)
}
