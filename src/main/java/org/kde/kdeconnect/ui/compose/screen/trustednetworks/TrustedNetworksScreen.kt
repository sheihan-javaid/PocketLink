/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.trustednetworks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.PocketLinkTopAppBar
import org.kde.kdeconnect.ui.compose.components.PocketLinkConfirmationDialog
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.PocketLinkStandardCard
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect.ui.compose.components.PocketLinkThemePreviews
import org.kde.kdeconnect.ui.compose.components.SectionHeader
import org.kde.kdeconnect_tp.R

@Composable
fun TrustedNetworksScreen(
    allNetworksAllowed: Boolean,
    trustedNetworks: List<String>,
    currentSsid: String?,
    hasLocationPermission: Boolean,
    onNavigateUp: () -> Unit,
    onAllNetworksAllowedChange: (Boolean) -> Unit,
    onAddCurrentNetwork: () -> Unit,
    onDeleteNetwork: (String) -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var networkPendingDelete by remember { mutableStateOf<String?>(null) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            PocketLinkTopAppBar(
                title = stringResource(R.string.trusted_networks),
                navIconOnClick = onNavigateUp
            )
        }
    ) { innerPadding ->
        TrustedNetworksScreenContent(
            allNetworksAllowed = allNetworksAllowed,
            trustedNetworks = trustedNetworks,
            currentSsid = currentSsid,
            onAllNetworksAllowedChange = { checked ->
                if (!checked && !hasLocationPermission) {
                    showPermissionRationaleDialog = true
                } else {
                    onAllNetworksAllowedChange(checked)
                }
            },
            onAddCurrentNetwork = onAddCurrentNetwork,
            onRequestDeleteNetwork = { networkPendingDelete = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    networkPendingDelete?.let { network ->
        PocketLinkConfirmationDialog(
            title = stringResource(R.string.remove_trusted_network_title),
            message = stringResource(R.string.remove_trusted_network_message, network),
            confirmLabel = stringResource(R.string.remove),
            dismissLabel = stringResource(android.R.string.cancel),
            onConfirm = {
                onDeleteNetwork(network)
                networkPendingDelete = null
            },
            onDismiss = { networkPendingDelete = null },
            destructive = true
        )
    }

    if (showPermissionRationaleDialog) {
        PocketLinkConfirmationDialog(
            title = stringResource(R.string.location_permission_needed_title),
            message = stringResource(R.string.location_permission_needed_desc),
            confirmLabel = stringResource(R.string.ok),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                showPermissionRationaleDialog = false
                onRequestLocationPermission()
            },
            onDismiss = { showPermissionRationaleDialog = false }
        )
    }
}

@Composable
fun TrustedNetworksScreenContent(
    allNetworksAllowed: Boolean,
    trustedNetworks: List<String>,
    currentSsid: String?,
    onAllNetworksAllowedChange: (Boolean) -> Unit,
    onAddCurrentNetwork: () -> Unit,
    onRequestDeleteNetwork: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = PocketLinkSpacing.compactContentMaxWidth)
                .padding(horizontal = PocketLinkSpacing.contentHorizontal),
            contentPadding = PaddingValues(
                top = PocketLinkSpacing.medium,
                bottom = PocketLinkSpacing.large
            ),
            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
        ) {
            item(key = "toggle_card") {
                PocketLinkStandardCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
                            .padding(PocketLinkSpacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.allow_all_networks_text),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.trusted_networks_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(PocketLinkSpacing.medium))
                        Switch(
                            checked = allNetworksAllowed,
                            onCheckedChange = onAllNetworksAllowedChange
                        )
                    }
                }
            }

            if (!allNetworksAllowed) {
                if (currentSsid != null && currentSsid !in trustedNetworks) {
                    item(key = "add_current_network") {
                        PocketLinkStandardCard(
                            onClick = onAddCurrentNetwork
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
                                    .padding(PocketLinkSpacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(PocketLinkSpacing.medium))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.add_trusted_network, currentSsid),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "header_trusted") {
                    SectionHeader(title = stringResource(R.string.trusted_networks))
                }

                if (trustedNetworks.isEmpty()) {
                    item(key = "empty_state") {
                        PocketLinkStatePanel(
                            title = stringResource(R.string.trusted_networks),
                            description = stringResource(R.string.empty_trusted_networks_list_text),
                            kind = PocketLinkPanelKind.Empty,
                            icon = null
                        )
                    }
                } else {
                    items(
                        items = trustedNetworks,
                        key = { it }
                    ) { network ->
                        PocketLinkStandardCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
                                    .padding(
                                        start = PocketLinkSpacing.cardPadding,
                                        top = PocketLinkSpacing.small,
                                        bottom = PocketLinkSpacing.small,
                                        end = PocketLinkSpacing.small
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_wifi),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(PocketLinkSpacing.medium))
                                Text(
                                    text = network,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRequestDeleteNetwork(network) },
                                    modifier = Modifier.defaultMinSize(
                                        minWidth = PocketLinkSpacing.minTouchTarget,
                                        minHeight = PocketLinkSpacing.minTouchTarget
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@PocketLinkThemePreviews
@Composable
private fun TrustedNetworksScreenPreview() {
    PocketLinkTheme(context = LocalContext.current) {
        TrustedNetworksScreenContent(
            allNetworksAllowed = false,
            trustedNetworks = listOf("Home-WiFi-5G", "Work-Office-Secure"),
            currentSsid = "CoffeeShop-Guest",
            onAllNetworksAllowedChange = {},
            onAddCurrentNetwork = {},
            onRequestDeleteNetwork = {}
        )
    }
}

