/*
 * sherpa-onnx Android Kotlin API 包装类（v1.13.8，Apache-2.0）。包名不可修改，见 FeatureConfig.kt 头注。
 * 同音字替换器需要额外词典/FST 模型，当前默认关闭（字段留空）。
 */
package com.k2fsa.sherpa.onnx

data class HomophoneReplacerConfig(
    var dictDir: String = "",
    var lexicon: String = "",
    var ruleFsts: String = "",
)
