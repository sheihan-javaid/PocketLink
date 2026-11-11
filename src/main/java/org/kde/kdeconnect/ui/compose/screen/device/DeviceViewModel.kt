/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.plugins.connectivityreport.ConnectivityReportPlugin
import org.kde.kdeconnect.plugins.mpris.MprisPlugin
import org.kde.kdeconnect.plugins.systemvolume.Sink
import org.kde.kdeconnect.plugins.systemvolume.SystemVolumePlugin
import org.kde.kdeconnect.plugins.wallpaper.WallpaperPlugin
import org.kde.kdeconnect.plugins.wallpaper.WallpaperRepository
import org.kde.kdeconnect.ui.compose.screen.device.modern.DesktopPreviewState
import org.kde.kdeconnect.ui.compose.screen.device.modern.MediaPlaybackUiState
import org.kde.kdeconnect.ui.compose.screen.device.modern.VolumeControlUiState

class DeviceViewModel : ViewModel() {

    companion object {
        private const val MPRIS_HANDLER_ID = "DeviceViewModel_MPRIS"
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

    private var desktopPreviewJob: Job? = null

    private var currentDevice: Device? = null

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

    fun bindDevice(device: Device) {
        if (currentDevice != null && currentDevice?.deviceId != device.deviceId) {
            unbindDevice()
        }
        currentDevice = device

        val mpris = device.getPlugin<MprisPlugin>()
        if (mpris != null && device.isPluginEnabled("MprisPlugin") && device.isReachable) {
            mpris.setPlayerListUpdatedHandler(MPRIS_HANDLER_ID) { updateMediaState() }
            mpris.setPlayerStatusUpdatedHandler(MPRIS_HANDLER_ID) { updateMediaState() }
        } else {
            mpris?.removePlayerListUpdatedHandler(MPRIS_HANDLER_ID)
            mpris?.removePlayerStatusUpdatedHandler(MPRIS_HANDLER_ID)
            _mediaPlaybackState.value = null
        }

        val sysVolume = device.getPlugin<SystemVolumePlugin>()
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
        _desktopPreviewState.value = DesktopPreviewState.Waiting
        val device = currentDevice
        if (device != null) {
            val mpris = device.getPlugin<MprisPlugin>()
            mpris?.removePlayerListUpdatedHandler(MPRIS_HANDLER_ID)
            mpris?.removePlayerStatusUpdatedHandler(MPRIS_HANDLER_ID)

            val sysVolume = device.getPlugin<SystemVolumePlugin>()
            sysVolume?.removeSinkListener(sinkListener)
            sysVolume?.sinks?.forEach { it.removeListener(sinkUpdateListener) }
        }
        currentDevice = null
        _mediaPlaybackState.value = null
        _volumeControlState.value = null
        _connectivityStatus.value = null
    }

    fun updateConnectivityState() {
        val device = currentDevice ?: run {
            _connectivityStatus.value = null
            return
        }
        val connectivityPlugin = device.getPlugin<ConnectivityReportPlugin>()
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
        // Wallpaper is a packet the remote SENDS to us, so it must be in the remote's outgoing
        // capabilities. supportedPlugins is computed via pluginsForCapabilities, which checks the
        // remote's outgoing capabilities against the plugin's supportedPacketTypes. Using
        // remoteCanReceive() here would be wrong: it checks incomingCapabilities (what the
        // remote can receive from us), not outgoingCapabilities.
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
        val mpris = device.getPlugin<MprisPlugin>()
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
        val sysVolume = device.getPlugin<SystemVolumePlugin>()
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
        val mpris = currentDevice?.getPlugin<MprisPlugin>() ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendPlayPause()
        updateMediaState()
    }

    fun onMediaNext() {
        val mpris = currentDevice?.getPlugin<MprisPlugin>() ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendNext()
        updateMediaState()
    }

    fun onMediaPrevious() {
        val mpris = currentDevice?.getPlugin<MprisPlugin>() ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendPrevious()
        updateMediaState()
    }

    fun onMediaSeek(positionMs: Long) {
        val mpris = currentDevice?.getPlugin<MprisPlugin>() ?: return
        val player = mpris.getPlayerStatus(mpris.playerList.firstOrNull())
        player?.sendSetPosition(positionMs.toInt())
        updateMediaState()
    }

    fun onVolumeChange(newVolume: Int) {
        val sysVolume = currentDevice?.getPlugin<SystemVolumePlugin>() ?: return
        val sinks = sysVolume.sinks
        val defaultSink = sinks.firstOrNull { it.isDefault } ?: sinks.firstOrNull()
        if (defaultSink != null) {
            sysVolume.sendVolume(defaultSink.name, newVolume)
            defaultSink.setVolume(newVolume)
            updateVolumeState()
        }
    }

    fun onVolumeToggleMute() {
        val sysVolume = currentDevice?.getPlugin<SystemVolumePlugin>() ?: return
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
