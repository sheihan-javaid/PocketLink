/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.HeroUiState
import org.kde.kdeconnect_tp.R

@Composable
fun HeroDeviceCard(
    hero: HeroUiState,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkThemeMode()
    val isReducedMotion = isReducedMotionMode()
    val fontScale = LocalDensity.current.fontScale
    val shouldReflow = fontScale > 1.15f

    val infiniteTransition = rememberInfiniteTransition(label = "connectedPulse")
    val dotAlpha by if (!isReducedMotion && hero.isConnected) {
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotPulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val neonMint = Color(0xFF4ADE9F)

    AuroraGlassCard(
        modifier = modifier,
        showDotGrid = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (shouldReflow) {
                // Stacked layout for large font scale (> 1.15x) to prevent clipping
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DeviceIconBox(hero = hero, isDark = isDark)
                    Spacer(modifier = Modifier.width(KdeSpacing.medium))
                    Text(
                        text = hero.deviceName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                // Full-width chips row under the title row (untruncated)
                DeviceStatusChipsRow(
                    hero = hero,
                    dotAlpha = dotAlpha,
                    neonMint = neonMint,
                    isDark = isDark
                )

                // Centered Battery Gauge
                if (hero.battery != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        BatteryRing(battery = hero.battery)
                        Text(
                            text = if (hero.battery.isCharging) stringResource(R.string.battery_charging) else stringResource(R.string.battery_discharging),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Standard 1.0x side-by-side top row + full-width chips row below
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        DeviceIconBox(hero = hero, isDark = isDark)
                        Spacer(modifier = Modifier.width(KdeSpacing.medium))
                        Text(
                            text = hero.deviceName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                    }

                    if (hero.battery != null) {
                        Spacer(modifier = Modifier.width(KdeSpacing.medium))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BatteryRing(battery = hero.battery)
                            Text(
                                text = if (hero.battery.isCharging) stringResource(R.string.battery_charging) else stringResource(R.string.battery_discharging),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Full-width chips row under title & icon (Never cramped, plenty of space for full "Wi-Fi (5 GHz)")
                DeviceStatusChipsRow(
                    hero = hero,
                    dotAlpha = dotAlpha,
                    neonMint = neonMint,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun DeviceIconBox(
    hero: HeroUiState,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x337C6CFF),
                Color(0x1A3DDBFF)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEEF2FF),
                Color(0xFFE0E7FF)
            )
        )
    }

    val iconTint = if (isDark) Color(0xFF3DDBFF) else Color(0xFF6254C7)

    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(hero.deviceIconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun DeviceStatusChipsRow(
    hero: HeroUiState,
    dotAlpha: Float,
    neonMint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusLabel = stringResource(
            if (hero.isConnected) R.string.device_discovery_connected
            else R.string.error_not_reachable
        )
        // Pulsing live status pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .semantics { contentDescription = statusLabel }
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (hero.isConnected) {
                        if (isDark) Color(0x244ADE9F) else Color(0xFFDCFCE7)
                    } else {
                        if (isDark) Color(0x26FF6B7A) else Color(0xFFFEE2E2)
                    }
                )
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            // Pulsing dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (hero.isConnected) neonMint.copy(alpha = dotAlpha)
                        else Color(0xFFFF6B7A)
                    )
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (hero.isConnected) {
                    if (isDark) neonMint else Color(0xFF166534)
                } else {
                    if (isDark) Color(0xFFFFB4AB) else Color(0xFF991B1B)
                },
                maxLines = 1
            )
        }

        // Wi-Fi Chip with Signal Icon - full width row guarantees zero truncation
        hero.networkTypes.forEach { netType ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .semantics { contentDescription = netType }
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_wifi),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = netType,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
