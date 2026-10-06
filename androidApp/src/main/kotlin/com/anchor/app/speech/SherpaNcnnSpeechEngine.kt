package com.anchor.app.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.k2fsa.sherpa.ncnn.DecoderConfig
import com.k2fsa.sherpa.ncnn.FeatureExtractorConfig
import com.k2fsa.sherpa.ncnn.ModelConfig
import com.k2fsa.sherpa.ncnn.RecognizerConfig
import com.k2fsa.sherpa.ncnn.SherpaNcnn
import java.io.File
import java.util.concurrent.Executors

const val SHERPA_MODEL_DIR = "sherpa-ncnn-streaming-zipformer-zh-14M-2023-02-23"

private const val TAG = "AnchorSpeech"

/** assets 里是否带模型（构建任务漏装时引擎选择会跳过 sherpa）。 */
fun sherpaModelPresent(context: Context): Boolean = try {
    context.assets.list(SHERPA_MODEL_DIR)?.contains("tokens.txt") == true
} catch (_: Exception) {
    false
}

/**
 * ncnn 的 OpenMP 运行时与线程绑定：第二次采集若在不同的 OS 线程进入并行区，
 * kmp 亲和性断言会 SIGABRT 直接杀进程。因此 sherpa 的所有调用固定在进程唯一的
 * 单线程执行器上，recognizer 常驻并在每次采集前 reset 复用。
 */
private val sherpaExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "sherpa-speech") }
private var sharedRecognizer: SherpaNcnn? = null

/**
 * sherpa-ncnn 流式转写引擎：AudioRecord（16kHz 单声道）喂数据给流式 Zipformer 出
 * 实时 partial；同时并行跑一路 MediaRecorder（同应用双路采集）产 m4a——文字与录音同时产出。
 */
class SherpaNcnnSpeechEngine(
    private val context: Context,
    private val sampleRate: Int = 16_000,
    private val maxCaptureMillis: Long = 10 * 60_000L,
) : SpeechEngine {
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var stopRequested = false

    @Volatile
    private var released = false

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
        onError: (String) -> Unit,
    ) {
        stopRequested = false
        released = false
        sherpaExecutor.execute {
            runCatching { capture(onPartial, onFinal) }.onFailure { cause ->
                Log.w(TAG, "sherpa capture 失败：${cause.message}")
                if (!released) main.post { onError(cause.message ?: "语音转写初始化失败。") }
            }
        }
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO 由 MainActivity 运行时把关
    private fun capture(
        onPartial: (String) -> Unit,
        onFinal: (SpeechFinalResult) -> Unit,
    ) {
        val recognizer = sharedRecognizer ?: SherpaNcnn(
            RecognizerConfig(
                featConfig = FeatureExtractorConfig(sampleRate = sampleRate.toFloat(), featureDim = 80),
                modelConfig = ModelConfig(
                    encoderParam = "$SHERPA_MODEL_DIR/encoder_jit_trace-pnnx.ncnn.param",
                    encoderBin = "$SHERPA_MODEL_DIR/encoder_jit_trace-pnnx.ncnn.bin",
                    decoderParam = "$SHERPA_MODEL_DIR/decoder_jit_trace-pnnx.ncnn.param",
                    decoderBin = "$SHERPA_MODEL_DIR/decoder_jit_trace-pnnx.ncnn.bin",
                    joinerParam = "$SHERPA_MODEL_DIR/joiner_jit_trace-pnnx.ncnn.param",
                    joinerBin = "$SHERPA_MODEL_DIR/joiner_jit_trace-pnnx.ncnn.bin",
                    tokens = "$SHERPA_MODEL_DIR/tokens.txt",
                    numThreads = 2,
                    useGPU = false, // ncnn Vulkan 在部分 ROM 上初始化不稳，CPU 对 14M 模型足够
                ),
                decoderConfig = DecoderConfig(method = "greedy_search", numActivePaths = 1),
            ),
            assetManager = context.assets,
        ).also { sharedRecognizer = it }
        recognizer.reset(recreate = true)

        // 并行一路 MediaRecorder 出 m4a；失败只影响录音，不影响转写。
        var recorder: MediaRecorder? = null
        var recorderFile: File? = null
        runCatching {
            val directory = File(context.filesDir, "voice-notes").apply { mkdirs() }
            val file = File(directory, "voice-${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        while (!stopRequested && !released) {
            val read = record.read(chunk, 0, chunk.size)
            if (read <= 0) continue
            recognizer.acceptSamples(FloatArray(read) { chunk[it] / 32768f })
            while (recognizer.isReady()) recognizer.decode()
            val text = (settledText + " " + recognizer.text).trim()
            if (text.isNotEmpty() && text != lastPartial) {
                lastPartial = text
                main.post { if (!released && !stopRequested) onPartial(text) }
            }
            if (recognizer.isEndpoint()) {
                // 端点判定（默认 2.4s 静音）：把已出文本收进 settledText 再复位，长停顿不丢字。
                settledText = text
                recognizer.reset()
                lastPartial = settledText
            }
            if (System.currentTimeMillis() - captureStartedAt >= maxCaptureMillis) stopRequested = true
        }

        record.stop()
        record.release()

        val text = (settledText + " " + recognizer.text).trim()
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
            main.post { onFinal(SpeechFinalResult(text, audioFile)) }
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
