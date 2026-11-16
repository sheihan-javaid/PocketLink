/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class DashboardCardKey {
    Hero,
    DesktopPreview,
    QuickActions,
    ShareTransfer,
    ControlComputer,
    MediaAudio,
    DeviceInfo,
    ManagePlugins,
    Attention,
    ErrorPanel
}

@Immutable
data class DashboardCardPalette(
    val cardKey: DashboardCardKey,
    val container: Color,
    val onContainer: Color,
    val accent: Color
)

val ModernLightCardPalettes: Map<DashboardCardKey, DashboardCardPalette> = mapOf(
    DashboardCardKey.Hero to DashboardCardPalette(
        cardKey = DashboardCardKey.Hero,
        container = Color(0xFFEFEDF9),
        onContainer = Color(0xFF0F172A),
        accent = Color(0xFF4F3FC0)
    ),
    DashboardCardKey.DesktopPreview to DashboardCardPalette(
        cardKey = DashboardCardKey.DesktopPreview,
        container = Color(0xFFECEDF1),
        onContainer = Color(0xFF0F172A),
        accent = Color(0xFF334155)
    ),
    DashboardCardKey.QuickActions to DashboardCardPalette(
        cardKey = DashboardCardKey.QuickActions,
        container = Color(0xFFEFEEFC),
        onContainer = Color(0xFF0F172A),
        accent = Color(0xFF4F46E5)
    ),
    DashboardCardKey.ShareTransfer to DashboardCardPalette(
        cardKey = DashboardCardKey.ShareTransfer,
        container = Color(0xFFE9F1F3),
        onContainer = Color(0xFF042F2E),
        accent = Color(0xFF0F766E)
    ),
    DashboardCardKey.ControlComputer to DashboardCardPalette(
        cardKey = DashboardCardKey.ControlComputer,
        container = Color(0xFFF1EBFB),
        onContainer = Color(0xFF2E1065),
        accent = Color(0xFF6D28D9)
    ),
    DashboardCardKey.MediaAudio to DashboardCardPalette(
        cardKey = DashboardCardKey.MediaAudio,
        container = Color(0xFFF7EAF1),
        onContainer = Color(0xFF500724),
        accent = Color(0xFFBE185D)
    ),
    DashboardCardKey.DeviceInfo to DashboardCardPalette(
        cardKey = DashboardCardKey.DeviceInfo,
        container = Color(0xFFE9F2F1),
        onContainer = Color(0xFF022C22),
        accent = Color(0xFF047857)
    ),
    DashboardCardKey.ManagePlugins to DashboardCardPalette(
        cardKey = DashboardCardKey.ManagePlugins,
        container = Color(0xFFE8F0F7),
        onContainer = Color(0xFF082F49),
        accent = Color(0xFF0369A1)
    ),
    DashboardCardKey.Attention to DashboardCardPalette(
        cardKey = DashboardCardKey.Attention,
        container = Color(0xFFF4EDEB),
        onContainer = Color(0xFF451A03),
        accent = Color(0xFF92400E)
    ),
    DashboardCardKey.ErrorPanel to DashboardCardPalette(
        cardKey = DashboardCardKey.ErrorPanel,
        container = Color(0xFFF7EAEC),
        onContainer = Color(0xFF450A0A),
        accent = Color(0xFFB91C1C)
    )
)

val ModernDarkCardPalettes: Map<DashboardCardKey, DashboardCardPalette> = mapOf(
    DashboardCardKey.Hero to DashboardCardPalette(
        cardKey = DashboardCardKey.Hero,
        container = Color(0xFF1B192C),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFFB7A1FF)
    ),
    DashboardCardKey.DesktopPreview to DashboardCardPalette(
        cardKey = DashboardCardKey.DesktopPreview,
        container = Color(0xFF1D1D2A),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFFCBD5E1)
    ),
    DashboardCardKey.QuickActions to DashboardCardPalette(
        cardKey = DashboardCardKey.QuickActions,
        container = Color(0xFF1A1A2C),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFFA5B4FC)
    ),
    DashboardCardKey.ShareTransfer to DashboardCardPalette(
        cardKey = DashboardCardKey.ShareTransfer,
        container = Color(0xFF101D27),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFF2DD4BF)
    ),
    DashboardCardKey.ControlComputer to DashboardCardPalette(
        cardKey = DashboardCardKey.ControlComputer,
        container = Color(0xFF1C172C),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFFC084FC)
    ),
    DashboardCardKey.MediaAudio to DashboardCardPalette(
        cardKey = DashboardCardKey.MediaAudio,
        container = Color(0xFF201526),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFFF472B6)
    ),
    DashboardCardKey.DeviceInfo to DashboardCardPalette(
        cardKey = DashboardCardKey.DeviceInfo,
        container = Color(0xFF111D24),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFF34D399)
    ),
    DashboardCardKey.ManagePlugins to DashboardCardPalette(
        cardKey = DashboardCardKey.ManagePlugins,
        container = Color(0xFF14192C),
        onContainer = Color(0xFFFFFFFF),
        accent = Color(0xFF60A5FA)
    ),
    DashboardCardKey.Attention to DashboardCardPalette(
        cardKey = DashboardCardKey.Attention,
        container = Color(0xFF211B1B),
        onContainer = Color(0xFFFEF3C7),
        accent = Color(0xFFFBBF24)
    ),
    DashboardCardKey.ErrorPanel to DashboardCardPalette(
        cardKey = DashboardCardKey.ErrorPanel,
        container = Color(0xFF211521),
        onContainer = Color(0xFFFEE2E2),
        accent = Color(0xFFF87171)
    )
)

val LocalDashboardCardPalettes = staticCompositionLocalOf<Map<DashboardCardKey, DashboardCardPalette>> {
    ModernLightCardPalettes
}

@Composable
fun PocketLinkTheme(
    context: Context,
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        when (darkTheme) {
            true -> dynamicDarkColorScheme(context)
            false -> dynamicLightColorScheme(context)
        }
    } else {
        when (darkTheme) {
            true -> ModernDarkColorScheme
            false -> ModernLightColorScheme
        }
    }
    val semanticColors = if (darkTheme) PocketLinkDarkSemanticColors else PocketLinkLightSemanticColors
    val cardPalettes = if (darkTheme) ModernDarkCardPalettes else ModernLightCardPalettes

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalPocketLinkSemanticColors provides semanticColors,
        LocalDashboardCardPalettes provides cardPalettes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PocketLinkTypography,
            shapes = PocketLinkShapes,
            content = content,
        )
    }
}

val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF5B4AC7),                // Brand indigo accent (> 5.5:1 on white)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBE5FB),       // Soft lavender container
    onPrimaryContainer = Color(0xFF1B0F5C),     // Deep indigo text (> 11:1 contrast)
    secondary = Color(0xFF724B8B),              // Rich violet accent
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E8FC),     // Delicate violet-tinted container
    onSecondaryContainer = Color(0xFF2B103F),   // Deep plum text (> 10:1 contrast)
    tertiary = Color(0xFF5E4B99),               // Royal purple accent
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE8FA),      // Subtle lilac container
    onTertiaryContainer = Color(0xFF1E1045),    // Deep violet text (> 11:1 contrast)
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),         // Soft red container light
    onErrorContainer = Color(0xFF410002),       // Dark crimson text light (> 7.4:1 contrast)
    background = Color(0xFFF7F5FC),             // Soft lavender-tinted pearl background
    onBackground = Color(0xFF191528),
    surface = Color(0xFFFFFFFF),                // Pure white card surface
    onSurface = Color(0xFF191528),              // Crisp high-contrast dark indigo-charcoal (> 16:1)
    surfaceVariant = Color(0xFFECE7F6),         // Subtle lavender surface variant
    onSurfaceVariant = Color(0xFF494358),       // Accessible muted slate-violet (> 6.2:1)
    outline = Color(0xFF7A7389),
    outlineVariant = Color(0xFFD6D0E4),         // Soft lavender border
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDD7E8),
    surfaceContainer = Color(0xFFF2EEFA),       // Lavender card container tier
    surfaceContainerHigh = Color(0xFFEAE4F5),
    surfaceContainerHighest = Color(0xFFE2DBF0),
    surfaceContainerLow = Color(0xFFF8F6FD),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA896FE),                // Luminous lavender-indigo on dark (> 7.5:1 on #181524)
    onPrimary = Color(0xFF1E0E5E),
    primaryContainer = Color(0xFF382478),       // Deep indigo container
    onPrimaryContainer = Color(0xFFEBE5FB),     // High-contrast soft lavender text (> 9:1)
    secondary = Color(0xFFDABCF3),              // Rich violet accent (> 9.5:1 on #181524)
    onSecondary = Color(0xFF37184D),
    secondaryContainer = Color(0xFF4C2B64),     // Deep violet container
    onSecondaryContainer = Color(0xFFF3E8FC),   // Soft violet text (> 8.5:1)
    tertiary = Color(0xFFCBBDF6),               // Lilac-indigo accent (> 9:1 on #181524)
    onTertiary = Color(0xFF281850),
    tertiaryContainer = Color(0xFF3F2D6B),      // Royal violet container
    onTertiaryContainer = Color(0xFFEDE8FA),    // Delicate lilac text (> 8:1)
    error = Color(0xFFFFB4AB),                  // Accessible coral/red
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),         // Deep red container
    onErrorContainer = Color(0xFFFFDAD6),       // Soft readable coral text (> 11:1)
    background = Color(0xFF0E0C15),             // Deep charcoal with subtle dark violet tint
    onBackground = Color(0xFFF4F0FA),
    surface = Color(0xFF181524),                // Dark violet-charcoal card surface
    onSurface = Color(0xFFF4F0FA),              // Crisp readable text (> 14.5:1)
    surfaceVariant = Color(0xFF262137),         // Elevated surface variant
    onSurfaceVariant = Color(0xFFC8C2D8),       // High contrast muted text (> 9:1)
    outline = Color(0xFF918A9E),
    outlineVariant = Color(0x38B8A7E8),         // 22% lavender hairline border
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF36304A),
    surfaceDim = Color(0xFF120F1C),
    surfaceContainer = Color(0xFF1E1A2D),       // Dark violet surface container
    surfaceContainerHigh = Color(0xFF27223A),
    surfaceContainerHighest = Color(0xFF312B47),
    surfaceContainerLow = Color(0xFF151220),
    surfaceContainerLowest = Color(0xFF0B0912)
)
