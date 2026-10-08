/*
 * sherpa-onnx Android Kotlin API 包装类（v1.13.8，Apache-2.0）。包名不可修改，见 FeatureConfig.kt 头注。
 * 标点模型（CT-transformer int8）的词表为同目录 tokens.json——native 按约定自动读取，config 不含 tokens 字段。
 */
package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class OfflinePunctuationModelConfig(
    var ctTransformer: String = "",
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
)


data class OfflinePunctuationConfig(
    var model: OfflinePunctuationModelConfig,
)

class OfflinePunctuation(
    assetManager: AssetManager? = null,
    config: OfflinePunctuationConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
        require(ptr != 0L) {
            "Invalid OfflinePunctuationConfig: failed to create native OfflinePunctuation"
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    fun addPunctuation(text: String) = addPunctuation(ptr, text)

    private external fun delete(ptr: Long)

    private external fun addPunctuation(ptr: Long, text: String): String

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: OfflinePunctuationConfig,
    ): Long

    private external fun newFromFile(
        config: OfflinePunctuationConfig,
    ): Long

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
