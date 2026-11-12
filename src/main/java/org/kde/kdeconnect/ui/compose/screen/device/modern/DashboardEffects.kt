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
import org.kde.kdeconnect.ui.compose.ModernLightColorScheme

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
 * Pure Material 3 and sampled-palette design tokens for the modern dashboard surfaces and components.
 * Matches reference screenshots with exact sampled hex values.
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
    val textDescription: Color = textSecondary,
    val sectionHeaderLabel: Color = textSecondary,
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
    val quickActionStroke: Color,
    // Screen gradient tokens
    val backgroundGradientTop: Color = if (isDark) Color(0xFF191731) else Color(0xFFE9E7F7),
    val backgroundGradientMid: Color = if (isDark) Color(0xFF0D0D1A) else Color(0xFFEEECF9),
    val backgroundGradientBottom: Color = if (isDark) Color(0xFF07080D) else Color(0xFFF4F3FB),
    // Accents
    val accentIndigo: Color = if (isDark) Color(0xFF7C6CFF) else Color(0xFF6254C7),
    val accentSoftIndigo: Color = if (isDark) Color(0xFFA99AEF) else Color(0xFF4338CA),
    val accentCyan: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF0F766E),
    // Battery Ring
    val ringArc: Color = Color(0xFF737BFF),
    val ringCap: Color = Color(0xFF3DDBFF),
    val ringTrack: Color = if (isDark) Color(0xFF1C1E2A) else Color(0xFFECEBFA),
    val pauseGradientStart: Color = Color(0xFF7773FF),
    val pauseGradientEnd: Color = Color(0xFF3FD6FF),
    // Status indicators
    val statusConnectedText: Color = if (isDark) Color(0xFF4ADE9F) else Color(0xFF166534),
    val statusConnectedBg: Color = if (isDark) Color(0xFF172E2C) else Color(0xFFDCFCE7),
    val statusDisconnectedText: Color = if (isDark) Color(0xFFFF6B7A) else Color(0xFF991B1B),
    val statusDisconnectedBg: Color = if (isDark) Color(0xFF3C222C) else Color(0xFFFEF2F2),
    val networkChipBg: Color = if (isDark) Color(0xFF2C2E36) else Color(0xFFF1F5F9),
    val networkChipText: Color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
    // Attention/Permission Banner
    val permissionBannerBg: Color = if (isDark) Color(0xFF2E151B) else Color(0xFFFEF2F2),
    val permissionBannerBorder: Color = if (isDark) Color(0xFF4D222B) else Color(0xFFFDBCBC),
    val permissionBannerTitle: Color = if (isDark) Color(0xFFFF7A88) else Color(0xFF991B1B),
    val permissionBannerSubtitle: Color = if (isDark) Color(0xFFE5989B) else Color(0xFFA83B3B),
    // Laptop icon tile
    val laptopTileBg: Color = if (isDark) Color(0xFF232445) else Color(0xFFECF0FF),
    val laptopTileIcon: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF6254C7),
    // Quick-action tiles (tile fill / icon colour)
    val qaSendFilesBg: Color = if (isDark) Color(0xFF232244) else Color(0xFFEBF0FF),
    val qaSendFilesIcon: Color = if (isDark) Color(0xFFA99AEF) else Color(0xFF4338CA),
    val qaClipboardBg: Color = if (isDark) Color(0xFF173644) else Color(0xFFEBFDF9),
    val qaClipboardIcon: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF0F766E),
    val qaRemoteInputBg: Color = if (isDark) Color(0xFF312245) else Color(0xFFF4F2FF),
    val qaRemoteInputIcon: Color = if (isDark) Color(0xFFE0A7FF) else Color(0xFF6D28D9),
    val qaRingMacBg: Color = if (isDark) Color(0xFF3B2F22) else Color(0xFFFFF5E8),
    val qaRingMacIcon: Color = if (isDark) Color(0xFFFFB547) else Color(0xFFC2410C),
    val qaRunCommandBg: Color = if (isDark) Color(0xFF1A3733) else Color(0xFFEDFDF2),
    val qaRunCommandIcon: Color = if (isDark) Color(0xFF4ADE9F) else Color(0xFF15803D),
    val qaPresenterBg: Color = if (isDark) Color(0xFF3C222C) else Color(0xFFFDF2FF),
    val qaPresenterIcon: Color = if (isDark) Color(0xFFFF6B7A) else Color(0xFFA21CAF),
    // List-row icon tiles (tile / icon)
    val rowShareBg: Color = if (isDark) Color(0xFF1F1E3C) else Color(0xFFF0FDFA),
    val rowShareIcon: Color = if (isDark) Color(0xFFA99AEF) else Color(0xFF0F766E),
    val rowControlBg: Color = if (isDark) Color(0xFF162F3C) else Color(0xFFF0FDFA),
    val rowControlIcon: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF0F766E),
    val rowDeviceInfoBg: Color = if (isDark) Color(0xFF182F2E) else Color(0xFFF0FDFA),
    val rowDeviceInfoIcon: Color = if (isDark) Color(0xFF4ADE9F) else Color(0xFF0F766E),
    // Action pills
    val actionPillBg: Color = if (isDark) Color(0xFF222238) else Color(0xFFEEF2FF),
    val actionPillText: Color = if (isDark) Color(0xFFA99AEF) else Color(0xFF4338CA),
    // Controls
    val volumeChipBg: Color = if (isDark) Color(0xFF162E3A) else Color(0xFFECE8F6),
    val volumeChipText: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF6254C7),
    val sliderFill: Color = if (isDark) Color(0xFF7C6CFF) else Color(0xFF6254C7),
    val sliderTrack: Color = if (isDark) Color(0xFF133845) else Color(0xFFECE8F6),
    val sliderThumbFill: Color = Color(0xFFFFFFFF),
    val sliderThumbRing: Color = if (isDark) Color(0xFF3DDBFF) else Color(0xFF6254C7),
    val toggleTrackOn: Color = if (isDark) Color(0xFF7C6CFF) else Color(0xFF6254C7),
    val toggleThumbOn: Color = Color(0xFFFFFFFF),
    val toggleTrackOff: Color = if (isDark) Color(0xFF232530) else Color(0xFFE6E0E9),
    val toggleThumbOff: Color = if (isDark) Color(0xFF798598) else Color(0xFF8C909C),
    val toggleBorderOff: Color = if (isDark) Color(0xFF2D2F37) else Color(0xFF8C909C),
    val sectionHeaderCount: Color = if (isDark) Color(0xFF798598) else Color(0xFF6A7586),
    val searchBarBg: Color = if (isDark) Color(0xFF0F111A) else Color(0xFFFFFFFF),
    val searchBarBorder: Color = if (isDark) Color(0xFF2D2F37) else Color(0xFFE2E8F0),
    val searchBarText: Color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
    val searchBarPlaceholder: Color = if (isDark) Color(0xFF798598) else Color(0xFF475569),
    val searchBarFocusRing: Color = if (isDark) Color(0xFF7C6CFF) else Color(0xFF6254C7)
)

fun createDashboardTokens(colorScheme: ColorScheme, isDark: Boolean): DashboardDesignTokens {
    val cardBackground = if (isDark) Color(0xFF0F111A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2D2F37) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val textDescription = if (isDark) Color(0xFF798598) else Color(0xFF64748B)
    val sectionHeaderLabel = if (isDark) Color(0xFF778496) else Color(0xFF6A7586)
    val divider = if (isDark) Color(0xFF2C2E36) else Color(0xFFE2E8F0)

    return DashboardDesignTokens(
        isDark = isDark,
        pageBackground = if (isDark) Color(0xFF0D0D1A) else Color(0xFFEEECF9),
        cardBackground = cardBackground,
        cardElevated = cardBackground,
        cardPressed = if (isDark) Color(0xFF141724) else Color(0xFFF1F5F9),
        cardBorderTop = cardBorder,
        cardBorderBottom = cardBorder,
        cardSheenTop = Color.Transparent, // No sheen or shadow per prompt
        divider = divider,
        glow = if (isDark) Color(0x30191731) else Color(0x20E9E7F7),
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textDescription,
        textDescription = textDescription,
        sectionHeaderLabel = sectionHeaderLabel,
        shareContainer = if (isDark) Color(0xFF1F1E3C) else Color(0xFFF0FDFA),
        onShareContainer = if (isDark) Color(0xFFA99AEF) else Color(0xFF0F766E),
        controlContainer = if (isDark) Color(0xFF162F3C) else Color(0xFFF0FDFA),
        onControlContainer = if (isDark) Color(0xFF3DDBFF) else Color(0xFF0F766E),
        mediaContainer = if (isDark) Color(0xFF1E1D3A) else Color(0xFFEEF2FF),
        onMediaContainer = if (isDark) Color(0xFFA99AEF) else Color(0xFF4338CA),
        deviceContainer = if (isDark) Color(0xFF182F2E) else Color(0xFFF0FDFA),
        onDeviceContainer = if (isDark) Color(0xFF4ADE9F) else Color(0xFF0F766E),
        permissionBadgeBg = if (isDark) Color(0xFF2E151B) else Color(0xFFFEF2F2),
        permissionBadgeText = if (isDark) Color(0xFFFF7A88) else Color(0xFF991B1B),
        optionalBadgeBg = if (isDark) Color(0xFF2D2538) else Color(0xFFF3E8FC),
        optionalBadgeText = if (isDark) Color(0xFFDABCF3) else Color(0xFF6D28D9),
        mediaScrim = if (isDark) Color(0xD00F111A) else Color(0xD0FFFFFF),
        quickActionChipBg = if (isDark) Color(0xFF1A1C28) else Color(0xFFF8FAFC),
        quickActionChipPressed = if (isDark) Color(0xFF242738) else Color(0xFFE2E8F0),
        quickActionStroke = cardBorder
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
