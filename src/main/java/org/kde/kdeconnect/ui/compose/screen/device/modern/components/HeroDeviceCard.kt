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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.ui.compose.screen.device.modern.HeroUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect_tp.R

/**
 * Compact connected-device card with desktop preview, identity, network, and battery state.
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DesktopPreviewThumbnail(
                modifier = Modifier.width(88.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = hero.deviceName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )

                val statusLabel = if (hero.isConnected) {
                    stringResource(R.string.device_discovery_connected)
                } else {
                    stringResource(R.string.device_disconnected)
                }
                val statusDotColor = if (hero.isConnected) {
                    tokens.statusConnectedText.copy(alpha = dotAlpha)
                } else {
                    tokens.statusDisconnectedText
                }
                val statusTextColor = if (hero.isConnected) {
                    tokens.statusConnectedText
                } else {
                    tokens.statusDisconnectedText
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .semantics { contentDescription = statusLabel }
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusTextColor,
                        maxLines = 1
                    )
                }

                val networkName = hero.networkTypes.firstOrNull()
                    ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                    ?: stringResource(R.string.network_name_unavailable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.semantics { contentDescription = networkName }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_wifi),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tokens.networkChipText),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = networkName,
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.networkChipText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.width(84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hero.battery != null) {
                    BatteryRing(
                        battery = hero.battery,
                        size = 72.dp,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = if (hero.battery.isCharging) {
                            stringResource(R.string.battery_charging)
                        } else {
                            stringResource(R.string.battery_discharging)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
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
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = tokens.textTertiary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopPreviewThumbnail(modifier: Modifier = Modifier) {
    val tokens = LocalDashboardTokens.current
    val previewDescription = stringResource(R.string.desktop_preview_desc)
    val shape = RoundedCornerShape(6.dp)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(16f / 10f)
            .clip(shape)
            .background(tokens.accentIndigo)
            .border(1.dp, tokens.cardBorderBottom, shape)
            .semantics { contentDescription = previewDescription }
    ) {
        val previewWidth = maxWidth
        val previewHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.07f)
                .background(tokens.accentSoftIndigo.copy(alpha = 0.75f))
        )

        Box(
            modifier = Modifier
                .offset(x = previewWidth * 0.09f, y = previewHeight * 0.2f)
                .size(width = previewWidth * 0.52f, height = previewHeight * 0.58f)
                .clip(RoundedCornerShape(3.dp))
                .background(tokens.laptopTileBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.12f)
                    .background(tokens.cardBorderTop)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .align(Alignment.TopStart)
                    .padding(start = previewWidth * 0.08f, top = previewHeight * 0.24f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 1) 0.6f else 0.82f)
                            .fillMaxHeight(0.07f)
                            .background(tokens.accentSoftIndigo.copy(alpha = 0.55f))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -(previewWidth * 0.08f), y = previewHeight * 0.36f)
                .size(width = previewWidth * 0.3f, height = previewHeight * 0.44f)
                .clip(RoundedCornerShape(3.dp))
                .background(tokens.accentSoftIndigo)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(previewHeight * 0.05f))
                .fillMaxWidth(0.42f)
                .fillMaxHeight(0.07f)
                .clip(RoundedCornerShape(4.dp))
                .background(tokens.laptopTileBg)
        )
    }
}
