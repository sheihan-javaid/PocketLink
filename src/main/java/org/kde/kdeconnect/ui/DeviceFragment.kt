/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.kde.kdeconnect.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.activity.OnBackPressedCallback
import androidx.annotation.UiThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.kde.kdeconnect.BackgroundService
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.Device.PluginsChangedListener
import org.kde.kdeconnect.KdeConnect
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.base.BaseFragment
import org.kde.kdeconnect.extensions.setupBottomPadding
import org.kde.kdeconnect.helpers.DeviceHelper
import org.kde.kdeconnect.helpers.security.SslHelper
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.plugins.connectivityreport.ConnectivityReportPlugin
import org.kde.kdeconnect.plugins.PluginFactory
import org.kde.kdeconnect.ui.compose.KdeTheme
import org.kde.kdeconnect.ui.compose.screen.device.ConnectedDeviceDashboard
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect.ui.compose.screen.device.DevicePairingScreen
import org.kde.kdeconnect.ui.compose.screen.device.PairingSuccessBanner
import org.kde.kdeconnect.ui.compose.screen.device.DeviceViewModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernConnectedDeviceDashboard
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardUiStateMapper
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardActions
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionAvailability
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionUiModel
import org.kde.kdeconnect.ui.compose.KdeSpacing
import org.kde.kdeconnect_tp.R
import org.kde.kdeconnect_tp.databinding.ActivityDeviceBinding

/**
 * Main view. Displays the current device and its plugins
 */
class DeviceFragment : BaseFragment<ActivityDeviceBinding>() {

    companion object {
        private const val ARG_DEVICE_ID = "deviceId"
        private const val ARG_FROM_DEVICE_LIST = "fromDeviceList"
        private const val TAG = "KDE/DeviceFragment"
        fun newInstance(deviceId: String, fromDeviceList: Boolean): DeviceFragment {
            val frag = DeviceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEVICE_ID, deviceId)
                    putBoolean(ARG_FROM_DEVICE_LIST, fromDeviceList)
                }
            }
            return frag
        }
    }

    override fun getActionBarTitle() = null

    val deviceId: String by lazy {
        arguments?.getString(ARG_DEVICE_ID)
            ?: throw RuntimeException("You must instantiate a new DeviceFragment using DeviceFragment.newInstance()")
    }

    private val device by lazy { KdeConnect.getInstance().getDevice(deviceId) }

    private val viewModel by viewModels<DeviceViewModel>()

    override fun onInflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ActivityDeviceBinding {
        val fromDeviceList = requireArguments().getBoolean(ARG_FROM_DEVICE_LIST, false)
        if (fromDeviceList) {
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle back button, so we go to the list of devices in case we came from there
                    (mActivity as? MainActivity)?.onDeviceSelected(null)
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), callback)
        }
        return ActivityDeviceBinding.inflate(inflater, container, false)
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menu.clear()
            val device = device ?: return

            //Plugins button list
            val menuEntries: Collection<Plugin.PluginUiMenuEntry> = if (device.isReachable) {
                device.loadedPlugins.values.flatMap { it.getUiMenuEntries() }
            } else {
                emptyList()
            }
            for (p in menuEntries) {
                menu.add(p.name).setOnMenuItemClickListener {
                    p.onClick(mActivity!!)
                    true
                }
            }
            val intent = PluginSettingsActivity.createIntent(mActivity!!, deviceId)
            menu.add(R.string.device_menu_plugins).setOnMenuItemClickListener {
                startActivity(intent)
                true
            }
            if (device.isReachable) {
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle(requireContext().resources.getString(R.string.encryption_info_title))
                builder.setPositiveButton(requireContext().resources.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                builder.setMessage(
                    "${
                        requireContext().resources.getString(R.string.my_device_fingerprint)
                    } \n ${
                        SslHelper.getCertificateHash(SslHelper.certificate)
                    } \n\n ${
                        requireContext().resources.getString(R.string.remote_device_fingerprint)
                    } \n ${
                        SslHelper.getCertificateHash(device.certificate)
                    } \n\n ${
                        requireContext().resources.getString(R.string.protocol_version)
                    } ${
                        device.protocolVersion
                    }"
                )
                menu.add(R.string.encryption_info_title).setOnMenuItemClickListener {
                    builder.show()
                    true
                }
            }
            if (device.isPaired) {
                menu.add(R.string.device_menu_unpair).setOnMenuItemClickListener {
                    device.apply {
                        // Remove listener so buttons don't show for an instant before changing the view
                        removePairingCallback(pairingCallback)
                        removePluginsChangedListener(pluginsChangedListener)
                        unpair()
                    }
                    (mActivity as? MainActivity)?.onDeviceSelected(null)
                    true
                }
            }
            if (device.pairStatus == PairingHandler.PairState.Requested) {
                menu.add(R.string.cancel_pairing).setOnMenuItemClickListener {
                    device.cancelPairing()
                    true
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.deviceView.setupBottomPadding()
        device?.apply {
            mActivity?.supportActionBar?.title = name
            removePairingCallback(pairingCallback)
            addPairingCallback(pairingCallback)
            removePluginsChangedListener(pluginsChangedListener)
            addPluginsChangedListener(pluginsChangedListener)
            viewModel.bindDevice(this)
        } ?: run { // device is null
            Log.e(TAG, "Trying to display a device fragment but the device is not present")
            (mActivity as? MainActivity)?.onDeviceSelected(null)
        }
        mActivity?.addMenuProvider(menuProvider, viewLifecycleOwner)
        refreshUI()
    }

    private fun refreshDevicesAction() {
        BackgroundService.ForceRefreshConnections(requireContext())
        viewModel.setRefreshing(refreshing = true)
        binding.deviceView.postDelayed({
            if (isResumed && !isDetached) { // the view might be destroyed by now
                viewModel.setRefreshing(refreshing = false)
            }
        }, 1500)
    }

    override fun onPause() {
        viewModel.setRefreshing(refreshing = false)
        super.onPause()
    }

    private val pluginsChangedListener =
        PluginsChangedListener { mActivity?.runOnUiThread { refreshUI() } }

    override fun onDestroyView() {
        viewModel.unbindDevice()
        device?.apply {
            removePluginsChangedListener(pluginsChangedListener)
            removePairingCallback(pairingCallback)
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        device?.let { viewModel.bindDevice(it) }
        with(requireView()) {
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    @UiThread
    private fun refreshUI() {
        if (!hasBinding) return // in case onDestroyView has already been called
        val device = device ?: return

        //Once in-app, there is no point in keep displaying the notification if any
        device.hidePairingNotification()

        binding.deviceView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KdeTheme(context) {
                    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                    val pairingErrorMessage by viewModel.pairingErrorMessage.collectAsStateWithLifecycle()
                    val pairingSucceeded by viewModel.pairingSucceeded.collectAsStateWithLifecycle()
                    val useLegacyDashboard by viewModel.useLegacyDashboard.collectAsStateWithLifecycle()
                    val liveMedia by viewModel.mediaPlaybackState.collectAsStateWithLifecycle()
                    val liveVolume by viewModel.volumeControlState.collectAsStateWithLifecycle()

                    when (device.pairStatus) {
                        PairingHandler.PairState.Paired -> {
                            val pluginModels = if (device.isReachable) {
                                PluginFactory.sortPluginList(device.supportedPlugins)
                                    .filter { it != "ReceiveNotificationsPlugin" }
                                    .map { pluginKey ->
                                    val pluginInfo = PluginFactory.getPluginInfo(pluginKey)
                                    DevicePluginUiModel(
                                        pluginKey = pluginKey,
                                        displayName = pluginInfo.displayName,
                                        description = pluginInfo.description,
                                        isEnabled = device.isPluginEnabled(pluginKey),
                                        actionButtons = device.loadedPlugins[pluginKey]
                                            ?.getUiButtons()
                                            .orEmpty(),
                                        needsRequiredPermission =
                                            device.isPluginEnabled(pluginKey) &&
                                                pluginKey in device.pluginsWithoutPermissions,
                                        needsOptionalPermission =
                                            device.isPluginEnabled(pluginKey) &&
                                                pluginKey in device.pluginsWithoutOptionalPermissions
                                    )
                                }
                            } else {
                                emptyList()
                            }
                            val batteryStatus = device.getPlugin<BatteryPlugin>()
                                ?.remoteBatteryInfo
                                ?.let { batteryInfo ->
                                    val statusRes = when {
                                        batteryInfo.isCharging ->
                                            R.string.battery_status_charging_format
                                        BatteryPlugin.isLowBattery(batteryInfo) ->
                                            R.string.battery_status_low_format
                                        else -> R.string.battery_status_format
                                    }
                                    getString(statusRes, batteryInfo.currentCharge)
                                }
                            val connectivityStatus =
                                device.getPlugin<ConnectivityReportPlugin>()
                                    ?.remoteConnectivityInfo
                                    ?.values
                                    ?.map { it.networkType }
                                    ?.filter { it.isNotEmpty() }
                                    ?.distinct()
                                    ?.joinToString(", ")
                                    ?.ifEmpty { null }

                            if (useLegacyDashboard) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    ConnectedDeviceDashboard(
                                        deviceName = device.name,
                                        deviceIconRes = device.iconDrawable,
                                        isConnected = device.isReachable,
                                        plugins = pluginModels,
                                        batteryStatus = batteryStatus,
                                        connectivityStatus = connectivityStatus,
                                        isRefreshing = isRefreshing,
                                        onRefresh = { refreshDevicesAction() },
                                        onPluginEnabledChange = device::setPluginEnabled,
                                        onButtonClick = { button -> button.onClick(mActivity!!) },
                                        onRequiredPermissionClick = { pluginKey ->
                                            device.pluginsWithoutPermissions[pluginKey]
                                                ?.permissionExplanationDialog
                                                ?.show(childFragmentManager, null)
                                        },
                                        onOptionalPermissionClick = { pluginKey ->
                                            device.pluginsWithoutOptionalPermissions[pluginKey]
                                                ?.optionalPermissionExplanationDialog
                                                ?.show(childFragmentManager, null)
                                        }
                                    )
                                    if (pairingSucceeded) {
                                        PairingSuccessBanner(
                                            deviceName = device.name,
                                            onDismiss = { viewModel.setPairingSucceeded(false) },
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(KdeSpacing.medium)
                                        )
                                    }
                                }
                            } else {
                                val modernDashboardState = DashboardUiStateMapper.map(
                                    deviceName = device.name,
                                    deviceIconRes = device.iconDrawable,
                                    isConnected = device.isReachable,
                                    plugins = pluginModels,
                                    batteryPercent = device.getPlugin<BatteryPlugin>()?.remoteBatteryInfo?.currentCharge,
                                    isBatteryCharging = device.getPlugin<BatteryPlugin>()?.remoteBatteryInfo?.isCharging ?: false,
                                    isBatteryLow = device.getPlugin<BatteryPlugin>()?.remoteBatteryInfo?.let { BatteryPlugin.isLowBattery(it) } ?: false,
                                    connectivitySummary = connectivityStatus,
                                    mediaState = liveMedia,
                                    volumeState = liveVolume,
                                    isRefreshing = isRefreshing,
                                    localDeviceName = DeviceHelper.getDeviceName(requireContext())
                                )

                                val modernActions = remember(device) {
                                    object : DashboardActions {
                                        override fun onRefresh() {
                                            refreshDevicesAction()
                                        }

                                        override fun onQuickActionClick(action: QuickActionUiModel) {
                                            when (action.availability) {
                                                is QuickActionAvailability.NeedsPermission -> {
                                                    val pluginKey = when (action.id) {
                                                        "send_files" -> "SharePlugin"
                                                        "clipboard" -> "ClipboardPlugin"
                                                        "remote_input" -> "MousePadPlugin"
                                                        "ring_device" -> "FindRemoteDevicePlugin"
                                                        "run_command" -> "RunCommandPlugin"
                                                        "presenter" -> "PresenterPlugin"
                                                        else -> null
                                                    }
                                                    if (pluginKey != null) {
                                                        device.pluginsWithoutPermissions[pluginKey]?.permissionExplanationDialog?.show(childFragmentManager, null)
                                                            ?: device.pluginsWithoutOptionalPermissions[pluginKey]?.optionalPermissionExplanationDialog?.show(childFragmentManager, null)
                                                    }
                                                }
                                                is QuickActionAvailability.Ready -> {
                                                    action.actionButton?.onClick(mActivity!!)
                                                }
                                                is QuickActionAvailability.Disabled -> {}
                                            }
                                        }

                                        override fun onPluginButtonClick(button: Plugin.PluginUiButton) {
                                            button.onClick(mActivity!!)
                                        }

                                        override fun onPermissionItemClick(pluginKey: String, isRequired: Boolean) {
                                            if (isRequired) {
                                                device.pluginsWithoutPermissions[pluginKey]
                                                    ?.permissionExplanationDialog
                                                    ?.show(childFragmentManager, null)
                                            } else {
                                                device.pluginsWithoutOptionalPermissions[pluginKey]
                                                    ?.optionalPermissionExplanationDialog
                                                    ?.show(childFragmentManager, null)
                                            }
                                        }

                                        override fun onAttentionBannerClick() {}

                                        override fun onManagePluginsClick() {}

                                        override fun onMediaPlayPause() {
                                            viewModel.onMediaPlayPause()
                                        }

                                        override fun onMediaNext() {
                                            viewModel.onMediaNext()
                                        }

                                        override fun onMediaPrevious() {
                                            viewModel.onMediaPrevious()
                                        }

                                        override fun onMediaSeek(positionMs: Long) {
                                            viewModel.onMediaSeek(positionMs)
                                        }

                                        override fun onVolumeChange(newVolume: Int) {
                                            viewModel.onVolumeChange(newVolume)
                                        }
                                    }
                                }

                                ModernConnectedDeviceDashboard(
                                    state = modernDashboardState,
                                    actions = modernActions,
                                    pluginModelsForManagePage = pluginModels,
                                    onPluginEnabledChange = device::setPluginEnabled,
                                    onButtonClick = { button -> button.onClick(mActivity!!) },
                                    onRequiredPermissionClick = { pluginKey ->
                                        device.pluginsWithoutPermissions[pluginKey]
                                            ?.permissionExplanationDialog
                                            ?.show(childFragmentManager, null)
                                    },
                                    onOptionalPermissionClick = { pluginKey ->
                                        device.pluginsWithoutOptionalPermissions[pluginKey]
                                            ?.optionalPermissionExplanationDialog
                                            ?.show(childFragmentManager, null)
                                    },
                                    pairingSucceeded = pairingSucceeded,
                                    onDismissPairingSuccess = { viewModel.setPairingSucceeded(false) }
                                )
                            }
                        }

                        else -> {
                            DevicePairingScreen(
                                deviceName = device.name,
                                pairStatus = device.pairStatus,
                                verificationKey = device.verificationKey ?: "",
                                pairMessage = pairingErrorMessage,
                                onRequestPairing = {
                                    viewModel.setPairingErrorMessage(message =null)
                                    viewModel.setPairingSucceeded(false)
                                    device.requestPairing()
                                    refreshUI()
                                },
                                onAcceptPairing = {
                                    device.acceptPairing()
                                    refreshUI()
                                },
                                onRejectPairing = {
                                    device.apply {
                                        // Remove listener so buttons don't show for an instant before changing the view
                                        removePluginsChangedListener(pluginsChangedListener)
                                        removePairingCallback(pairingCallback)
                                        cancelPairing()
                                    }
                                    (mActivity as? MainActivity)?.onDeviceSelected(null)
                                },
                                onCancelPairing = {
                                    device.cancelPairing()
                                    refreshUI()
                                }
                            )
                        }
                    }
                }
            }
        }
        mActivity?.invalidateOptionsMenu()
    }

    private val pairingCallback: PairingHandler.PairingCallback =
        object : PairingHandler.PairingCallback {
            override fun incomingPairRequest() {
                mActivity?.runOnUiThread { refreshUI() }
            }

            override fun pairingSuccessful() {
                viewModel.setPairingSucceeded(true)
                val accessibilityManager =
                    requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                if (accessibilityManager?.isEnabled == true) {
                    @Suppress("DEPRECATION")
                    val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        AccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                    } else {
                        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                    }
                    event.text.add(getString(R.string.pair_succeeded))
                    accessibilityManager.sendAccessibilityEvent(event)
                }
                mActivity?.runOnUiThread { refreshUI() }
            }

            override fun pairingFailed(error: String) {
                mActivity?.runOnUiThread {
                    viewModel.setPairingErrorMessage(message =error)
                    refreshUI()
                }
            }

            override fun unpaired(device: Device) {
                mActivity?.runOnUiThread {
                    viewModel.setPairingErrorMessage(message =null)
                    viewModel.setPairingSucceeded(false)
                    refreshUI()
                }
            }
        }


}
