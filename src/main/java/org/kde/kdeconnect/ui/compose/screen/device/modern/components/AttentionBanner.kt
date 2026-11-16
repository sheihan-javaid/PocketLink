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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.DashboardCardKey
import org.kde.kdeconnect.ui.compose.LocalDashboardCardPalettes
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.AttentionUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardShapes
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalEffectsTier
import org.kde.kdeconnect.ui.compose.screen.device.modern.PermissionSeverity
import org.kde.kdeconnect.ui.compose.screen.device.modern.glassSurface
import org.kde.kdeconnect_tp.R

@Composable
fun AttentionBanner(
    attention: AttentionUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkThemeMode()
    val isReducedMotion = isReducedMotionMode()
    val tokens = LocalDashboardTokens.current
    val attentionPalette = LocalDashboardCardPalettes.current.getValue(DashboardCardKey.Attention)
    val isRequired = attention.severity == PermissionSeverity.Required

    val containerColor = if (isRequired) attentionPalette.container else tokens.optionalBadgeBg
    val titleColor = if (isRequired) attentionPalette.onContainer else tokens.optionalBadgeText
    val subtitleColor = if (isRequired) attentionPalette.onContainer.copy(alpha = 0.85f) else tokens.optionalBadgeText.copy(alpha = 0.85f)
    val strokeColor = if (isRequired) attentionPalette.accent else tokens.optionalBadgeText.copy(alpha = 0.35f)

    val infiniteTransition = rememberInfiniteTransition(label = "attentionPulse")
    val pulseScale by if (!isReducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconScale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val tier = LocalEffectsTier.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(attentionPalette, DashboardShapes.CardShape, isDark, tier)
            .clickable(role = Role.Button, onClick = onClick),
        shape = DashboardShapes.CardShape,
        border = null,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PocketLinkSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(titleColor.copy(alpha = if (isDark) 0.30f else 0.18f))
                        .border(1.dp, strokeColor.copy(alpha = if (isDark) 0.38f else 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRequired) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(PocketLinkSpacing.medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isRequired) {
                            if (attention.totalCount == 1) {
                                stringResource(R.string.attention_feature_needs_permission, attention.totalCount)
                            } else {
                                stringResource(R.string.attention_features_need_permission, attention.totalCount)
                            }
                        } else {
                            stringResource(R.string.attention_optional_permissions, attention.optionalCount)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = titleColor,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = stringResource(R.string.attention_tap_to_review),
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(PocketLinkSpacing.small))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
