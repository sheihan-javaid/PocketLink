/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.presenter

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PresenterScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun presenterScreenShowsIdentityAndSlideNavigationButtons() {
        val nextCalls = AtomicInteger()
        val prevCalls = AtomicInteger()

        composeRule.setContent {
            PresenterScreenContent(
                deviceName = "Conference Laptop",
                onBackPressed = {},
                onPrevious = { prevCalls.incrementAndGet() },
                onNext = { nextCalls.incrementAndGet() },
                onFullscreen = {},
                onExit = {},
                onZoomIn = {},
                onZoomOut = {},
                volumeKeys = true,
                offScreenControlsSupported = true,
                hasSensorManager = true,
                onPointerStart = {},
                onPointerStop = {}
            )
        }

        composeRule.onNodeWithText("Conference Laptop").assertIsDisplayed()

        val nextDesc = composeRule.activity.getString(R.string.mpris_next)
        val prevDesc = composeRule.activity.getString(R.string.mpris_previous)

        composeRule.onNodeWithContentDescription(nextDesc).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(prevDesc).assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, nextCalls.get())
            assertEquals(1, prevCalls.get())
        }

        val pointerText = composeRule.activity.getString(R.string.presenter_pointer)
        composeRule.onNodeWithText(pointerText).assertIsDisplayed()
    }

    @Test
    fun zoomControlsCanBeOpenedAndTriggerZoomActions() {
        val zoomInCalls = AtomicInteger()
        val zoomOutCalls = AtomicInteger()

        composeRule.setContent {
            PresenterScreenContent(
                deviceName = "Conference Laptop",
                onBackPressed = {},
                onPrevious = {},
                onNext = {},
                onFullscreen = {},
                onExit = {},
                onZoomIn = { zoomInCalls.incrementAndGet() },
                onZoomOut = { zoomOutCalls.incrementAndGet() },
                volumeKeys = false,
                offScreenControlsSupported = false,
                hasSensorManager = false,
                onPointerStart = {},
                onPointerStop = {}
            )
        }

        val optionsDesc = composeRule.activity.getString(R.string.extra_options)
        composeRule.onNodeWithContentDescription(optionsDesc).performClick()

        val zoomOption = composeRule.activity.getString(R.string.presenter_change_zoom)
        composeRule.onNodeWithText(zoomOption).performClick()

        val zoomInDesc = composeRule.activity.getString(R.string.presenter_zoom_in)
        val zoomOutDesc = composeRule.activity.getString(R.string.presenter_zoom_out)

        composeRule.onNodeWithContentDescription(zoomInDesc).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(zoomOutDesc).assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, zoomInCalls.get())
            assertEquals(1, zoomOutCalls.get())
        }
    }
}

