package com.example.hardware

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatteryHardwareManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test singleton instance identity`() {
        val instance1 = BatteryHardwareManager.getInstance(context)
        val instance2 = BatteryHardwareManager.getInstance(context)
        assertSame("BatteryHardwareManager must be a singleton instance", instance1, instance2)
    }

    @Test
    fun `test initial telemetry populated`() {
        val manager = BatteryHardwareManager.getInstance(context)
        val telemetry = manager.telemetry.value
        assertNotNull(telemetry)
        assertNotNull(telemetry.statusText)
        assertNotNull(manager.deviceInfo)
    }
}
