package com.anchor.app.storage

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.anchor.app.db.AnchorDatabase
import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SafetyOutcome
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.SafetyState
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Android SQLCipher-backed store for local Anchor data. */
class AndroidEncryptedProbeStore(
    context: Context,
    private val databaseName: String,
    passphrase: ByteArray,
) : AnchorStore, AutoCloseable {
    private val appContext = context.applicationContext
    private val driver = AndroidSqliteDriver(
        schema = AnchorDatabase.Schema,
        context = appContext,
        name = databaseName,
        factory = SupportOpenHelperFactory(passphrase.copyOf()),
    )
    private val queries = AnchorDatabase(driver).encryptedProbeQueries

    fun write(value: String) {
        queries.store(value)
    }

    fun read(): String? = queries.read().executeAsOneOrNull()

    override fun evaluateAndStore(input: AssessmentInput): SafetyOutcome = queries.transactionWithResult {
        val outcome = SafetyPolicy.evaluate(
            phq9 = input.phq9,
            gad7 = input.gad7,
            completedAtMillis = input.completedAtMillis,
            previous = safetyState(),
            keywordHit = input.keywordHit,
            clarification = input.clarification,
            repeatedLowSelfEvaluation = input.repeatedLowSelfEvaluation,
        )
        queries.insertAssessment(
            completed_at = input.completedAtMillis,
            phq9_answers = input.phq9.joinToString(","),
            gad7_answers = input.gad7.joinToString(","),
            phq9_score = outcome.assessment.phq9Score.toLong(),
            phq9_band = outcome.assessment.phq9Band.name,
            gad7_score = outcome.assessment.gad7Score.toLong(),
            gad7_band = outcome.assessment.gad7Band.name,
            action = outcome.action.storageName(),
        )
        queries.upsertSafetyState(
            mode = outcome.state.mode.name,
            first_low_assessment_at = outcome.state.firstLowAssessmentAtMillis,
        )
        outcome
    }

    override fun safetyState(): SafetyState = queries.readSafetyState { mode, firstLow ->
        SafetyState(SafetyMode.valueOf(mode), firstLow)
    }.executeAsOneOrNull() ?: SafetyState()

    override fun assessments(): List<StoredAssessment> = queries.selectAssessments {
            completedAt, phqAnswers, gadAnswers, phqScore, phqBand, gadScore, gadBand, action ->
        StoredAssessment(
            completedAtMillis = completedAt,
            phq9 = phqAnswers.toAnswers(),
            gad7 = gadAnswers.toAnswers(),
            phq9Score = phqScore.toInt(),
            phq9Band = AssessmentBand.valueOf(phqBand),
            gad7Score = gadScore.toInt(),
            gad7Band = AssessmentBand.valueOf(gadBand),
            action = action.toSafetyAction(),
        )
    }.executeAsList()

    override fun userProfile(): UserProfile = queries.readUserProfile { ageGroup, complete, firstAnchor, crisisRegion ->
        UserProfile(
            ageGroup = ageGroup?.let(AgeGroup::valueOf),
            onboardingComplete = complete == 1L,
            firstAnchor = firstAnchor?.let(FirstAnchor::valueOf),
            crisisRegion = crisisRegion?.let(CrisisRegion::valueOf) ?: CrisisRegion.MainlandChina,
        )
    }.executeAsOneOrNull() ?: UserProfile()

    override fun saveUserProfile(profile: UserProfile) {
        queries.upsertUserProfile(
            age_group = profile.ageGroup?.name,
            onboarding_complete = if (profile.onboardingComplete) 1 else 0,
            first_anchor = profile.firstAnchor?.name,
            crisis_region = profile.crisisRegion.name,
        )
    }

    override fun addEmotionCard(emotion: String, event: String, hardestPart: String, createdAtMillis: Long) {
        validateEmotionCard(emotion, event, hardestPart)
        queries.insertEmotionCard(emotion, event.trim(), hardestPart.trim(), createdAtMillis)
    }

    override fun emotionCards(): List<EmotionCard> = queries.selectEmotionCards(::EmotionCard).executeAsList()

    override fun markEmotionCardPassed(id: Long, passedAtMillis: Long) {
        require(queries.emotionCardExists(id).executeAsOne() == 1L) { "情绪卡片不存在。" }
        queries.markEmotionCardPassed(passedAtMillis, id)
    }

    override fun addCameraLog(fact: String, inference: String, createdAtMillis: Long) {
        validateCameraLog(fact, inference)
        queries.insertCameraLog(
            fact.trim(), inference.trim(), if (cameraFactNeedsHint(fact)) 1 else 0, createdAtMillis,
        )
    }

    override fun cameraLogs(): List<CameraLog> = queries.selectCameraLogs { id, fact, inference, hint, createdAt ->
        CameraLog(id, fact, inference, hint == 1L, createdAt)
    }.executeAsList()

    override fun addWorryCard(content: String, sealedAtMillis: Long, nextSessionAtMillis: Long, audioFileName: String?) {
        validateWorryCard(content, sealedAtMillis, nextSessionAtMillis, audioFileName)
        queries.insertWorryCard(content.trim(), sealedAtMillis, nextSessionAtMillis, audioFileName)
    }

    override fun worryCards(): List<WorryCard> = queries.selectWorryCards { id, content, sealedAt, nextAt, resolution, action, audioFileName ->
        WorryCard(id, content, sealedAt, nextAt, WorryResolution.valueOf(resolution), action, audioFileName)
    }.executeAsList()

    override fun resolveWorryCard(id: Long, resolution: WorryResolution, action: String?) {
        require(resolution != WorryResolution.Pending) { "请选择处理结果。" }
        if (resolution == WorryResolution.Action) require(!action.isNullOrBlank()) { "请写下明天能做的一个动作。" }
        require(queries.pendingWorryCardExists(id).executeAsOne() == 1L) { "忧虑卡片不存在或已处理。" }
        queries.resolveWorryCard(resolution.name, action?.trim(), id)
    }

    override fun dismissWorryCard(id: Long) {
        require(queries.worryCardExists(id).executeAsOne() == 1L) { "忧虑卡片不存在。" }
        val audioFileName = queries.selectWorryAudioFileName(id) { it.orEmpty() }
            .executeAsOneOrNull()
            ?.takeIf(String::isNotEmpty)
        audioFileName?.let {
            val file = File(appContext.filesDir, "voice-notes/$it")
            check(!file.exists() || file.delete()) { "无法删除关联录音。" }
        }
        queries.dismissWorryCard(id)
    }

    override fun resolveWorryAsAction(id: Long, action: String, createdAtMillis: Long) {
        require(action.isNotBlank()) { "请写下明天能做的一个动作。" }
        queries.transaction {
            require(queries.pendingWorryCardExists(id).executeAsOne() == 1L) { "忧虑卡片不存在或已处理。" }
            val title = action.trim()
            queries.insertMicroAction(title, id, createdAtMillis)
            queries.resolveWorryCard(WorryResolution.Action.name, title, id)
        }
    }

    override fun addMicroAction(title: String, createdAtMillis: Long) {
        require(title.isNotBlank()) { "请写下一个低到无需说服自己的动作。" }
        queries.insertMicroAction(title.trim(), null, createdAtMillis)
    }

    override fun microActions(): List<MicroAction> = queries.selectMicroActions {
            id, title, sourceWorryId, createdAt, predicted, startedAt, actual, completedAt ->
        MicroAction(id, title, sourceWorryId, createdAt, predicted?.toInt(), startedAt, actual?.toInt(), completedAt)
    }.executeAsList()

    override fun startMicroAction(id: Long, predictedDifficulty: Int, startedAtMillis: Long) {
        require(predictedDifficulty in 1..10) { "预测困难度必须在 1 到 10 之间。" }
        val action = microActions().firstOrNull { it.id == id }
        require(action != null && action.startedAtMillis == null) { "微行动不存在或已开始。" }
        queries.startMicroAction(predictedDifficulty.toLong(), startedAtMillis, id)
    }

    override fun completeMicroAction(id: Long, actualDifficulty: Int, completedAtMillis: Long) {
        require(actualDifficulty in 1..10) { "实际体感必须在 1 到 10 之间。" }
        val action = microActions().firstOrNull { it.id == id }
        require(action?.startedAtMillis != null && action.completedAtMillis == null) {
            "微行动不存在、尚未开始或已完成。"
        }
        require(completedAtMillis >= action.startedAtMillis) { "完成时间不能早于开始时间。" }
        queries.completeMicroAction(actualDifficulty.toLong(), completedAtMillis, id)
    }

    override fun addRhythmEntry(wakeAtMillis: Long?, lightAtMillis: Long?, createdAtMillis: Long) {
        validateRhythmEntry(wakeAtMillis, lightAtMillis, createdAtMillis)
        queries.insertRhythmEntry(wakeAtMillis, lightAtMillis, createdAtMillis)
    }

    override fun rhythmEntries(): List<RhythmEntry> = queries.selectRhythmEntries(::RhythmEntry).executeAsList()

    override fun addRelationContact(name: String, note: String?, createdAtMillis: Long) {
        validateRelationContact(name)
        queries.insertRelationContact(name.trim(), note?.trim()?.ifBlank { null }, createdAtMillis)
    }

    override fun relationContacts(): List<RelationContact> = queries.selectRelationContacts { id, name, note, monitors, createdAt ->
        RelationContact(id, name, note, monitors?.let { it == 1L }, createdAt)
    }.executeAsList()

    override fun setRelationMonitorsSelf(id: Long, monitorsSelf: Boolean) {
        require(queries.relationContactExists(id).executeAsOne() > 0) { "联系人不存在。" }
        queries.setRelationMonitorsSelf(if (monitorsSelf) 1 else 0, id)
    }

    override fun addRelationEnergy(contactId: Long, mark: EnergyMark, createdAtMillis: Long) {
        require(queries.relationContactExists(contactId).executeAsOne() > 0) { "联系人不存在。" }
        queries.insertRelationEnergy(contactId, mark.name, createdAtMillis)
    }

    override fun relationEnergy(): List<RelationEnergyEntry> = queries.selectRelationEnergy { id, contactId, mark, createdAt ->
        RelationEnergyEntry(id, contactId, EnergyMark.valueOf(mark), createdAt)
    }.executeAsList()

    override fun addAltruismDraw(title: String, kind: AltruismKind, drawnAtMillis: Long) {
        require(title.isNotBlank()) { "请先抽一张小事。" }
        queries.insertAltruismDraw(title.trim(), kind.name, drawnAtMillis)
    }

    override fun altruismDraws(): List<AltruismDraw> = queries.selectAltruismDraws { id, title, kind, drawnAt, felt, completedAt ->
        AltruismDraw(id, title, AltruismKind.valueOf(kind), drawnAt, felt?.let(AltruismFeel::valueOf), completedAt)
    }.executeAsList()

    override fun completeAltruismDraw(id: Long, felt: AltruismFeel, completedAtMillis: Long) {
        queries.completeAltruismDraw(felt.name, completedAtMillis, id)
    }

    override fun clearAllData() {
        queries.transaction {
            queries.clearAssessments()
            queries.clearSafetyState()
            queries.clearUserProfile()
            queries.clearEmotionCards()
            queries.clearCameraLogs()
            queries.clearMicroActions()
            queries.clearRhythmEntries()
            queries.clearWorryCards()
            queries.clearRelationEnergy()
            queries.clearAltruismDraws()
            queries.clearRelationContacts()
            queries.clear()
        }
        val voiceNotes = File(appContext.filesDir, "voice-notes")
        check(!voiceNotes.exists() || voiceNotes.deleteRecursively()) { "无法删除本地录音。" }
    }

    fun restoreFromJson(json: String) {
        val root = JSONObject(json)
        require(root.getInt("exportVersion") in 2..3) { "不支持的备份版本。" }
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
            validateWorryCard(it.getString("content"), it.getLong("sealedAt"), it.getLong("nextSessionAt"), if (it.optBoolean("hasAudio")) "missing-audio" else null)
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
            )
            queries.upsertSafetyState(safety.getString("mode"), safety.nullableLong("firstLowAssessmentAt"))
            assessments.objects().forEach { a -> queries.restoreAssessment(a.getLong("completedAt"), a.getJSONArray("phq9").ints().joinToString(","), a.getJSONArray("gad7").ints().joinToString(","), a.getLong("phq9Score"), a.getString("phq9Band"), a.getLong("gad7Score"), a.getString("gad7Band"), a.getString("action")) }
            emotions.objects().forEach { e -> queries.restoreEmotionCard(e.getLong("id"), e.getString("emotion"), e.getString("event"), e.getString("hardestPart"), e.getLong("createdAt"), e.nullableLong("passedAt")) }
            cameraLogs.objects().forEach { c -> queries.restoreCameraLog(c.getLong("id"), c.getString("fact"), c.getString("inference"), if (c.getBoolean("factNeedsHint")) 1 else 0, c.getLong("createdAt")) }
            worries.objects().forEach { w -> queries.restoreWorryCard(w.getLong("id"), w.getString("content"), w.getLong("sealedAt"), w.getLong("nextSessionAt"), w.getString("resolution"), w.nullableString("action")) }
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

private fun String.toAnswers(): List<Int> = split(',').map(String::toInt)
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONArray.ints(): List<Int> = (0 until length()).map(::getInt)
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

private fun String.toSafetyAction(): SafetyAction = when (this) {
    "Continue" -> SafetyAction.Continue
    "AskClarification" -> SafetyAction.AskClarification
    "CrisisGuidance" -> SafetyAction.CrisisGuidance
    "MedicalWaiting" -> SafetyAction.MedicalWaiting
    "ClinicalReview" -> SafetyAction.ClinicalReview
    else -> error("未知安全动作：$this")
}

private fun SafetyAction.storageName(): String = when (this) {
    SafetyAction.Continue -> "Continue"
    SafetyAction.AskClarification -> "AskClarification"
    SafetyAction.CrisisGuidance -> "CrisisGuidance"
    SafetyAction.MedicalWaiting -> "MedicalWaiting"
    SafetyAction.ClinicalReview -> "ClinicalReview"
}
