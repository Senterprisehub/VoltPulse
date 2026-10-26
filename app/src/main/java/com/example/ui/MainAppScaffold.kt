package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.OverchargeGuardModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HealthHistoryScreen
import com.example.ui.screens.LongevityTipsScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

enum class AppTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Bolt),
    DIAGNOSTICS("Diagnostics", Icons.Default.Cable),
    HEALTH("Health", Icons.Default.Favorite),
    TIPS("Tips & Alarms", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: BatteryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startMonitorService()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "VoltPulse Logo",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "VOLTPULSE",
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Hardware Power Telemetry",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                },
                actions = {
                    // Quick charging status pill
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(ObsidianSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (uiState.telemetry.isCharging) NeonLime else NeonCyan,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.telemetry.percentage}% • ${uiState.telemetry.powerWatts}W",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.telemetry.isCharging) NeonLime else TextWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = TextWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianBg,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    onToggle80Alert = { viewModel.toggleEightyPercentAlert(it) },
                    onToggleLowAlert = { viewModel.toggleLowBatteryAlert(it) },
                    onToggleService = { viewModel.toggleService() },
                    onRequestBatteryOpt = { context -> viewModel.requestBatteryOptimizationExemption(context) }
                )
                AppTab.DIAGNOSTICS -> DiagnosticsScreen(
                    uiState = uiState,
                    onRunBenchmark = { viewModel.runCableBenchmark() }
                )
                AppTab.HEALTH -> HealthHistoryScreen(
                    uiState = uiState,
                    onClearHistory = { viewModel.clearAllHistory() }
                )
                AppTab.TIPS -> LongevityTipsScreen(
                    uiState = uiState,
                    onToggleSound = { viewModel.toggleSoundAlert(it) },
                    onToggleVibe = { viewModel.toggleVibrationAlert(it) },
                    onToggleCableAlert = { viewModel.toggleCableAlert(it) },
                    onSetLowThreshold = { viewModel.setLowBatteryThreshold(it) },
                    onToggleFullAlert = { viewModel.toggleFullChargeAlert(it) },
                    onToggleAutoStop = { viewModel.toggleAutoStop(it) },
                    onToggleRootCutoff = { viewModel.toggleRootCutoff(it) },
                    onToggleSmartPlug = { viewModel.toggleSmartPlugWebhook(it) },
                    onUpdateWebhookUrl = { viewModel.updateSmartPlugWebhookUrl(it) },
                    onUpdateWebhookMethod = { viewModel.updateSmartPlugWebhookMethod(it) },
                    onToggleFullScreenGuard = { viewModel.toggleFullScreenGuard(it) },
                    onTestAutoStop = { viewModel.testAutoStopSequence() },
                    onTestWebhook = { callback -> viewModel.testSmartPlugWebhook(callback) },
                    onTestFullCharge = { viewModel.testFullChargeVoiceAlert() },
                    onTestLowBattery = { viewModel.testLowBatteryVoiceAlert() },
                    onTest80Percent = { viewModel.testEightyPercentVoiceAlert() },
                    onTestCableConnected = { viewModel.testCableConnectedVoiceAlert() },
                    onTestCableDisconnected = { viewModel.testCableDisconnectedVoiceAlert() },
                    onToggleVoiceFullCharge = { viewModel.toggleVoiceFullCharge(it) },
                    onToggleVoiceLowBattery = { viewModel.toggleVoiceLowBattery(it) },
                    onToggleVoice80Percent = { viewModel.toggleVoice80Percent(it) },
                    onToggleVoiceCableConnected = { viewModel.toggleVoiceCableConnected(it) },
                    onToggleVoiceCableDisconnected = { viewModel.toggleVoiceCableDisconnected(it) }
                )
            }

            // Full-Screen Auto-Stop Overcharge Protection Guard
            OverchargeGuardModal(
                visible = uiState.isOverchargeGuardVisible,
                telemetry = uiState.telemetry,
                autoStopResult = uiState.latestAutoStopResult,
                onDismiss = { viewModel.dismissOverchargeGuard() }
            )
        }
    }
}
