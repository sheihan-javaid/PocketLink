/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.kde.kdeconnect.helpers.DeviceHelper
import org.kde.kdeconnect.helpers.TrustedDevices

class SendQueueLifecycleTest {
    private val context: Context = mockk()

    @Before
    fun setUp() {
        val deviceId = "testDevice"
        val name = "Test Device"
        val encodedCertificate = """
            MIIDVzCCAj+gAwIBAgIBCjANBgkqhkiG9w0BAQUFADBVMS8wLQYDVQQDDCZfZGExNzlhOTFfZjA2
            NF80NzhlX2JlOGNfMTkzNWQ3NTQ0ZDU0XzEMMAoGA1UECgwDS0RFMRQwEgYDVQQLDAtLZGUgY29u
            bmVjdDAeFw0xNTA2MDMxMzE0MzhaFw0yNTA2MDMxMzE0MzhaMFUxLzAtBgNVBAMMJl9kYTE3OWE5
            MV9mMDY0XzQ3OGVfYmU4Y18xOTM1ZDc1NDRkNTRfMQwwCgYDVQQKDANLREUxFDASBgNVBAsMC0tk
            ZSBjb25uZWN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzH9GxS1lctpwYdSGAoPH
            ws+MnVaL0PVDCuzrpxzXc+bChR87xofhQIesLPLZEcmUJ1MlEJ6jx4W+gVhvY2tUN7SoiKKbnq8s
            WjI5ovs5yML3C1zPbOSJAdK613FcdkK+UGd/9dQk54gIozinC58iyTAChVVpB3pAF38EPxwKkuo2
            qTzwk24d6PRxz1skkzwEphUQQzGboyHsAlJHN1MzM2/yFGB4l8iUua2d3ETyfy/xFEh/SwtGtXE5
            KLz4cpb0fxjeYQZVruBKxzE07kgDO3zOhmP3LJ/KSPHWYImd1DWmpY9iDvoXr6+V7FAnRloaEIyg
            7WwdlSCpo3TXVuIjLwIDAQABozIwMDAdBgNVHQ4EFgQUwmbHo8YbiR463GRKSLL3eIKyvDkwDwYD
            VR0TAQH/BAUwAwIBADANBgkqhkiG9w0BAQUFAAOCAQEAydijH3rbnvpBDB/30w2PCGMT7O0N/XYM
            wBtUidqa4NFumJrNrccx5Ehp4UP66BfP61HW8h2U/EekYfOsZyyWd4KnsDD6ycR8h/WvpK3BC2cn
            I299wbqCEZmk5ZFFaEIDHdLAdgMCuxJkAzy9mMrWEa05Soxi2/ZXdrU9nXo5dzuPGYlirVPDHl7r
            /urBxD6HVX3ObQJRJ7r/nAWyUVdX3/biJaDRsydftOpGU6Gi5c1JK4MWIz8Bsjh6mEjCsVatbPPl
            yygGiJbDZfAvN2XoaVEBii2GDDCWfaFwPVPYlNTvjkUkMP8YThlMsiJ8Q4693XoLOL94GpNlCfUg
            7n+KOQ==
        """.trimIndent()

        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any<ByteArray>(), any()) } answers {
            java.util.Base64.getMimeEncoder().encodeToString(firstArg())
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getMimeDecoder().decode(firstArg<String>())
        }

        val deviceSettings = MockSharedPreference()
        val editor = deviceSettings.edit()
        editor.putString("deviceName", name)
        editor.putString("deviceType", "phone")
        editor.putString("certificate", encodedCertificate)
        editor.apply()
        every { context.getSharedPreferences(deviceId, Context.MODE_PRIVATE) } returns deviceSettings

        val trustedSettings = MockSharedPreference()
        trustedSettings.edit().putBoolean(deviceId, true).apply()
        every { context.getSharedPreferences("trusted_devices", Context.MODE_PRIVATE) } returns trustedSettings

        mockkStatic(PreferenceManager::class)
        val defaultSettings = MockSharedPreference()
        every { PreferenceManager.getDefaultSharedPreferences(any()) } returns defaultSettings

        mockkStatic(ContextCompat::class)
        every { context.getSystemService<NotificationManager>() } returns mockk(relaxed = true)

        mockkStatic(android.util.Log::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun sendPacketFailsFastWhenNoActiveLink() {
        val device = Device(context, "testDevice")

        var failed = false
        var succeeded = false
        device.sendPacket(NetworkPacket("test.packet"), object : Device.SendPacketStatusCallback() {
            override fun onSuccess() { succeeded = true }
            override fun onFailure(e: Throwable) { failed = true }
        })

        Assert.assertFalse("sendPacket must not report success with no active link", succeeded)
        Assert.assertTrue("sendPacket must report failure with no active link", failed)
    }

    @Test
    fun remoteCanReceiveReturnsTrueWhenCapabilitiesNull() {
        val device = Device(context, "testDevice")
        device.deviceInfo.incomingCapabilities = null
        device.deviceInfo.outgoingCapabilities = null

        Assert.assertTrue(
            "remoteCanReceive should return true when incomingCapabilities is null (legacy compat)",
            device.remoteCanReceive("kdeconnect.ping")
        )
    }
}