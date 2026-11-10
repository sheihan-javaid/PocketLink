/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.ui.compose.PocketLinkTheme

@Composable
private fun PocketLinkText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text,
        color = color,
        style = style,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = maxLines,
        modifier = if (onClick != null) {
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        }
    )
}

@Composable
fun PocketLinkBodySmallText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) = PocketLinkText(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    modifier = modifier,
    textAlign = textAlign,
    color = color,
    fontSize = fontSize,
    maxLines = maxLines,
    onClick = onClick
)

@Composable
fun PocketLinkBodyMediumText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) =
    PocketLinkText(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
        textAlign = textAlign,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        onClick = onClick
    )

@Composable
fun PocketLinkBodyLargeText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) =
    PocketLinkText(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier,
        textAlign = textAlign,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        onClick = onClick
    )

@Composable
fun PocketLinkTitleMediumText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
) =
    PocketLinkText(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
        textAlign = textAlign,
        color = color,
        fontSize = fontSize,
        maxLines = maxLines,
        onClick = onClick
    )

@PocketLinkPortraitThemePreviews
@Composable
private fun PocketLinkTextsPreview() {
    PocketLinkTheme(context = LocalContext.current) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PocketLinkBodySmallText(
                text = "PocketLinkBodySmallText",
                color = Color(0xFFCC2222),
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(all = 16.dp),
            )
            PocketLinkBodyMediumText(
                text = "PocketLinkBodyMediumText",
                modifier = Modifier.padding(all = 16.dp),
            )
            PocketLinkBodyLargeText(
                text = "PocketLinkBodyLargeText",
                modifier = Modifier.padding(all = 16.dp),
            )
            PocketLinkTitleMediumText(
                text = "PocketLinkTitleMediumText",
                modifier = Modifier.padding(all = 16.dp),
            )
        }
    }
}
