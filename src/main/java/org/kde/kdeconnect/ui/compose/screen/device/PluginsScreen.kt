/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.components.PocketLinkSettingsRow
import org.kde.kdeconnect.ui.compose.components.PocketLinkStandardCard
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.SectionHeader
import org.kde.kdeconnect_tp.R

data class DevicePluginUiModel(
    val pluginKey: String,
    val displayName: String,
    val description: String,
    val isEnabled: Boolean,
    val actionButtons: List<Plugin.PluginUiButton>,
    val needsRequiredPermission: Boolean,
    val needsOptionalPermission: Boolean
)

@Composable
fun PluginsScreen(
    plugins: List<DevicePluginUiModel>,
    onPluginEnabledChange: (pluginKey: String, enabled: Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: (pluginKey: String) -> Unit,
    onOptionalPermissionClick: (pluginKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
    ) {
        SectionHeader(stringResource(R.string.device_menu_plugins))

        if (plugins.isEmpty()) {
            PocketLinkStatePanel(
                title = stringResource(R.string.device_menu_plugins),
                description = stringResource(R.string.device_no_supported_plugins),
                kind = PocketLinkPanelKind.Unsupported
            )
        } else {
            plugins.forEach { plugin ->
                DevicePluginCard(
                    plugin = plugin,
                    onEnabledChange = { onPluginEnabledChange(plugin.pluginKey, it) },
                    onButtonClick = onButtonClick,
                    onRequiredPermissionClick = {
                        onRequiredPermissionClick(plugin.pluginKey)
                    },
                    onOptionalPermissionClick = {
                        onOptionalPermissionClick(plugin.pluginKey)
                    }
                )
            }
        }
    }
}

@Composable
private fun DevicePluginCard(
    plugin: DevicePluginUiModel,
    onEnabledChange: (Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: () -> Unit,
    onOptionalPermissionClick: () -> Unit
) {
    PocketLinkStandardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PocketLinkSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.xSmall)
                ) {
                    Text(plugin.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        plugin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics {
                        contentDescription = plugin.displayName
                    }
                )
            }

            if (plugin.needsRequiredPermission) {
                PocketLinkSettingsRow(
                    title = stringResource(R.string.plugins_need_permission),
                    summary = stringResource(R.string.permission_explanation),
                    onClick = onRequiredPermissionClick
                )
            }

            if (plugin.needsOptionalPermission) {
                PocketLinkSettingsRow(
                    title = stringResource(R.string.plugins_need_optional_permission),
                    summary = stringResource(R.string.permission_explanation),
                    onClick = onOptionalPermissionClick
                )
            }

            if (plugin.actionButtons.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
                ) {
                    plugin.actionButtons.forEach { button ->
                        Card(
                            onClick = { onButtonClick(button) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 64.dp)
                                .semantics { role = Role.Button },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PocketLinkSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
                            ) {
                                Icon(
                                    painter = painterResource(button.iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = button.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
