/*
 * sherpa-onnx Android Kotlin API 包装类。
 *
 * 来源：https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8/sherpa-onnx/kotlin-api
 * 许可：Apache License 2.0（Copyright 2021-2025 Xiaomi Corporation, csukuangfj, ksung）
 *
 * 包名 com.k2fsa.sherpa.onnx 不可修改：JNI 原生符号按该包路径绑定。
 */
package com.k2fsa.sherpa.onnx

data class FeatureConfig(
    var sampleRate: Int = 16000,
    var featureDim: Int = 80,
    var dither: Float = 0.0f
)
