/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kde.kdeconnect.ui.compose.DashboardCardKey
import org.kde.kdeconnect.ui.compose.LocalDashboardCardPalettes
import org.kde.kdeconnect.ui.compose.screen.device.modern.DesktopPreviewState
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect_tp.R

/**
 * Microsoft Phone Link-inspired compact MacBook desktop preview card.
 *
 * Displays:
 * - The real connected Mac desktop wallpaper when [DesktopPreviewState.Available] /
 *   [DesktopPreviewState.AvailableOffline] (decoded off-main-thread, downsampled to card size).
 * - A stylized macOS desktop canvas for [Waiting], [NotSupported], [NotApplicable].
 * - An optional "Enable wallpaper sync" CTA for [Disabled] via [onEnableClick].
 * - Subtle macOS menu bar, Dock representation, and live connection indicator.
 */
@Composable
fun MacDesktopPreview(
    state: DesktopPreviewState,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    previewWidth: androidx.compose.ui.unit.Dp = 104.dp,
    onEnableClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tokens = LocalDashboardTokens.current
    val previewPalette = LocalDashboardCardPalettes.current.getValue(DashboardCardKey.DesktopPreview)
    val isReducedMotion = isReducedMotionMode()
    var showInfoDialog by remember { mutableStateOf(false) }

    // Decode the cached wallpaper off the main thread, downsampled to the card size,
    // keyed on the wallpaper hash so it recomputes when the wallpaper changes.
    val wallpaperBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = state) {
        val wallpaperTarget = when (state) {
            is DesktopPreviewState.Available -> state.wallpaperFilePath to state.wallpaperSha256
            is DesktopPreviewState.AvailableOffline -> state.wallpaperFilePath to state.wallpaperSha256
            else -> null
        }
        val bitmap = wallpaperTarget?.let { (filePath, _) ->
            withContext(Dispatchers.IO) {
                val cardWidthPx = density.run { previewWidth.toPx().toInt() }
                val cardHeightPx = density.run { (previewWidth * 10f / 16f).toPx().toInt() }
                decodeWallpaper(filePath, cardWidthPx, cardHeightPx)
            }
        }
        value = bitmap
    }

    val infiniteTransition = rememberInfiniteTransition(label = "desktopPulse")
    val dotAlpha by if (!isReducedMotion && isConnected) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "previewDotPulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val showWallpaper = state is DesktopPreviewState.Available || state is DesktopPreviewState.AvailableOffline
    val contentDesc = stringResource(R.string.desktop_preview_desc)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Outer MacBook Aluminum Display Frame (16:10 aspect ratio)
        Box(
            modifier = Modifier
                .width(previewWidth)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(8.dp))
                .background(previewPalette.container)
                .clickable(role = Role.Button) {
                    when (state) {
                        is DesktopPreviewState.NotSupported,
                        is DesktopPreviewState.NotApplicable -> showInfoDialog = true
                        is DesktopPreviewState.Disabled -> onEnableClick?.invoke()
                        is DesktopPreviewState.Available,
                        is DesktopPreviewState.AvailableOffline,
                        is DesktopPreviewState.Waiting -> {}
                    }
                }
                .semantics {
                    role = Role.Button
                    contentDescription = contentDesc
                }
        ) {
            // Screen Bezel Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )
                    )
            ) {
                // Display Canvas: real wallpaper or stylized macOS desktop canvas
                if (showWallpaper && wallpaperBitmap != null) {
                    Image(
                        bitmap = wallpaperBitmap!!,
                        contentDescription = stringResource(R.string.desktop_preview_title),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Refined Apple-style violet/indigo macOS desktop gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        tokens.glow,
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_device_laptop_32dp),
                            contentDescription = null,
                            alpha = if (tokens.isDark) 0.35f else 0.45f,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Top macOS Menu Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tokens.cardBackground.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(tokens.textPrimary.copy(alpha = 0.75f))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(tokens.textSecondary.copy(alpha = 0.6f))
                        )
                        Box(
                            modifier = Modifier
                                .size(2.dp)
                                .clip(CircleShape)
                                .background(tokens.textSecondary.copy(alpha = 0.6f))
                        )
                    }
                }

                // Bottom Minimal macOS Dock Representation
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(tokens.cardBackground.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(tokens.deviceContainer)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }

                // Live Connection Status Dot in top-right
                val indicatorColor = if (isConnected) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = dotAlpha)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }

        // Laptop base stand highlight
        Box(
            modifier = Modifier
                .width(previewWidth + 10.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(tokens.cardBorderBottom)
        )

        // "Enable wallpaper sync" CTA when the plugin is disabled locally.
        if (state is DesktopPreviewState.Disabled && onEnableClick != null) {
            TextButton(
                onClick = onEnableClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.desktop_preview_cta_enable),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }

    // Companion Preview Information Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.desktop_preview_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = when (state) {
                        is DesktopPreviewState.NotSupported ->
                            stringResource(R.string.desktop_preview_unavailable_message)
                        is DesktopPreviewState.NotApplicable ->
                            stringResource(R.string.desktop_preview_unavailable_message)
                        else -> stringResource(R.string.desktop_preview_waiting)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(
                        text = stringResource(R.string.desktop_preview_dialog_dismiss),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = tokens.cardBackground
        )
    }
}

/**
 * Decodes [filePath] into an [ImageBitmap] downsampled to fit within [maxWidthPx] x [maxHeightPx],
 * keeping memory bounded for large wallpapers.
 */
private fun decodeWallpaper(filePath: String, maxWidthPx: Int, maxHeightPx: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(filePath, bounds)
    val outW = bounds.outWidth
    val outH = bounds.outHeight
    if (outW <= 0 || outH <= 0) return null

    var inSampleSize = 1
    while (outW / (inSampleSize * 2) >= maxWidthPx && outH / (inSampleSize * 2) >= maxHeightPx) {
        inSampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSampleSize }
    return BitmapFactory.decodeFile(filePath, decodeOpts)?.asImageBitmap()
}
