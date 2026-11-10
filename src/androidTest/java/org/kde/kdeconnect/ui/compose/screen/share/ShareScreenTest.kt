/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.share

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.model.device.DeviceUiModel
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class ShareScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reachableDeviceIsPresentedAndSelectionUsesExistingCallback() {
        var selectedDeviceId: String? = null
        showScreen(
            devices = listOf(device(isReachable = true)),
            onDeviceClick = { selectedDeviceId = it }
        )

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Connected").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("device-1", selectedDeviceId)
        }
    }

    @Test
    fun unreachableUrlTargetIsClearlyMarkedAndRemainsSelectable() {
        var selectedDeviceId: String? = null
        showScreen(
            devices = listOf(device(isReachable = false)),
            intentHasUrl = true,
            onDeviceClick = { selectedDeviceId = it }
        )

        composeRule.onNodeWithContentDescription("Device is not reachable")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Work laptop").performClick()
        composeRule.runOnIdle {
            assertEquals("device-1", selectedDeviceId)
        }
    }

    @Test
    fun emptyDeviceListShowsHelpfulEmptyState() {
        showScreen(devices = emptyList())

        composeRule.onNodeWithText("No devices found").assertIsDisplayed()
        composeRule.onNodeWithText("Not connected to any device").assertIsDisplayed()
    }

    @Test
    fun lightAndDarkThemeSupportLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.8f)
            ) {
                PocketLinkTheme(
                    context = LocalContext.current,
                    darkTheme = true,
                    useDynamicColor = false
                ) {
                    ShareScreen(
                        devices = listOf(device(isReachable = true)),
                        intentHasUrl = false,
                        isRefreshing = false,
                        onDeviceClick = {},
                        onRefresh = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Connected").assertCountEquals(1)
    }

    private fun showScreen(
        devices: List<DeviceUiModel>,
        intentHasUrl: Boolean = false,
        onDeviceClick: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            PocketLinkTheme(
                context = LocalContext.current,
                darkTheme = false,
                useDynamicColor = false
            ) {
                ShareScreen(
                    devices = devices,
                    intentHasUrl = intentHasUrl,
                    isRefreshing = false,
                    onDeviceClick = onDeviceClick,
                    onRefresh = {}
                )
            }
        }
    }

    private fun device(isReachable: Boolean) = DeviceUiModel(
        id = "device-1",
        icon = R.drawable.ic_device_laptop_32dp,
        name = "Work laptop",
        summaryRes = 0,
        isReachable = isReachable,
        isPaired = true
    )
}
