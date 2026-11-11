/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.wallpaper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kde.kdeconnect.NetworkPacket
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect_tp.R

@org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin
class WallpaperPlugin : Plugin() {

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_wallpaper)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_wallpaper_desc)

    override val isEnabledByDefault: Boolean
        get() = false

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_WALLPAPER)

    override val outgoingPacketTypes: Array<String> = emptyArray()

    override fun onCreate() {
        WallpaperRepository.initialize(context)
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        if (PACKET_TYPE_WALLPAPER != np.type) return false
        val deviceId = device.deviceId

        val claimedSha256 = np.getStringOrNull("sha256") ?: return false

        // Hash short-circuit: drop without reading the payload.
        val current = WallpaperRepository.getWallpaperInfo(deviceId).value
        if (current?.sha256 == claimedSha256) {
            np.payload?.close()
            return true
        }

        val payload = np.payload ?: return false
        val inputStream = payload.inputStream ?: run {
            payload.close()
            return false
        }

        val mimeType = np.getStringOrNull("mimeType") ?: "application/octet-stream"
        val width = np.getInt("width", 0)
        val height = np.getInt("height", 0)

        // Stream copy + validation happens on the IO dispatcher; store() closes the stream.
        CoroutineScope(Dispatchers.IO).launch {
            WallpaperRepository.store(deviceId, inputStream, claimedSha256, mimeType, width, height)
        }
        return true
    }

    override fun onDeviceUnpaired(context: Context, deviceId: String) {
        WallpaperRepository.deleteDevice(deviceId)
    }

    override fun onDestroy() {
        // Abort any in-flight wallpaper transfer so it cannot outlive this plugin instance.
        WallpaperRepository.cancelStore(device.deviceId)
    }

    companion object {
        internal const val PACKET_TYPE_WALLPAPER = "pocketlink.wallpaper"
    }
}
