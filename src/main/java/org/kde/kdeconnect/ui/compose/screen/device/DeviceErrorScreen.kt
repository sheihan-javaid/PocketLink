/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kde.kdeconnect.ui.compose.components.KdePanelKind
import org.kde.kdeconnect.ui.compose.components.KdeStatePanel
import org.kde.kdeconnect_tp.R

@Composable
internal fun DeviceErrorPanel(onRefresh: () -> Unit) {
    KdeStatePanel(
        title = stringResource(R.string.error_not_reachable),
        description = stringResource(R.string.unreachable_description),
        kind = KdePanelKind.Offline,
        actionLabel = stringResource(R.string.refresh),
        onAction = onRefresh
    )
}
