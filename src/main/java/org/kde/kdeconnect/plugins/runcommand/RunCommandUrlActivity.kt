/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.runcommand

import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect_tp.R

class RunCommandUrlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != null) {
            try {
                val uri = intent.data ?: run {
                    finish()
                    return
                }
                if (uri.pathSegments.size < 2) {
                    finish()
                    return
                }
                val deviceId = uri.pathSegments[0]
                val commandKey = uri.pathSegments[1]

                val device = PocketLink.getInstance().getDevice(deviceId)

                if (device == null) {
                    error(R.string.runcommand_nosuchdevice)
                    return
                }

                if (!device.isPaired) {
                    error(R.string.runcommand_notpaired)
                    return
                }

                if (!device.isReachable) {
                    error(R.string.runcommand_notreachable)
                    return
                }

                val plugin = device.getPlugin<RunCommandPlugin>()
                if (plugin == null) {
                    error(R.string.runcommand_noruncommandplugin)
                    return
                }

                val commandName = plugin.commandList.firstOrNull { it.key == commandKey }?.name ?: commandKey
                val deviceName = device.name

                showConfirmationDialog(deviceName, commandName) {
                    plugin.runCommand(commandKey)
                    val vibrator = getSystemService(Vibrator::class.java)
                    if (vibrator != null && vibrator.hasVibrator()) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                    finish()
                }
            } catch (e: Exception) {
                Log.e("RuncommandPlugin", "Exception", e)
                finish()
            }
        } else {
            finish()
        }
    }

    @VisibleForTesting
    fun showConfirmationDialog(deviceName: String, commandName: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.runcommand_confirm_title)
            .setMessage(getString(R.string.runcommand_confirm_description, commandName, deviceName))
            .setPositiveButton(R.string.runcommand_confirm_button) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun error(@StringRes message: Int) {
        val view = TextView(this).apply {
            setText(message)
            gravity = Gravity.CENTER
        }
        setContentView(view)
    }
}
