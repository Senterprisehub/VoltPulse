package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AutoStopExecutionResult
import com.example.model.BatteryTelemetry
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
fun OverchargeGuardModal(
    visible: Boolean,
    telemetry: BatteryTelemetry,
    autoStopResult: AutoStopExecutionResult?,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg.copy(alpha = 0.96f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonLime)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Glowing Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeonLime.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, NeonLime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Overcharge Shield",
                            tint = NeonLime,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "100% CHARGE COMPLETE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonLime,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Auto-Stop Overcharge Guard Active",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your battery has reached maximum capacity. The system has initiated auto-cutoff protection to prevent cell oxidation, excessive cathode strain, and thermal aging.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Telemetry Pill Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricMiniPill("BATTERY", "${telemetry.percentage}%", NeonLime)
                        MetricMiniPill("VOLTAGE", "${telemetry.voltageMv} mV", NeonCyan)
                        MetricMiniPill("TEMP", "${String.format("%.1f", telemetry.temperatureC)}°C", if (telemetry.temperatureC > 38f) NeonAmber else TextWhite)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Multi-tier Cutoff Execution Status
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianBg, RoundedCornerShape(12.dp))
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "HARDWARE CUTOFF SYSTEM STATUS",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tier 1: Root PMIC Switch
                        CutoffStatusRow(
                            name = "Tier 1: Root Kernel PMIC Switch",
                            status = when {
                                autoStopResult?.isRootCutoffSuccess == true -> "ACTIVE (0 mA Dispatched)"
                                autoStopResult?.isRootCutoffAttempted == true -> "N/A (Standard Non-Root OS)"
                                else -> "Standby"
                            },
                            isSuccess = autoStopResult?.isRootCutoffSuccess == true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tier 2: IoT Smart Plug Webhook
                        CutoffStatusRow(
                            name = "Tier 2: Smart Plug Webhook AC Cutoff",
                            status = when {
                                autoStopResult?.isWebhookDispatched == true && autoStopResult.webhookStatusCode in 200..299 ->
                                    "SIGNAL SENT (HTTP ${autoStopResult.webhookStatusCode})"
                                autoStopResult?.isWebhookDispatched == true ->
                                    autoStopResult.webhookMessage ?: "Dispatched"
                                else -> "Disabled in Settings"
                            },
                            isSuccess = autoStopResult?.isWebhookDispatched == true && autoStopResult.webhookStatusCode in 200..299
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tier 3: Voice & Full-Screen Alarm
                        CutoffStatusRow(
                            name = "Tier 3: Spoken Voice & Screen Alarm",
                            status = "ACTIVE & SOUNDING",
                            isSuccess = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Acknowledge Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_overcharge_guard"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonLime),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerOff,
                            contentDescription = null,
                            tint = ObsidianBg,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I Have Unplugged The Charger",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBg
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("silence_overcharge_guard"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Mute Alarm (Keep Charging)",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniPill(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CutoffStatusRow(name: String, status: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSuccess) NeonLime else NeonAmber,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = status,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isSuccess) NeonLime else TextMuted
        )
    }
}
