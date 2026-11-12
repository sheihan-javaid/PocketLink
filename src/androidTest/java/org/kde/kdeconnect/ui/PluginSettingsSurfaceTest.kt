/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui

import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
