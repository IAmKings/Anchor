package com.anchor.app.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReturnToPracticeCopyTest {
    @Test
    fun recoveredOnlyWhenLeavingMedicalWaiting() {
        val waiting = SafetyState(SafetyMode.MedicalWaiting)
        val normal = SafetyState()
        assertTrue(recoveredToPractice(waiting, normal))
        assertFalse(recoveredToPractice(normal, normal))
        assertFalse(recoveredToPractice(waiting, waiting))
        assertFalse(recoveredToPractice(normal, waiting))
    }

    @Test
    fun welcomeCopyAvoidsPepAndEnglishWelcomeBack() {
        assertEquals("平稳回落，欢迎回归练习", returnToPracticeTitle)
        assertFalse(returnToPracticeBody.contains("Welcome"))
        assertFalse(returnToPracticeBody.contains("提升"))
        assertFalse(practiceReturnedBanner.contains("streak"))
        assertEquals("回到今天", returnHomeLabel)
    }
}
