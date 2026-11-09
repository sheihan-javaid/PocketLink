/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardShapes
import org.kde.kdeconnect.ui.compose.screen.device.modern.EffectsTier
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalEffectsTier
import org.kde.kdeconnect.ui.compose.screen.device.modern.dashboardCardBorder

@Composable
fun isDarkThemeMode(): Boolean {
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

@Composable
fun isReducedMotionMode(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Premium One UI-style grouped surface container card.
 * Features tonal surface fill, a 1dp top-edge highlight stroke, soft sheen, and 26dp rounded corners.
 * Strictly uses Material 3 colorScheme tokens with zero hardcoded colors.
 */
@Composable
fun AuroraGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = DashboardShapes.CardShape,
    isElevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalDashboardTokens.current
    val tier = LocalEffectsTier.current
    val isDark = tokens.isDark

    val containerColor = if (isElevated) tokens.cardElevated else tokens.cardBackground
    val border = dashboardCardBorder(tokens, tier)
    val elevation: Dp = if (isDark) 0.dp else if (isElevated) 2.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        border = border,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (tier != EffectsTier.Minimal) {
                        // Subtle top gloss sheen (vertical fade over top 40% of the card)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(tokens.cardSheenTop, Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.40f
                            ),
                            size = Size(size.width, size.height * 0.40f)
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}
