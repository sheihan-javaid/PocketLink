/*
 * SPDX-FileCopyrightText: 2026 KDE Connect contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device.modern

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kde.kdeconnect.DeviceStats
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.PocketLinkSpacing
import org.kde.kdeconnect.ui.compose.components.PocketLinkPanelKind
import org.kde.kdeconnect.ui.compose.components.PocketLinkStatePanel
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.DeviceErrorPanel
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect.ui.compose.screen.device.PairingSuccessBanner
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.AttentionBanner
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.AuroraGlassCard
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.HeroDeviceCard
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.ManagePluginsRow
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.QuickActionsGrid
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.ServiceSection
import org.kde.kdeconnect.ui.compose.screen.device.modern.components.isDarkThemeMode
import org.kde.kdeconnect_tp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernConnectedDeviceDashboard(
    state: DashboardUiState,
    actions: DashboardActions,
    pluginModelsForManagePage: List<DevicePluginUiModel>,
    onPluginEnabledChange: (pluginKey: String, enabled: Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: (pluginKey: String) -> Unit,
    onOptionalPermissionClick: (pluginKey: String) -> Unit,
    onPluginSettingsClick: (pluginKey: String) -> Unit = {},
    deviceId: String = "",
    onPluginStatsClick: () -> Unit = {},
    pairingSucceeded: Boolean = false,
    onDismissPairingSuccess: () -> Unit = {},
    onManagingPluginsChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isDarkThemeMode()
    val colorScheme = MaterialTheme.colorScheme
    val tokens = remember(isDark, colorScheme) {
        createDashboardTokens(colorScheme, isDark)
    }
    val tier = remember(context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isPowerSave = powerManager?.isPowerSaveMode == true
        val isLowRam = activityManager?.isLowRamDevice == true
        val animatorScale = try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (e: Exception) { 1f }
        resolveEffectsTier(Build.VERSION.SDK_INT, isPowerSave, animatorScale, isLowRam)
    }

    CompositionLocalProvider(
        LocalDashboardTokens provides tokens,
        LocalEffectsTier provides tier
    ) {
        var isManagingPlugins by rememberSaveable { mutableStateOf(false) }
        var showStatsDialog by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(isManagingPlugins) {
            onManagingPluginsChange(isManagingPlugins)
        }

        if (isManagingPlugins) {
            BackHandler { isManagingPlugins = false }
            ManagePluginsPage(
                plugins = pluginModelsForManagePage,
                onBack = { isManagingPlugins = false },
                onPluginEnabledChange = onPluginEnabledChange,
                onButtonClick = onButtonClick,
                onRequiredPermissionClick = onRequiredPermissionClick,
                onOptionalPermissionClick = onOptionalPermissionClick,
                onPluginSettingsClick = onPluginSettingsClick,
                onStatsClick = {
                    showStatsDialog = true
                    onPluginStatsClick()
                },
                modifier = modifier
            )
        } else {
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                tokens.backgroundGradientTop,
                                tokens.backgroundGradientMid,
                                tokens.backgroundGradientBottom
                            )
                        )
                    )
            ) {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = actions::onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                                .padding(
                                    start = PocketLinkSpacing.contentHorizontal,
                                    end = PocketLinkSpacing.contentHorizontal,
                                    top = 10.dp,
                                    bottom = 32.dp + bottomInset
                                ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top: Hero Device Card
                            HeroDeviceCard(
                                hero = state.hero,
                                desktopPreview = state.desktopPreview,
                                onPreviewEnableClick = { isManagingPlugins = true }
                            )

                            if (!state.hero.isConnected) {
                                DeviceErrorPanel(onRefresh = actions::onRefresh)
                            } else {
                                val allSectionsEmpty = state.sections.all { it.rows.isEmpty() }

                                if (allSectionsEmpty) {
                                    PocketLinkStatePanel(
                                        title = stringResource(R.string.device_no_supported_plugins_title),
                                        description = stringResource(R.string.device_no_supported_plugins),
                                        kind = PocketLinkPanelKind.Unsupported
                                    )
                                } else {
                                    // Attention Banner (Consolidated pending permission alert)
                                    AnimatedVisibility(
                                        visible = state.attention != null,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        state.attention?.let { attentionState ->
                                            AttentionBanner(
                                                attention = attentionState,
                                                onClick = {
                                                    actions.onAttentionBannerClick()
                                                    isManagingPlugins = true
                                                }
                                            )
                                        }
                                    }

                                    // Quick Actions Grid
                                    if (state.quickActions.isNotEmpty()) {
                                        QuickActionsGrid(
                                            actions = state.quickActions,
                                            onActionClick = actions::onQuickActionClick
                                        )
                                    }

                                    // -------------------------------------------------------------
                                    // One UI-Style 5 Grouped Cards Layout
                                    // -------------------------------------------------------------

                                    // Card 1: Media and Audio (integrating contextual media controls & volume slider)
                                    val mediaSection = state.sections.firstOrNull { it.category == PluginCategory.MediaAudio }
                                        ?: ServiceSectionUiModel(
                                            category = PluginCategory.MediaAudio,
                                            title = PluginCategoryRegistry.categoryTitle(PluginCategory.MediaAudio),
                                            rows = emptyList(),
                                            titleRes = PluginCategory.MediaAudio.displayNameRes
                                        )
                                    ServiceSection(
                                        section = mediaSection,
                                        onPluginButtonClick = actions::onPluginButtonClick,
                                        onPermissionClick = actions::onPermissionItemClick,
                                        mediaState = state.media,
                                        volumeState = state.volume,
                                        onMediaPlayPause = actions::onMediaPlayPause,
                                        onMediaNext = actions::onMediaNext,
                                        onMediaPrevious = actions::onMediaPrevious,
                                        onMediaSeek = actions::onMediaSeek,
                                        onVolumeChange = actions::onVolumeChange,
                                        onVolumeToggleMute = actions::onVolumeToggleMute
                                    )

                                    // Card 2: Share and Transfer
                                    val shareSection = state.sections.firstOrNull { it.category == PluginCategory.ShareTransfer }
                                    if (shareSection != null && shareSection.rows.isNotEmpty()) {
                                        ServiceSection(
                                            section = shareSection,
                                            onPluginButtonClick = actions::onPluginButtonClick,
                                            onPermissionClick = actions::onPermissionItemClick
                                        )
                                    }

                                    // Card 3: Control Computer
                                    val controlSection = state.sections.firstOrNull { it.category == PluginCategory.ControlComputer }
                                    if (controlSection != null && controlSection.rows.isNotEmpty()) {
                                        ServiceSection(
                                            section = controlSection,
                                            onPluginButtonClick = actions::onPluginButtonClick,
                                            onPermissionClick = actions::onPermissionItemClick
                                        )
                                    }

                                    // Card 4: Device Information
                                    val deviceSection = state.sections.firstOrNull { it.category == PluginCategory.DeviceInfo }
                                    if (deviceSection != null && deviceSection.rows.isNotEmpty()) {
                                        ServiceSection(
                                            section = deviceSection,
                                            onPluginButtonClick = actions::onPluginButtonClick,
                                            onPermissionClick = actions::onPermissionItemClick
                                        )
                                    }
                                }

                                // Card 5: Manage Plugins (summary row with chevron opening management page)
                                ManagePluginsRow(
                                    summary = state.managePluginsSummary,
                                    onClick = {
                                        actions.onManagePluginsClick()
                                        isManagingPlugins = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Pairing Success Banner (Animated top overlay when pairing succeeds)
                if (pairingSucceeded) {
                    PairingSuccessBanner(
                        deviceName = state.hero.deviceName,
                        onDismiss = onDismissPairingSuccess,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = PocketLinkSpacing.medium, end = PocketLinkSpacing.medium)
                    )
                }
            }
        }

        if (showStatsDialog) {
            val stats = remember(deviceId) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && deviceId.isNotEmpty()) {
                    DeviceStats.getStatsForDevice(deviceId)
                } else {
                    ""
                }
            }
            val displayText = stats.ifBlank {
                stringResource(R.string.plugin_stats_empty)
            }

            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.plugin_stats),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    SelectionContainer {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatsDialog = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePluginsPage(
    plugins: List<DevicePluginUiModel>,
    onBack: () -> Unit,
    onPluginEnabledChange: (pluginKey: String, enabled: Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: (pluginKey: String) -> Unit,
    onOptionalPermissionClick: (pluginKey: String) -> Unit,
    modifier: Modifier = Modifier,
    onPluginSettingsClick: (pluginKey: String) -> Unit = {},
    onStatsClick: () -> Unit = {}
) {
    val isDark = isDarkThemeMode()
    var searchQuery by remember { mutableStateOf("") }
    val filteredPlugins = remember(plugins, searchQuery) {
        if (searchQuery.isBlank()) {
            plugins
        } else {
            val query = searchQuery.trim().lowercase()
            plugins.filter {
                it.displayName.lowercase().contains(query) ||
                    it.description.lowercase().contains(query)
            }
        }
    }

    val enabledCount = plugins.count { it.isEnabled }
    val totalCount = plugins.size
    val disabledCount = totalCount - enabledCount
    val pendingPermissionsCount = plugins.count { it.needsRequiredPermission || it.needsOptionalPermission }

    val categories = listOf(
        PluginCategory.ShareTransfer,
        PluginCategory.ControlComputer,
        PluginCategory.MediaAudio,
        PluginCategory.DeviceInfo
    )

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tokens = LocalDashboardTokens.current

    // Manage Plugins page uses full screen gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tokens.backgroundGradientTop,
                        tokens.backgroundGradientMid,
                        tokens.backgroundGradientBottom
                    )
                )
            )
    ) {
        // Sticky header content height: topInset + Back title row (~44dp) + Search field (~52dp) + bottom buffer (~6dp)
        val headerContentHeight = topInset + 102.dp

        // 1. Scrollable Content Layer (underneath sticky header)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(
                        start = PocketLinkSpacing.contentHorizontal,
                        end = PocketLinkSpacing.contentHorizontal,
                        top = headerContentHeight + 8.dp,
                        bottom = 48.dp + navBottomInset
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary card keeping "N of 23 enabled"
                AuroraGlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.manage_plugins_summary_with_disabled,
                                enabledCount,
                                totalCount,
                                disabledCount
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            ),
                            color = tokens.textSecondary
                        )
                        if (pendingPermissionsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tokens.permissionBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.attention_pending_count, pendingPermissionsCount),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.permissionBadgeText
                                )
                            }
                        }
                    }
                }

                // Empty search results state
                if (filteredPlugins.isEmpty() && searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.manage_plugins_no_results, searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    }
                }

                // Categorized Plugin Sections
                categories.forEach { category ->
                    val categoryPlugins = filteredPlugins.filter {
                        PluginCategoryRegistry.categoryFor(it.pluginKey) == category
                    }
                    if (categoryPlugins.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small)
                        ) {
                            // Category Header with count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PocketLinkSpacing.small, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = (category.displayNameRes?.let { stringResource(it) }
                                        ?: PluginCategoryRegistry.categoryTitle(category)).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = tokens.sectionHeaderLabel,
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = stringResource(R.string.category_plugins_count_format, categoryPlugins.size),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = tokens.sectionHeaderCount
                                )
                            }

                            AuroraGlassCard {
                                categoryPlugins.forEachIndexed { index, plugin ->
                                    CompactManagePluginRow(
                                        plugin = plugin,
                                        category = category,
                                        onEnabledChange = { onPluginEnabledChange(plugin.pluginKey, it) },
                                        onButtonClick = onButtonClick,
                                        onRequiredPermissionClick = { onRequiredPermissionClick(plugin.pluginKey) },
                                        onOptionalPermissionClick = { onOptionalPermissionClick(plugin.pluginKey) },
                                        onSettingsClick = { onPluginSettingsClick(plugin.pluginKey) }
                                    )
                                    if (index < categoryPlugins.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                                            thickness = 0.5.dp,
                                            color = tokens.divider
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Sticky Header Layer (on top, aligned to TopCenter, compact layout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(tokens.backgroundGradientTop)
        ) {
            // Compact Header Row (replaces tall TopAppBar, slightly reduced heading size)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset + 4.dp, start = 8.dp, end = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.manage_plugins_back),
                        tint = tokens.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.manage_plugins_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onStatsClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.plugin_stats),
                        tint = tokens.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Compact Manage Plugins Search Bar (44dp pill, refined focus border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PocketLinkSpacing.contentHorizontal, vertical = 2.dp)
            ) {
                var isSearchFocused by remember { mutableStateOf(false) }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = tokens.searchBarText
                    ),
                    cursorBrush = SolidColor(tokens.searchBarFocusRing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(tokens.searchBarBg)
                                .border(
                                    width = if (isSearchFocused) 1.5.dp else 1.dp,
                                    color = if (isSearchFocused) tokens.searchBarFocusRing else tokens.searchBarBorder,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.manage_plugins_search_description),
                                tint = if (isSearchFocused) tokens.searchBarFocusRing else tokens.searchBarPlaceholder,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.manage_plugins_search_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.searchBarPlaceholder
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.manage_plugins_clear_search),
                                        tint = tokens.searchBarPlaceholder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Subtle 6dp fade to dissolve rows cleanly without hard cutoffs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(tokens.backgroundGradientTop, Color.Transparent)
                        )
                    )
            )
        }
    }
}

private fun getPluginIconRes(pluginKey: String): Int = when (pluginKey) {
    // Share & Transfer
    "SharePlugin" -> R.drawable.share_plugin_action_24dp
    "ClipboardPlugin" -> R.drawable.ic_baseline_content_paste_24
    "SftpPlugin" -> R.drawable.ic_modern_folder

    // Control Computer
    "MousePadPlugin" -> R.drawable.touchpad_plugin_action_24dp
    "RunCommandPlugin" -> R.drawable.run_command_plugin_icon_24dp
    "PresenterPlugin" -> R.drawable.ic_presenter_24dp
    "FindRemoteDevicePlugin" -> R.drawable.ic_bell_24dp
    "RemoteKeyboardPlugin" -> R.drawable.ic_action_keyboard_24dp
    "RemoteKeyboardIMEPlugin" -> R.drawable.ic_keyboard_return_black_24dp
    "DigitizerPlugin" -> R.drawable.ic_draw_24dp
    "LockDevicePlugin" -> R.drawable.ic_key

    // Media & Audio
    "MprisPlugin" -> R.drawable.ic_modern_album_art
    "SystemVolumePlugin" -> R.drawable.ic_modern_volume
    "MprisReceiverPlugin" -> R.drawable.mpris_plugin_action_24dp

    // Device Information & Connectivity
    "BatteryPlugin" -> R.drawable.ic_device_laptop_32dp
    "ConnectivityReportPlugin" -> R.drawable.ic_wifi
    "NotificationsPlugin" -> R.drawable.ic_modern_notifications
    "ContactsPlugin" -> R.drawable.ic_modern_contacts
    "FindMyPhonePlugin" -> R.drawable.ic_modern_phone_ring
    "InputDevicesReceiverPlugin" -> R.drawable.ic_modern_gamepad
    "MouseReceiverPlugin" -> R.drawable.ic_modern_mouse
    "PingPlugin" -> R.drawable.ic_baseline_send_24
    "SMSPlugin", "SmsPlugin" -> R.drawable.ic_baseline_sms_24
    "TelephonyPlugin" -> R.drawable.ic_device_phone_32dp
    else -> R.drawable.ic_settings_24dp
}

@Composable
private fun CompactManagePluginRow(
    plugin: DevicePluginUiModel,
    category: PluginCategory,
    onEnabledChange: (Boolean) -> Unit,
    onButtonClick: (Plugin.PluginUiButton) -> Unit,
    onRequiredPermissionClick: () -> Unit,
    onOptionalPermissionClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    val tokens = LocalDashboardTokens.current
    val iconRes = getPluginIconRes(plugin.pluginKey)
    val (iconBg, iconTint) = when (category) {
        PluginCategory.ShareTransfer -> Pair(tokens.rowShareBg, tokens.rowShareIcon)
        PluginCategory.ControlComputer -> Pair(tokens.rowControlBg, tokens.rowControlIcon)
        PluginCategory.MediaAudio -> Pair(tokens.mediaContainer, tokens.onMediaContainer)
        PluginCategory.DeviceInfo -> Pair(tokens.rowDeviceInfoBg, tokens.rowDeviceInfoIcon)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                // Leading 40dp icon in tinted circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconTint),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(PocketLinkSpacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(PocketLinkSpacing.small))

            if (plugin.isEnabled && plugin.hasSettings) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_24dp),
                        contentDescription = stringResource(R.string.manage_plugins_settings),
                        tint = tokens.textSecondary
                    )
                }
            }

            // Switch toggle styled with exact tokens
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.toggleThumbOn,
                    checkedTrackColor = tokens.toggleTrackOn,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = tokens.toggleThumbOff,
                    uncheckedTrackColor = tokens.toggleTrackOff,
                    uncheckedBorderColor = tokens.toggleBorderOff
                )
            )
        }

        // Tonal action buttons if available
        if (plugin.actionButtons.isNotEmpty() || plugin.needsRequiredPermission || plugin.needsOptionalPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(PocketLinkSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (plugin.needsRequiredPermission) {
                    FilledTonalButton(
                        onClick = onRequiredPermissionClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = tokens.permissionBadgeBg,
                            contentColor = tokens.permissionBadgeText
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.manage_plugins_grant_permission),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                if (plugin.needsOptionalPermission) {
                    FilledTonalButton(
                        onClick = onOptionalPermissionClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = tokens.optionalBadgeBg,
                            contentColor = tokens.optionalBadgeText
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.manage_plugins_optional_permission),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                plugin.actionButtons.forEach { button ->
                    FilledTonalButton(
                        onClick = { onButtonClick(button) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = tokens.actionPillBg,
                            contentColor = tokens.actionPillText
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(button.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}
