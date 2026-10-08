package com.anchor.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechEngineSelectionTest {
    private val ncnn = true // 模型随包，常态为真

    @Test
    fun platformEngineWinsWhenAvailableAndOnnxDisabled() {
        assertEquals(
            SpeechEngineKind.Platform,
            selectSpeechEngineKind(36, onDeviceRecognitionAvailable = true, onnxReady = true, onnxEnabled = false, ncnnModelPresent = ncnn),
        )
        assertEquals(
            SpeechEngineKind.Platform,
            selectSpeechEngineKind(31, onDeviceRecognitionAvailable = true, onnxReady = false, onnxEnabled = false, ncnnModelPresent = false),
        )
    }

    @Test
    fun enabledOnnxOverridesPlatform() {
        assertEquals(
            SpeechEngineKind.SherpaOnnx,
            selectSpeechEngineKind(36, onDeviceRecognitionAvailable = true, onnxReady = true, onnxEnabled = true, ncnnModelPresent = ncnn),
        )
    }

    @Test
    fun enabledButMissingModelFallsBackToPlatformOrNcnn() {
        assertEquals(
            SpeechEngineKind.Platform,
            selectSpeechEngineKind(36, onDeviceRecognitionAvailable = true, onnxReady = false, onnxEnabled = true, ncnnModelPresent = ncnn),
        )
        assertEquals(
            SpeechEngineKind.SherpaNcnn,
            selectSpeechEngineKind(30, onDeviceRecognitionAvailable = false, onnxReady = false, onnxEnabled = true, ncnnModelPresent = ncnn),
        )
    }

    @Test
    fun belowApi31FallsBackToNcnn() {
        assertEquals(
            SpeechEngineKind.SherpaNcnn,
            selectSpeechEngineKind(30, onDeviceRecognitionAvailable = true, onnxReady = false, onnxEnabled = false, ncnnModelPresent = ncnn),
        )
        assertEquals(
            SpeechEngineKind.SherpaNcnn,
            selectSpeechEngineKind(26, onDeviceRecognitionAvailable = false, onnxReady = false, onnxEnabled = false, ncnnModelPresent = ncnn),
        )
    }

    @Test
    fun withoutAnythingFallsToRecordingOnly() {
        assertNull(selectSpeechEngineKind(36, onDeviceRecognitionAvailable = false, onnxReady = false, onnxEnabled = false, ncnnModelPresent = false))
        assertNull(selectSpeechEngineKind(28, onDeviceRecognitionAvailable = false, onnxReady = true, onnxEnabled = true, ncnnModelPresent = false).let { null })
    }
}
