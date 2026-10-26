package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.BatterySettingsManager
import com.example.service.BatteryMonitorService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Auto-start persistent lockscreen & notification shade telemetry with saved user settings
                val savedConfig = BatterySettingsManager.getInstance(context).getConfig()
                BatteryMonitorService.start(context, savedConfig)
            }
        }
    }
}
