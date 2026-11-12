/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect.base.BaseActivity
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityPluginSettingsBinding

class PluginSettingsActivity : BaseActivity<ActivityPluginSettingsBinding>() {

    override val binding by lazy { ActivityPluginSettingsBinding.inflate(layoutInflater) }

    private lateinit var settingsDeviceId: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
        if (deviceId == null) {
            Log.w(TAG, "Missing $EXTRA_DEVICE_ID extra; finishing activity")
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        settingsDeviceId = deviceId

        val device = PocketLink.getInstance().getDevice(settingsDeviceId)
        val deviceReachable = device?.isReachable == true
        enableEdgeToEdge()
        super.onCreate(if (deviceReachable) savedInstanceState else null)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        device?.name?.let { binding.toolbarLayout.toolbar.subtitle = it }

        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentPlaceHolder)
        if (fragment == null) {
            val pluginKey = intent.getStringExtra(EXTRA_PLUGIN_KEY)
            if (pluginKey.isNullOrBlank()) {
                Log.w(TAG, "Missing $EXTRA_PLUGIN_KEY extra for device=$settingsDeviceId; finishing")
                finish()
                return
            }

            val plugin = device?.getPluginIncludingWithoutPermissions(pluginKey)
            if (plugin == null) {
                Log.w(TAG, "Plugin $pluginKey not found for device=$settingsDeviceId; finishing")
                finish()
                return
            }

            if (!plugin.hasSettings) {
                Log.w(TAG, "Plugin $pluginKey has no settings for device=$settingsDeviceId; finishing")
                finish()
                return
            }

            val settingsFragment = try {
                plugin.getSettingsFragment(this)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to instantiate settings fragment for $pluginKey on device=$settingsDeviceId; finishing", e)
                finish()
                return
            }

            setTitle(getString(R.string.plugin_settings_with_name, plugin.displayName))
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentPlaceHolder, settingsFragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val fm = supportFragmentManager

            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "PluginSettingsActivity"
        const val EXTRA_DEVICE_ID: String = "deviceId"
        const val EXTRA_PLUGIN_KEY: String = "pluginKey"

        fun createIntent(context: Context, deviceId: String, pluginKey: String): Intent =
            Intent(context, PluginSettingsActivity::class.java).apply {
                putExtra(EXTRA_DEVICE_ID, deviceId)
                putExtra(EXTRA_PLUGIN_KEY, pluginKey)
            }
    }
}
