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
data class KdeSemanticColors(
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
    val brandAccent: Color = Color(0xFF6254C7),
    val onBrandAccent: Color = Color(0xFFFFFFFF),
    val brandContainer: Color = Color(0xFFE9E4FA),
    val onBrandContainer: Color = Color(0xFF1B1440)
)

internal val KdeLightSemanticColors = KdeSemanticColors(
    success = Color(0xFF176B45),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD7F3E2),
    onSuccessContainer = Color(0xFF083B25),
    warning = Color(0xFF815500),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFEFC5),
    onWarningContainer = Color(0xFF382400),
    information = Color(0xFF2457C5),
    onInformation = Color(0xFFFFFFFF),
    informationContainer = Color(0xFFDCE5FF),
    onInformationContainer = Color(0xFF102044),
    brandAccent = Color(0xFF6254C7),
    onBrandAccent = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFE9E4FA),
    onBrandContainer = Color(0xFF1B1440)
)

internal val KdeDarkSemanticColors = KdeSemanticColors(
    success = Color(0xFF82D6A9),
    onSuccess = Color(0xFF073B25),
    successContainer = Color(0xFF165638),
    onSuccessContainer = Color(0xFFB6F0CC),
    warning = Color(0xFFF0C36A),
    onWarning = Color(0xFF382400),
    warningContainer = Color(0xFF5C4200),
    onWarningContainer = Color(0xFFFFEFC5),
    information = Color(0xFFB4C7FF),
    onInformation = Color(0xFF102044),
    informationContainer = Color(0xFF304B80),
    onInformationContainer = Color(0xFFDCE5FF),
    brandAccent = Color(0xFFA99AEF),
    onBrandAccent = Color(0xFF1B1440),
    brandContainer = Color(0xFF261D57),
    onBrandContainer = Color(0xFFE9E4FA)
)

internal val LocalKdeSemanticColors = staticCompositionLocalOf { KdeLightSemanticColors }

@Composable
fun kdeSemanticColors(): KdeSemanticColors = LocalKdeSemanticColors.current
