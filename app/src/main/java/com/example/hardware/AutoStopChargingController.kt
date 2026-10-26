package com.example.hardware

import android.content.Context
import android.util.Log
import com.example.model.AutoStopExecutionResult
import com.example.model.BatteryAlertConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AutoStopChargingController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _latestResult = MutableStateFlow<AutoStopExecutionResult?>(null)
    val latestResult: StateFlow<AutoStopExecutionResult?> = _latestResult.asStateFlow()

    private val _isRootAvailable = MutableStateFlow<Boolean?>(null)
    val isRootAvailable: StateFlow<Boolean?> = _isRootAvailable.asStateFlow()

    init {
        scope.launch {
            checkRootAvailability()
        }
    }

    /**
     * Checks if Root (su) binary is present and executable.
     */
    suspend fun checkRootAvailability(): Boolean = withContext(Dispatchers.IO) {
        val rootFound = try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()
            output != null && output.isNotBlank()
        } catch (_: Exception) {
            false
        }
        _isRootAvailable.value = rootFound
        rootFound
    }

    /**
     * Executes the multi-tier auto-stop charging routine when 100% full charge is reached.
     */
    suspend fun executeAutoStop(config: BatteryAlertConfig): AutoStopExecutionResult = withContext(Dispatchers.IO) {
        var rootAttempted = false
        var rootSuccess = false
        var webhookDispatched = false
        var webhookCode: Int? = null
        var webhookMsg: String? = null

        // 1. Tier 1: Hardware Root Kernel PMIC Cutoff
        if (config.isRootCutoffEnabled) {
            rootAttempted = true
            rootSuccess = applyRootChargingSwitch(enableCharging = false)
        }

        // 2. Tier 2: IoT Smart Plug Webhook AC Cutoff
        if (config.isSmartPlugWebhookEnabled && config.smartPlugWebhookUrl.isNotBlank()) {
            webhookDispatched = true
            val (success, code, msg) = dispatchSmartPlugWebhook(
                urlStr = config.smartPlugWebhookUrl,
                method = config.smartPlugWebhookMethod,
                payload = """{"action":"cut_power","event":"battery_100_percent_full","device":"VoltPulse Overcharge Guard"}"""
            )
            webhookCode = code
            webhookMsg = msg
        }

        val summaryMessage = buildString {
            append("100% Full Charge Overcharge Protection Triggered: ")
            if (rootSuccess) {
                append("Root hardware PMIC charging disabled. ")
            } else if (rootAttempted) {
                append("Root PMIC switch N/A (Standard Non-Root OS). ")
            }
            if (webhookDispatched) {
                if (webhookCode in 200..299) {
                    append("Smart Plug webhook sent (HTTP $webhookCode). ")
                } else {
                    append("Smart Plug webhook response ($webhookMsg). ")
                }
            }
            append("Full-screen unplug guard active.")
        }

        val result = AutoStopExecutionResult(
            timestamp = System.currentTimeMillis(),
            isRootCutoffAttempted = rootAttempted,
            isRootCutoffSuccess = rootSuccess,
            isWebhookDispatched = webhookDispatched,
            webhookStatusCode = webhookCode,
            webhookMessage = webhookMsg,
            isFullScreenGuardTriggered = config.isFullScreenGuardEnabled,
            message = summaryMessage
        )

        _latestResult.value = result
        result
    }

    /**
     * Restores normal charging capability at the kernel level when cable is unplugged.
     */
    suspend fun restoreCharging(config: BatteryAlertConfig): Boolean = withContext(Dispatchers.IO) {
        if (config.isRootCutoffEnabled && (_isRootAvailable.value == true)) {
            applyRootChargingSwitch(enableCharging = true)
        } else {
            true
        }
    }

    /**
     * Attempts to write to Linux kernel power supply sysfs nodes via su.
     */
    private fun applyRootChargingSwitch(enableCharging: Boolean): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = OutputStreamWriter(process.outputStream)

            val chargingValue = if (enableCharging) "1" else "0"
            val inputSuspendValue = if (enableCharging) "0" else "1"

            // Linux/Android PMIC battery sysfs control nodes
            val commands = listOf(
                "echo $chargingValue > /sys/class/power_supply/battery/charging_enabled",
                "echo $inputSuspendValue > /sys/class/power_supply/battery/input_suspend",
                "echo $chargingValue > /sys/class/power_supply/battery/battery_charging_enabled",
                "echo $chargingValue > /sys/devices/platform/soc/soc:google,battery/charging_enabled",
                "echo $chargingValue > /sys/class/power_supply/main/charging_enabled",
                "acc -s c=$chargingValue" // ACC module support
            )

            for (cmd in commands) {
                os.write("$cmd\n")
            }
            os.write("exit\n")
            os.flush()
            os.close()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.d("AutoStopCharging", "Root charging switch failed: ${e.message}")
            false
        }
    }

    /**
     * Dispatches HTTP GET or POST request to the smart plug or automation webhook.
     */
    suspend fun dispatchSmartPlugWebhook(
        urlStr: String,
        method: String,
        payload: String = ""
    ): Triple<Boolean, Int?, String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val trimmedUrl = urlStr.trim()
            if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                return@withContext Triple(false, null, "Invalid URL schema (must start with http:// or https://)")
            }

            val url = URL(trimmedUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method.uppercase()
                connectTimeout = 5000
                readTimeout = 5000
                doInput = true
                setRequestProperty("User-Agent", "VoltPulse-Android-Overcharge-Guard/1.0")
                if (method.uppercase() == "POST" || method.uppercase() == "PUT") {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
            }

            if ((method.uppercase() == "POST" || method.uppercase() == "PUT") && payload.isNotBlank()) {
                connection.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }
            }

            val responseCode = connection.responseCode
            val success = responseCode in 200..299
            val responseMessage = if (success) {
                "Success (HTTP $responseCode)"
            } else {
                "Server responded HTTP $responseCode"
            }

            Triple(success, responseCode, responseMessage)
        } catch (e: Exception) {
            Log.e("AutoStopCharging", "Webhook dispatch failed", e)
            Triple(false, null, "Connection failed: ${e.localizedMessage ?: "Timeout"}")
        } finally {
            connection?.disconnect()
        }
    }
}
