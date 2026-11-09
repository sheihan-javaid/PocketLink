/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect_tp.R
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ConnectedDeviceDashboardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun connectedDashboardShowsVerifiedIdentityStatusAndAvailableInformation() {
        showDashboard(
            batteryStatus = "Battery: 72%",
            connectivityStatus = "5G"
        )

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Connected").assertIsDisplayed()
        composeRule.onNodeWithText("Battery: 72%").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Connectivity report").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("5G").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unavailableInformationIsOmittedAndOfflineActionsAreNotShown() {
        showDashboard(
            isConnected = false,
            batteryStatus = null,
            connectivityStatus = null,
            plugins = listOf(pluginModel())
        )

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Device not reachable").assertIsDisplayed()
        composeRule.onAllNodesWithText("Battery: 72%").assertCountEquals(0)
        composeRule.onAllNodesWithText("File sharing").assertCountEquals(0)
    }

    @Test
    fun supportedPluginActionAndToggleRouteToExistingCallbacks() {
        val actionCalls = AtomicInteger()
        val toggleCalls = AtomicInteger()
        var toggleValue: Boolean? = null
        val model = pluginModel(
            actionButtons = listOf(
                Plugin.PluginUiButton("Open files", R.drawable.ic_device_laptop_32dp) {
                    actionCalls.incrementAndGet()
                }
            )
        )

        showDashboard(
            plugins = listOf(model),
            onPluginEnabledChange = { _, enabled ->
                toggleValue = enabled
                toggleCalls.incrementAndGet()
            },
            onButtonClick = { it.onClick(composeRule.activity) }
        )

        composeRule.onNodeWithText("Open files").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("File sharing").performClick()
        composeRule.runOnIdle {
            assertEquals(1, actionCalls.get())
            assertEquals(1, toggleCalls.get())
            assertEquals(false, toggleValue)
        }
    }

    @Test
    fun permissionGuidanceRoutesToExistingPermissionExplanation() {
        val permissionCalls = AtomicInteger()
        showDashboard(
            plugins = listOf(pluginModel(needsRequiredPermission = true)),
            onRequiredPermissionClick = { permissionCalls.incrementAndGet() }
        )

        composeRule.onNodeWithText(
            "Some Plugins need permissions to work (tap for more info):"
        ).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, permissionCalls.get())
        }
    }

    @Test
    fun emptySupportedPluginListHasAnExplicitEmptyState() {
        showDashboard(plugins = emptyList())

        composeRule.onNodeWithText("No compatible features are available for this device.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun offlineRecoveryActionRoutesToExistingRefreshCallback() {
        val refreshCalls = AtomicInteger()
        showDashboard(
            isConnected = false,
            onRefresh = { refreshCalls.incrementAndGet() }
        )

        composeRule.onNodeWithText("Refresh").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, refreshCalls.get())
        }
    }

    @Test
    fun dashboardRemainsReadableInDarkThemeAtLargeFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.8f)
            ) {
                KdeTheme(
                    context = LocalContext.current,
                    darkTheme = true,
                    useDynamicColor = false
                ) {
                    ConnectedDeviceDashboard(
                        deviceName = "Work laptop",
                        deviceIconRes = R.drawable.ic_device_laptop_32dp,
                        isConnected = true,
                        plugins = listOf(pluginModel()),
                        batteryStatus = null,
                        connectivityStatus = null,
                        isRefreshing = false,
                        onRefresh = {},
                        onPluginEnabledChange = { _, _ -> },
                        onButtonClick = {},
                        onRequiredPermissionClick = {},
                        onOptionalPermissionClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Connected").assertIsDisplayed()
    }

    private fun showDashboard(
        isConnected: Boolean = true,
        plugins: List<DevicePluginUiModel> = listOf(pluginModel()),
        batteryStatus: String? = "Battery: 72%",
        connectivityStatus: String? = null,
        onPluginEnabledChange: (String, Boolean) -> Unit = { _, _ -> },
        onButtonClick: (Plugin.PluginUiButton) -> Unit = {},
        onRequiredPermissionClick: (String) -> Unit = {},
        onOptionalPermissionClick: (String) -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        composeRule.setContent {
            KdeTheme(
                context = LocalContext.current,
                darkTheme = false,
                useDynamicColor = false
            ) {
                ConnectedDeviceDashboard(
                    deviceName = "Work laptop",
                    deviceIconRes = R.drawable.ic_device_laptop_32dp,
                    isConnected = isConnected,
                    plugins = plugins,
                    batteryStatus = batteryStatus,
                    connectivityStatus = connectivityStatus,
                    isRefreshing = false,
                    onRefresh = onRefresh,
                    onPluginEnabledChange = onPluginEnabledChange,
                    onButtonClick = onButtonClick,
                    onRequiredPermissionClick = onRequiredPermissionClick,
                    onOptionalPermissionClick = onOptionalPermissionClick
                )
            }
        }
    }

    private fun pluginModel(
        actionButtons: List<Plugin.PluginUiButton> = emptyList(),
        needsRequiredPermission: Boolean = false
    ) = DevicePluginUiModel(
        pluginKey = "test.plugin",
        displayName = "File sharing",
        description = "Share files with this device.",
        isEnabled = true,
        actionButtons = actionButtons,
        needsRequiredPermission = needsRequiredPermission,
        needsOptionalPermission = false
    )
}
