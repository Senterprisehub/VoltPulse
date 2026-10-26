package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryDatabase
import com.example.data.BatteryRepository
import com.example.hardware.BatteryHardwareManager
import com.example.model.BatteryAlertConfig
import com.example.model.BatterySessionLog
import com.example.model.BatteryTelemetry
import com.example.model.BenchmarkEntity
import com.example.model.CableBenchmarkResult
import com.example.model.DeviceInfo
import com.example.service.BatteryMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.data.BatterySettingsManager
import com.example.hardware.AutoStopChargingController
import com.example.model.AutoStopExecutionResult

data class BatteryUiState(
    val telemetry: BatteryTelemetry = BatteryTelemetry(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val alertConfig: BatteryAlertConfig = BatteryAlertConfig(),
    val isBatteryOptimizationIgnored: Boolean = false,
    val isBenchmarking: Boolean = false,
    val benchmarkProgress: Float = 0f,
    val benchmarkStatusText: String = "Ready to test",
    val latestBenchmark: CableBenchmarkResult? = null,
    val benchmarksList: List<BenchmarkEntity> = emptyList(),
    val sessionLogs: List<BatterySessionLog> = emptyList(),
    val isServiceRunning: Boolean = true,
    val isRootAvailable: Boolean = false,
    val latestAutoStopResult: AutoStopExecutionResult? = null,
    val isOverchargeGuardVisible: Boolean = false
)

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BatteryRepository
    private val hardwareManager: BatteryHardwareManager = BatteryHardwareManager.getInstance(application)
    private val settingsManager: BatterySettingsManager = BatterySettingsManager.getInstance(application)
    private val autoStopController: AutoStopChargingController = AutoStopChargingController(application)

    private val _uiState = MutableStateFlow(
        BatteryUiState(
            deviceInfo = hardwareManager.deviceInfo,
            alertConfig = settingsManager.getConfig(),
            isBatteryOptimizationIgnored = hardwareManager.isIgnoringBatteryOptimizations.value
        )
    )
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    init {
        val database = BatteryDatabase.getInstance(application)
        repository = BatteryRepository(database.batteryDao())

        // Start Foreground Service with persistent settings
        startMonitorService()

        // Observe persistent configuration changes
        viewModelScope.launch {
            settingsManager.config.collect { persistedConfig ->
                _uiState.update { it.copy(alertConfig = persistedConfig) }
            }
        }

        // Observe Room DB flows
        viewModelScope.launch {
            repository.benchmarks.collect { list ->
                _uiState.update { it.copy(benchmarksList = list) }
            }
        }

        viewModelScope.launch {
            repository.recentSessions.collect { list ->
                _uiState.update { it.copy(sessionLogs = list) }
            }
        }

        // Observe root & auto-stop states
        viewModelScope.launch {
            autoStopController.isRootAvailable.collect { rootAvailable ->
                _uiState.update { it.copy(isRootAvailable = rootAvailable == true) }
            }
        }

        viewModelScope.launch {
            autoStopController.latestResult.collect { result ->
                if (result != null) {
                    _uiState.update { it.copy(latestAutoStopResult = result) }
                }
            }
        }

        // Active 1.5s hardware refresh loop when ViewModel is active
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                hardwareManager.refresh()
                val currentTel = hardwareManager.telemetry.value
                val isOptIgnored = hardwareManager.isIgnoringBatteryOptimizations.value

                val is100PercentCharging = currentTel.percentage >= 100 && currentTel.isCharging
                val shouldShowGuard = is100PercentCharging && _uiState.value.alertConfig.isAutoStopOverchargeEnabled && _uiState.value.alertConfig.isFullScreenGuardEnabled

                _uiState.update {
                    it.copy(
                        telemetry = currentTel,
                        isBatteryOptimizationIgnored = isOptIgnored,
                        isOverchargeGuardVisible = if (!currentTel.isCharging) false else (it.isOverchargeGuardVisible || shouldShowGuard)
                    )
                }

                delay(1500)
            }
        }

        // Periodic historical snapshot logger to Room (every 5 minutes or on change)
        viewModelScope.launch(Dispatchers.IO) {
            var lastRecordedLevel = -1
            while (isActive) {
                val tel = hardwareManager.telemetry.value
                if (tel.percentage != lastRecordedLevel) {
                    lastRecordedLevel = tel.percentage
                    repository.logSession(
                        BatterySessionLog(
                            eventType = if (tel.isCharging) "CHARGING" else "DISCHARGING",
                            batteryLevel = tel.percentage,
                            voltageMv = tel.voltageMv,
                            currentMa = tel.currentMa,
                            powerWatts = tel.powerWatts,
                            temperatureC = tel.temperatureC,
                            plugType = tel.plugType.displayName
                        )
                    )
                }
                delay(60000) // Check snapshot every 1 minute
            }
        }
    }

    fun refreshTelemetry() {
        hardwareManager.refresh()
        _uiState.update {
            it.copy(
                telemetry = hardwareManager.telemetry.value,
                isBatteryOptimizationIgnored = hardwareManager.isIgnoringBatteryOptimizations.value
            )
        }
    }

    fun startMonitorService() {
        val app = getApplication<Application>()
        BatteryMonitorService.start(app, _uiState.value.alertConfig)
        _uiState.update { it.copy(isServiceRunning = true) }
    }

    fun stopMonitorService() {
        val app = getApplication<Application>()
        BatteryMonitorService.stop(app)
        _uiState.update { it.copy(isServiceRunning = false) }
    }

    fun toggleService() {
        if (_uiState.value.isServiceRunning) {
            stopMonitorService()
        } else {
            startMonitorService()
        }
    }

    fun toggleEightyPercentAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isEightyPercentAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleLowBatteryAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isLowBatteryAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun setLowBatteryThreshold(threshold: Int) {
        val newConfig = _uiState.value.alertConfig.copy(lowBatteryThreshold = threshold)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleFullChargeAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isFullChargeAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleSoundAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isSoundAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVibrationAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVibrationAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVoiceFullCharge(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVoiceFullChargeEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVoiceLowBattery(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVoiceLowBatteryEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVoice80Percent(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVoiceEightyPercentEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVoiceCableConnected(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVoiceCableConnectedEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleVoiceCableDisconnected(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isVoiceCableDisconnectedEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    private fun syncServiceConfig(config: BatteryAlertConfig) {
        settingsManager.saveConfig(config)
        if (_uiState.value.isServiceRunning) {
            BatteryMonitorService.start(getApplication(), config)
        }
    }

    fun testFullChargeVoiceAlert() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_FULL_CHARGE
        }
        app.startService(intent)
    }

    fun testLowBatteryVoiceAlert() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_LOW_BATTERY
        }
        app.startService(intent)
    }

    fun testEightyPercentVoiceAlert() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_80_PERCENT
        }
        app.startService(intent)
    }

    fun toggleCableAlert(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isCableAlertEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleAutoStop(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isAutoStopOverchargeEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleRootCutoff(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isRootCutoffEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleSmartPlugWebhook(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isSmartPlugWebhookEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun updateSmartPlugWebhookUrl(url: String) {
        val newConfig = _uiState.value.alertConfig.copy(smartPlugWebhookUrl = url)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun updateSmartPlugWebhookMethod(method: String) {
        val newConfig = _uiState.value.alertConfig.copy(smartPlugWebhookMethod = method)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun toggleFullScreenGuard(enabled: Boolean) {
        val newConfig = _uiState.value.alertConfig.copy(isFullScreenGuardEnabled = enabled)
        _uiState.update { it.copy(alertConfig = newConfig) }
        syncServiceConfig(newConfig)
    }

    fun dismissOverchargeGuard() {
        _uiState.update { it.copy(isOverchargeGuardVisible = false) }
    }

    fun testAutoStopSequence() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_AUTO_STOP
        }
        app.startService(intent)
        viewModelScope.launch {
            val result = autoStopController.executeAutoStop(_uiState.value.alertConfig)
            _uiState.update {
                it.copy(
                    latestAutoStopResult = result,
                    isOverchargeGuardVisible = true
                )
            }
        }
    }

    fun testSmartPlugWebhook(onResult: (Boolean, String) -> Unit) {
        val config = _uiState.value.alertConfig
        viewModelScope.launch {
            val (success, code, msg) = autoStopController.dispatchSmartPlugWebhook(
                urlStr = config.smartPlugWebhookUrl,
                method = config.smartPlugWebhookMethod,
                payload = """{"action":"test_ping","device":"VoltPulse Overcharge Guard","event":"manual_webhook_test"}"""
            )
            val resultMessage = if (success) {
                "✅ Webhook Success! HTTP $code response received from smart socket."
            } else {
                "❌ Webhook Failed: $msg"
            }
            onResult(success, resultMessage)
        }
    }

    fun testCableConnectedVoiceAlert() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_CABLE_CONNECTED
        }
        app.startService(intent)
    }

    fun testCableDisconnectedVoiceAlert() {
        val app = getApplication<Application>()
        val intent = Intent(app, BatteryMonitorService::class.java).apply {
            action = BatteryMonitorService.ACTION_TEST_CABLE_DISCONNECTED
        }
        app.startService(intent)
    }

    fun testAlarmSoundAndVibration() {
        testFullChargeVoiceAlert()
    }

    fun runCableBenchmark() {
        if (_uiState.value.isBenchmarking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBenchmarking = true,
                    benchmarkProgress = 0f,
                    benchmarkStatusText = "Connecting hardware bus..."
                )
            }

            val result = hardwareManager.runCableBenchmark { progress, status ->
                _uiState.update {
                    it.copy(
                        benchmarkProgress = progress,
                        benchmarkStatusText = status
                    )
                }
            }

            // Save to Room DB
            repository.saveBenchmark(
                BenchmarkEntity(
                    watts = result.averageWatts,
                    voltageMv = result.averageVoltageMv,
                    currentMa = result.averageCurrentMa,
                    voltageSagMv = result.voltageSagMv,
                    qualityGrade = result.qualityGrade,
                    protocol = result.chargerProtocol,
                    assessment = result.assessment
                )
            )

            _uiState.update {
                it.copy(
                    isBenchmarking = false,
                    latestBenchmark = result,
                    benchmarkStatusText = "Benchmark complete: Grade ${result.qualityGrade}"
                )
            }
        }
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to general battery saver settings
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(latestBenchmark = null) }
        }
    }
}
