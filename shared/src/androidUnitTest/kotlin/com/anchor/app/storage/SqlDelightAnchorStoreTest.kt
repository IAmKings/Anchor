package com.anchor.app.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.anchor.app.db.AnchorDatabase
import com.anchor.app.safety.SafetyAction
import com.anchor.app.storage.FirstAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightAnchorStoreTest {
    @Test
    fun persistsProfileAndRejectsEmptyEmotionAcrossReopen() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AnchorDatabase.Schema.create(driver)
        val first = SqlDelightAnchorStore(AnchorDatabase(driver).encryptedProbeQueries)
        first.saveUserProfile(
            UserProfile(
                ageGroup = AgeGroup.Adult18Plus,
                onboardingComplete = true,
                firstAnchor = FirstAnchor.EmotionLabel,
                firstAnchorAtMillis = 1_000,
            ),
        )
        first.addCameraLog("10:00 收到回复", "他可能不高兴", 1_500)
        val blocked = first.evaluateAndStore(
            AssessmentInput(answers(9, 0), answers(7, 0), 2_000),
        )
        assertEquals(SafetyAction.Continue, blocked.action)

        val reopened = SqlDelightAnchorStore(AnchorDatabase(driver).encryptedProbeQueries)
        assertEquals(FirstAnchor.EmotionLabel, reopened.userProfile().firstAnchor)
        assertEquals("他可能不高兴", reopened.cameraLogs().single().inference)
        assertEquals(1, reopened.assessments().size)
        val thrown = runCatching { reopened.addEmotionCard("难受", "开会", "没人说话", 3_000) }
        assertTrue(thrown.exceptionOrNull()?.message?.contains("具体情绪词") == true)
    }

    private fun answers(size: Int, total: Int): List<Int> {
        var remaining = total
        return List(size) { minOf(3, remaining).also { remaining -= it } }
    }
}
