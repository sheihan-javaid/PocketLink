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
import androidx.compose.foundation.layout.padding
import androidx.activity.OnBackPressedCallback
import androidx.annotation.UiThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.kde.kdeconnect.BackgroundService
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.Device.PluginsChangedListener
import org.kde.kdeconnect.PocketLink
import org.kde.kdeconnect.PairingHandler
import org.kde.kdeconnect.base.BaseFragment
import org.kde.kdeconnect.extensions.setupBottomPadding
import org.kde.kdeconnect.helpers.DeviceHelper
import org.kde.kdeconnect.helpers.security.SslHelper
import org.kde.kdeconnect.plugins.Plugin
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.ui.compose.PocketLinkTheme
import org.kde.kdeconnect.ui.compose.screen.device.DevicePluginUiModel
import org.kde.kdeconnect.ui.compose.screen.device.DevicePairingScreen
import org.kde.kdeconnect.ui.compose.screen.device.DeviceViewModel
import org.kde.kdeconnect.ui.compose.screen.device.modern.ModernConnectedDeviceDashboard
import org.kde.kdeconnect.ui.compose.screen.device.modern.DashboardActions
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionAvailability
import org.kde.kdeconnect.ui.compose.screen.device.modern.QuickActionUiModel
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

    private val device by lazy { PocketLink.getInstance().getDevice(deviceId) }

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
                    activity?.let { act -> p.onClick(act) }
                    true
                }
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
            viewModel.bindDevice(this, DeviceHelper.getDeviceName(requireContext()))
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
        PluginsChangedListener {
            mActivity?.runOnUiThread {
                viewModel.updateConnectivityState()
                refreshUI()
            }
        }

    override fun onDestroyView() {
        (mActivity as? MainActivity)?.findViewById<View>(R.id.toolbar_layout)?.visibility = View.VISIBLE
        viewModel.unbindDevice()
        device?.apply {
            removePluginsChangedListener(pluginsChangedListener)
            removePairingCallback(pairingCallback)
        }
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        device?.let { viewModel.bindDevice(it, context?.let { ctx -> DeviceHelper.getDeviceName(ctx) }) }
        with(requireView()) {
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    @UiThread
    private fun refreshUI() {
        if (!hasBinding) return // in case onDestroyView has already been called
        val device = device ?: return

        viewModel.bindDevice(device, context?.let { ctx -> DeviceHelper.getDeviceName(ctx) })

        //Once in-app, there is no point in keep displaying the notification if any
        device.hidePairingNotification()

        binding.deviceView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PocketLinkTheme(context) {
                    val pairingErrorMessage by viewModel.pairingErrorMessage.collectAsStateWithLifecycle()
                    val pairingSucceeded by viewModel.pairingSucceeded.collectAsStateWithLifecycle()
                    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
                    val pluginModels by viewModel.pluginModels.collectAsStateWithLifecycle()

                    when (device.pairStatus) {
                        PairingHandler.PairState.Paired -> {
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
                                                activity?.let { act -> action.actionButton?.onClick(act) }
                                            }
                                            is QuickActionAvailability.Disabled -> {}
                                        }
                                    }

                                    override fun onPluginButtonClick(button: Plugin.PluginUiButton) {
                                        activity?.let { act -> button.onClick(act) }
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

                                    override fun onVolumeToggleMute() {
                                        viewModel.onVolumeToggleMute()
                                    }
                                }
                            }

                            ModernConnectedDeviceDashboard(
                                state = dashboardState,
                                actions = modernActions,
                                pluginModelsForManagePage = pluginModels,
                                deviceId = device.deviceId,
                                onPluginEnabledChange = device::setPluginEnabled,
                                onButtonClick = { button -> activity?.let { act -> button.onClick(act) } },
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
                                onPluginSettingsClick = { pluginKey ->
                                    context?.let { ctx ->
                                        ctx.startActivity(
                                            PluginSettingsActivity.createIntent(ctx, device.deviceId, pluginKey)
                                        )
                                    }
                                },
                                pairingSucceeded = pairingSucceeded,
                                onDismissPairingSuccess = { viewModel.setPairingSucceeded(false) },
                                onManagingPluginsChange = { isManaging ->
                                    (mActivity as? MainActivity)?.findViewById<View>(R.id.toolbar_layout)?.visibility =
                                        if (isManaging) View.GONE else View.VISIBLE
                                }
                            )
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
                    viewModel.unbindDevice()
                    viewModel.setPairingErrorMessage(message = null)
                    viewModel.setPairingSucceeded(false)
                    refreshUI()
                }
            }
        }


}
