package com.anchor.app.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SafetyState
import java.nio.charset.StandardCharsets
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidEncryptedProbeStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "m0-encrypted-probe.db"
    private val correctKey = "m0-correct-key".toByteArray()

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun persistsEncryptedDataRejectsWrongKeyAndDeletesFiles() {
        context.deleteDatabase(databaseName)

        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { store ->
            store.write("anchor-private-value")
        }

        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { reopened ->
            assertEquals("anchor-private-value", reopened.read())
        }

        val databaseFile = context.getDatabasePath(databaseName)
        assertTrue(databaseFile.exists())
        val header = databaseFile.inputStream().use { it.readNBytes(16) }
        assertNotEquals("SQLite format 3\u0000", header.toString(StandardCharsets.US_ASCII))

        val wrongKeyFailure = runCatching {
            AndroidEncryptedProbeStore(
                context,
                databaseName,
                "m0-wrong-key".toByteArray(),
            ).use { it.read() }
        }.exceptionOrNull()
        assertNotNull("SQLCipher accepted an incorrect passphrase", wrongKeyFailure)

        val finalStore = AndroidEncryptedProbeStore(context, databaseName, correctKey)
        assertTrue(finalStore.delete())
        assertFalse(databaseFile.exists())
        assertFalse(context.getDatabasePath("$databaseName-wal").exists())
        assertFalse(context.getDatabasePath("$databaseName-shm").exists())
    }

    @Test
    fun migratesEncryptedV1DataToCurrentSchema() {
        context.deleteDatabase(databaseName)
        System.loadLibrary("sqlcipher")
        AndroidSqliteDriver(
            schema = V1Schema,
            context = context,
            name = databaseName,
            factory = SupportOpenHelperFactory(correctKey.copyOf()),
        ).use { driver ->
            driver.execute(
                identifier = null,
                sql = "INSERT INTO encrypted_probe(id, value) VALUES (1, 'legacy-value')",
                parameters = 0,
            )
        }

        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { migrated ->
            assertEquals("legacy-value", migrated.read())
            assertEquals(1L, migrated.migrationMarker())
        }

        val header = context.getDatabasePath(databaseName)
            .inputStream()
            .use { it.readNBytes(16) }
        assertNotEquals("SQLite format 3\u0000", header.toString(StandardCharsets.US_ASCII))
    }

    @Test
    fun persistsAssessmentAndSafetyStateAtomicallyAcrossReopen() {
        context.deleteDatabase(databaseName)
        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { store ->
            val blocked = store.evaluateAndStore(AssessmentInput(
                phq9 = answers(9, 15),
                gad7 = answers(7, 0),
                completedAtMillis = 1_000,
            ))
            assertEquals(SafetyAction.MedicalWaiting, blocked.action)
            store.saveUserProfile(UserProfile(AgeGroup.Adult18Plus, onboardingComplete = true, firstAnchor = FirstAnchor.MicroAction))
            store.addEmotionCard("被轻视", "今天开会", "讲完后没人回应", 1_500)
            store.addCameraLog("10:00 发了消息", "他不想理我", 1_700)
            store.addWorryCard("担心明天的汇报", 1_800, 2_000, "voice.m4a")
            store.addRhythmEntry(1_000, 1_500, 2_000)
        }

        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { reopened ->
            assertEquals(SafetyState(SafetyMode.MedicalWaiting), reopened.safetyState())
            val history = reopened.assessments()
            assertEquals(1, history.size)
            assertEquals(15, history.single().phq9Score)
            assertEquals(AssessmentBand.ModeratelySevere, history.single().phq9Band)
            assertEquals(9, history.single().phq9.size)
            assertEquals(UserProfile(AgeGroup.Adult18Plus, true, FirstAnchor.MicroAction), reopened.userProfile())
            val emotionCard = reopened.emotionCards().single()
            assertEquals("被轻视", emotionCard.emotion)
            reopened.markEmotionCardPassed(emotionCard.id, 1_600)
            assertEquals("他不想理我", reopened.cameraLogs().single().inference)
            val worry = reopened.worryCards().single()
            assertEquals("voice.m4a", worry.audioFileName)
            reopened.resolveWorryAsAction(worry.id, "打开文档写一句话", 1_900)

            reopened.evaluateAndStore(AssessmentInput(answers(9, 0), answers(7, 0), 2_000))
        }

        AndroidEncryptedProbeStore(context, databaseName, correctKey).use { reopened ->
            val firstLow = reopened.safetyState()
            assertEquals(2_000L, firstLow.firstLowAssessmentAtMillis)
            val recovered = reopened.evaluateAndStore(AssessmentInput(
                answers(9, 0),
                answers(7, 0),
                2_000 + SEVEN_DAYS,
            ))
            assertEquals(SafetyAction.Continue, recovered.action)
            assertEquals(SafetyState(), reopened.safetyState())
            assertEquals(3, reopened.assessments().size)
            assertEquals(1_600L, reopened.emotionCards().single().passedAtMillis)
            assertEquals(WorryResolution.Action, reopened.worryCards().single().resolution)
            assertEquals("打开文档写一句话", reopened.microActions().single().title)
            assertEquals(1_500L, reopened.rhythmEntries().single().lightAtMillis)

            val backupJson = buildLocalExport(reopened).json
            reopened.clearAllData()
            assertTrue(reopened.assessments().isEmpty())
            assertEquals(SafetyState(), reopened.safetyState())
            assertEquals(UserProfile(), reopened.userProfile())
            assertTrue(reopened.emotionCards().isEmpty())
            assertTrue(reopened.cameraLogs().isEmpty())
            assertTrue(reopened.worryCards().isEmpty())
            assertTrue(reopened.microActions().isEmpty())
            assertTrue(reopened.rhythmEntries().isEmpty())

            reopened.restoreFromJson(backupJson)
            assertEquals(3, reopened.assessments().size)
            assertEquals(UserProfile(AgeGroup.Adult18Plus, true, FirstAnchor.MicroAction), reopened.userProfile())
            assertEquals(reopened.worryCards().single().id, reopened.microActions().single().sourceWorryId)
            assertEquals(null, reopened.worryCards().single().audioFileName)
        }
    }

    @Test
    fun reusesKeystoreWrappedRandomDatabaseKey() {
        val keyDatabaseName = "keystore-$databaseName"
        context.deleteDatabase(keyDatabaseName)
        val first = AndroidDatabaseKey.getOrCreate(context, keyDatabaseName)
        val second = AndroidDatabaseKey.getOrCreate(context, keyDatabaseName)

        assertEquals(32, first.size)
        assertTrue(first.contentEquals(second))
        first.fill(0)
        second.fill(0)
    }

    private fun answers(size: Int, total: Int): List<Int> {
        var remaining = total
        return List(size) { minOf(3, remaining).also { remaining -= it } }
    }

    private object V1Schema : SqlSchema<QueryResult.Value<Unit>> {
        override val version = 1L

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE encrypted_probe (
                      id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                      value TEXT NOT NULL
                    )
                """.trimIndent(),
                parameters = 0,
            )
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ) = QueryResult.Unit
    }

    private companion object {
        const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1_000
    }
}
