package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Real-time hardware battery telemetry data pulled directly from Android BatteryManager.
 */
data class BatteryTelemetry(
    val level: Int = 50,
    val scale: Int = 100,
    val percentage: Int = 50,
    val voltageMv: Int = 4000,
    val currentMa: Int = 0, // Positive for charging, negative for discharging
    val powerWatts: Double = 0.0,
    val temperatureC: Double = 25.0,
    val isCharging: Boolean = false,
    val plugType: PlugType = PlugType.NONE,
    val health: BatteryHealth = BatteryHealth.GOOD,
    val technology: String = "Li-ion",
    val statusText: String = "Discharging",
    val timeRemainingMillis: Long = -1L, // Discharging time left
    val timeToFullMillis: Long = -1L, // Charging time to 100%
    val timeToEightyMillis: Long = -1L, // Charging time to 80%
    val chargeCounterUah: Long = 0L, // Remaining charge in uAh
    val estimatedCapacityMah: Int = 4500
) {
    val temperatureF: Double
        get() = (temperatureC * 9.0 / 5.0) + 32.0

    val voltageVolts: Double
        get() = voltageMv / 1000.0

    val chargingSpeedType: String
        get() = when {
            !isCharging -> "Discharging"
            powerWatts >= 25.0 -> "Super Fast Charging"
            powerWatts >= 15.0 -> "Fast Charging"
            powerWatts >= 7.5 -> "Standard Charging"
            plugType == PlugType.WIRELESS -> "Wireless Charging"
            else -> "Slow Charging"
        }
}

enum class PlugType(val displayName: String) {
    NONE("Unplugged"),
    AC("Wall Adapter (AC)"),
    USB("USB Port"),
    WIRELESS("Wireless (Qi)"),
    DOCK("Dock Station")
}

enum class BatteryHealth(val displayName: String) {
    UNKNOWN("Unknown"),
    GOOD("Good (Normal)"),
    OVERHEAT("Overheating!"),
    DEAD("Dead Cell"),
    OVER_VOLTAGE("Over Voltage!"),
    UNSPECIFIED_FAILURE("Hardware Failure"),
    COLD("Freezing Cold")
}

/**
 * Genuine hardware device details resolved via android.os.Build.
 */
data class DeviceInfo(
    val manufacturer: String = "Android",
    val model: String = "Device",
    val marketName: String = "Android Device",
    val androidVersion: String = "15",
    val apiLevel: Int = 35,
    val hardwareBoard: String = "Default",
    val socName: String = "Default SOC",
    val batteryDesignCapacityMah: Int = 4500
)

/**
 * User configurable alert settings with individual voice notification switches.
 */
data class BatteryAlertConfig(
    val isEightyPercentAlertEnabled: Boolean = true,
    val isLowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    val isFullChargeAlertEnabled: Boolean = true,
    val isSoundAlertEnabled: Boolean = true,
    val isVibrationAlertEnabled: Boolean = true,
    val isCableAlertEnabled: Boolean = true,
    val isPersistentNotificationEnabled: Boolean = true,
    val isAutoStopOverchargeEnabled: Boolean = true,
    val isRootCutoffEnabled: Boolean = true,
    val isSmartPlugWebhookEnabled: Boolean = false,
    val smartPlugWebhookUrl: String = "",
    val smartPlugWebhookMethod: String = "POST",
    val isFullScreenGuardEnabled: Boolean = true,
    // Individual On/Off switches for each voice notification
    val isVoiceFullChargeEnabled: Boolean = true,
    val isVoiceLowBatteryEnabled: Boolean = true,
    val isVoiceEightyPercentEnabled: Boolean = true,
    val isVoiceCableConnectedEnabled: Boolean = true,
    val isVoiceCableDisconnectedEnabled: Boolean = true
)

/**
 * Result report from executing Auto-Stop Overcharge Cutoff routine.
 */
data class AutoStopExecutionResult(
    val timestamp: Long = System.currentTimeMillis(),
    val isRootCutoffAttempted: Boolean = false,
    val isRootCutoffSuccess: Boolean = false,
    val isWebhookDispatched: Boolean = false,
    val webhookStatusCode: Int? = null,
    val webhookMessage: String? = null,
    val isFullScreenGuardTriggered: Boolean = false,
    val message: String = ""
)

/**
 * Diagnostic cable and adapter benchmark result.
 */
data class CableBenchmarkResult(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val averageWatts: Double = 0.0,
    val peakWatts: Double = 0.0,
    val averageVoltageMv: Int = 0,
    val averageCurrentMa: Int = 0,
    val voltageSagMv: Int = 0,
    val stabilityPercentage: Double = 98.0,
    val estimatedResistanceOhms: Double = 0.12,
    val qualityGrade: String = "A", // A+, A, B, C, Fair, Poor
    val chargerProtocol: String = "USB-PD / Fast Charger",
    val assessment: String = "Excellent cable & adapter throughput.",
    val recommendation: String = "Your cable and power adapter are operating at peak efficiency with minimal resistance."
)

/**
 * Room entity for historical charging sessions.
 */
@Entity(tableName = "battery_session_logs")
data class BatterySessionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "CHARGING_START", "CHARGING_END", "BATTERY_SNAPSHOT"
    val batteryLevel: Int,
    val voltageMv: Int,
    val currentMa: Int,
    val powerWatts: Double,
    val temperatureC: Double,
    val plugType: String
)

/**
 * Room entity for saved hardware benchmark tests.
 */
@Entity(tableName = "cable_benchmarks")
data class BenchmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val watts: Double,
    val voltageMv: Int,
    val currentMa: Int,
    val voltageSagMv: Int,
    val qualityGrade: String,
    val protocol: String,
    val assessment: String
)
