/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernDarkColorScheme
import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernLightColorScheme

@Composable
fun KdeTheme(
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
    val semanticColors = if (darkTheme) KdeDarkSemanticColors else KdeLightSemanticColors

    androidx.compose.runtime.CompositionLocalProvider(LocalKdeSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KdeTypography,
            shapes = KdeShapes,
            content = content,
        )
    }
}
