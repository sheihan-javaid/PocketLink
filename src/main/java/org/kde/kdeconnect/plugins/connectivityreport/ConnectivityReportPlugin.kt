/*
 * SPDX-FileCopyrightText: 2025 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.connectivityreport

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.kde.kdeconnect.NetworkPacket
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin
import org.kde.kdeconnect.plugins.connectivityreport.ConnectivityListener.SubscriptionState
import org.kde.kdeconnect_tp.R

@LoadablePlugin
class ConnectivityReportPlugin : Plugin() {

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report_desc)

    /**
     * Packet used to report the current connectivity state
     *
     * The body should contain a key "signalStrengths" which has a dict that maps
     * a SubscriptionID (opaque value) to a dict with the connection info (See below)
     *
     * For example:
     * {
     *     "signalStrengths": {
     *         "6": {
     *             "networkType": "4G",
     *             "signalStrength": 3
     *         },
     *         "17": {
     *             "networkType": "HSPA",
     *             "signalStrength": 2
     *         },
     *         ...
     *     }
     * }
     */
    private val connectivityInfo = NetworkPacket(PACKET_TYPE_CONNECTIVITY_REPORT)

    var listener = object : ConnectivityListener.StateCallback {
        override fun statesChanged(states : Map<Int, SubscriptionState>) {
            if (states.isEmpty()) {
                return
            }
            val signalStrengths = buildJsonObject {
                states.forEach { (subID: Int, subscriptionState: SubscriptionState) ->
                    putJsonObject(subID.toString()) {
                        put("networkType", subscriptionState.networkType)
                        put("signalStrength", subscriptionState.signalStrength)
                    }
                }
            }
            connectivityInfo["signalStrengths"] = signalStrengths
            val ssid = getCurrentWifiSsid()
            if (ssid.isNotBlank()) {
                connectivityInfo["ssid"] = ssid
            }
            device.sendPacket(connectivityInfo)
        }
    }

    /**
     * Returns the current WiFi SSID, or blank string if unavailable.
     */
    private fun getCurrentWifiSsid(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return ""
        }
        val wifiManager = context.getSystemService<WifiManager>() ?: return ""
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo.supplicantState != SupplicantState.COMPLETED) return ""
        val ssid = wifiInfo.ssid
        return when {
            ssid.equals("<unknown ssid>", ignoreCase = true) || ssid.isBlank() -> ""
            else -> ssid
        }
    }

    /**
     * The latest connectivity information received from the linked device. Will be empty or null
     * if the linked device has not sent us any information yet.
     */
    var remoteConnectivityInfo: Map<String, DeviceConnectivityInfo> = emptyMap()
        private set

    override fun onCreate() {
        if (checkRequiredPermissions()) {
            ConnectivityListener.getInstance(context).listenStateChanges(listener)
        }
    }

    override fun onDestroy() {
        ConnectivityListener.getInstance(context).cancelActiveListener(listener)
    }

    override fun loadPluginWhenRequiredPermissionsMissing(): Boolean = true

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        if (PACKET_TYPE_CONNECTIVITY_REPORT != np.type) {
            return false
        }
        remoteConnectivityInfo = DeviceConnectivityInfo.fromPacket(np)
        device.onPluginsChanged()
        return true
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_CONNECTIVITY_REPORT)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_CONNECTIVITY_REPORT)

    override val requiredPermissions: Array<String> = arrayOf(Manifest.permission.READ_PHONE_STATE)

    companion object {
        const val PACKET_TYPE_CONNECTIVITY_REPORT = "kdeconnect.connectivity_report"
    }
}
