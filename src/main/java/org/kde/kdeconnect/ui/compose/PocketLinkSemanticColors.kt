/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PocketLinkSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val information: Color,
    val onInformation: Color,
    val informationContainer: Color,
    val onInformationContainer: Color,
    val brandAccent: Color = Color(0xFF5B4AC7),
    val onBrandAccent: Color = Color(0xFFFFFFFF),
    val brandContainer: Color = Color(0xFFEBE5FB),
    val onBrandContainer: Color = Color(0xFF1B0F5C)
)

internal val PocketLinkLightSemanticColors = PocketLinkSemanticColors(
    success = Color(0xFF176B45),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD7F3E2),
    onSuccessContainer = Color(0xFF083B25),
    warning = Color(0xFF815500),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFEFC5),
    onWarningContainer = Color(0xFF382400),
    information = Color(0xFF5B4AC7),
    onInformation = Color(0xFFFFFFFF),
    informationContainer = Color(0xFFEBE5FB),
    onInformationContainer = Color(0xFF1B0F5C),
    brandAccent = Color(0xFF5B4AC7),
    onBrandAccent = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFEBE5FB),
    onBrandContainer = Color(0xFF1B0F5C)
)

internal val PocketLinkDarkSemanticColors = PocketLinkSemanticColors(
    success = Color(0xFF82D6A9),
    onSuccess = Color(0xFF073B25),
    successContainer = Color(0xFF165638),
    onSuccessContainer = Color(0xFFB6F0CC),
    warning = Color(0xFFF0C36A),
    onWarning = Color(0xFF382400),
    warningContainer = Color(0xFF5C4200),
    onWarningContainer = Color(0xFFFFEFC5),
    information = Color(0xFFA896FE),
    onInformation = Color(0xFF1E0E5E),
    informationContainer = Color(0xFF382478),
    onInformationContainer = Color(0xFFEBE5FB),
    brandAccent = Color(0xFFA896FE),
    onBrandAccent = Color(0xFF1E0E5E),
    brandContainer = Color(0xFF382478),
    onBrandContainer = Color(0xFFEBE5FB)
)

internal val LocalPocketLinkSemanticColors = staticCompositionLocalOf { PocketLinkLightSemanticColors }

@Composable
fun kdeSemanticColors(): PocketLinkSemanticColors = LocalPocketLinkSemanticColors.current
