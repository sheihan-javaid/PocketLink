/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.modern.LocalDashboardTokens
import org.kde.kdeconnect.ui.compose.screen.device.modern.VolumeControlUiState
import org.kde.kdeconnect_tp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualVolumeCard(
    volumeState: VolumeControlUiState,
    onVolumeChange: (Int) -> Unit,
    onVolumeToggleMute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AuroraGlassCard(modifier = modifier) {
        ContextualVolumeContent(
            volumeState = volumeState,
            onVolumeChange = onVolumeChange,
            onVolumeToggleMute = onVolumeToggleMute,
            modifier = Modifier.padding(start = KdeSpacing.cardPadding, end = KdeSpacing.cardPadding, top = 12.dp, bottom = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualVolumeContent(
    volumeState: VolumeControlUiState,
    onVolumeChange: (Int) -> Unit,
    onVolumeToggleMute: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(volumeState.volume) {
        mutableFloatStateOf(volumeState.volume.toFloat())
    }

    val haptic = LocalHapticFeedback.current
    var lastHapticStep by remember { mutableFloatStateOf(sliderPosition / 10f) }
    val tokens = LocalDashboardTokens.current

    val isMuted = sliderPosition <= 0f || volumeState.isMuted
    val isLowVolume = sliderPosition in 1f..33f

    val speakerIconRes = if (isMuted) R.drawable.ic_modern_volume_mute else R.drawable.ic_modern_volume
    val speakerIconTint = when {
        isMuted -> MaterialTheme.colorScheme.error
        isLowVolume -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val speakerContentDesc = when {
        isMuted -> stringResource(R.string.volume_muted_desc)
        isLowVolume -> stringResource(R.string.volume_low_desc, sliderPosition.toInt())
        else -> stringResource(R.string.volume_normal_desc, sliderPosition.toInt())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onVolumeToggleMute,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(speakerIconRes),
                        contentDescription = speakerContentDesc,
                        colorFilter = ColorFilter.tint(speakerIconTint),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(KdeSpacing.xSmall))
                Text(
                    text = volumeState.sinkName.ifEmpty { stringResource(R.string.pref_plugin_systemvolume) },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.shareContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${sliderPosition.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = tokens.onShareContainer
                )
            }
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                val currentStep = (newValue / 10f).toInt()
                if (currentStep != lastHapticStep.toInt()) {
                    lastHapticStep = currentStep.toFloat()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onValueChangeFinished = {
                onVolumeChange(sliderPosition.toInt())
            },
            valueRange = 0f..volumeState.maxVolume.toFloat(),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(6.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    drawStopIndicator = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}
