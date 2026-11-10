/*
 * SPDX-FileCopyrightText: 2025 Martin Sh <hemisputnik@proton.me>
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.digitizer

import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect.ui.PluginSettingsActivity
import org.kde.kdeconnect_tp.R
import kotlin.math.roundToInt

class DigitizerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var deviceId: String
    private lateinit var plugin: DigitizerPlugin
    private var device: Device? = null

    private var drawingPadView: DrawingPadView? = null
    private var isFullscreen by mutableStateOf(false)
    private var hideDrawButton by mutableStateOf(false)
    private var drawButtonSide by mutableStateOf("bottom_left")

    private val fullscreenBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            updateFullscreenMode(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: run {
            finish()
            return
        }
        val currentDevice = PocketLink.getInstance().getDevice(deviceId)
        device = currentDevice
        plugin = currentDevice?.getPlugin<DigitizerPlugin>() ?: run {
            finish()
            return
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        onBackPressedDispatcher.addCallback(this, fullscreenBackCallback)

        loadPreferences()

        setContent {
            DigitizerScreen(
                deviceName = currentDevice.name,
                device = currentDevice,
                onBackPressedDispatcher = onBackPressedDispatcher,
                onOpenSettings = {
                    startActivity(
                        PluginSettingsActivity.createIntent(
                            this,
                            deviceId,
                            DigitizerPlugin::class.java.simpleName
                        )
                    )
                },
                isFullscreen = isFullscreen,
                onToggleFullscreen = { updateFullscreenMode(!isFullscreen) },
                hideDrawButton = hideDrawButton,
                drawButtonSide = drawButtonSide,
                onToolEvent = { plugin.reportEvent(it) },
                onDrawingPadReady = { pad ->
                    drawingPadView = pad
                    startSessionIfReady(pad)
                }
            )
        }
    }

    private fun loadPreferences() {
        hideDrawButton = prefs.getBoolean(
            getString(R.string.digitizer_preference_key_hide_draw_button),
            false
        )
        drawButtonSide = prefs.getString(
            getString(R.string.digitizer_preference_key_draw_button_side),
            "bottom_left"
        ) ?: "bottom_left"
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
    }

    override fun onStart() {
        super.onStart()
        drawingPadView?.let { startSessionIfReady(it) }
    }

    private fun startSessionIfReady(pad: DrawingPadView) {
        pad.post {
            if (pad.width > 0 && pad.height > 0) {
                plugin.startSession(
                    pad.width,
                    pad.height,
                    (resources.displayMetrics.xdpi * INCHES_TO_MM).roundToInt(),
                    (resources.displayMetrics.ydpi * INCHES_TO_MM).roundToInt()
                )
            }
        }
    }

    private fun updateFullscreenMode(enabled: Boolean) {
        isFullscreen = enabled
        fullscreenBackCallback.isEnabled = enabled

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStop() {
        super.onStop()
        if (::plugin.isInitialized) {
            plugin.endSession()
        }
    }

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"
        private const val INCHES_TO_MM = 0.0393701
    }
}
