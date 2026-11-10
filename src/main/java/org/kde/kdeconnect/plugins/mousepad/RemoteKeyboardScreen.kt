/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.mousepad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.PocketLinkTextButton
import org.kde.kdeconnect.ui.compose.PocketLinkTextField
import org.kde.kdeconnect.ui.compose.PocketLinkTopAppBar
import org.kde.kdeconnect.ui.compose.components.PocketLinkStandardCard
import org.kde.kdeconnect_tp.R

@Composable
internal fun RemoteKeyboardScreen(
    deviceName: String,
    input: MutableState<String>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            PocketLinkTopAppBar(
                title = stringResource(R.string.compose_send_title),
                subTitle = deviceName,
                navIconOnClick = onBack,
                navIconDescription = stringResource(
                    androidx.appcompat.R.string.abc_action_bar_up_description
                ),
                actions = {
                    PocketLinkTextButton(
                        modifier = Modifier.padding(horizontal = PocketLinkSpacing.small),
                        onClick = onClear,
                        text = stringResource(R.string.clear_compose)
                    )
                }
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxSize()
                .imePadding()
                .padding(
                    horizontal = PocketLinkSpacing.contentHorizontal,
                    vertical = PocketLinkSpacing.medium
                ),
            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
        ) {
            PocketLinkStandardCard(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PocketLinkSpacing.small)
                ) {
                    PocketLinkTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .fillMaxSize(),
                        input = input,
                        label = stringResource(R.string.click_here_to_type)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PocketLinkTextButton(
                    onClick = onSend,
                    modifier = Modifier,
                    enabled = input.value.isNotEmpty(),
                    text = stringResource(R.string.send_compose),
                    iconLeft = Icons.AutoMirrored.Filled.Send
                )
            }
        }
    }
}
