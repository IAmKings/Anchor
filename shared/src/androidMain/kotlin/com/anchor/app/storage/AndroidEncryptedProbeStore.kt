package com.anchor.app.storage

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.anchor.app.db.AnchorDatabase
import com.anchor.app.db.EncryptedProbeQueries
import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyMode
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Android SQLCipher-backed store for local Anchor data. */
class AndroidEncryptedProbeStore private constructor(
    private val appContext: Context,
    private val databaseName: String,
    private val driver: AndroidSqliteDriver,
    private val queries: EncryptedProbeQueries,
    private val impl: SqlDelightAnchorStore,
) : AnchorStore by impl, AutoCloseable {
    constructor(
        context: Context,
        databaseName: String,
        passphrase: ByteArray,
    ) : this(openAndroidStore(context.applicationContext, databaseName, passphrase))

    private constructor(opened: OpenedAndroidStore) : this(
        opened.appContext,
        opened.databaseName,
        opened.driver,
        opened.queries,
        opened.impl,
    )

    fun write(value: String) = impl.write(value)

    fun read(): String? = impl.read()

    fun restoreFromJson(json: String, restoredAudioNames: Set<String> = emptySet()) {
        val root = JSONObject(json)
        require(root.getInt("exportVersion") in MIN_EXPORT_VERSION..CURRENT_EXPORT_VERSION) { "不支持的备份版本。" }
        val profile = root.getJSONObject("userProfile")
        val safety = root.getJSONObject("safetyState")
        val assessments = root.getJSONArray("assessments")
        val emotions = root.getJSONArray("emotionCards")
        val cameraLogs = root.getJSONArray("cameraLogs")
        val worries = root.getJSONArray("worries")
        val actions = root.getJSONArray("microActions")
        val rhythm = root.getJSONArray("rhythmEntries")

        profile.nullableString("ageGroup")?.let(AgeGroup::valueOf)
        profile.nullableString("firstAnchor")?.let(FirstAnchor::valueOf)
        profile.nullableString("crisisRegion")?.let(CrisisRegion::valueOf)
        SafetyMode.valueOf(safety.getString("mode"))
        assessments.objects().forEach {
            require(it.getJSONArray("phq9").ints().size == 9 && it.getJSONArray("gad7").ints().size == 7)
            AssessmentBand.valueOf(it.getString("phq9Band"))
            AssessmentBand.valueOf(it.getString("gad7Band"))
            it.getString("action").toSafetyAction()
        }
        emotions.objects().forEach { validateEmotionCard(it.getString("emotion"), it.getString("event"), it.getString("hardestPart")) }
        cameraLogs.objects().forEach { validateCameraLog(it.getString("fact"), it.getString("inference")) }
        worries.objects().forEach {
            WorryResolution.valueOf(it.getString("resolution"))
            val restoredName = restoredAudioName(it, restoredAudioNames)
            validateWorryCard(
                it.getString("content"),
                it.getLong("sealedAt"),
                it.getLong("nextSessionAt"),
                if (it.optBoolean("hasAudio")) restoredName ?: "missing-audio" else null,
            )
        }
        rhythm.objects().forEach { validateRhythmEntry(it.nullableLong("wakeAt"), it.nullableLong("lightAt"), it.getLong("createdAt")) }

        queries.transaction {
            queries.clearAssessments(); queries.clearSafetyState(); queries.clearUserProfile()
            queries.clearEmotionCards(); queries.clearCameraLogs(); queries.clearMicroActions()
            queries.clearRhythmEntries(); queries.clearWorryCards()
            queries.clearRelationEnergy(); queries.clearAltruismDraws(); queries.clearRelationContacts()
            queries.clear()
            queries.upsertUserProfile(
                profile.nullableString("ageGroup"),
                if (profile.getBoolean("onboardingComplete")) 1 else 0,
                profile.nullableString("firstAnchor"),
                profile.nullableString("crisisRegion") ?: CrisisRegion.MainlandChina.name,
                profile.nullableLong("firstAnchorAt"),
            )
            queries.upsertSafetyState(safety.getString("mode"), safety.nullableLong("firstLowAssessmentAt"))
            assessments.objects().forEach { a -> queries.restoreAssessment(a.getLong("completedAt"), a.getJSONArray("phq9").ints().joinToString(","), a.getJSONArray("gad7").ints().joinToString(","), a.getLong("phq9Score"), a.getString("phq9Band"), a.getLong("gad7Score"), a.getString("gad7Band"), a.getString("action")) }
            emotions.objects().forEach { e -> queries.restoreEmotionCard(e.getLong("id"), e.getString("emotion"), e.getString("event"), e.getString("hardestPart"), e.getLong("createdAt"), e.nullableLong("passedAt")) }
            cameraLogs.objects().forEach { c -> queries.restoreCameraLog(c.getLong("id"), c.getString("fact"), c.getString("inference"), if (c.getBoolean("factNeedsHint")) 1 else 0, c.getLong("createdAt")) }
            worries.objects().forEach { w ->
                queries.restoreWorryCard(
                    w.getLong("id"),
                    w.getString("content"),
                    w.getLong("sealedAt"),
                    w.getLong("nextSessionAt"),
                    w.getString("resolution"),
                    w.nullableString("action"),
                    restoredAudioName(w, restoredAudioNames),
                )
            }
            actions.objects().forEach { a -> queries.restoreMicroAction(a.getLong("id"), a.getString("title"), a.nullableLong("sourceWorryId"), a.getLong("createdAt"), a.nullableLong("predictedDifficulty"), a.nullableLong("startedAt"), a.nullableLong("actualDifficulty"), a.nullableLong("completedAt")) }
            rhythm.objects().forEach { r -> queries.restoreRhythmEntry(r.getLong("id"), r.nullableLong("wakeAt"), r.nullableLong("lightAt"), r.getLong("createdAt")) }
            root.optJSONArray("relationContacts")?.objects()?.forEach { c ->
                queries.restoreRelationContact(c.getLong("id"), c.getString("name"), c.nullableString("note"), if (c.isNull("monitorsSelf")) null else if (c.getBoolean("monitorsSelf")) 1 else 0, c.getLong("createdAt"))
            }
            root.optJSONArray("relationEnergy")?.objects()?.forEach { e ->
                queries.restoreRelationEnergy(e.getLong("id"), e.getLong("contactId"), e.getString("mark"), e.getLong("createdAt"))
            }
            root.optJSONArray("altruismDraws")?.objects()?.forEach { d ->
                queries.restoreAltruismDraw(d.getLong("id"), d.getString("title"), d.getString("kind"), d.getLong("drawnAt"), d.nullableString("felt"), d.nullableLong("completedAt"))
            }
        }
    }

    internal fun migrationMarker(): Long? =
        queries.readMigrationMarker().executeAsOneOrNull()

    override fun close() {
        driver.close()
    }

    fun delete(): Boolean {
        close()
        return appContext.deleteDatabase(databaseName)
    }

    internal fun databaseFile() = appContext.getDatabasePath(databaseName)

    companion object {
        init {
            System.loadLibrary("sqlcipher")
        }
    }
}

private class OpenedAndroidStore(
    val appContext: Context,
    val databaseName: String,
    val driver: AndroidSqliteDriver,
    val queries: EncryptedProbeQueries,
    val impl: SqlDelightAnchorStore,
)

private fun openAndroidStore(
    appContext: Context,
    databaseName: String,
    passphrase: ByteArray,
): OpenedAndroidStore {
    val driver = AndroidSqliteDriver(
        schema = AnchorDatabase.Schema,
        context = appContext,
        name = databaseName,
        factory = SupportOpenHelperFactory(passphrase.copyOf()),
    )
    val queries = AnchorDatabase(driver).encryptedProbeQueries
    return OpenedAndroidStore(
        appContext = appContext,
        databaseName = databaseName,
        driver = driver,
        queries = queries,
        impl = SqlDelightAnchorStore(
            queries = queries,
            deleteAudioFile = { name ->
                val file = File(appContext.filesDir, "voice-notes/$name")
                check(!file.exists() || file.delete()) { "无法删除关联录音。" }
            },
            clearLocalFiles = {
                val voiceNotes = File(appContext.filesDir, "voice-notes")
                check(!voiceNotes.exists() || voiceNotes.deleteRecursively()) { "无法删除本地录音。" }
            },
        ),
    )
}

private fun restoredAudioName(worry: JSONObject, restoredAudioNames: Set<String>): String? {
    val named = worryAudioExportName(worry.optString("audioName").takeIf { it.isNotBlank() })
    return named?.takeIf { it in restoredAudioNames }
}

private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONArray.ints(): List<Int> = (0 until length()).map(::getInt)
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
