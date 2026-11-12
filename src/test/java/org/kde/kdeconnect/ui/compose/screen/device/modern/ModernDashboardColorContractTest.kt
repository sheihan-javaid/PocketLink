/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import androidx.compose.ui.graphics.Color
import org.kde.kdeconnect.ui.compose.ModernDarkColorScheme
import org.kde.kdeconnect.ui.compose.ModernLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ModernDashboardColorContractTest {

    private fun linearize(channel: Float): Double {
        return if (channel <= 0.04045f) {
            channel.toDouble() / 12.92
        } else {
            ((channel.toDouble() + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testLightColorSchemeWcagCompliance() {
        val scheme = ModernLightColorScheme
        val tokens = createDashboardTokens(scheme, isDark = false)

        // 1. Text contrast >= 4.5:1 on card surface
        val primaryTextContrast = contrastRatio(scheme.onSurface, scheme.surface)
        assertTrue("Primary text on light surface contrast ($primaryTextContrast) must be >= 4.5:1", primaryTextContrast >= 4.5)

        val secondaryTextContrast = contrastRatio(scheme.onSurfaceVariant, scheme.surface)
        assertTrue("Secondary text on light surface contrast ($secondaryTextContrast) must be >= 4.5:1", secondaryTextContrast >= 4.5)

        // 2. Non-text / Brand accent >= 3:1 on card surface
        val brandAccentContrast = contrastRatio(scheme.primary, scheme.surface)
        assertTrue("Primary accent on light surface contrast ($brandAccentContrast) must be >= 3.0:1", brandAccentContrast >= 3.0)

        // 3. Status container badges >= 4.5:1
        val errorContrast = contrastRatio(scheme.onErrorContainer, scheme.errorContainer)
        assertTrue("Error container text contrast ($errorContrast) must be >= 4.5:1", errorContrast >= 4.5)

        val secondaryContainerContrast = contrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertTrue("Secondary container text contrast ($secondaryContainerContrast) must be >= 4.5:1", secondaryContainerContrast >= 4.5)

        val tertiaryContainerContrast = contrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertTrue("Tertiary container text contrast ($tertiaryContainerContrast) must be >= 4.5:1", tertiaryContainerContrast >= 4.5)

        val primaryContainerContrast = contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
        assertTrue("Primary container text contrast ($primaryContainerContrast) must be >= 4.5:1", primaryContainerContrast >= 4.5)
    }

    @Test
    fun testDarkColorSchemeWcagCompliance() {
        val scheme = ModernDarkColorScheme
        val tokens = createDashboardTokens(scheme, isDark = true)

        // 1. Text contrast >= 4.5:1 on card surface
        val primaryTextContrast = contrastRatio(scheme.onSurface, scheme.surface)
        assertTrue("Primary text on dark surface contrast ($primaryTextContrast) must be >= 4.5:1", primaryTextContrast >= 4.5)

        val secondaryTextContrast = contrastRatio(scheme.onSurfaceVariant, scheme.surface)
        assertTrue("Secondary text on dark surface contrast ($secondaryTextContrast) must be >= 4.5:1", secondaryTextContrast >= 4.5)

        // 2. Non-text / Brand accent >= 3:1 on card surface
        val brandAccentContrast = contrastRatio(scheme.primary, scheme.surface)
        assertTrue("Primary accent on dark surface contrast ($brandAccentContrast) must be >= 3.0:1", brandAccentContrast >= 3.0)

        // 3. Status container badges >= 4.5:1
        val errorContrast = contrastRatio(scheme.onErrorContainer, scheme.errorContainer)
        assertTrue("Error container text contrast ($errorContrast) must be >= 4.5:1", errorContrast >= 4.5)

        val secondaryContainerContrast = contrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertTrue("Secondary container text contrast ($secondaryContainerContrast) must be >= 4.5:1", secondaryContainerContrast >= 4.5)

        val tertiaryContainerContrast = contrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertTrue("Tertiary container text contrast ($tertiaryContainerContrast) must be >= 4.5:1", tertiaryContainerContrast >= 4.5)

        val primaryContainerContrast = contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
        assertTrue("Primary container text contrast ($primaryContainerContrast) must be >= 4.5:1", primaryContainerContrast >= 4.5)
    }

    @Test
    fun testNoHardcodedColorsInModernComponents() {
        val modernDir = File("src/main/java/org/kde/kdeconnect/ui/compose/screen/device/modern")
        assertTrue("Modern directory should exist", modernDir.exists())

        val hexColorRegex = Regex("""Color\s*\(\s*0x[0-9a-fA-F]+\s*\)""")
        val forbiddenFiles = mutableListOf<String>()

        modernDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "DashboardEffects.kt" } // DesignTokens definitions
            .forEach { file ->
                val text = file.readText()
                if (hexColorRegex.containsMatchIn(text)) {
                    forbiddenFiles.add(file.name)
                }
            }

        assertTrue(
            "Found hardcoded Color(0x...) in modern dashboard files (must use M3 ColorScheme / DashboardDesignTokens): $forbiddenFiles",
            forbiddenFiles.isEmpty()
        )
    }

    @Test
    fun testEffectsTierResolution() {
        // Minimal tier when animations disabled or power save active
        assertEquals(EffectsTier.Minimal, resolveEffectsTier(sdk = 34, powerSave = false, animatorScale = 0f, lowRam = false))
        assertEquals(EffectsTier.Minimal, resolveEffectsTier(sdk = 34, powerSave = true, animatorScale = 1f, lowRam = false))

        // Reduced tier for pre-Android 12 or low RAM devices
        assertEquals(EffectsTier.Reduced, resolveEffectsTier(sdk = 30, powerSave = false, animatorScale = 1f, lowRam = false))
        assertEquals(EffectsTier.Reduced, resolveEffectsTier(sdk = 34, powerSave = false, animatorScale = 1f, lowRam = true))

        // Full tier for modern devices with animations enabled
        assertEquals(EffectsTier.Full, resolveEffectsTier(sdk = 34, powerSave = false, animatorScale = 1f, lowRam = false))
    }

    @Test
    fun testPluginCategoryRegistry() {
        assertEquals(PluginCategory.ShareTransfer, PluginCategoryRegistry.categoryFor("SharePlugin"))
        assertEquals(PluginCategory.ShareTransfer, PluginCategoryRegistry.categoryFor("ClipboardPlugin"))
        assertEquals(PluginCategory.ShareTransfer, PluginCategoryRegistry.categoryFor("SftpPlugin"))

        assertEquals(PluginCategory.ControlComputer, PluginCategoryRegistry.categoryFor("MousePadPlugin"))
        assertEquals(PluginCategory.ControlComputer, PluginCategoryRegistry.categoryFor("RunCommandPlugin"))
        assertEquals(PluginCategory.ControlComputer, PluginCategoryRegistry.categoryFor("PresenterPlugin"))
        assertEquals(PluginCategory.ControlComputer, PluginCategoryRegistry.categoryFor("FindRemoteDevicePlugin"))

        assertEquals(PluginCategory.MediaAudio, PluginCategoryRegistry.categoryFor("MprisPlugin"))
        assertEquals(PluginCategory.MediaAudio, PluginCategoryRegistry.categoryFor("SystemVolumePlugin"))
        assertEquals(PluginCategory.MediaAudio, PluginCategoryRegistry.categoryFor("MprisReceiverPlugin"))

        assertEquals(PluginCategory.DeviceInfo, PluginCategoryRegistry.categoryFor("BatteryPlugin"))
        assertEquals(PluginCategory.DeviceInfo, PluginCategoryRegistry.categoryFor("NotificationsPlugin"))
    }

    @Test
    fun testPurpleVioletIndigoIdentity() {
        val light = ModernLightColorScheme
        val dark = ModernDarkColorScheme

        // Light mode indigo primary, violet secondary, purple tertiary
        assertEquals(Color(0xFF5B4AC7), light.primary)
        assertEquals(Color(0xFF724B8B), light.secondary)
        assertEquals(Color(0xFF5E4B99), light.tertiary)

        // Dark mode lavender primary, violet secondary, lilac tertiary
        assertEquals(Color(0xFFA896FE), dark.primary)
        assertEquals(Color(0xFFDABCF3), dark.secondary)
        assertEquals(Color(0xFFCBBDF6), dark.tertiary)
    }

    @Test
    fun testStandardizedCardShapes() {
        val density = androidx.compose.ui.unit.Density(1f)
        val size = androidx.compose.ui.geometry.Size(200f, 200f)

        // Main cards standardized to 26dp
        assertEquals(26f, DashboardShapes.CardShape.topStart.toPx(size, density), 0.01f)

        // Action tiles standardized to 18dp
        assertEquals(18f, DashboardShapes.TileShape.topStart.toPx(size, density), 0.01f)
    }
}

