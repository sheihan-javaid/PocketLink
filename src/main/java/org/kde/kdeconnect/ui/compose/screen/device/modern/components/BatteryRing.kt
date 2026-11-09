/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.ui.compose.screen.device.modern.BatteryUiState
import org.kde.kdeconnect_tp.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryRing(
    battery: BatteryUiState,
    modifier: Modifier = Modifier,
    size: Dp = 82.dp,
    strokeWidth: Dp = 6.5.dp
) {
    val isDark = isDarkThemeMode()
    val isReducedMotion = isReducedMotionMode()

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(battery.percent) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered || isReducedMotion) (battery.percent / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = if (isReducedMotion) tween(0) else tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "batteryGaugeProgress"
    )

    // Gauge geometry: 270 degrees total, starting at 135 degrees (bottom-left)
    val startAngle = 135f
    val totalSweep = 270f
    val currentSweep = totalSweep * animatedProgress

    val trackColor = if (isDark) Color(0x333DDBFF) else Color(0x286254C7)

    val neonIndigo = Color(0xFF7C6CFF)
    val neonCyan = Color(0xFF3DDBFF)
    val neonMint = Color(0xFF4ADE9F)
    val neonCoral = Color(0xFFFF6B7A)

    val glowColor = when {
        battery.isCharging -> neonMint
        battery.isLow -> neonCoral
        else -> neonCyan
    }

    val accessibilityDesc = when {
        battery.isCharging -> stringResource(R.string.battery_status_charging_format, battery.percent)
        battery.isLow -> stringResource(R.string.battery_status_low_format, battery.percent)
        else -> stringResource(R.string.battery_status_format, battery.percent)
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = accessibilityDesc },
        contentAlignment = Alignment.Center
    ) {
        val fontScale = LocalDensity.current.fontScale
        // Cap ring numeral size growth at 1.35x
        val cappedFontSize = if (fontScale > 1.0f) (16.sp / fontScale.coerceAtLeast(1f)) else 16.sp

        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // Pad by 14dp so glow stroke never clips canvas edge
            val radius = (this.size.minDimension - strokePx - 16.dp.toPx()) / 2f
            val center = this.center

            // 1. Thin outer hairline perimeter
            val outerHairlineRadius = radius + (strokePx / 2f) + 2.5.dp.toPx()
            drawArc(
                color = trackColor.copy(alpha = if (isDark) 0.25f else 0.18f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - outerHairlineRadius, center.y - outerHairlineRadius),
                size = Size(outerHairlineRadius * 2, outerHairlineRadius * 2),
                style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Visible full 270-degree track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 3. Subtle tick marks along perimeter (inset so they never touch numerals, fade in sequence)
            val tickCount = 7
            val tickStep = totalSweep / (tickCount - 1)
            for (i in 0 until tickCount) {
                val angleDeg = startAngle + (i * tickStep)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val innerEdge = radius - (strokePx / 2f)
                val tickStartR = innerEdge - 1.dp.toPx()
                val tickEndR = innerEdge - 3.dp.toPx()
                val startOffset = Offset(
                    x = center.x + (tickStartR * cos(angleRad)).toFloat(),
                    y = center.y + (tickStartR * sin(angleRad)).toFloat()
                )
                val endOffset = Offset(
                    x = center.x + (tickEndR * cos(angleRad)).toFloat(),
                    y = center.y + (tickEndR * sin(angleRad)).toFloat()
                )
                val tickAlpha = (0.25f + (i.toFloat() / (tickCount - 1)) * 0.45f) * (if (isDark) 0.8f else 0.6f)
                drawLine(
                    color = trackColor.copy(alpha = tickAlpha),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Progress arc + Soft glow underlay + End-cap glow
            if (currentSweep > 0f) {
                // Glow underlay
                drawArc(
                    color = glowColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Foreground gradient sweep
                val progressBrush = when {
                    battery.isCharging -> Brush.linearGradient(
                        colors = listOf(neonMint, neonCyan),
                        start = Offset(center.x - radius, center.y + radius),
                        end = Offset(center.x + radius, center.y - radius)
                    )
                    battery.isLow -> Brush.linearGradient(
                        colors = listOf(neonCoral, Color(0xFFFFB547)),
                        start = Offset(center.x - radius, center.y + radius),
                        end = Offset(center.x + radius, center.y - radius)
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(neonIndigo, neonCyan),
                        start = Offset(center.x - radius, center.y + radius),
                        end = Offset(center.x + radius, center.y - radius)
                    )
                }

                drawArc(
                    brush = progressBrush,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Soft end-cap glow dot at the tip of the sweep
                val tipAngleDeg = startAngle + currentSweep
                val tipAngleRad = Math.toRadians(tipAngleDeg.toDouble())
                val tipCenter = Offset(
                    x = center.x + (radius * cos(tipAngleRad)).toFloat(),
                    y = center.y + (radius * sin(tipAngleRad)).toFloat()
                )
                drawCircle(
                    color = glowColor.copy(alpha = if (isDark) 0.5f else 0.35f),
                    radius = strokePx * 0.75f,
                    center = tipCenter
                )
            }
        }

        // Center Percentage & Charging Indicator with capped numeral size
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${battery.percent}%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = cappedFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (battery.isCharging) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "⚡",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = cappedFontSize * 0.75f),
                    color = neonMint
                )
            }
        }
    }
}