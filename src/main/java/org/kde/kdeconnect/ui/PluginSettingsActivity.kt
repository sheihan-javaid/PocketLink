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
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityPluginSettingsBinding

class PluginSettingsActivity : BaseActivity<ActivityPluginSettingsBinding>() {

    override val binding by lazy { ActivityPluginSettingsBinding.inflate(layoutInflater) }

    private lateinit var settingsDeviceId: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
        if (deviceId == null) {
            Log.w(TAG, "Missing $EXTRA_DEVICE_ID extra, finishing activity")
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        settingsDeviceId = deviceId

        // Don't restore a specific plugin settings fragment if the device is no longer connected
        val deviceReachable = PocketLink.getInstance().getDevice(settingsDeviceId)?.isReachable == true
        enableEdgeToEdge()
        super.onCreate(if (deviceReachable) savedInstanceState else null)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val device = PocketLink.getInstance().getDevice(settingsDeviceId)
        device?.name?.let { binding.toolbarLayout.toolbar.subtitle = it }

        var fragment = supportFragmentManager.findFragmentById(R.id.fragmentPlaceHolder)
        if (fragment == null) {
            val pluginKey = intent.getStringExtra(EXTRA_PLUGIN_KEY)
            val plugin = if (pluginKey != null) {
                device?.getPluginIncludingWithoutPermissions(pluginKey)
            } else {
                null
            }

            fragment = plugin?.getSettingsFragment(this)

            if (fragment != null) {
                plugin?.displayName?.let { title ->
                    setTitle(getString(R.string.plugin_settings_with_name, title))
                }
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentPlaceHolder, fragment)
                    .commit()
            } else {
                Log.w(TAG, "No settings fragment available for pluginKey=$pluginKey on device=$settingsDeviceId; finishing")
                finish()
            }
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

        fun createIntent(context: Context, deviceId: String, pluginKey: String? = null): Intent =
            Intent(context, PluginSettingsActivity::class.java).apply {
                putExtra(EXTRA_DEVICE_ID, deviceId)
                pluginKey?.let { putExtra(EXTRA_PLUGIN_KEY, it) }
            }
    }
}
