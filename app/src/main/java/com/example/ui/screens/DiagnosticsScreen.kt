package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BenchmarkEntity
import com.example.model.CableBenchmarkResult
import com.example.ui.BatteryUiState
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    uiState: BatteryUiState,
    onRunBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry = uiState.telemetry

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Info
            Text(
                text = "CABLE & ADAPTER DIAGNOSTICS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Hardware Compatibility Benchmark",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Real-time impedance test evaluates charging rail stability, voltage sag under physical load, and cable resistance to detect damaged accessories.",
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )
        }

        item {
            // Connection Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (telemetry.isCharging) NeonLime.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = "Cable",
                            tint = if (telemetry.isCharging) NeonLime else NeonAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (telemetry.isCharging) "POWER SOURCE CONNECTED" else "DISCONNECTED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.isCharging) NeonLime else NeonAmber
                        )

                        Text(
                            text = if (telemetry.isCharging) telemetry.plugType.displayName else "Plug in cable to benchmark",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Text(
                            text = "Live Rail: ${telemetry.voltageMv} mV • ${telemetry.powerWatts} W",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        item {
            // Benchmark Action & Progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.isBenchmarking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.benchmarkStatusText,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { uiState.benchmarkProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = NeonCyan,
                            trackColor = ObsidianSurfaceElevated
                        )
                    } else {
                        Button(
                            onClick = onRunBenchmark,
                            modifier = Modifier.fillMaxWidth().testTag("start_benchmark_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Test",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Run Live Cable & Adapter Benchmark",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Latest Benchmark Result Card
        uiState.latestBenchmark?.let { result ->
            item {
                BenchmarkResultCard(result)
            }
        }

        // Historical Benchmarks
        if (uiState.benchmarksList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SAVED BENCHMARK HISTORY",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            items(uiState.benchmarksList) { benchmark ->
                SavedBenchmarkItem(benchmark)
            }
        }
    }
}

@Composable
fun BenchmarkResultCard(result: CableBenchmarkResult) {
    val gradeColor = when (result.qualityGrade) {
        "A+", "A" -> NeonLime
        "B" -> NeonCyan
        "C" -> NeonAmber
        else -> NeonCrimson
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("benchmark_result_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, gradeColor.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "COMPATIBILITY GRADE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = result.chargerProtocol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                // Grade Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(gradeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, gradeColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result.qualityGrade,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = gradeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benchmark Metric Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(title = "AVG WATTS", value = "${result.averageWatts} W", accent = NeonCyan)
                MetricColumn(title = "PEAK WATTS", value = "${result.peakWatts} W", accent = NeonLime)
                MetricColumn(title = "VOLTAGE SAG", value = "${result.voltageSagMv} mV", accent = if (result.voltageSagMv < 250) NeonLime else NeonAmber)
                MetricColumn(title = "RESISTANCE", value = "~${result.estimatedResistanceOhms} Ω", accent = TextWhite)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = result.assessment,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.recommendation,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String, accent: Color) {
    Column {
        Text(
            text = title,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

@Composable
fun SavedBenchmarkItem(item: BenchmarkEntity) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    val gradeColor = when (item.qualityGrade) {
        "A+", "A" -> NeonLime
        "B" -> NeonCyan
        "C" -> NeonAmber
        else -> NeonCrimson
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianSurface, RoundedCornerShape(12.dp))
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = item.protocol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = "$dateStr • ${item.watts}W • ${item.voltageMv}mV (${item.voltageSagMv}mV sag)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .background(gradeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.qualityGrade,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor
                )
            }
        }
    }
}
