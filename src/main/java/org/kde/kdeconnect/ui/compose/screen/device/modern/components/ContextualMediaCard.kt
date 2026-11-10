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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect_tp.R

@Composable
fun ContextualMediaCard(
    media: MediaPlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AuroraGlassCard(
        modifier = modifier
    ) {
        ContextualMediaContent(
            media = media,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            modifier = Modifier.padding(KdeSpacing.cardPadding)
        )
    }
}

@Composable
fun ContextualMediaContent(
    media: MediaPlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isReducedMotion = isReducedMotionMode()
    val tokens = LocalDashboardTokens.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album Art Fallback with container gradient & music note
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.mediaContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_modern_album_art),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tokens.onMediaContainer),
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
                        text = media.title.ifEmpty { stringResource(R.string.now_playing) },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.textPrimary,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Animated 4-bar equalizer while playing
                    if (media.isPlaying) {
                        EqualizerBars(
                            isReducedMotion = isReducedMotion,
                            barColor = tertiaryColor
                        )
                    }
                }

                Text(
                    text = if (media.artist.isNotEmpty()) "${media.artist} · ${media.playerName}" else media.playerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Real MPRIS playback progress (rendered only when valid duration and position are available)
                val hasValidProgress = media.durationMs > 0L && media.positionMs >= 0L
                if (hasValidProgress) {
                    val progressFraction = (media.positionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
                    val playheadCoreColor = primaryColor
                    val playheadGlowColor = primaryColor.copy(alpha = 0.35f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(8.dp)
                            .then(
                                if (media.canSeek) {
                                    Modifier.clickable(role = Role.Button) {
                                        onSeek(media.positionMs)
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        // Active progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primaryColor, secondaryColor)
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
                        contentDescription = stringResource(R.string.mpris_previous),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // High-contrast Gradient Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(tokens.pauseGradientStart, tokens.pauseGradientEnd)
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
                        contentDescription = stringResource(
                            if (media.isPlaying) R.string.mpris_pause
                            else R.string.mpris_play
                        ),
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
                        contentDescription = stringResource(R.string.mpris_next),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(20.dp)
                    )
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