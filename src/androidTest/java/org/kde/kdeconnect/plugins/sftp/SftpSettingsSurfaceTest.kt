/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.sftp

import android.content.res.Configuration
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class SftpSettingsSurfaceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun storageLocationFieldUsesThemeSemanticColorInLightAndDarkModes() {
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
                .inflate(R.layout.fragment_storage_preference_dialog, null, false)
            val location = root.findViewById<TextInputEditText>(R.id.storageLocation)
            val semanticColor = TypedValue()

            assertTrue(context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                semanticColor,
                true
            ))
            assertEquals(semanticColor.data, location.currentTextColor)
        }
    }
}
