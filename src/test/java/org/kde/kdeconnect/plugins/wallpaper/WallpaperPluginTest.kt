/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.wallpaper

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.NetworkPacket
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class WallpaperPluginTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var plugin: WallpaperPlugin

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        WallpaperRepository.initialize(context)
        device = mockk {
            every { deviceId } returns "testDeviceWallpaperPlugin0000000000001"
        }
        plugin = WallpaperPlugin().apply {
            setContext(context, device)
        }
        WallpaperRepository.deleteDevice(device.deviceId)
    }

    @Test
    fun unsupportedPacketTypeReturnsFalse() {
        assertFalse(plugin.onPacketReceived(NetworkPacket("kdeconnect.battery")))
    }

    @Test
    fun supportedPacketTypeWithoutPayloadReturnsFalse() {
        val np = NetworkPacket(WallpaperPlugin.PACKET_TYPE_WALLPAPER)
        np["sha256"] = "anyhash"
        assertFalse(plugin.onPacketReceived(np))
    }

    @Test
    fun supportedPacketTypeWithPayloadReturnsTrue() {
        val bytes = "test-wallpaper-bytes".toByteArray()
        val sha = bytes.joinToString("") { "%02x".format(it) } // not a real hash, just non-matching so store runs
        val np = NetworkPacket(WallpaperPlugin.PACKET_TYPE_WALLPAPER).apply {
            this["sha256"] = sha
            this["mimeType"] = "image/png"
            this["width"] = 4
            this["height"] = 4
            payload = org.kde.kdeconnect.NetworkPacket.Payload(bytes)
        }
        assertTrue(plugin.onPacketReceived(np))
    }

    @Test
    fun isEnabledByDefaultIsFalse() {
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun supportedPacketTypesContainsWallpaper() {
        assertEquals(listOf(WallpaperPlugin.PACKET_TYPE_WALLPAPER), plugin.supportedPacketTypes.toList())
    }

    @Test
    fun outgoingPacketTypesIsEmpty() {
        assertTrue(plugin.outgoingPacketTypes.isEmpty())
    }
}
