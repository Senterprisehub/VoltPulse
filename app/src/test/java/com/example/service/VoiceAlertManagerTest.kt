package com.example.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatteryAlertConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceAlertManagerTest {

    private lateinit var context: Context
    private lateinit var voiceAlertManager: VoiceAlertManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        voiceAlertManager = VoiceAlertManager(context)
    }

    @Test
    fun `test battery alert config has cable alerts enabled by default`() {
        val config = BatteryAlertConfig()
        assertTrue(config.isCableAlertEnabled)
        assertTrue(config.isSoundAlertEnabled)
        assertTrue(config.isFullChargeAlertEnabled)
        assertTrue(config.isEightyPercentAlertEnabled)
        assertTrue(config.isLowBatteryAlertEnabled)
    }

    @Test
    fun `test cable connected and disconnected alert invocations`() {
        assertNotNull(voiceAlertManager)
        // Verify methods execute cleanly without throwing exceptions
        voiceAlertManager.speakCableConnectedAlert()
        voiceAlertManager.speakCableDisconnectedAlert()
    }
}
