/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernKdeTheme
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.AttentionUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.BatteryUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardActions
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.HeroUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.ManagePluginsSummary
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernConnectedDeviceDashboard
import org.kde.kdeconnect.ui.compose.screen.device.modern.PermissionItem
import org.kde.kdeconnect.ui.compose.screen.device.modern.PluginCategory
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionAvailability
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceRowUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ServiceSectionUiModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.VolumeControlUiState
import org.kde.kdeconnect_tp.R

object DashboardFixtures {

    val mockActions = object : DashboardActions {
        override fun onRefresh() {}
        override fun onQuickActionClick(action: QuickActionUiModel) {}
        override fun onPluginButtonClick(button: Plugin.PluginUiButton) {}
        override fun onPermissionItemClick(pluginKey: String, isRequired: Boolean) {}
        override fun onAttentionBannerClick() {}
        override fun onManagePluginsClick() {}
        override fun onMediaPlayPause() {}
        override fun onMediaNext() {}
        override fun onMediaPrevious() {}
        override fun onMediaSeek(positionMs: Long) {}
        override fun onVolumeChange(newVolume: Int) {}
        override fun onVolumeToggleMute() {}
    }

    val fullFeaturedState = DashboardUiState(
        hero = HeroUiState(
            deviceName = "MacBook Pro",
            deviceIconRes = R.drawable.ic_device_laptop_32dp,
            isConnected = true,
            battery = BatteryUiState(percent = 55, isCharging = false, isLow = false),
            networkTypes = listOf("Wi-Fi (5 GHz)")
        ),
        attention = AttentionUiState(
            requiredCount = 1,
            optionalCount = 1,
            items = listOf(
                PermissionItem("ContactsPlugin", "Contacts Synchroniser", isRequired = true),
                PermissionItem("MouseReceiverPlugin", "Mouse Receiver", isRequired = false)
            )
        ),
        quickActions = listOf(
            QuickActionUiModel("send_files", "Send Files", R.drawable.share_plugin_action_24dp, QuickActionAvailability.Ready),
            QuickActionUiModel("clipboard", "Clipboard", R.drawable.ic_baseline_content_paste_24, QuickActionAvailability.Ready),
            QuickActionUiModel("remote_input", "Remote Input", R.drawable.touchpad_plugin_action_24dp, QuickActionAvailability.Ready),
            QuickActionUiModel("ring_device", "Ring Mac", R.drawable.ic_modern_bell, QuickActionAvailability.Ready),
            QuickActionUiModel("run_command", "Run Command", R.drawable.run_command_plugin_icon_24dp, QuickActionAvailability.Ready),
            QuickActionUiModel("presenter", "Presenter", R.drawable.ic_presenter_24dp, QuickActionAvailability.Ready)
        ),
        media = MediaPlaybackUiState(
            playerName = "Music",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            isPlaying = true
        ),
        volume = VolumeControlUiState(
            sinkName = "MacBook Pro Speakers",
            volume = 72,
            isMuted = false
        ),
        sections = listOf(
            ServiceSectionUiModel(
                category = PluginCategory.ShareTransfer,
                title = "Share & Transfer",
                rows = listOf(
                    ServiceRowUiModel("SharePlugin", "Share and receive", "Share files and URLs between devices", iconRes = R.drawable.share_plugin_action_24dp, isEnabled = true),
                    ServiceRowUiModel("ClipboardPlugin", "Clipboard sync", "Synchronize clipboard contents", iconRes = R.drawable.ic_baseline_content_paste_24, isEnabled = true),
                    ServiceRowUiModel("SftpPlugin", "Filesystem access", "Allows remote browsing of filesystem", iconRes = R.drawable.ic_modern_folder, isEnabled = true)
                )
            ),
            ServiceSectionUiModel(
                category = PluginCategory.ControlComputer,
                title = "Control Computer",
                rows = listOf(
                    ServiceRowUiModel("MousePadPlugin", "Remote input", "Touchpad and keyboard control", iconRes = R.drawable.touchpad_plugin_action_24dp, isEnabled = true),
                    ServiceRowUiModel("RunCommandPlugin", "Run Command", "Execute configured Mac scripts", iconRes = R.drawable.run_command_plugin_icon_24dp, isEnabled = true),
                    ServiceRowUiModel("PresenterPlugin", "Presentation remote", "Slide navigation and laser pointer", iconRes = R.drawable.ic_presenter_24dp, isEnabled = true),
                    ServiceRowUiModel("FindRemoteDevicePlugin", "Find remote device", "Ring remote computer", iconRes = R.drawable.ic_modern_bell, isEnabled = true)
                )
            ),
            ServiceSectionUiModel(
                category = PluginCategory.DeviceInfo,
                title = "Device Information",
                rows = listOf(
                    ServiceRowUiModel("BatteryPlugin", "Battery report", "Periodic battery level reports", iconRes = R.drawable.ic_device_laptop_32dp, supportingValue = "55% · Discharging", isEnabled = true),
                    ServiceRowUiModel("ConnectivityReportPlugin", "Connectivity report", "Network signal and status", iconRes = R.drawable.ic_wifi, supportingValue = "Wi-Fi (5 GHz)", isEnabled = true),
                    ServiceRowUiModel("NotificationsPlugin", "Notification sync", "Access notifications from other devices", iconRes = R.drawable.ic_modern_notifications, supportingValue = "Samsung → Mac only", isEnabled = true)
                )
            )
        ),
        managePluginsSummary = ManagePluginsSummary(
            enabledCount = 20,
            totalCount = 23,
            hasPermissionsPending = true
        ),
        isRefreshing = false
    )

    val stateWithoutAttention = fullFeaturedState.copy(attention = null)

    val samplePluginModels = listOf(
        // Share & Transfer (3)
        DevicePluginUiModel("SharePlugin", "Share and receive", "Share files and URLs between devices", true, listOf(Plugin.PluginUiButton("Send Files", R.drawable.share_plugin_action_24dp) {}), false, false),
        DevicePluginUiModel("ClipboardPlugin", "Clipboard sync", "Synchronize clipboard contents across devices", true, listOf(Plugin.PluginUiButton("Send Clipboard", R.drawable.ic_baseline_content_paste_24) {}), false, false),
        DevicePluginUiModel("SftpPlugin", "Filesystem access", "Allows remote browsing of filesystem via SFTP", true, listOf(Plugin.PluginUiButton("Browse", R.drawable.ic_modern_folder) {}), false, false),

        // Control Computer (7)
        DevicePluginUiModel("MousePadPlugin", "Remote input", "Touchpad and keyboard control for your computer", true, listOf(Plugin.PluginUiButton("Open Touchpad", R.drawable.touchpad_plugin_action_24dp) {}), false, false),
        DevicePluginUiModel("RunCommandPlugin", "Run Command", "Execute configured Mac scripts and commands", true, listOf(Plugin.PluginUiButton("Commands", R.drawable.run_command_plugin_icon_24dp) {}), false, false),
        DevicePluginUiModel("PresenterPlugin", "Presentation remote", "Slide navigation and laser pointer control", true, listOf(Plugin.PluginUiButton("Presenter", R.drawable.ic_presenter_24dp) {}), false, false),
        DevicePluginUiModel("FindRemoteDevicePlugin", "Find remote device", "Ring remote computer to locate it", true, listOf(Plugin.PluginUiButton("Ring", R.drawable.ic_modern_bell) {}), false, false),
        DevicePluginUiModel("RemoteKeyboardPlugin", "Remote keyboard", "Receive keyboard input from remote device", true, emptyList(), false, false),
        DevicePluginUiModel("RemoteKeyboardIMEPlugin", "Remote keyboard IME", "Input method for remote keyboard entry", false, emptyList(), false, false),
        DevicePluginUiModel("DigitizerPlugin", "Drawing tablet", "Send stylus and touch input to computer", false, emptyList(), false, false),

        // Media & Audio (3)
        DevicePluginUiModel("MprisPlugin", "Media control", "Control remote computer media playback", true, emptyList(), false, false),
        DevicePluginUiModel("SystemVolumePlugin", "System volume", "Control remote computer audio volume", true, emptyList(), false, false),
        DevicePluginUiModel("MprisReceiverPlugin", "Media control receiver", "Allow remote device to control phone playback", true, emptyList(), false, false),

        // Device Information & Connectivity (10)
        DevicePluginUiModel("BatteryPlugin", "Battery report", "Periodic battery level and charging reports", true, emptyList(), false, false),
        DevicePluginUiModel("ConnectivityReportPlugin", "Connectivity report", "Network signal strength and connection status", true, emptyList(), false, false),
        DevicePluginUiModel("NotificationsPlugin", "Notification sync", "Forward phone notifications to Mac (one-way)", true, emptyList(), false, false),
        DevicePluginUiModel("ContactsPlugin", "Contacts synchroniser", "Synchronize phone contacts with remote computer", true, emptyList(), true, false),
        DevicePluginUiModel("FindMyPhonePlugin", "Find my phone", "Allow remote device to ring this phone", true, emptyList(), false, false),
        DevicePluginUiModel("InputDevicesReceiverPlugin", "Input devices receiver", "Receive input device events from computer", false, emptyList(), false, false),
        DevicePluginUiModel("MouseReceiverPlugin", "Mouse receiver", "Receive mouse pointer events on phone", true, emptyList(), false, true),
        DevicePluginUiModel("PingPlugin", "Ping", "Send and receive connection check pings", true, listOf(Plugin.PluginUiButton("Send Ping", R.drawable.ic_baseline_send_24) {}), false, false),
        DevicePluginUiModel("SMSPlugin", "SMS / MMS", "Send and receive SMS messages from computer", true, emptyList(), false, false),
        DevicePluginUiModel("TelephonyPlugin", "Telephony", "Call alerts and incoming call notifications", true, emptyList(), false, false)
    )
}

@PreviewLightDark
@Composable
private fun ModernConnectedDeviceDashboardPreview() {
    ModernKdeTheme(context = LocalContext.current) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ModernConnectedDeviceDashboard(
                state = DashboardFixtures.fullFeaturedState,
                actions = DashboardFixtures.mockActions,
                pluginModelsForManagePage = DashboardFixtures.samplePluginModels,
                onPluginEnabledChange = { _, _ -> },
                onButtonClick = {},
                onRequiredPermissionClick = {},
                onOptionalPermissionClick = {}
            )
        }
    }
}