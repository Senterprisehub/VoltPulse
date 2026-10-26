package com.example.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RobustBatteryEstimatorTest {

    private lateinit var estimator: RobustBatteryEstimator

    @Before
    fun setUp() {
        estimator = RobustBatteryEstimator()
    }

    @Test
    fun `test discharge time calculation stability during sudden CPU spike`() {
        val capacityMah = 5000
        val percentage = 50

        // Normal discharge baseline (~400mA)
        for (i in 1..5) {
            estimator.update(
                isCharging = false,
                percentage = percentage,
                currentNowMa = 400,
                currentAvgMa = 400,
                capacityMah = capacityMah
            )
        }

        val baselineHours = estimator.computeTimeRemainingMillis(percentage, capacityMah) / (1000.0 * 3600.0)
        assertTrue("Baseline hours should be between 5 and 7 hours", baselineHours in 5.0..7.0)

        // Inject sudden transient CPU surge (e.g. 3500mA spike for 1 sample)
        estimator.update(
            isCharging = false,
            percentage = percentage,
            currentNowMa = 3500,
            currentAvgMa = 400,
            capacityMah = capacityMah
        )

        val spikeHours = estimator.computeTimeRemainingMillis(percentage, capacityMah) / (1000.0 * 3600.0)
        // With spike filtering and hysteresis, the estimate should not drop to 40 minutes!
        assertTrue(
            "Spike should be suppressed and estimate remains stable (> 4.5 hours, got $spikeHours)",
            spikeHours >= 4.5
        )
    }

    @Test
    fun `test CC-CV charging curve takes longer above 80 percent due to saturation`() {
        val capacityMah = 5000

        estimator.update(
            isCharging = true,
            percentage = 40,
            currentNowMa = 2000,
            currentAvgMa = 2000,
            capacityMah = capacityMah
        )
        val time40ToFullMs = estimator.computeTimeToFullMillis(40, capacityMah)

        estimator.reset(isCharging = true)
        estimator.update(
            isCharging = true,
            percentage = 85,
            currentNowMa = 2000,
            currentAvgMa = 2000,
            capacityMah = capacityMah
        )
        val time85ToFullMs = estimator.computeTimeToFullMillis(85, capacityMah)

        // 15% remaining at 85% vs 60% remaining at 40%
        assertTrue("TimeToFull at 40% should be greater than at 85%", time40ToFullMs > time85ToFullMs)
        assertTrue("TimeToFull at 85% should still be positive and accounted for", time85ToFullMs > 0)
    }

    @Test
    fun `test state transition resets properly when plugged in`() {
        val capacityMah = 4500

        // Discharging
        estimator.update(
            isCharging = false,
            percentage = 60,
            currentNowMa = 350,
            currentAvgMa = 350,
            capacityMah = capacityMah
        )
        val dischargeTime = estimator.computeTimeRemainingMillis(60, capacityMah)
        assertTrue(dischargeTime > 0)

        // Plugged in
        estimator.update(
            isCharging = true,
            percentage = 60,
            currentNowMa = 2500,
            currentAvgMa = 2500,
            capacityMah = capacityMah
        )
        val chargeTime = estimator.computeTimeToFullMillis(60, capacityMah)
        assertTrue(chargeTime > 0)
    }

    @Test
    fun `test 100 percent charge returns 0 time remaining`() {
        val capacityMah = 5000
        val timeToFull = estimator.computeTimeToFullMillis(100, capacityMah)
        assertEquals(0L, timeToFull)
    }

    @Test
    fun `test system reported charge time is directly prioritized to match lock screen 1 to 1`() {
        val capacityMah = 5000
        val percentage = 72
        val vivoSystemReported15MinMs = 15 * 60 * 1000L // 900,000 ms (15 min)

        val timeToFull = estimator.computeTimeToFullMillis(
            percentage = percentage,
            capacityMah = capacityMah,
            systemReportedMs = vivoSystemReported15MinMs
        )

        assertEquals("Should directly match the 15 min reported by system HAL", vivoSystemReported15MinMs, timeToFull)

        val timeTo80 = estimator.computeTimeToEightyMillis(
            percentage = percentage,
            capacityMah = capacityMah,
            systemReportedMs = vivoSystemReported15MinMs
        )

        // At 72%, (80 - 72) / (100 - 72) * 15 min = 8 / 28 * 15 min ~= 4.28 min (~257,142 ms)
        val minutesTo80 = timeTo80 / (60 * 1000.0)
        assertTrue("Time to 80% should be ~4.3 minutes, got $minutesTo80", minutesTo80 in 4.0..5.0)
    }

    @Test
    fun `test time to 80 is zero when percentage is already at or above 80`() {
        val capacityMah = 5000
        val timeTo80 = estimator.computeTimeToEightyMillis(
            percentage = 82,
            capacityMah = capacityMah,
            systemReportedMs = 600_000L
        )
        assertEquals(0L, timeTo80)
    }

    @Test
    fun `test concurrent multi-threaded updates and sorting do not throw NullPointerException`() {
        val capacityMah = 5000
        val threads = (1..6).map { threadIdx ->
            Thread {
                for (i in 1..200) {
                    val isCharging = i % 2 == 0
                    val current = if (isCharging) 2000 + (threadIdx * 50) else 400 + (threadIdx * 20)
                    estimator.update(
                        isCharging = isCharging,
                        percentage = 50 + (i % 30),
                        currentNowMa = current,
                        currentAvgMa = current,
                        capacityMah = capacityMah
                    )
                    estimator.computeTimeToFullMillis(60, capacityMah)
                    estimator.computeTimeRemainingMillis(50, capacityMah)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}
