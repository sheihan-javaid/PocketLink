/*
 * SPDX-FileCopyrightText: 2026 Antigravity
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Visual effects execution tier based on device hardware capabilities and accessibility settings.
 */
enum class EffectsTier {
    Full,     // API 31+ with blur, glossy highlights, smooth animations
    Reduced,  // Fallback for API < 31 or low RAM: gradients without blur
    Minimal   // Zero motion, static flat tonal fills (when animations disabled or power save)
}

fun resolveEffectsTier(
    sdk: Int,
    powerSave: Boolean,
    animatorScale: Float,
    lowRam: Boolean
): EffectsTier = when {
    animatorScale == 0f || powerSave -> EffectsTier.Minimal
    sdk < Build.VERSION_CODES.S || lowRam -> EffectsTier.Reduced
    else -> EffectsTier.Full
}

val LocalEffectsTier = staticCompositionLocalOf { EffectsTier.Full }

/**
 * Pure Material 3 derived design tokens for the modern dashboard surfaces and components.
 * Zero hardcoded Color(0x...) hexes. All colors derive strictly from M3 ColorScheme roles.
 */
@Immutable
data class DashboardDesignTokens(
    val isDark: Boolean,
    val pageBackground: Color,
    val cardBackground: Color,
    val cardElevated: Color,
    val cardPressed: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val cardSheenTop: Color,
    val divider: Color,
    val glow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val shareContainer: Color,
    val onShareContainer: Color,
    val controlContainer: Color,
    val onControlContainer: Color,
    val mediaContainer: Color,
    val onMediaContainer: Color,
    val deviceContainer: Color,
    val onDeviceContainer: Color,
    val permissionBadgeBg: Color,
    val permissionBadgeText: Color,
    val optionalBadgeBg: Color,
    val optionalBadgeText: Color,
    val mediaScrim: Color,
    val quickActionChipBg: Color,
    val quickActionChipPressed: Color,
    val quickActionStroke: Color
)

fun createDashboardTokens(colorScheme: ColorScheme, isDark: Boolean): DashboardDesignTokens {
    return DashboardDesignTokens(
        isDark = isDark,
        pageBackground = if (isDark) colorScheme.surfaceContainerLowest else colorScheme.surfaceContainer,
        cardBackground = if (isDark) colorScheme.surfaceContainer else colorScheme.surfaceContainerLowest,
        cardElevated = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surface,
        cardPressed = if (isDark) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainerLow,
        cardBorderTop = if (isDark) colorScheme.onSurface.copy(alpha = 0.14f) else colorScheme.onSurface.copy(alpha = 0.08f),
        cardBorderBottom = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f),
        cardSheenTop = if (isDark) colorScheme.onSurface.copy(alpha = 0.05f) else colorScheme.surfaceBright.copy(alpha = 0.45f),
        divider = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.55f),
        glow = colorScheme.primary.copy(alpha = if (isDark) 0.20f else 0.08f),
        textPrimary = colorScheme.onSurface,
        textSecondary = colorScheme.onSurfaceVariant,
        textTertiary = colorScheme.outline,
        shareContainer = colorScheme.primaryContainer,
        onShareContainer = colorScheme.onPrimaryContainer,
        controlContainer = colorScheme.secondaryContainer,
        onControlContainer = colorScheme.onSecondaryContainer,
        mediaContainer = colorScheme.tertiaryContainer,
        onMediaContainer = colorScheme.onTertiaryContainer,
        deviceContainer = colorScheme.surfaceVariant,
        onDeviceContainer = colorScheme.onSurfaceVariant,
        permissionBadgeBg = colorScheme.errorContainer,
        permissionBadgeText = colorScheme.onErrorContainer,
        optionalBadgeBg = colorScheme.secondaryContainer,
        optionalBadgeText = colorScheme.onSecondaryContainer,
        mediaScrim = colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
        quickActionChipBg = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainerLowest,
        quickActionChipPressed = if (isDark) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainerLow,
        quickActionStroke = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.4f else 0.6f)
    )
}

val LocalDashboardTokens = staticCompositionLocalOf {
    createDashboardTokens(ModernLightColorScheme, isDark = false)
}

object DashboardShapes {
    val CardShape = RoundedCornerShape(26.dp)
    val RowTopShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val RowBottomShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
    val RowSingleShape = RoundedCornerShape(26.dp)
    val TileShape = RoundedCornerShape(18.dp)
    val IconCircleShape = RoundedCornerShape(12.dp)
}

fun Modifier.springPressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.97f,
    effectsTier: EffectsTier = EffectsTier.Full
): Modifier = composed {
    if (effectsTier == EffectsTier.Minimal) {
        this
    } else {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) targetScale else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pressScale"
        )
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
}

fun dashboardCardBorder(tokens: DashboardDesignTokens, tier: EffectsTier = EffectsTier.Full): BorderStroke {
    return if (tier == EffectsTier.Minimal) {
        BorderStroke(1.dp, tokens.cardBorderBottom)
    } else {
        BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(tokens.cardBorderTop, tokens.cardBorderBottom)
            )
        )
    }
}
