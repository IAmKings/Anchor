package com.anchor.app.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlin.concurrent.thread

private const val TAG = "AnchorOnnx"

const val ONNX_MODEL_DIR = "sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30"
private const val ONNX_ARCHIVE_BYTES = 132_634_597L
private const val ONNX_ARCHIVE_SHA256 = "5a2832047ea1f97dd0dc595b816c230c4bafad65cfc0341fa57517cadc50afd0"
private const val MIN_FREE_BYTES = 450L * 1024 * 1024

// 标点模型（CT-transformer zh-en int8；词表 tokens.json 与模型同目录由 native 自动读取）。
private const val PUNCT_ONNX_BYTES = 75_519_198L
private const val PUNCT_ONNX_SHA256 = "65a3fb9f5ad7bfb96bf69e0dc4481df97f6ee60513c1d94ce981ba6effd524b1"
private const val PUNCT_TOKENS_SHA256 = "c960ab87bccea4aa15cf49a59f71973c2c330b46668048cd8da253749ec71ee3"
private const val PUNCT_ONNX = "model.int8.onnx"
private const val PUNCT_TOKENS = "tokens.json"
const val PUNCT_MODEL_DIR = "sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12"
private const val PUNCT_BASE_URL = "https://modelscope.cn/models/ranger810/sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12-int8/resolve/master"

/** 模型归档的下载源，按顺序尝试。 */
private val ONNX_SOURCES = listOf(
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30.tar.bz2",
    "https://modelscope.cn/models/yuhang0329/sherpa-onnx-streaming-zipformer-zh-int8/resolve/master/encoder.int8.onnx",
)

/** 启用高精度后的语音识别状态（「我的」页展示 + 引擎选择）。 */
data class OnnxAsrState(
    val modelReady: Boolean = false,
    val enabled: Boolean = false,
    val downloading: Boolean = false,
    val downloadProgress: Int = 0, // 0..100
    val error: String? = null,
)

/**
 * 高精度 onnx 模型的按需下载管理：双源（GitHub → ModelScope 裸文件）断点续传、
 * sha256 校验、空间预检、解压到 filesDir。所有公开方法线程安全，状态回调在主线程。
 */
class AndroidOnnxModelManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("anchor-onnx-asr", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "onnx-download") }

    @Volatile
    private var cancelRequested = false

    val enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false) && modelReady()

    fun modelReady(): Boolean = modelFile(ONNX_ENCODER).isFile &&
        modelFile(ONNX_DECODER).isFile &&
        modelFile(ONNX_JOINER).isFile &&
        modelFile(ONNX_TOKENS).isFile

    /** 标点模型（随高精度包下载；缺失只影响标点质量，不影响转写）。 */
    fun punctReady(): Boolean = punctFile(PUNCT_ONNX).isFile &&
        punctFile(PUNCT_TOKENS).isFile

    fun currentState(): OnnxAsrState = OnnxAsrState(
        modelReady = modelReady(),
        enabled = enabled,
        downloading = downloading,
        downloadProgress = progressPercent,
        error = lastError,
    )

    @Volatile
    private var downloading = false

    @Volatile
    private var progressPercent = 0

    @Volatile
    private var lastError: String? = null

    var onStateChanged: ((OnnxAsrState) -> Unit)? = null

    /** 启用开关（UI Switch）。关闭只影响引擎选择，不删模型。 */
    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        notifyChanged()
    }

    fun modelFile(name: String): File =
        File(File(context.filesDir, "models/$ONNX_MODEL_DIR"), name)

    fun punctFile(name: String): File =
        File(File(context.filesDir, "models/$PUNCT_MODEL_DIR"), name)

    /** 模型占用的磁盘（用于「删除模型」展示与删除）。 */
    fun deleteModel() {
        cancelRequested = true
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        File(context.filesDir, "models/$ONNX_MODEL_DIR").deleteRecursively()
        File(context.filesDir, "models/$PUNCT_MODEL_DIR").deleteRecursively()
        partialArchive().delete()
        notifyChanged()
    }

    /** 开始下载（已在下载或模型已就绪时忽略）。 */
    @Synchronized
    fun startDownload() {
        if (downloading || modelReady()) return
        val stat = StatFs(context.filesDir.absolutePath)
        if (stat.availableBytes < MIN_FREE_BYTES) {
            fail("剩余空间不足，需要约 450MB。")
            return
        }
        cancelRequested = false
        downloading = true
        progressPercent = 0
        lastError = null
        notifyChanged()
        io.execute {
            try {
                downloadAndExtract()
                if (cancelRequested) {
                    downloading = false
                    notifyChanged()
                    return@execute
                }
                check(modelReady()) { "解压后模型文件不完整。" }
                // 标点模型失败不阻断转写（回落 L0 启发式），只记日志。
                runCatching { downloadPunct() }
                    .onFailure { Log.w(TAG, "标点模型下载失败（转写不受影响）：${it.message}") }
                if (!cancelRequested) {
                    downloading = false
                    progressPercent = 100
                    notifyChanged()
                } else {
                    downloading = false
                    notifyChanged()
                }
            } catch (e: Exception) {
                Log.w(TAG, "模型下载失败：${e.message}")
                downloading = false
                if (!cancelRequested) fail(e.message ?: "下载失败。")
                else notifyChanged()
            }
        }
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    private fun fail(message: String) {
        downloading = false
        lastError = message
        partialArchive().delete()
        notifyChanged()
    }

    private fun downloadAndExtract() {
        val archive = partialArchive()
        val finalArchive = File(context.cacheDir, "$ONNX_MODEL_DIR.tar.bz2")
        var lastSourceError: String? = null
        for (source in ONNX_SOURCES) {
            if (cancelRequested) return
            try {
                if (source.endsWith(".tar.bz2")) {
                    downloadArchiveWithResume(source, archive)
                    verifyArchive(archive)
                    archive.renameTo(finalArchive).let { ok ->
                        if (!ok) {
                            // rename 跨文件系统失败时退回复制。
                            archive.copyTo(finalArchive, overwrite = true)
                            archive.delete()
                        }
                    }
                    extractArchive(finalArchive)
                    finalArchive.delete()
                    return
                } else {
                    // ModelScope 裸文件源：逐文件下载 + per-file sha256 校验。
                    downloadBareFiles(source)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "下载源失败 $source：${e.message}")
                lastSourceError = e.message
                archive.delete()
                finalArchive.delete()
            }
        }
        throw IOException("所有下载源都失败了（${lastSourceError ?: "未知原因"}）。")
    }

    /** GitHub tarball：断点续传（Range），206 追加 / 200 重来。 */
    private fun downloadArchiveWithResume(url: String, target: File) {
        val expectedTotal = ONNX_ARCHIVE_BYTES
        while (true) {
            if (cancelRequested) return
            val have = if (target.isFile) target.length() else 0L
            if (have >= expectedTotal) return
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "anchor-app")
            if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
            val code = conn.responseCode
            if (code != 206 && code != 200) throw IOException("HTTP $code")
            val append = code == 206 && have > 0
            if (!append) target.delete()
            java.io.FileOutputStream(target, append).use { fos ->
                if (append) fos.channel.position(have)
                conn.inputStream.use { input ->
                    val buf = ByteArray(1 shl 16)
                    var written = 0L
                    while (true) {
                        if (cancelRequested) return
                        val n = input.read(buf)
                        if (n < 0) break
                        fos.write(buf, 0, n)
                        written += n
                        val total = (if (append) have else 0L) + written
                        val percent = (total * 100 / expectedTotal).toInt().coerceIn(0, 99)
                        if (percent != progressPercent) {
                            progressPercent = percent
                            notifyChanged()
                        }
                    }
                }
            }
            if (target.length() >= expectedTotal) return
            // 提前断流：循环里再次带 Range 续传。
        }
    }

    private fun verifyArchive(archive: File) {
        if (archive.length() != ONNX_ARCHIVE_BYTES) {
            throw IOException("下载不完整（${archive.length()} / $ONNX_ARCHIVE_BYTES 字节）。")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        archive.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            var n: Int
            while (input.read(buf).also { n = it } > 0) digest.update(buf, 0, n)
        }
        val actual = digest.digest().joinToString("") { String.format("%02x", it) }
        if (actual != ONNX_ARCHIVE_SHA256) throw IOException("校验和不匹配，下载可能被损坏。")
    }

    private fun extractArchive(archive: File) {
        val work = File(context.cacheDir, "onnx-extract")
        work.deleteRecursively()
        work.mkdirs()
        val process = ProcessBuilder("tar", "-xjf", archive.absolutePath, "-C", work.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        if (code != 0) throw IOException("解压失败（tar 退出码 $code）：$output")
        val srcDir = File(work, ONNX_MODEL_DIR)
        val dstDir = File(context.filesDir, "models/$ONNX_MODEL_DIR")
        dstDir.mkdirs()
        for (name in listOf(ONNX_ENCODER, ONNX_DECODER, ONNX_JOINER, ONNX_TOKENS)) {
            val src = File(srcDir, name)
            if (!src.isFile) throw IOException("模型包缺少 $name")
            src.copyTo(File(dstDir, name), overwrite = true)
        }
        work.deleteRecursively()
    }

    /** 标点模型：逐文件下载 + per-file sha256（ModelScope 单源，国内稳定）。 */
    private fun downloadPunct() {
        val dstDir = File(context.filesDir, "models/$PUNCT_MODEL_DIR")
        dstDir.mkdirs()
        val files = listOf(
            PUNCT_ONNX to PUNCT_ONNX_SHA256,
            PUNCT_TOKENS to PUNCT_TOKENS_SHA256,
        )
        for ((name, sha256) in files) {
            if (cancelRequested) return
            val dst = File(dstDir, name)
            if (dst.isFile && sha256Of(dst) == sha256) continue
            downloadFileResume("$PUNCT_BASE_URL/$name", dst, expectedSha256 = sha256)
        }
    }

    /** ModelScope 裸文件源：逐文件下载 + per-file sha256。 */
    private fun downloadBareFiles(baseUrl: String) {
        val dstDir = File(context.filesDir, "models/$ONNX_MODEL_DIR")
        dstDir.mkdirs()
        val files = listOf(
            ONNX_ENCODER to "5ac51e27981bb4dab01bb9be4958453ba50c3b61c063ddda0eab23fd3671aa4f",
            ONNX_DECODER to "06522ad63cec0fdf6809f4e1db9bb4f7d710c34582e3b35db62ac60eccafac7e",
            ONNX_JOINER to "b34584dc6f561089e1d747fedebb3765f2caa72c927ef54d7ca55e5ae40a814b",
            ONNX_TOKENS to "6193c7ea1c96d0d9a1e9652789b40d13a8a913b434a5451e93158f5a09fd6652",
        )
        var done = 0
        for ((name, sha256) in files) {
            if (cancelRequested) return
            val dst = File(dstDir, name)
            if (dst.isFile && sha256Of(dst) == sha256) {
                done++
                continue
            }
            downloadFileResume("$baseUrl/$name", dst, expectedSha256 = sha256)
            done++
            progressPercent = (done * 100 / files.size).coerceIn(0, 99)
            notifyChanged()
        }
    }

    private fun downloadFileResume(url: String, target: File, expectedSha256: String? = null) {
        while (true) {
            if (cancelRequested) return
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "anchor-app")
            val have = if (target.isFile) target.length() else 0L
            if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
            val code = conn.responseCode
            if (code != 206 && code != 200) throw IOException("HTTP $code")
            val append = code == 206 && have > 0
            java.io.FileOutputStream(target, append).use { fos ->
                conn.inputStream.use { input ->
                    val buf = ByteArray(1 shl 16)
                    var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        if (cancelRequested) return
                        fos.write(buf, 0, n)
                    }
                }
            }
            if (expectedSha256 == null || sha256Of(target) == expectedSha256) return
            target.delete()
            throw IOException("文件校验失败：$url")
        }
    }

    private fun punctPartial(): File = File(context.cacheDir, "punct.part")

    private fun partialArchive(): File = File(context.cacheDir, "$ONNX_MODEL_DIR.tar.bz2.part")

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            var n: Int
            while (input.read(buf).also { n = it } > 0) digest.update(buf, 0, n)
        }
        return digest.digest().joinToString("") { String.format("%02x", it) }
    }

    private fun notifyChanged() {
        main.post { onStateChanged?.invoke(currentState()) }
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val ONNX_ENCODER = "encoder.int8.onnx"
        const val ONNX_DECODER = "decoder.onnx"
        const val ONNX_JOINER = "joiner.int8.onnx"
        const val ONNX_TOKENS = "tokens.txt"
    }
}
