/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class KdeWindowSizeClass {
    Compact,
    Medium,
    Expanded
}

@Composable
fun KdeResponsiveContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = KdeSpacing.contentHorizontal),
    content: @Composable (KdeWindowSizeClass) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = when {
            maxWidth < KdeSpacing.compactContentMaxWidth -> KdeWindowSizeClass.Compact
            maxWidth < KdeSpacing.mediumContentMaxWidth -> KdeWindowSizeClass.Medium
            else -> KdeWindowSizeClass.Expanded
        }
        val maxContentWidth = when (sizeClass) {
            KdeWindowSizeClass.Compact -> null
            KdeWindowSizeClass.Medium -> KdeSpacing.mediumContentMaxWidth
            KdeWindowSizeClass.Expanded -> KdeSpacing.expandedContentMaxWidth
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(maxContentWidth?.let { Modifier.widthIn(max = it) } ?: Modifier)
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content(sizeClass)
        }
    }
}
