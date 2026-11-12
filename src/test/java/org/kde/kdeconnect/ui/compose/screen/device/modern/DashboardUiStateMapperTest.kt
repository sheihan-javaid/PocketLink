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
        "TelephonyPlugin",
        "WallpaperPlugin"
    )

    private val pluginsWithSettings = setOf(
        "ClipboardPlugin",
        "DigitizerPlugin",
        "FindMyPhonePlugin",
        "MousePadPlugin",
        "MprisPlugin",
        "NotificationsPlugin",
        "PresenterPlugin",
        "RemoteKeyboardIMEPlugin",
        "RunCommandPlugin",
        "SftpPlugin",
        "SharePlugin",
        "SMSPlugin",
        "TelephonyPlugin"
    )

    private fun createSampleModels(): List<DevicePluginUiModel> {
        return allExpectedPluginKeys.map { key ->
            DevicePluginUiModel(
                pluginKey = key,
                displayName = key.removeSuffix("Plugin"),
                description = "Description for $key",
                isEnabled = true,
                hasSettings = key in pluginsWithSettings,
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

        // Verify total visible count is 24 (25 minus ReceiveNotificationsPlugin, including WallpaperPlugin)
        assertEquals(24, state.managePluginsSummary.totalCount)

        // Verify ReceiveNotificationsPlugin is not in any service section row
        val allRowKeys = state.sections.flatMap { it.rows }.map { it.pluginKey }
        assertFalse(
            "ReceiveNotificationsPlugin must NOT appear in any service row",
            allRowKeys.contains("ReceiveNotificationsPlugin")
        )

        // Verify NotificationsPlugin is present and labeled appropriately with dynamic device names (no hardcoded brand)
        val notifRow = state.sections.flatMap { it.rows }.find { it.pluginKey == "NotificationsPlugin" }
        assertNotNull("NotificationsPlugin must be present", notifRow)
        assertEquals("This device → MacBook Pro only", notifRow?.supportingValue)

        val stateWithLocal = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = sampleModels,
            batteryPercent = 80,
            localDeviceName = "Pixel 8"
        )
        val notifRowCustom = stateWithLocal.sections.flatMap { it.rows }.find { it.pluginKey == "NotificationsPlugin" }
        assertEquals("Pixel 8 → MacBook Pro only", notifRowCustom?.supportingValue)
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

        val ringAction = state.quickActions.find { it.id == "ring_device" }
        assertNotNull(ringAction)
        assertEquals("Ring MacBook Pro", ringAction?.label)
    }

    @Test
    fun serviceRowPreservesAllActionButtonsInOrder() {
        val multiButtonModel = DevicePluginUiModel(
            pluginKey = "MousePadPlugin",
            displayName = "Remote input",
            description = "Use phone as touchpad",
            isEnabled = true,
            hasSettings = true,
            actionButtons = listOf(
                Plugin.PluginUiButton("Open Touchpad", R.drawable.touchpad_plugin_action_24dp) {},
                Plugin.PluginUiButton("Open TV Remote", R.drawable.touchpad_plugin_action_24dp) {}
            ),
            needsRequiredPermission = false,
            needsOptionalPermission = false
        )

        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = listOf(multiButtonModel),
            batteryPercent = 90
        )

        val row = state.sections.flatMap { it.rows }.find { it.pluginKey == "MousePadPlugin" }
        assertNotNull("MousePadPlugin row must exist", row)
        assertEquals("Must preserve all 2 action buttons", 2, row?.buttons?.size)
        assertEquals("Open Touchpad", row?.buttons?.get(0)?.name)
        assertEquals("Open TV Remote", row?.buttons?.get(1)?.name)
    }

    @Test
    fun serviceRowDifferentiatesRequiredVsOptionalPermissions() {
        val reqModel = DevicePluginUiModel(
            pluginKey = "ContactsPlugin",
            displayName = "Contacts",
            description = "Contacts sync",
            isEnabled = true,
            hasSettings = false,
            actionButtons = emptyList(),
            needsRequiredPermission = true,
            needsOptionalPermission = false
        )
        val optModel = DevicePluginUiModel(
            pluginKey = "MouseReceiverPlugin",
            displayName = "Mouse",
            description = "Mouse receiver",
            isEnabled = true,
            hasSettings = false,
            actionButtons = emptyList(),
            needsRequiredPermission = false,
            needsOptionalPermission = true
        )

        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = listOf(reqModel, optModel),
            batteryPercent = 90
        )

        val reqRow = state.sections.flatMap { it.rows }.find { it.pluginKey == "ContactsPlugin" }
        assertNotNull(reqRow)
        assertTrue("Needs permission must be true", reqRow!!.needsPermission)
        assertTrue("Required permission flag must be true", reqRow.isRequiredPermission)

        val optRow = state.sections.flatMap { it.rows }.find { it.pluginKey == "MouseReceiverPlugin" }
        assertNotNull(optRow)
        assertTrue("Needs permission must be true", optRow!!.needsPermission)
        assertFalse("Optional permission must NOT have required flag", optRow.isRequiredPermission)
    }

    @Test
    fun mediaPlaybackStatePreservesLiveTelemetry() {
        val mediaState = MediaPlaybackUiState(
            playerName = "Spotify",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "A Night at the Opera",
            isPlaying = true,
            canPlayPause = true,
            canSkipNext = true,
            canSkipPrev = true,
            positionMs = 120000L,
            durationMs = 354000L,
            canSeek = true
        )

        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 90,
            mediaState = mediaState
        )

        assertNotNull("Media state must be present", state.media)
        assertEquals("Spotify", state.media?.playerName)
        assertEquals("Bohemian Rhapsody", state.media?.title)
        assertEquals("Queen", state.media?.artist)
        assertTrue(state.media?.isPlaying == true)
        assertEquals(120000L, state.media?.positionMs)
        assertEquals(354000L, state.media?.durationMs)
        assertTrue(state.media?.canSeek == true)
    }

    @Test
    fun volumeControlStatePreservesSinkTelemetry() {
        val volumeState = VolumeControlUiState(
            sinkName = "Headphones",
            volume = 42,
            maxVolume = 100,
            isMuted = false
        )

        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 90,
            volumeState = volumeState
        )

        assertNotNull("Volume state must be present", state.volume)
        assertEquals("Headphones", state.volume?.sinkName)
        assertEquals(42, state.volume?.volume)
        assertEquals(100, state.volume?.maxVolume)
        assertFalse(state.volume?.isMuted == true)
    }

    @Test
    fun desktopPreviewStateIsPropagatedToDashboardState() {
        val available = DesktopPreviewState.Available("/data/user/0/wallpapers/deviceId/wallpaper.img", "abc123")
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 80,
            desktopPreview = available
        )
        assertEquals(available, state.desktopPreview)
    }

    @Test
    fun desktopPreviewStateDefaultsToWaiting() {
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = createSampleModels(),
            batteryPercent = 80
        )
        assertEquals(DesktopPreviewState.Waiting, state.desktopPreview)
    }

    @Test
    fun desktopPreviewStateAvailableOfflinePropagates() {
        val offline = DesktopPreviewState.AvailableOffline("/data/user/0/wallpapers/deviceId/wallpaper.img", "def456")
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = false,
            plugins = createSampleModels(),
            batteryPercent = 80,
            desktopPreview = offline
        )
        assertEquals(offline, state.desktopPreview)
    }

    @Test
    fun pluginModelHasSettingsReflectsExpectedConfiguration() {
        val sampleModels = createSampleModels()
        val modelsWithSettings = sampleModels.filter { it.hasSettings }.map { it.pluginKey }.toSet()

        assertEquals("Exactly 13 plugins should have settings", 13, modelsWithSettings.size)
        assertEquals(pluginsWithSettings, modelsWithSettings)

        assertTrue("ClipboardPlugin must have settings", sampleModels.first { it.pluginKey == "ClipboardPlugin" }.hasSettings)
        assertTrue("NotificationsPlugin must have settings", sampleModels.first { it.pluginKey == "NotificationsPlugin" }.hasSettings)
        assertTrue("SharePlugin must have settings", sampleModels.first { it.pluginKey == "SharePlugin" }.hasSettings)
        assertFalse("BatteryPlugin must not have settings", sampleModels.first { it.pluginKey == "BatteryPlugin" }.hasSettings)
        assertFalse("ContactsPlugin must not have settings", sampleModels.first { it.pluginKey == "ContactsPlugin" }.hasSettings)
        assertFalse("PingPlugin must not have settings", sampleModels.first { it.pluginKey == "PingPlugin" }.hasSettings)
    }

    @Test
    fun mapperWithEmptyPluginsProducesEmptySectionsAndZeroCounts() {
        val state = DashboardUiStateMapper.map(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            plugins = emptyList(),
            batteryPercent = null
        )

        assertTrue("Sections must be empty when no plugins provided", state.sections.isEmpty())
        assertTrue(
            "All quick actions must be disabled when no plugins provided",
            state.quickActions.all { it.availability is QuickActionAvailability.Disabled }
        )
        assertEquals("Total count in summary must be 0", 0, state.managePluginsSummary.totalCount)
        assertEquals("Enabled count in summary must be 0", 0, state.managePluginsSummary.enabledCount)
        assertFalse("Permissions pending must be false", state.managePluginsSummary.hasPermissionsPending)
        assertNull("Attention must be null", state.attention)
    }
}

