/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.plugins.runcommand

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.ui.compose.PocketLinkComponentSize
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.PocketLinkTopAppBar
import org.kde.kdeconnect.ui.compose.components.DeviceDisconnectedOverlay
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.PocketLinkStandardCard
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect.ui.compose.kdeSemanticColors
import org.kde.kdeconnect_tp.R
import kotlin.time.Duration.Companion.seconds

@Composable
fun RunCommandScreen(
    plugin: RunCommandPlugin,
    device: Device,
    commandList: SnapshotStateList<CommandEntry>,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onCopyUrlToClipboard: (CommandEntry) -> Unit,
    onUpdate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(plugin) {
        val callback = RunCommandPlugin.CommandsChangedCallback {
            scope.launch(Dispatchers.Main.immediate) {
                onUpdate()
            }
        }
        plugin.addCommandsUpdatedCallback(callback)

        onDispose {
            plugin.removeCommandsUpdatedCallback(callback)
        }
    }

    val isReachable by device.isReachableFlow.collectAsStateWithLifecycle()
    val showOutputCard = commandList.isNotEmpty() &&
        device.canSendPacketType(RunCommandPlugin.PACKET_TYPE_RUNCOMMAND_OUTPUT)
    val commandRunning by plugin.commandRunning

    RunCommandScreenContent(
        subTitle = device.name,
        device = device,
        isReachable = isReachable,
        canAddCommand = plugin.canAddCommand(),
        showOutputCard = showOutputCard,
        commandList = commandList,
        outputList = plugin.output,
        commandRunning = commandRunning,
        onBackPressed = { onBackPressedDispatcher.onBackPressed() },
        onRunCommand = { plugin.runCommand(it) },
        onCopyUrl = onCopyUrlToClipboard,
        onClearOutput = { plugin.clearOutput() },
        onStopCommand = { plugin.sendStop() },
        onAddCommand = {
            plugin.sendSetupPacket()
            showDialog = true
        },
        showDialog = showDialog,
        onDismissDialog = {
            showDialog = false
            onUpdate()
        }
    )
}

@Composable
internal fun RunCommandScreenContent(
    subTitle: String,
    isReachable: Boolean,
    canAddCommand: Boolean,
    showOutputCard: Boolean,
    commandList: List<CommandEntry>,
    outputList: List<RunCommandOutput>,
    commandRunning: Boolean,
    onBackPressed: () -> Unit,
    onRunCommand: (String) -> Unit,
    onCopyUrl: (CommandEntry) -> Unit,
    onClearOutput: () -> Unit,
    onStopCommand: () -> Unit,
    onAddCommand: () -> Unit,
    modifier: Modifier = Modifier,
    device: Device? = null,
    showDialog: Boolean = false,
    onDismissDialog: () -> Unit = {}
) {
    val context = LocalContext.current

    PocketLinkTheme(context) {
        Scaffold(
            modifier = modifier.safeDrawingPadding(),
            topBar = {
                RunCommandAppBar(
                    subTitle = subTitle,
                    onBackPressed = onBackPressed,
                    showClearOutput = showOutputCard,
                    onClearOutput = onClearOutput
                )
            },
            floatingActionButton = {
                if (canAddCommand && isReachable) {
                    val addCommandDesc = stringResource(R.string.add_command)
                    FloatingActionButton(
                        onClick = onAddCommand,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = addCommandDesc
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_image_edit_24dp),
                            contentDescription = addCommandDesc
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = onDismissDialog,
                    shape = MaterialTheme.shapes.extraLarge,
                    title = {
                        Text(
                            stringResource(R.string.add_command),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.add_command_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = onDismissDialog,
                            modifier = Modifier.defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = PocketLinkSpacing.compactContentMaxWidth)
                ) {
                    if (commandList.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = PocketLinkSpacing.medium,
                                bottom = PocketLinkSpacing.xLarge
                            ),
                            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
                        ) {
                            if (showOutputCard) {
                                item {
                                    OutputCard(
                                        outputList = outputList,
                                        commandRunning = commandRunning,
                                        onStopCommand = onStopCommand
                                    )
                                }
                            }
                            items(commandList, key = { it.key }) { command ->
                                CommandCard(
                                    command = command,
                                    onRun = { onRunCommand(command.key) },
                                    onCopyUrl = { onCopyUrl(command) }
                                )
                            }
                        }
                    } else {
                        var explanation = stringResource(R.string.addcommand_explanation)
                        if (!canAddCommand) {
                            explanation += "\n" + stringResource(R.string.addcommand_explanation2)
                        }
                        PocketLinkStatePanel(
                            title = stringResource(R.string.pref_plugin_runcommand),
                            description = explanation,
                            kind = PocketLinkPanelKind.Empty,
                            icon = Icons.Default.PlayArrow,
                            actionLabel = if (canAddCommand && isReachable) stringResource(R.string.add_command) else null,
                            onAction = if (canAddCommand && isReachable) onAddCommand else null,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    device?.let { DeviceDisconnectedOverlay(device = it) }
                }
            }
        }
    }
}

@Composable
private fun CommandCard(
    command: CommandEntry,
    onRun: () -> Unit,
    onCopyUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    PocketLinkStandardCard(
        modifier = modifier
            .padding(horizontal = PocketLinkSpacing.contentHorizontal)
            .semantics {
                contentDescription = "${command.name}: ${command.command}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = PocketLinkComponentSize.deviceCardMinHeight)
                .combinedClickable(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRun,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    }
                )
                .padding(PocketLinkSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_image_edit_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PocketLinkComponentSize.iconLarge)
            )
            Spacer(Modifier.width(PocketLinkSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = command.command,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(PocketLinkComponentSize.controlMinHeight)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.extra_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_url_to_clipboard)) },
                        onClick = {
                            menuExpanded = false
                            onCopyUrl()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputCard(
    outputList: List<RunCommandOutput>,
    commandRunning: Boolean,
    onStopCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showStopButton by remember { mutableStateOf(false) }
    val semanticColors = kdeSemanticColors()

    LaunchedEffect(commandRunning) {
        if (commandRunning) {
            delay(1.seconds)
            showStopButton = true
        } else {
            showStopButton = false
        }
    }

    PocketLinkStandardCard(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = PocketLinkSpacing.contentHorizontal, vertical = PocketLinkSpacing.xSmall)
    ) {
        if (outputList.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PocketLinkSpacing.cardPadding),
                    state = state,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(outputList) { text ->
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = text.string,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (text.isCommand) FontWeight.Bold else FontWeight.Normal,
                            color = when (text.commandStatus) {
                                RunCommandStatus.COMMAND_SUCCESSFUL -> semanticColors.success
                                RunCommandStatus.COMMAND_FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                if (showStopButton) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PocketLinkSpacing.small),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        IconButton(onClick = onStopCommand) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_stop),
                                contentDescription = stringResource(R.string.runcommand_stop),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PocketLinkSpacing.large),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.runcommand_output_no_output),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(PocketLinkSpacing.xSmall))
                Text(
                    text = stringResource(R.string.runcommand_output_no_output_desc),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    LaunchedEffect(outputList.size) {
        if (outputList.isNotEmpty()) {
            coroutineScope.launch {
                state.animateScrollToItem(outputList.size - 1)
            }
        }
    }
}

@Composable
fun RunCommandAppBar(
    subTitle: String,
    onBackPressed: () -> Unit,
    showClearOutput: Boolean,
    onClearOutput: () -> Unit,
) {
    PocketLinkTopAppBar(
        title = stringResource(R.string.pref_plugin_runcommand),
        subTitle = subTitle,
        navIconOnClick = onBackPressed,
        actions = {
            if (showClearOutput) {
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.extra_options),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.runcommand_clear_output)) },
                        onClick = {
                            menuExpanded = false
                            onClearOutput()
                        },
                    )
                }
            }
        },
    )
}
