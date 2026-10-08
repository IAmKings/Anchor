package com.anchor.app.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.OfflinePunctuation
import com.k2fsa.sherpa.onnx.OfflinePunctuationConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "AnchorOnnxAsr"

/**
 * onnxruntime 的流式识别器常驻进程级单例：162MB 模型冷加载需 2-5 秒（另含标点模型 76MB），
 * 启用开关后由 MainActivity 在启动时与开关翻转时预热，之后按住说话零延迟。
 * 所有 native 调用固定在单线程执行器上串行（预热 / 采集 / 释放互斥）。
 */
object SherpaOnnxRuntime {
    @Volatile
    var warming: Boolean = false
        private set

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "onnx-asr") }
    private var recognizer: OnlineRecognizer? = null
    private var punctuator: OfflinePunctuation? = null

    fun isWarm(): Boolean = recognizer != null

    /** 后台预热；已完成则立即回调。 */
    fun warmUp(context: Context, onDone: (Boolean) -> Unit) {
        executor.execute {
            val result = runCatching {
                ensureRecognizer(context)
                // 标点器随预热一并加载；模型未下载时失败不打断 ASR 预热（标点回落启发式）。
                runCatching { ensurePunctuator(context) }
                    .onFailure { Log.w(TAG, "标点器预热失败：${it.message}") }
            }
            result.onFailure { Log.w(TAG, "warmUp 失败：${it::class.simpleName}: ${it.message}") }
            Log.d(TAG, "warmUp: ok=${result.isSuccess}")
            onDone(result.isSuccess)
        }
    }

    /** 仅在 executor 线程调用。 */
    private fun ensurePunctuator(context: Context): OfflinePunctuation =
        punctuator ?: OfflinePunctuation(
            config = OfflinePunctuationConfig(
                model = OfflinePunctuationModelConfig(
                    ctTransformer = File(
                        File(context.filesDir, "models/$PUNCT_MODEL_DIR"),
                        "model.int8.onnx",
                    ).absolutePath,
                    numThreads = 2,
                ),
            ),
        ).also {
            punctuator = it
            Log.d(TAG, "punctuator ready")
        }

    /** 释放常驻 recognizer（关闭开关 / 低内存时）。 */
    fun release() {
        executor.execute {
            recognizer?.release()
            recognizer = null
            punctuator?.release()
            punctuator = null
            Log.d(TAG, "warmUp: released")
        }
    }

    /** 采集必须在 executor 线程执行（与预热/释放串行）。 */
    fun post(block: () -> Unit) {
        executor.execute(block)
    }

    /** 解码分档（按用户机型分布定标）：≥8GB（正常+常规主体）beam 满血；4–8GB（极低概率）greedy。 */
    internal fun decodingMethod(context: Context): String {
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager ?: return "greedy_search"
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return if (memoryInfo.totalMem >= 8L * 1024 * 1024 * 1024) {
            "modified_beam_search"
        } else {
            "greedy_search"
        }
    }

    /**
     * 标点推理（文本级，3000 字亚秒~秒级）。模型未就绪返回 null（调用方回落 L0）。
     * 排队到同一执行器：与采集/预热/释放互斥。
     */
    fun addPunctuation(context: Context, text: String, onDone: (String?) -> Unit) {
        executor.execute {
            val result = runCatching {
                ensurePunctuator(context).addPunctuation(text)
            }.onFailure { Log.w(TAG, "标点推理失败：${it.message}") }
                .getOrNull()
            onDone(result)
        }
    }

    /** 仅在 executor 线程调用（采集与预热都在其上，天然互斥）。 */
    internal fun ensureRecognizer(context: Context): OnlineRecognizer =
        recognizer ?: OnlineRecognizer(
            config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = modelPath(context, "encoder.int8.onnx"),
                        decoder = modelPath(context, "decoder.onnx"),
                        joiner = modelPath(context, "joiner.int8.onnx"),
                    ),
                    tokens = modelPath(context, "tokens.txt"),
                    numThreads = 2,
                    provider = "cpu",
                ),
                endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(false, 2.4f, 0.0f),
                    rule2 = EndpointRule(true, 1.4f, 0.0f),
                    rule3 = EndpointRule(false, 0.0f, 20.0f),
                ),
                enableEndpoint = true,
                decodingMethod = decodingMethod(context),
                maxActivePaths = 4,
            ),
        ).also {
            recognizer = it
            warming = false
        }

    private fun modelPath(context: Context, name: String): String =
        File(File(context.filesDir, "models/$ONNX_MODEL_DIR"), name).absolutePath
}

/**
 * sherpa-onnx 流式转写引擎（高精度，按需下载启用）：AudioRecord 喂流式 Zipformer 出
 * 实时 partial；并行 MediaRecorder 产 m4a——文字与录音同时产出（与 ncnn 引擎同构）。
 */
class SherpaOnnxSpeechEngine(
    private val context: Context,
    private val sampleRate: Int = 16_000,
    private val maxCaptureMillis: Long = 11 * 60_000L,
) : SpeechEngine {
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var stopRequested = false

    @Volatile
    private var released = false

    override fun start(
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        stopRequested = false
        released = false
        SherpaOnnxRuntime.post {
            runCatching { capture(onPartial, onAmplitude, onFinal) }.onFailure { cause ->
                Log.w(TAG, "onnx capture 失败：${cause.message}")
                if (!released) main.post { onError(cause.message ?: "高精度语音转写初始化失败。") }
            }
        }
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO 由 MainActivity 运行时把关
    private fun capture(
        onPartial: (String) -> Unit,
        onAmplitude: (Float) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
    ) {
        val recognizer = SherpaOnnxRuntime.ensureRecognizer(context)

        // 并行一路 MediaRecorder 出 m4a；失败只影响录音，不影响转写。
        var recorder: MediaRecorder? = null
        var recorderFile: File? = null
        runCatching {
            val directory = File(context.filesDir, "voice-notes").apply { mkdirs() }
            val file = File(directory, "voice-${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            recorderFile = file
        }.onFailure {
            Log.w(TAG, "并行录音启动失败（转写不受影响）：${it.message}")
        }

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 4, 16_384),
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "麦克风被占用或不可用。" }
        record.startRecording()
        val captureStartedAt = System.currentTimeMillis()

        val chunk = ShortArray(1_024)
        var lastPartial = ""
        var settledText = ""
        val segments = mutableListOf<String>()
        var lastPartialPostAt = 0L
        var lastAmplitudePostAt = 0L
        var lastAmplitudePosted = -1f
        // RTF 埋点：解码耗时 / 音频时长，量化不同 SoC 的解码负载。
        var decodeNanos = 0L
        var audioNanos = 0L

        val stream = recognizer.createStream()
        var finalText = ""
        try {
            while (!stopRequested && !released) {
                val read = record.read(chunk, 0, chunk.size)
                if (read <= 0) continue
                val amplitude = (computeAmplitude(chunk, read) * 5f).coerceIn(0f, 1f)
                val now = System.currentTimeMillis()
                if (now - lastAmplitudePostAt >= 80 || kotlin.math.abs(amplitude - lastAmplitudePosted) > 0.15f) {
                    lastAmplitudePostAt = now
                    lastAmplitudePosted = amplitude
                    main.post { if (!released && !stopRequested) onAmplitude(amplitude) }
                }
                stream.acceptWaveform(FloatArray(read) { chunk[it] / 32768f }, sampleRate)
                audioNanos += read.toLong() * 1_000_000_000L / sampleRate
                val decodeStartedAt = System.nanoTime()
                while (recognizer.isReady(stream)) recognizer.decode(stream)
                decodeNanos += System.nanoTime() - decodeStartedAt
                // partial = 已定稿段落 + 当前段：端点复位后前面的文字仍完整出现在输入框里。
                val text = ((if (settledText.isEmpty()) "" else "$settledText ") + recognizer.getResult(stream).text.trim())
                    .trim()
                if (text.isNotEmpty() && text != lastPartial) {
                    lastPartial = text
                    // 三档流式：3000 字内（=10 分钟典型输入）固定 200ms 流式，越线阶梯降频。
                    val now = System.currentTimeMillis()
                    if (now - lastPartialPostAt >= partialIntervalMillis(text.length)) {
                        lastPartialPostAt = now
                        main.post { if (!released && !stopRequested) onPartial(text) }
                    }
                }
                if (recognizer.isEndpoint(stream)) {
                    // 端点判定（2.4s 静音）：收进 settledText 与分段表，长停顿不丢字。
                    settledText = text
                    segments += text
                    recognizer.reset(stream)
                    lastPartial = settledText
                    lastPartialPostAt = 0L // 端点分段立即送出，不等节流窗口
                }
                if (System.currentTimeMillis() - captureStartedAt >= maxCaptureMillis) stopRequested = true
            }
            // 最终文本必须在 stream.release() 之前取：释放后再读 native 指针会 SIGSEGV。
            finalText = recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }

        record.stop()
        record.release()

        if (finalText.isNotEmpty()) segments += finalText
        val text = (settledText + " " + finalText).trim()
        val audioFile = if (!released) {
            val stopped = runCatching { recorder?.stop() }.isSuccess
            recorder?.release()
            recorderFile?.takeIf { stopped && it.length() > 0 }
        } else {
            runCatching { recorder?.stop() }
            recorder?.release()
            recorderFile?.delete()
            null
        }

        if (!released) {
            val rtf = if (audioNanos > 0) decodeNanos.toDouble() / audioNanos else null
            main.post { onFinal(SpeechFinalResult(text, audioFile, rtf, segments.toList())) }
        }
    }

    override fun stop() {
        Log.d(TAG, "capture: stop() 被调用")
        stopRequested = true
    }

    override fun destroy() {
        released = true
        stopRequested = true
    }
}
