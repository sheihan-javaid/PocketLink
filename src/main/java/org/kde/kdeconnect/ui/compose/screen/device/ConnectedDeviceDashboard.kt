/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.components.KdeConnectionStatus
import org.kde.kdeconnect.ui.compose.components.KdeSettingsRow
import org.kde.kdeconnect.ui.compose.components.KdeStandardCard
import org.kde.kdeconnect.ui.compose.components.KdeStatusKind
import org.kde.kdeconnect.ui.compose.components.SectionHeader
import org.kde.kdeconnect_tp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDeviceDashboard(
    deviceName: String,
    deviceIconRes: Int,
    isConnected: Boolean,
    plugins: List<DevicePluginUiModel>,
    batteryStatus: String?,
    connectivityStatus: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPluginEnabledChange: (pluginKey: String, enabled: Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: (pluginKey: String) -> Unit,
    onOptionalPermissionClick: (pluginKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(
                        horizontal = KdeSpacing.contentHorizontal,
                        vertical = KdeSpacing.medium
                    ),
                verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
            ) {
                DeviceIdentityCard(
                    deviceName = deviceName,
                    deviceIconRes = deviceIconRes,
                    isConnected = isConnected
                )

                if (!isConnected) {
                    DeviceErrorPanel(onRefresh = onRefresh)
                } else {
                    if (batteryStatus != null || connectivityStatus != null) {
                        SectionHeader(stringResource(R.string.view_status_title))
                        KdeStandardCard {
                            batteryStatus?.let {
                                KdeSettingsRow(title = it)
                            }
                            connectivityStatus?.let {
                                KdeSettingsRow(
                                    title = stringResource(R.string.pref_plugin_connectivity_report),
                                    summary = it
                                )
                            }
                        }
                    }

                    PluginsScreen(
                        plugins = plugins,
                        onPluginEnabledChange = onPluginEnabledChange,
                        onButtonClick = onButtonClick,
                        onRequiredPermissionClick = onRequiredPermissionClick,
                        onOptionalPermissionClick = onOptionalPermissionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceIdentityCard(
    deviceName: String,
    deviceIconRes: Int,
    isConnected: Boolean
) {
    KdeStandardCard {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(KdeSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(deviceIconRes),
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                KdeConnectionStatus(
                    label = stringResource(
                        if (isConnected) R.string.device_discovery_connected
                        else R.string.error_not_reachable
                    ),
                    kind = if (isConnected) KdeStatusKind.Connected else KdeStatusKind.Warning
                )
            }
        }
    }
}
