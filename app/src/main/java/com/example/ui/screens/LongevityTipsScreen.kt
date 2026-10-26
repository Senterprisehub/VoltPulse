package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BatteryUiState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.components.AutoStopConfigCard
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun LongevityTipsScreen(
    uiState: BatteryUiState,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibe: (Boolean) -> Unit,
    onToggleCableAlert: (Boolean) -> Unit,
    onSetLowThreshold: (Int) -> Unit,
    onToggleFullAlert: (Boolean) -> Unit,
    onToggleAutoStop: (Boolean) -> Unit,
    onToggleRootCutoff: (Boolean) -> Unit,
    onToggleSmartPlug: (Boolean) -> Unit,
    onUpdateWebhookUrl: (String) -> Unit,
    onUpdateWebhookMethod: (String) -> Unit,
    onToggleFullScreenGuard: (Boolean) -> Unit,
    onTestAutoStop: () -> Unit,
    onTestWebhook: ((Boolean, String) -> Unit) -> Unit,
    onTestFullCharge: () -> Unit,
    onTestLowBattery: () -> Unit,
    onTest80Percent: () -> Unit,
    onTestCableConnected: () -> Unit,
    onTestCableDisconnected: () -> Unit,
    onToggleVoiceFullCharge: (Boolean) -> Unit = {},
    onToggleVoiceLowBattery: (Boolean) -> Unit = {},
    onToggleVoice80Percent: (Boolean) -> Unit = {},
    onToggleVoiceCableConnected: (Boolean) -> Unit = {},
    onToggleVoiceCableDisconnected: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val alertConfig = uiState.alertConfig

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "BATTERY LONGEVITY & OVERCHARGE CUTOFF",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Auto-Cutoff & Voice Alarms",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }

        item {
            // Auto-Stop Overcharge Protection Card
            AutoStopConfigCard(
                uiState = uiState,
                onToggleAutoStop = onToggleAutoStop,
                onToggleRootCutoff = onToggleRootCutoff,
                onToggleSmartPlug = onToggleSmartPlug,
                onUpdateWebhookUrl = onUpdateWebhookUrl,
                onUpdateWebhookMethod = onUpdateWebhookMethod,
                onToggleFullScreenGuard = onToggleFullScreenGuard,
                onTestAutoStop = onTestAutoStop,
                onTestWebhook = onTestWebhook
            )
        }

        item {
            // Spoken Voice Announcements Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("voice_alerts_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonLime.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Voice Announcements",
                                    tint = NeonLime,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SPOKEN VOICE ALERTS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Speaks alert messages out loud",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = alertConfig.isSoundAlertEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = NeonLime,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ObsidianSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CUSTOM VOICE ANNOUNCEMENTS (TAP TO PREVIEW):",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Alert 1: Full Charge
                    VoiceAlertRow(
                        title = "1. Full Charge Alert",
                        speechText = "\"Device fully charged, remove the cable.\"",
                        icon = Icons.Default.Alarm,
                        accentColor = NeonLime,
                        enabled = alertConfig.isVoiceFullChargeEnabled,
                        onCheckedChange = onToggleVoiceFullCharge,
                        onPlayPreview = onTestFullCharge,
                        testTag = "preview_full_charge"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Alert 2: Low Battery
                    VoiceAlertRow(
                        title = "2. Low Battery Alert (20%, 10%, 5% Milestones)",
                        speechText = "\"Low battery, plug in for charging.\"",
                        icon = Icons.Default.BatteryAlert,
                        accentColor = NeonAmber,
                        enabled = alertConfig.isVoiceLowBatteryEnabled,
                        onCheckedChange = onToggleVoiceLowBattery,
                        onPlayPreview = onTestLowBattery,
                        testTag = "preview_low_battery"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Alert 3: 80% Cutoff
                    VoiceAlertRow(
                        title = "3. 80% Health Protector Alert",
                        speechText = "\"Battery charged to 80%, unplug charger to protect battery health.\"",
                        icon = Icons.Default.Hearing,
                        accentColor = NeonCyan,
                        enabled = alertConfig.isVoiceEightyPercentEnabled,
                        onCheckedChange = onToggleVoice80Percent,
                        onPlayPreview = onTest80Percent,
                        testTag = "preview_80_percent"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Alert 4: Cable Connected
                    VoiceAlertRow(
                        title = "4. Cable Connected Alert",
                        speechText = "\"Charging cable connected.\"",
                        icon = Icons.Default.Cable,
                        accentColor = NeonLime,
                        enabled = alertConfig.isVoiceCableConnectedEnabled,
                        onCheckedChange = onToggleVoiceCableConnected,
                        onPlayPreview = onTestCableConnected,
                        testTag = "preview_cable_connected"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Alert 5: Cable Disconnected
                    VoiceAlertRow(
                        title = "5. Cable Disconnected Alert",
                        speechText = "\"Charging Cable disconnected.\"",
                        icon = Icons.Default.Cable,
                        accentColor = NeonAmber,
                        enabled = alertConfig.isVoiceCableDisconnectedEnabled,
                        onCheckedChange = onToggleVoiceCableDisconnected,
                        onPlayPreview = onTestCableDisconnected,
                        testTag = "preview_cable_disconnected"
                    )
                }
            }
        }

        item {
            // Feedback Settings Card (Vibration & Thresholds)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HAPTIC & TRIGGER THRESHOLDS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AlarmToggleRow(
                        title = "Cable Connect/Disconnect Voice Alert",
                        subtitle = "Speaks \"Charging cable connected\" and \"Charging Cable disconnected\" upon plugging/unplugging.",
                        icon = Icons.Default.Cable,
                        checked = alertConfig.isCableAlertEnabled,
                        onCheckedChange = onToggleCableAlert
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AlarmToggleRow(
                        title = "Haptic Vibration Pulses",
                        subtitle = "Vibrate device with priority pattern when voice alert fires.",
                        icon = Icons.Default.Vibration,
                        checked = alertConfig.isVibrationAlertEnabled,
                        onCheckedChange = onToggleVibe
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AlarmToggleRow(
                        title = "100% Full Charge Trigger",
                        subtitle = "Enable alert as soon as battery level reaches 100%.",
                        icon = Icons.Default.Alarm,
                        checked = alertConfig.isFullChargeAlertEnabled,
                        onCheckedChange = onToggleFullAlert
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Low threshold selector & explanation
                    Text(
                        text = "LOW BATTERY ALERT MILESTONES (FIXED: 20%, 10%, 5%)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Alerts strictly at 20%, 10%, and 5% checkpoints to prevent repetitive announcements on every percentage drop.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("20% (Warning)", "10% (Low)", "5% (Critical)").forEach { milestoneLabel ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        ObsidianSurfaceElevated,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = milestoneLabel,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "ACTIONABLE LONGEVITY TIPS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        item {
            TipCard(
                title = "The 80% Charge Sweet Spot",
                description = "Charging lithium-ion cells past 80% places exponential stress on the cathode. Unplugging at 80% can increase battery cycle life from 500 to over 1,200 cycles.",
                icon = Icons.Default.BatteryAlert,
                accentColor = NeonCyan
            )
        }

        item {
            TipCard(
                title = "Avoid High Charging Thermals (>38°C)",
                description = "Heat is the #1 cause of permanent capacity loss. Remove thick phone cases during fast charging and avoid intense gaming while connected to a high-wattage charger.",
                icon = Icons.Default.Thermostat,
                accentColor = NeonAmber
            )
        }

        item {
            TipCard(
                title = "Prevent Deep Discharges (<15%)",
                description = "Dropping to 0% causes mechanical contraction in the anode and copper dendrite growth. Recharge your phone whenever it hits 20% to maintain steady voltage stability.",
                icon = Icons.Default.Lightbulb,
                accentColor = NeonLime
            )
        }
    }
}

@Composable
private fun VoiceAlertRow(
    title: String,
    speechText: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPlayPreview: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (enabled) accentColor else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) TextWhite else TextMuted
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag("${testTag}_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ObsidianSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = speechText,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = if (enabled) accentColor.copy(alpha = 0.9f) else TextMuted.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onPlayPreview,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag(testTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Play",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hear Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AlarmToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(ObsidianSurfaceElevated, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurfaceElevated
            )
        )
    }
}

@Composable
private fun TipCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface, RoundedCornerShape(14.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
