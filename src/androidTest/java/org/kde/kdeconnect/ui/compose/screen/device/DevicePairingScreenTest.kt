/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class DevicePairingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun incomingRequestShowsIdentityVerificationAndRoutesAccept() {
        val acceptActions = AtomicInteger()
        val rejectActions = AtomicInteger()

        showPairingScreen(
            pairStatus = PairingHandler.PairState.RequestedByPeer,
            verificationKey = "12AB34CD",
            onAcceptPairing = { acceptActions.incrementAndGet() },
            onRejectPairing = { rejectActions.incrementAndGet() }
        )

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithText("Pairing request from 'Work laptop'").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Verification code").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("🔑 12AB34CD").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Compare this code on both devices before accepting.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Accept").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, acceptActions.get())
            assertEquals(0, rejectActions.get())
        }
    }

    @Test
    fun incomingRequestRoutesReject() {
        val rejectActions = AtomicInteger()

        showPairingScreen(
            pairStatus = PairingHandler.PairState.RequestedByPeer,
            verificationKey = "12AB34CD",
            onRejectPairing = { rejectActions.incrementAndGet() }
        )

        composeRule.onNodeWithText("Reject").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, rejectActions.get())
        }
    }

    @Test
    fun outgoingRequestShowsWaitAndRoutesCancellation() {
        val cancelActions = AtomicInteger()

        showPairingScreen(
            pairStatus = PairingHandler.PairState.Requested,
            verificationKey = "12AB34CD",
            onCancelPairing = { cancelActions.incrementAndGet() }
        )

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithText("Waiting for Work laptop to accept the request.")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancel pairing").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, cancelActions.get())
        }
    }

    @Test
    fun pairedPresentationIdentifiesTrustedDevice() {
        showPairingScreen(
            pairStatus = PairingHandler.PairState.Paired,
            verificationKey = ""
        )

        composeRule.onNodeWithContentDescription("Pair succeeded").assertIsDisplayed()
        composeRule.onNodeWithText("Paired with Work laptop. This device is now trusted.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun timeoutFailureExplainsCurrentStateAndOffersExistingRetryAction() {
        val requestActions = AtomicInteger()

        showPairingScreen(
            pairStatus = PairingHandler.PairState.NotPaired,
            verificationKey = "",
            pairMessage = "Timed out",
            onRequestPairing = { requestActions.incrementAndGet() }
        )

        composeRule.onNodeWithText("Timed out").assertIsDisplayed()
        composeRule.onNodeWithText("Request pairing").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, requestActions.get())
        }
    }

    @Test
    fun incomingRequestRemainsReadableInDarkThemeAtLargeFontScale() {
        composeRule.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f)
            ) {
                PocketLinkTheme(context = context, darkTheme = true, useDynamicColor = false) {
                    DevicePairingScreen(
                        deviceName = "Work laptop",
                        pairStatus = PairingHandler.PairState.RequestedByPeer,
                        verificationKey = "12AB34CD",
                        onRequestPairing = {},
                        onAcceptPairing = {},
                        onRejectPairing = {},
                        onCancelPairing = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Work laptop").assertIsDisplayed()
        composeRule.onNodeWithText("Accept").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reject").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun successBannerCanBeDismissed() {
        val dismissActions = AtomicInteger()

        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                PairingSuccessBanner(
                    deviceName = "Work laptop",
                    onDismiss = { dismissActions.incrementAndGet() }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pair succeeded").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.runOnIdle {
            assertEquals(1, dismissActions.get())
        }
    }

    private fun showPairingScreen(
        pairStatus: PairingHandler.PairState,
        verificationKey: String,
        pairMessage: String? = null,
        onRequestPairing: () -> Unit = {},
        onAcceptPairing: () -> Unit = {},
        onRejectPairing: () -> Unit = {},
        onCancelPairing: () -> Unit = {}
    ) {
        composeRule.setContent {
            PocketLinkTheme(context = LocalContext.current, darkTheme = false, useDynamicColor = false) {
                DevicePairingScreen(
                    deviceName = "Work laptop",
                    pairStatus = pairStatus,
                    verificationKey = verificationKey,
                    pairMessage = pairMessage,
                    onRequestPairing = onRequestPairing,
                    onAcceptPairing = onAcceptPairing,
                    onRejectPairing = onRejectPairing,
                    onCancelPairing = onCancelPairing
                )
            }
        }
    }
}