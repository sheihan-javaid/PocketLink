package org.kde.kdeconnect.plugins.notifications

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.NetworkPacket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NotificationsPluginTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var appDatabase: AppDatabase
    private lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        NotificationsPlugin.clearAppIconLruCache()
        NotificationsPlugin.clearRateLimitTimestamps()
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = true

        context = mockk(relaxed = true)
        device = mockk(relaxed = true)
        appDatabase = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)

        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo(any(), any<Int>()) } throws PackageManager.NameNotFoundException()
        every { appDatabase.isEnabled(any()) } returns true
        every { appDatabase.getPrivacy(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        NotificationsPlugin.clearAppIconLruCache()
        NotificationsPlugin.clearRateLimitTimestamps()
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = true
        unmockkAll()
    }

    private fun createMockPlugin(): NotificationsPlugin {
        val plugin = spyk(NotificationsPlugin())
        plugin.setContext(context, device)
        plugin.setAppDatabase(appDatabase)
        return plugin
    }

    private fun createMockNotification(
        packageName: String,
        color: Int = Notification.COLOR_DEFAULT,
        title: String = "Test Title",
        text: String = "Test Text"
    ): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        val extras = mockk<Bundle>(relaxed = true)

        every { extras.getCharSequence(any()) } answers {
            when (firstArg<String>()) {
                "android.title" -> title
                "android.text" -> text
                else -> null
            }
        }
        every { extras.containsKey(any()) } returns false

        notification.flags = 0
        notification.category = null
        notification.color = color
        notification.extras = extras
        notification.actions = null
        notification.tickerText = null
        notification.icon = 0

        every { notification.getLargeIcon() } returns null
        every { notification.smallIcon } returns null

        every { sbn.notification } returns notification
        every { sbn.packageName } returns packageName
        every { sbn.postTime } returns 1700000000000L
        every { sbn.isClearable } returns true
        every { sbn.tag } returns null
        every { sbn.key } returns "0|$packageName|1|null|1000"
        every { sbn.id } returns 1

        return sbn
    }

    @Test
    fun testHexColorFormatting() {
        // COLOR_DEFAULT (0) should format to null (omitted)
        assertNull(NotificationsPlugin.formatNotificationColor(Notification.COLOR_DEFAULT))
        assertNull(NotificationsPlugin.formatNotificationColor(0))

        // Negative ARGB ints (high bit set for alpha)
        // 0xFF123456 = -15584170
        assertEquals("#123456", NotificationsPlugin.formatNotificationColor(0xFF123456.toInt()))

        // 0xFF000000 = -16777216 (opaque black)
        assertEquals("#000000", NotificationsPlugin.formatNotificationColor(0xFF000000.toInt()))

        // Mask off alpha: 0x7F112233
        assertEquals("#112233", NotificationsPlugin.formatNotificationColor(0x7F112233))

        // Pure red, green, blue
        assertEquals("#FF0000", NotificationsPlugin.formatNotificationColor(0xFFFF0000.toInt()))
        assertEquals("#00FF00", NotificationsPlugin.formatNotificationColor(0xFF00FF00.toInt()))
        assertEquals("#0000FF", NotificationsPlugin.formatNotificationColor(0xFF0000FF.toInt()))

        // Padding with leading zeros
        assertEquals("#000007", NotificationsPlugin.formatNotificationColor(0xFF000007.toInt()))
        assertEquals("#0000A5", NotificationsPlugin.formatNotificationColor(0xFF0000A5.toInt()))
    }

    @Test
    fun testSha256ShortHash() {
        val testBytes = "PocketLink".toByteArray(Charsets.UTF_8)
        val hash = NotificationsPlugin.getSha256ShortHash(testBytes)
        assertEquals(16, hash.length)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val expectedFull = md.digest(testBytes).joinToString("") { "%02x".format(it) }
        assertEquals(expectedFull.substring(0, 16), hash)
    }

    @Test
    fun testCacheKeyBehavior() {
        val key1 = NotificationsPlugin.getLauncherIconCacheKey("com.whatsapp", 123456789L)
        assertEquals("com.whatsapp:123456789", key1)

        val key2 = NotificationsPlugin.getLauncherIconCacheKey("com.whatsapp", 987654321L)
        assertEquals("com.whatsapp:987654321", key2)

        val key3 = NotificationsPlugin.getLauncherIconCacheKey("com.whatsapp", 123456789L)
        assertEquals(key1, key3)
    }

    @Test
    fun testLruCacheCapacity() {
        // Cap is 40 entries
        for (i in 1..45) {
            val key = "app.pkg.$i:100"
            val entry = NotificationsPlugin.AppIconCacheEntry("base64_$i", "hash_$i")
            NotificationsPlugin.putAppIconInCache(key, entry)
        }
        assertEquals(40, NotificationsPlugin.getAppIconCacheSize())
        // Oldest entries (1..5) should have been evicted
        assertNull(NotificationsPlugin.getAppIconFromCache("app.pkg.1:100"))
        assertNull(NotificationsPlugin.getAppIconFromCache("app.pkg.5:100"))
        // Recent entries (6..45) should remain
        assertNotNull(NotificationsPlugin.getAppIconFromCache("app.pkg.45:100"))
    }

    @Test
    fun testAdaptiveIconRenderingDimensions() {
        mockkStatic(Bitmap::class)
        mockkConstructor(Canvas::class)

        val fullBmp = mockk<Bitmap>(relaxed = true)
        val croppedBmp = mockk<Bitmap>(relaxed = true)
        val finalBmp = mockk<Bitmap>(relaxed = true)

        every { Bitmap.createBitmap(324, 324, any<Bitmap.Config>()) } returns fullBmp
        every { Bitmap.createBitmap(fullBmp, 54, 54, 216, 216) } returns croppedBmp
        every { Bitmap.createScaledBitmap(croppedBmp, 128, 128, true) } returns finalBmp

        val bgDrawable = mockk<Drawable>(relaxed = true)
        val fgDrawable = mockk<Drawable>(relaxed = true)

        val adaptiveIcon = mockk<AdaptiveIconDrawable>()
        every { adaptiveIcon.background } returns bgDrawable
        every { adaptiveIcon.foreground } returns fgDrawable

        val plugin = NotificationsPlugin()
        val result = plugin.renderAdaptiveIcon(adaptiveIcon)

        assertEquals(finalBmp, result)
        verify { bgDrawable.setBounds(0, 0, 324, 324) }
        verify { fgDrawable.setBounds(0, 0, 324, 324) }
        verify { Bitmap.createBitmap(fullBmp, 54, 54, 216, 216) }
        verify { Bitmap.createScaledBitmap(croppedBmp, 128, 128, true) }
    }

    @Test
    fun testLegacyIconRenderingDimensions() {
        mockkStatic(Bitmap::class)
        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().setDrawFilter(any()) } returns Unit

        val finalBmp = mockk<Bitmap>(relaxed = true)
        every { Bitmap.createBitmap(128, 128, any<Bitmap.Config>()) } returns finalBmp

        val legacyDrawable = mockk<Drawable>(relaxed = true)

        val plugin = NotificationsPlugin()
        val result = plugin.renderLegacyIcon(legacyDrawable)

        assertEquals(finalBmp, result)
        verify { legacyDrawable.setBounds(0, 0, 128, 128) }
        verify { Bitmap.createBitmap(128, 128, any<Bitmap.Config>()) }
    }

    @Test
    fun testPlIconKindSelection() {
        val plugin = createMockPlugin()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } returns true

        // 1. Large icon extracted
        every { plugin.extractIcon(any(), any()) } returns NotificationsPlugin.ExtractedIcon(mockBitmap, isLarge = true)
        val sbnLarge = createMockNotification("com.test.large", color = 0xFF123456.toInt())
        val packetLarge = plugin.buildNotificationPacket(sbnLarge, false)
        assertNotNull(packetLarge)
        assertEquals("large", packetLarge!!.getString("plIconKind"))
        assertEquals("#123456", packetLarge.getString("plColor"))

        // 2. Small icon extracted
        every { plugin.extractIcon(any(), any()) } returns NotificationsPlugin.ExtractedIcon(mockBitmap, isLarge = false)
        val sbnSmall = createMockNotification("com.test.small", color = 0)
        val packetSmall = plugin.buildNotificationPacket(sbnSmall, false)
        assertNotNull(packetSmall)
        assertEquals("small", packetSmall!!.getString("plIconKind"))
        // Default color -> plColor omitted
        assertFalse(packetSmall.has("plColor"))

        // 3. Flag OFF -> plIconKind and plColor omitted
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = false
        val packetFlagOff = plugin.buildNotificationPacket(sbnLarge, false)
        assertNotNull(packetFlagOff)
        assertFalse(packetFlagOff!!.has("plIconKind"))
        assertFalse(packetFlagOff.has("plColor"))
    }

    @Test
    fun testRateLimitAndSessionTracking() {
        val plugin = createMockPlugin()
        val dummyEntry = NotificationsPlugin.AppIconCacheEntry("BASE64DATA", "hash123456789012")
        every { plugin.getLauncherIconData("com.whatsapp") } returns dummyEntry
        every { plugin.extractIcon(any(), any()) } returns null

        val sbn = createMockNotification("com.whatsapp", color = 0)

        // 1. First notification in session: sends plAppIcon + plAppIconHash
        val np1 = plugin.buildNotificationPacket(sbn, false)!!
        assertEquals("hash123456789012", np1.getString("plAppIconHash"))
        assertEquals("BASE64DATA", np1.getString("plAppIcon"))
        plugin.markAppIconSent("com.whatsapp", "hash123456789012")

        // 2. Second notification in same session: sends only plAppIconHash
        val np2 = plugin.buildNotificationPacket(sbn, false)!!
        assertEquals("hash123456789012", np2.getString("plAppIconHash"))
        assertFalse(np2.has("plAppIcon"))

        // 3. Reconnect occurs (session cleared), but within 10 minutes: rate limited!
        plugin.clearSessionSentAppIcons()
        val np3 = plugin.buildNotificationPacket(sbn, false)!!
        assertEquals("hash123456789012", np3.getString("plAppIconHash"))
        assertFalse(np3.has("plAppIcon")) // rate limited across reconnect!

        // 4. Fast forward time past 10 minutes (e.g. 11 minutes later)
        val pastTime = SystemClock.elapsedRealtime() - (11 * 60 * 1000L)
        NotificationsPlugin.setLastSentAppIconTime("com.whatsapp", pastTime)
        plugin.clearSessionSentAppIcons() // new session after 11 min
        val np4 = plugin.buildNotificationPacket(sbn, false)!!
        assertEquals("hash123456789012", np4.getString("plAppIconHash"))
        assertEquals("BASE64DATA", np4.getString("plAppIcon")) // Allowed after 10 min window
    }

    @Test
    fun testInFlightExtractionGuard() {
        val plugin = createMockPlugin()
        val executionCount = AtomicInteger(0)

        every { plugin.getPackageLastUpdateTime("com.whatsapp") } returns 1000L
        // Mock getApplicationIcon to simulate slow extraction
        every { packageManager.getApplicationIcon("com.whatsapp") } answers {
            executionCount.incrementAndGet()
            Thread.sleep(100) // simulate extraction work
            mockk<Drawable>(relaxed = true)
        }
        every { plugin.renderLegacyIcon(any()) } returns mockk(relaxed = true)
        every { plugin.encodeAppIconPng(any()) } returns Pair(ByteArray(10), "TESTBASE64")

        val threadCount = 4
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        for (i in 1..threadCount) {
            executor.execute {
                try {
                    plugin.getLauncherIconData("com.whatsapp")
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        executor.shutdown()

        // Guard must have ensured only 1 extraction occurred despite 4 concurrent requests
        assertEquals(1, executionCount.get())
    }

    @Test
    fun testSerializedBodyExceeds32KbDropsAppIcon() {
        val plugin = createMockPlugin()
        // Provide an app icon entry
        val largeBase64 = "A".repeat(20 * 1024)
        val dummyEntry = NotificationsPlugin.AppIconCacheEntry(largeBase64, "hash123456789012")
        every { plugin.getLauncherIconData("com.test.large") } returns dummyEntry
        every { plugin.extractIcon(any(), any()) } returns null

        // Notification with text (~15 KB), causing total body > 32 KB with app icon (~35 KB total)
        // but <= 32 KB without app icon (~15 KB total).
        val longText = "B".repeat(15 * 1024)
        val sbn = createMockNotification("com.test.large", color = 0, text = longText)

        val packet = plugin.buildNotificationPacket(sbn, false)!!
        // plAppIcon must be dropped, plAppIconHash kept!
        assertFalse(packet.has("plAppIcon"))
        assertEquals("hash123456789012", packet.getString("plAppIconHash"))

        // Body byte size must be <= 32 KB
        val bodyBytes = NotificationsPlugin.getSerializedBodyByteSize(packet.serialize())
        assertTrue(bodyBytes <= 32 * 1024)
    }

    @Test
    fun testFlagOffEquivalence() {
        val plugin = createMockPlugin()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } returns true
        every { plugin.extractIcon(any(), any()) } returns NotificationsPlugin.ExtractedIcon(mockBitmap, isLarge = true)
        every { plugin.getLauncherIconData(any()) } returns NotificationsPlugin.AppIconCacheEntry("BASE64", "hash123456789012")

        val sbn = createMockNotification("com.test.app", color = 0xFF336699.toInt(), title = "Hello", text = "World")

        // 1. With flag ON
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = true
        val richPacket = plugin.buildNotificationPacket(sbn, false)!!
        assertTrue(richPacket.has("plIconKind"))
        assertTrue(richPacket.has("plColor"))
        assertTrue(richPacket.has("plAppIcon"))
        assertTrue(richPacket.has("plAppIconHash"))

        // 2. With flag OFF
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = false
        val legacyPacket = plugin.buildNotificationPacket(sbn, false)!!
        assertFalse(legacyPacket.has("plIconKind"))
        assertFalse(legacyPacket.has("plColor"))
        assertFalse(legacyPacket.has("plAppIcon"))
        assertFalse(legacyPacket.has("plAppIconHash"))

        // Verify standard keys remain identical
        assertEquals(richPacket.getString("id"), legacyPacket.getString("id"))
        assertEquals(richPacket.getString("appName"), legacyPacket.getString("appName"))
        assertEquals(richPacket.getString("title"), legacyPacket.getString("title"))
        assertEquals(richPacket.getString("text"), legacyPacket.getString("text"))
        assertEquals(richPacket.getString("time"), legacyPacket.getString("time"))
        assertEquals(richPacket.getBoolean("silent"), legacyPacket.getBoolean("silent"))
        assertEquals(richPacket.getBoolean("isClearable"), legacyPacket.getBoolean("isClearable"))
    }

    @Test
    fun testRealNotificationFixtures() {
        val plugin = createMockPlugin()
        val dummyIcon = NotificationsPlugin.AppIconCacheEntry(
            base64 = "iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAAD1BMVEUAAAD///8AAAC1tbX///+m" +
                    "55QOAAAAAXRSTlMAQObYZgAAAFNJREFUeNrtwTEBAAAAwqD1T20ND6AAAAAAAAAAAAAAA" +
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgA0Y" +
                    "kQABgN7kXgAAAABJRU5ErkJggg==",
            hash = "2c75a40b9381c190"
        )

        val fixtures = listOf(
            Triple("WhatsApp (Chat DM)", "com.whatsapp", 0xFF25D366.toInt()),
            Triple("WhatsApp (Group)", "com.whatsapp", 0xFF25D366.toInt()),
            Triple("Gmail", "com.google.android.gm", 0xFFEA4335.toInt()),
            Triple("Instagram", "com.instagram.android", 0xFFE1306C.toInt()),
            Triple("Chrome", "com.android.chrome", 0xFF4285F4.toInt()),
            Triple("Settings", "com.android.settings", Notification.COLOR_DEFAULT)
        )

        for ((label, pkg, color) in fixtures) {
            every { plugin.getLauncherIconData(pkg) } returns dummyIcon
            val mockBitmap = mockk<Bitmap>(relaxed = true)
            every { mockBitmap.compress(any(), any(), any()) } returns true
            val isLarge = (label.contains("Chat") || label.contains("Gmail"))
            every { plugin.extractIcon(any(), any()) } returns NotificationsPlugin.ExtractedIcon(mockBitmap, isLarge = isLarge)

            val sbn = createMockNotification(pkg, color = color, title = "$label Notification", text = "Sample message")
            val packet = plugin.buildNotificationPacket(sbn, false)!!

            val plIconKind = packet.getStringOrNull("plIconKind")
            val plColor = packet.getStringOrNull("plColor")
            val plAppIcon = packet.getStringOrNull("plAppIcon")
            val plAppIconHash = packet.getStringOrNull("plAppIconHash")

            val appIconTruncated = if (plAppIcon != null) "<base64 len=${plAppIcon.length}>" else "null"

            println("=== Real Notification Fixture: $label ===")
            println("Package: $pkg")
            println("plIconKind: $plIconKind")
            println("plColor: $plColor")
            println("plAppIconHash: $plAppIconHash")
            println("plAppIcon: $appIconTruncated")
            println("Serialized Packet Body: ${packet.serialize().trim()}")
            println("==========================================")

            assertEquals(if (isLarge) "large" else "small", plIconKind)
            if (color != Notification.COLOR_DEFAULT) {
                assertNotNull(plColor)
                assertTrue(plColor!!.startsWith("#"))
            } else {
                assertNull(plColor)
            }
            assertEquals("2c75a40b9381c190", plAppIconHash)

            if (label == "WhatsApp (Group)") {
                // Subsequent notification from the same package in the same session must omit plAppIcon
                assertNull(plAppIcon)
            } else {
                // First notification from package must send plAppIcon
                assertNotNull(plAppIcon)
                plugin.markAppIconSent(pkg, dummyIcon.hash)
            }
        }
    }

    @Test
    fun testNotificationOrderingThroughExecutor() {
        val plugin = createMockPlugin()
        val executor = Executors.newSingleThreadExecutor()
        plugin.setNotificationExecutor(executor)

        val processed = Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(3)

        for (i in 1..3) {
            executor.execute {
                if (i == 1) Thread.sleep(50)
                processed.add(i)
                latch.countDown()
            }
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        executor.shutdown()
        assertEquals(listOf(1, 2, 3), processed)
    }

    @Test
    fun testNoPlKeysWrittenWhenFlagOffAndNoNullValues() {
        val plugin = createMockPlugin()
        NotificationsPlugin.ENABLE_RICH_NOTIFICATION_ICONS = false
        every { plugin.extractIcon(any(), any()) } returns null

        val sbn = createMockNotification("com.test.app", color = 0xFF123456.toInt())
        val packet = plugin.buildNotificationPacket(sbn, false)!!

        assertFalse(packet.has("plIconKind"))
        assertFalse(packet.has("plColor"))
        assertFalse(packet.has("plAppIcon"))
        assertFalse(packet.has("plAppIconHash"))

        val jsonString = packet.serialize()
        assertFalse(jsonString.contains("plIconKind"))
        assertFalse(jsonString.contains("plColor"))
        assertFalse(jsonString.contains("plAppIcon"))
        assertFalse(jsonString.contains("plAppIconHash"))
        assertFalse(jsonString.contains(":null"))
    }

    @Test
    fun testPlIconKindStillSentWhenPayloadSkipped() {
        val plugin = createMockPlugin()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } returns true
        every { plugin.extractIcon(any(), any()) } returns NotificationsPlugin.ExtractedIcon(mockBitmap, isLarge = false)
        every { plugin.getLauncherIconData(any()) } returns null

        val sbn = createMockNotification("com.test.app")

        // First packet sends payload and payloadHash
        val np1 = plugin.buildNotificationPacket(sbn, false)!!
        assertNotNull(np1.payload)
        assertEquals("small", np1.getString("plIconKind"))

        // Second packet with same icon: payload is skipped, but plIconKind and payloadHash must still be present!
        val np2 = plugin.buildNotificationPacket(sbn, false)!!
        assertNull(np2.payload) // payload skipped because hash matched cache
        assertEquals("small", np2.getString("plIconKind")) // plIconKind STILL SENT!
        assertTrue(np2.has("payloadHash"))
    }

    @Test
    fun testRecordAppIconOnlyAfterSendPacketSucceeds() {
        val plugin = createMockPlugin()
        val dummyEntry = NotificationsPlugin.AppIconCacheEntry("BASE64DATA", "hash123456789012")
        every { plugin.getLauncherIconData("com.whatsapp") } returns dummyEntry
        every { plugin.extractIcon(any(), any()) } returns null

        var capturedCallback: Device.SendPacketStatusCallback? = null
        every { device.sendPacket(any(), any()) } answers {
            capturedCallback = secondArg()
        }

        val sbn = createMockNotification("com.whatsapp", color = 0)

        // Reflection to call private sendNotification
        val sendNotiMethod = NotificationsPlugin::class.java.getDeclaredMethod("sendNotification", StatusBarNotification::class.java, Boolean::class.javaPrimitiveType)
        sendNotiMethod.isAccessible = true

        // 1. sendNotification called
        sendNotiMethod.invoke(plugin, sbn, false)
        assertNotNull(capturedCallback)

        // Simulate failure: neither sessionSentAppIcons nor lastSentAppIconTime should be recorded
        capturedCallback!!.onFailure(Exception("Network error"))

        val npFail = plugin.buildNotificationPacket(sbn, false)!!
        // plAppIcon must STILL be present because sendPacket failed!
        assertTrue(npFail.has("plAppIcon"))

        // 2. Now simulate success
        sendNotiMethod.invoke(plugin, sbn, false)
        capturedCallback!!.onSuccess()

        val npSuccess = plugin.buildNotificationPacket(sbn, false)!!
        // Now plAppIcon must be omitted because sendPacket succeeded!
        assertFalse(npSuccess.has("plAppIcon"))
        assertTrue(npSuccess.has("plAppIconHash"))
    }
}
