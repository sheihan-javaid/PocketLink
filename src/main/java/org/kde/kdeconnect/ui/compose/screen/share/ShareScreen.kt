/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.extensions.safeDrawingBottomPadding
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
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
fun ShareScreen(
    devices: List<DeviceUiModel>,
    intentHasUrl: Boolean,
    isRefreshing: Boolean,
    onDeviceClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val state = rememberLazyGridState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .padding(horizontal = PocketLinkSpacing.contentHorizontal)
            ) {
                SectionHeader(title = stringResource(R.string.share_to))
                if (intentHasUrl) {
                    Text(
                        text = stringResource(R.string.unreachable_device_url_share_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = PocketLinkSpacing.medium,
                            vertical = PocketLinkSpacing.small
                        )
                    )
                }

                if (devices.isEmpty()) {
                    PocketLinkStatePanel(
                        title = stringResource(R.string.device_discovery_empty_title),
                        description = stringResource(R.string.foreground_notification_no_devices),
                        kind = PocketLinkPanelKind.Empty
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        contentPadding = safeDrawingBottomPadding(),
                        horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
                    ) {
                        items(devices, key = DeviceUiModel::id) { device ->
                            ShareDeviceCard(
                                device = device,
                                onClick = { onDeviceClick(device.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareDeviceCard(
    device: DeviceUiModel,
    onClick: () -> Unit
) {
    PocketLinkStandardCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PocketLinkSpacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (device.icon > 0) {
                Image(
                    imageVector = ImageVector.vectorResource(device.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PocketLinkConnectionStatus(
                    label = stringResource(
                        if (device.isReachable) R.string.device_discovery_connected
                        else R.string.runcommand_notreachable
                    ),
                    kind = if (device.isReachable) PocketLinkStatusKind.Connected else PocketLinkStatusKind.Warning
                )
            }
        }
    }
}
