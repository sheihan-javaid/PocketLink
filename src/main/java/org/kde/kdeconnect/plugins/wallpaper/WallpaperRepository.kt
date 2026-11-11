/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kde.kdeconnect.helpers.JsonHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "KDE/WallpaperRepository"

private const val DIR_NAME = "wallpapers"
private const val IMAGE_NAME = "wallpaper.img"
private const val META_NAME = "wallpaper.meta"

internal const val MAX_BYTES: Long = 8L * 1024 * 1024 // 8 MB
private const val MAX_SIDE = 4096
private const val MAX_PIXELS: Long = 16L * 1024 * 1024 // 16 MP

@Serializable
data class WallpaperMeta(
    val sha256: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val lastModified: Long
)

data class WallpaperInfo(
    val deviceId: String,
    val file: File,
    val sha256: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val lastModified: Long
)

class WallpaperException(message: String) : Exception(message)

/**
 * Process-scoped repository caching the desktop wallpaper received from the connected Mac companion.
 *
 * State is per device and persisted under [Context.getNoBackupFilesDir]/wallpapers/<deviceId>/ so it
 * survives plugin reloads and disconnects (AvailableOffline). The cache is deleted only on device
 * unpair or user-initiated plugin disable.
 */
object WallpaperRepository {
    private lateinit var context: Context

    private val deviceFlows = ConcurrentHashMap<String, MutableStateFlow<WallpaperInfo?>>()
    private val deviceLocks = ConcurrentHashMap<String, Any>()

    fun initialize(context: Context) {
        this.context = context.applicationContext
        clearOrphanedTempFiles()
    }

    /**
     * Deletes orphaned [.wallpaper.tmp] files left behind by a crashed transfer.
     * Runs once at application startup, when no transfer can be in-progress in
     * this process, so we never touch an active transfer's temp file.
     */
    private fun clearOrphanedTempFiles() {
        val wallpapersRoot = File(context.getNoBackupFilesDir(), DIR_NAME)
        if (!wallpapersRoot.exists()) return
        wallpapersRoot.listFiles()?.forEach { deviceDir ->
            val tmp = File(deviceDir, ".wallpaper.tmp")
            if (tmp.exists()) {
                tmp.delete()
                Log.i(TAG, "Deleted orphaned wallpaper temp file for ${deviceDir.name}")
            }
        }
    }

    /**
     * Returns a per-device [StateFlow] emitting the latest [WallpaperInfo], rehydrated from disk.
     * Never emits a [android.graphics.Bitmap]; only file path + metadata.
     */
    fun getWallpaperInfo(deviceId: String): StateFlow<WallpaperInfo?> {
        return deviceFlows.getOrPut(deviceId) {
            MutableStateFlow(loadFromDisk(deviceId))
        }.asStateFlow()
    }

    /**
     * Receives a wallpaper payload stream, validates it, and stores it atomically.
     * Must be called on [Dispatchers.IO]. Returns the stored [WallpaperInfo] or null on failure.
     * Always closes [inputStream], including on rejection.
     */
    suspend fun store(
        deviceId: String,
        inputStream: InputStream,
        claimedSha256: String,
        claimedMimeType: String,
        claimedWidth: Int,
        claimedHeight: Int
    ): WallpaperInfo? = withContext(Dispatchers.IO) {
        val dir = deviceDir(deviceId)
        dir.mkdirs()
        val lock = deviceLocks.getOrPut(deviceId) { Any() }
        synchronized(lock) {
            val tempFile = File(dir, ".wallpaper.tmp")
            var success = false
            var storedInfo: WallpaperInfo? = null
            inputStream.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                try {
                    DigestInputStream(input, digest).use { digestInput ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (true) {
                                read = digestInput.read(buffer)
                                if (read == -1) break
                                if (read > 0) {
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }

                    val totalBytes = tempFile.length()
                    if (totalBytes > MAX_BYTES) {
                        throw WallpaperException("Exceeds max size ($MAX_BYTES bytes)")
                    }

                    val computedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (computedSha256 != claimedSha256.lowercase()) {
                        throw WallpaperException("SHA-256 mismatch")
                    }

                    // Validate by content: decoded bounds, not the body's mimeType/width/height hints.
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(tempFile.absolutePath, options)
                    if (options.outWidth <= 0 || options.outHeight <= 0) {
                        throw WallpaperException("Undecodable image")
                    }
                    if (options.outWidth > MAX_SIDE || options.outHeight > MAX_SIDE) {
                        throw WallpaperException("Exceeds max side ($MAX_SIDE px)")
                    }
                    if (options.outWidth.toLong() * options.outHeight > MAX_PIXELS) {
                        throw WallpaperException("Exceeds max pixels ($MAX_PIXELS)")
                    }

                    val mime = detectMime(tempFile)

                    // Atomic rename on the same filesystem.
                    val imageFile = File(dir, IMAGE_NAME)
                    if (!tempFile.renameTo(imageFile)) {
                        throw WallpaperException("Failed to rename temp file")
                    }

                    val meta = WallpaperMeta(computedSha256, mime, options.outWidth, options.outHeight, System.currentTimeMillis())
                    val metaFile = File(dir, META_NAME)
                    val jsonText = JsonHelper.json.encodeToString(WallpaperMeta.serializer(), meta)
                    metaFile.outputStream().use { it.write(jsonText.toByteArray()) }

                    storedInfo = WallpaperInfo(
                        deviceId = deviceId,
                        file = imageFile,
                        sha256 = computedSha256,
                        mimeType = mime,
                        width = options.outWidth,
                        height = options.outHeight,
                        lastModified = meta.lastModified
                    )
                    deviceFlows.getOrPut(deviceId) {
                        MutableStateFlow(null)
                    }.value = storedInfo
                    success = true
                } catch (e: Exception) {
                    Log.w(TAG, "Wallpaper receive failed for $deviceId", e)
                } finally {
                    if (!success) {
                        tempFile.delete()
                    }
                }
            }
            storedInfo
        }
    }

    /** Deletes the cached wallpaper for [deviceId] (unpair path). Fires from any thread. */
    fun deleteDevice(deviceId: String) {
        // context is initialized by PocketLink.onCreate in production; guard for unit tests that
        // exercise unpair without initializing the repository.
        if (!::context.isInitialized) {
            deviceFlows[deviceId]?.value = null
            return
        }
        val dir = deviceDir(deviceId)
        File(dir, IMAGE_NAME).delete()
        File(dir, META_NAME).delete()
        deviceFlows[deviceId]?.value = null
    }

    /**
     * Deletes the cached wallpaper for [deviceId] when the user disables the plugin.
     * Fires from any thread.
     */
    fun deleteOnDisable(deviceId: String) {
        deleteDevice(deviceId)
    }

    /** Ignores/removes the legacy mac_wallpaper.png previously held in cacheDir/filesDir. */
    fun clearLegacyCache() {
        if (!::context.isInitialized) return
        File(context.cacheDir, "mac_wallpaper.png").delete()
        File(context.filesDir, "mac_wallpaper.png").delete()
    }

    private fun deviceDir(deviceId: String): File {
        val safeId = deviceId.replace(Regex("[/\\\\]"), "_")
        return File(context.getNoBackupFilesDir(), "$DIR_NAME/$safeId")
    }

    private fun loadFromDisk(deviceId: String): WallpaperInfo? {
        val dir = deviceDir(deviceId)
        val imageFile = File(dir, IMAGE_NAME)
        if (!imageFile.exists()) return null

        val metaFile = File(dir, META_NAME)
        var meta: WallpaperMeta? = null
        if (metaFile.exists()) {
            try {
                val metaText = metaFile.bufferedReader().use { it.readText() }
                meta = JsonHelper.json.decodeFromString(WallpaperMeta.serializer(), metaText)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read wallpaper sidecar for $deviceId", e)
            }
        }

        val sha256 = meta?.sha256 ?: computeSha256(imageFile)
        val mime = meta?.mimeType ?: detectMime(imageFile)
        val width = meta?.width ?: 0
        val height = meta?.height ?: 0
        val lastModified = meta?.lastModified ?: imageFile.lastModified()

        return WallpaperInfo(deviceId, imageFile, sha256, mime, width, height, lastModified)
    }

    private fun computeSha256(file: File): String {
        return FileInputStream(file).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(input, digest).use { digestInput ->
                val buffer = ByteArray(8192)
                while (digestInput.read(buffer) != -1) {
                    // bytes are digested as they're read through DigestInputStream
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }
    }

    private fun detectMime(file: File): String {
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                if (read < 4) "application/octet-stream"
                else when {
                    header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte() -> "image/jpeg"
                    header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() -> "image/png"
                    else -> "application/octet-stream"
                }
            }
        } catch (e: IOException) {
            "application/octet-stream"
        }
    }
}
