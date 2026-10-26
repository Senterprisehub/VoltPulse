package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryTelemetry
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun TelemetryGrid(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val tempColor = when {
        telemetry.temperatureC >= 42.0 -> NeonCrimson
        telemetry.temperatureC >= 36.0 -> NeonAmber
        else -> NeonCyan
    }

    val currentFlowSign = if (telemetry.isCharging) "+" else ""
    val powerColor = if (telemetry.isCharging) NeonLime else NeonCyan

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryTile(
                title = "POWER SPEED",
                value = "${telemetry.powerWatts} W",
                subtitle = if (telemetry.isCharging) "Input Rate" else "Discharge Rate",
                icon = Icons.Default.Bolt,
                accentColor = powerColor,
                modifier = Modifier.weight(1f)
            )

            TelemetryTile(
                title = "CURRENT FLOW",
                value = "$currentFlowSign${telemetry.currentMa} mA",
                subtitle = if (telemetry.isCharging) "Charging Stream" else "Active Hardware Draw",
                icon = Icons.Default.Speed,
                accentColor = if (telemetry.isCharging) NeonLime else NeonAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryTile(
                title = "BUS VOLTAGE",
                value = "${String.format("%.2f", telemetry.voltageVolts)} V",
                subtitle = "${telemetry.voltageMv} mV Rail",
                icon = Icons.Default.ElectricMeter,
                accentColor = NeonCyan,
                modifier = Modifier.weight(1f)
            )

            TelemetryTile(
                title = "THERMALS",
                value = "${String.format("%.1f", telemetry.temperatureC)}°C",
                subtitle = "${String.format("%.1f", telemetry.temperatureF)}°F • ${telemetry.health.displayName}",
                icon = Icons.Default.Thermostat,
                accentColor = tempColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TelemetryTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ObsidianSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}
