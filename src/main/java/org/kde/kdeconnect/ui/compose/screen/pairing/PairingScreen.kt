/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.PocketLinkComponentSize
import org.kde.kdeconnect.ui.compose.PocketLinkResponsiveContent
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.PocketLinkWindowSizeClass
import org.kde.kdeconnect.ui.compose.components.PocketLinkConnectionStatus
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.PocketLinkStandardCard
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatusKind
import org.kde.kdeconnect.ui.compose.components.SectionHeader
import org.kde.kdeconnect.ui.compose.model.device.DeviceUiModel
import org.kde.kdeconnect_tp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    uiState: PairingUiState,
    onClick: (String) -> Unit,
    onWifiSettingsClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onLocalNetworkPermissionClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onCustomDevicesClick: () -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        PocketLinkResponsiveContent(
            contentPadding = PaddingValues(horizontal = PocketLinkSpacing.contentHorizontal)
        ) { windowSizeClass ->
            PairingDeviceList(
                uiState = uiState,
                windowSizeClass = windowSizeClass,
                onClick = onClick,
                onWifiSettingsClick = onWifiSettingsClick,
                onNotificationSettingsClick = onNotificationSettingsClick,
                onLocalNetworkPermissionClick = onLocalNetworkPermissionClick,
                onRefresh = onRefresh,
                onCustomDevicesClick = onCustomDevicesClick
            )
        }
    }
}

@Composable
private fun PairingDeviceList(
    uiState: PairingUiState,
    windowSizeClass: PocketLinkWindowSizeClass,
    onClick: (String) -> Unit,
    onWifiSettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLocalNetworkPermissionClick: () -> Unit,
    onRefresh: () -> Unit,
    onCustomDevicesClick: () -> Unit
) {
    val horizontalPadding = when (windowSizeClass) {
        PocketLinkWindowSizeClass.Compact -> 0.dp
        PocketLinkWindowSizeClass.Medium -> PocketLinkSpacing.medium
        PocketLinkWindowSizeClass.Expanded -> PocketLinkSpacing.large
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        contentPadding = PaddingValues(
            top = PocketLinkSpacing.medium,
            bottom = PocketLinkSpacing.xLarge
        ),
        verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
    ) {
        item(key = "header") {
            PairingHeader(
                onRefresh = onRefresh,
                onCustomDevicesClick = onCustomDevicesClick
            )
        }

        if (!uiState.hasLocalNetworkPermission) {
            item(key = "local-network-permission") {
                PairingNotice(
                    text = stringResource(R.string.pairing_missing_local_network_permission),
                    iconRes = R.drawable.ic_warning,
                    onClick = onLocalNetworkPermissionClick
                )
            }
        }

        if (!uiState.hasNotificationsPermission) {
            item(key = "notifications-permission") {
                PairingNotice(
                    text = stringResource(R.string.no_notifications),
                    iconRes = R.drawable.ic_warning,
                    onClick = onNotificationSettingsClick
                )
            }
        }

        if (uiState.hasLocalNetworkPermission && uiState.hasNotificationsPermission) {
            if (uiState.hasDuplicateNames) {
                item(key = "duplicate-names") {
                    PairingNotice(
                        text = stringResource(R.string.pairing_duplicate_names),
                        iconRes = R.drawable.ic_warning
                    )
                }
            }

            if (!uiState.isWifiAvailable && uiState.connected.isEmpty() && uiState.available.isEmpty()) {
                item(key = "network-unavailable") {
                    PairingNotice(
                        text = stringResource(R.string.no_wifi),
                        iconRes = R.drawable.ic_wifi,
                        onClick = onWifiSettingsClick
                    )
                }
            } else if (!uiState.isTrustedNetwork) {
                item(key = "untrusted-network") {
                    PairingNotice(text = stringResource(R.string.on_non_trusted_message))
                }
            }
        }

        item(key = "connected-heading") {
            SectionHeader(title = stringResource(R.string.category_connected_devices))
        }
        if (uiState.connected.isEmpty()) {
            item(key = "connected-empty") {
                Text(
                    text = stringResource(R.string.device_list_empty),
                    modifier = Modifier.padding(
                        horizontal = PocketLinkSpacing.medium,
                        vertical = PocketLinkSpacing.small
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.connected, key = { "connected-${it.id}" }) { device ->
                DeviceRosterCard(
                    device = device,
                    status = stringResource(R.string.device_discovery_connected),
                    statusKind = PocketLinkStatusKind.Connected,
                    onClick = onClick
                )
            }
        }

        item(key = "available-heading") {
            SectionHeader(title = stringResource(R.string.category_not_paired_devices))
        }
        if (uiState.available.isEmpty()) {
            item(key = "available-empty") {
                val description = when {
                    !uiState.hasLocalNetworkPermission ->
                        stringResource(R.string.pairing_missing_local_network_permission)
                    !uiState.isWifiAvailable && uiState.connected.isEmpty() ->
                        stringResource(R.string.no_wifi)
                    else -> stringResource(R.string.pairing_description)
                }

                PocketLinkStatePanel(
                    title = stringResource(R.string.device_discovery_empty_title),
                    description = description,
                    kind = if (!uiState.hasLocalNetworkPermission) {
                        PocketLinkPanelKind.Error
                    } else {
                        PocketLinkPanelKind.Empty
                    },
                    actionLabel = stringResource(R.string.custom_device_list),
                    onAction = onCustomDevicesClick
                )
            }
        } else {
            items(uiState.available, key = { "available-${it.id}" }) { device ->
                DeviceRosterCard(
                    device = device,
                    status = stringResource(R.string.request_pairing),
                    statusKind = PocketLinkStatusKind.Pending,
                    onClick = onClick
                )
            }
        }

        if (uiState.remembered.isNotEmpty()) {
            item(key = "remembered-heading") {
                SectionHeader(title = stringResource(R.string.category_remembered_devices))
            }
            items(uiState.remembered, key = { "remembered-${it.id}" }) { device ->
                DeviceRosterCard(
                    device = device,
                    status = stringResource(R.string.error_not_reachable),
                    statusKind = PocketLinkStatusKind.Neutral,
                    onClick = onClick
                )
            }
        }

        if (uiState.connected.isEmpty() && uiState.available.isEmpty() && uiState.remembered.isEmpty()) {
            item(key = "bluetooth-guidance") {
                Text(
                    text = stringResource(R.string.device_discovery_bluetooth_hint),
                    modifier = Modifier.padding(
                        horizontal = PocketLinkSpacing.medium,
                        vertical = PocketLinkSpacing.small
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PairingHeader(
    onRefresh: () -> Unit,
    onCustomDevicesClick: () -> Unit
) {
    val refreshLabel = stringResource(R.string.refresh)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PocketLinkSpacing.small),
        verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pairing_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.pairing_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.semantics {
                    contentDescription = refreshLabel
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(
            onClick = onCustomDevicesClick,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(PocketLinkSpacing.small))
            Text(stringResource(R.string.custom_device_list))
        }
    }
}

@Composable
private fun DeviceRosterCard(
    device: DeviceUiModel,
    status: String,
    statusKind: PocketLinkStatusKind,
    onClick: (String) -> Unit
) {
    PocketLinkStandardCard(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        onClick = { onClick(device.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = PocketLinkComponentSize.deviceCardMinHeight)
                .padding(PocketLinkSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (device.icon > 0) {
                Image(
                    painter = painterResource(device.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(PocketLinkComponentSize.iconLarge)
                )
                Spacer(Modifier.width(PocketLinkSpacing.medium))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (device.summaryRes > 0) {
                    Text(
                        text = stringResource(device.summaryRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PocketLinkConnectionStatus(label = status, kind = statusKind)
            }
        }
    }
}

@Composable
private fun PairingNotice(
    text: String,
    iconRes: Int? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PocketLinkSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(PocketLinkSpacing.medium))
            } else {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(PocketLinkSpacing.medium))
            }
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
