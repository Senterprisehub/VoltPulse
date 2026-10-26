package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceAlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceAlertManager", "Failed to instantiate TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.getDefault()
                }
                engine.setSpeechRate(0.95f) // Clear, natural pacing
                engine.setPitch(1.0f)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    engine.setAudioAttributes(audioAttributes)
                }

                isInitialized = true
                pendingSpeech?.let { text ->
                    speak(text)
                    pendingSpeech = null
                }
            }
        }
    }

    fun speakFullChargeAlert() {
        speak("Device fully charged, remove the cable.")
    }

    fun speakLowBatteryAlert(level: Int = 20) {
        when (level) {
            5 -> speak("Critical battery at 5 percent, plug in charger immediately.")
            10 -> speak("Low battery at 10 percent, plug in for charging.")
            else -> speak("Low battery, plug in for charging.")
        }
    }

    fun speakEightyPercentAlert() {
        speak("Battery charged to 80 percent, unplug charger to protect battery health.")
    }

    fun speakCableConnectedAlert() {
        speak("Charging cable connected.")
    }

    fun speakCableDisconnectedAlert() {
        speak("Charging Cable disconnected.")
    }

    fun speak(text: String) {
        // Also play standard notification chime as an immediate audio cue
        playChime()

        if (!isInitialized || tts == null) {
            pendingSpeech = text
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "voltpulse_alert_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                val params = HashMap<String, String>().apply {
                    put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString())
                }
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            }
        } catch (e: Exception) {
            Log.e("VoiceAlertManager", "TTS speak failed", e)
        }
    }

    private fun playChime() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isInitialized = false
    }
}
