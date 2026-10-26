package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BatterySettingsManager
import com.example.hardware.AutoStopChargingController
import com.example.hardware.BatteryHardwareManager
import com.example.model.BatteryAlertConfig
import com.example.model.BatteryTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class BatteryMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var hardwareManager: BatteryHardwareManager
    private lateinit var settingsManager: BatterySettingsManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var voiceAlertManager: VoiceAlertManager
    private lateinit var autoStopController: AutoStopChargingController

    private var alertConfig = BatteryAlertConfig()
    private var lastTriggeredAlert: String? = null
    private var lastTriggeredLowBatteryMilestone: Int? = null
    private var lastChargingState: Boolean? = null
    private var lastCableEventTime: Long = 0L

    // Native event-driven broadcast receiver for 0% CPU background operation
    private val systemBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    triggerCableConnected()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    triggerCableDisconnected()
                }
            }

            hardwareManager.refresh()
            val tel = hardwareManager.telemetry.value

            // Fallback for ACTION_BATTERY_CHANGED if vendor driver omitted power action
            if (action == Intent.ACTION_BATTERY_CHANGED) {
                if (lastChargingState != null && lastChargingState != tel.isCharging) {
                    val now = System.currentTimeMillis()
                    if (now - lastCableEventTime > 2000L) {
                        if (tel.isCharging) {
                            triggerCableConnected()
                        } else {
                            triggerCableDisconnected()
                        }
                    }
                }
            }
            lastChargingState = tel.isCharging

            updateNotificationAndAlarms(tel)
        }
    }

    private fun triggerCableConnected() {
        lastCableEventTime = System.currentTimeMillis()
        if (alertConfig.isCableAlertEnabled && alertConfig.isSoundAlertEnabled && alertConfig.isVoiceCableConnectedEnabled) {
            voiceAlertManager.speakCableConnectedAlert()
        }
        if (alertConfig.isCableAlertEnabled && alertConfig.isVibrationAlertEnabled) {
            vibrateDevice()
        }
    }

    private fun triggerCableDisconnected() {
        lastCableEventTime = System.currentTimeMillis()
        serviceScope.launch {
            autoStopController.restoreCharging(alertConfig)
        }
        if (alertConfig.isCableAlertEnabled && alertConfig.isSoundAlertEnabled && alertConfig.isVoiceCableDisconnectedEnabled) {
            voiceAlertManager.speakCableDisconnectedAlert()
        }
        if (alertConfig.isCableAlertEnabled && alertConfig.isVibrationAlertEnabled) {
            vibrateDevice()
        }
    }

    companion object {
        const val CHANNEL_MONITOR_ID = "voltpulse_live_monitor"
        const val CHANNEL_ALARM_VIBRATE_ID = "voltpulse_alarms_vibrate"
        const val CHANNEL_ALARM_SILENT_VIB_ID = "voltpulse_alarms_silent_vib"
        const val NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TEST_FULL_CHARGE = "com.example.service.ACTION_TEST_FULL_CHARGE"
        const val ACTION_TEST_LOW_BATTERY = "com.example.service.ACTION_TEST_LOW_BATTERY"
        const val ACTION_TEST_80_PERCENT = "com.example.service.ACTION_TEST_80_PERCENT"
        const val ACTION_TEST_CABLE_CONNECTED = "com.example.service.ACTION_TEST_CABLE_CONNECTED"
        const val ACTION_TEST_CABLE_DISCONNECTED = "com.example.service.ACTION_TEST_CABLE_DISCONNECTED"
        const val ACTION_TEST_AUTO_STOP = "com.example.service.ACTION_TEST_AUTO_STOP"

        const val EXTRA_LOW_THRESHOLD = "extra_low_threshold"
        const val EXTRA_ENABLE_80_ALERT = "extra_enable_80_alert"
        const val EXTRA_ENABLE_LOW_ALERT = "extra_enable_low_alert"
        const val EXTRA_ENABLE_FULL_ALERT = "extra_enable_full_alert"
        const val EXTRA_ENABLE_SOUND = "extra_enable_sound"
        const val EXTRA_ENABLE_VIBE = "extra_enable_vibe"
        const val EXTRA_ENABLE_CABLE_ALERT = "extra_enable_cable_alert"
        const val EXTRA_ENABLE_AUTO_STOP = "extra_enable_auto_stop"
        const val EXTRA_ENABLE_ROOT_CUTOFF = "extra_enable_root_cutoff"
        const val EXTRA_ENABLE_SMART_PLUG = "extra_enable_smart_plug"
        const val EXTRA_SMART_PLUG_URL = "extra_smart_plug_url"
        const val EXTRA_SMART_PLUG_METHOD = "extra_smart_plug_method"
        const val EXTRA_ENABLE_FULLSCREEN_GUARD = "extra_enable_fullscreen_guard"

        fun start(context: Context, config: BatteryAlertConfig? = null) {
            config?.let {
                BatterySettingsManager.getInstance(context).saveConfig(it)
            }
            val intent = Intent(context, BatteryMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        hardwareManager = BatteryHardwareManager.getInstance(applicationContext)
        settingsManager = BatterySettingsManager.getInstance(applicationContext)
        alertConfig = settingsManager.getConfig()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        voiceAlertManager = VoiceAlertManager(applicationContext)
        autoStopController = AutoStopChargingController(applicationContext)
        createNotificationChannels()

        // Keep local alertConfig reactive to persistent settings changes
        serviceScope.launch {
            settingsManager.config.collect { updatedConfig ->
                alertConfig = updatedConfig
            }
        }

        // Register passive broadcast receiver for ultra-low battery consumption
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(systemBatteryReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST_FULL_CHARGE -> {
                voiceAlertManager.speakFullChargeAlert()
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            ACTION_TEST_LOW_BATTERY -> {
                voiceAlertManager.speakLowBatteryAlert(alertConfig.lowBatteryThreshold)
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            ACTION_TEST_80_PERCENT -> {
                voiceAlertManager.speakEightyPercentAlert()
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            ACTION_TEST_CABLE_CONNECTED -> {
                voiceAlertManager.speakCableConnectedAlert()
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            ACTION_TEST_CABLE_DISCONNECTED -> {
                voiceAlertManager.speakCableDisconnectedAlert()
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            ACTION_TEST_AUTO_STOP -> {
                serviceScope.launch {
                    autoStopController.executeAutoStop(alertConfig)
                }
                voiceAlertManager.speakFullChargeAlert()
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
            }
            else -> {
                alertConfig = settingsManager.getConfig()
                val initialNotification = buildLiveNotification(hardwareManager.telemetry.value)
                startForeground(NOTIFICATION_ID, initialNotification)
                startAdaptiveTelemetryLoop()
            }
        }
        return START_STICKY
    }

    /**
     * Ultra-low power consumption loop:
     * - When discharging, avoids frequent timer loops completely; updates are driven purely by OS broadcasts.
     * - When charging, updates gently every 5s to refresh live watts and countdown on lock screen.
     */
    private fun startAdaptiveTelemetryLoop() {
        serviceScope.launch {
            while (isActive) {
                val isCharging = hardwareManager.telemetry.value.isCharging
                if (isCharging) {
                    // While charging, update live Watts and countdown periodically
                    delay(5000)
                    hardwareManager.refresh()
                    updateNotificationAndAlarms(hardwareManager.telemetry.value)
                } else {
                    // While discharging on battery, sleep longer to preserve battery life
                    delay(30000)
                    hardwareManager.refresh()
                    updateNotificationAndAlarms(hardwareManager.telemetry.value)
                }
            }
        }
    }

    private fun updateNotificationAndAlarms(tel: BatteryTelemetry) {
        val notification = buildLiveNotification(tel)
        notificationManager.notify(NOTIFICATION_ID, notification)
        checkAlertConditions(tel)
    }

    private fun checkAlertConditions(tel: BatteryTelemetry) {
        val pct = tel.percentage
        if (tel.isCharging) {
            // 80% battery protector alarm
            if (alertConfig.isEightyPercentAlertEnabled && pct >= 80 && pct < 85 && lastTriggeredAlert != "80_PERCENT") {
                lastTriggeredAlert = "80_PERCENT"
                if (alertConfig.isSoundAlertEnabled && alertConfig.isVoiceEightyPercentEnabled) {
                    voiceAlertManager.speakEightyPercentAlert()
                }
                if (alertConfig.isVibrationAlertEnabled) {
                    vibrateDevice()
                }
                postAlarmNotification(
                    "⚡ 80% Battery Limit Reached",
                    "Unplug now to preserve battery health and double cycle life."
                )
            } else if (pct >= 100 && lastTriggeredAlert != "100_PERCENT") {
                lastTriggeredAlert = "100_PERCENT"
                if (alertConfig.isAutoStopOverchargeEnabled) {
                    serviceScope.launch {
                        autoStopController.executeAutoStop(alertConfig)
                    }
                }
                if (alertConfig.isSoundAlertEnabled && alertConfig.isFullChargeAlertEnabled && alertConfig.isVoiceFullChargeEnabled) {
                    // Voice Alert requested by user: "device fully charged, remove the cable."
                    voiceAlertManager.speakFullChargeAlert()
                }
                if (alertConfig.isVibrationAlertEnabled && alertConfig.isFullChargeAlertEnabled) {
                    vibrateDevice()
                }
                postAlarmNotification(
                    "🛑 Battery 100% Fully Charged",
                    "Auto-stop overcharge protection active. Remove the cable."
                )
            }
        } else {
            // Low battery alarm: strictly fixed at 20%, 10%, and 5% milestones only.
            // Intermediate drops (e.g. 19%, 18%, 17%, 14%, etc.) stay completely silent.
            val milestone = when {
                pct <= 5 -> 5
                pct <= 10 -> 10
                pct <= 20 -> 20
                else -> null
            }

            if (alertConfig.isLowBatteryAlertEnabled && milestone != null) {
                val shouldTrigger = when (val last = lastTriggeredLowBatteryMilestone) {
                    null -> true
                    else -> milestone < last
                }

                if (shouldTrigger) {
                    lastTriggeredLowBatteryMilestone = milestone
                    if (alertConfig.isSoundAlertEnabled && alertConfig.isVoiceLowBatteryEnabled) {
                        voiceAlertManager.speakLowBatteryAlert(milestone)
                    }
                    if (alertConfig.isVibrationAlertEnabled) {
                        vibrateDevice()
                    }
                    val title = if (milestone == 5) "🚨 Critical Battery Alert (5%)" else "⚠️ Low Battery Alert ($milestone%)"
                    val message = if (milestone == 5) "Critical battery, connect charger immediately." else "Low battery ($milestone%), plug in for charging."
                    postAlarmNotification(title, message)
                }
            }
        }

        // Reset trigger flags when conditions change
        if (tel.isCharging) {
            lastTriggeredLowBatteryMilestone = null
            if (pct < 75) {
                if (lastTriggeredAlert == "80_PERCENT" || lastTriggeredAlert == "100_PERCENT") {
                    lastTriggeredAlert = null
                }
            }
        } else if (pct > 25) {
            lastTriggeredLowBatteryMilestone = null
            lastTriggeredAlert = null
        }
    }

    private fun vibrateDevice() {
        if (!alertConfig.isVibrationAlertEnabled) return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 350, 150, 350, 150, 600)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 350, 150, 350), -1)
            }
        } catch (_: Exception) {
        }
    }

    private fun postAlarmNotification(title: String, message: String) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (alertConfig.isVibrationAlertEnabled) {
            CHANNEL_ALARM_VIBRATE_ID
        } else {
            CHANNEL_ALARM_SILENT_VIB_ID
        }

        val vibrationPattern = if (alertConfig.isVibrationAlertEnabled) {
            longArrayOf(0, 350, 150, 350, 150, 600)
        } else {
            longArrayOf(0)
        }

        val alarmNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(vibrationPattern)
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID, alarmNotification)
    }

    private fun buildLiveNotification(tel: BatteryTelemetry): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedWatts = String.format(Locale.US, "%.1f", tel.powerWatts)
        val formattedTemp = String.format(Locale.US, "%.1f", tel.temperatureC)

        val title = if (tel.isCharging) {
            "${tel.percentage}% • ${formattedWatts}W • ${tel.chargingSpeedType}"
        } else {
            "${tel.percentage}% • Discharging (${formattedWatts}W)"
        }

        val content = if (tel.isCharging) {
            val timeText = if (tel.timeToFullMillis > 0) formatDuration(tel.timeToFullMillis) else "Calculating..."
            "$timeText until full • $formattedTemp°C"
        } else {
            val timeText = if (tel.timeRemainingMillis > 0) formatDuration(tel.timeRemainingMillis) else "Calculating..."
            "Running time left: $timeText • $formattedTemp°C"
        }

        return NotificationCompat.Builder(this, CHANNEL_MONITOR_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setProgress(100, tel.percentage, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "< 1m"
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                "Live Battery Lock Screen Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live battery percentage, watts, and charging progress on lock screen."
                setShowBadge(false)
                enableVibration(false)
                vibrationPattern = longArrayOf(0)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val alarmVibrateChannel = NotificationChannel(
                CHANNEL_ALARM_VIBRATE_ID,
                "Battery Alarms (With Vibration)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audio & haptic vibration alerts for low battery, 80% cutoff, and full charge."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350, 150, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val alarmSilentVibChannel = NotificationChannel(
                CHANNEL_ALARM_SILENT_VIB_ID,
                "Battery Alarms (Silent Haptics)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audio & visual alerts for low battery, 80% cutoff, and full charge without any vibration."
                enableVibration(false)
                vibrationPattern = longArrayOf(0)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alarmVibrateChannel)
            notificationManager.createNotificationChannel(alarmSilentVibChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        voiceAlertManager.shutdown()
        try {
            unregisterReceiver(systemBatteryReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
