/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect_tp.R

class DashboardUiStateMapperTest {

    private val allExpectedPluginKeys = listOf(
        "BatteryPlugin",
        "ClipboardPlugin",
        "ConnectivityReportPlugin",
        "ContactsPlugin",
        "DigitizerPlugin",
        "FindMyPhonePlugin",
        "FindRemoteDevicePlugin",
        "InputDevicesReceiverPlugin",
        "MousePadPlugin",
        "MouseReceiverPlugin",
        "MprisPlugin",
        "MprisReceiverPlugin",
        "NotificationsPlugin",
        "PingPlugin",
        "PresenterPlugin",
        "ReceiveNotificationsPlugin",
        "RemoteKeyboardPlugin",
        "RemoteKeyboardIMEPlugin",
        "RunCommandPlugin",
        "SftpPlugin",
        "SharePlugin",
        "SMSPlugin",
        "SystemVolumePlugin",
        "TelephonyPlugin"
    )

    private fun createSampleModels(): List<DevicePluginUiModel> {
        return allExpectedPluginKeys.map { key ->
            DevicePluginUiModel(
                pluginKey = key,
                displayName = key.removeSuffix("Plugin"),
                description = "Description for $key",
                isEnabled = true,
                actionButtons = listOf(Plugin.PluginUiButton("Action for $key", R.drawable.ic_notification) {}),
                needsRequiredPermission = key == "ContactsPlugin",
                needsOptionalPermission = key == "MouseReceiverPlugin"
            )
        }
    }

    @Test
    fun allPluginsAreCategorizedIntoValidSections() {
        val visibleKeys = allExpectedPluginKeys - "ReceiveNotificationsPlugin"
        for (key in visibleKeys) {
            val category = PluginCategoryRegistry.categoryFor(key)
            assertNotNull("Category for $key must not be null", category)
            assertTrue("Category title must be non-empty", PluginCategoryRegistry.categoryTitle(category).isNotEmpty())
        }
    }

    @Test
    fun oneWayNotificationPolicyExcludesReceiveNotificationsPlugin() {
        val sampleModels = createSampleModels()
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = sampleModels,
            batteryPercent = 80
        )

        // Verify total visible count is 23 (24 minus ReceiveNotificationsPlugin)
        assertEquals(23, state.managePluginsSummary.totalCount)

        // Verify ReceiveNotificationsPlugin is not in any service section row
        val allRowKeys = state.sections.flatMap { it.rows }.map { it.pluginKey }
        assertFalse(
            "ReceiveNotificationsPlugin must NOT appear in any service row",
            allRowKeys.contains("ReceiveNotificationsPlugin")
        )

        // Verify NotificationsPlugin is present and labeled appropriately
        val notifRow = state.sections.flatMap { it.rows }.find { it.pluginKey == "NotificationsPlugin" }
        assertNotNull("NotificationsPlugin must be present", notifRow)
        assertEquals("Samsung → Mac only", notifRow?.supportingValue)
    }

    @Test
    fun attentionBannerAggregatesRequiredAndOptionalPermissions() {
        val sampleModels = createSampleModels()
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = sampleModels,
            batteryPercent = 80
        )

        assertNotNull("Attention state must not be null when permissions are needed", state.attention)
        assertEquals(1, state.attention?.requiredCount)
        assertEquals(1, state.attention?.optionalCount)
        assertEquals(2, state.attention?.totalCount)
        assertEquals(PermissionSeverity.Required, state.attention?.severity)
    }

    @Test
    fun attentionBannerIsNullWhenNoPermissionsNeeded() {
        val cleanModels = createSampleModels().map {
            it.copy(needsRequiredPermission = false, needsOptionalPermission = false)
        }
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = cleanModels,
            batteryPercent = 80
        )

        assertNull("Attention state must be null when all permissions granted", state.attention)
    }

    @Test
    fun batteryTelemetryGracefullyHandlesNullAndValidValues() {
        val stateWithBattery = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 65,
            isBatteryCharging = true,
            isBatteryLow = false
        )
        assertNotNull(stateWithBattery.hero.battery)
        assertEquals(65, stateWithBattery.hero.battery?.percent)
        assertTrue(stateWithBattery.hero.battery?.isCharging == true)

        val stateWithoutBattery = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = null
        )
        assertNull("Battery state must be null when telemetry unavailable", stateWithoutBattery.hero.battery)
    }

    @Test
    fun quickActionsMapToVerifiedPlugins() {
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 80
        )

        assertEquals("Must produce 6 priority quick actions", 6, state.quickActions.size)
        val actionIds = state.quickActions.map { it.id }
        assertTrue(actionIds.contains("send_files"))
        assertTrue(actionIds.contains("clipboard"))
        assertTrue(actionIds.contains("remote_input"))
        assertTrue(actionIds.contains("ring_device"))
        assertTrue(actionIds.contains("run_command"))
        assertTrue(actionIds.contains("presenter"))
    }
}

