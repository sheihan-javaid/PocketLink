/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.wallpaper

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class WallpaperRepositoryTest {

    private lateinit var context: Context
    private val deviceId = "testDeviceWallpaperRepo00000000000001"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Application>()
        WallpaperRepository.initialize(context)
        WallpaperRepository.deleteDevice(deviceId) // clean slate
    }

    @After
    fun cleanup() {
        WallpaperRepository.deleteDevice(deviceId)
    }

    private fun createTestPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(4, 4, Config.RGB_565)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    private fun sha256Of(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun storeValidImageSucceedsAndEmits() = runBlocking {
        val bytes = createTestPngBytes()
        val sha = sha256Of(bytes)
        val info = WallpaperRepository.store(
            deviceId = deviceId,
            inputStream = ByteArrayInputStream(bytes),
            claimedSha256 = sha,
            claimedMimeType = "image/png",
            claimedWidth = 4,
            claimedHeight = 4
        )

        assertNotNull("store must return WallpaperInfo for a valid image", info)
        assertEquals(sha, info?.sha256)
        assertEquals("image/png", info?.mimeType)
        assertEquals(4, info?.width)
        assertEquals(4, info?.height)
        assertTrue("Cached wallpaper file must exist", info?.file!!.exists())

        // The per-device StateFlow must emit the stored info.
        val emitted = WallpaperRepository.getWallpaperInfo(deviceId).value
        assertNotNull("StateFlow must emit stored wallpaper", emitted)
        assertEquals(sha, emitted?.sha256)
    }

    @Test
    fun hashMismatchIsDiscarded() = runBlocking {
        val bytes = createTestPngBytes()
        val info = WallpaperRepository.store(
            deviceId = deviceId,
            inputStream = ByteArrayInputStream(bytes),
            claimedSha256 = "deadbeef00000000000000000000000000000000000000000000000000000000",
            claimedMimeType = "image/png",
            claimedWidth = 4,
            claimedHeight = 4
        )

        assertNull("store must return null on SHA-256 mismatch", info)
        assertNull("StateFlow must stay empty after discarded write", WallpaperRepository.getWallpaperInfo(deviceId).value)
    }

    @Test
    fun exceedsMaxSizeIsDiscarded() = runBlocking {
        val oversized = ByteArray((8L * 1024 * 1024 + 1024).toInt())
        val sha = sha256Of(oversized)
        val info = WallpaperRepository.store(
            deviceId = deviceId,
            inputStream = ByteArrayInputStream(oversized),
            claimedSha256 = sha,
            claimedMimeType = "image/png",
            claimedWidth = 4,
            claimedHeight = 4
        )

        assertNull("store must return null when exceeding max size", info)
        assertNull("StateFlow must stay empty after oversized write", WallpaperRepository.getWallpaperInfo(deviceId).value)
    }

    @Test
    fun deleteDeviceClearsState() = runBlocking {
        val bytes = createTestPngBytes()
        val sha = sha256Of(bytes)
        WallpaperRepository.store(
            deviceId = deviceId,
            inputStream = ByteArrayInputStream(bytes),
            claimedSha256 = sha,
            claimedMimeType = "image/png",
            claimedWidth = 4,
            claimedHeight = 4
        )
        assertNotNull(WallpaperRepository.getWallpaperInfo(deviceId).value)

        WallpaperRepository.deleteDevice(deviceId)

        assertNull("StateFlow must be cleared after deleteDevice", WallpaperRepository.getWallpaperInfo(deviceId).value)
    }

    @Test
    fun getWallpaperInfoIsNullWhenNothingCached() {
        assertNull("StateFlow must be null when nothing cached", WallpaperRepository.getWallpaperInfo(deviceId).value)
    }

    /**
     * InputStream that sleeps between bytes so a transfer stays "in-flight" long enough for
     * deleteDevice()/deleteOnDisable() to race it.
     */
    private class SlowInputStream(
        private val data: ByteArray,
        private val delayMs: Long
    ) : InputStream() {
        private var pos = 0

        override fun read(): Int {
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return -1
                }
            }
            return if (pos < data.size) data[pos++].toInt() and 0xFF else -1
        }
    }

    @Test
    fun unpairDuringTransferDoesNotResurrectWallpaper() = runBlocking {
        val bytes = createTestPngBytes()
        val sha = sha256Of(bytes)
        val storeJob = CoroutineScope(Dispatchers.IO).launch {
            WallpaperRepository.store(deviceId, SlowInputStream(bytes, 10), sha, "image/png", 4, 4)
        }
        delay(50) // let the copy start before unpairing
        WallpaperRepository.deleteDevice(deviceId) // unpair mid-transfer
        storeJob.join()

        // delete won the race: state cleared and not resurrected by the in-flight store.
        assertNull(
            "Wallpaper flow must be cleared after unpair during transfer",
            WallpaperRepository.getWallpaperInfo(deviceId).value
        )
    }

    @Test
    fun disableDuringTransferDoesNotResurrectWallpaper() = runBlocking {
        val bytes = createTestPngBytes()
        val sha = sha256Of(bytes)
        val storeJob = CoroutineScope(Dispatchers.IO).launch {
            WallpaperRepository.store(deviceId, SlowInputStream(bytes, 10), sha, "image/png", 4, 4)
        }
        delay(50) // let the copy start before disabling
        WallpaperRepository.deleteOnDisable(deviceId) // plugin disable mid-transfer
        storeJob.join()

        assertNull(
            "Wallpaper flow must be cleared after disable during transfer",
            WallpaperRepository.getWallpaperInfo(deviceId).value
        )
    }

    @Test
    fun concurrentReceivesDoNotCorruptWallpaper() = runBlocking {
        val bytesA = createTestPngBytes()
        val bytesB = createTestPngBytes()
        val shaA = sha256Of(bytesA)
        val shaB = sha256Of(bytesB)
        val jobA = CoroutineScope(Dispatchers.IO).launch {
            WallpaperRepository.store(deviceId, ByteArrayInputStream(bytesA), shaA, "image/png", 4, 4)
        }
        val jobB = CoroutineScope(Dispatchers.IO).launch {
            WallpaperRepository.store(deviceId, ByteArrayInputStream(bytesB), shaB, "image/png", 4, 4)
        }
        jobA.join()
        jobB.join()

        // Exactly one valid wallpaper must remain; no corruption or crash.
        val info = WallpaperRepository.getWallpaperInfo(deviceId).value
        assertNotNull("Concurrent receives must leave a valid wallpaper", info)
        assertTrue("Wallpaper file must exist", info.file.exists())
    }

    @Test
    fun failedStoreLeavesNoTempFile() = runBlocking {
        val bytes = createTestPngBytes()
        val dir = File(context.getNoBackupFilesDir(), "wallpapers/$deviceId")
        WallpaperRepository.store(
            deviceId,
            ByteArrayInputStream(bytes),
            "deadbeef0000000000000000000000000000000000000000000000000000000000",
            "image/png", 4, 4
        )
        val tempFiles = dir.listFiles()?.filter { it.name.startsWith(".wallpaper.tmp") } ?: emptyList()
        assertTrue("No .wallpaper.tmp must remain after a failed store", tempFiles.isEmpty())
    }
}
