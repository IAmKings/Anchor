/*
 * sherpa-onnx Android Kotlin API 包装类（v1.13.8，Apache-2.0）。包名不可修改，见 FeatureConfig.kt 头注。
 */
package com.k2fsa.sherpa.onnx

data class QnnConfig(
    var backendLib: String = "",
    var contextBinary: String = "",
    var systemLib: String = "",
)
