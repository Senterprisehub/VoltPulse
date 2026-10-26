package com.example.hardware

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatteryAlertConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoStopChargingControllerTest {

    private lateinit var context: Context
    private lateinit var controller: AutoStopChargingController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = AutoStopChargingController(context)
    }

    @Test
    fun `test auto-stop default config values`() {
        val config = BatteryAlertConfig()
        assertTrue(config.isAutoStopOverchargeEnabled)
        assertTrue(config.isRootCutoffEnabled)
        assertFalse(config.isSmartPlugWebhookEnabled)
        assertEquals("", config.smartPlugWebhookUrl)
        assertEquals("POST", config.smartPlugWebhookMethod)
        assertTrue(config.isFullScreenGuardEnabled)
    }

    @Test
    fun `test executeAutoStop returns valid execution report`() = runBlocking {
        val config = BatteryAlertConfig(
            isAutoStopOverchargeEnabled = true,
            isRootCutoffEnabled = false,
            isSmartPlugWebhookEnabled = false,
            isFullScreenGuardEnabled = true
        )

        val result = controller.executeAutoStop(config)
        assertNotNull(result)
        assertFalse(result.isRootCutoffAttempted)
        assertFalse(result.isWebhookDispatched)
        assertTrue(result.isFullScreenGuardTriggered)
        assertTrue(result.message.contains("100% Full Charge Overcharge Protection"))
    }

    @Test
    fun `test webhook validation rejects invalid schema`() = runBlocking {
        val (success, code, msg) = controller.dispatchSmartPlugWebhook("ftp://invalid-url", "POST")
        assertFalse(success)
        assertTrue(msg.contains("Invalid URL schema"))
    }

    @Test
    fun `test restoreCharging executes cleanly`() = runBlocking {
        val config = BatteryAlertConfig()
        val success = controller.restoreCharging(config)
        assertTrue(success)
    }
}
