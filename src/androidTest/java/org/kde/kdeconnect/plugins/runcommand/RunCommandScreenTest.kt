/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.runcommand

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class RunCommandScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyStateShowsExplanatoryPanelAndAddCommandAction() {
        var addCommandClicked = false

        composeRule.setContent {
            RunCommandScreenContent(
                subTitle = "Linux Desktop",
                isReachable = true,
                canAddCommand = true,
                showOutputCard = false,
                commandList = emptyList(),
                outputList = emptyList(),
                commandRunning = false,
                onBackPressed = {},
                onRunCommand = {},
                onCopyUrl = {},
                onClearOutput = {},
                onStopCommand = {},
                onAddCommand = { addCommandClicked = true }
            )
        }

        composeRule.onNodeWithText("Linux Desktop").assertIsDisplayed()
        val addCommandText = composeRule.activity.getString(R.string.add_command)

        composeRule.onNodeWithText(addCommandText).assertIsDisplayed().performClick()
        assertTrue(addCommandClicked)
    }

    @Test
    fun populatedCommandsAreDisplayedAndClickable() {
        var ranCommandKey: String? = null
        var copiedCommand: CommandEntry? = null

        val commands = listOf(
            CommandEntry(CommandInfo("Lock Screen", "loginctl lock-session", "lock-screen")),
            CommandEntry(CommandInfo("Suspend PC", "systemctl suspend", "suspend"))
        )

        composeRule.setContent {
            RunCommandScreenContent(
                subTitle = "Linux Desktop",
                isReachable = true,
                canAddCommand = false,
                showOutputCard = false,
                commandList = commands,
                outputList = emptyList(),
                commandRunning = false,
                onBackPressed = {},
                onRunCommand = { ranCommandKey = it },
                onCopyUrl = { copiedCommand = it },
                onClearOutput = {},
                onStopCommand = {},
                onAddCommand = {}
            )
        }

        composeRule.onNodeWithText("Lock Screen").assertIsDisplayed()
        composeRule.onNodeWithText("loginctl lock-session").assertIsDisplayed()
        composeRule.onNodeWithText("Suspend PC").assertIsDisplayed()

        composeRule.onNodeWithText("Lock Screen").performClick()
        composeRule.runOnIdle {
            assertEquals("lock-screen", ranCommandKey)
        }
    }

    @Test
    fun outputCardRendersCommandOutputs() {
        val outputs = listOf(
            RunCommandOutput(RunCommandStatus.COMMAND_SUCCESSFUL, "Executing script...", 1),
            RunCommandOutput(RunCommandStatus.COMMAND_SUCCESSFUL, "Done: exit code 0", 2)
        )

        composeRule.setContent {
            RunCommandScreenContent(
                subTitle = "Linux Desktop",
                isReachable = true,
                canAddCommand = false,
                showOutputCard = true,
                commandList = listOf(CommandEntry(CommandInfo("Test", "echo test", "test"))),
                outputList = outputs,
                commandRunning = false,
                onBackPressed = {},
                onRunCommand = {},
                onCopyUrl = {},
                onClearOutput = {},
                onStopCommand = {},
                onAddCommand = {}
            )
        }

        composeRule.onNodeWithText("Executing script...").assertIsDisplayed()
        composeRule.onNodeWithText("Done: exit code 0").assertIsDisplayed()
    }
}
