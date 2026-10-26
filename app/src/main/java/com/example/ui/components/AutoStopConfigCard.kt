package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun AutoStopConfigCard(
    uiState: BatteryUiState,
    onToggleAutoStop: (Boolean) -> Unit,
    onToggleRootCutoff: (Boolean) -> Unit,
    onToggleSmartPlug: (Boolean) -> Unit,
    onUpdateWebhookUrl: (String) -> Unit,
    onUpdateWebhookMethod: (String) -> Unit,
    onToggleFullScreenGuard: (Boolean) -> Unit,
    onTestAutoStop: () -> Unit,
    onTestWebhook: ((Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val alertConfig = uiState.alertConfig
    var webhookTestMessage by remember { mutableStateOf<String?>(null) }
    var isTestingWebhook by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("auto_stop_config_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (alertConfig.isAutoStopOverchargeEnabled) NeonLime.copy(alpha = 0.5f) else ObsidianCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Auto-Stop Overcharge",
                            tint = NeonLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "100% AUTO-STOP OVERCHARGE GUARD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Auto halt charging to protect battery",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Switch(
                    checked = alertConfig.isAutoStopOverchargeEnabled,
                    onCheckedChange = onToggleAutoStop,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = NeonLime,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ObsidianSurfaceElevated
                    ),
                    modifier = Modifier.testTag("toggle_auto_stop_master")
                )
            }

            AnimatedVisibility(visible = alertConfig.isAutoStopOverchargeEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Multi-tier explanation banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceElevated, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "TRIPLE-TIER HARDWARE OVERCHARGE PROTECTION:",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Root PMIC Switch: Disables charging current at Linux kernel level\n2. Smart Plug Webhook: Dispatches HTTP cutoff to AC smart socket\n3. Overcharge Guard: Loud spoken alarm with lockscreen unplug modal",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tier 1: Root PMIC Switch Row
                    AutoStopSubToggleRow(
                        title = "Tier 1: Root Kernel PMIC Cutoff",
                        subtitle = if (uiState.isRootAvailable) "Root access detected: writes 0 to PMIC charging nodes" else "Standard non-root OS: Uses Tier 2 & 3 fallbacks",
                        icon = Icons.Default.Terminal,
                        badgeText = if (uiState.isRootAvailable) "ROOT ACTIVE" else "NON-ROOT",
                        badgeColor = if (uiState.isRootAvailable) NeonLime else NeonAmber,
                        checked = alertConfig.isRootCutoffEnabled,
                        onCheckedChange = onToggleRootCutoff,
                        testTag = "toggle_root_cutoff"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tier 2: Smart Plug Webhook Cutoff
                    AutoStopSubToggleRow(
                        title = "Tier 2: IoT Smart Plug Webhook",
                        subtitle = "Triggers HTTP command to smart socket (Home Assistant/Shelly/Tuya/Tasmota)",
                        icon = Icons.Default.Router,
                        badgeText = "SMART PLUG",
                        badgeColor = NeonCyan,
                        checked = alertConfig.isSmartPlugWebhookEnabled,
                        onCheckedChange = onToggleSmartPlug,
                        testTag = "toggle_smart_plug_webhook"
                    )

                    AnimatedVisibility(visible = alertConfig.isSmartPlugWebhookEnabled) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .background(ObsidianBg, RoundedCornerShape(10.dp))
                                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "SMART PLUG WEBHOOK URL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = alertConfig.smartPlugWebhookUrl,
                                onValueChange = onUpdateWebhookUrl,
                                placeholder = {
                                    Text(
                                        "http://192.168.1.100/relay/0?turn=off",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("smart_plug_url_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextWhite
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    cursorColor = NeonCyan
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // HTTP Method Pills & Test Webhook Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("POST", "GET").forEach { method ->
                                        val isSelected = alertConfig.smartPlugWebhookMethod.equals(method, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) NeonCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonCyan else ObsidianCardBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { onUpdateWebhookMethod(method) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = method,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) NeonCyan else TextMuted
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        isTestingWebhook = true
                                        onTestWebhook { success, msg ->
                                            isTestingWebhook = false
                                            webhookTestMessage = msg
                                        }
                                    },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_test_webhook"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTestingWebhook) "Testing..." else "Test Socket",
                                        fontSize = 11.sp,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (webhookTestMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = webhookTestMessage!!,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (webhookTestMessage!!.startsWith("✅")) NeonLime else NeonAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tier 3: Full-Screen Guard Alert Switch
                    AutoStopSubToggleRow(
                        title = "Tier 3: Full-Screen Overcharge Modal",
                        subtitle = "Pops up high-visibility guard screen when 100% full charge is reached",
                        icon = Icons.Default.Security,
                        badgeText = "GUARD UI",
                        badgeColor = NeonLime,
                        checked = alertConfig.isFullScreenGuardEnabled,
                        onCheckedChange = onToggleFullScreenGuard,
                        testTag = "toggle_fullscreen_guard"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Cutoff Button
                    Button(
                        onClick = onTestAutoStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_simulate_auto_stop"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerOff,
                            contentDescription = null,
                            tint = NeonLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulate 100% Auto-Stop Sequence",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoStopSubToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(ObsidianSurfaceElevated, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = badgeColor,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = ObsidianSurfaceElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
