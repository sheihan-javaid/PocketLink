/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect.backends.loopback.LoopbackLink
import org.kde.kdeconnect.backends.loopback.LoopbackLinkProvider
import org.kde.kdeconnect.plugins.battery.BatteryPlugin

@RunWith(AndroidJUnit4::class)
class PluginSettingsActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var testDevice: Device
    private lateinit var testDeviceId: String

    @Before
    fun setUp() {
        val linkProvider = LoopbackLinkProvider(context)
        val link = LoopbackLink(context, linkProvider)
        testDevice = Device(context, link)
        testDeviceId = testDevice.deviceId

        val batteryPlugin = BatteryPlugin().apply {
            setContext(context, testDevice)
        }
        testDevice.loadedPlugins["BatteryPlugin"] = batteryPlugin
        PocketLink.getInstance().devices[testDeviceId] = testDevice
    }

    @After
    fun tearDown() {
        if (::testDeviceId.isInitialized) {
            PocketLink.getInstance().devices.remove(testDeviceId)
        }
    }

    @Test
    fun missingPluginKeyFinishesActivityWithoutCrashing() {
        val intent = Intent(context, PluginSettingsActivity::class.java).apply {
            putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, testDeviceId)
        }
        ActivityScenario.launch<PluginSettingsActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish and be destroyed when plugin key is missing",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun blankPluginKeyFinishesActivityWithoutCrashing() {
        val intent = Intent(context, PluginSettingsActivity::class.java).apply {
            putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, testDeviceId)
            putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, "   ")
        }
        ActivityScenario.launch<PluginSettingsActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish and be destroyed when plugin key is blank",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun unknownPluginKeyFinishesActivityWithoutCrashing() {
        val intent = Intent(context, PluginSettingsActivity::class.java).apply {
            putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, testDeviceId)
            putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, "NonExistentPluginKey")
        }
        ActivityScenario.launch<PluginSettingsActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish and be destroyed when plugin key is unknown",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun pluginWithoutSettingsFinishesActivityWithoutCrashing() {
        val intent = Intent(context, PluginSettingsActivity::class.java).apply {
            putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, testDeviceId)
            putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, "BatteryPlugin")
        }
        ActivityScenario.launch<PluginSettingsActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish and be destroyed when plugin has no settings",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }
}
