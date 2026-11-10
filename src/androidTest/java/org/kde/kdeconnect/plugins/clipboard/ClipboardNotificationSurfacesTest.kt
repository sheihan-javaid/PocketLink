/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.clipboard

import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.core.view.drawToBitmap
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.plugins.notifications.NotificationsPlugin
import org.kde.kdeconnect.ui.StartActivityAlertDialogFragment
import org.kde.kdeconnect_tp.R
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ClipboardNotificationSurfacesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clipboardPreferencePersistsSensitiveSyncSetting() {
        val context = composeRule.activity
        val preferencesName = "stage5_clipboard_surface_test"
        val key = context.getString(R.string.clipboard_preference_key_skip_sensitive)
        val preferences = context.getSharedPreferences(preferencesName, 0)
        preferences.edit().clear().commit()

        try {
            val preferenceManager = PreferenceManager(context).apply {
                setSharedPreferencesName(preferencesName)
            }
            val screen = preferenceManager.inflateFromResource(
                context,
                R.xml.clipboardplugin_preferences,
                null
            )
            val setting = screen.findPreference<SwitchPreference>(key)

            assertNotNull(setting)
            assertFalse(setting!!.isChecked)
            setting.performClick()
            assertTrue(setting.isChecked)
            assertTrue(preferences.getBoolean(key, false))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun notificationPreferencePersistsExistingScreenOffSetting() {
        val context = composeRule.activity
        val preferencesName = "stage5_notification_surface_test"
        val key = context.getString(R.string.pref_notification_screen_off_key)
        val preferences = context.getSharedPreferences(preferencesName, 0)
        preferences.edit().clear().commit()

        try {
            val preferenceManager = PreferenceManager(context).apply {
                setSharedPreferencesName(preferencesName)
            }
            val screen = preferenceManager.inflateFromResource(
                context,
                R.xml.notificationsplugin_preferences,
                null
            )
            val setting = screen.findPreference<SwitchPreference>(key)

            assertNotNull(setting)
            assertFalse(setting!!.isChecked)
            setting.performClick()
            assertTrue(setting.isChecked)
            assertTrue(preferences.getBoolean(key, false))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun notificationFilterRowMeetsTouchTargetAndListSelectionIsCheckable() {
        var selectionCallbackCount = 0
        var selected = false
        showNotificationFilterFixture {
            selectionCallbackCount += 1
            selected = !selected
        }

        onView(withText("Messages")).check(matches(isDisplayed())).perform(click())
        val appRow = composeRule.activity.findViewById<CheckedTextView>(R.id.notification_filter_app_row)
        assertNotNull(appRow.checkMarkDrawable)
        assertTrue(
            appRow.minimumHeight >= composeRule.activity.resources.getDimensionPixelSize(R.dimen.kde_min_touch_target)
        )

        composeRule.runOnIdle {
            assertEquals(1, selectionCallbackCount)
            assertTrue(selected)
            assertTrue(appRow.isChecked)
        }

        val isDark = (composeRule.activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        saveScreenshot(
            if (isDark) "stage5-clipboard-notifications-dark.png"
            else "stage5-clipboard-notifications-light.png"
        )
    }

    @Test
    fun clipboardFocusActivityRemainsInvisibleAndHiddenFromAccessibility() {
        val root = LayoutInflater.from(composeRule.activity)
            .inflate(R.layout.activity_clipboard_floating, null, false)
        val trigger = root.findViewById<View>(R.id.floating_button)

        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, root.importantForAccessibility)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, trigger.importantForAccessibility)
        assertEquals(0f, trigger.alpha)
    }

    @Test
    fun notificationPermissionGuidanceStillUsesExistingSystemSettings() {
        val dialog = NotificationsPlugin().permissionExplanationDialog
        assertTrue(dialog is StartActivityAlertDialogFragment)
        assertEquals(
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
            dialog.arguments?.getString("IntentAction")
        )

        assertTrue(
            composeRule.activity.getString(R.string.receive_notifications_permission_explanation).isNotBlank()
        )
    }

    @Test
    fun notificationPrivacyOptionsHaveReadableTouchTargets() {
        val themedContext = ContextThemeWrapper(composeRule.activity, R.style.PocketLinkTheme)
        val options = LayoutInflater.from(themedContext)
            .inflate(R.layout.privacy_options, null, false)
        val minimumTarget = composeRule.activity.resources
            .getDimensionPixelSize(R.dimen.kde_min_touch_target)

        assertTrue(options.findViewById<View>(R.id.checkbox_contents).minimumHeight >= minimumTarget)
        assertTrue(options.findViewById<View>(R.id.checkbox_images).minimumHeight >= minimumTarget)
    }

    private fun showNotificationFilterFixture(onSelectionChanged: () -> Unit) {
        composeRule.activity.runOnUiThread {
            val activity = composeRule.activity
            val themedContext = ContextThemeWrapper(activity, R.style.PocketLinkTheme)
            val inflater = LayoutInflater.from(themedContext)
            val root = inflater.inflate(R.layout.fragment_notification_filter, null, false)
            activity.setContentView(root)

            root.findViewById<View>(R.id.spinner).visibility = View.GONE
            val list = root.findViewById<ListView>(R.id.lvFilterApps)
            list.visibility = View.VISIBLE
            list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
            list.adapter = object : BaseAdapter() {
                override fun getCount(): Int = 1

                override fun getItem(position: Int): Any = "Messages"

                override fun getItemId(position: Int): Long = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val row = convertView as? CheckedTextView
                        ?: inflater.inflate(R.layout.notification_filter_app_row, parent, false) as CheckedTextView
                    row.text = getItem(position) as String
                    row.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        android.R.drawable.sym_def_app_icon,
                        0,
                        0,
                        0
                    )
                    return row
                }
            }
            list.setOnItemClickListener { _, _, _, _ -> onSelectionChanged() }
        }
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.waitForIdle()
        var bitmap: Bitmap? = null
        composeRule.activity.runOnUiThread {
            bitmap = composeRule.activity.window.decorView.drawToBitmap()
        }
        val outputFile = File(
            requireNotNull(composeRule.activity.getExternalFilesDir(null)),
            fileName
        )
        FileOutputStream(outputFile).use {
            requireNotNull(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
