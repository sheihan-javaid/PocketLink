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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect.ui.compose.screen.device.DeviceErrorPanel
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
    pairingSucceeded: Boolean = false,
    onDismissPairingSuccess: () -> Unit = {},
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
        var isManagingPlugins by remember { mutableStateOf(false) }

        if (isManagingPlugins) {
            BackHandler { isManagingPlugins = false }
            ManagePluginsPage(
                plugins = pluginModelsForManagePage,
                onBack = { isManagingPlugins = false },
                onPluginEnabledChange = onPluginEnabledChange,
                onButtonClick = onButtonClick,
                onRequiredPermissionClick = onRequiredPermissionClick,
                onOptionalPermissionClick = onOptionalPermissionClick,
                modifier = modifier
            )
        } else {
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(tokens.pageBackground)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(tokens.glow, Color.Transparent),
                                center = Offset(size.width * 0.15f, size.height * 0.1f),
                                radius = size.maxDimension * 0.85f
                            )
                        )
                    }
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
                                    start = KdeSpacing.contentHorizontal,
                                    end = KdeSpacing.contentHorizontal,
                                    top = KdeSpacing.medium + topInset,
                                    bottom = 48.dp + bottomInset
                                ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top: Hero Device Card
                            HeroDeviceCard(hero = state.hero)

                            if (!state.hero.isConnected) {
                                DeviceErrorPanel(onRefresh = actions::onRefresh)
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

                                // Card 1: Share and Transfer
                                val shareSection = state.sections.firstOrNull { it.category == PluginCategory.ShareTransfer }
                                if (shareSection != null && shareSection.rows.isNotEmpty()) {
                                    ServiceSection(
                                        section = shareSection,
                                        onPluginButtonClick = actions::onPluginButtonClick,
                                        onPermissionClick = actions::onPermissionItemClick
                                    )
                                }

                                // Card 2: Control Computer
                                val controlSection = state.sections.firstOrNull { it.category == PluginCategory.ControlComputer }
                                if (controlSection != null && controlSection.rows.isNotEmpty()) {
                                    ServiceSection(
                                        section = controlSection,
                                        onPluginButtonClick = actions::onPluginButtonClick,
                                        onPermissionClick = actions::onPermissionItemClick
                                    )
                                }

                                // Card 3: Media and Audio (integrating contextual media controls & volume slider)
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

                                // Card 4: Device Information
                                val deviceSection = state.sections.firstOrNull { it.category == PluginCategory.DeviceInfo }
                                if (deviceSection != null && deviceSection.rows.isNotEmpty()) {
                                    ServiceSection(
                                        section = deviceSection,
                                        onPluginButtonClick = actions::onPluginButtonClick,
                                        onPermissionClick = actions::onPermissionItemClick
                                    )
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

                // Top Status-Bar Scrim/Fade so scrolling content never collides with status bar icons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topInset + 14.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to tokens.pageBackground,
                                0.7f to tokens.pageBackground.copy(alpha = 0.85f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                // Pairing Success Banner (Animated top overlay when pairing succeeds)
                if (pairingSucceeded) {
                    PairingSuccessBanner(
                        deviceName = state.hero.deviceName,
                        onDismiss = onDismissPairingSuccess,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topInset + KdeSpacing.medium, start = KdeSpacing.medium, end = KdeSpacing.medium)
                    )
                }
            }
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
    modifier: Modifier = Modifier
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

    // Sticky header: draw the page glow behind BOTH header and list (one background layer, no seam),
    // with the header transparent over it and a ~24dp fade matching background color at that y.
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(tokens.pageBackground)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.glow, Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.1f),
                        radius = size.maxDimension * 0.85f
                    )
                )
            }
    ) {
        // Sticky header content height: status bar + top app bar (56dp) + compact search field (48dp)
        val headerContentHeight = topInset + 104.dp

        // 1. Scrollable Content Layer (underneath the sticky header and 20dp fade)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startY = headerContentHeight.toPx(),
                            endY = (headerContentHeight + 20.dp).toPx()
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(
                        start = KdeSpacing.contentHorizontal,
                        end = KdeSpacing.contentHorizontal,
                        top = headerContentHeight + 10.dp,
                        bottom = 48.dp + navBottomInset
                    ),
                verticalArrangement = Arrangement.spacedBy(KdeSpacing.medium)
            ) {
                // Summary card keeping "N of 23 enabled"
                AuroraGlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            verticalArrangement = Arrangement.spacedBy(KdeSpacing.small)
                        ) {
                            // Category Header with count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = KdeSpacing.small, vertical = 2.dp),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.semantics { heading() }
                                )
                                Text(
                                    text = stringResource(R.string.category_plugins_count_format, categoryPlugins.size),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                                        onOptionalPermissionClick = { onOptionalPermissionClick(plugin.pluginKey) }
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

        // 2. Sticky Header Layer (on top, aligned to TopCenter, transparent over page glow)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_plugins_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.manage_plugins_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Compact Manage Plugins Search Bar (42dp pill, refined purple/violet focus border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KdeSpacing.contentHorizontal, vertical = 2.dp)
            ) {
                var isSearchFocused by remember { mutableStateOf(false) }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = tokens.textPrimary
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(tokens.cardBackground)
                                .border(
                                    width = if (isSearchFocused) 1.5.dp else 1.dp,
                                    color = if (isSearchFocused) MaterialTheme.colorScheme.primary else tokens.cardBorderBottom,
                                    shape = RoundedCornerShape(21.dp)
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.manage_plugins_search_description),
                                tint = if (isSearchFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.manage_plugins_search_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textSecondary.copy(alpha = 0.65f)
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.manage_plugins_clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // ~24dp fade matching background color at that y so rows dissolve seamlessly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(tokens.pageBackground, Color.Transparent)
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
    "FindRemoteDevicePlugin" -> R.drawable.ic_modern_bell
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
    modifier: Modifier = Modifier
) {
    val tokens = LocalDashboardTokens.current
    val iconRes = getPluginIconRes(plugin.pluginKey)
    val (iconBg, iconTint) = when (category) {
        PluginCategory.ShareTransfer -> Pair(tokens.shareContainer, tokens.onShareContainer)
        PluginCategory.ControlComputer -> Pair(tokens.controlContainer, tokens.onControlContainer)
        PluginCategory.MediaAudio -> Pair(tokens.mediaContainer, tokens.onMediaContainer)
        PluginCategory.DeviceInfo -> Pair(tokens.deviceContainer, tokens.onDeviceContainer)
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

                Spacer(modifier = Modifier.width(KdeSpacing.medium))

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

            Spacer(modifier = Modifier.width(KdeSpacing.small))

            // Switch toggle
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = onEnabledChange
            )
        }

        // Tonal action buttons if available
        if (plugin.actionButtons.isNotEmpty() || plugin.needsRequiredPermission || plugin.needsOptionalPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(KdeSpacing.small),
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
                            containerColor = tokens.shareContainer,
                            contentColor = tokens.onShareContainer
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
