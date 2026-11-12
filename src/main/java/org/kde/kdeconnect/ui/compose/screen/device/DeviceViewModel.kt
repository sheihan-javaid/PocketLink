/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.plugins.PluginFactory
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.plugins.connectivityreport.ConnectivityReportPlugin
import org.kde.kdeconnect.plugins.mpris.MprisPlugin
import org.kde.kdeconnect.plugins.systemvolume.Sink
import org.kde.kdeconnect.plugins.systemvolume.SystemVolumePlugin
import org.kde.kdeconnect.plugins.wallpaper.WallpaperPlugin
import org.kde.kdeconnect.plugins.wallpaper.WallpaperRepository
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardUiStateMapper
import org.kde.kdeconnect.ui.compose.screen.device.modern.DesktopPreviewState
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.VolumeControlUiState

interface PluginMetadataProvider {
    val availablePlugins: Set<String>
    fun getDisplayName(pluginKey: String): String
    fun getDescription(pluginKey: String): String
    fun hasSettings(pluginKey: String): Boolean
    fun sortPluginList(plugins: Collection<String>): List<String>
}

object DefaultPluginMetadataProvider : PluginMetadataProvider {
    override val availablePlugins: Set<String>
        get() = runCatching { PluginFactory.availablePlugins }.getOrDefault(emptySet())

    override fun getDisplayName(pluginKey: String): String =
        runCatching { PluginFactory.getPluginInfo(pluginKey).displayName }.getOrDefault(pluginKey)

    override fun getDescription(pluginKey: String): String =
        runCatching { PluginFactory.getPluginInfo(pluginKey).description }.getOrDefault("")

    override fun hasSettings(pluginKey: String): Boolean =
        runCatching { PluginFactory.getPluginInfo(pluginKey).hasSettings }.getOrDefault(false)

    override fun sortPluginList(plugins: Collection<String>): List<String> =
        runCatching { PluginFactory.sortPluginList(plugins) }.getOrDefault(plugins.sorted())
}

data class DeviceInfoState(
    val deviceName: String = "",
    val deviceIconRes: Int = 0,
    val isConnected: Boolean = false,
    val localDeviceName: String? = null
) {
    companion object {
        val Empty = DeviceInfoState()
    }
}

data class BatteryState(
    val percent: Int? = null,
    val isCharging: Boolean = false,
    val isLow: Boolean = false
) {
    companion object {
        val Empty = BatteryState()
    }
}

class DeviceViewModel(
    private val pluginMetadataProvider: PluginMetadataProvider = DefaultPluginMetadataProvider,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    companion object {
        private const val MPRIS_HANDLER_ID = "DeviceViewModel_MPRIS"

        private inline fun <reified T : org.kde.kdeconnect.plugins.Plugin> Device.findPlugin(key: String): T? =
            getPlugin(key) as? T
    }

    private val _isRefreshing = MutableStateFlow(value = false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _pairingErrorMessage = MutableStateFlow<String?>(value = null)
    val pairingErrorMessage: StateFlow<String?> = _pairingErrorMessage.asStateFlow()

    private val _pairingSucceeded = MutableStateFlow(value = false)
    val pairingSucceeded: StateFlow<Boolean> = _pairingSucceeded.asStateFlow()

    private val _connectivityStatus = MutableStateFlow<String?>(value = null)
    val connectivityStatus: StateFlow<String?> = _connectivityStatus.asStateFlow()

    private val _mediaPlaybackState = MutableStateFlow<MediaPlaybackUiState?>(value = null)
    val mediaPlaybackState: StateFlow<MediaPlaybackUiState?> = _mediaPlaybackState.asStateFlow()

    private val _volumeControlState = MutableStateFlow<VolumeControlUiState?>(value = null)
    val volumeControlState: StateFlow<VolumeControlUiState?> = _volumeControlState.asStateFlow()

    private val _desktopPreviewState = MutableStateFlow<DesktopPreviewState>(DesktopPreviewState.Waiting)
    val desktopPreviewState: StateFlow<DesktopPreviewState> = _desktopPreviewState.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfoState.Empty)
    private val _batteryState = MutableStateFlow(BatteryState.Empty)
    private val _pluginsState = MutableStateFlow<List<DevicePluginUiModel>>(emptyList())
    val pluginModels: StateFlow<List<DevicePluginUiModel>> = _pluginsState.asStateFlow()

    private val deviceCombinedFlow = combine(
        _deviceInfo,
        _batteryState,
        _pluginsState
    ) { info, battery, plugins ->
        Triple(info, battery, plugins)
    }

    private val mediaAndVolumeFlow = combine(
        _mediaPlaybackState,
        _volumeControlState
    ) { media, volume ->
        Pair(media, volume)
    }

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        deviceCombinedFlow,
        mediaAndVolumeFlow,
        _connectivityStatus,
        _isRefreshing,
        _desktopPreviewState
    ) { (info, battery, plugins), (media, volume), connectivity, isRefreshing, desktopPreview ->
        DashboardUiStateMapper.map(
            deviceName = info.deviceName,
            deviceIconRes = info.deviceIconRes,
            isConnected = info.isConnected,
            plugins = plugins,
            batteryPercent = battery.percent,
            isBatteryCharging = battery.isCharging,
            isBatteryLow = battery.isLow,
            connectivitySummary = connectivity,
            mediaState = media,
            volumeState = volume,
            isRefreshing = isRefreshing,
            localDeviceName = info.localDeviceName,
            desktopPreview = desktopPreview
        )
    }
        .flowOn(backgroundDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiStateMapper.map(
                deviceName = "",
                deviceIconRes = 0,
                isConnected = false,
                plugins = emptyList(),
                batteryPercent = null
            )
        )

    private var desktopPreviewJob: Job? = null
    private var reachableJob: Job? = null

    private var currentDevice: Device? = null

    private val pluginsChangedListener = Device.PluginsChangedListener {
        val dev = currentDevice ?: return@PluginsChangedListener
        updateBatteryState(dev)
        updatePluginsState(dev)
        updateConnectivityState()
        updateMediaState()
        updateVolumeState()
    }

    private val sinkListener = object : SystemVolumePlugin.SinkListener {
        override fun sinksChanged() {
            updateVolumeState()
        }
    }

    private val sinkUpdateListener = object : Sink.UpdateListener {
        override fun updateSink(sink: Sink) {
            updateVolumeState()
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun setPairingErrorMessage(message: String?) {
        _pairingErrorMessage.value = message
    }

    fun setPairingSucceeded(succeeded: Boolean) {
        _pairingSucceeded.value = succeeded
    }

    fun updateBattery(percent: Int?, isCharging: Boolean = false, isLow: Boolean = false) {
        _batteryState.value = BatteryState(percent, isCharging, isLow)
    }

    fun updateConnection(isConnected: Boolean) {
        _deviceInfo.value = _deviceInfo.value.copy(isConnected = isConnected)
    }

    fun updatePlugins(plugins: List<DevicePluginUiModel>) {
        _pluginsState.value = plugins
    }

    fun updateDevice(deviceName: String, deviceIconRes: Int = 0, isConnected: Boolean = true, localDeviceName: String? = null) {
        _deviceInfo.value = DeviceInfoState(deviceName, deviceIconRes, isConnected, localDeviceName)
    }

    fun updateBatteryState(device: Device) {
        val batteryPlugin = device.findPlugin<BatteryPlugin>("BatteryPlugin")
        val batteryInfo = batteryPlugin?.remoteBatteryInfo
        if (batteryInfo != null) {
            _batteryState.value = BatteryState(
                percent = batteryInfo.currentCharge,
                isCharging = batteryInfo.isCharging,
                isLow = BatteryPlugin.isLowBattery(batteryInfo)
            )
        } else {
            _batteryState.value = BatteryState.Empty
        }
    }

    fun updatePluginsState(device: Device) {
        _pluginsState.value = buildPluginsList(device)
    }

    private fun buildPluginsList(device: Device): List<DevicePluginUiModel> {
        val pluginKeys = pluginMetadataProvider.sortPluginList(pluginMetadataProvider.availablePlugins)
            .filter { it != "ReceiveNotificationsPlugin" }
        return pluginKeys.map { pluginKey ->
            DevicePluginUiModel(
                pluginKey = pluginKey,
                displayName = pluginMetadataProvider.getDisplayName(pluginKey),
                description = pluginMetadataProvider.getDescription(pluginKey),
                isEnabled = device.isPluginEnabled(pluginKey),
                hasSettings = pluginMetadataProvider.hasSettings(pluginKey),
                actionButtons = device.loadedPlugins[pluginKey]?.getUiButtons().orEmpty(),
                needsRequiredPermission = device.isPluginEnabled(pluginKey) &&
                    pluginKey in device.pluginsWithoutPermissions,
                needsOptionalPermission = device.isPluginEnabled(pluginKey) &&
                    pluginKey in device.pluginsWithoutOptionalPermissions
            )
        }
    }

    fun bindDevice(device: Device, localDeviceName: String? = null) {
        if (currentDevice != null && currentDevice?.deviceId != device.deviceId) {
            unbindDevice()
        }
        currentDevice = device

        _deviceInfo.value = DeviceInfoState(
            deviceName = device.name,
            deviceIconRes = device.iconDrawable,
            isConnected = device.isReachable,
            localDeviceName = localDeviceName
        )
        updateBatteryState(device)
        updatePluginsState(device)

        reachableJob?.cancel()
        reachableJob = viewModelScope.launch {
            device.isReachableFlow.collect { reachable ->
                _deviceInfo.value = _deviceInfo.value.copy(isConnected = reachable)
                updateDesktopPreviewState(device)
            }
        }

        device.removePluginsChangedListener(pluginsChangedListener)
        device.addPluginsChangedListener(pluginsChangedListener)

        val mpris = device.findPlugin<MprisPlugin>("MprisPlugin")
        if (mpris != null && device.isPluginEnabled("MprisPlugin") && device.isReachable) {
            mpris.setPlayerListUpdatedHandler(MPRIS_HANDLER_ID) { updateMediaState() }
            mpris.setPlayerStatusUpdatedHandler(MPRIS_HANDLER_ID) { updateMediaState() }
        } else {
            mpris?.removePlayerListUpdatedHandler(MPRIS_HANDLER_ID)
            mpris?.removePlayerStatusUpdatedHandler(MPRIS_HANDLER_ID)
            _mediaPlaybackState.value = null
        }

        val sysVolume = device.findPlugin<SystemVolumePlugin>("SystemVolumePlugin")
        if (sysVolume != null && device.isPluginEnabled("SystemVolumePlugin") && device.isReachable) {
            sysVolume.removeSinkListener(sinkListener)
            sysVolume.addSinkListener(sinkListener)
            sysVolume.requestSinkList()
        } else {
            sysVolume?.removeSinkListener(sinkListener)
            sysVolume?.sinks?.forEach { it.removeListener(sinkUpdateListener) }
            _volumeControlState.value = null
        }

        updateMediaState()
        updateVolumeState()
        updateConnectivityState()
        updateDesktopPreviewState(device)
    }

    fun unbindDevice() {
        desktopPreviewJob?.cancel()
        desktopPreviewJob = null
        reachableJob?.cancel()
        reachableJob = null
        _desktopPreviewState.value = DesktopPreviewState.Waiting
        val device = currentDevice
        if (device != null) {
            device.removePluginsChangedListener(pluginsChangedListener)
            val mpris = device.findPlugin<MprisPlugin>("MprisPlugin")
            mpris?.removePlayerListUpdatedHandler(MPRIS_HANDLER_ID)
            mpris?.removePlayerStatusUpdatedHandler(MPRIS_HANDLER_ID)

            val sysVolume = device.findPlugin<SystemVolumePlugin>("SystemVolumePlugin")
            sysVolume?.removeSinkListener(sinkListener)
            sysVolume?.sinks?.forEach { it.removeListener(sinkUpdateListener) }
        }
        currentDevice = null
        _deviceInfo.value = DeviceInfoState.Empty
        _batteryState.value = BatteryState.Empty
        _pluginsState.value = emptyList()
        _mediaPlaybackState.value = null
        _volumeControlState.value = null
        _connectivityStatus.value = null
    }

    fun updateConnectivityState() {
        val device = currentDevice ?: run {
            _connectivityStatus.value = null
            return
        }
        val connectivityPlugin = device.findPlugin<ConnectivityReportPlugin>("ConnectivityReportPlugin")
        val infoList = connectivityPlugin?.remoteConnectivityInfo?.values
        val status = infoList
            ?.mapNotNull { info ->
                info.networkName?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                    ?: info.networkType.takeIf { it.isNotBlank() }
            }
            ?.distinct()
            ?.joinToString(", ")
            ?.ifEmpty { null }
        _connectivityStatus.value = status
    }

    private fun updateDesktopPreviewState(device: Device) {
        desktopPreviewJob?.cancel()
        desktopPreviewJob = null

        val pluginKey = WallpaperPlugin::class.java.simpleName
        if (!device.supportedPlugins.contains(pluginKey)) {
            _desktopPreviewState.value = DesktopPreviewState.NotSupported
            return
        }
        if (!device.isPluginEnabled("WallpaperPlugin")) {
            _desktopPreviewState.value = DesktopPreviewState.Disabled
            return
        }

        desktopPreviewJob = viewModelScope.launch {
            WallpaperRepository.getWallpaperInfo(device.deviceId).collect { info ->
                when {
                    info == null -> _desktopPreviewState.value = DesktopPreviewState.Waiting
                    device.isReachable -> _desktopPreviewState.value = DesktopPreviewState.Available(
                        info.file.absolutePath, info.sha256
                    )
                    else -> _desktopPreviewState.value = DesktopPreviewState.AvailableOffline(
                        info.file.absolutePath, info.sha256
                    )
                }
            }
        }
    }

    fun updateMediaState() {
        val device = currentDevice ?: run {
            _mediaPlaybackState.value = null
            return
        }
        val mpris = device.findPlugin<MprisPlugin>("MprisPlugin")
        if (mpris == null || !device.isPluginEnabled("MprisPlugin") || !device.isReachable) {
            _mediaPlaybackState.value = null
            return
        }

        val playerList = mpris.playerList
        val activePlayerName = playerList.firstOrNull()
        val player = mpris.getPlayerStatus(activePlayerName)

        if (player != null && (player.title.isNotEmpty() || player.isPlaying)) {
            _mediaPlaybackState.value = MediaPlaybackUiState(
                playerName = player.playerName.ifEmpty { "Media Player" },
                title = player.title,
                artist = player.artist,
                album = player.album,
                isPlaying = player.isPlaying,
                canPlayPause = player.isPlayAllowed || player.isPauseAllowed,
                canSkipNext = player.isGoNextAllowed,
                canSkipPrev = player.isGoPreviousAllowed,
                positionMs = player.position,
                durationMs = player.length,
                canSeek = player.isSeekAllowed
            )
        } else {
            _mediaPlaybackState.value = null
        }
    }

    fun updateVolumeState() {
        val device = currentDevice ?: run {
            _volumeControlState.value = null
            return
        }
        val sysVolume = device.findPlugin<SystemVolumePlugin>("SystemVolumePlugin")
        if (sysVolume == null || !device.isPluginEnabled("SystemVolumePlugin") || !device.isReachable) {
            _volumeControlState.value = null
            return
        }

        val sinks = sysVolume.sinks
        sinks.forEach { it.addListener(sinkUpdateListener) }
        val defaultSink = sinks.firstOrNull { it.isDefault } ?: sinks.firstOrNull()
        if (defaultSink != null) {
            _volumeControlState.value = VolumeControlUiState(
                sinkName = defaultSink.description.ifEmpty { defaultSink.name },
                volume = defaultSink.volume,
                maxVolume = defaultSink.maxVolume,
                isMuted = defaultSink.isMute()
            )
        } else {
            _volumeControlState.value = null
        }
    }

    fun onMediaPlayPause() {
        val mpris = currentDevice?.findPlugin<MprisPlugin>("MprisPlugin") ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendPlayPause()
        updateMediaState()
    }

    fun onMediaNext() {
        val mpris = currentDevice?.findPlugin<MprisPlugin>("MprisPlugin") ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendNext()
        updateMediaState()
    }

    fun onMediaPrevious() {
        val mpris = currentDevice?.findPlugin<MprisPlugin>("MprisPlugin") ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendPrevious()
        updateMediaState()
    }

    fun onMediaSeek(positionMs: Long) {
        val mpris = currentDevice?.findPlugin<MprisPlugin>("MprisPlugin") ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendSetPosition(positionMs.toInt())
        updateMediaState()
    }

    fun onVolumeChange(newVolume: Int) {
        val sysVolume = currentDevice?.findPlugin<SystemVolumePlugin>("SystemVolumePlugin") ?: return
        val sinks = sysVolume.sinks
        val defaultSink = sinks.firstOrNull { it.isDefault } ?: sinks.firstOrNull()
        if (defaultSink != null) {
            sysVolume.sendVolume(defaultSink.name, newVolume)
            defaultSink.setVolume(newVolume)
            updateVolumeState()
        }
    }

    fun onVolumeToggleMute() {
        val sysVolume = currentDevice?.findPlugin<SystemVolumePlugin>("SystemVolumePlugin") ?: return
        val sinks = sysVolume.sinks
        val defaultSink = sinks.firstOrNull { it.isDefault } ?: sinks.firstOrNull()
        if (defaultSink != null) {
            val newMute = !defaultSink.isMute()
            sysVolume.sendMute(defaultSink.name, newMute)
            defaultSink.setMute(newMute)
            updateVolumeState()
        }
    }

    override fun onCleared() {
        unbindDevice()
        super.onCleared()
    }
}
