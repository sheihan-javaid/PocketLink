/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.ui.compose.screen.device.modern.BatteryUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect_tp.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryRing(
    battery: BatteryUiState,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 6.dp
) {
    val isReducedMotion = isReducedMotionMode()
    val tokens = LocalDashboardTokens.current
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(battery.percent) {
        animationTriggered = true
    }

    val progress by animateFloatAsState(
        targetValue = if (animationTriggered || isReducedMotion) {
            (battery.percent / 100f).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else 500,
            easing = FastOutSlowInEasing
        ),
        label = "batteryGaugeProgress"
    )
    val stateColorProgress by animateFloatAsState(
        targetValue = if (battery.isCharging) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else 400,
            easing = FastOutSlowInEasing
        ),
        label = "batteryGaugeStateColor"
    )
    val stateColor = lerp(tokens.ringArc, tokens.statusConnectedText, stateColorProgress)
    val accessibilityDescription = when {
        battery.isCharging -> stringResource(R.string.battery_status_charging_format, battery.percent)
        battery.isLow -> stringResource(R.string.battery_status_low_format, battery.percent)
        else -> stringResource(R.string.battery_status_format, battery.percent)
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityDescription },
        contentAlignment = Alignment.Center
    ) {
        key(battery.isCharging, isReducedMotion) {
            BatteryRingContents(
                battery = battery,
                progress = progress,
                stateColor = stateColor,
                strokeWidth = strokeWidth,
                isReducedMotion = isReducedMotion,
                fontScale = LocalDensity.current.fontScale
            )
        }
    }
}

@Composable
private fun BoxScope.BatteryRingContents(
    battery: BatteryUiState,
    progress: Float,
    stateColor: Color,
    strokeWidth: Dp,
    isReducedMotion: Boolean,
    fontScale: Float
) {
    val tokens = LocalDashboardTokens.current
    val transition = if (isReducedMotion) {
        null
    } else {
        rememberInfiniteTransition(label = "batteryState")
    }
    val shimmerPhase by if (transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (battery.isCharging) 1600 else 2800,
                    easing = LinearEasing
                )
            ),
            label = "batteryShimmer"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val accentPhase by if (transition != null && battery.isCharging) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing)
            ),
            label = "batteryPulse"
        )
    } else if (transition != null) {
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "batteryBreath"
        )
    } else {
        remember { mutableFloatStateOf(if (battery.isCharging) 0f else 1f) }
    }
    val iconPhase by if (transition != null) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (battery.isCharging) 600 else 900,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "batteryStateIcon"
        )
    } else {
        remember { mutableFloatStateOf(0.5f) }
    }

    val wave = 0.5f - cos(PI * iconPhase).toFloat() * 0.5f
    val iconAlpha = if (isReducedMotion) {
        1f
    } else if (battery.isCharging) {
        0.45f + 0.55f * wave
    } else {
        0.5f + 0.5f * wave
    }
    val iconScale = if (isReducedMotion || !battery.isCharging) 1f else 0.9f + 0.2f * wave
    val iconOffsetY = if (isReducedMotion || battery.isCharging) 0f else -1f + 3f * wave
    val shimmerAlpha = if (isReducedMotion) {
        0f
    } else {
        sin(PI * shimmerPhase).toFloat() * if (battery.isCharging) 0.5f else 0.3f
    }
    val fontSize = if (fontScale > 1f) 14.sp / fontScale else 14.sp
    val ringBrush = if (battery.isCharging) {
        Brush.linearGradient(listOf(stateColor, stateColor))
    } else {
        Brush.linearGradient(
            colors = listOf(
                tokens.ringCap,
                stateColor
            )
        )
    }
    val shimmerStartAngle = if (battery.isCharging) {
        shimmerPhase
    } else {
        1f - shimmerPhase
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val strokePx = strokeWidth.toPx()
        val radius = (size.minDimension - strokePx - 4.dp.toPx()) / 2f
        val center = this.center
        val arcBounds = Offset(
            center.x - radius,
            center.y - radius
        )
        val arcSize = Size(radius * 2f, radius * 2f)
        val startAngle = 135f
        val totalSweep = 270f
        val currentSweep = totalSweep * progress

        drawArc(
            color = tokens.ringTrack,
            startAngle = startAngle,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = arcBounds,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        if (currentSweep > 0f) {
            drawArc(
                brush = ringBrush,
                startAngle = startAngle,
                sweepAngle = currentSweep,
                useCenter = false,
                topLeft = arcBounds,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val segmentSweep = minOf(18f, currentSweep * 0.5f)
            if (segmentSweep > 0f && shimmerAlpha > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = shimmerAlpha),
                    startAngle = startAngle + (currentSweep - segmentSweep) * shimmerStartAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = arcBounds,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            val tipAngle = Math.toRadians((startAngle + currentSweep).toDouble())
            val tipCenter = Offset(
                x = center.x + radius * cos(tipAngle).toFloat(),
                y = center.y + radius * sin(tipAngle).toFloat()
            )
            val dotRadius = strokePx * 0.7f

            if (battery.isCharging && !isReducedMotion) {
                val pulseRadius = dotRadius * (1f + 1.4f * accentPhase)
                drawCircle(
                    color = stateColor.copy(alpha = 0.55f * (1f - accentPhase)),
                    radius = pulseRadius,
                    center = tipCenter,
                    style = Stroke(width = strokePx * 0.45f)
                )
            }
            drawCircle(
                color = stateColor.copy(alpha = if (battery.isCharging) 1f else accentPhase),
                radius = dotRadius,
                center = tipCenter
            )
        }
    }

    Text(
        text = "${battery.percent}%",
        modifier = Modifier.align(Alignment.Center),
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
        ),
        color = tokens.textPrimary
    )

    Canvas(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .size(10.dp)
            .graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
                alpha = iconAlpha
                translationY = iconOffsetY
            }
    ) {
        val scale = size.width / 24f
        val path = Path().apply {
            if (battery.isCharging) {
                moveTo(13f * scale, 3f * scale)
                lineTo(13f * scale, 10f * scale)
                lineTo(19f * scale, 10f * scale)
                lineTo(11f * scale, 21f * scale)
                lineTo(11f * scale, 14f * scale)
                lineTo(5f * scale, 14f * scale)
                lineTo(13f * scale, 3f * scale)
            } else {
                moveTo(12f * scale, 5f * scale)
                lineTo(12f * scale, 19f * scale)
                moveTo(6f * scale, 13f * scale)
                lineTo(12f * scale, 19f * scale)
                lineTo(18f * scale, 13f * scale)
            }
        }
        drawPath(
            path = path,
            color = stateColor,
            style = Stroke(
                width = 2f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
