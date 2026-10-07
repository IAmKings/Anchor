package com.anchor.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * 平台端侧识别引擎（API 31+ 的 createOnDeviceSpeechRecognizer）。系统不暴露音频流，
 * 产物只有文字。partial 结果此前一直被丢弃，这里作为流式文本送出。
 */
class PlatformSpeechEngine(private val context: Context) : SpeechEngine {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var delivered = false

    override fun start(
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        delivered = false
        // 平台端侧识别拿不到音频流，振幅回调不可用（UI 侧降级为静态提示）。
        main.post { begin(onPartial, onFinal, onError) }
    }

    private fun begin(
        onPartial: (String) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        val listener = object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                if (delivered) return
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) onPartial(text)
            }

            override fun onResults(results: Bundle) {
                if (delivered) return
                delivered = true
                val text = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                destroyRecognizer()
                onFinal(SpeechFinalResult(text, null))
            }

            override fun onError(error: Int) {
                if (delivered) return
                delivered = true
                destroyRecognizer()
                onError("端侧识别失败（${describeError(error)}）。")
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { speech ->
            speech.setRecognitionListener(listener)
            speech.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1),
            )
        }
    }

    override fun stop() {
        main.post { runCatching { recognizer?.stopListening() } }
    }

    override fun destroy() {
        main.post { destroyRecognizer() }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "没有听清内容"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限"
        else -> "错误码 $error"
    }
}
