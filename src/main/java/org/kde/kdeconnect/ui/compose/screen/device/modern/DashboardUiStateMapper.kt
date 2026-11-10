/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect_tp.R

object PluginCategoryRegistry {
    fun categoryFor(pluginKey: String): PluginCategory = when (pluginKey) {
        "SharePlugin", "ClipboardPlugin", "SftpPlugin" ->
            PluginCategory.ShareTransfer
        "MousePadPlugin", "RunCommandPlugin", "PresenterPlugin", "FindRemoteDevicePlugin",
        "LockDevicePlugin", "RemoteKeyboardPlugin", "RemoteKeyboardIMEPlugin", "DigitizerPlugin" ->
            PluginCategory.ControlComputer
        "MprisPlugin", "SystemVolumePlugin", "MprisReceiverPlugin" ->
            PluginCategory.MediaAudio
        else ->
            PluginCategory.DeviceInfo
    }

    fun categoryTitle(category: PluginCategory): String = when (category) {
        PluginCategory.ShareTransfer -> "Share & Transfer"
        PluginCategory.ControlComputer -> "Control Computer"
        PluginCategory.MediaAudio -> "Media & Audio"
        PluginCategory.DeviceInfo -> "Device Information"
    }

    fun totalCountFor(category: PluginCategory): Int = when (category) {
        PluginCategory.ShareTransfer -> 3
        PluginCategory.ControlComputer -> 7
        PluginCategory.MediaAudio -> 3
        PluginCategory.DeviceInfo -> 10
    }
}

object DashboardUiStateMapper {

    // One-way notification policy: ReceiveNotificationsPlugin is permanently excluded from active user-facing UI.
    const val POLICY_HIDDEN_PLUGIN_KEY = "ReceiveNotificationsPlugin"

    fun map(
        deviceName: String,
        deviceIconRes: Int,
        isConnected: Boolean,
        plugins: List<DevicePluginUiModel>,
        batteryPercent: Int?,
        isBatteryCharging: Boolean = false,
        isBatteryLow: Boolean = false,
        connectivitySummary: String? = null,
        mediaState: MediaPlaybackUiState? = null,
        volumeState: VolumeControlUiState? = null,
        isRefreshing: Boolean = false,
        localDeviceName: String? = null
    ): DashboardUiState {
        // Enforce policy: filter out reverse notification plugin
        val activePlugins = plugins.filter { it.pluginKey != POLICY_HIDDEN_PLUGIN_KEY }

        // Hero state
        val batteryUiState = if (batteryPercent != null && batteryPercent in 0..100) {
            BatteryUiState(
                percent = batteryPercent,
                isCharging = isBatteryCharging,
                isLow = isBatteryLow
            )
        } else {
            null
        }

        val networkList = connectivitySummary
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val hero = HeroUiState(
            deviceName = deviceName,
            deviceIconRes = deviceIconRes,
            isConnected = isConnected,
            battery = batteryUiState,
            networkTypes = networkList
        )

        // Attention banner: collect plugins needing required or optional permissions
        val permissionItems = activePlugins
            .filter { it.needsRequiredPermission || it.needsOptionalPermission }
            .map { model ->
                PermissionItem(
                    pluginKey = model.pluginKey,
                    displayName = model.displayName,
                    isRequired = model.needsRequiredPermission
                )
            }

        val requiredCount = permissionItems.count { it.isRequired }
        val optionalCount = permissionItems.size - requiredCount
        val attention = if (permissionItems.isNotEmpty()) {
            AttentionUiState(
                requiredCount = requiredCount,
                optionalCount = optionalCount,
                items = permissionItems
            )
        } else {
            null
        }

        // Quick actions: 6 primary action tiles mapped directly to underlying plugin actions
        val quickActions = buildQuickActions(activePlugins, deviceName)

        // Grouped service sections
        val groupedSections = buildServiceSections(activePlugins, batteryUiState, connectivitySummary, deviceName, localDeviceName)

        // Manage plugins summary
        val enabledCount = activePlugins.count { it.isEnabled }
        val manageSummary = ManagePluginsSummary(
            enabledCount = enabledCount,
            totalCount = activePlugins.size,
            hasPermissionsPending = permissionItems.isNotEmpty()
        )

        return DashboardUiState(
            hero = hero,
            attention = attention,
            quickActions = quickActions,
            media = mediaState,
            volume = volumeState,
            sections = groupedSections,
            managePluginsSummary = manageSummary,
            isRefreshing = isRefreshing
        )
    }

    private fun buildQuickActions(plugins: List<DevicePluginUiModel>, deviceName: String): List<QuickActionUiModel> {
        val pluginMap = plugins.associateBy { it.pluginKey }

        data class ActionSpec(
            val id: String,
            val label: String,
            val pluginKey: String,
            val iconRes: Int,
            val labelRes: Int? = null
        )

        val specs = listOf(
            ActionSpec("send_files", "Send Files", "SharePlugin", R.drawable.ic_share_white, R.string.quick_action_send_files),
            ActionSpec("clipboard", "Clipboard", "ClipboardPlugin", R.drawable.ic_baseline_content_paste_24, R.string.quick_action_clipboard),
            ActionSpec("remote_input", "Remote Input", "MousePadPlugin", R.drawable.ic_finger_24dp, R.string.quick_action_remote_input),
            ActionSpec("ring_device", "Ring $deviceName", "FindRemoteDevicePlugin", R.drawable.ic_notification, R.string.quick_action_ring_device),
            ActionSpec("run_command", "Run Command", "RunCommandPlugin", R.drawable.ic_baseline_code_24, R.string.quick_action_run_command),
            ActionSpec("presenter", "Presenter", "PresenterPlugin", R.drawable.ic_presenter_24dp, R.string.quick_action_presenter)
        )

        return specs.map { spec ->
            val model = pluginMap[spec.pluginKey]
            val firstButton = model?.actionButtons?.firstOrNull()
            val availability = when {
                model == null || !model.isEnabled -> QuickActionAvailability.Disabled
                model.needsRequiredPermission -> QuickActionAvailability.NeedsPermission("Requires permission")
                else -> QuickActionAvailability.Ready
            }

            QuickActionUiModel(
                id = spec.id,
                label = spec.label,
                iconRes = spec.iconRes,
                availability = availability,
                actionButton = firstButton,
                labelRes = spec.labelRes
            )
        }
    }

    private fun buildServiceSections(
        plugins: List<DevicePluginUiModel>,
        batteryState: BatteryUiState?,
        connectivitySummary: String?,
        deviceName: String,
        localDeviceName: String?
    ): List<ServiceSectionUiModel> {
        val byCategory = plugins.groupBy { PluginCategoryRegistry.categoryFor(it.pluginKey) }

        return listOf(
            PluginCategory.ShareTransfer,
            PluginCategory.ControlComputer,
            PluginCategory.MediaAudio,
            PluginCategory.DeviceInfo
        ).mapNotNull { cat ->
            val categoryPlugins = byCategory[cat].orEmpty()
            if (categoryPlugins.isEmpty()) return@mapNotNull null

            val rows = categoryPlugins.map { plugin ->
                val supporting = when (plugin.pluginKey) {
                    "BatteryPlugin" -> batteryState?.let {
                        "${it.percent}%${if (it.isCharging) " · Charging" else ""}"
                    }
                    "ConnectivityReportPlugin" -> connectivitySummary
                    "NotificationsPlugin" -> "${localDeviceName ?: "This device"} → $deviceName only"
                    else -> null
                }

                ServiceRowUiModel(
                    pluginKey = plugin.pluginKey,
                    title = plugin.displayName,
                    subtitle = plugin.description,
                    iconRes = resolvePluginIcon(plugin.pluginKey),
                    supportingValue = supporting,
                    isEnabled = plugin.isEnabled,
                    needsPermission = plugin.needsRequiredPermission || plugin.needsOptionalPermission,
                    isRequiredPermission = plugin.needsRequiredPermission,
                    buttons = plugin.actionButtons
                )
            }

            ServiceSectionUiModel(
                category = cat,
                title = PluginCategoryRegistry.categoryTitle(cat),
                rows = rows,
                titleRes = cat.displayNameRes
            )
        }
    }

    private fun resolvePluginIcon(pluginKey: String): Int? {
        val normalized = pluginKey.removeSuffix("Plugin").lowercase()
        return when (normalized) {
            "share" -> R.drawable.share_plugin_action_24dp
            "clipboard" -> R.drawable.ic_baseline_content_paste_24
            "sftp" -> R.drawable.ic_modern_folder
            "mousepad" -> R.drawable.touchpad_plugin_action_24dp
            "runcommand" -> R.drawable.run_command_plugin_icon_24dp
            "presenter" -> R.drawable.ic_presenter_24dp
            "findremotedevice" -> R.drawable.ic_modern_bell
            "findmyphone" -> R.drawable.ic_modern_phone_ring
            "lockdevice" -> R.drawable.ic_key
            "remotekeyboard" -> R.drawable.ic_action_keyboard_24dp
            "remotekeyboardime" -> R.drawable.ic_keyboard_return_black_24dp
            "digitizer" -> R.drawable.ic_draw_24dp
            "mpris" -> R.drawable.ic_modern_album_art
            "mprisreceiver" -> R.drawable.mpris_plugin_action_24dp
            "systemvolume" -> R.drawable.ic_modern_volume
            "battery" -> R.drawable.ic_device_laptop_32dp
            "connectivity_report", "connectivityreport" -> R.drawable.ic_wifi
            "notifications" -> R.drawable.ic_modern_notifications
            "contacts" -> R.drawable.ic_modern_contacts
            "inputdevicesreceiver" -> R.drawable.ic_modern_gamepad
            "mousereceiver" -> R.drawable.ic_modern_mouse
            "ping" -> R.drawable.ic_baseline_send_24
            "sms" -> R.drawable.ic_baseline_sms_24
            "telephony" -> R.drawable.ic_device_phone_32dp
            else -> null
        }
    }
}

