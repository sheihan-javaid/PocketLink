/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.runcommand

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class RunCommandUrlActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun nullDataFinishesActivity() {
        val intent = Intent(context, RunCommandUrlActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        ActivityScenario.launch<RunCommandUrlActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish when intent data is null",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun insufficientUriSegmentsFinishesActivity() {
        val intent = Intent(context, RunCommandUrlActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("kdeconnect://runcommand/onlyonepart")
        }
        ActivityScenario.launch<RunCommandUrlActivity>(intent).use { scenario ->
            assertEquals(
                "Activity must finish when URI path segments are insufficient",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun confirmationStringsAreDefined() {
        val title = context.getString(R.string.runcommand_confirm_title)
        val description = context.getString(R.string.runcommand_confirm_description, "Lock", "MacBook Pro")
        val confirmBtn = context.getString(R.string.runcommand_confirm_button)

        assertNotNull(title)
        assertNotNull(description)
        assertNotNull(confirmBtn)
    }
}
