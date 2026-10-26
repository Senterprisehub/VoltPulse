package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatteryAlertConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatterySettingsManagerTest {

    private lateinit var context: Context
    private lateinit var settingsManager: BatterySettingsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = BatterySettingsManager.getInstance(context)
    }

    @Test
    fun `test default settings loaded initially`() {
        val config = settingsManager.getConfig()
        assertTrue(config.isEightyPercentAlertEnabled)
        assertTrue(config.isLowBatteryAlertEnabled)
        assertEquals(20, config.lowBatteryThreshold)
        assertTrue(config.isFullChargeAlertEnabled)
        assertTrue(config.isAutoStopOverchargeEnabled)
        assertTrue(config.isVoiceFullChargeEnabled)
        assertTrue(config.isVoiceLowBatteryEnabled)
        assertTrue(config.isVoiceEightyPercentEnabled)
        assertTrue(config.isVoiceCableConnectedEnabled)
        assertTrue(config.isVoiceCableDisconnectedEnabled)
    }

    @Test
    fun `test save and strict persistence across instance reload`() {
        val updatedConfig = BatteryAlertConfig(
            isEightyPercentAlertEnabled = false,
            isLowBatteryAlertEnabled = true,
            lowBatteryThreshold = 15,
            isFullChargeAlertEnabled = false,
            isSoundAlertEnabled = false,
            isVibrationAlertEnabled = true,
            isCableAlertEnabled = false,
            isPersistentNotificationEnabled = true,
            isAutoStopOverchargeEnabled = true,
            isRootCutoffEnabled = false,
            isSmartPlugWebhookEnabled = true,
            smartPlugWebhookUrl = "http://192.168.1.120/relay/0?turn=off",
            smartPlugWebhookMethod = "GET",
            isFullScreenGuardEnabled = false,
            isVoiceFullChargeEnabled = false,
            isVoiceLowBatteryEnabled = true,
            isVoiceEightyPercentEnabled = false,
            isVoiceCableConnectedEnabled = false,
            isVoiceCableDisconnectedEnabled = true
        )

        settingsManager.saveConfig(updatedConfig)

        // Verify currently exposed config
        val current = settingsManager.getConfig()
        assertEquals(15, current.lowBatteryThreshold)
        assertFalse(current.isEightyPercentAlertEnabled)
        assertFalse(current.isFullChargeAlertEnabled)
        assertFalse(current.isSoundAlertEnabled)
        assertTrue(current.isSmartPlugWebhookEnabled)
        assertEquals("http://192.168.1.120/relay/0?turn=off", current.smartPlugWebhookUrl)
        assertEquals("GET", current.smartPlugWebhookMethod)
        assertFalse(current.isFullScreenGuardEnabled)
        assertFalse(current.isVoiceFullChargeEnabled)
        assertTrue(current.isVoiceLowBatteryEnabled)
        assertFalse(current.isVoiceEightyPercentEnabled)
        assertFalse(current.isVoiceCableConnectedEnabled)
        assertTrue(current.isVoiceCableDisconnectedEnabled)

        // Verify reloading from a fresh manager instance preserves all modifications
        val reloadedManager = BatterySettingsManager.getInstance(context)
        val reloaded = reloadedManager.getConfig()
        assertEquals(15, reloaded.lowBatteryThreshold)
        assertEquals("http://192.168.1.120/relay/0?turn=off", reloaded.smartPlugWebhookUrl)
        assertEquals("GET", reloaded.smartPlugWebhookMethod)
        assertFalse(reloaded.isEightyPercentAlertEnabled)
        assertFalse(reloaded.isVoiceFullChargeEnabled)
        assertTrue(reloaded.isVoiceLowBatteryEnabled)
        assertFalse(reloaded.isVoiceEightyPercentEnabled)
        assertFalse(reloaded.isVoiceCableConnectedEnabled)
        assertTrue(reloaded.isVoiceCableDisconnectedEnabled)
    }
}
