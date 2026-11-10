/*
 * SPDX-FileCopyrightText: 2019 Juan David Vega <jdvr.93@hotmail.com>
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.kde.kdeconnect.helpers.TrustedNetworkHelper
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.screen.trustednetworks.TrustedNetworksScreen

class TrustedNetworksActivity : AppCompatActivity() {

    private val trustedNetworkHelper: TrustedNetworkHelper by lazy {
        TrustedNetworkHelper(applicationContext)
    }

    private val trustedNetworks = mutableStateListOf<String>()
    private var allNetworksAllowed by mutableStateOf(true)
    private var currentSsid by mutableStateOf<String?>(null)
    private var hasLocationPermission by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            allNetworksAllowed = false
            trustedNetworkHelper.allNetworksAllowed = false
            currentSsid = trustedNetworkHelper.getCurrentSSID()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasLocationPermission = trustedNetworkHelper.hasPermissions
        allNetworksAllowed = trustedNetworkHelper.allNetworksAllowed
        trustedNetworks.clear()
        trustedNetworks.addAll(trustedNetworkHelper.trustedNetworks)
        currentSsid = trustedNetworkHelper.getCurrentSSID()

        setContent {
            PocketLinkTheme(this) {
                TrustedNetworksScreen(
                    allNetworksAllowed = allNetworksAllowed,
                    trustedNetworks = trustedNetworks,
                    currentSsid = currentSsid,
                    hasLocationPermission = hasLocationPermission,
                    onNavigateUp = { onBackPressedDispatcher.onBackPressed() },
                    onAllNetworksAllowedChange = { allowed ->
                        if (trustedNetworkHelper.hasPermissions) {
                            allNetworksAllowed = allowed
                            trustedNetworkHelper.allNetworksAllowed = allowed
                            currentSsid = trustedNetworkHelper.getCurrentSSID()
                        }
                    },
                    onAddCurrentNetwork = {
                        val ssid = currentSsid
                        if (ssid != null && ssid !in trustedNetworks) {
                            trustedNetworks.add(ssid)
                            trustedNetworkHelper.trustedNetworks = trustedNetworks.toList()
                        }
                    },
                    onDeleteNetwork = { network ->
                        trustedNetworks.remove(network)
                        trustedNetworkHelper.trustedNetworks = trustedNetworks.toList()
                        currentSsid = trustedNetworkHelper.getCurrentSSID()
                    },
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasLocationPermission = trustedNetworkHelper.hasPermissions
        if (!trustedNetworkHelper.hasPermissions) {
            allNetworksAllowed = true
        }
        currentSsid = trustedNetworkHelper.getCurrentSSID()
    }
}
