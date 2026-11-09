/*
 * SPDX-FileCopyrightText: 2026 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins.notifications

import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.DialogFragment
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.NetworkPacket
import org.kde.kdeconnect.helpers.AppsHelper.appNameLookup
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin
import org.kde.kdeconnect.ui.PluginSettingsFragment
import org.kde.kdeconnect.ui.StartActivityAlertDialogFragment
import org.kde.kdeconnect_tp.BuildConfig
import org.kde.kdeconnect_tp.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

@LoadablePlugin
class NotificationsPlugin : Plugin(), NotificationReceiver.NotificationListener {
    data class AppIconCacheEntry(val base64: String, val hash: String)
    data class ExtractedIcon(val bitmap: Bitmap, val isLarge: Boolean)

    private lateinit var appDatabase: AppDatabase
    private val currentNotifications = Collections.synchronizedSet(HashSet<String>())
    // Here we will map every notification to it's icon(hash)
    private val notificationsIcons = ConcurrentHashMap<String, String>()
    private val postedNotifications = Collections.synchronizedSet(HashSet<String>())
    private val pendingIntents = ConcurrentHashMap<String, RepliableNotification>()
    private val pendingActions = ConcurrentHashMap<String, MutableList<Notification.Action>>()
    private val sessionSentAppIcons = ConcurrentHashMap<String, String>()
    private val notificationLock = Any()
    private var serviceReady = false
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var mainHandler: Handler
    private val postedNotificationsLock = Any()
    private var notificationExecutor: ExecutorService? = null

    @VisibleForTesting
    fun clearSessionSentAppIcons() {
        sessionSentAppIcons.clear()
    }

    @VisibleForTesting
    internal fun setNotificationExecutor(executor: ExecutorService?) {
        this.notificationExecutor = executor
    }

    @VisibleForTesting
    internal fun setAppDatabase(db: AppDatabase) {
        this.appDatabase = db
    }

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_notifications)

    override val description: String
        get() = context.getString(R.string.pref_plugin_notifications_desc)

    override val hasSettings: Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return NotificationsSettingsFragment.newInstance(pluginKey, device.deviceId)
    }

    override fun checkRequiredPermissions(): Boolean {
        return NotificationReceiver.hasReadNotificationsPermission(context)
    }

    override fun onCreate() {
        sessionSentAppIcons.clear()
        appDatabase = AppDatabase.getInstance(context)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        mainHandler = Handler(Looper.getMainLooper())
        notificationExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "PocketLink-Notifications")
        }
        NotificationReceiver.RunCommand(context) { service ->
            service.addListener(this@NotificationsPlugin)
        }
    }

    override fun onDestroy() {
        notificationExecutor?.shutdown()
        notificationExecutor = null

        synchronized(notificationLock) {
            currentNotifications.clear()
            notificationsIcons.clear()
            pendingIntents.clear()
            pendingActions.clear()
            sessionSentAppIcons.clear()
        }

        synchronized(postedNotificationsLock) {
            mainHandler.removeCallbacksAndMessages(null)
            postedNotifications.clear()
        }

        NotificationReceiver.RunCommand(context) { service ->
            service.removeListener(this@NotificationsPlugin)
        }
    }

    override fun onListenerConnected(service: NotificationReceiver) {
        serviceReady = true
        NotificationReceiver.RunCommand(context) { service ->
            if (ENABLE_RICH_NOTIFICATION_ICONS) {
                val executor = notificationExecutor
                if (executor != null && !executor.isShutdown) {
                    try {
                        executor.execute {
                            this.sendCurrentNotifications(service)
                        }
                    } catch (e: RejectedExecutionException) {
                        Log.w(TAG, "Notification executor rejected task", e)
                    }
                } else {
                    this.sendCurrentNotifications(service)
                }
            } else {
                this.sendCurrentNotifications(service)
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        val id = getNotificationKeyCompat(statusBarNotification)

        synchronized(postedNotificationsLock) {
            postedNotifications.remove(id)
            cancelDelayedNotification(id)
        }

        synchronized(notificationLock) {
            pendingActions.remove(id)
        }

        if (!appDatabase.isEnabled(statusBarNotification.packageName)) {
            synchronized(notificationLock) {
                currentNotifications.remove(id)
            }
            return
        }

        val np = NetworkPacket(PACKET_TYPE_NOTIFICATION)
        np["id"] = id
        np["isCancel"] = true
        device.sendPacket(np)
        synchronized(notificationLock) {
            currentNotifications.remove(id)
            notificationsIcons.remove(id)
        }
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val key = getNotificationKeyCompat(statusBarNotification)
        synchronized(postedNotificationsLock) {
            postedNotifications.add(key)
        }

        // If the user only wants to be alerted while the phone is locked, still send the notification but as silent
        val alertOnlyIfLocked = preferences.getBoolean(context.getString(R.string.pref_notification_screen_off_key), false)
        val silent = alertOnlyIfLocked && !keyguardManager.isKeyguardLocked
        sendNotificationWithDelay(statusBarNotification, silent)
    }

    private fun sendNotificationWithDelay(statusBarNotification: StatusBarNotification, silent: Boolean) {
        val key = getNotificationKeyCompat(statusBarNotification)
        synchronized(postedNotificationsLock) {
            cancelDelayedNotification(key)
            if (!postedNotifications.contains(key)) {
                return
            }
            val delayedNotification = Message.obtain(mainHandler, Runnable {
                synchronized(postedNotificationsLock) {
                    if (!postedNotifications.contains(key)) {
                        return@Runnable
                    }
                }
                if (ENABLE_RICH_NOTIFICATION_ICONS) {
                    val executor = notificationExecutor
                    if (executor != null && !executor.isShutdown) {
                        try {
                            executor.execute {
                                synchronized(postedNotificationsLock) {
                                    if (!postedNotifications.contains(key)) {
                                        return@execute
                                    }
                                }
                                sendNotification(statusBarNotification, silent)
                            }
                        } catch (e: RejectedExecutionException) {
                            Log.w(TAG, "Notification executor rejected task", e)
                        }
                    } else {
                        sendNotification(statusBarNotification, silent)
                    }
                } else {
                    sendNotification(statusBarNotification, silent)
                }
            })
            delayedNotification.obj = key
            mainHandler.sendMessageDelayed(delayedNotification, NOTIFICATION_SYNC_DELAY_MS)
        }
    }

    private fun cancelDelayedNotification(key: String) {
        mainHandler.removeCallbacksAndMessages(key)
    }

    // silent is true for notifications that shouldn't alert the user on the other end, eg: preexisting notifications
    // that we are sending in response to a request command, or new ones while the screen is on if the user asked so
    private fun sendNotification(
        statusBarNotification: StatusBarNotification,
        silent: Boolean
    ) {
        val np = buildNotificationPacket(statusBarNotification, silent) ?: return
        val packageName = statusBarNotification.packageName
        val appIconHash = if (ENABLE_RICH_NOTIFICATION_ICONS && np.has("plAppIcon")) {
            np.getStringOrNull("plAppIconHash")
        } else {
            null
        }

        if (appIconHash != null) {
            device.sendPacket(np, object : Device.SendPacketStatusCallback() {
                override fun onSuccess() {
                    markAppIconSent(packageName, appIconHash)
                }

                override fun onFailure(e: Throwable) {
                    Log.w(TAG, "Failed sending packet with launcher icon for $packageName: ${e.message}")
                }
            })
        } else {
            device.sendPacket(np)
        }
    }

    @VisibleForTesting
    internal fun markAppIconSent(packageName: String, hash: String) {
        sessionSentAppIcons[packageName] = hash
        lastSentAppIconTime[packageName] = SystemClock.elapsedRealtime()
    }

    @VisibleForTesting
    internal fun buildNotificationPacket(
        statusBarNotification: StatusBarNotification,
        silent: Boolean
    ): NetworkPacket? {
        val notification = statusBarNotification.notification

        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
            || (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            || (notification.flags and Notification.FLAG_LOCAL_ONLY) != 0
            || (notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY) != 0 // The notification that groups other notifications
            || Notification.CATEGORY_TRANSPORT == notification.category // Media playback controls are handled by the MPRIS plugin
        ) {
            // This is not a notification we want
            return null
        }

        val packageName = statusBarNotification.packageName
        if (!appDatabase.isEnabled(packageName)) {
            // Notification excluded by the user
            return null
        }

        if ("com.android.systemui" == packageName) {
            if ("low_battery" == statusBarNotification.tag) {
                // HACK: Android low battery notification are posted again every few seconds. Ignore them, as we already have a battery indicator.
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if ("MediaOngoingActivity" == notification.channelId) {
                    // HACK: Samsung OneUI sends this notification when media playback is started. Ignore it, as this is handled by Mpris plugin
                    return null
                }
            }
        }

        if ("org.kde.kdeconnect_tp" == packageName || "org.kde.kdeconnect_tp.debug" == packageName) {
            // Don't send our own notifications
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (context.packageManager.isPackageSuspended(packageName)) {
                    // App is paused/suspended by the user (Digital Wellbeing, Focus Mode, etc.)
                    return null
                }
            } catch (_: PackageManager.NameNotFoundException) {
                // Package not found or uninstalled
            }
        }

        val key = getNotificationKeyCompat(statusBarNotification)
        val isUpdate = synchronized(notificationLock) {
            val update = currentNotifications.contains(key)
            if (!update) {
                currentNotifications.add(key)
            }
            update
        }

        val np = NetworkPacket(PACKET_TYPE_NOTIFICATION)

        val extractedIcon = extractIcon(statusBarNotification, notification)
        val appIcon = extractedIcon?.bitmap

        if (appIcon != null && !appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)) {
            val iconBytes = getIconBytes(appIcon)
            val iconHash = getIconHash(iconBytes)

            // If it's the same icon, the other end should have it already, so there's no need to send it again.
            synchronized(notificationLock) {
                if (iconHash != notificationsIcons[key]) {
                    np.payload = NetworkPacket.Payload(iconBytes)
                    notificationsIcons[key] = iconHash
                }
            }
            // We should always send the icon's hash so the other end can know which icon to use.
            np["payloadHash"] = iconHash

            if (ENABLE_RICH_NOTIFICATION_ICONS) {
                np["plIconKind"] = if (extractedIcon.isLarge) "large" else "small"
            }
        }

        var launcherIconEntry: AppIconCacheEntry? = null
        if (ENABLE_RICH_NOTIFICATION_ICONS) {
            val colorStr = formatNotificationColor(notification.color)
            if (colorStr != null) {
                np["plColor"] = colorStr
            }

            if (!appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)) {
                try {
                    val entry = getLauncherIconData(packageName)
                    if (entry != null) {
                        launcherIconEntry = entry
                        np["plAppIconHash"] = entry.hash

                        val lastSentHash = sessionSentAppIcons[packageName]
                        val shouldSendIcon = (lastSentHash != entry.hash)
                        val now = SystemClock.elapsedRealtime()
                        val lastSentTime = lastSentAppIconTime[packageName] ?: 0L
                        val isRateLimited = (lastSentTime > 0L && (now - lastSentTime) < APP_ICON_RESEND_INTERVAL_MS)

                        if (shouldSendIcon && !isRateLimited) {
                            np["plAppIcon"] = entry.base64
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed attaching launcher icon for $packageName: ${e.message}")
                }
            }
        }

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        val isConversation = messagingStyle != null
        val isGroupConversation = isConversation && messagingStyle.isGroupConversation() && messagingStyle.conversationTitle != null

        synchronized(notificationLock) {
            np["actions"] = extractActions(notification, key)
        }

        // Some apps ask to notify the user only once they are posted and not for updates, but (for some reason),
        // some messaging apps also set that flag, and we should notify the user for every new message (since they
        // just update the notification), so ignore their ask.
        val isSilent = silent || (isUpdate && (notification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0 && !isConversation)

        np["id"] = key
        np["isClearable"] = statusBarNotification.isClearable
        np["appName"] = appNameLookup(context, packageName)
        np["time"] = statusBarNotification.postTime.toString()
        np["silent"] = isSilent

        if (!appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS)) {
            val rn = extractRepliableNotification(statusBarNotification)
            if (rn != null) {
                np["requestReplyId"] = rn.id
                synchronized(notificationLock) {
                    pendingIntents[rn.id] = rn
                }
            }

            np["ticker"] = getTickerText(notification)

            var conversationLength = 0
            if (isConversation) {
                val conversation = extractConversation(messagingStyle)
                conversationLength = conversation.size
                if (conversationLength != 0) {
                    np["conversation"] = conversation
                }
            }

            // even if it's a conversation, we set `title` and `text` for compatibility.
            if (isGroupConversation) {
                val groupName = messagingStyle.conversationTitle.toString()
                // FIXME: When there're more than one message in group conversation and the user didn't reply, the number of messages appears between "()" next to the name of the group
                // We don't want to show them because Android doesn't show them.
                np["groupName"] = groupName
                // HACK: To differentiate between groups messages and DMs, we set `title` to be the group name and `text` to be `<sender>: <message>`.
                np["title"] = groupName
                val (sender, content) = getMessageAt(messagingStyle, conversationLength - 1)
                np["text"] = "$sender: $content"
            } else {
                val title = extractStringFromExtra(notification.extras, NotificationCompat.EXTRA_TITLE)
                if (title != null) {
                    np["title"] = title
                }
                val text = extractText(notification)
                if (text != null) {
                    np["text"] = text
                }
            }
        }

        if (ENABLE_RICH_NOTIFICATION_ICONS && np.has("plAppIcon")) {
            val serialized = np.serialize()
            val bodyBytes = getSerializedBodyByteSize(serialized)
            if (bodyBytes > MAX_PACKET_BODY_BYTES) {
                np.remove("plAppIcon")
            }
        }
        return np
    }

    private fun extractText(notification: Notification): String? {
        val extras = notification.extras
        if (extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) {
            return extractStringFromExtra(extras, NotificationCompat.EXTRA_BIG_TEXT)
        }
        return extractStringFromExtra(extras, NotificationCompat.EXTRA_TEXT)
    }

    private fun getIconBytes(appIcon: Bitmap): ByteArray {
        val outStream = ByteArrayOutputStream()
        appIcon.compress(Bitmap.CompressFormat.PNG, 90, outStream)
        return outStream.toByteArray()
    }

    private fun getIconHash(iconBytes: ByteArray): String {
        return getChecksum(iconBytes)
    }

    private fun getMessageAt(
        messagingStyle: NotificationCompat.MessagingStyle,
        index: Int
    ): Pair<String, String> {
        val message = messagingStyle.messages.getOrNull(index)
        val sender = message?.person?.name?.toString() ?: context.getString(R.string.unknown_sender)
        val content = message?.text?.toString() ?: ""
        return Pair(sender, content)
    }

    @VisibleForTesting
    internal fun extractIcon(
        statusBarNotification: StatusBarNotification,
        notification: Notification
    ): ExtractedIcon? {
        try {
            val foreignContext = context.createPackageContext(statusBarNotification.packageName, 0)
            if (notification.getLargeIcon() != null) {
                val bmp = iconToBitmap(foreignContext, notification.getLargeIcon())
                if (bmp != null) {
                    return ExtractedIcon(bmp, isLarge = true)
                }
            }
            if (notification.smallIcon != null) {
                val bmp = iconToBitmap(foreignContext, notification.smallIcon)
                if (bmp != null) {
                    return ExtractedIcon(bmp, isLarge = false)
                }
            }
            val foreignResources = context.packageManager.getResourcesForApplication(statusBarNotification.packageName)
            val foreignIcon = foreignResources.getDrawable(notification.icon) // Might throw Resources.NotFoundException
            val bmp = drawableToBitmap(foreignIcon)
            if (bmp != null) {
                return ExtractedIcon(bmp, isLarge = false)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package name not found", e)
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Package not found", e)
        }
        return null
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    @VisibleForTesting
    internal fun renderAdaptiveIcon(drawable: AdaptiveIconDrawable): Bitmap {
        val fullSize = 324
        val safeSize = (fullSize * 72) / 108 // 216
        val offset = (fullSize - safeSize) / 2 // 54

        val fullBitmap = createBitmap(fullSize, fullSize)
        val canvas = Canvas(fullBitmap)

        drawable.background?.let { bg ->
            bg.setBounds(0, 0, fullSize, fullSize)
            bg.draw(canvas)
        }

        drawable.foreground?.let { fg ->
            fg.setBounds(0, 0, fullSize, fullSize)
            fg.draw(canvas)
        }

        val cropped = Bitmap.createBitmap(fullBitmap, offset, offset, safeSize, safeSize)
        val scaled = Bitmap.createScaledBitmap(cropped, 128, 128, true)

        if (cropped != scaled) {
            cropped.recycle()
        }
        fullBitmap.recycle()

        return scaled
    }

    @VisibleForTesting
    internal fun renderLegacyIcon(drawable: Drawable): Bitmap {
        val bitmap = createBitmap(128, 128)
        val canvas = Canvas(bitmap)
        canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        drawable.setBounds(0, 0, 128, 128)
        drawable.draw(canvas)
        return bitmap
    }

    private fun compressBitmapToPng(bitmap: Bitmap): ByteArray {
        val outStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        return outStream.toByteArray()
    }

    @VisibleForTesting
    internal fun encodeAppIconPng(bitmap: Bitmap): Pair<ByteArray, String>? {
        var pngBytes = compressBitmapToPng(bitmap)
        var base64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP)

        if (base64.length > MAX_BASE64_BYTES) {
            // Re-encode smaller: try 96x96
            val scaled96 = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            val bytes96 = compressBitmapToPng(scaled96)
            if (scaled96 != bitmap) scaled96.recycle()
            val base64_96 = Base64.encodeToString(bytes96, Base64.NO_WRAP)
            if (base64_96.length <= MAX_BASE64_BYTES) {
                pngBytes = bytes96
                base64 = base64_96
            } else {
                // Still too large, try 64x64
                val scaled64 = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                val bytes64 = compressBitmapToPng(scaled64)
                if (scaled64 != bitmap) scaled64.recycle()
                val base64_64 = Base64.encodeToString(bytes64, Base64.NO_WRAP)
                if (base64_64.length <= MAX_BASE64_BYTES) {
                    pngBytes = bytes64
                    base64 = base64_64
                } else {
                    return null
                }
            }
        }
        return Pair(pngBytes, base64)
    }

    @VisibleForTesting
    internal fun getPackageLastUpdateTime(packageName: String): Long {
        val pm = context.packageManager
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
        return packageInfo?.lastUpdateTime ?: 0L
    }

    private fun dumpLauncherIconForDebug(packageName: String, pngBytes: ByteArray) {
        if (!BuildConfig.DEBUG) return
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "$packageName.png")
            FileOutputStream(file).use { out ->
                out.write(pngBytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed dumping debug icon for $packageName: ${e.message}")
        }
    }

    @VisibleForTesting
    internal fun getLauncherIconData(packageName: String): AppIconCacheEntry? {
        val lastUpdateTime = getPackageLastUpdateTime(packageName)
        val cacheKey = getLauncherIconCacheKey(packageName, lastUpdateTime)

        val cachedEarly = appIconLruCache.get(cacheKey)
        if (cachedEarly != null) {
            Log.d(TAG, "Launcher icon: pkg=$packageName, duration=0ms, hash=${cachedEarly.hash} (hit)")
            return cachedEarly
        }

        val packageLock = packageExtractionLocks.computeIfAbsent(packageName) { Any() }
        synchronized(packageLock) {
            val startTime = SystemClock.elapsedRealtime()
            val cached = appIconLruCache.get(cacheKey)
            if (cached != null) {
                val durationMs = SystemClock.elapsedRealtime() - startTime
                Log.d(TAG, "Launcher icon: pkg=$packageName, duration=${durationMs}ms, hash=${cached.hash} (hit)")
                return cached
            }

            try {
                val pm = context.packageManager
                val appDrawable = pm.getApplicationIcon(packageName)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appDrawable is AdaptiveIconDrawable) {
                    renderAdaptiveIcon(appDrawable)
                } else {
                    renderLegacyIcon(appDrawable)
                }

                val encoded = encodeAppIconPng(bitmap)
                bitmap.recycle()

                if (encoded == null) {
                    return null
                }

                val (pngBytes, base64) = encoded
                val hash = getSha256ShortHash(pngBytes)
                val entry = AppIconCacheEntry(base64, hash)
                appIconLruCache.put(cacheKey, entry)

                if (BuildConfig.DEBUG) {
                    dumpLauncherIconForDebug(packageName, pngBytes)
                }

                val durationMs = SystemClock.elapsedRealtime() - startTime
                Log.d(TAG, "Launcher icon: pkg=$packageName, duration=${durationMs}ms, size=${pngBytes.size}B, hash=$hash (miss)")
                return entry
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extract launcher icon for $packageName: ${e.message}")
                return null
            }
        }
    }

    private fun extractActions(notification: Notification, key: String): JsonArray? {
        val notiActions = notification.actions
        if (notiActions.isNullOrEmpty()) {
            return null
        }

        return buildJsonArray {
            for (action in notiActions) {
                val title = action.title
                    ?: continue

                // Check whether it is a reply action. We have special treatment for them
                if (action.remoteInputs?.isNotEmpty() == true) {
                    continue
                }

                add(title.toString())

                pendingActions.getOrPut(key) { Collections.synchronizedList(mutableListOf()) }.add(action)
            }
        }
    }

    private fun extractConversation(messagingStyle: NotificationCompat.MessagingStyle?): JsonArray {
        if (messagingStyle == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return JsonArray(emptyList())
        }
        val messages = messagingStyle.messages
        return buildJsonArray {
            for (message in messages) {
                val sender = message?.person?.name?.toString() ?: context.getString(R.string.unknown_sender)
                val content = message.text?.toString() ?: ""
                addJsonObject {
                    put("sender", sender)
                    put("content", content)
                }
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        drawable ?: return null
        val res = if (drawable.intrinsicWidth > 128 || drawable.intrinsicHeight > 128) {
            createBitmap(96, 96)
        } else if (drawable.intrinsicWidth <= 64 || drawable.intrinsicHeight <= 64) {
            createBitmap(96, 96)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        val canvas = Canvas(res)
        drawable.setBounds(0, 0, res.getWidth(), res.getHeight())
        drawable.draw(canvas)
        return res
    }

    private fun iconToBitmap(foreignContext: Context?, icon: Icon?): Bitmap? {
        icon ?: return null
        return drawableToBitmap(icon.loadDrawable(foreignContext))
    }

    private fun replyToNotification(id: String, message: String) {
        val repliableNotification = synchronized(notificationLock) {
            pendingIntents[id]
        }
        if (repliableNotification == null) {
            Log.e(TAG, "No such notification")
            return
        }
        val remoteInputs = arrayOfNulls<RemoteInput>(repliableNotification.remoteInputs.size)

        val localIntent = Intent()
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val localBundle = Bundle()
        repliableNotification.remoteInputs.forEachIndexed { i, remoteInput ->
            remoteInputs[i] = remoteInput
            localBundle.putCharSequence(remoteInput.resultKey, message)
        }
        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle)

        try {
            repliableNotification.pendingIntent.send(context, 0, localIntent)
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "replyToNotification error: ${e.message}")
        }
        synchronized(notificationLock) {
            pendingIntents.remove(id)
        }
    }

    private fun extractRepliableNotification(statusBarNotification: StatusBarNotification): RepliableNotification? {
        val notiActions = statusBarNotification.notification.actions
            ?: return null

        for (action in notiActions) {
            if (action != null && action.remoteInputs != null && action.actionIntent != null) {
                // Is a reply
                return RepliableNotification(
                    pendingIntent = action.actionIntent,
                    remoteInputs = action.remoteInputs.toList(),
                    packageName = statusBarNotification.packageName,
                )
            }
        }

        return null
    }

    /**
     * Returns the the title and text of the notification.
     */
    private fun getTickerText(notification: Notification): String {
        var ticker = ""

        try {
            val extras = notification.extras
            val extraTitle = extractStringFromExtra(extras, NotificationCompat.EXTRA_TITLE)
            val extraText = extractStringFromExtra(extras, NotificationCompat.EXTRA_TEXT)

            if (extraTitle != null && !TextUtils.isEmpty(extraText)) {
                ticker = "$extraTitle: $extraText"
            } else if (extraTitle != null) {
                ticker = extraTitle
            } else if (extraText != null) {
                ticker = extraText
            }
        } catch (e: Exception) {
            Log.e(TAG, "problem parsing notification extras", e)
        }

        if (ticker.isEmpty()) {
            ticker = if (notification.tickerText != null) notification.tickerText.toString() else ""
        }

        return ticker
    }

    private fun sendCurrentNotifications(service: NotificationReceiver) {
        if (!NotificationReceiver.hasReadNotificationsPermission(context)) {
            return
        }
        val notifications = try {
            service.getActiveNotifications()
        } catch (_: SecurityException) {
            return
        }
        if (notifications == null) {
            return // Can happen only on API 23 and lower
        }
        for (notification in notifications) {
            sendNotification(notification, true)
        }
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        if (np.type == PACKET_TYPE_NOTIFICATION_ACTION) {
            val key = np.getString("key")
            val title = np.getString("action")
            val intent = synchronized(notificationLock) {
                pendingActions[key]?.firstOrNull { it.title?.contentEquals(title) == true }?.actionIntent
            }

            if (intent != null) {
                try {
                    intent.send()
                } catch (e: PendingIntent.CanceledException) {
                    Log.e(TAG, "Triggering action failed", e)
                }
            }
        } else if (np.getBoolean("request")) {
            sessionSentAppIcons.clear()
            if (serviceReady) {
                NotificationReceiver.RunCommand(context) { service ->
                    if (ENABLE_RICH_NOTIFICATION_ICONS) {
                        val executor = notificationExecutor
                        if (executor != null && !executor.isShutdown) {
                            try {
                                executor.execute {
                                    this.sendCurrentNotifications(service)
                                }
                            } catch (e: RejectedExecutionException) {
                                Log.w(TAG, "Notification executor rejected task", e)
                            }
                        } else {
                            this.sendCurrentNotifications(service)
                        }
                    } else {
                        this.sendCurrentNotifications(service)
                    }
                }
            }
        } else if (np.has("cancel")) {
            val dismissedId = np.getString("cancel")
            synchronized(notificationLock) {
                currentNotifications.remove(dismissedId)
            }
            NotificationReceiver.RunCommand(context) { service ->
                try {
                    service.cancelNotification(dismissedId)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Can't cancel notification", e)
                }
            }
        } else if (np.has("requestReplyId") && np.has("message")) {
            replyToNotification(np.getString("requestReplyId"), np.getString("message"))
        }

        return true
    }

    override val permissionExplanationDialog: DialogFragment
        get() = StartActivityAlertDialogFragment.Builder()
            .setTitle(R.string.pref_plugin_notifications)
            .setMessage(R.string.no_permissions)
            .setPositiveButton(R.string.open_settings)
            .setNegativeButton(R.string.cancel)
            .setIntentAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            .setReloadPluginsOnResult(true)
            .create()

    override val supportedPacketTypes = arrayOf(
            PACKET_TYPE_NOTIFICATION_REQUEST,
            PACKET_TYPE_NOTIFICATION_REPLY,
            PACKET_TYPE_NOTIFICATION_ACTION
        )

    override val outgoingPacketTypes = arrayOf(PACKET_TYPE_NOTIFICATION)


    companion object {
        private const val PACKET_TYPE_NOTIFICATION = "kdeconnect.notification"
        private const val PACKET_TYPE_NOTIFICATION_REQUEST = "kdeconnect.notification.request"
        private const val PACKET_TYPE_NOTIFICATION_REPLY = "kdeconnect.notification.reply"
        private const val PACKET_TYPE_NOTIFICATION_ACTION = "kdeconnect.notification.action"
        private const val NOTIFICATION_SYNC_DELAY_MS = 50L
        private const val MAX_BASE64_BYTES = 24 * 1024
        private const val MAX_PACKET_BODY_BYTES = 32 * 1024
        private const val APP_ICON_RESEND_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes

        private const val TAG = "KDE/NotificationsPlugin"

        @VisibleForTesting
        var ENABLE_RICH_NOTIFICATION_ICONS: Boolean = true

        private val appIconLruCache = LruCache<String, AppIconCacheEntry>(40)
        private val packageExtractionLocks = ConcurrentHashMap<String, Any>()
        private val lastSentAppIconTime = ConcurrentHashMap<String, Long>()

        @VisibleForTesting
        fun clearAppIconLruCache() {
            appIconLruCache.evictAll()
        }

        @VisibleForTesting
        fun putAppIconInCache(key: String, entry: AppIconCacheEntry) {
            appIconLruCache.put(key, entry)
        }

        @VisibleForTesting
        fun getAppIconFromCache(key: String): AppIconCacheEntry? {
            return appIconLruCache.get(key)
        }

        @VisibleForTesting
        fun getAppIconCacheSize(): Int {
            return appIconLruCache.size()
        }

        @VisibleForTesting
        fun clearRateLimitTimestamps() {
            lastSentAppIconTime.clear()
        }

        @VisibleForTesting
        fun setLastSentAppIconTime(packageName: String, timeMs: Long) {
            lastSentAppIconTime[packageName] = timeMs
        }

        @VisibleForTesting
        fun getSerializedBodyByteSize(serializedPacket: String): Int {
            val bodyKeyIndex = serializedPacket.indexOf("\"body\":")
            if (bodyKeyIndex == -1) return 0
            val bodyStart = bodyKeyIndex + 7
            val payloadIndex = serializedPacket.indexOf(",\"payload", bodyStart)
            val bodyEnd = if (payloadIndex != -1) {
                payloadIndex
            } else {
                val trimmed = serializedPacket.trimEnd()
                if (trimmed.endsWith("}}")) {
                    trimmed.length - 1
                } else {
                    val lastBrace = trimmed.lastIndexOf('}')
                    if (lastBrace > bodyStart) lastBrace else trimmed.length
                }
            }
            val bodySubstring = serializedPacket.substring(bodyStart, bodyEnd)
            return bodySubstring.toByteArray(Charsets.UTF_8).size
        }

        @VisibleForTesting
        fun formatNotificationColor(color: Int): String? {
            if (color == Notification.COLOR_DEFAULT) {
                return null
            }
            val rgb = color and 0x00FFFFFF
            return String.format(Locale.US, "#%06X", rgb)
        }

        @VisibleForTesting
        fun getSha256ShortHash(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hex = bytesToHex(digest)
            return hex.substring(0, 16)
        }

        @VisibleForTesting
        fun getLauncherIconCacheKey(packageName: String, lastUpdateTime: Long): String {
            return "$packageName:$lastUpdateTime"
        }

        private fun extractStringFromExtra(extras: Bundle, key: String): String? {
            return extras.getCharSequence(key)?.toString()
        }

        private fun getNotificationKeyCompat(statusBarNotification: StatusBarNotification): String {
            // first check if it's one of our remoteIds
            val tag = statusBarNotification.tag
            return if (tag != null && tag.startsWith("kdeconnectId:")) {
                statusBarNotification.id.toString()
            } else {
                statusBarNotification.key
            }
        }

        private fun getChecksum(data: ByteArray): String {
            val md = MessageDigest.getInstance("MD5")
            md.update(data)
            return bytesToHex(md.digest())
        }

        private fun bytesToHex(bytes: ByteArray): String {
            val hexArray = "0123456789ABCDEF".toCharArray()
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars).lowercase(Locale.getDefault())
        }
    }
}
