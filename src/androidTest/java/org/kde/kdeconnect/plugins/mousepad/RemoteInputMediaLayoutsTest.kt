/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.mousepad

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class RemoteInputMediaLayoutsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mousepadRetainsAccessibleButtonsAndMprisRetainsSupportedControls() {
        lateinit var mousepad: android.view.View
        lateinit var mpris: android.view.View
        lateinit var sink: android.view.View
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setTheme(R.style.PocketLinkTheme)
            mousepad = activity.layoutInflater.inflate(R.layout.activity_mousepad, null)
            mpris = activity.layoutInflater.inflate(R.layout.mpris_now_playing, null)
            sink = activity.layoutInflater.inflate(R.layout.list_item_systemvolume, null)
        }

        composeRule.runOnIdle {
            assertEquals(
                composeRule.activity.getString(R.string.left_click),
                mousepad.findViewById<android.view.View>(R.id.mouse_click_left)
                    .contentDescription
            )
            assertEquals(
                composeRule.activity.getString(R.string.middle_click),
                mousepad.findViewById<android.view.View>(R.id.mouse_click_middle)
                    .contentDescription
            )
            assertEquals(
                composeRule.activity.getString(R.string.right_click),
                mousepad.findViewById<android.view.View>(R.id.mouse_click_right)
                    .contentDescription
            )
            assertNotNull(mpris.findViewById<android.view.View>(R.id.play_button))
            assertNotNull(mpris.findViewById<android.view.View>(R.id.positionSeek))
            assertNotNull(mpris.findViewById<android.view.View>(R.id.volume_seek))
            assertNotNull(sink.findViewById<android.view.View>(R.id.systemvolume_mute))
            assertNotNull(sink.findViewById<android.view.View>(R.id.systemvolume_seek))
        }
    }
}
