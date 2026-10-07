package com.anchor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anchor.app.speech.createSpeechEngine
import com.anchor.app.speech.selectSpeechEngineKind
import com.anchor.app.speech.AndroidLocalAudioRecorder
import android.os.Build
import java.io.File

class SpeechProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        resultData = when (intent.action) {
            ACTION_STATUS ->
                "kind=${selectSpeechEngineKind(
                    apiLevel = Build.VERSION.SDK_INT,
                    onDeviceRecognitionAvailable = onDeviceAvailable(context),
                    sherpaModelPresent = sherpaPresent(context),
                )},recording=${recorder != null}," +
                    "result=${preferences(context).getString(RESULT, null)}"
            ACTION_RECOGNIZE -> {
                speech?.destroy()
                val engine = createSpeechEngine(context)
                if (engine == null) {
                    saveResult(context, "fallback=no-engine")
                } else {
                    speech = engine.also { recognizer ->
                        recognizer.start(
                            onPartial = { },
                            onAmplitude = { },
                            onFinal = { final -> saveResult(context, "recognized=${final.text},audio=${final.audioFile != null}") },
                            onError = { saveResult(context, "fallback=$it") },
                        )
                    }
                }
                "recognition-started"
            }
            ACTION_RECORD_START -> {
                recorder?.stop()
                recorder = AndroidLocalAudioRecorder(context).also { it.start() }
                "recording-started"
            }
            ACTION_RECORD_STOP -> {
                val file = recorder?.stop()
                recorder = null
                lastFile = file
                if (file == null) "recording-failed" else
                    "file=${file.absolutePath},bytes=${file.length()}"
            }
            ACTION_CLEAN -> {
                speech?.destroy()
                speech = null
                recorder?.stop()?.delete()
                recorder = null
                lastFile?.delete()
                lastFile = null
                File(context.filesDir, "voice-notes").listFiles()?.forEach(File::delete)
                preferences(context).edit().clear().apply()
                "cleaned"
            }
            else -> "unknown-action"
        }
    }

    private fun onDeviceAvailable(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    private fun sherpaPresent(context: Context): Boolean =
        runCatching {
            context.assets.list("sherpa-ncnn-streaming-zipformer-zh-14M-2023-02-23")
                ?.contains("tokens.txt") == true
        }.getOrDefault(false)

    private fun saveResult(context: Context, value: String) {
        preferences(context).edit().putString(RESULT, value).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("speech-probe", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_STATUS = "com.anchor.app.speech.STATUS_PROBE"
        const val ACTION_RECOGNIZE = "com.anchor.app.speech.RECOGNIZE_PROBE"
        const val ACTION_RECORD_START = "com.anchor.app.speech.RECORD_START_PROBE"
        const val ACTION_RECORD_STOP = "com.anchor.app.speech.RECORD_STOP_PROBE"
        const val ACTION_CLEAN = "com.anchor.app.speech.CLEAN_PROBE"
        private const val RESULT = "result"
        private var speech: com.anchor.app.speech.SpeechEngine? = null
        private var recorder: AndroidLocalAudioRecorder? = null
        private var lastFile: File? = null
    }
}
