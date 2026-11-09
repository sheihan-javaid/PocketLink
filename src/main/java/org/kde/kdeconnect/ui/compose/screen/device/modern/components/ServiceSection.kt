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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.PluginCategory
import org.kde.kdeconnect.ui.compose.screen.device.modern.PluginCategoryRegistry
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceRowUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceSectionUiModel

@Composable
fun ServiceSection(
    section: ServiceSectionUiModel,
    onPluginButtonClick: (Plugin.PluginUiButton) -> Unit,
    modifier: Modifier = Modifier
) {
    if (section.rows.isEmpty()) return

    val isDark = isDarkThemeMode()
    val totalInCategory = PluginCategoryRegistry.totalCountFor(section.category)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KdeSpacing.small, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = section.title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "${section.rows.size} of $totalInCategory",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        AuroraGlassCard {
            section.rows.forEachIndexed { index, row ->
                ServiceRow(
                    row = row,
                    category = section.category,
                    isDark = isDark,
                    onPluginButtonClick = onPluginButtonClick
                )
                if (index < section.rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5F9)
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
    isDark: Boolean,
    onPluginButtonClick: (Plugin.PluginUiButton) -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconBg, iconTint) = when (category) {
        PluginCategory.ShareTransfer -> Pair(
            if (isDark) Color(0x267C6CFF) else Color(0xFFEEF2FF),
            if (isDark) Color(0xFFA99AEF) else Color(0xFF4338CA)
        )
        PluginCategory.ControlComputer -> Pair(
            if (isDark) Color(0x263DDBFF) else Color(0xFFF0FDFA),
            if (isDark) Color(0xFF3DDBFF) else Color(0xFF0F766E)
        )
        PluginCategory.MediaAudio -> Pair(
            if (isDark) Color(0x26C46BFF) else Color(0xFFF5F3FF),
            if (isDark) Color(0xFFE0A7FF) else Color(0xFF6D28D9)
        )
        PluginCategory.DeviceInfo -> Pair(
            if (isDark) Color(0x264ADE9F) else Color(0xFFF0FDF4),
            if (isDark) Color(0xFF4ADE9F) else Color(0xFF15803D)
        )
    }

    val firstButton = row.buttons.firstOrNull()
    val isRowClickable = firstButton != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (firstButton != null) {
                    Modifier.clickable(role = Role.Button) {
                        onPluginButtonClick(firstButton)
                    }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (row.iconRes != null) {
                    Image(
                        painter = painterResource(row.iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconTint),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = row.title.firstOrNull()?.uppercase() ?: "•",
                        style = MaterialTheme.typography.titleMedium,
                        color = iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.width(KdeSpacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KdeSpacing.small)
                ) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (row.needsPermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Permission",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
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
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (row.buttons.isNotEmpty()) {
            Spacer(modifier = Modifier.width(KdeSpacing.small))
            FilledTonalButton(
                onClick = { onPluginButtonClick(row.buttons.first()) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .padding(start = KdeSpacing.xSmall)
            ) {
                Text(
                    text = row.buttons.first().name,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
