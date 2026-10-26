package com.example.hardware

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Robust, stabilized estimation engine for lithium-ion battery charging and runtime.
 * Implements hardware PMIC smoothing, spike filtering, empirical SOC delta tracking,
 * and CC/CV non-linear curve compensation.
 */
class RobustBatteryEstimator {

    private val lock = Any()
    private val sampleWindowSize = 15
    private val currentSamples = IntArray(sampleWindowSize)
    private var sampleCount = 0
    private var sampleHead = 0
    private var smoothedCurrentMa: Double = 0.0
    private var lastChargingState: Boolean? = null

    // Empirical SOC delta tracker
    private data class SocPoint(val timestampMs: Long, val percentage: Int)
    private val socHistory = ArrayDeque<SocPoint>()
    private val maxSocHistorySize = 10

    // Output stabilization (hysteresis)
    private var lastTimeToFullMs: Long = 0L
    private var lastTimeRemainingMs: Long = 0L
    private var lastTimeToEightyMs: Long = 0L

    /**
     * Resets internal tracking buffers (e.g. when charger is plugged or unplugged).
     */
    fun reset(isCharging: Boolean) = synchronized(lock) {
        sampleCount = 0
        sampleHead = 0
        smoothedCurrentMa = 0.0
        socHistory.clear()
        lastChargingState = isCharging
        lastTimeToFullMs = 0L
        lastTimeRemainingMs = 0L
        lastTimeToEightyMs = 0L
    }

    /**
     * Ingests latest hardware telemetry and updates stabilized models.
     */
    fun update(
        isCharging: Boolean,
        percentage: Int,
        currentNowMa: Int,
        currentAvgMa: Int,
        capacityMah: Int,
        timestampMs: Long = System.currentTimeMillis()
    ) = synchronized(lock) {
        // Detect state transition (e.g. plugged in or unplugged)
        if (lastChargingState != null && lastChargingState != isCharging) {
            reset(isCharging)
        }
        lastChargingState = isCharging

        // 1. Hardware PMIC Current Selection & Spike Suppression
        val rawCurrent = abs(if (currentAvgMa != 0) currentAvgMa else currentNowMa)
        val filteredCurrent = filterCurrentSpikes(rawCurrent)

        // Exponential Moving Average (EMA with alpha = 0.15 for smooth dampening)
        smoothedCurrentMa = if (smoothedCurrentMa <= 0.0) {
            filteredCurrent.toDouble()
        } else {
            (0.15 * filteredCurrent) + (0.85 * smoothedCurrentMa)
        }

        // 2. Empirical SOC delta tracking
        if (socHistory.isEmpty() || socHistory.last().percentage != percentage) {
            socHistory.addLast(SocPoint(timestampMs, percentage))
            if (socHistory.size > maxSocHistorySize) {
                socHistory.removeFirst()
            }
        }
    }

    private fun filterCurrentSpikes(raw: Int): Int = synchronized(lock) {
        if (sampleCount >= 4) {
            val snapshot = IntArray(sampleCount)
            for (i in 0 until sampleCount) {
                val index = (sampleHead - sampleCount + i + sampleWindowSize * 2) % sampleWindowSize
                snapshot[i] = currentSamples[index]
            }
            // DualPivotQuicksort for primitive ints: impossible to throw NullPointerException
            snapshot.sort()
            val median = snapshot[sampleCount / 2]
            // Reject wild transient spikes (> 2.5x median or < 0.25x median if non-zero)
            if (median > 100 && (raw > median * 2.5 || raw < median * 0.25)) {
                addSample(median)
                return median
            }
        }

        addSample(raw)
        return raw
    }

    private fun addSample(value: Int) {
        currentSamples[sampleHead] = value
        sampleHead = (sampleHead + 1) % sampleWindowSize
        if (sampleCount < sampleWindowSize) {
            sampleCount++
        }
    }

    /**
     * Computes stabilized time to 100% full charge in milliseconds.
     * Directly prioritizes the Android OS battery HAL computeChargeTimeRemaining()
     * to match the device's inbuilt lock screen and settings charging time 1:1.
     */
    fun computeTimeToFullMillis(
        percentage: Int,
        capacityMah: Int,
        systemReportedMs: Long = 0L
    ): Long = synchronized(lock) {
        if (percentage >= 100) return 0L

        // When Android OS system HAL provides a valid charge completion estimate,
        // use it directly so our app is in 100% exact alignment with device lock screen & settings.
        if (systemReportedMs > 0L) {
            lastTimeToFullMs = systemReportedMs
            return systemReportedMs
        }

        val baseCurrent = max(250.0, smoothedCurrentMa)
        val remainingPct = max(0, 100 - percentage)
        val remainingMah = (remainingPct / 100.0) * capacityMah

        // Non-linear CC/CV Curve:
        // Above 75% SOC, battery internal resistance rises and current tapers significantly
        val cvMultiplier = when {
            percentage >= 95 -> 2.10
            percentage >= 90 -> 1.75
            percentage >= 80 -> 1.45
            percentage >= 75 -> 1.25
            else -> 1.10
        }

        val currentBasedHours = (remainingMah / baseCurrent) * cvMultiplier
        var targetMs = (currentBasedHours * 3600 * 1000).toLong()

        // Incorporate empirical charge rate if available (over at least 3 minutes)
        val empiricalRatePctPerHour = computeEmpiricalRatePctPerHour()
        if (empiricalRatePctPerHour != null && empiricalRatePctPerHour > 1.0) {
            val empiricalHours = remainingPct / empiricalRatePctPerHour
            val empiricalMs = (empiricalHours * 3600 * 1000).toLong()
            // Weighted blend: 60% empirical trajectory, 40% PMIC current
            targetMs = ((empiricalMs * 0.6) + (targetMs * 0.4)).toLong()
        }

        // Apply hysteresis to prevent abrupt 1-second display jumps
        lastTimeToFullMs = if (lastTimeToFullMs <= 0L) {
            targetMs
        } else {
            val delta = targetMs - lastTimeToFullMs
            val clampedDelta = (delta * 0.2).toLong()
            lastTimeToFullMs + clampedDelta
        }

        return max(60_000L, lastTimeToFullMs)
    }

    /**
     * Computes stabilized time to 80% charge threshold in milliseconds.
     */
    fun computeTimeToEightyMillis(
        percentage: Int,
        capacityMah: Int,
        systemReportedMs: Long = 0L
    ): Long = synchronized(lock) {
        if (percentage >= 80) return 0L

        // If system HAL provides total time to full, calculate the proportional time to 80%
        if (systemReportedMs > 0L) {
            val pctTo80 = (80 - percentage).coerceAtLeast(0)
            val pctTo100 = (100 - percentage).coerceAtLeast(1)
            val proportionalMs = ((pctTo80.toDouble() / pctTo100.toDouble()) * systemReportedMs).toLong()
            lastTimeToEightyMs = max(60_000L, proportionalMs)
            return lastTimeToEightyMs
        }

        val baseCurrent = max(250.0, smoothedCurrentMa)
        val pctTo80 = 80 - percentage
        val mahTo80 = (pctTo80 / 100.0) * capacityMah
        val hoursTo80 = (mahTo80 / baseCurrent) * 1.05
        var targetMs = (hoursTo80 * 3600 * 1000).toLong()

        val empiricalRatePctPerHour = computeEmpiricalRatePctPerHour()
        if (empiricalRatePctPerHour != null && empiricalRatePctPerHour > 1.0) {
            val empiricalHours = pctTo80 / empiricalRatePctPerHour
            val empiricalMs = (empiricalHours * 3600 * 1000).toLong()
            targetMs = ((empiricalMs * 0.6) + (targetMs * 0.4)).toLong()
        }

        lastTimeToEightyMs = if (lastTimeToEightyMs <= 0L) {
            targetMs
        } else {
            val delta = targetMs - lastTimeToEightyMs
            lastTimeToEightyMs + (delta * 0.2).toLong()
        }

        return max(30_000L, lastTimeToEightyMs)
    }

    /**
     * Computes stabilized operational runtime remaining while discharging on battery.
     */
    fun computeTimeRemainingMillis(
        percentage: Int,
        capacityMah: Int
    ): Long = synchronized(lock) {
        if (percentage <= 0) return 0L

        val dischargeCurrentMa = max(120.0, smoothedCurrentMa)
        val usableMah = (percentage / 100.0) * capacityMah
        val currentHoursLeft = usableMah / dischargeCurrentMa
        var targetMs = (currentHoursLeft * 3600 * 1000).toLong()

        // Empirical discharge slope
        val empiricalRatePctPerHour = computeEmpiricalRatePctPerHour()
        if (empiricalRatePctPerHour != null && empiricalRatePctPerHour > 0.5) {
            val empiricalHours = percentage / empiricalRatePctPerHour
            val empiricalMs = (empiricalHours * 3600 * 1000).toLong()
            // Blend empirical rate with PMIC current
            targetMs = ((empiricalMs * 0.55) + (targetMs * 0.45)).toLong()
        }

        // Hysteresis stabilization
        lastTimeRemainingMs = if (lastTimeRemainingMs <= 0L) {
            targetMs
        } else {
            val delta = targetMs - lastTimeRemainingMs
            lastTimeRemainingMs + (delta * 0.15).toLong()
        }

        return max(60_000L, lastTimeRemainingMs)
    }

    /**
     * Computes historical rate of battery percentage change per hour from recorded transitions.
     */
    private fun computeEmpiricalRatePctPerHour(): Double? = synchronized(lock) {
        if (socHistory.size < 2) return null
        val first = socHistory.firstOrNull() ?: return null
        val last = socHistory.lastOrNull() ?: return null
        val timeDeltaMs = last.timestampMs - first.timestampMs

        // Need at least 2 minutes (120,000 ms) of history to establish reliable trajectory
        if (timeDeltaMs < 120_000L) return null

        val pctDelta = abs(last.percentage - first.percentage)
        if (pctDelta <= 0) return null

        val hoursDelta = timeDeltaMs / (1000.0 * 3600.0)
        return pctDelta / hoursDelta
    }
}
