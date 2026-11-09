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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect_tp.R

@Composable
fun ContextualMediaCard(
    media: MediaPlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkThemeMode()
    val isReducedMotion = isReducedMotionMode()

    val neonIndigo = Color(0xFF7C6CFF)
    val neonCyan = Color(0xFF3DDBFF)

    AuroraGlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Album Art Fallback with Aurora gradient & music note
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                if (isDark) {
                                    listOf(Color(0xFF2E1A66), Color(0xFF133B5C))
                                } else {
                                    listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_modern_album_art),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (isDark) neonCyan else Color(0xFF6254C7)),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(KdeSpacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = media.title.ifEmpty { "Now Playing" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // Animated 4-bar equalizer while playing
                        if (media.isPlaying) {
                            EqualizerBars(
                                isReducedMotion = isReducedMotion,
                                barColor = if (isDark) neonCyan else Color(0xFF0F766E)
                            )
                        }
                    }

                    Text(
                        text = if (media.artist.isNotEmpty()) "${media.artist} · ${media.playerName}" else media.playerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Thin neon progress line with contrast glowing playhead (no gap)
                    val progressFraction = 0.42f
                    val playheadCoreColor = if (isDark) Color(0xFF3DDBFF) else Color(0xFF6254C7)
                    val playheadGlowColor = if (isDark) Color(0x663DDBFF) else Color(0x406254C7)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                        )
                        // Active progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(neonIndigo, neonCyan)
                                    )
                                )
                        )
                        // Glowing playhead with high contrast core
                        Box(
                            modifier = Modifier.fillMaxWidth(progressFraction),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(playheadGlowColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(playheadCoreColor)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(KdeSpacing.small))

                // Playback Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KdeSpacing.xSmall)
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = media.canSkipPrev,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_previous_white),
                            contentDescription = "Previous Track",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // High-contrast Gradient Play Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(neonIndigo, neonCyan)
                                )
                            )
                            .semantics { role = Role.Button }
                            .clickable(enabled = media.canPlayPause, onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                if (media.isPlaying) R.drawable.ic_pause_white
                                else R.drawable.ic_play_white
                            ),
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        enabled = media.canSkipNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_next_white),
                            contentDescription = "Next Track",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerBars(
    isReducedMotion: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "equalizerTransition")

    val h1 by if (!isReducedMotion) {
        transition.animateFloat(
            initialValue = 4f,
            targetValue = 14f,
            animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h1"
        )
    } else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(8f) }

    val h2 by if (!isReducedMotion) {
        transition.animateFloat(
            initialValue = 14f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h2"
        )
    } else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(14f) }

    val h3 by if (!isReducedMotion) {
        transition.animateFloat(
            initialValue = 6f,
            targetValue = 16f,
            animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h3"
        )
    } else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(10f) }

    val h4 by if (!isReducedMotion) {
        transition.animateFloat(
            initialValue = 12f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h4"
        )
    } else androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(6f) }

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(h1.dp).clip(RoundedCornerShape(1.dp)).background(barColor))
        Box(modifier = Modifier.width(2.5.dp).height(h2.dp).clip(RoundedCornerShape(1.dp)).background(barColor))
        Box(modifier = Modifier.width(2.5.dp).height(h3.dp).clip(RoundedCornerShape(1.dp)).background(barColor))
        Box(modifier = Modifier.width(2.5.dp).height(h4.dp).clip(RoundedCornerShape(1.dp)).background(barColor))
    }
}