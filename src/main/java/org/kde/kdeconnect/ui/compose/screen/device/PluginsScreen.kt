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
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.components.KdeSettingsRow
import org.kde.kdeconnect.ui.compose.components.KdeStandardCard
import org.kde.kdeconnect.ui.compose.components.KdeStatePanel
import org.kde.kdeconnect.ui.compose.components.KdePanelKind
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
        verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
    ) {
        SectionHeader(stringResource(R.string.device_menu_plugins))

        if (plugins.isEmpty()) {
            KdeStatePanel(
                title = stringResource(R.string.device_menu_plugins),
                description = stringResource(R.string.device_no_supported_plugins),
                kind = KdePanelKind.Unsupported
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
    KdeStandardCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KdeSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(KdeSpacing.xSmall)
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
                KdeSettingsRow(
                    title = stringResource(R.string.plugins_need_permission),
                    summary = stringResource(R.string.permission_explanation),
                    onClick = onRequiredPermissionClick
                )
            }

            if (plugin.needsOptionalPermission) {
                KdeSettingsRow(
                    title = stringResource(R.string.plugins_need_optional_permission),
                    summary = stringResource(R.string.permission_explanation),
                    onClick = onOptionalPermissionClick
                )
            }

            if (plugin.actionButtons.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KdeSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
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
                                    .padding(KdeSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KdeSpacing.small)
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
