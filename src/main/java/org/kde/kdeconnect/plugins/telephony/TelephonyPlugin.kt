/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.telephony

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import org.kde.kdeconnect.NetworkPacket
import org.kde.kdeconnect.helpers.ContactsHelper
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin
import org.kde.kdeconnect.plugins.notifications.NotificationReceiver
import org.kde.kdeconnect.ui.PluginSettingsFragment
import org.kde.kdeconnect_tp.R
import java.util.Timer
import java.util.TimerTask

@LoadablePlugin
class TelephonyPlugin : Plugin(), NotificationReceiver.NotificationListener {
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var lastPacket: NetworkPacket? = null
    private var isMuted = false

    // Native dialer packages whose calls are handled by the existing broadcast path.
    // We ignore them here to avoid double notifications for native cellular calls.
    private val nativeDialerPackages = setOf(
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer"
    )

    // Tracks active VoIP calls by StatusBarNotification.key so we do not re-fire on
    // notification updates (e.g. call answered) and can dismiss the popup on removal.
    private val activeVoipCalls = mutableMapOf<String, String>()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            //Log.e("TelephonyPlugin", "Telephony event: $action")
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED == intent.action) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val intState = when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                    else -> TelephonyManager.CALL_STATE_IDLE
                }

                // We will get a second broadcast with the phone number https://developer.android.com/reference/android/telephony/TelephonyManager#ACTION_PHONE_STATE_CHANGED
                if (!intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) return
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (intState != lastState) {
                    lastState = intState
                    callBroadcastReceived(intState, number)
                }
            }
        }
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_telephony)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_telephony_desc)

    private fun sendRinging(phoneNumber: String?, contactName: String?) {
        if (isNumberBlocked(phoneNumber)) return

        val np = NetworkPacket(PACKET_TYPE_TELEPHONY)
        np["event"] = "ringing"

        // Don't send an empty phoneNumber; fall back to the contact name / title.
        val numberToSend = if (phoneNumber.isNullOrEmpty()) contactName else phoneNumber
        if (numberToSend != null) np["phoneNumber"] = numberToSend
        if (contactName != null) np["contactName"] = contactName

        device.sendPacket(np)
        lastPacket = np
    }

    private fun callBroadcastReceived(state: Int, phoneNumber: String?) {
        if (isNumberBlocked(phoneNumber)) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                unmuteRinger()
                var contactName: String? = null
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                if (permissionCheck == PackageManager.PERMISSION_GRANTED && phoneNumber != null) {
                    val result = ContactsHelper.phoneNumberLookup(context, phoneNumber)
                    contactName = result.name
                } else if (phoneNumber != null) {
                    contactName = phoneNumber
                }
                sendRinging(phoneNumber, contactName)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                val np = NetworkPacket(PACKET_TYPE_TELEPHONY)
                if (phoneNumber != null) np["phoneNumber"] = phoneNumber
                np["event"] = "talking"
                device.sendPacket(np)
                lastPacket = np
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                val lastPacket = lastPacket ?: return
                // Resend a cancel of the last event (can either be "ringing" or "talking")
                lastPacket["isCancel"] = "true"
                device.sendPacket(lastPacket)

                if (isMuted) {
                    val timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            unmuteRinger()
                        }
                    }, 500)
                }

                // Emit a missed call notification if needed
                if ("ringing" == lastPacket.getString("event")) {
                    val np = NetworkPacket(PACKET_TYPE_TELEPHONY)
                    np["event"] = "missedCall"
                    val phoneNumber = lastPacket.getString("phoneNumber")
                    if (phoneNumber != null) {
                        np["phoneNumber"] = phoneNumber
                    }
                    val contactName = lastPacket.getStringOrNull("contactName")
                    if (contactName != null) {
                        np["contactName"] = contactName
                    }
                    device.sendPacket(np)
                }
            }
        }
    }

    private fun unmuteRinger() {
        if (isMuted) {
            val am = context.getSystemService<AudioManager>() ?: return
            am.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            isMuted = false
        }
    }

    private fun muteRinger() {
        if (isMuted) return

        val am = context.getSystemService<AudioManager>() ?: return
        if (!am.isStreamMute(AudioManager.STREAM_RING)) {
            am.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            isMuted = true
        }
    }

    override val permissionExplanation: Int = R.string.telephony_permission_explanation

    override val optionalPermissionExplanation: Int = R.string.telephony_optional_permission_explanation

    override fun onCreate() {
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        filter.priority = 500
        context.registerReceiver(receiver, filter)

        NotificationReceiver.RunCommand(context) { service ->
            service.addListener(this@TelephonyPlugin)
        }
    }

    override fun onDestroy() {
        NotificationReceiver.RunCommand(context) { service ->
            service.removeListener(this@TelephonyPlugin)
        }
        context.unregisterReceiver(receiver)
        unmuteRinger()
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        when (np.type) {
            PACKET_TYPE_TELEPHONY_REQUEST_MUTE -> muteRinger()
        }
        return true
    }

    override fun onListenerConnected(service: NotificationReceiver) {
        // No-op: we register in onCreate and the service delivers notifications as they arrive.
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val notification = statusBarNotification.notification
        if (notification.category != Notification.CATEGORY_CALL) return

        val packageName = statusBarNotification.packageName
        if (nativeDialerPackages.contains(packageName)) return

        val key = statusBarNotification.key
        if (activeVoipCalls.containsKey(key)) return

        // Confirm this is an incoming call. On API 31+ prefer EXTRA_CALL_TYPE;
        // otherwise fall back to a full-screen intent being present.
        val incoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callType = notification.extras?.getInt(TelecomManager.EXTRA_CALL_TYPE, -1) ?: -1
            if (callType != -1) callType == 1 // TelecomManager.CALL_TYPE_INCOMING (API 31)
            else notification.fullScreenIntent != null
        } else {
            notification.fullScreenIntent != null
        }
        if (!incoming) return

        val title = notification.extras?.getString(Notification.EXTRA_TITLE) ?: return
        val phoneNumber = title

        var contactName: String? = null
        if (looksLikePhoneNumber(title)) {
            val result = ContactsHelper.phoneNumberLookup(context, title)
            contactName = result.name ?: title
        } else {
            contactName = title
        }

        activeVoipCalls[key] = phoneNumber
        sendRinging(phoneNumber, contactName)
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        val key = statusBarNotification.key
        val phoneNumber = activeVoipCalls.remove(key) ?: return

        // Cancel the ringing popup, mirroring the native IDLE path's cancel.
        val cancelPacket = NetworkPacket(PACKET_TYPE_TELEPHONY)
        cancelPacket["event"] = "ringing"
        cancelPacket["isCancel"] = "true"
        cancelPacket["phoneNumber"] = phoneNumber
        device.sendPacket(cancelPacket)

        // WhatsApp posts its own missed-call notification, which the notifications
        // plugin already syncs, so we don't synthesize one here.
    }

    private fun looksLikePhoneNumber(candidate: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PhoneNumberUtils.isGlobalPhoneNumber(candidate)
        } else {
            candidate.any { it.isDigit() } && candidate.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' || it == '.' }
        }
    }

    private fun isNumberBlocked(number: String?): Boolean {
        val blockedNumbers: List<String> = preferences.getString(KEY_PREF_BLOCKED_NUMBERS, "")!!.split("\n").dropLastWhile { it.isEmpty() }

        return blockedNumbers.any { s -> PhoneNumberUtils.compare(number, s) }
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_TELEPHONY_REQUEST_MUTE)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_TELEPHONY)

    override val requiredPermissions: Array<String> = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)

    override val optionalPermissions: Array<String> = arrayOf(Manifest.permission.READ_CONTACTS)

    override val hasSettings: Boolean = true

    override val settingsScope = SettingsScope.APP_WIDE

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return PluginSettingsFragment.newInstance(pluginKey, device.deviceId, R.xml.telephonyplugin_preferences)
    }

    companion object {
        /**
         * Packet used for simple call events
         *
         * It contains the key "event" which maps to a string indicating the type of event:
         * - "ringing" - A phone call is incoming
         * - "missedCall" - An incoming call was not answered
         * - "sms" - An incoming SMS message
         * - Note: As of this writing (15 May 2018) the SMS interface is being improved and this type of event
         * is no longer the preferred way of handling SMS. Use the packets defined by the SMS plugin instead.
         *
         * Depending on the event, other fields may be defined
         */
        const val PACKET_TYPE_TELEPHONY: String = "kdeconnect.telephony"

        /**
         * Packet sent to indicate the user has requested the device mute its ringer
         *
         * The body should be empty
         */
        private const val PACKET_TYPE_TELEPHONY_REQUEST_MUTE = "kdeconnect.telephony.request_mute"

        private const val KEY_PREF_BLOCKED_NUMBERS = "telephony_blocked_numbers"
    }
}
