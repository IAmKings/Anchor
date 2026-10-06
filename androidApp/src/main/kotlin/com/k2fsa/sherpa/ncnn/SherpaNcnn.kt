/*
 * sherpa-ncnn Android JNI 包装类。
 *
 * 来源：https://github.com/k2-fsa/sherpa-ncnn/tree/v2.1.15/android/SherpaNcnn/app/src/main/java/com/k2fsa/sherpa/ncnn/SherpaNcnn.kt
 * 许可：Apache License 2.0（Copyright 2022-2024 ksung, csukuangfj, Xiaomi Corporation）
 *
 * 包名 com.k2fsa.sherpa.ncnn 不可修改：JNI 原生符号按该包路径绑定
 * （libsherpa-ncnn-jni.so 里的 Java_com_k2fsa_sherpa_ncnn_SherpaNcnn_*）。
 * 相对上游仅删除了未使用的 getModelConfig 等辅助函数，其余保持原样。
 */
package com.k2fsa.sherpa.ncnn

import android.content.res.AssetManager

data class FeatureExtractorConfig(
    var sampleRate: Float,
    var featureDim: Int,
)

data class ModelConfig(
    var encoderParam: String,
    var encoderBin: String,
    var decoderParam: String,
    var decoderBin: String,
    var joinerParam: String,
    var joinerBin: String,
    var tokens: String,
    var numThreads: Int = 1,
    var useGPU: Boolean = true, // If there is a GPU and useGPU true, we will use GPU
)

data class DecoderConfig(
    var method: String = "modified_beam_search", // valid values: greedy_search, modified_beam_search
    var numActivePaths: Int = 4, // used only by modified_beam_search
)

data class RecognizerConfig(
    var featConfig: FeatureExtractorConfig,
    var modelConfig: ModelConfig,
    var decoderConfig: DecoderConfig,
    var enableEndpoint: Boolean = true,
    var rule1MinTrailingSilence: Float = 2.4f,
    var rule2MinTrailingSilence: Float = 1.0f,
    var rule3MinUtteranceLength: Float = 30.0f,
    var hotwordsFile: String = "",
    var hotwordsScore: Float = 1.5f,
)

class SherpaNcnn(
    var config: RecognizerConfig,
    assetManager: AssetManager? = null,
) {
    private val ptr: Long

    init {
        if (assetManager != null) {
            ptr = newFromAsset(assetManager, config)
        } else {
            ptr = newFromFile(config)
        }
    }

    protected fun finalize() {
        delete(ptr)
    }

    fun acceptSamples(samples: FloatArray) =
        acceptWaveform(ptr, samples = samples, sampleRate = config.featConfig.sampleRate)

    fun isReady() = isReady(ptr)

    fun decode() = decode(ptr)

    fun inputFinished() = inputFinished(ptr)
    fun isEndpoint(): Boolean = isEndpoint(ptr)
    fun reset(recreate: Boolean = false) = reset(ptr, recreate = recreate)

    val text: String
        get() = getText(ptr)

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: RecognizerConfig,
    ): Long

    private external fun newFromFile(
        config: RecognizerConfig,
    ): Long

    private external fun delete(ptr: Long)
    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Float)
    private external fun inputFinished(ptr: Long)
    private external fun isReady(ptr: Long): Boolean
    private external fun decode(ptr: Long)
    private external fun isEndpoint(ptr: Long): Boolean
    private external fun reset(ptr: Long, recreate: Boolean)
    private external fun getText(ptr: Long): String

    companion object {
        init {
            System.loadLibrary("sherpa-ncnn-jni")
        }
    }
}
