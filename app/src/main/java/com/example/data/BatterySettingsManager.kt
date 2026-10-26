package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BatteryAlertConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe persistent settings repository using SharedPreferences.
 * Strict Rule: User settings are permanently stored on disk and will NEVER reset
 * across app restarts, background service restarts, or device reboots.
 */
class BatterySettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _config = MutableStateFlow(loadConfigFromDisk())
    val config: StateFlow<BatteryAlertConfig> = _config.asStateFlow()

    fun getConfig(): BatteryAlertConfig {
        return _config.value
    }

    /**
     * Persists new configuration immediately to disk and broadcasts to all active flows.
     */
    @Synchronized
    fun saveConfig(newConfig: BatteryAlertConfig) {
        prefs.edit().apply {
            putBoolean(KEY_80_ALERT, newConfig.isEightyPercentAlertEnabled)
            putBoolean(KEY_LOW_ALERT, newConfig.isLowBatteryAlertEnabled)
            putInt(KEY_LOW_THRESHOLD, newConfig.lowBatteryThreshold)
            putBoolean(KEY_FULL_ALERT, newConfig.isFullChargeAlertEnabled)
            putBoolean(KEY_SOUND_ALERT, newConfig.isSoundAlertEnabled)
            putBoolean(KEY_VIBE_ALERT, newConfig.isVibrationAlertEnabled)
            putBoolean(KEY_CABLE_ALERT, newConfig.isCableAlertEnabled)
            putBoolean(KEY_PERSISTENT_NOTIF, newConfig.isPersistentNotificationEnabled)
            putBoolean(KEY_AUTO_STOP, newConfig.isAutoStopOverchargeEnabled)
            putBoolean(KEY_ROOT_CUTOFF, newConfig.isRootCutoffEnabled)
            putBoolean(KEY_SMART_PLUG, newConfig.isSmartPlugWebhookEnabled)
            putString(KEY_SMART_PLUG_URL, newConfig.smartPlugWebhookUrl)
            putString(KEY_SMART_PLUG_METHOD, newConfig.smartPlugWebhookMethod)
            putBoolean(KEY_FULLSCREEN_GUARD, newConfig.isFullScreenGuardEnabled)
            putBoolean(KEY_VOICE_FULL_ALERT, newConfig.isVoiceFullChargeEnabled)
            putBoolean(KEY_VOICE_LOW_ALERT, newConfig.isVoiceLowBatteryEnabled)
            putBoolean(KEY_VOICE_80_ALERT, newConfig.isVoiceEightyPercentEnabled)
            putBoolean(KEY_VOICE_CABLE_CONN, newConfig.isVoiceCableConnectedEnabled)
            putBoolean(KEY_VOICE_CABLE_DISCONN, newConfig.isVoiceCableDisconnectedEnabled)
            apply()
        }
        _config.value = newConfig
    }

    private fun loadConfigFromDisk(): BatteryAlertConfig {
        return BatteryAlertConfig(
            isEightyPercentAlertEnabled = prefs.getBoolean(KEY_80_ALERT, true),
            isLowBatteryAlertEnabled = prefs.getBoolean(KEY_LOW_ALERT, true),
            lowBatteryThreshold = prefs.getInt(KEY_LOW_THRESHOLD, 20),
            isFullChargeAlertEnabled = prefs.getBoolean(KEY_FULL_ALERT, true),
            isSoundAlertEnabled = prefs.getBoolean(KEY_SOUND_ALERT, true),
            isVibrationAlertEnabled = prefs.getBoolean(KEY_VIBE_ALERT, true),
            isCableAlertEnabled = prefs.getBoolean(KEY_CABLE_ALERT, true),
            isPersistentNotificationEnabled = prefs.getBoolean(KEY_PERSISTENT_NOTIF, true),
            isAutoStopOverchargeEnabled = prefs.getBoolean(KEY_AUTO_STOP, true),
            isRootCutoffEnabled = prefs.getBoolean(KEY_ROOT_CUTOFF, true),
            isSmartPlugWebhookEnabled = prefs.getBoolean(KEY_SMART_PLUG, false),
            smartPlugWebhookUrl = prefs.getString(KEY_SMART_PLUG_URL, "") ?: "",
            smartPlugWebhookMethod = prefs.getString(KEY_SMART_PLUG_METHOD, "POST") ?: "POST",
            isFullScreenGuardEnabled = prefs.getBoolean(KEY_FULLSCREEN_GUARD, true),
            isVoiceFullChargeEnabled = prefs.getBoolean(KEY_VOICE_FULL_ALERT, true),
            isVoiceLowBatteryEnabled = prefs.getBoolean(KEY_VOICE_LOW_ALERT, true),
            isVoiceEightyPercentEnabled = prefs.getBoolean(KEY_VOICE_80_ALERT, true),
            isVoiceCableConnectedEnabled = prefs.getBoolean(KEY_VOICE_CABLE_CONN, true),
            isVoiceCableDisconnectedEnabled = prefs.getBoolean(KEY_VOICE_CABLE_DISCONN, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "voltpulse_user_persistent_settings"

        private const val KEY_80_ALERT = "key_80_percent_alert"
        private const val KEY_LOW_ALERT = "key_low_battery_alert"
        private const val KEY_LOW_THRESHOLD = "key_low_battery_threshold"
        private const val KEY_FULL_ALERT = "key_full_charge_alert"
        private const val KEY_SOUND_ALERT = "key_sound_alert"
        private const val KEY_VIBE_ALERT = "key_vibration_alert"
        private const val KEY_CABLE_ALERT = "key_cable_alert"
        private const val KEY_PERSISTENT_NOTIF = "key_persistent_notification"
        private const val KEY_AUTO_STOP = "key_auto_stop_overcharge"
        private const val KEY_ROOT_CUTOFF = "key_root_cutoff"
        private const val KEY_SMART_PLUG = "key_smart_plug_webhook"
        private const val KEY_SMART_PLUG_URL = "key_smart_plug_url"
        private const val KEY_SMART_PLUG_METHOD = "key_smart_plug_method"
        private const val KEY_FULLSCREEN_GUARD = "key_fullscreen_guard"

        private const val KEY_VOICE_FULL_ALERT = "key_voice_full_alert"
        private const val KEY_VOICE_LOW_ALERT = "key_voice_low_alert"
        private const val KEY_VOICE_80_ALERT = "key_voice_80_alert"
        private const val KEY_VOICE_CABLE_CONN = "key_voice_cable_conn"
        private const val KEY_VOICE_CABLE_DISCONN = "key_voice_cable_disconn"

        @Volatile
        private var instance: BatterySettingsManager? = null

        fun getInstance(context: Context): BatterySettingsManager {
            return instance ?: synchronized(this) {
                instance ?: BatterySettingsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
