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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.HeroUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect_tp.R

/**
 * Compact two-column Connected MacBook Dashboard Card.
 *
 * Left column: Device identity (laptop preview icon, device name, connection status)
 * Right column: Live device info (battery gauge/charging animation, network name)
 */
@Composable
fun HeroDeviceCard(
    hero: HeroUiState,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionMode()
    val tokens = LocalDashboardTokens.current

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

    AuroraGlassCard(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // -------------------------------------------------------------
            // Left Column: Device Identity (compact, vertically aligned)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Existing laptop preview icon at the top
                MacDesktopPreview(
                    deviceName = hero.deviceName,
                    isConnected = hero.isConnected,
                    previewWidth = 98.dp
                )

                // Device name directly underneath
                Text(
                    text = hero.deviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )

                // Connection status below name: Green + "Connected" / Soft Red + "Disconnected"
                val statusLabel = if (hero.isConnected) {
                    stringResource(R.string.device_discovery_connected)
                } else {
                    stringResource(R.string.device_disconnected)
                }
                val statusDotColor = if (hero.isConnected) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = dotAlpha)
                } else {
                    MaterialTheme.colorScheme.error
                }
                val statusTextColor = if (hero.isConnected) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
                val statusBgColor = if (hero.isConnected) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .semantics { contentDescription = statusLabel }
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusTextColor,
                        maxLines = 1
                    )
                }
            }

            // Subtle vertical divider between the two columns
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(0.5.dp)
                    .height(96.dp)
                    .background(tokens.divider)
            )

            // -------------------------------------------------------------
            // Right Column: Live Device Information
            // -------------------------------------------------------------
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Existing battery/charging animation and charging status
                if (hero.battery != null) {
                    BatteryRing(
                        battery = hero.battery,
                        size = 62.dp,
                        strokeWidth = 5.dp
                    )
                    Text(
                        text = "${hero.battery.percent}% · " + if (hero.battery.isCharging) {
                            stringResource(R.string.battery_charging)
                        } else {
                            stringResource(R.string.battery_discharging)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(tokens.deviceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_device_laptop_32dp),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(tokens.onDeviceContainer),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.battery_not_available),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = tokens.textTertiary,
                        maxLines = 1
                    )
                }

                // Connected Wi-Fi/network name, when available
                if (hero.networkTypes.isNotEmpty()) {
                    val netType = hero.networkTypes.first()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .semantics { contentDescription = netType }
                            .clip(RoundedCornerShape(8.dp))
                            .background(tokens.quickActionChipBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_wifi),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = netType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
