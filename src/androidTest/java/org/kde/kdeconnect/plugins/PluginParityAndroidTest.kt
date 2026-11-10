/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluginParityAndroidTest {

    private lateinit var context: Context

    private val expectedPluginKeys = setOf(
        "BatteryPlugin",
        "ClipboardPlugin",
        "ConnectivityReportPlugin",
        "ContactsPlugin",
        "DigitizerPlugin",
        "FindMyPhonePlugin",
        "FindRemoteDevicePlugin",
        "InputDevicesReceiverPlugin",
        "MousePadPlugin",
        "MouseReceiverPlugin",
        "MprisPlugin",
        "MprisReceiverPlugin",
        "NotificationsPlugin",
        "PingPlugin",
        "PresenterPlugin",
        "ReceiveNotificationsPlugin",
        "RemoteKeyboardPlugin",
        "RemoteKeyboardIMEPlugin",
        "RunCommandPlugin",
        "SftpPlugin",
        "SharePlugin",
        "SMSPlugin",
        "SystemVolumePlugin",
        "TelephonyPlugin"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PluginFactory.initPluginInfo(context)
    }

    @Test
    fun allExpectedPluginsAreRegisteredAndDiscoverable() {
        val available = PluginFactory.availablePlugins
        assertEquals("Exactly 24 plugins must be registered in PluginFactory", 24, available.size)
        assertEquals("Available plugin set must match expected inventory", expectedPluginKeys, available)
    }

    @Test
    fun allPluginsHaveNonEmptyMetadataAndCanBeInstantiated() {
        for (key in expectedPluginKeys) {
            val info = PluginFactory.getPluginInfo(key)
            assertNotNull("PluginInfo for $key must not be null", info)
            assertTrue("Display name for $key must be non-empty", info.displayName.isNotBlank())
            assertTrue("Description for $key must be non-empty", info.description.isNotBlank())
            assertNotNull("Instantiable class for $key must exist", info.instantiableClass)

            // Verify constructor instantiation
            val instance = info.instantiableClass.getDeclaredConstructor().newInstance()
            instance.setContext(context, null)
            assertEquals("Plugin instance key must match registration key", key, instance.pluginKey)
        }
    }

    @Test
    fun bidirectionalAndReceiverPluginPairsAreBothRegistered() {
        // Remote keyboard: both Accessibility-based and IME-based plugins must coexist
        assertTrue("RemoteKeyboardPlugin must be registered", PluginFactory.availablePlugins.contains("RemoteKeyboardPlugin"))
        assertTrue("RemoteKeyboardIMEPlugin must be registered", PluginFactory.availablePlugins.contains("RemoteKeyboardIMEPlugin"))

        // MPRIS: both Remote player control and Local media session receiver must coexist
        assertTrue("MprisPlugin must be registered", PluginFactory.availablePlugins.contains("MprisPlugin"))
        assertTrue("MprisReceiverPlugin must be registered", PluginFactory.availablePlugins.contains("MprisReceiverPlugin"))

        // Notifications: both outgoing notification forwarding and incoming notification reception must coexist
        assertTrue("NotificationsPlugin must be registered", PluginFactory.availablePlugins.contains("NotificationsPlugin"))
        assertTrue("ReceiveNotificationsPlugin must be registered", PluginFactory.availablePlugins.contains("ReceiveNotificationsPlugin"))

        // Remote mouse / pointer input: both outgoing Touchpad and incoming Mouse receiver + Input devices receiver must coexist
        assertTrue("MousePadPlugin must be registered", PluginFactory.availablePlugins.contains("MousePadPlugin"))
        assertTrue("MouseReceiverPlugin must be registered", PluginFactory.availablePlugins.contains("MouseReceiverPlugin"))
        assertTrue("InputDevicesReceiverPlugin must be registered", PluginFactory.availablePlugins.contains("InputDevicesReceiverPlugin"))

        // Telephony, SMS, Contacts, Battery, Connectivity Report, Find Remote Device, Ping, System Volume, Digitizer
        assertTrue("TelephonyPlugin must be registered", PluginFactory.availablePlugins.contains("TelephonyPlugin"))
        assertTrue("SMSPlugin must be registered", PluginFactory.availablePlugins.contains("SMSPlugin"))
        assertTrue("ContactsPlugin must be registered", PluginFactory.availablePlugins.contains("ContactsPlugin"))
        assertTrue("BatteryPlugin must be registered", PluginFactory.availablePlugins.contains("BatteryPlugin"))
        assertTrue("ConnectivityReportPlugin must be registered", PluginFactory.availablePlugins.contains("ConnectivityReportPlugin"))
        assertTrue("FindRemoteDevicePlugin must be registered", PluginFactory.availablePlugins.contains("FindRemoteDevicePlugin"))
        assertTrue("PingPlugin must be registered", PluginFactory.availablePlugins.contains("PingPlugin"))
        assertTrue("SystemVolumePlugin must be registered", PluginFactory.availablePlugins.contains("SystemVolumePlugin"))
        assertTrue("DigitizerPlugin must be registered", PluginFactory.availablePlugins.contains("DigitizerPlugin"))
    }

    @Test
    fun capabilityNegotiationIncludesCorePacketTypes() {
        val incoming = PluginFactory.incomingCapabilities
        val outgoing = PluginFactory.outgoingCapabilities

        // Battery
        assertTrue("Must support incoming battery packet", incoming.contains("kdeconnect.battery"))
        assertTrue("Must support outgoing battery packet", outgoing.contains("kdeconnect.battery"))

        // Mousepad
        assertTrue("Must support incoming mousepad request", incoming.contains("kdeconnect.mousepad.request"))
        assertTrue("Must support outgoing mousepad request", outgoing.contains("kdeconnect.mousepad.request"))

        // MPRIS
        assertTrue("Must support incoming mpris packet", incoming.contains("kdeconnect.mpris"))
        assertTrue("Must support outgoing mpris packet", outgoing.contains("kdeconnect.mpris"))

        // Notifications
        assertTrue("Must support incoming notification packet", incoming.contains("kdeconnect.notification"))
        assertTrue("Must support outgoing notification packet", outgoing.contains("kdeconnect.notification"))

        // Ping
        assertTrue("Must support incoming ping packet", incoming.contains("kdeconnect.ping"))
        assertTrue("Must support outgoing ping packet", outgoing.contains("kdeconnect.ping"))

        // SFTP (phone receives sftp.request and sends sftp parameters)
        assertTrue("Must support incoming sftp request packet", incoming.contains("kdeconnect.sftp.request"))
        assertTrue("Must support outgoing sftp packet", outgoing.contains("kdeconnect.sftp"))

        // Share
        assertTrue("Must support incoming share request", incoming.contains("kdeconnect.share.request"))
        assertTrue("Must support outgoing share request", outgoing.contains("kdeconnect.share.request"))
    }
}
