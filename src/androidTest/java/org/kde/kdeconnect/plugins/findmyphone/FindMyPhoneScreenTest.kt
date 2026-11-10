/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.findmyphone

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class FindMyPhoneScreenTest {

    @Test
    fun alertScreenShowsDeviceIdentityAndAccessibleFoundAction() {
        val scenario = launchScreen()

        try {
            onView(withId(R.id.tvFindMyPhoneTitle)).check(matches(isDisplayed()))
            onView(withText(R.string.findmyphone_description)).check(matches(isDisplayed()))
            onView(withId(R.id.bFindMyPhone)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val button = activity.findViewById<View>(R.id.bFindMyPhone)
                val minimumTarget = activity.resources.getDimensionPixelSize(R.dimen.pocketlink_min_touch_target)
                assertTrue(button.minimumHeight >= minimumTarget)
            }
            onView(withId(R.id.bFindMyPhone)).perform(click())
            scenario.onActivity { activity -> assertTrue(activity.isFinishing) }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun lightAndDarkAlertScreensRenderAndCapture() {
        val originalNightMode = AppCompatDelegate.getDefaultNightMode()

        try {
            listOf(
                AppCompatDelegate.MODE_NIGHT_NO to "stage5-find-my-phone-light.png",
                AppCompatDelegate.MODE_NIGHT_YES to "stage5-find-my-phone-dark.png"
            ).forEach { (nightMode, fileName) ->
                AppCompatDelegate.setDefaultNightMode(nightMode)
                val scenario = launchScreen()
                try {
                    onView(withId(R.id.tvFindMyPhoneTitle)).check(matches(isDisplayed()))
                    onView(withId(R.id.bFindMyPhone)).check(matches(isDisplayed()))
                    scenario.onActivity { activity -> saveScreenshot(activity, fileName) }
                } finally {
                    scenario.close()
                }
            }
        } finally {
            AppCompatDelegate.setDefaultNightMode(originalNightMode)
        }
    }

    private fun launchScreen(): ActivityScenario<FindMyPhoneActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, FindMyPhoneActivity::class.java)
            .putExtra(FindMyPhoneActivity.EXTRA_DEVICE_ID, "presentation-only-find-my-phone-test")
        return ActivityScenario.launch(intent)
    }

    private fun saveScreenshot(activity: FindMyPhoneActivity, fileName: String) {
        val bitmap: Bitmap = activity.window.decorView.drawToBitmap()
        val outputFile = File(requireNotNull(activity.getExternalFilesDir(null)), fileName)
        FileOutputStream(outputFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}
