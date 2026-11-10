/*
 * SPDX-FileCopyrightText: 2023 Dmitry Yudin <dgyudin@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.mousepad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect_tp.R

private const val INPUT_CACHE_KEY = "compose_send_input_cache"

class ComposeSendActivity : AppCompatActivity() {
    private lateinit var deviceId: String
    private lateinit var device: Device
    private lateinit var plugin: MousePadPlugin
    private val userInput = mutableStateOf(String())
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs.getString(INPUT_CACHE_KEY, null)?.let { userInput.value = it }

        deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)!!
        device = PocketLink.getInstance().getDevice(deviceId) ?: run {
            finish()
            return
        }
        plugin = device.getPlugin<MousePadPlugin>() ?: run {
            finish()
            return
        }

        setContent {
            PocketLinkTheme(this) {
                RemoteKeyboardScreen(
                    deviceName = device.name,
                    input = userInput,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onClear = { clearComposeInput() },
                    onSend = { sendComposed() },
                    modifier = androidx.compose.ui.Modifier.safeDrawingPadding()
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        prefs.edit {
            if (userInput.value.isNotBlank()) putString(INPUT_CACHE_KEY,userInput.value)
            else remove(INPUT_CACHE_KEY)
        }
    }

    private fun sendComposed() {
        if (!device.isReachable) {
            // Keep the text so it can be sent once the device reconnects
            Toast.makeText(this, R.string.error_not_reachable, Toast.LENGTH_SHORT).show()
            return
        }
        plugin.sendText(userInput.value)
        clearComposeInput()
    }

    private fun clearComposeInput() {
        userInput.value = String()
    }

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"
    }
}
