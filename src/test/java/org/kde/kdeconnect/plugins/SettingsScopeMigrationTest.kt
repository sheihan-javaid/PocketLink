/*
 * SPDX-FileCopyrightText: 2026 PocketLink contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [SettingsScopeMigration].
 *
 * Covers:
 * - F-01 Initial migration completion while unresolved keys remain durably recorded
 * - F-02 Reconciliation after initial migration completion
 * - F-03 Two-device global preference fan-out
 * - F-04 Fan-out interrupted after one successful destination
 * - F-05 Fan-out destination failure (real commit failure)
 * - F-06 Destination type mismatch
 * - F-07 Realistic types across plugins, plus empty string / empty StringSet / false / zero
 *        and every supported SharedPreferences type
 * - F-08 Re-pairing and per-device isolation
 * - F-09 Plugin initialization retry after a failed attempt
 * - F-10 Failed plugin is not partially registered and does not affect other plugins
 * - F-11 Exact key-prefix collision (malformed id and longer-id-with-trusted-prefix)
 * - F-12 Mpris global (PER_PLUGIN) preference preservation
 * - F-13 Restart/recovery after each durable operation
 * - F-14 Repeated reconciliation is idempotent
 * - F-15 Mixed trusted/untrusted keys: version committed, only untrusted keys left unresolved
 */
@RunWith(AndroidJUnit4::class)
class SettingsScopeMigrationTest {

    private lateinit var context: Context
    private lateinit var defaultPrefs: SharedPreferences

    private companion object {
        // Device ids are defined once so that keys built from them can never drift apart.
        val DEVICE_1 = "0".repeat(31) + "1"
        val DEVICE_2 = "0".repeat(31) + "2"

        const val STATE_COMPLETED = "COMPLETED"
        const val STATE_FAILED = "FAILED"
        const val STATE_IN_PROGRESS = "IN_PROGRESS"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        // Per-device / per-plugin preference files survive across tests on a real device,
        // so wipe every preference file, not just the default one.
        deleteAllPreferenceFiles()
        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    @After
    fun tearDown() {
        deleteAllPreferenceFiles()
    }

    private fun deleteAllPreferenceFiles() {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        dir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".xml")) {
                val name = file.name.removeSuffix(".xml")
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }
        }
    }

    // ===== Test helpers ======================================================

    private fun putString(prefs: SharedPreferences, key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }

    private fun putBoolean(prefs: SharedPreferences, key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).commit()
    }

    private fun putInt(prefs: SharedPreferences, key: String, value: Int) {
        prefs.edit().putInt(key, value).commit()
    }

    private fun putLong(prefs: SharedPreferences, key: String, value: Long) {
        prefs.edit().putLong(key, value).commit()
    }

    private fun putFloat(prefs: SharedPreferences, key: String, value: Float) {
        prefs.edit().putFloat(key, value).commit()
    }

    private fun putStringSet(prefs: SharedPreferences, key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).commit()
    }

    private fun getString(prefs: SharedPreferences, key: String): String? {
        return prefs.getString(key, null)
    }

    private fun getStringSet(prefs: SharedPreferences, key: String): Set<String>? {
        return prefs.getStringSet(key, null)
    }

    /**
     * Boolean assertion that cannot pass vacuously: `false` is also what getBoolean returns for a
     * missing key, so presence is checked explicitly and the fallback is the opposite value.
     */
    private fun assertBooleanPref(prefs: SharedPreferences, key: String, expected: Boolean) {
        assertTrue("Expected '$key' to be present", prefs.contains(key))
        assertEquals(expected, prefs.getBoolean(key, !expected))
    }

    private fun devicePrefs(deviceId: String, plugin: String): SharedPreferences =
        context.getSharedPreferences("${deviceId}_${plugin}_preferences", Context.MODE_PRIVATE)

    private fun pluginPrefs(plugin: String): SharedPreferences =
        context.getSharedPreferences("${plugin}_preferences", Context.MODE_PRIVATE)

    private fun unresolvedKeys(): Set<String> =
        defaultPrefs.getStringSet(SettingsScopeMigration.UNRESOLVED_KEYS_KEY, emptySet()) ?: emptySet()

    private fun pluginState(plugin: String): String? =
        defaultPrefs.getString("migration_plugin_$plugin", null)

    private fun shareUrlKey(deviceId: String) = "key_unreachable_url_list_$deviceId"

    private fun commandsKey(deviceId: String) = "commands_preference_$deviceId"

    /** A context whose named preference files refuse to persist anything (commit() returns false). */
    private fun failingContext(vararg failingFileNames: String): Context =
        FailingWriteContext(context, failingFileNames.toSet())

    // ===== F-01: Initial migration completes while unresolved keys remain durably recorded =====

    @Test
    fun testInitialMigrationCompletesWithUnresolvedKeys() {
        // Given: A global key and device-specific keys for devices that are not trusted
        putString(defaultPrefs, "telephony_blocked_numbers", "1234567890,9876543210")
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_1), setOf("http://example.com"))
        putString(defaultPrefs, commandsKey(DEVICE_2), "cmd1;cmd2")

        // When: Migration runs with an empty trusted device set
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, emptySet())

        // Then: Migration version is committed despite unresolved keys
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))

        // And: Unresolved keys are durably recorded
        assertTrue(unresolvedKeys().contains(shareUrlKey(DEVICE_1)))
        assertTrue(unresolvedKeys().contains(commandsKey(DEVICE_2)))

        // And: Source keys are NOT removed (they remain in default prefs)
        assertEquals("1234567890,9876543210", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
        assertTrue(defaultPrefs.contains(shareUrlKey(DEVICE_1)))
        assertTrue(defaultPrefs.contains(commandsKey(DEVICE_2)))
    }

    // ===== F-02: Reconciliation after initial migration completion =====

    @Test
    fun testReconciliationAfterInitialMigration() {
        // Given: Initial migration with unresolved keys
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_1), setOf("http://example.com"))
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, emptySet())
        assertTrue(unresolvedKeys().contains(shareUrlKey(DEVICE_1)))

        // When: Device becomes trusted (re-paired) and reconciliation runs
        SettingsScopeMigration.markDevicePluginReady(context, "SharePlugin", DEVICE_1)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: The previously unresolved key is migrated to the device's preference file
        val devicePrefs = devicePrefs(DEVICE_1, "SharePlugin")
        assertEquals(setOf("http://example.com"), getStringSet(devicePrefs, shareUrlKey(DEVICE_1)))
        assertFalse(defaultPrefs.contains(shareUrlKey(DEVICE_1)))
        assertFalse(unresolvedKeys().contains(shareUrlKey(DEVICE_1)))
    }

    // ===== F-03: Two-device global preference fan-out =====

    @Test
    fun testTwoDeviceGlobalPreferenceFanOut() {
        // Given: Two trusted devices and a global preference
        putString(defaultPrefs, "telephony_blocked_numbers", "123,456")
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1, DEVICE_2))

        // Then: Both devices have the value in their PER_DEVICE preference file
        assertEquals("123,456", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
        assertEquals("123,456", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))
    }

    // ===== F-04: Fan-out interrupted after one successful destination =====

    @Test
    fun testFanOutInterruptedAfterOneDestination() {
        // Given: Two trusted devices and a global boolean preference
        putBoolean(defaultPrefs, "share_destination_custom", false)
        SettingsScopeMigration.initializeMigration(context)

        // Simulate a crash: device 1 received the value, device 2 did not, source not yet removed
        putBoolean(devicePrefs(DEVICE_1, "SharePlugin"), "share_destination_custom", false)
        assertFalse(devicePrefs(DEVICE_2, "SharePlugin").contains("share_destination_custom"))

        // When: Migration runs again after the restart
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1, DEVICE_2))

        // Then: The fan-out is completed for device 2 and device 1's value is preserved
        assertBooleanPref(devicePrefs(DEVICE_1, "SharePlugin"), "share_destination_custom", false)
        assertBooleanPref(devicePrefs(DEVICE_2, "SharePlugin"), "share_destination_custom", false)

        // And: The source is removed only now that every destination has the value
        assertFalse(defaultPrefs.contains("share_destination_custom"))
    }

    // ===== F-05: Fan-out destination failure =====

    @Test
    fun testFanOutDestinationFailure() {
        // Given: The destination preference file cannot be committed
        putString(defaultPrefs, "telephony_blocked_numbers", "123")
        SettingsScopeMigration.initializeMigration(context)
        val failing = failingContext("TelephonyPlugin_preferences")

        // When: Migration runs
        SettingsScopeMigration.runMigration(failing, setOf(DEVICE_1))

        // Then: The plugin is marked FAILED
        assertEquals(STATE_FAILED, pluginState("TelephonyPlugin"))

        // And: The source is kept and nothing reached the destination
        assertEquals("123", getString(defaultPrefs, "telephony_blocked_numbers"))
        assertFalse(pluginPrefs("TelephonyPlugin").contains("telephony_blocked_numbers"))

        // And: The migration version is not committed
        assertEquals(0, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
    }

    // ===== F-06: Destination type mismatch =====

    @Test
    fun testDestinationTypeMismatch() {
        // Given: Destination already has an incompatible type for the same key (Int instead of String)
        val devicePrefs = pluginPrefs("TelephonyPlugin")
        putInt(devicePrefs, "telephony_blocked_numbers", 12345)
        putString(defaultPrefs, "telephony_blocked_numbers", "555-0199")

        // When: Migration runs
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Destination incompatible value is replaced with the source value
        assertEquals("555-0199", getString(devicePrefs, "telephony_blocked_numbers"))
        // And: Source key is removed from default prefs
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))

        // And: The plugin is marked COMPLETED and the migration version is committed
        assertEquals(STATE_COMPLETED, pluginState("TelephonyPlugin"))
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
    }

    // ===== F-07: Supported types =====

    @Test
    fun testRealisticTypesAcrossPlugins() {
        // Given: String and boolean preferences belonging to different PER_DEVICE and PER_PLUGIN plugins
        putString(defaultPrefs, "telephony_blocked_numbers", "555-0199")
        putString(defaultPrefs, "pref_presenter_sensitivity", "75")
        putBoolean(defaultPrefs, "pref_presenter_enable_volume_keys", false)

        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: All values are preserved in the right scope-specific prefs
        assertEquals("555-0199", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))

        val presenterPrefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        assertEquals("75", getString(presenterPrefs, "pref_presenter_sensitivity"))
        assertBooleanPref(presenterPrefs, "pref_presenter_enable_volume_keys", false)

        // And: Sources are gone
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))
        assertFalse(defaultPrefs.contains("pref_presenter_sensitivity"))
        assertFalse(defaultPrefs.contains("pref_presenter_enable_volume_keys"))
    }

    /**
     * Exercises every SharedPreferences type plus falsy values. Keys are borrowed from known
     * plugins purely to route them; the migration is expected to copy by runtime type, so the
     * value types here intentionally differ from the real preference types.
     */
    @Test
    fun testAllSharedPreferencesTypesAndFalsyValues() {
        // Given: One value of every type, including empty / zero / false
        putString(defaultPrefs, "telephony_blocked_numbers", "")              // empty String
        putInt(defaultPrefs, "pref_presenter_sensitivity", 0)                 // zero Int
        putBoolean(defaultPrefs, "pref_presenter_enable_volume_keys", false)  // false Boolean
        putStringSet(defaultPrefs, "share_destination_custom", emptySet())    // empty StringSet
        putLong(defaultPrefs, "set_runcommand_name_as_title", 0L)             // zero Long
        putFloat(defaultPrefs, "mpris_time_key", 1.5f)                        // Float (PER_PLUGIN)
        putStringSet(defaultPrefs, "mpris_keepwatching_key", setOf("a", "b")) // non-empty StringSet (PER_PLUGIN)

        // When: Migration runs
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Every value arrives with its type and value intact
        val telephony = pluginPrefs("TelephonyPlugin")
        assertTrue(telephony.contains("telephony_blocked_numbers"))
        assertEquals("", telephony.getString("telephony_blocked_numbers", "sentinel"))

        val presenter = devicePrefs(DEVICE_1, "PresenterPlugin")
        assertTrue(presenter.contains("pref_presenter_sensitivity"))
        assertEquals(0, presenter.getInt("pref_presenter_sensitivity", -1))
        assertBooleanPref(presenter, "pref_presenter_enable_volume_keys", false)

        val share = devicePrefs(DEVICE_1, "SharePlugin")
        assertTrue(share.contains("share_destination_custom"))
        assertEquals(emptySet<String>(), share.getStringSet("share_destination_custom", setOf("sentinel")))

        val runCommand = devicePrefs(DEVICE_1, "RunCommandPlugin")
        assertTrue(runCommand.contains("set_runcommand_name_as_title"))
        assertEquals(0L, runCommand.getLong("set_runcommand_name_as_title", -1L))

        val mpris = pluginPrefs("MprisPlugin")
        assertTrue(mpris.contains("mpris_time_key"))
        assertEquals(1.5f, mpris.getFloat("mpris_time_key", -1f), 0f)
        assertEquals(setOf("a", "b"), getStringSet(mpris, "mpris_keepwatching_key"))

        // And: None of the sources remain
        listOf(
            "telephony_blocked_numbers",
            "pref_presenter_sensitivity",
            "pref_presenter_enable_volume_keys",
            "share_destination_custom",
            "set_runcommand_name_as_title",
            "mpris_time_key",
            "mpris_keepwatching_key"
        ).forEach { assertFalse("'$it' should have been removed", defaultPrefs.contains(it)) }
    }

    // ===== F-08: Re-pairing and per-device isolation =====

    @Test
    fun testRePairingMigratesOnlyThatDevicesKeys() {
        // Given: Device-specific keys for device 1 (trusted) and device 2 (not yet trusted)
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_1), setOf("http://one.example"))
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_2), setOf("http://two.example"))
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Device 1 is migrated; device 2's key is still parked in default prefs
        assertEquals(setOf("http://one.example"), getStringSet(devicePrefs(DEVICE_1, "SharePlugin"), shareUrlKey(DEVICE_1)))
        assertTrue(defaultPrefs.contains(shareUrlKey(DEVICE_2)))
        assertFalse(devicePrefs(DEVICE_2, "SharePlugin").contains(shareUrlKey(DEVICE_2)))

        // When: Device 2 is re-paired (becomes trusted)
        SettingsScopeMigration.markDevicePluginReady(context, "SharePlugin", DEVICE_2)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1, DEVICE_2))

        // Then: Device 2 gets its own value and nothing leaks between devices
        assertEquals(setOf("http://two.example"), getStringSet(devicePrefs(DEVICE_2, "SharePlugin"), shareUrlKey(DEVICE_2)))
        assertEquals(setOf("http://one.example"), getStringSet(devicePrefs(DEVICE_1, "SharePlugin"), shareUrlKey(DEVICE_1)))
        assertFalse(devicePrefs(DEVICE_1, "SharePlugin").contains(shareUrlKey(DEVICE_2)))
        assertFalse(devicePrefs(DEVICE_2, "SharePlugin").contains(shareUrlKey(DEVICE_1)))
        assertFalse(defaultPrefs.contains(shareUrlKey(DEVICE_2)))
        assertTrue(unresolvedKeys().isEmpty())
    }

    // ===== F-09: Plugin initialization retry after a failed attempt =====

    @Test
    fun testPluginInitRetryAfterFailedMigration() {
        // Given: The first attempt cannot write RemoteKeyboardIMEPlugin's (PER_PLUGIN) preferences
        putBoolean(defaultPrefs, "remotekeyboard_editing_only", false)
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(
            failingContext("RemoteKeyboardIMEPlugin_preferences"),
            setOf(DEVICE_1)
        )
        assertEquals(STATE_FAILED, pluginState("RemoteKeyboardIMEPlugin"))
        assertFalse(SettingsScopeMigration.isMigrationReady(context, "RemoteKeyboardIMEPlugin", DEVICE_1))

        // When: The migration is retried and storage works
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: The plugin is ready and the value arrived
        assertEquals(STATE_COMPLETED, pluginState("RemoteKeyboardIMEPlugin"))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "RemoteKeyboardIMEPlugin", DEVICE_1))
        assertBooleanPref(pluginPrefs("RemoteKeyboardIMEPlugin"), "remotekeyboard_editing_only", false)
        assertFalse(defaultPrefs.contains("remotekeyboard_editing_only"))
    }

    // ===== F-10: Failed plugin instance is not partially registered =====

    @Test
    fun testFailedPluginInstanceNotPartiallyRegistered() {
        // Given: One plugin whose migration fails and one that succeeds
        putBoolean(defaultPrefs, "remotekeyboard_editing_only", true)
        putString(defaultPrefs, "telephony_blocked_numbers", "555-0199")
        SettingsScopeMigration.initializeMigration(context)

        // When: Migration runs and RemoteKeyboardIMEPlugin's storage refuses writes
        SettingsScopeMigration.runMigration(
            failingContext("RemoteKeyboardIMEPlugin_preferences"),
            setOf(DEVICE_1)
        )

        // Then: The failed plugin is FAILED (not COMPLETED), not ready, and holds no partial data
        assertEquals(STATE_FAILED, pluginState("RemoteKeyboardIMEPlugin"))
        assertFalse(SettingsScopeMigration.isMigrationReady(context, "RemoteKeyboardIMEPlugin", DEVICE_1))
        assertFalse(pluginPrefs("RemoteKeyboardIMEPlugin").contains("remotekeyboard_editing_only"))
        assertTrue(defaultPrefs.contains("remotekeyboard_editing_only"))

        // And: The unrelated plugin is unaffected
        assertEquals(STATE_COMPLETED, pluginState("TelephonyPlugin"))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "TelephonyPlugin", DEVICE_1))
        assertEquals("555-0199", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
    }

    // ===== F-11: Exact key-prefix collision =====

    @Test
    fun testExactKeyPrefixCollision() {
        // Given: A key with a malformed device id, and a key whose id merely STARTS WITH a trusted id
        val malformedKey = shareUrlKey("notadeviceid")
        val longerIdKey = shareUrlKey(DEVICE_1 + "0")
        putStringSet(defaultPrefs, malformedKey, setOf("http://evil.com"))
        putStringSet(defaultPrefs, longerIdKey, setOf("http://other.example"))

        // When: Migration runs with DEVICE_1 trusted
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Both keys are preserved as unresolved, not treated as global or as DEVICE_1's
        assertTrue(unresolvedKeys().contains(malformedKey))
        assertTrue(unresolvedKeys().contains(longerIdKey))
        assertTrue(defaultPrefs.contains(malformedKey))
        assertTrue(defaultPrefs.contains(longerIdKey))

        // And: Nothing leaked into DEVICE_1's preference file
        val devicePrefs = devicePrefs(DEVICE_1, "SharePlugin")
        assertFalse(devicePrefs.contains(malformedKey))
        assertFalse(devicePrefs.contains(longerIdKey))
    }

    // ===== F-12: Mpris global preference preservation =====

    @Test
    fun testMprisGlobalPreferencePreservation() {
        // Given: MprisPlugin is PER_PLUGIN, not PER_DEVICE
        putString(defaultPrefs, "mpris_time_key", "5000")
        putBoolean(defaultPrefs, "mpris_keepwatching_key", false)

        // When: Migration runs
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Values are in MprisPlugin_preferences with their types intact
        val mprisPrefs = pluginPrefs("MprisPlugin")
        assertEquals("5000", getString(mprisPrefs, "mpris_time_key"))
        assertBooleanPref(mprisPrefs, "mpris_keepwatching_key", false)

        // And: They were NOT fanned out per device
        val perDevice = devicePrefs(DEVICE_1, "MprisPlugin")
        assertFalse(perDevice.contains("mpris_time_key"))
        assertFalse(perDevice.contains("mpris_keepwatching_key"))

        // And: Sources are removed from default prefs
        assertFalse(defaultPrefs.contains("mpris_time_key"))
        assertFalse(defaultPrefs.contains("mpris_keepwatching_key"))
    }

    // ===== F-13: Full restart/recovery after each durable operation =====

    @Test
    fun testRestartWithInProgressStateBeforeAnyWrite() {
        // Given: A previous run recorded IN_PROGRESS and then died before writing anything
        putString(defaultPrefs, "telephony_blocked_numbers", "555-0199")
        defaultPrefs.edit().putString("migration_plugin_TelephonyPlugin", STATE_IN_PROGRESS).commit()

        // When: Migration runs again
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Migration completes successfully
        assertEquals(STATE_COMPLETED, pluginState("TelephonyPlugin"))
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))
        assertEquals("555-0199", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
    }

    @Test
    fun testRestartAfterDestinationWriteBeforeSourceRemoval() {
        // Given: A previous run wrote the destination but died before removing the source
        putString(defaultPrefs, "telephony_blocked_numbers", "555-0199")
        putString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers", "555-0199")
        defaultPrefs.edit().putString("migration_plugin_TelephonyPlugin", STATE_IN_PROGRESS).commit()

        // When: Migration runs again
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: The destination is unchanged, the source is finally removed, and the plugin completes
        assertEquals("555-0199", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))
        assertEquals(STATE_COMPLETED, pluginState("TelephonyPlugin"))
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
    }

    // ===== F-14: Repeated reconciliation =====

    @Test
    fun testRepeatedReconciliation() {
        // Given: Unresolved key for an untrusted device
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_1), setOf("http://example.com"))
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, emptySet())

        // When: Device becomes trusted and reconciliation runs
        SettingsScopeMigration.markDevicePluginReady(context, "SharePlugin", DEVICE_1)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))
        val devicePrefs = devicePrefs(DEVICE_1, "SharePlugin")
        val afterFirst = HashMap(devicePrefs.all)

        // And: It runs a second time
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: The key was migrated exactly once and the second run changed nothing
        assertEquals(setOf("http://example.com"), getStringSet(devicePrefs, shareUrlKey(DEVICE_1)))
        assertEquals(afterFirst, HashMap(devicePrefs.all))
        assertFalse(defaultPrefs.contains(shareUrlKey(DEVICE_1)))
        assertFalse(unresolvedKeys().contains(shareUrlKey(DEVICE_1)))
    }

    // ===== F-15: Mixed trusted/untrusted keys =====

    @Test
    fun testInitialMigrationVersionCommittedWithMixedTrust() {
        // Given: Both trusted and untrusted device-specific keys
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_1), setOf("http://trusted.com"))
        putStringSet(defaultPrefs, shareUrlKey(DEVICE_2), setOf("http://untrusted.com"))

        // When: Migration runs with only device 1 trusted
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Version is committed despite the unresolved key for device 2
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
        assertTrue(unresolvedKeys().contains(shareUrlKey(DEVICE_2)))
        assertFalse(unresolvedKeys().contains(shareUrlKey(DEVICE_1)))

        // And: Device 1's key is migrated and removed from default prefs
        assertEquals(setOf("http://trusted.com"), getStringSet(devicePrefs(DEVICE_1, "SharePlugin"), shareUrlKey(DEVICE_1)))
        assertFalse(defaultPrefs.contains(shareUrlKey(DEVICE_1)))

        // And: Device 2's key is untouched and not written anywhere
        assertTrue(defaultPrefs.contains(shareUrlKey(DEVICE_2)))
        assertFalse(devicePrefs(DEVICE_2, "SharePlugin").contains(shareUrlKey(DEVICE_2)))
    }

    @Test
    fun testZeroTrustedDevicesPreservesStaticPerDeviceKeysUntilFirstPair() {
        // Given: Static PER_DEVICE preference present before any device is paired
        putBoolean(defaultPrefs, "pref_presenter_enable_volume_keys", true)
        putInt(defaultPrefs, "pref_presenter_sensitivity", 75)
        SettingsScopeMigration.initializeMigration(context)

        // When: Migration runs with zero trusted devices
        SettingsScopeMigration.runMigration(context, emptySet())

        // Then: Source keys are NOT deleted because there was no destination
        assertTrue(defaultPrefs.contains("pref_presenter_enable_volume_keys"))
        assertTrue(defaultPrefs.contains("pref_presenter_sensitivity"))

        // When: First device is paired later and migration runs
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: First device receives the migrated settings
        val device1Prefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        assertTrue(device1Prefs.getBoolean("pref_presenter_enable_volume_keys", false))
        assertEquals(75, device1Prefs.getInt("pref_presenter_sensitivity", 0))

        // And: Source keys are now removed
        assertFalse(defaultPrefs.contains("pref_presenter_enable_volume_keys"))
        assertFalse(defaultPrefs.contains("pref_presenter_sensitivity"))

        // When: A second device is paired subsequently
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1, DEVICE_2))

        // Then: Second device does not receive the original setting (documents the subsequent-pair fan-out gap)
        val device2Prefs = devicePrefs(DEVICE_2, "PresenterPlugin")
        assertFalse(device2Prefs.contains("pref_presenter_enable_volume_keys"))
        assertFalse(device2Prefs.contains("pref_presenter_sensitivity"))
    }

    @Test
    fun testFailedKeyPreservesSourceAndRetriesOnSubsequentRun() {
        // Given: Two preferences for PresenterPlugin: sensitivity (will fail on first run) and volume keys (will succeed)
        // And: A preference for another plugin (TelephonyPlugin) that succeeds
        putString(defaultPrefs, "telephony_blocked_numbers", "123-456")
        putBoolean(defaultPrefs, "pref_presenter_enable_volume_keys", true)
        putInt(defaultPrefs, "pref_presenter_sensitivity", 50)
        SettingsScopeMigration.initializeMigration(context)

        // When: Migration runs with PresenterPlugin failing to persist pref_presenter_sensitivity
        val failingContext = KeyFailingContext(context, setOf("pref_presenter_sensitivity"))
        SettingsScopeMigration.runMigration(failingContext, setOf(DEVICE_1))

        // Then: The failed key keeps its source value in default prefs
        assertTrue("Failed key must remain in default prefs", defaultPrefs.contains("pref_presenter_sensitivity"))
        assertEquals(50, defaultPrefs.getInt("pref_presenter_sensitivity", -1))

        // And: The failed key is not marked complete (migration version not committed, plugin failed, not ready)
        assertEquals(0, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
        assertEquals(STATE_FAILED, pluginState("PresenterPlugin"))
        assertFalse(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_1))

        // And: Keys that already succeeded stay migrated and removed from source
        val presenterDevicePrefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        assertBooleanPref(presenterDevicePrefs, "pref_presenter_enable_volume_keys", true)
        assertFalse("Succeeded key must be removed from default prefs", defaultPrefs.contains("pref_presenter_enable_volume_keys"))
        assertEquals("123-456", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))
        assertFalse(defaultPrefs.contains("telephony_blocked_numbers"))

        // When: Migration runs again on the next run with storage working
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: The previously failed key now succeeds and arrives at destination
        assertEquals(50, presenterDevicePrefs.getInt("pref_presenter_sensitivity", -1))
        assertFalse("Source key must now be removed from default prefs", defaultPrefs.contains("pref_presenter_sensitivity"))

        // And: Keys that already succeeded stay migrated with their values intact
        assertBooleanPref(presenterDevicePrefs, "pref_presenter_enable_volume_keys", true)
        assertEquals("123-456", getString(pluginPrefs("TelephonyPlugin"), "telephony_blocked_numbers"))

        // And: Migration is now marked complete and plugin is ready
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
        assertEquals(STATE_COMPLETED, pluginState("PresenterPlugin"))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_1))
    }

    @Test
    fun testExistingDestinationValueWinsAndSourceRemoved() {
        // Given: Source preference has sensitivity = 50
        putInt(defaultPrefs, "pref_presenter_sensitivity", 50)
        // And: Destination already holds a DIFFERENT value (e.g. user modified it on device)
        val presenterDevicePrefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        putInt(presenterDevicePrefs, "pref_presenter_sensitivity", 80)

        // When: Migration runs
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Existing destination value wins
        assertEquals(80, presenterDevicePrefs.getInt("pref_presenter_sensitivity", -1))
        // And: Source key is removed now that destination holds a value
        assertFalse("Source key should be removed once destination holds a value", defaultPrefs.contains("pref_presenter_sensitivity"))
        // And: Plugin is completed and ready
        assertEquals(STATE_COMPLETED, pluginState("PresenterPlugin"))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_1))
    }

    @Test
    fun testTwoDevicesDevice1KeepsExistingValueDevice2ReceivesSourceValue() {
        // Given: Source preference has sensitivity = 50
        putInt(defaultPrefs, "pref_presenter_sensitivity", 50)
        // And: Device 1 already has its own value (80), while Device 2 has no value
        val device1Prefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        putInt(device1Prefs, "pref_presenter_sensitivity", 80)
        val device2Prefs = devicePrefs(DEVICE_2, "PresenterPlugin")

        // When: Migration runs for both devices
        SettingsScopeMigration.initializeMigration(context)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1, DEVICE_2))

        // Then: Device 1 keeps its own value (80)
        assertEquals(80, device1Prefs.getInt("pref_presenter_sensitivity", -1))
        // And: Device 2 receives the source value (50)
        assertEquals(50, device2Prefs.getInt("pref_presenter_sensitivity", -1))
        // And: Source key is removed from default prefs
        assertFalse(defaultPrefs.contains("pref_presenter_sensitivity"))
        // And: Plugin completes and both devices are ready
        assertEquals(STATE_COMPLETED, pluginState("PresenterPlugin"))
        assertEquals(1, defaultPrefs.getInt(SettingsScopeMigration.MIGRATION_PREFS_KEY, 0))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_1))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_2))
    }

    @Test
    fun testReachableRePairOrRestorePreservesExistingDeviceValue() {
        // Given: Device 1 already exists and was configured with sensitivity = 80
        val device1Prefs = devicePrefs(DEVICE_1, "PresenterPlugin")
        putInt(device1Prefs, "pref_presenter_sensitivity", 80)
        // And: defaultPrefs has an older legacy setting restored from backup/re-pair
        putInt(defaultPrefs, "pref_presenter_sensitivity", 50)

        // When: Re-pairing or app startup triggers migration
        SettingsScopeMigration.markDevicePluginReady(context, "PresenterPlugin", DEVICE_1)
        SettingsScopeMigration.runMigration(context, setOf(DEVICE_1))

        // Then: Device 1 keeps its user-configured value (80)
        assertEquals(80, device1Prefs.getInt("pref_presenter_sensitivity", -1))
        // And: Stale source preference is deleted from default prefs
        assertFalse(defaultPrefs.contains("pref_presenter_sensitivity"))
        // And: Plugin is COMPLETED and ready
        assertEquals(STATE_COMPLETED, pluginState("PresenterPlugin"))
        assertTrue(SettingsScopeMigration.isMigrationReady(context, "PresenterPlugin", DEVICE_1))
    }
}

// ===== Failure-injection support =============================================

/**
 * Wraps a context so that the named SharedPreferences files silently refuse to persist anything.
 * Everything else (including the default preferences used for migration state) is passed through.
 */
private class FailingWriteContext(
    base: Context,
    private val failingFileNames: Set<String>
) : ContextWrapper(base) {

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val real = super.getSharedPreferences(name, mode)
        return if (name != null && name in failingFileNames) CommitFailingPreferences(real) else real
    }

    // Prevent code under test from escaping the wrapper via applicationContext.
    override fun getApplicationContext(): Context = this
}

/** Reads go to the real file; every write returns false from commit() and persists nothing. */
private class CommitFailingPreferences(
    delegate: SharedPreferences
) : SharedPreferences by delegate {
    override fun edit(): SharedPreferences.Editor = DiscardingEditor()
}

private class DiscardingEditor : SharedPreferences.Editor {
    override fun putString(key: String?, value: String?): SharedPreferences.Editor = this
    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
    override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
    override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
    override fun remove(key: String?): SharedPreferences.Editor = this
    override fun clear(): SharedPreferences.Editor = this
    override fun commit(): Boolean = false
    override fun apply() {}
}

private class KeyFailingContext(
    base: Context,
    private val failingKeys: Set<String>
) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val real = super.getSharedPreferences(name, mode)
        return KeyFailingPreferences(real, failingKeys)
    }
    override fun getApplicationContext(): Context = this
}

private class KeyFailingPreferences(
    private val delegate: SharedPreferences,
    private val failingKeys: Set<String>
) : SharedPreferences by delegate {
    override fun edit(): SharedPreferences.Editor = KeyFailingEditor(delegate.edit(), failingKeys)
}

private class KeyFailingEditor(
    private val delegate: SharedPreferences.Editor,
    private val failingKeys: Set<String>
) : SharedPreferences.Editor by delegate {
    private var failed = false

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putString(key, value)
        return this
    }
    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putInt(key, value)
        return this
    }
    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putBoolean(key, value)
        return this
    }
    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putLong(key, value)
        return this
    }
    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putFloat(key, value)
        return this
    }
    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null && key in failingKeys) failed = true else delegate.putStringSet(key, values)
        return this
    }
    override fun commit(): Boolean {
        if (failed) return false
        return delegate.commit()
    }
}