/*
 * SPDX-FileCopyrightText: 2026 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.connectivityreport

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.kde.kdeconnect.NetworkPacket

/**
 * Representation of remote subscription state received by [ConnectivityReportPlugin].
 *
 * @param networkType Cellular technology type (e.g., "5G", "4G", "HSPA", etc.)
 * @param signalStrength Signal level from 0 to 4 (or ASU level)
 */
data class DeviceConnectivityInfo(
    val networkType: String,
    val signalStrength: Int,
    val networkName: String? = null,
) {
    companion object {
        /**
         * Parses the "signalStrengths" map from a [ConnectivityReportPlugin.PACKET_TYPE_CONNECTIVITY_REPORT] packet.
         *
         * @return Map of subscriptionId string to [DeviceConnectivityInfo]
         */
        fun fromPacket(np: NetworkPacket): Map<String, DeviceConnectivityInfo> {
            require(np.type == ConnectivityReportPlugin.PACKET_TYPE_CONNECTIVITY_REPORT) {
                "Packet type must be PACKET_TYPE_CONNECTIVITY_REPORT"
            }
            val globalSsid = np.getString("ssid").takeIf { it.isNotBlank() }
                ?: np.getString("networkName").takeIf { it.isNotBlank() }
            val globalNetworkType = np.getString("networkType").takeIf { it.isNotBlank() }
            val signalStrengths = np.getJsonObject("signalStrengths")
            if (signalStrengths == null) {
                if (!globalNetworkType.isNullOrBlank() || !globalSsid.isNullOrBlank()) {
                    val netType = globalNetworkType ?: "Wi-Fi"
                    val strength = np.getInt("signalStrength", 4)
                    return mapOf("0" to DeviceConnectivityInfo(netType, strength, globalSsid))
                }
                return emptyMap()
            }
            val result = mutableMapOf<String, DeviceConnectivityInfo>()
            for ((key, signalStrength) in signalStrengths) {
                signalStrength as? JsonObject ?: continue
                val networkType = (signalStrength["networkType"] as? JsonPrimitive)?.contentOrNull
                    ?: (signalStrength["name"] as? JsonPrimitive)?.contentOrNull
                    ?: globalNetworkType
                    ?: ""
                val signalStrengthVal = (signalStrength["signalStrength"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                    ?: np.getInt("signalStrength", 0)
                val networkName = (signalStrength["ssid"] as? JsonPrimitive)?.contentOrNull
                    ?: (signalStrength["networkName"] as? JsonPrimitive)?.contentOrNull
                    ?: (signalStrength["name"] as? JsonPrimitive)?.contentOrNull
                    ?: globalSsid
                result[key] = DeviceConnectivityInfo(networkType, signalStrengthVal, networkName)
            }
            if (result.isEmpty() && (!globalNetworkType.isNullOrBlank() || !globalSsid.isNullOrBlank())) {
                val netType = globalNetworkType ?: "Wi-Fi"
                val strength = np.getInt("signalStrength", 4)
                result["0"] = DeviceConnectivityInfo(netType, strength, globalSsid)
            }
            return result
        }
    }
}
