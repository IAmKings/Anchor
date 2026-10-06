package com.anchor.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechEngineSelectionTest {
    @Test
    fun platformEngineWinsWhenOnDeviceRecognitionIsAvailable() {
        assertEquals(
            SpeechEngineKind.Platform,
            selectSpeechEngineKind(apiLevel = 31, onDeviceRecognitionAvailable = true, sherpaModelPresent = true),
        )
        assertEquals(
            SpeechEngineKind.Platform,
            selectSpeechEngineKind(apiLevel = 36, onDeviceRecognitionAvailable = true, sherpaModelPresent = false),
        )
    }

    @Test
    fun belowApi31FallsBackToSherpa() {
        assertEquals(
            SpeechEngineKind.Sherpa,
            selectSpeechEngineKind(apiLevel = 30, onDeviceRecognitionAvailable = true, sherpaModelPresent = true),
        )
        assertEquals(
            SpeechEngineKind.Sherpa,
            selectSpeechEngineKind(apiLevel = 26, onDeviceRecognitionAvailable = false, sherpaModelPresent = true),
        )
    }

    @Test
    fun withoutRecognitionServiceOrModelFallsToRecordingOnly() {
        assertNull(selectSpeechEngineKind(apiLevel = 36, onDeviceRecognitionAvailable = false, sherpaModelPresent = false))
        assertNull(selectSpeechEngineKind(apiLevel = 28, onDeviceRecognitionAvailable = false, sherpaModelPresent = false))
    }
}
