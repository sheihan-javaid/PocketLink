/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class PluginSettingsSurfaceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun preferenceThemeIsResolvedInAppTheme() {
        val context = ContextThemeWrapper(composeRule.activity, R.style.PocketLinkTheme)
        val preferenceThemeVal = TypedValue()
        val resolved = context.theme.resolveAttribute(androidx.preference.R.attr.preferenceTheme, preferenceThemeVal, true)
        assertTrue("preferenceTheme must be resolved", resolved)
        assertTrue(preferenceThemeVal.resourceId != 0)
    }

    @Test
    fun preferenceWithButtonLayoutUsesMaterial3SemanticColorsAndTouchTarget() {
        val baseContext = composeRule.activity
        val nightModes = listOf(
            Configuration.UI_MODE_NIGHT_NO,
            Configuration.UI_MODE_NIGHT_YES
        )

        nightModes.forEach { nightMode ->
            val configuration = Configuration(baseContext.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            val context = ContextThemeWrapper(
                baseContext.createConfigurationContext(configuration),
                R.style.PocketLinkTheme
            )
            val root = LayoutInflater.from(context)
                .inflate(R.layout.preference_with_button, null, false)

            val minTarget = context.resources.getDimensionPixelSize(R.dimen.kde_min_touch_target)
            assertTrue("Root minHeight must be >= minTouchTarget", root.minimumHeight >= minTarget)

            val titleView = root.findViewById<TextView>(android.R.id.title)
            val summaryView = root.findViewById<TextView>(android.R.id.summary)
            val dividerView = root.findViewById<View>(R.id.divider)

            val onSurfaceColor = TypedValue()
            assertTrue(context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface,
                onSurfaceColor,
                true
            ))
            assertEquals(onSurfaceColor.data, titleView.currentTextColor)

            val onSurfaceVariantColor = TypedValue()
            assertTrue(context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                onSurfaceVariantColor,
                true
            ))
            assertEquals(onSurfaceVariantColor.data, summaryView.currentTextColor)

            val outlineVariantColor = TypedValue()
            assertTrue(context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOutlineVariant,
                outlineVariantColor,
                true
            ))
            val dividerColor = (dividerView.background as? ColorDrawable)?.color
            assertEquals(outlineVariantColor.data, dividerColor)
        }
    }
}