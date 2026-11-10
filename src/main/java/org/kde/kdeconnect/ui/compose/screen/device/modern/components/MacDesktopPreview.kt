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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect_tp.R
import java.io.File

/**
 * Microsoft Phone Link-inspired compact MacBook desktop preview card.
 *
 * Displays a refined MacBook desktop frame with:
 * - Real connected Mac desktop wallpaper if cached/transferred from companion
 * - Honest, elegant unavailable state when companion protocol does not broadcast wallpaper
 * - Subtle macOS menu bar
 * - Minimal macOS Dock representation
 * - Live connection indicator
 */
@Composable
fun MacDesktopPreview(
    deviceName: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    previewWidth: androidx.compose.ui.unit.Dp = 104.dp
) {
    val context = LocalContext.current
    val tokens = LocalDashboardTokens.current
    val isReducedMotion = isReducedMotionMode()
    var showInfoDialog by remember { mutableStateOf(false) }

    // Check for cached wallpaper file asynchronously
    val cachedWallpaper by produceState<ImageBitmap?>(initialValue = null, key1 = deviceName) {
        value = withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "mac_wallpaper.png")
            val filesFile = File(context.filesDir, "mac_wallpaper.png")
            val target = when {
                cacheFile.exists() && cacheFile.length() > 0 -> cacheFile
                filesFile.exists() && filesFile.length() > 0 -> filesFile
                else -> null
            }
            target?.let {
                try {
                    BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
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
                .background(tokens.cardBackground)
                .clickable(role = Role.Button) {
                    showInfoDialog = true
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
                // Display Canvas: Wallpaper or Minimal macOS Desktop Canvas
                if (cachedWallpaper != null) {
                    Image(
                        bitmap = cachedWallpaper!!,
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
                    // Miniature Apple logo / dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(tokens.textPrimary.copy(alpha = 0.75f))
                    )

                    // Miniature status indicator dots
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
                        // 5 miniature rounded squircle Dock app icons in palette hues
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
    }

    // Honest Companion Preview Information Dialog
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
                    text = stringResource(R.string.desktop_preview_unavailable_message),
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

