/*
 * SPDX-FileCopyrightText: 2026 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsPluginAndroidTest {

    private lateinit var context: Context
    private lateinit var plugin: NotificationsPlugin

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        plugin = NotificationsPlugin()
        plugin.setContext(context, null)
    }

    @Test
    fun testRealLauncherIconExtractionOnDevice() {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(0)
            .filter { appInfo ->
                try {
                    pm.getApplicationIcon(appInfo.packageName)
                    true
                } catch (_: Exception) {
                    false
                }
            }

        assertTrue("Device must have at least two installed packages with icons", installedApps.size >= 2)

        val pkg1 = installedApps[0].packageName
        val pkg2 = installedApps[1].packageName

        val entry1 = plugin.getLauncherIconData(pkg1)
        val entry2 = plugin.getLauncherIconData(pkg2)

        assertNotNull("Launcher icon for package 1 ($pkg1) must not be null", entry1)
        assertNotNull("Launcher icon for package 2 ($pkg2) must not be null", entry2)

        // Decode base64 PNG bytes
        val bytes1 = Base64.decode(entry1!!.base64, Base64.NO_WRAP)
        val bytes2 = Base64.decode(entry2!!.base64, Base64.NO_WRAP)

        val bitmap1 = BitmapFactory.decodeByteArray(bytes1, 0, bytes1.size)
        val bitmap2 = BitmapFactory.decodeByteArray(bytes2, 0, bytes2.size)

        assertNotNull("Decoded bitmap 1 must not be null", bitmap1)
        assertNotNull("Decoded bitmap 2 must not be null", bitmap2)

        // Assert 128x128 dimensions
        assertEquals("Bitmap 1 width must be 128", 128, bitmap1.width)
        assertEquals("Bitmap 1 height must be 128", 128, bitmap1.height)
        assertEquals("Bitmap 2 width must be 128", 128, bitmap2.width)
        assertEquals("Bitmap 2 height must be 128", 128, bitmap2.height)

        // Assert not fully transparent (check that at least some pixels have alpha > 0)
        assertTrue("Bitmap 1 must not be fully transparent", hasNonTransparentPixels(bitmap1))
        assertTrue("Bitmap 2 must not be fully transparent", hasNonTransparentPixels(bitmap2))

        // Assert hashes are 16 hex chars and differ
        assertEquals("Hash 1 must be 16 hex characters", 16, entry1.hash.length)
        assertEquals("Hash 2 must be 16 hex characters", 16, entry2.hash.length)
        assertNotEquals("Launcher icon hashes for distinct packages ($pkg1, $pkg2) must differ", entry1.hash, entry2.hash)
    }

    private fun hasNonTransparentPixels(bitmap: Bitmap): Boolean {
        // Sample several points across the bitmap
        val stepX = bitmap.width / 8
        val stepY = bitmap.height / 8
        for (x in stepX until bitmap.width step stepX) {
            for (y in stepY until bitmap.height step stepY) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) {
                    return true
                }
            }
        }
        return false
    }
}
