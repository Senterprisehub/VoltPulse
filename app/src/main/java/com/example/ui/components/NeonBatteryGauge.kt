package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryTelemetry
import com.example.ui.theme.GaugeFillCritical
import com.example.ui.theme.GaugeFillLow
import com.example.ui.theme.GaugeTrack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun NeonBatteryGauge(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = telemetry.percentage.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "battery_percent_anim"
    )

    // Pulse animation when connected to charger
    val infiniteTransition = rememberInfiniteTransition(label = "charging_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val activeColor = when {
        telemetry.isCharging -> NeonLime
        telemetry.percentage <= 15 -> GaugeFillCritical
        telemetry.percentage <= 25 -> GaugeFillLow
        else -> NeonCyan
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("neon_battery_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Track Arc (260 degree arc)
            drawArc(
                color = GaugeTrack,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Neon Glow underlayer
            val glowColor = if (telemetry.isCharging) NeonLime.copy(alpha = pulseAlpha * 0.45f) else activeColor.copy(alpha = 0.25f)
            drawArc(
                color = glowColor,
                startAngle = 140f,
                sweepAngle = (animatedPercentage / 100f) * 260f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Foreground Active Progress Arc
            val brush = Brush.sweepGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.8f),
                    activeColor,
                    if (telemetry.isCharging) NeonCyanGlow else activeColor
                )
            )

            drawArc(
                brush = brush,
                startAngle = 140f,
                sweepAngle = (animatedPercentage / 100f) * 260f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Digital Telemetry Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${telemetry.percentage}%",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextWhite,
                letterSpacing = (-1).sp,
                modifier = Modifier.testTag("gauge_percentage_text")
            )

            Text(
                text = if (telemetry.isCharging) "+${telemetry.powerWatts}W" else "${telemetry.powerWatts}W",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = activeColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (telemetry.isCharging) "CHARGING" else "DISCHARGING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}
