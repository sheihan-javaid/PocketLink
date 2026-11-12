/*
 * SPDX-FileCopyrightText: 2026 Saul Cintero Chocarro <scintero@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.ui.compose.screen.device

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kde.kdeconnect.Device
import org.kde.kdeconnect.plugins.battery.BatteryPlugin
import org.kde.kdeconnect.plugins.battery.DeviceBatteryInfo
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun flowEmitsInitialSensibleState() = runTest {
        val viewModel = DeviceViewModel(backgroundDispatcher = testDispatcher)
        val state = viewModel.dashboardUiState.value

        assertEquals("", state.hero.deviceName)
        assertFalse(state.hero.isConnected)
        assertNull(state.hero.battery)
        assertEquals(0, state.managePluginsSummary.totalCount)
        assertEquals(0, state.managePluginsSummary.enabledCount)
        assertTrue(state.sections.isEmpty())
        assertFalse(state.isRefreshing)
    }

    @Test
    fun updatesOnBatteryChange() = runTest {
        val viewModel = DeviceViewModel(backgroundDispatcher = testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardUiState.collect()
        }

        viewModel.updateBattery(percent = 85, isCharging = true, isLow = false)
        var state = viewModel.dashboardUiState.value

        assertNotNull(state.hero.battery)
        assertEquals(85, state.hero.battery?.percent)
        assertTrue(state.hero.battery?.isCharging == true)
        assertFalse(state.hero.battery?.isLow == true)

        viewModel.updateBattery(percent = 15, isCharging = false, isLow = true)
        state = viewModel.dashboardUiState.value

        assertNotNull(state.hero.battery)
        assertEquals(15, state.hero.battery?.percent)
        assertFalse(state.hero.battery?.isCharging == true)
        assertTrue(state.hero.battery?.isLow == true)
    }

    @Test
    fun updatesOnConnectionChange() = runTest {
        val viewModel = DeviceViewModel(backgroundDispatcher = testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardUiState.collect()
        }

        viewModel.updateConnection(isConnected = true)
        var state = viewModel.dashboardUiState.value
        assertTrue(state.hero.isConnected)

        viewModel.updateConnection(isConnected = false)
        state = viewModel.dashboardUiState.value
        assertFalse(state.hero.isConnected)
    }

    @Test
    fun correctlyReflectsHasSettingsFromMetadataProvider() = runTest {
        val testProvider = object : PluginMetadataProvider {
            override val availablePlugins: Set<String> = setOf("SharePlugin", "MousePadPlugin", "PingPlugin")
            override fun getDisplayName(pluginKey: String): String = when (pluginKey) {
                "SharePlugin" -> "Share and Receive"
                "MousePadPlugin" -> "Touchpad"
                else -> "Ping"
            }
            override fun getDescription(pluginKey: String): String = "$pluginKey description"
            override fun hasSettings(pluginKey: String): Boolean = when (pluginKey) {
                "SharePlugin" -> true
                "MousePadPlugin" -> true
                "PingPlugin" -> false
                else -> false
            }
            override fun sortPluginList(plugins: Collection<String>): List<String> = plugins.sorted()
        }

        val viewModel = DeviceViewModel(
            pluginMetadataProvider = testProvider,
            backgroundDispatcher = testDispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardUiState.collect()
        }

        val mockDevice = mockk<Device>(relaxed = true)
        val isReachableFlow = MutableStateFlow(true)
        every { mockDevice.getPlugin(any<String>()) } answers {
            mockDevice.loadedPlugins[firstArg<String>()]
        }
        every { mockDevice.deviceId } returns "mock-dev-123"
        every { mockDevice.name } returns "Companion Laptop"
        every { mockDevice.iconDrawable } returns 0
        every { mockDevice.isReachable } returns true
        every { mockDevice.isReachableFlow } returns isReachableFlow
        every { mockDevice.supportedPlugins } returns setOf("SharePlugin", "MousePadPlugin", "PingPlugin")
        every { mockDevice.isPluginEnabled("SharePlugin") } returns true
        every { mockDevice.isPluginEnabled("MousePadPlugin") } returns true
        every { mockDevice.isPluginEnabled("PingPlugin") } returns true
        every { mockDevice.loadedPlugins } returns ConcurrentHashMap()
        every { mockDevice.pluginsWithoutPermissions } returns ConcurrentHashMap()
        every { mockDevice.pluginsWithoutOptionalPermissions } returns ConcurrentHashMap()

        viewModel.bindDevice(mockDevice)

        val pluginModels = viewModel.pluginModels.value
        assertEquals(3, pluginModels.size)

        val sharePlugin = pluginModels.first { it.pluginKey == "SharePlugin" }
        assertTrue("SharePlugin should have hasSettings=true", sharePlugin.hasSettings)

        val mousePadPlugin = pluginModels.first { it.pluginKey == "MousePadPlugin" }
        assertTrue("MousePadPlugin should have hasSettings=true", mousePadPlugin.hasSettings)

        val pingPlugin = pluginModels.first { it.pluginKey == "PingPlugin" }
        assertFalse("PingPlugin should have hasSettings=false", pingPlugin.hasSettings)
    }

    @Test
    fun bindDevicePropagatesBatteryAndConnectionFromDevice() = runTest {
        val testProvider = object : PluginMetadataProvider {
            override val availablePlugins: Set<String> = emptySet()
            override fun getDisplayName(pluginKey: String): String = pluginKey
            override fun getDescription(pluginKey: String): String = ""
            override fun hasSettings(pluginKey: String): Boolean = false
            override fun sortPluginList(plugins: Collection<String>): List<String> = plugins.toList()
        }

        val viewModel = DeviceViewModel(
            pluginMetadataProvider = testProvider,
            backgroundDispatcher = testDispatcher
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dashboardUiState.collect()
        }

        val mockDevice = mockk<Device>(relaxed = true)
        val isReachableFlow = MutableStateFlow(true)
        val loadedPluginsMap = ConcurrentHashMap<String, org.kde.kdeconnect.plugins.Plugin>()
        every { mockDevice.loadedPlugins } returns loadedPluginsMap
        every { mockDevice.getPlugin(any<String>()) } answers {
            loadedPluginsMap[firstArg<String>()]
        }
        every { mockDevice.deviceId } returns "dev-abc"
        every { mockDevice.name } returns "Pixel Tablet"
        every { mockDevice.iconDrawable } returns 42
        every { mockDevice.isReachable } returns true
        every { mockDevice.isReachableFlow } returns isReachableFlow
        every { mockDevice.supportedPlugins } returns emptySet()

        val mockBatteryPlugin = mockk<BatteryPlugin>(relaxed = true)
        val mockBatteryInfo = mockk<DeviceBatteryInfo>(relaxed = true)
        every { mockBatteryInfo.currentCharge } returns 92
        every { mockBatteryInfo.isCharging } returns true
        every { mockBatteryInfo.thresholdEvent } returns 0
        every { mockBatteryPlugin.remoteBatteryInfo } returns mockBatteryInfo
        loadedPluginsMap["BatteryPlugin"] = mockBatteryPlugin

        viewModel.bindDevice(mockDevice, localDeviceName = "Local Phone")

        val state = viewModel.dashboardUiState.value
        assertEquals("Pixel Tablet", state.hero.deviceName)
        assertEquals(42, state.hero.deviceIconRes)
        assertTrue(state.hero.isConnected)
        assertNotNull(state.hero.battery)
        assertEquals(92, state.hero.battery?.percent)
        assertTrue(state.hero.battery?.isCharging == true)

        // Connection change through reachable flow
        isReachableFlow.value = false
        val disconnectedState = viewModel.dashboardUiState.value
        assertFalse(disconnectedState.hero.isConnected)
    }
}
