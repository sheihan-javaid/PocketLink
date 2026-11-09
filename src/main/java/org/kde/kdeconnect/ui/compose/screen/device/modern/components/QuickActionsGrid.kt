/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionAvailability
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionUiModel

@Composable
fun QuickActionsGrid(
    actions: List<QuickActionUiModel>,
    onActionClick: (QuickActionUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 480.dp) 3 else 2
        val spacing = KdeSpacing.medium
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
    val isDark = isDarkThemeMode()
    val isEnabled = action.availability !is QuickActionAvailability.Disabled
    val isNeedsPermission = action.availability is QuickActionAvailability.NeedsPermission

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileScale"
    )

    // Restrained per-category neon/vibrant hues with >= 3:1 icon vs chip contrast
    val (chipBgBrush, iconTintColor) = if (isDark) {
        when (action.id) {
            "send_files" -> Pair(
                Brush.linearGradient(listOf(Color(0x337C6CFF), Color(0x1A7C6CFF))),
                Color(0xFFA99AEF)
            )
            "clipboard" -> Pair(
                Brush.linearGradient(listOf(Color(0x333DDBFF), Color(0x1A3DDBFF))),
                Color(0xFF3DDBFF)
            )
            "remote_input" -> Pair(
                Brush.linearGradient(listOf(Color(0x33C46BFF), Color(0x1AC46BFF))),
                Color(0xFFE0A7FF)
            )
            "ring_device" -> Pair(
                Brush.linearGradient(listOf(Color(0x33FFB547), Color(0x1AFFB547))),
                Color(0xFFFFB547)
            )
            "run_command" -> Pair(
                Brush.linearGradient(listOf(Color(0x334ADE9F), Color(0x1A4ADE9F))),
                Color(0xFF4ADE9F)
            )
            "presenter" -> Pair(
                Brush.linearGradient(listOf(Color(0x33FF6B7A), Color(0x1AFF6B7A))),
                Color(0xFFFF6B7A)
            )
            else -> Pair(
                Brush.linearGradient(listOf(Color(0x337C6CFF), Color(0x1A7C6CFF))),
                Color(0xFFA99AEF)
            )
        }
    } else {
        // Light mode: High-contrast rich color tokens (contrast > 5:1 against chip)
        when (action.id) {
            "send_files" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))),
                Color(0xFF4338CA)
            )
            "clipboard" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1))),
                Color(0xFF0F766E)
            )
            "remote_input" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE))),
                Color(0xFF6D28D9)
            )
            "ring_device" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))),
                Color(0xFFC2410C)
            )
            "run_command" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))),
                Color(0xFF15803D)
            )
            "presenter" -> Pair(
                Brush.linearGradient(listOf(Color(0xFFFDF4FF), Color(0xFFFAE8FF))),
                Color(0xFFA21CAF)
            )
            else -> Pair(
                Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))),
                Color(0xFF4338CA)
            )
        }
    }

    val cardBg = if (isDark) {
        if (!isEnabled) Color(0xFF11131C)
        else if (isNeedsPermission) Color(0xFF231417)
        else Color(0xFF0F111A)
    } else {
        if (!isEnabled) Color(0xFFF1F5F9)
        else if (isNeedsPermission) Color(0xFFFEF2F2)
        else Color(0xFFFFFFFF)
    }

    val borderBrush = if (isDark) {
        if (isNeedsPermission) Brush.verticalGradient(listOf(Color(0xFF8C1D18).copy(alpha = 0.6f), Color(0xFF8C1D18).copy(alpha = 0.2f)))
        else if (isPressed) Brush.verticalGradient(listOf(Color(0xFF7C6CFF), Color(0xFF3DDBFF)))
        else Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x08FFFFFF)))
    } else {
        if (isNeedsPermission) Brush.verticalGradient(listOf(Color(0xFFFCA5A5), Color(0xFFFCA5A5)))
        else if (isPressed) Brush.verticalGradient(listOf(Color(0xFF6254C7), Color(0xFFA99AEF)))
        else Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9)))
    }

    val textColor = when {
        !isEnabled -> if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
        isNeedsPermission -> if (isDark) Color(0xFFFFB4AB) else Color(0xFF991B1B)
        else -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }

    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 88.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .semantics { role = Role.Button }
            .then(
                if (isEnabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderBrush),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KdeSpacing.medium, horizontal = KdeSpacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBgBrush),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(action.iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTintColor),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = KdeSpacing.small)
            )

            if (isNeedsPermission) {
                Text(
                    text = "Permission",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color(0xFFFF6B7A) else Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
