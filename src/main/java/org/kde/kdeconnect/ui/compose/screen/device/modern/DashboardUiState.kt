/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect_tp.R

/**
 * High-level category for organizing PocketLink plugins on the modern dashboard.
 */
enum class PluginCategory(@StringRes val displayNameRes: Int? = null) {
    ShareTransfer(R.string.category_share_transfer),
    ControlComputer(R.string.category_control_computer),
    MediaAudio(R.string.category_media_audio),
    DeviceInfo(R.string.category_device_info)
}

/**
 * Availability state for quick action tiles.
 */
sealed interface QuickActionAvailability {
    object Ready : QuickActionAvailability
    data class NeedsPermission(val permissionExplanation: String? = null) : QuickActionAvailability
    object Disabled : QuickActionAvailability
}

/**
 * Severity level for the consolidated attention banner.
 */
enum class PermissionSeverity {
    Required,
    Optional
}

@Immutable
data class PermissionItem(
    val pluginKey: String,
    val displayName: String,
    val isRequired: Boolean
)

@Immutable
data class AttentionUiState(
    val requiredCount: Int,
    val optionalCount: Int,
    val items: List<PermissionItem>
) {
    val totalCount: Int get() = requiredCount + optionalCount
    val severity: PermissionSeverity
        get() = if (requiredCount > 0) PermissionSeverity.Required else PermissionSeverity.Optional
}

@Immutable
data class BatteryUiState(
    val percent: Int,
    val isCharging: Boolean,
    val isLow: Boolean
)

@Immutable
data class HeroUiState(
    val deviceName: String,
    @DrawableRes val deviceIconRes: Int,
    val isConnected: Boolean,
    val battery: BatteryUiState? = null,
    val networkTypes: List<String> = emptyList()
)

@Immutable
data class QuickActionUiModel(
    val id: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val availability: QuickActionAvailability = QuickActionAvailability.Ready,
    val actionButton: Plugin.PluginUiButton? = null,
    @StringRes val labelRes: Int? = null
)

@Immutable
data class ServiceRowUiModel(
    val pluginKey: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int? = null,
    val supportingValue: String? = null,
    val isEnabled: Boolean = true,
    val needsPermission: Boolean = false,
    val isRequiredPermission: Boolean = false,
    val buttons: List<Plugin.PluginUiButton> = emptyList()
)

@Immutable
data class ServiceSectionUiModel(
    val category: PluginCategory,
    val title: String,
    val rows: List<ServiceRowUiModel>,
    @StringRes val titleRes: Int? = null
)

@Immutable
data class MediaPlaybackUiState(
    val playerName: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val isPlaying: Boolean,
    val canPlayPause: Boolean = true,
    val canSkipNext: Boolean = true,
    val canSkipPrev: Boolean = true,
    val positionMs: Long = -1L,
    val durationMs: Long = -1L,
    val canSeek: Boolean = false
)

@Immutable
data class VolumeControlUiState(
    val sinkName: String,
    val volume: Int,
    val maxVolume: Int = 100,
    val isMuted: Boolean = false
)

@Immutable
data class ManagePluginsSummary(
    val enabledCount: Int,
    val totalCount: Int,
    val hasPermissionsPending: Boolean = false
)

/**
 * Complete immutable UI state for the modern connected device dashboard.
 */
@Immutable
data class DashboardUiState(
    val hero: HeroUiState,
    val attention: AttentionUiState? = null,
    val quickActions: List<QuickActionUiModel> = emptyList(),
    val media: MediaPlaybackUiState? = null,
    val volume: VolumeControlUiState? = null,
    val sections: List<ServiceSectionUiModel> = emptyList(),
    val managePluginsSummary: ManagePluginsSummary,
    val isRefreshing: Boolean = false
)

/**
 * Consolidated interaction contract for all dashboard events.
 */
interface DashboardActions {
    fun onRefresh()
    fun onQuickActionClick(action: QuickActionUiModel)
    fun onPluginButtonClick(button: Plugin.PluginUiButton)
    fun onPermissionItemClick(pluginKey: String, isRequired: Boolean)
    fun onAttentionBannerClick()
    fun onManagePluginsClick()
    fun onMediaPlayPause()
    fun onMediaNext()
    fun onMediaPrevious()
    fun onMediaSeek(positionMs: Long) {}
    fun onVolumeChange(newVolume: Int)
    fun onVolumeToggleMute() {}
}

