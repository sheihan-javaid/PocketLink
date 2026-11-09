/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.digitizer

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class DigitizerScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun digitizerScreenDisplaysTopBarAndDrawButton() {
        var fullscreenToggled = false
        var settingsOpened = false

        composeRule.setContent {
            DigitizerScreen(
                deviceName = "Galaxy Book Pro",
                device = null,
                onBackPressedDispatcher = OnBackPressedDispatcher(),
                onOpenSettings = { settingsOpened = true },
                isFullscreen = false,
                onToggleFullscreen = { fullscreenToggled = true },
                hideDrawButton = false,
                drawButtonSide = "bottom_left",
                onDrawingPadReady = {}
            )
        }

        composeRule.onNodeWithText("Galaxy Book Pro").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.enable_fullscreen))
            .assertIsDisplayed()
            .performClick()

        assertTrue(fullscreenToggled)

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.digitizer_draw_button))
            .assertIsDisplayed()
    }

    @Test
    fun fullscreenModeShowsExitButtonAndToggles() {
        var exitFullscreenClicked = false

        composeRule.setContent {
            DigitizerScreen(
                deviceName = "MacBook Pro",
                device = null,
                onBackPressedDispatcher = OnBackPressedDispatcher(),
                onOpenSettings = {},
                isFullscreen = true,
                onToggleFullscreen = { exitFullscreenClicked = true },
                hideDrawButton = false,
                drawButtonSide = "bottom_right",
                onDrawingPadReady = {}
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.close))
            .assertIsDisplayed()
            .performClick()

        assertTrue(exitFullscreenClicked)
    }

    @Test
    fun hideDrawButtonOptionHidesFloatingAction() {
        composeRule.setContent {
            DigitizerScreen(
                deviceName = "Desktop PC",
                device = null,
                onBackPressedDispatcher = OnBackPressedDispatcher(),
                onOpenSettings = {},
                isFullscreen = false,
                onToggleFullscreen = {},
                hideDrawButton = true,
                drawButtonSide = "top_right",
                onDrawingPadReady = {}
            )
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.digitizer_draw_button))
            .assertDoesNotExist()
    }
}
