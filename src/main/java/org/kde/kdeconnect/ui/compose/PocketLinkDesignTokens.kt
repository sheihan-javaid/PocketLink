/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object PocketLinkPalette {
    val White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val Black = androidx.compose.ui.graphics.Color(0xFF000000)

    // Neon HUD Accent Tokens (Aurora Glass + Neon HUD)
    val NeonIndigo = androidx.compose.ui.graphics.Color(0xFF7C6CFF)
    val NeonCyan   = androidx.compose.ui.graphics.Color(0xFF3DDBFF)
    val NeonViolet = androidx.compose.ui.graphics.Color(0xFFC46BFF)
    val NeonMint   = androidx.compose.ui.graphics.Color(0xFF4ADE9F)
    val NeonAmber  = androidx.compose.ui.graphics.Color(0xFFFFB547)
    val NeonCoral  = androidx.compose.ui.graphics.Color(0xFFFF6B7A)

    // Aurora Glass Surfaces
    val AuroraDarkBackground = androidx.compose.ui.graphics.Color(0xFF07080D)
    val AuroraDarkGlassSurface = androidx.compose.ui.graphics.Color(0xFF0F111A)
    val AuroraDarkGlassBorder = androidx.compose.ui.graphics.Color(0x2EFFFFFF)
    val AuroraLightBackground = androidx.compose.ui.graphics.Color(0xFFF4F3FB)
    val AuroraLightGlassSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

    // PocketLink Brand Identity Tokens: Premium Apple-like Indigo & Lavender
    val Indigo950 = androidx.compose.ui.graphics.Color(0xFF1B1440)
    val Indigo900 = androidx.compose.ui.graphics.Color(0xFF261D57)
    val Indigo800 = androidx.compose.ui.graphics.Color(0xFF3F3287)
    val Indigo700 = androidx.compose.ui.graphics.Color(0xFF5141B0)
    val Indigo600 = androidx.compose.ui.graphics.Color(0xFF6254C7) // Primary brand accent (#6254C7)
    val Violet500 = androidx.compose.ui.graphics.Color(0xFF8065D8) // Secondary accent (#8065D8)
    val Lavender400 = androidx.compose.ui.graphics.Color(0xFF9682E6)
    val Lavender300 = androidx.compose.ui.graphics.Color(0xFFA99AEF) // Dark primary (#A99AEF)
    val Lavender200 = androidx.compose.ui.graphics.Color(0xFFC7BCF6)
    val Lavender100 = androidx.compose.ui.graphics.Color(0xFFE9E4FA) // Soft lavender container (#E9E4FA)
    val Lavender50  = androidx.compose.ui.graphics.Color(0xFFF7F5FE) // Ultra-soft surface tint

    val Blue950 = androidx.compose.ui.graphics.Color(0xFF102044)
    val Blue800 = androidx.compose.ui.graphics.Color(0xFF304B80)
    val Blue600 = androidx.compose.ui.graphics.Color(0xFF2457C5)
    val Blue200 = androidx.compose.ui.graphics.Color(0xFFB4C7FF)
    val Blue100 = androidx.compose.ui.graphics.Color(0xFFDCE5FF)

    val Teal950 = androidx.compose.ui.graphics.Color(0xFF123C39)
    val Teal800 = androidx.compose.ui.graphics.Color(0xFF285B56)
    val Teal700 = androidx.compose.ui.graphics.Color(0xFF356B68)
    val Teal200 = androidx.compose.ui.graphics.Color(0xFFA1D4CE)
    val Teal100 = androidx.compose.ui.graphics.Color(0xFFD6F0EC)

    val Violet950 = androidx.compose.ui.graphics.Color(0xFF35244D)
    val Violet800 = androidx.compose.ui.graphics.Color(0xFF5C4678)
    val Violet700 = androidx.compose.ui.graphics.Color(0xFF72548F)
    val Violet200 = androidx.compose.ui.graphics.Color(0xFFD0B8F2)
    val Violet100 = androidx.compose.ui.graphics.Color(0xFFEADDFF)

    val Red950 = androidx.compose.ui.graphics.Color(0xFF410E0B)
    val Red800 = androidx.compose.ui.graphics.Color(0xFF8C1D18)
    val Red700 = androidx.compose.ui.graphics.Color(0xFFB3261E)
    val Red200 = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
    val Red100 = androidx.compose.ui.graphics.Color(0xFFF9DEDC)

    val Neutral950 = androidx.compose.ui.graphics.Color(0xFF111216) // Deep neutral black
    val Neutral900 = androidx.compose.ui.graphics.Color(0xFF1A1B22) // Elevated card surface dark
    val Neutral850 = androidx.compose.ui.graphics.Color(0xFF22242E) // Surface container dark
    val Neutral800 = androidx.compose.ui.graphics.Color(0xFF2B2D3A) // Surface variant dark
    val Neutral700 = androidx.compose.ui.graphics.Color(0xFF454954)
    val Neutral600 = androidx.compose.ui.graphics.Color(0xFF5E626E)
    val Neutral400 = androidx.compose.ui.graphics.Color(0xFF8C909C)
    val Neutral300 = androidx.compose.ui.graphics.Color(0xFFBFC3CD)
    val Neutral200 = androidx.compose.ui.graphics.Color(0xFFD9DDE6)
    val Neutral100 = androidx.compose.ui.graphics.Color(0xFFECEFF5)
    val Neutral50  = androidx.compose.ui.graphics.Color(0xFFF4F6FA)
    val Neutral25  = androidx.compose.ui.graphics.Color(0xFFF8F9FD) // Clean bright background light
}

object PocketLinkSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val mediumSmall = 12.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
    val contentHorizontal = 16.dp
    val cardPadding = 16.dp
    val minTouchTarget = 48.dp
    val compactContentMaxWidth = 600.dp
    val mediumContentMaxWidth = 840.dp
    val expandedContentMaxWidth = 720.dp
}

object PocketLinkComponentSize {
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 24.dp
    val iconFeatured = 32.dp
    val deviceCardMinHeight = 72.dp
    val actionCardMinHeight = 96.dp
    val controlMinHeight = 48.dp
}

internal val PocketLinkTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

internal val PocketLinkShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)
