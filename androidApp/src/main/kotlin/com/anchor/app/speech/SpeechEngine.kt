package com.anchor.app.speech

import android.content.Context

/** 一次语音转写的最终产物。[text] 为转写文本（可能为空串）。 */
data class SpeechFinalResult(val text: String, val audioFile: java.io.File?)

/**
 * 语音转写引擎。实现方保证所有回调都在主线程。
 */
interface SpeechEngine {
    /**
     * 开始采集并转写。初始化失败通过 [onError] 报告（上层落到纯录音兜底），
     * 部分文本经 [onPartial] 实时送达，停止后经 [onFinal] 一次性给出最终结果；
     * [onAmplitude] 送出归一化录音振幅（0..1，驱动音波动效），无音频流的实现可以不回调。
     */
    fun start(
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
        onError: (String) -> Unit,
    )

    /** 用户停止。引擎在收尾后回调 [onFinal]。 */
    fun stop()

    /** 释放底层资源，放弃尚未产出的结果。 */
    fun destroy()
}

enum class SpeechEngineKind { Platform, Sherpa }

/**
 * 引擎选择（纯函数便于单测）：API 31+ 且设备带端侧识别服务走平台引擎；
 * 否则用打包了 sherpa 模型的流式转写；两者皆无返回 null（上层落纯录音兜底）。
 */
fun selectSpeechEngineKind(
    apiLevel: Int,
    onDeviceRecognitionAvailable: Boolean,
    sherpaModelPresent: Boolean,
): SpeechEngineKind? = when {
    apiLevel >= 31 && onDeviceRecognitionAvailable -> SpeechEngineKind.Platform
    sherpaModelPresent -> SpeechEngineKind.Sherpa
    else -> null
}

/**
 * 16bit PCM 块的归一化 RMS 振幅（0..1）。纯函数便于单测。
 */
fun computeAmplitude(chunk: ShortArray, count: Int = chunk.size): Float {
    if (count <= 0) return 0f
    var sum = 0.0
    for (i in 0 until count) {
        val v = chunk[i] / 32768.0
        sum += v * v
    }
    return kotlin.math.sqrt(sum / count).toFloat().coerceIn(0f, 1f)
}

/** 按当前设备能力构造引擎；返回 null 表示两条转写路径都不可用（上层落纯录音兜底）。 */
fun createSpeechEngine(context: Context): SpeechEngine? {
    val kind = selectSpeechEngineKind(
        apiLevel = android.os.Build.VERSION.SDK_INT,
        onDeviceRecognitionAvailable = android.os.Build.VERSION.SDK_INT >= 31 &&
            android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context),
        sherpaModelPresent = sherpaModelPresent(context),
    ) ?: return null
    return when (kind) {
        SpeechEngineKind.Platform -> PlatformSpeechEngine(context)
        SpeechEngineKind.Sherpa -> SherpaNcnnSpeechEngine(context)
    }
}
