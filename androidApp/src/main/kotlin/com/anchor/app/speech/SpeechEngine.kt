package com.anchor.app.speech

import android.content.Context

/** 一次语音转写的最终产物。[text] 为转写文本（可能为空串）；[decodeLoad] 为解码耗时/音频时长（可选）。 */
data class SpeechFinalResult(
    val text: String,
    val audioFile: java.io.File?,
    val decodeLoad: Double? = null,
    /** 端点检测切出的倾诉段落（空表示引擎未分段）。结算时由 L0 启发式加标点。 */
    val segments: List<String> = emptyList(),
)

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

enum class SpeechEngineKind { Platform, SherpaOnnx, SherpaNcnn }

/**
 * 引擎选择（纯函数便于单测）。用户在「我的」下载并启用高精度模型后 onnx 优先；
 * 未启用走平台端侧识别（API 31+ 且设备带服务），否则 14M ncnn；都没有返回 null
 * （上层落纯录音兜底）。
 */
fun selectSpeechEngineKind(
    apiLevel: Int,
    onDeviceRecognitionAvailable: Boolean,
    onnxReady: Boolean,
    onnxEnabled: Boolean,
    ncnnModelPresent: Boolean,
): SpeechEngineKind? = when {
    onnxEnabled && onnxReady -> SpeechEngineKind.SherpaOnnx
    apiLevel >= 31 && onDeviceRecognitionAvailable -> SpeechEngineKind.Platform
    ncnnModelPresent -> SpeechEngineKind.SherpaNcnn
    else -> null
}

// 流式预览的节流阈值（按机型分布定标）：3000 字 = 10 分钟中文语速（4–5 字/秒）的换算上限，
// 也是用户确认的流式底线——下限机型（一加 13 级）单次排版 3–8ms，200ms 节流占帧预算 <3%。
// 超出典型输入 3 倍（1 万字）后进入极端保护。调整只动这两个常量。
const val STREAM_LIMIT_CHARS = 3_000
const val SAFE_LIMIT_CHARS = 10_000

/** partial 送 UI 的间隔：底线内 200ms 流式，越线阶梯降频保护排版。 */
fun partialIntervalMillis(textLength: Int): Long = when {
    textLength < STREAM_LIMIT_CHARS -> 200L
    textLength < SAFE_LIMIT_CHARS -> 500L
    else -> 1_000L
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

/** 按当前设备能力构造引擎；返回 null 表示所有转写路径都不可用（上层落纯录音兜底）。 */
fun createSpeechEngine(context: Context): SpeechEngine? {
    val onnx = AndroidOnnxModelManager(context)
    // <4GB 设备不在本应用的现实机型分布内，隐形保险：不启用 onnx，回落 ncnn/录音。
    val onnxDeviceSupported = runCatching {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        mi.totalMem >= 4L * 1024 * 1024 * 1024
    }.getOrDefault(false)
    val kind = selectSpeechEngineKind(
        apiLevel = android.os.Build.VERSION.SDK_INT,
        onDeviceRecognitionAvailable = android.os.Build.VERSION.SDK_INT >= 31 &&
            android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context),
        onnxReady = onnx.modelReady() && onnxDeviceSupported,
        onnxEnabled = onnx.enabled,
        ncnnModelPresent = sherpaModelPresent(context),
    ) ?: return null
    return when (kind) {
        SpeechEngineKind.SherpaOnnx -> SherpaOnnxSpeechEngine(context)
        SpeechEngineKind.Platform -> PlatformSpeechEngine(context)
        SpeechEngineKind.SherpaNcnn -> SherpaNcnnSpeechEngine(context)
    }
}
