/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect_tp.R

@Composable
internal fun DeviceErrorPanel(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    PocketLinkStatePanel(
        title = stringResource(R.string.error_not_reachable),
        description = stringResource(R.string.unreachable_description),
        kind = PocketLinkPanelKind.Offline,
        actionLabel = stringResource(R.string.refresh),
        onAction = onRefresh,
        modifier = modifier
    )
}
