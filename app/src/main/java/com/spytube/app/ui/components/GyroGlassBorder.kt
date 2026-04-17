package com.spytube.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ─── Shared gyro angle via CompositionLocal ──────────────────────────

/** Shared gyro angle — provide once at root (MainActivity). */
val LocalGyroAngle = compositionLocalOf { 0f }


@Composable
fun rememberGyroAngle(): Float {
    val context = LocalContext.current
    var rawAngle by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                rawAngle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val smoothAngle by animateFloatAsState(
        targetValue = rawAngle,
        animationSpec = tween(durationMillis = 150),
        label = "gyroAngle"
    )

    return smoothAngle
}

// ─── Shader-level refractive edge ────────────────────────────────────


@Composable
fun Modifier.refractiveEdge(
    cornerRadius: Dp = 12.dp,
    isCircle: Boolean = false,
    strokeWidth: Dp = 1.dp,
    peakAlpha: Float = 0.55f,
    baseAlpha: Float = 0.04f
): Modifier {
    val angle = LocalGyroAngle.current

    return this.drawWithContent {
        drawContent()

        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rad = Math.toRadians(angle.toDouble())

        // Specular highlight point based on gyro tilt
        val highlightX = cx + cos(rad).toFloat() * cx * 0.8f
        val highlightY = cy + sin(rad).toFloat() * cy * 0.8f

        val edgeBrush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = peakAlpha),
                Color.White.copy(alpha = peakAlpha * 0.6f),
                Color.White.copy(alpha = baseAlpha),
                Color.White.copy(alpha = baseAlpha * 0.5f)
            ),
            center = Offset(highlightX, highlightY),
            radius = maxOf(w, h) * 0.9f
        )

        val sw = strokeWidth.toPx()

        if (isCircle) {
            drawOval(
                brush = edgeBrush,
                topLeft = Offset(sw / 2, sw / 2),
                size = Size(w - sw, h - sw),
                style = Stroke(width = sw)
            )
        } else {
            val cr = cornerRadius.toPx()
            drawRoundRect(
                brush = edgeBrush,
                topLeft = Offset(sw / 2, sw / 2),
                size = Size(w - sw, h - sw),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = sw)
            )
        }
    }
}
