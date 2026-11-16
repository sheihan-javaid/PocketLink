/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.DashboardCardKey
import org.kde.kdeconnect.ui.compose.LocalDashboardCardPalettes
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardShapes
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalEffectsTier
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionAvailability
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.dashboardCardBorder
import org.kde.kdeconnect.ui.compose.screen.device.modern.glassSurface
import org.kde.kdeconnect.ui.compose.screen.device.modern.springPressScale
import org.kde.kdeconnect_tp.R

@Composable
fun QuickActionsGrid(
    actions: List<QuickActionUiModel>,
    onActionClick: (QuickActionUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 480.dp) 3 else 2
        val spacing = PocketLinkSpacing.medium
        val itemWidth = ((maxWidth - (spacing * (columns - 1))) / columns) - 2.dp

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            maxItemsInEachRow = columns
        ) {
            actions.forEach { action ->
                QuickActionTile(
                    action = action,
                    onClick = { onActionClick(action) },
                    modifier = Modifier.width(itemWidth)
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: QuickActionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val tokens = LocalDashboardTokens.current
    val qaPalette = LocalDashboardCardPalettes.current.getValue(DashboardCardKey.QuickActions)
    val tier = LocalEffectsTier.current
    val isEnabled = action.availability !is QuickActionAvailability.Disabled
    val isNeedsPermission = action.availability is QuickActionAvailability.NeedsPermission

    val interactionSource = remember { MutableInteractionSource() }

    val tileAccent = when (action.id) {
        "send_files" -> tokens.qaSendFilesIcon
        "clipboard" -> tokens.qaClipboardIcon
        "remote_input" -> tokens.qaRemoteInputIcon
        "ring_device" -> tokens.qaRingMacIcon
        "run_command" -> tokens.qaRunCommandIcon
        "presenter" -> tokens.qaPresenterIcon
        else -> qaPalette.accent
    }
    val tilePalette = remember(qaPalette, tileAccent) { qaPalette.copy(accent = tileAccent) }
    val chipBg = tileAccent.copy(alpha = if (tokens.isDark) 0.22f else 0.14f)
    val chipBorderColor = tileAccent.copy(alpha = if (tokens.isDark) 0.38f else 0.25f)

    val textColor = when {
        !isEnabled -> tokens.textTertiary
        isNeedsPermission -> tokens.permissionBadgeText
        else -> qaPalette.onContainer
    }

    val tileModifier = if (!isEnabled) {
        modifier
            .defaultMinSize(minHeight = 88.dp)
            .clip(DashboardShapes.TileShape)
            .background(tokens.cardPressed, DashboardShapes.TileShape)
    } else {
        modifier
            .defaultMinSize(minHeight = 88.dp)
            .springPressScale(interactionSource = interactionSource, effectsTier = tier)
            .glassSurface(tilePalette, DashboardShapes.TileShape, tokens.isDark, tier)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    }

    Card(
        modifier = tileModifier,
        shape = DashboardShapes.TileShape,
        border = if (isNeedsPermission) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = PocketLinkSpacing.medium, horizontal = PocketLinkSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(DashboardShapes.IconCircleShape)
                    .background(chipBg)
                    .border(1.dp, chipBorderColor, DashboardShapes.IconCircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(action.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tileAccent),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = action.labelRes?.let { stringResource(it) } ?: action.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PocketLinkSpacing.small)
            )

            if (isNeedsPermission) {
                Text(
                    text = stringResource(R.string.permission_badge),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = tokens.permissionBadgeText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
