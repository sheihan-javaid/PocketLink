/*
 * SPDX-FileCopyrightText: 2026 PocketLink contributors
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

/** Moves legacy app-wide settings to the storage scope declared by each plugin. */
object SettingsScopeMigration {
    internal const val MIGRATION_PREFS_KEY = "settings_scope_migration_version"
    internal const val CURRENT_MIGRATION_VERSION = 1
    internal const val UNRESOLVED_KEYS_KEY = "migration_unresolved_keys"
    private const val TAG = "SettingsScopeMigration"

    private val pluginKeys = mapOf(
        "PresenterPlugin" to listOf("pref_presenter_enable_volume_keys", "pref_presenter_sensitivity"),
        "SharePlugin" to listOf("share_destination_custom", "share_destination_folder_preference", "share_notification_preference"),
        "MousePadPlugin" to listOf("mousepad_single_tap_key", "mousepad_double_tap_key", "mousepad_triple_tap_key", "mousepad_sensitivity_key", "mousepad_acceleration_profile_key", "mousepad_scroll_direction", "mousepad_scroll_sensitivity", "gyro_mouse_enabled", "gyro_mouse_sensitivity", "mousepad_mouse_buttons_enabled_pref", "mousepad_doubletap_drag_enabled_pref", "pref_sendkeystrokes_enabled", "pref_send_safe_text_immediately", "pref_mousepad_show_keyboard", "bigscreen_hide_mouse_input"),
        "TelephonyPlugin" to listOf("telephony_blocked_numbers"),
        "DigitizerPlugin" to listOf("digitizer_preference_key_hide_draw_button", "digitizer_preference_key_draw_button_side"),
        "SMSPlugin" to listOf("set_group_message_as_mms", "set_long_text_as_mms", "convert_to_mms_after"),
        "MprisPlugin" to listOf("mpris_time_key", "mpris_notification_key", "mpris_keepwatching_key", "pref_mpris_volume_control_key"),
        "RemoteKeyboardIMEPlugin" to listOf("remotekeyboard_editing_only"),
        "FindMyPhonePlugin" to listOf("findmyphone_preference_key_ringtone", "findmyphone_preference_key_flashlight"),
        "RunCommandPlugin" to listOf("set_runcommand_name_as_title"),
    )
    private val devicePrefixes = mapOf("SharePlugin" to "key_unreachable_url_list_", "RunCommandPlugin" to "commands_preference_")
    private val perPlugin = setOf("TelephonyPlugin", "MprisPlugin", "RemoteKeyboardIMEPlugin", "FindMyPhonePlugin")
    private val deviceIdRegex = Regex("^[a-zA-Z0-9_-]{32,38}$")

    /** Called before devices/plugins are restored. */
    fun initializeMigration(context: Context) = Unit

    // Synchronous commit() is required during migration to check persistence before removing source keys or updating flags.
    @SuppressLint("ApplySharedPref")
    @Synchronized
    fun runMigration(context: Context, trustedDeviceIds: Set<String>) {
        val source = PreferenceManager.getDefaultSharedPreferences(context)
        val unresolved = (source.getStringSet(UNRESOLVED_KEYS_KEY, emptySet()) ?: emptySet()).toMutableSet()
        var failed = false
        for ((plugin, staticKeys) in pluginKeys) {
            val prefix = devicePrefixes[plugin]
            val keys = (staticKeys + if (prefix == null) emptyList() else source.all.keys.filter { it.startsWith(prefix) }).distinct()
            var pluginFailed = false
            keys.forEach { key ->
                try {
                    if (!source.contains(key)) { unresolved.remove(key); return@forEach }
                    val dynamicId = prefix?.let { if (key.startsWith(it)) key.removePrefix(it) else null }
                    if (dynamicId != null && !deviceIdRegex.matches(dynamicId)) {
                        unresolved.add(key); return@forEach
                    }
                    val value = source.all[key] ?: return@forEach
                    val targetIds: List<String?> = when {
                        perPlugin.contains(plugin) -> listOf(null)
                        dynamicId != null -> if (dynamicId in trustedDeviceIds) listOf(dynamicId) else { unresolved.add(key); emptyList() }
                        else -> trustedDeviceIds.sorted().map { it }
                    }
                    if (targetIds.isEmpty()) return@forEach
                    var destinationsReady = true
                    for (deviceId in targetIds) {
                        val name = if (deviceId == null) "${plugin}_preferences" else "${deviceId}_${plugin}_preferences"
                        val dest = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                        if (!dest.contains(key)) {
                            if (!writeValue(dest, key, value)) destinationsReady = false
                        } else {
                            val existing = dest.all[key]
                            if (!sameType(existing, value)) {
                                Log.w(TAG, "Destination key '$key' has type mismatch (expected ${value.javaClass.simpleName}); replacing with source value")
                                if (!writeValue(dest, key, value)) destinationsReady = false
                            }
                        }
                    }
                    if (destinationsReady) {
                        if (source.edit().remove(key).commit()) unresolved.remove(key) else { pluginFailed = true; unresolved.add(key) }
                    } else {
                        pluginFailed = true; unresolved.add(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed migrating key '$key' for plugin '$plugin'", e)
                    pluginFailed = true
                    unresolved.add(key)
                }
            }
            if (!source.edit().putString("migration_plugin_$plugin", if (pluginFailed) "FAILED" else "COMPLETED").commit()) failed = true
            if (pluginFailed) failed = true
        }
        source.edit().putStringSet(UNRESOLVED_KEYS_KEY, unresolved.toSet()).commit()
        // Unresolved entries with no trusted target are recoverable on a later pair. Commit the
        // version only when all attempted writes/removals succeeded; failed destinations retry.
        if (!failed) {
            source.edit().putInt(MIGRATION_PREFS_KEY, CURRENT_MIGRATION_VERSION).commit()
        } else {
            source.edit().remove(MIGRATION_PREFS_KEY).commit()
        }
        trustedDeviceIds.forEach { id -> pluginKeys.keys.forEach { plugin ->
            val state = if (isPluginReady(source, plugin)) "COMPLETED" else "PENDING"
            source.edit().putString("migration_device_${id}_$plugin", state).commit()
        } }
    }

    private fun isPluginReady(prefs: SharedPreferences, plugin: String): Boolean =
        prefs.getString("migration_plugin_$plugin", null) == "COMPLETED"

    private fun sameType(a: Any?, b: Any?): Boolean = when {
        a == null || b == null -> false
        a is Set<*> && b is Set<*> -> true
        else -> a.javaClass == b.javaClass
    }

    private fun sameValue(a: Any?, b: Any?): Boolean = when {
        a is Set<*> && b is Set<*> -> a == b
        else -> a == b && a?.javaClass == b?.javaClass
    }

    // Synchronous commit() ensures boolean return status indicates actual write success to destination file.
    @SuppressLint("ApplySharedPref")
    private fun writeValue(prefs: SharedPreferences, key: String, value: Any): Boolean = try {
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> {
                if (value.any { it !is String }) return false
                editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
            else -> return false
        }
        editor.commit()
    } catch (e: Exception) {
        Log.w(TAG, "Could not migrate preference '$key'", e); false
    }

    fun isMigrationReady(context: Context, pluginKey: String, deviceId: String?): Boolean {
        // Plugins that did not use app-wide settings have no migration prerequisite.
        if (pluginKey !in pluginKeys) return true
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getString("migration_plugin_$pluginKey", null) != "COMPLETED") return false
        return deviceId == null || prefs.getString("migration_device_${deviceId}_$pluginKey", null) == "COMPLETED"
    }

    /** Re-evaluate readiness after a device is paired/re-paired; source values remain until copied. */
    // Synchronous commit() persists device readiness immediately before plugin instantiation.
    @SuppressLint("ApplySharedPref")
    fun markDevicePluginReady(context: Context, pluginKey: String, deviceId: String) {
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isKeyPending = (pluginKeys[pluginKey] ?: emptyList()).any(defaultPrefs::contains) ||
            (devicePrefixes[pluginKey]?.let { p -> defaultPrefs.all.keys.any { it.startsWith(p) } } == true)
        defaultPrefs.edit().putString("migration_device_${deviceId}_$pluginKey", if (isKeyPending) "PENDING" else "COMPLETED").commit()
    }

    // Synchronous commit() ensures clean state is persisted before re-evaluating readiness.
    @SuppressLint("ApplySharedPref")
    fun clearDeviceReadiness(context: Context, deviceId: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        pluginKeys.keys.forEach { editor.remove("migration_device_${deviceId}_$it") }
        editor.commit()
    }

    fun getUnresolvedKeys(context: Context): Set<String> =
        PreferenceManager.getDefaultSharedPreferences(context).getStringSet(UNRESOLVED_KEYS_KEY, emptySet())?.toSet() ?: emptySet()
}
