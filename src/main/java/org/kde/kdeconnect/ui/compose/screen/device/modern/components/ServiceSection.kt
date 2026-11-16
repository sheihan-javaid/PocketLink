/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.DashboardCardKey
import org.kde.kdeconnect.ui.compose.DashboardCardPalette
import org.kde.kdeconnect.ui.compose.LocalDashboardCardPalettes
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.PluginCategory
import org.kde.kdeconnect.ui.compose.screen.device.modern.PluginCategoryRegistry
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceRowUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceSectionUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.VolumeControlUiState
import org.kde.kdeconnect_tp.R

/**
 * Premium One UI-style grouped service card representing one of the major functional domains.
 * Renders with 26dp rounded corners, tonal container fill, 40dp circular tinted domain icons,
 * 68dp inset dividers, and min 48dp touch targets.
 */
@Composable
fun ServiceSection(
    section: ServiceSectionUiModel,
    onPluginButtonClick: (Plugin.PluginUiButton) -> Unit,
    onPermissionClick: ((pluginKey: String, isRequired: Boolean) -> Unit)? = null,
    mediaState: MediaPlaybackUiState? = null,
    volumeState: VolumeControlUiState? = null,
    onMediaPlayPause: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaSeek: (Long) -> Unit = {},
    onVolumeChange: (Int) -> Unit = {},
    onVolumeToggleMute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMediaCategory = section.category == PluginCategory.MediaAudio
    val hasMediaOrVolume = isMediaCategory && (mediaState != null || volumeState != null)

    // Hide empty card if there are no rows and no media/volume content
    if (section.rows.isEmpty() && !hasMediaOrVolume) return

    val tokens = LocalDashboardTokens.current
    val totalInCategory = PluginCategoryRegistry.totalCountFor(section.category)

    val cardKey = when (section.category) {
        PluginCategory.ShareTransfer -> DashboardCardKey.ShareTransfer
        PluginCategory.ControlComputer -> DashboardCardKey.ControlComputer
        PluginCategory.MediaAudio -> DashboardCardKey.MediaAudio
        PluginCategory.DeviceInfo -> DashboardCardKey.DeviceInfo
    }
    val palette = LocalDashboardCardPalettes.current.getValue(cardKey)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = (section.titleRes?.let { stringResource(it) } ?: section.title).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = tokens.sectionHeaderLabel,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.category_item_count_format, section.rows.size, totalInCategory),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                color = tokens.sectionHeaderLabel
            )
        }

        AuroraGlassCard(
            cardKey = cardKey
        ) {
            // In Media & Audio card, integrate Contextual Media controls & Volume controls directly
            if (isMediaCategory && mediaState != null) {
                ContextualMediaContent(
                    media = mediaState,
                    onPlayPause = onMediaPlayPause,
                    onNext = onMediaNext,
                    onPrevious = onMediaPrevious,
                    onSeek = onMediaSeek,
                    modifier = Modifier.padding(16.dp)
                )
                if (volumeState != null || section.rows.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = tokens.divider
                    )
                }
            }

            if (isMediaCategory && volumeState != null) {
                ContextualVolumeContent(
                    volumeState = volumeState,
                    onVolumeChange = onVolumeChange,
                    onVolumeToggleMute = onVolumeToggleMute,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                if (section.rows.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = tokens.divider
                    )
                }
            }

            section.rows.forEachIndexed { index, row ->
                ServiceRow(
                    row = row,
                    category = section.category,
                    palette = palette,
                    onPluginButtonClick = onPluginButtonClick,
                    onPermissionClick = onPermissionClick
                )
                if (index < section.rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = tokens.divider
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(
    row: ServiceRowUiModel,
    category: PluginCategory,
    palette: DashboardCardPalette? = null,
    onPluginButtonClick: (Plugin.PluginUiButton) -> Unit,
    onPermissionClick: ((pluginKey: String, isRequired: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDashboardTokens.current

    val iconTint = palette?.accent ?: when (category) {
        PluginCategory.ShareTransfer -> tokens.rowShareIcon
        PluginCategory.ControlComputer -> tokens.rowControlIcon
        PluginCategory.MediaAudio -> tokens.onMediaContainer
        PluginCategory.DeviceInfo -> tokens.rowDeviceInfoIcon
    }
    val iconBg = palette?.accent?.copy(alpha = if (tokens.isDark) 0.18f else 0.12f) ?: tokens.actionPillBg

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(
                if (row.buttons.size == 1 && !row.needsPermission) {
                    Modifier.clickable(role = Role.Button) {
                        onPluginButtonClick(row.buttons.first())
                    }
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Leading 40dp icon in tinted circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (row.iconRes != null) {
                        Image(
                            painter = painterResource(row.iconRes),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconTint),
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = row.title.firstOrNull()?.uppercase() ?: "•",
                            style = MaterialTheme.typography.titleMedium,
                            color = iconTint
                        )
                    }
                }

                Spacer(modifier = Modifier.width(PocketLinkSpacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
                    ) {
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = palette?.onContainer ?: tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (row.needsPermission) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tokens.permissionBadgeBg)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                    .then(
                                        if (onPermissionClick != null) {
                                            Modifier.clickable(role = Role.Button) {
                                                onPermissionClick(row.pluginKey, row.isRequiredPermission)
                                            }
                                        } else Modifier
                                    )
                            ) {
                                Text(
                                    text = stringResource(R.string.permission_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.permissionBadgeText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = row.supportingValue ?: row.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.supportingValue != null) MaterialTheme.colorScheme.primary
                        else tokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Single trailing button when no permissions needed
            if (row.buttons.size == 1 && !row.needsPermission) {
                Spacer(modifier = Modifier.width(PocketLinkSpacing.small))
                FilledTonalButton(
                    onClick = { onPluginButtonClick(row.buttons.first()) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .padding(start = PocketLinkSpacing.xSmall)
                ) {
                    Text(
                        text = row.buttons.first().name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        // Render every button when multiple buttons exist or when permission resolution action is needed
        if (row.buttons.size > 1 || (row.needsPermission && (row.buttons.isNotEmpty() || onPermissionClick != null))) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small),
                verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
            ) {
                if (row.needsPermission && onPermissionClick != null) {
                    FilledTonalButton(
                        onClick = { onPermissionClick(row.pluginKey, row.isRequiredPermission) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = tokens.permissionBadgeBg,
                            contentColor = tokens.permissionBadgeText
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (row.isRequiredPermission) R.string.manage_plugins_grant_permission
                                else R.string.manage_plugins_optional_permission
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                if (row.buttons.size > 1) {
                    row.buttons.forEach { button ->
                        FilledTonalButton(
                            onClick = { onPluginButtonClick(button) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = button.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                } else if (row.buttons.size == 1 && row.needsPermission) {
                    val button = row.buttons.first()
                    FilledTonalButton(
                        onClick = { onPluginButtonClick(button) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = button.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
