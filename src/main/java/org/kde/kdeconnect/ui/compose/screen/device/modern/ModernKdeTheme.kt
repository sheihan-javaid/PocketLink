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
    primary = Color(0xFF6254C7),             // Subtle Indigo Brand Accent (#6254C7)
    onPrimary = KdePalette.White,
    primaryContainer = KdePalette.Lavender100,  // Soft Lavender Container (#E9E4FA)
    onPrimaryContainer = KdePalette.Indigo950,
    secondary = Color(0xFF0F766E),           // High-contrast Teal Accent
    onSecondary = KdePalette.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF15803D),
    onTertiary = KdePalette.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF166534),
    error = Color(0xFFDC2626),
    onError = KdePalette.White,
    errorContainer = Color(0xFFFEF2F2),         // Soft red container light (#FEF2F2)
    onErrorContainer = Color(0xFF991B1B),       // Dark crimson text light (AAA contrast)
    background = Color(0xFFF4F3FB),             // Pearl light background (#F4F3FB)
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),                // Pure white card surface
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFECE8F6),
    onSurfaceVariant = Color(0xFF475569),       // > 4.5:1 contrast
    outline = KdePalette.Neutral400,
    outlineVariant = Color(0xFFE2E8F0),
    scrim = KdePalette.Black
)

val ModernDarkColorScheme = darkColorScheme(
    primary = KdePalette.NeonIndigo,            // #7C6CFF (Neon Indigo)
    onPrimary = KdePalette.White,
    primaryContainer = Color(0xFF261D57),       // Deep Indigo Container
    onPrimaryContainer = Color(0xFFE9E4FA),
    secondary = KdePalette.NeonCyan,            // #3DDBFF (Neon Cyan)
    onSecondary = Color(0xFF003643),
    secondaryContainer = Color(0xFF004E5F),
    onSecondaryContainer = Color(0xFFC4EFFF),
    tertiary = KdePalette.NeonMint,             // #4ADE9F (Neon Mint)
    onTertiary = Color(0xFF00381D),
    tertiaryContainer = Color(0xFF00532D),
    onTertiaryContainer = Color(0xFFA5F4C8),
    error = KdePalette.NeonCoral,               // #FF6B7A (Neon Coral)
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF2A1517),         // Deep desaturated red container dark
    onErrorContainer = Color(0xFFFFB4AB),       // Soft readable coral text
    background = Color(0xFF07080D),             // #07080D Deep Aurora night black
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F111A),                // #0F111A Aurora Glass card tier
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E2130),
    onSurfaceVariant = Color(0xFF94A3B8),       // > 4.5:1 contrast on #07080D & #0F111A
    outline = Color(0xFF475569),
    outlineVariant = Color(0x2EFFFFFF),         // 18% white hairline gradient border
    scrim = KdePalette.Black
)
