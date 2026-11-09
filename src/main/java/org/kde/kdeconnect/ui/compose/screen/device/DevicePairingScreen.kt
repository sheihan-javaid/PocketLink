/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.ui.compose.KdeResponsiveContent
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.components.KdeConnectionStatus
import org.kde.kdeconnect.ui.compose.components.KdeStandardCard
import org.kde.kdeconnect.ui.compose.components.KdeStatusKind
import org.kde.kdeconnect.ui.compose.components.KdeThemePreviews
import org.kde.kdeconnect_tp.R

@Composable
fun DevicePairingScreen(
    deviceName: String,
    pairStatus: PairingHandler.PairState,
    verificationKey: String,
    pairMessage: String? = null,
    onRequestPairing: () -> Unit,
    onAcceptPairing: () -> Unit,
    onRejectPairing: () -> Unit,
    onCancelPairing: () -> Unit
) {
    val statusLabel = when (pairStatus) {
        PairingHandler.PairState.NotPaired -> stringResource(R.string.device_not_paired)
        PairingHandler.PairState.Requested -> stringResource(R.string.pair_requested)
        PairingHandler.PairState.RequestedByPeer -> stringResource(R.string.pair_requested)
        PairingHandler.PairState.Paired -> stringResource(R.string.pair_succeeded)
    }

    KdeResponsiveContent {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = KdeSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
        ) {
            PairingIdentityCard(
                deviceName = deviceName,
                status = statusLabel,
                statusKind = when {
                    pairMessage != null -> KdeStatusKind.Error
                    pairStatus == PairingHandler.PairState.Paired -> KdeStatusKind.Connected
                    pairStatus == PairingHandler.PairState.Requested ||
                        pairStatus == PairingHandler.PairState.RequestedByPeer -> KdeStatusKind.Pending
                    else -> KdeStatusKind.Neutral
                }
            )

            when (pairStatus) {
                PairingHandler.PairState.NotPaired -> {
                    PairingMessageCard(
                        title = pairMessage ?: stringResource(R.string.device_not_paired),
                        message = if (pairMessage == null) {
                            stringResource(R.string.pairing_explanation)
                        } else {
                            null
                        },
                        statusKind = if (pairMessage != null) {
                            KdeStatusKind.Error
                        } else {
                            KdeStatusKind.Neutral
                        }
                    )
                    Button(
                        onClick = onRequestPairing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KdeSpacing.xSmall)
                    ) {
                        Text(stringResource(R.string.request_pairing))
                    }
                }

                PairingHandler.PairState.Requested -> {
                    PairingMessageCard(
                        title = stringResource(R.string.pair_requested),
                        message = stringResource(R.string.pairing_waiting_for_device, deviceName)
                    )
                    VerificationCodeCard(verificationKey = verificationKey)
                    TextButton(
                        onClick = onCancelPairing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KdeSpacing.xSmall)
                    ) {
                        Text(stringResource(R.string.cancel_pairing))
                    }
                }

                PairingHandler.PairState.RequestedByPeer -> {
                    PairingMessageCard(
                        title = stringResource(R.string.pairing_request_from, deviceName),
                        message = stringResource(R.string.pairing_explanation)
                    )
                    VerificationCodeCard(verificationKey = verificationKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = KdeSpacing.xSmall),
                        horizontalArrangement = Arrangement.spacedBy(KdeSpacing.small)
                    ) {
                        OutlinedButton(
                            onClick = onRejectPairing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.pairing_reject),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = onAcceptPairing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.pairing_accept))
                        }
                    }
                }

                PairingHandler.PairState.Paired -> {
                    PairingMessageCard(
                        title = stringResource(R.string.pair_succeeded),
                        message = stringResource(R.string.pairing_trusted_device, deviceName),
                        statusKind = KdeStatusKind.Connected
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingIdentityCard(
    deviceName: String,
    status: String,
    statusKind: KdeStatusKind
) {
    KdeStandardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            KdeConnectionStatus(label = status, kind = statusKind)
        }
    }
}

@Composable
private fun PairingMessageCard(
    title: String,
    message: String?,
    statusKind: KdeStatusKind = KdeStatusKind.Neutral
) {
    KdeStandardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .semantics {
                        heading()
                        if (statusKind == KdeStatusKind.Error) {
                            liveRegion = LiveRegionMode.Polite
                        }
                    }
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerificationCodeCard(verificationKey: String) {
    if (verificationKey.isBlank()) return

    KdeStandardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
        ) {
            Text(
                text = stringResource(R.string.pairing_verification_code_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.pairing_verification_code, verificationKey),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.pairing_compare_verification_code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PairingSuccessBanner(
    deviceName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    KdeStandardCard(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = KdeSpacing.cardPadding, end = KdeSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = KdeSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(KdeSpacing.xSmall)
            ) {
                KdeConnectionStatus(
                    label = stringResource(R.string.pair_succeeded),
                    kind = KdeStatusKind.Connected
                )
                Text(
                    text = stringResource(R.string.pairing_trusted_device, deviceName),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(KdeSpacing.xSmall))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(vertical = KdeSpacing.xSmall)
            ) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@KdeThemePreviews
@Composable
private fun DevicePairingScreenIncomingPreview() {
    KdeTheme(context = LocalContext.current) {
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
