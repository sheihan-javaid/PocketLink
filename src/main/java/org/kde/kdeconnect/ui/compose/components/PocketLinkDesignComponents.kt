/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.kde.kdeconnect.ui.compose.PocketLinkComponentSize
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.kdeSemanticColors

@Composable
fun PocketLinkStandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = content
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = content
        )
    }
}

@Composable
fun PocketLinkDeviceCard(
    name: String,
    status: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    PocketLinkStandardCard(
        modifier = modifier.semantics { role = Role.Button },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = PocketLinkComponentSize.deviceCardMinHeight)
                .padding(PocketLinkSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PocketLinkComponentSize.iconLarge)
                )
                Spacer(Modifier.width(PocketLinkSpacing.medium))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PocketLinkPluginActionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = PocketLinkComponentSize.actionCardMinHeight),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PocketLinkSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

enum class PocketLinkStatusKind {
    Connected,
    Pending,
    Warning,
    Error,
    Neutral
}

enum class PocketLinkPanelKind {
    Empty,
    Error,
    Offline,
    Unsupported
}

@Composable
fun PocketLinkConnectionStatus(
    label: String,
    kind: PocketLinkStatusKind,
    modifier: Modifier = Modifier
) {
    val semanticColors = kdeSemanticColors()
    val color = when (kind) {
        PocketLinkStatusKind.Connected -> semanticColors.success
        PocketLinkStatusKind.Pending, PocketLinkStatusKind.Warning -> semanticColors.warning
        PocketLinkStatusKind.Error -> MaterialTheme.colorScheme.error
        PocketLinkStatusKind.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = label
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(PocketLinkSpacing.small))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun PocketLinkPermissionStatusRow(
    title: String,
    explanation: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    optionalLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            .padding(horizontal = PocketLinkSpacing.medium, vertical = PocketLinkSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(explanation, style = MaterialTheme.typography.bodySmall)
            if (!required && optionalLabel != null) {
                Text(
                    optionalLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(PocketLinkSpacing.small))
        TextButton(
            onClick = onAction,
            modifier = Modifier.defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
fun PocketLinkStatePanel(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    kind: PocketLinkPanelKind = PocketLinkPanelKind.Empty,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val semanticColors = kdeSemanticColors()
    val iconTint = when (kind) {
        PocketLinkPanelKind.Empty -> MaterialTheme.colorScheme.primary
        PocketLinkPanelKind.Error -> MaterialTheme.colorScheme.error
        PocketLinkPanelKind.Offline -> semanticColors.warning
        PocketLinkPanelKind.Unsupported -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PocketLinkSpacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.medium)
    ) {
        Icon(
            icon ?: Icons.Default.Info,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(PocketLinkComponentSize.iconFeatured)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun PocketLinkLoadingState(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(PocketLinkSpacing.medium))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PocketLinkLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (label != null) contentDescription = label }
        )
    }
}

@Composable
fun PocketLinkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun PocketLinkSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            .padding(horizontal = PocketLinkSpacing.medium, vertical = PocketLinkSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClick == null) Modifier
                    else Modifier.clickable(role = Role.Button, onClick = onClick)
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailingContent?.let {
            Spacer(Modifier.width(PocketLinkSpacing.medium))
            it()
        }
    }
}

@Composable
fun PocketLinkConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            ) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = PocketLinkSpacing.minTouchTarget)
            ) {
                Text(dismissLabel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketLinkBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        content = content
    )
}

@Composable
fun PocketLinkSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier)
}
