/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.mousepad

import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect_tp.R
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class RemoteKeyboardScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sendUsesExistingCallbackAndIsDisabledForEmptyInput() {
        val input = mutableStateOf("")
        var sends = 0
        var clears = 0
        showScreen(input, onSend = { sends++ }, onClear = { clears++ })
        saveScreenshot("stage5-remote-keyboard-light.png")

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.send_compose))
            .assertIsNotEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.click_here_to_type))
            .performTextInput("hello from PocketLink")
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.send_compose))
            .performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clear_compose))
            .performClick()

        composeRule.runOnIdle {
            assertEquals("", input.value)
            assertEquals(1, sends)
            assertEquals(1, clears)
        }
    }

    @Test
    fun deviceIdentityAndControlsRemainReadableInDarkLargeText() {
        val input = mutableStateOf("")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.8f)
            ) {
                KdeTheme(
                    context = LocalContext.current,
                    darkTheme = true,
                    useDynamicColor = false
                ) {
                    RemoteKeyboardScreen(
                        deviceName = "Work laptop",
                        input = input,
                        onBack = {},
                        onClear = {},
                        onSend = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.click_here_to_type))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.send_compose))
            .assertIsDisplayed()
        saveScreenshot("stage5-remote-keyboard-dark.png")
    }

    private fun showScreen(
        input: androidx.compose.runtime.MutableState<String>,
        onSend: () -> Unit,
        onClear: () -> Unit
    ) {
        composeRule.setContent {
            KdeTheme(
                context = LocalContext.current,
                darkTheme = false,
                useDynamicColor = false
            ) {
                RemoteKeyboardScreen(
                    deviceName = "Work laptop",
                    input = input,
                    onBack = {},
                    onClear = {
                        onClear()
                        input.value = ""
                    },
                    onSend = onSend
                )
            }
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
