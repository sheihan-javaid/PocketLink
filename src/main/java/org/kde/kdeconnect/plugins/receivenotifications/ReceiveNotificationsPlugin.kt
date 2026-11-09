/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.receivenotifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.scale
import org.kde.kdeconnect.NetworkPacket
import org.kde.kdeconnect.helpers.NotificationHelper
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin
import org.kde.kdeconnect.ui.MainActivity
import org.kde.kdeconnect_tp.R

@LoadablePlugin
class ReceiveNotificationsPlugin : Plugin() {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_receive_notifications)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_receive_notifications_desc)

    override val isEnabledByDefault: Boolean = false

    // One-way notification policy: reverse notification sync (Mac -> Android) is intentionally disabled.
    override val isCompatible: Boolean
        get() = false && super.isCompatible

    override fun onCreate() {
        // Disabled per one-way notification policy: do not request remote notifications
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        // Disabled per one-way notification policy (Android -> Mac only).
        // Reverse notification mirroring (Mac -> Android) is intentionally disabled.
        // Silently consume and ignore any incoming notification packets without creating Android notifications.
        np.payload?.close()
        return false
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_NOTIFICATION)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_NOTIFICATION_REQUEST)

    override val requiredPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf()
    }

    override val permissionExplanation: Int = R.string.receive_notifications_permission_explanation

    companion object {
        private const val PACKET_TYPE_NOTIFICATION = "kdeconnect.notification"
        private const val PACKET_TYPE_NOTIFICATION_REQUEST = "kdeconnect.notification.request"
    }
}
