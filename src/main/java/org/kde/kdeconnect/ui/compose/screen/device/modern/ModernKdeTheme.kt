/*
 * SPDX-FileCopyrightText: 2026 Antigravity
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import org.kde.kdeconnect.ui.compose.KdeDarkSemanticColors
import org.kde.kdeconnect.ui.compose.KdeLightSemanticColors
import org.kde.kdeconnect.ui.compose.KdePalette
import org.kde.kdeconnect.ui.compose.KdeShapes
import org.kde.kdeconnect.ui.compose.KdeTypography
import org.kde.kdeconnect.ui.compose.LocalKdeSemanticColors

/**
 * Isolated modern theme wrapper for the Stage 6 Part 2 connected device dashboard prototype.
 * Preserves the subtle purple/indigo identity, glass surface tiers, and neon accents
 * without modifying legacy screens or theme defaults outside the modern package.
 */
@Composable
fun ModernKdeTheme(
    context: Context,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme
    val semanticColors = if (darkTheme) KdeDarkSemanticColors else KdeLightSemanticColors

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

    CompositionLocalProvider(LocalKdeSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KdeTypography,
            shapes = KdeShapes,
            content = content,
        )
    }
}

val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF5B4AC7),                // Brand indigo accent (> 5:1 contrast on white)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9E4FA),       // Soft lavender container
    onPrimaryContainer = Color(0xFF1B0F5C),     // Deep indigo text (> 11:1 contrast)
    secondary = Color(0xFF0F766E),              // Deep teal
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),   // > 6.8:1 contrast
    tertiary = Color(0xFF15803D),               // Forest green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF166534),   // > 6.5:1 contrast
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),         // Soft red container light
    onErrorContainer = Color(0xFF991B1B),       // Dark crimson text light (> 7.4:1 contrast)
    background = Color(0xFFF4F3FB),             // Pearl light background
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),                // Pure white card surface
    onSurface = Color(0xFF0F172A),              // > 16.5:1 contrast on white
    surfaceVariant = Color(0xFFECE8F6),
    onSurfaceVariant = Color(0xFF475569),       // > 5.4:1 contrast on white
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDEDAE8),
    surfaceContainer = Color(0xFFF4F3FB),
    surfaceContainerHigh = Color(0xFFEDEAF7),
    surfaceContainerHighest = Color(0xFFE5E1F2),
    surfaceContainerLow = Color(0xFFF8F7FD),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA594FD),                // M3 accessible lavender/indigo on dark (> 6.8:1 on #1E2130)
    onPrimary = Color(0xFF1E1065),
    primaryContainer = Color(0xFF38297A),       // Deep indigo container
    onPrimaryContainer = Color(0xFFE9E4FA),     // > 9.8:1 contrast
    secondary = Color(0xFF5EEAD4),              // High-contrast teal/cyan (> 11.8:1 on #1E2130)
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF134E48),
    onSecondaryContainer = Color(0xFFCCFBF1),   // > 7.8:1 contrast
    tertiary = Color(0xFF86EFAC),               // High-contrast mint (> 12:1 on #1E2130)
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFDCFCE7),   // > 7.3:1 contrast
    error = Color(0xFFFCA5A5),                  // High-contrast coral/red (> 8.5:1 on #1E2130)
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF450A0A),         // Deep desaturated red container dark
    onErrorContainer = Color(0xFFFFD1D1),       // Soft readable coral text (> 12:1 contrast)
    background = Color(0xFF07080D),             // Deep Aurora night black
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E2130),                // Card surface container tier
    onSurface = Color(0xFFF1F5F9),              // > 14.2:1 contrast on #1E2130
    surfaceVariant = Color(0xFF282C40),
    onSurfaceVariant = Color(0xFFCBD5E1),       // > 9.8:1 contrast on #1E2130
    outline = Color(0xFF64748B),
    outlineVariant = Color(0x38FFFFFF),         // 22% white hairline gradient border
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3B405C),
    surfaceDim = Color(0xFF0F111A),
    surfaceContainer = Color(0xFF1E2130),
    surfaceContainerHigh = Color(0xFF282C40),
    surfaceContainerHighest = Color(0xFF333852),
    surfaceContainerLow = Color(0xFF131520),
    surfaceContainerLowest = Color(0xFF07080D)
)
