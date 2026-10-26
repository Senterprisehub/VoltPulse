package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BatteryUiState
import com.example.ui.components.BatteryOptimizationCard
import com.example.ui.components.DeviceInfoCard
import com.example.ui.components.NeonBatteryGauge
import com.example.ui.components.TelemetryGrid
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun DashboardScreen(
    uiState: BatteryUiState,
    onToggle80Alert: (Boolean) -> Unit,
    onToggleLowAlert: (Boolean) -> Unit,
    onToggleService: () -> Unit,
    onRequestBatteryOpt: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry = uiState.telemetry
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Model & Hardware Card
        DeviceInfoCard(
            deviceInfo = uiState.deviceInfo,
            telemetry = telemetry
        )

        // Android Doze Mode / Battery Optimization Status
        BatteryOptimizationCard(
            isIgnored = uiState.isBatteryOptimizationIgnored,
            onRequestExemption = { onRequestBatteryOpt(context) }
        )

        // Central Neon Glowing Gauge
        NeonBatteryGauge(
            telemetry = telemetry,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Estimated Time Bar
        TimeEstimateCard(
            telemetry = telemetry
        )

        // 4-Tile Live Hardware Telemetry
        TelemetryGrid(
            telemetry = telemetry
        )

        // Quick Protection Switches Card
        QuickControlsCard(
            uiState = uiState,
            onToggle80Alert = onToggle80Alert,
            onToggleLowAlert = onToggleLowAlert,
            onToggleService = onToggleService
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TimeEstimateCard(
    telemetry: com.example.model.BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val speedColor = when (telemetry.chargingSpeedType) {
        "Super Fast Charging" -> NeonLime
        "Fast Charging" -> NeonCyan
        "Standard Charging" -> TextWhite
        else -> NeonAmber
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (telemetry.isCharging) speedColor.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (telemetry.isCharging) Icons.Default.Bolt else Icons.Default.HourglassTop,
                    contentDescription = "Time Estimate",
                    tint = if (telemetry.isCharging) speedColor else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (telemetry.isCharging) "TIME TO FULL CHARGE" else "OPERATIONAL RUNTIME REMAINING",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    if (telemetry.isCharging) {
                        Box(
                            modifier = Modifier
                                .background(speedColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, speedColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = telemetry.chargingSpeedType,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = speedColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val primaryTimeText = if (telemetry.isCharging) {
                    if (telemetry.timeToFullMillis > 0) {
                        formatDurationText(telemetry.timeToFullMillis) + " until full"
                    } else "Calculating charging rate..."
                } else {
                    if (telemetry.timeRemainingMillis > 0) {
                        formatDurationText(telemetry.timeRemainingMillis)
                    } else "Calculating usage..."
                }

                Text(
                    text = primaryTimeText,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (telemetry.isCharging) speedColor else TextWhite
                )

                if (telemetry.isCharging) {
                    if (telemetry.percentage < 80 && telemetry.timeToEightyMillis > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Time to 80% (Safe Cutoff): ${formatDurationText(telemetry.timeToEightyMillis)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonAmber
                        )
                    } else if (telemetry.percentage >= 80) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⚡ 80% safe charging limit reached",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonLime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickControlsCard(
    uiState: BatteryUiState,
    onToggle80Alert: (Boolean) -> Unit,
    onToggleLowAlert: (Boolean) -> Unit,
    onToggleService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ALERTS & LOCK SCREEN CONTROLS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            SwitchRow(
                title = "80% Battery Longevity Alarm",
                subtitle = "Sound/vibrate alarm when battery hits 80% to avoid thermal degradation.",
                checked = uiState.alertConfig.isEightyPercentAlertEnabled,
                onCheckedChange = onToggle80Alert,
                testTag = "toggle_80_alarm"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SwitchRow(
                title = "Low Battery Alert (${uiState.alertConfig.lowBatteryThreshold}%)",
                subtitle = "Proactively warns you when charge drops and needs power.",
                checked = uiState.alertConfig.isLowBatteryAlertEnabled,
                onCheckedChange = onToggleLowAlert,
                testTag = "toggle_low_alarm"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SwitchRow(
                title = "Lock Screen Live Notification",
                subtitle = "Ongoing progress bar, live Watts, and countdown timer on lock screen.",
                checked = uiState.isServiceRunning,
                onCheckedChange = { onToggleService() },
                testTag = "toggle_lockscreen_service"
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurfaceElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun formatDurationText(millis: Long): String {
    val totalMinutes = millis / (1000 * 60)
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "< 1m"
    }
}
