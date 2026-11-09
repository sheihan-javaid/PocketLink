/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.digitizer

import android.view.MotionEvent
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.KdeTopAppBar
import org.kde.kdeconnect.ui.compose.components.DeviceDisconnectedOverlay
import org.kde.kdeconnect_tp.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DigitizerScreen(
    deviceName: String,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onOpenSettings: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    hideDrawButton: Boolean,
    drawButtonSide: String,
    modifier: Modifier = Modifier,
    device: Device? = null,
    onToolEvent: (ToolEvent) -> Unit = {},
    onDrawingPadReady: (DrawingPadView) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val fullscreenDescription = stringResource(R.string.enable_fullscreen)
    val closeDescription = stringResource(R.string.close)
    var isFingerDrawingActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val buttonAlignment = when (drawButtonSide) {
        "top_left" -> Alignment.TopStart
        "top_right" -> Alignment.TopEnd
        "bottom_right" -> Alignment.BottomEnd
        else -> Alignment.BottomStart
    }

    KdeTheme(context) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = !isFullscreen,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    KdeTopAppBar(
                        title = stringResource(R.string.pref_plugin_digitizer),
                        subTitle = deviceName,
                        navIconOnClick = { onBackPressedDispatcher.onBackPressed() },
                        actions = {
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.semantics {
                                    contentDescription = fullscreenDescription
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_open_in_full_24dp),
                                    contentDescription = fullscreenDescription,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.extra_options),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.device_menu_plugins)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_settings_24dp),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isFullscreen) Modifier else Modifier.padding(paddingValues))
            ) {
                var drawingPadViewRef by remember { mutableStateOf<DrawingPadView?>(null) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        DrawingPadView(ctx).apply {
                            keepScreenOn = true
                            eventListener = object : DrawingPadView.EventListener {
                                override fun onToolEvent(event: ToolEvent) {
                                    onToolEvent(event)
                                }

                                override fun onFingerTouchEvent(touching: Boolean) {
                                    isFingerDrawingActive = touching
                                }
                            }
                            drawingPadViewRef = this
                            onDrawingPadReady(this)
                        }
                    }
                )

                if (!hideDrawButton) {
                    val drawButtonDesc = stringResource(R.string.digitizer_draw_button)
                    val containerColor = if (isFingerDrawingActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val contentColor = if (isFingerDrawingActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(KdeSpacing.large)
                            .then(if (isFullscreen) Modifier.safeDrawingPadding() else Modifier),
                        contentAlignment = buttonAlignment
                    ) {
                        FloatingActionButton(
                            onClick = {},
                            shape = CircleShape,
                            containerColor = containerColor,
                            contentColor = contentColor,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = if (isFingerDrawingActive) 8.dp else 4.dp
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .semantics {
                                    contentDescription = drawButtonDesc
                                }
                                .pointerInteropFilter { event ->
                                    val pad = drawingPadViewRef
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isFingerDrawingActive = true
                                            pad?.fingerTouchEventsEnabled = true
                                            true
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            isFingerDrawingActive = false
                                            pad?.fingerTouchEventsEnabled = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_finger_24dp),
                                contentDescription = drawButtonDesc,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                if (isFullscreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(KdeSpacing.medium),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        SmallFloatingActionButton(
                            onClick = onToggleFullscreen,
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = closeDescription
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = closeDescription
                            )
                        }
                    }
                }

                device?.let { DeviceDisconnectedOverlay(device = it) }
            }
        }
    }
}
