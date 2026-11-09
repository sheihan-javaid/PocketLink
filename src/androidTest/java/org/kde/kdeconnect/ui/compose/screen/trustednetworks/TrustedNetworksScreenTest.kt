/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.trustednetworks

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect_tp.R

@RunWith(AndroidJUnit4::class)
class TrustedNetworksScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun allowAllModeDisplaysSwitchAndHidesNetworkList() {
        composeRule.setContent {
            KdeTheme(composeRule.activity) {
                TrustedNetworksScreen(
                    allNetworksAllowed = true,
                    trustedNetworks = listOf("HomeWiFi"),
                    currentSsid = "OfficeWiFi",
                    hasLocationPermission = true,
                    onNavigateUp = {},
                    onAllNetworksAllowedChange = {},
                    onAddCurrentNetwork = {},
                    onDeleteNetwork = {},
                    onRequestLocationPermission = {}
                )
            }
        }

        val allowAllText = composeRule.activity.getString(R.string.allow_all_networks_text)
        composeRule.onNodeWithText(allowAllText).assertIsDisplayed()
        composeRule.onNodeWithText("HomeWiFi").assertDoesNotExist()
    }

    @Test
    fun restrictedModeWithEmptyListDisplaysEmptyStateAndAddCurrentNetwork() {
        var addClicked = false
        val currentSsid = "CoffeeShop-Guest"

        composeRule.setContent {
            KdeTheme(composeRule.activity) {
                TrustedNetworksScreen(
                    allNetworksAllowed = false,
                    trustedNetworks = emptyList(),
                    currentSsid = currentSsid,
                    hasLocationPermission = true,
                    onNavigateUp = {},
                    onAllNetworksAllowedChange = {},
                    onAddCurrentNetwork = { addClicked = true },
                    onDeleteNetwork = {},
                    onRequestLocationPermission = {}
                )
            }
        }

        val emptyText = composeRule.activity.getString(R.string.empty_trusted_networks_list_text)
        composeRule.onNodeWithText(emptyText).assertIsDisplayed()

        val addText = composeRule.activity.getString(R.string.add_trusted_network, currentSsid)
        composeRule.onNodeWithText(addText).assertIsDisplayed().performClick()
        assertTrue("Add current network should be clicked", addClicked)
    }

    @Test
    fun restrictedModeDisplaysNetworksAndAllowsDeletion() {
        var deletedNetwork: String? = null
        val networks = listOf("Home-Mesh-5G", "Work-HQ")

        composeRule.setContent {
            KdeTheme(composeRule.activity) {
                TrustedNetworksScreen(
                    allNetworksAllowed = false,
                    trustedNetworks = networks,
                    currentSsid = null,
                    hasLocationPermission = true,
                    onNavigateUp = {},
                    onAllNetworksAllowedChange = {},
                    onAddCurrentNetwork = {},
                    onDeleteNetwork = { deletedNetwork = it },
                    onRequestLocationPermission = {}
                )
            }
        }

        composeRule.onNodeWithText("Home-Mesh-5G").assertIsDisplayed()
        composeRule.onNodeWithText("Work-HQ").assertIsDisplayed()

        // Click delete on Home-Mesh-5G
        val removeDescription = composeRule.activity.getString(R.string.remove)
        val deleteButtons = composeRule.onAllNodes(
            androidx.compose.ui.test.hasContentDescription(removeDescription)
        )
        deleteButtons[0].performClick()

        // Confirmation dialog should appear
        val dialogTitle = composeRule.activity.getString(R.string.remove_trusted_network_title)
        composeRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        // Confirm delete
        composeRule.onNodeWithText(removeDescription).performClick()
        assertEquals("Home-Mesh-5G", deletedNetwork)
    }
}

