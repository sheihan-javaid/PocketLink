/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import org.kde.kdeconnect.plugins.Plugin

/**
 * UI model describing a plugin shown on the connected device dashboard.
 *
 * Moved here from [PluginsScreen] so it is shared by both the legacy and modern
 * dashboard code paths without import churn.
 */
data class DevicePluginUiModel(
    val pluginKey: String,
    val displayName: String,
    val description: String,
    val isEnabled: Boolean,
    val actionButtons: List<Plugin.PluginUiButton>,
    val needsRequiredPermission: Boolean,
    val needsOptionalPermission: Boolean
)
