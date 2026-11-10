/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.pairing

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.model.device.DeviceUiModel
import org.kde.kdeconnect_tp.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PairingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsDeviceGroupsAndRoutesAvailableDeviceSelection() {
        val selectedDeviceId = AtomicReference<String?>()
        val state = pairingState(
            connected = listOf(device("connected-id", "Work laptop", paired = true, reachable = true)),
            available = listOf(device("available-id", "Media PC", paired = false, reachable = true)),
            remembered = listOf(device("remembered-id", "Home desktop", paired = true, reachable = false)),
            hasDuplicateNames = true
        )

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                PairingScreen(uiState = state, onClick = selectedDeviceId::set)
            }
        }

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Connected").assertIsDisplayed()
        composeRule.onNodeWithText("Media PC").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Request pairing").assertIsDisplayed()
        composeRule.onNodeWithText("Home desktop").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Device not reachable").assertIsDisplayed()
        composeRule.onNodeWithText("Caution: There are multiple devices with the same name.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Media PC").performClick()
        composeRule.runOnIdle {
            assertEquals("available-id", selectedDeviceId.get())
        }
    }

    @Test
    fun emptyDiscoveryOffersManualAddressAndRefresh() {
        val customDeviceActions = AtomicInteger()
        val refreshActions = AtomicInteger()

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                PairingScreen(
                    uiState = pairingState(),
                    onClick = {},
                    onRefresh = { refreshActions.incrementAndGet() },
                    onCustomDevicesClick = { customDeviceActions.incrementAndGet() }
                )
            }
        }

        composeRule.onNodeWithText("No devices found").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Bluetooth can make devices already paired over Bluetooth available when support is enabled in Settings."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Add devices by IP").assertCountEquals(2)
        composeRule.onAllNodesWithText("Add devices by IP")[0].performClick()
        composeRule.onNodeWithContentDescription("Refresh").performClick()
        composeRule.runOnIdle {
            assertEquals(1, customDeviceActions.get())
            assertEquals(1, refreshActions.get())
        }
    }

    @Test
    fun localNetworkPermissionNoticeInvokesExistingPermissionCallback() {
        val permissionActions = AtomicInteger()
        val state = pairingState(hasLocalNetworkPermission = false)

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = true, useDynamicColor = false) {
                PairingScreen(
                    uiState = state,
                    onClick = {},
                    onLocalNetworkPermissionClick = { permissionActions.incrementAndGet() }
                )
            }
        }

        composeRule.onAllNodesWithText(
            "Missing permission to access nearby devices, the app will not work."
        )[0].performScrollTo().performClick()
        composeRule.onNodeWithText("No devices found").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, permissionActions.get())
        }
    }

    @Test
    fun unavailableNetworkShowsWifiGuidanceAndUsesExistingSettingsAction() {
        val wifiSettingsActions = AtomicInteger()

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                PairingScreen(
                    uiState = pairingState(isWifiAvailable = false),
                    onClick = {},
                    onWifiSettingsClick = { wifiSettingsActions.incrementAndGet() }
                )
            }
        }

        composeRule.onAllNodesWithText(
            "You're not connected to a Wi-Fi network, so you may not be able to see any devices. Click here to enable Wi-Fi."
        )[0].performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, wifiSettingsActions.get())
        }
    }

    @Test
    fun notificationPermissionNoticeUsesExistingSettingsAction() {
        val notificationSettingsActions = AtomicInteger()

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                PairingScreen(
                    uiState = pairingState(hasNotificationsPermission = false),
                    onClick = {},
                    onNotificationSettingsClick = { notificationSettingsActions.incrementAndGet() }
                )
            }
        }

        composeRule.onNodeWithText(
            "Notifications are disabled, you won't receive incoming pairing request notifications."
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, notificationSettingsActions.get())
        }
    }

    private fun pairingState(
        connected: List<DeviceUiModel> = emptyList(),
        available: List<DeviceUiModel> = emptyList(),
        remembered: List<DeviceUiModel> = emptyList(),
        hasDuplicateNames: Boolean = false,
        hasLocalNetworkPermission: Boolean = true,
        hasNotificationsPermission: Boolean = true,
        isWifiAvailable: Boolean = true
    ) = PairingUiState(
        isWifiAvailable = isWifiAvailable,
        hasNotificationsPermission = hasNotificationsPermission,
        hasLocalNetworkPermission = hasLocalNetworkPermission,
        isTrustedNetwork = true,
        hasDuplicateNames = hasDuplicateNames,
        connected = connected,
        available = available,
        remembered = remembered
    )

    private fun device(
        id: String,
        name: String,
        paired: Boolean,
        reachable: Boolean
    ) = DeviceUiModel(
        id = id,
        icon = R.drawable.ic_device_laptop_32dp,
        name = name,
        summaryRes = 0,
        isReachable = reachable,
        isPaired = paired
    )
}
