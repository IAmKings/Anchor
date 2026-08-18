package com.anchor.app.storage

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.Clarification
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyOutcome
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.SafetyState

data class AssessmentInput(
    val phq9: List<Int>,
    val gad7: List<Int>,
    val completedAtMillis: Long,
    val keywordHit: Boolean = false,
    val clarification: Clarification? = null,
    val repeatedLowSelfEvaluation: Boolean = false,
)

data class StoredAssessment(
    val completedAtMillis: Long,
    val phq9: List<Int>,
    val gad7: List<Int>,
    val phq9Score: Int,
    val phq9Band: AssessmentBand,
    val gad7Score: Int,
    val gad7Band: AssessmentBand,
    val action: SafetyAction,
)

enum class AgeGroup { Youth14To17, Adult18Plus }
enum class CrisisRegion { MainlandChina, HongKong, Macau, Taiwan, UnitedStates, UnitedKingdom, Japan, Other }
enum class FirstAnchor { EmotionLabel, FactsJournal, WorryVault, MicroAction, Rhythm, WaveWaiting, SocialEnergy, AltruisticTask }

data class UserProfile(
    val ageGroup: AgeGroup? = null,
    val onboardingComplete: Boolean = false,
    val firstAnchor: FirstAnchor? = null,
    val crisisRegion: CrisisRegion = CrisisRegion.MainlandChina,
)

data class EmotionCard(
    val id: Long,
    val emotion: String,
    val event: String,
    val hardestPart: String,
    val createdAtMillis: Long,
    val passedAtMillis: Long? = null,
)

data class CameraLog(
    val id: Long,
    val fact: String,
    val inference: String,
    val factNeedsHint: Boolean,
    val createdAtMillis: Long,
)

enum class WorryResolution { Pending, Action, Unsolvable }

data class WorryCard(
    val id: Long,
    val content: String,
    val sealedAtMillis: Long,
    val nextSessionAtMillis: Long,
    val resolution: WorryResolution = WorryResolution.Pending,
    val action: String? = null,
    val audioFileName: String? = null,
)

data class MicroAction(
    val id: Long,
    val title: String,
    val sourceWorryId: Long?,
    val createdAtMillis: Long,
    val predictedDifficulty: Int? = null,
    val startedAtMillis: Long? = null,
    val actualDifficulty: Int? = null,
    val completedAtMillis: Long? = null,
)

data class RhythmEntry(
    val id: Long,
    val wakeAtMillis: Long?,
    val lightAtMillis: Long?,
    val createdAtMillis: Long,
)

enum class EnergyMark { Filled, Drained }
enum class AltruismKind { NonSocial, Social }
enum class AltruismFeel { Lighter, Tighter }

data class RelationContact(
    val id: Long,
    val name: String,
    val note: String?,
    val monitorsSelf: Boolean?,
    val createdAtMillis: Long,
)

data class RelationEnergyEntry(
    val id: Long,
    val contactId: Long,
    val mark: EnergyMark,
    val createdAtMillis: Long,
)

data class AltruismDraw(
    val id: Long,
    val title: String,
    val kind: AltruismKind,
    val drawnAtMillis: Long,
    val felt: AltruismFeel? = null,
    val completedAtMillis: Long? = null,
)

val emotionVocabulary = listOf(
    "被轻视", "被排除在外", "没有被认真对待", "被误解", "被拒绝", "被控制", "被背叛", "孤立无援",
    "羞耻", "内疚", "不服气", "委屈", "失望", "嫉妒", "愤怒", "怨恨",
    "心慌", "不安", "担心被发现", "害怕失败", "害怕失去", "失控感", "无助", "绝望",
    "空虚", "虚无", "麻木", "孤独", "疲惫", "厌倦", "局促", "困惑",
)

interface AnchorStore {
    fun evaluateAndStore(input: AssessmentInput): SafetyOutcome
    fun safetyState(): SafetyState
    fun assessments(): List<StoredAssessment>
    fun userProfile(): UserProfile
    fun saveUserProfile(profile: UserProfile)
    fun addEmotionCard(emotion: String, event: String, hardestPart: String, createdAtMillis: Long)
    fun emotionCards(): List<EmotionCard>
    fun markEmotionCardPassed(id: Long, passedAtMillis: Long)
    fun addCameraLog(fact: String, inference: String, createdAtMillis: Long)
    fun cameraLogs(): List<CameraLog>
    fun addWorryCard(content: String, sealedAtMillis: Long, nextSessionAtMillis: Long, audioFileName: String? = null)
    fun worryCards(): List<WorryCard>
    fun resolveWorryCard(id: Long, resolution: WorryResolution, action: String? = null)
    fun resolveWorryAsAction(id: Long, action: String, createdAtMillis: Long)
    fun dismissWorryCard(id: Long)
    fun microActions(): List<MicroAction>
    fun addMicroAction(title: String, createdAtMillis: Long)
    fun startMicroAction(id: Long, predictedDifficulty: Int, startedAtMillis: Long)
    fun completeMicroAction(id: Long, actualDifficulty: Int, completedAtMillis: Long)
    fun addRhythmEntry(wakeAtMillis: Long?, lightAtMillis: Long?, createdAtMillis: Long)
    fun rhythmEntries(): List<RhythmEntry>
    fun addRelationContact(name: String, note: String?, createdAtMillis: Long)
    fun relationContacts(): List<RelationContact>
    fun setRelationMonitorsSelf(id: Long, monitorsSelf: Boolean)
    fun addRelationEnergy(contactId: Long, mark: EnergyMark, createdAtMillis: Long)
    fun relationEnergy(): List<RelationEnergyEntry>
    fun addAltruismDraw(title: String, kind: AltruismKind, drawnAtMillis: Long)
    fun altruismDraws(): List<AltruismDraw>
    fun completeAltruismDraw(id: Long, felt: AltruismFeel, completedAtMillis: Long)
    fun clearAllData()
}

class InMemoryAnchorStore : AnchorStore {
    private val history = mutableListOf<StoredAssessment>()
    private var state = SafetyState()
    private var profile = UserProfile()
    private val emotionCards = mutableListOf<EmotionCard>()
    private var nextEmotionCardId = 1L
    private val cameraLogs = mutableListOf<CameraLog>()
    private var nextCameraLogId = 1L
    private val worryCards = mutableListOf<WorryCard>()
    private var nextWorryCardId = 1L
    private val microActions = mutableListOf<MicroAction>()
    private var nextMicroActionId = 1L
    private val rhythmEntries = mutableListOf<RhythmEntry>()
    private var nextRhythmEntryId = 1L
    private val relationContacts = mutableListOf<RelationContact>()
    private var nextRelationContactId = 1L
    private val relationEnergy = mutableListOf<RelationEnergyEntry>()
    private var nextRelationEnergyId = 1L
    private val altruismDraws = mutableListOf<AltruismDraw>()
    private var nextAltruismDrawId = 1L

    override fun evaluateAndStore(input: AssessmentInput): SafetyOutcome {
        val outcome = SafetyPolicy.evaluate(
            phq9 = input.phq9,
            gad7 = input.gad7,
            completedAtMillis = input.completedAtMillis,
            previous = state,
            keywordHit = input.keywordHit,
            clarification = input.clarification,
            repeatedLowSelfEvaluation = input.repeatedLowSelfEvaluation,
        )
        history += StoredAssessment(
            completedAtMillis = input.completedAtMillis,
            phq9 = input.phq9.toList(),
            gad7 = input.gad7.toList(),
            phq9Score = outcome.assessment.phq9Score,
            phq9Band = outcome.assessment.phq9Band,
            gad7Score = outcome.assessment.gad7Score,
            gad7Band = outcome.assessment.gad7Band,
            action = outcome.action,
        )
        state = outcome.state
        return outcome
    }

    override fun safetyState(): SafetyState = state
    override fun assessments(): List<StoredAssessment> = history.toList()
    override fun userProfile(): UserProfile = profile
    override fun saveUserProfile(profile: UserProfile) {
        this.profile = profile
    }

    override fun addEmotionCard(emotion: String, event: String, hardestPart: String, createdAtMillis: Long) {
        validateEmotionCard(emotion, event, hardestPart)
        emotionCards += EmotionCard(nextEmotionCardId++, emotion, event.trim(), hardestPart.trim(), createdAtMillis)
    }

    override fun emotionCards(): List<EmotionCard> = emotionCards.toList().reversed()

    override fun markEmotionCardPassed(id: Long, passedAtMillis: Long) {
        val index = emotionCards.indexOfFirst { it.id == id }
        require(index >= 0) { "情绪卡片不存在。" }
        emotionCards[index] = emotionCards[index].copy(passedAtMillis = passedAtMillis)
    }

    override fun addCameraLog(fact: String, inference: String, createdAtMillis: Long) {
        validateCameraLog(fact, inference)
        cameraLogs += CameraLog(
            nextCameraLogId++, fact.trim(), inference.trim(), cameraFactNeedsHint(fact), createdAtMillis,
        )
    }

    override fun cameraLogs(): List<CameraLog> = cameraLogs.toList().reversed()

    override fun addWorryCard(content: String, sealedAtMillis: Long, nextSessionAtMillis: Long, audioFileName: String?) {
        validateWorryCard(content, sealedAtMillis, nextSessionAtMillis, audioFileName)
        worryCards += WorryCard(nextWorryCardId++, content.trim(), sealedAtMillis, nextSessionAtMillis, audioFileName = audioFileName)
    }

    override fun worryCards(): List<WorryCard> = worryCards.toList().reversed()

    override fun resolveWorryCard(id: Long, resolution: WorryResolution, action: String?) {
        require(resolution != WorryResolution.Pending) { "请选择处理结果。" }
        if (resolution == WorryResolution.Action) require(!action.isNullOrBlank()) { "请写下明天能做的一个动作。" }
        val index = worryCards.indexOfFirst { it.id == id && it.resolution == WorryResolution.Pending }
        require(index >= 0) { "忧虑卡片不存在或已处理。" }
        worryCards[index] = worryCards[index].copy(resolution = resolution, action = action?.trim())
    }

    override fun dismissWorryCard(id: Long) {
        require(worryCards.removeAll { it.id == id }) { "忧虑卡片不存在。" }
    }

    override fun resolveWorryAsAction(id: Long, action: String, createdAtMillis: Long) {
        require(action.isNotBlank()) { "请写下明天能做的一个动作。" }
        val index = worryCards.indexOfFirst { it.id == id && it.resolution == WorryResolution.Pending }
        require(index >= 0) { "忧虑卡片不存在或已处理。" }
        val title = action.trim()
        microActions += MicroAction(nextMicroActionId++, title, id, createdAtMillis)
        worryCards[index] = worryCards[index].copy(resolution = WorryResolution.Action, action = title)
    }

    override fun microActions(): List<MicroAction> = microActions.toList().reversed()

    override fun addMicroAction(title: String, createdAtMillis: Long) {
        require(title.isNotBlank()) { "请写下一个低到无需说服自己的动作。" }
        microActions += MicroAction(nextMicroActionId++, title.trim(), null, createdAtMillis)
    }

    override fun startMicroAction(id: Long, predictedDifficulty: Int, startedAtMillis: Long) {
        require(predictedDifficulty in 1..10) { "预测困难度必须在 1 到 10 之间。" }
        val index = microActions.indexOfFirst { it.id == id && it.startedAtMillis == null }
        require(index >= 0) { "微行动不存在或已开始。" }
        microActions[index] = microActions[index].copy(
            predictedDifficulty = predictedDifficulty,
            startedAtMillis = startedAtMillis,
        )
    }

    override fun completeMicroAction(id: Long, actualDifficulty: Int, completedAtMillis: Long) {
        require(actualDifficulty in 1..10) { "实际体感必须在 1 到 10 之间。" }
        val index = microActions.indexOfFirst {
            it.id == id && it.startedAtMillis != null && it.completedAtMillis == null
        }
        require(index >= 0) { "微行动不存在、尚未开始或已完成。" }
        require(completedAtMillis >= microActions[index].startedAtMillis!!) { "完成时间不能早于开始时间。" }
        microActions[index] = microActions[index].copy(
            actualDifficulty = actualDifficulty,
            completedAtMillis = completedAtMillis,
        )
    }

    override fun addRhythmEntry(wakeAtMillis: Long?, lightAtMillis: Long?, createdAtMillis: Long) {
        validateRhythmEntry(wakeAtMillis, lightAtMillis, createdAtMillis)
        rhythmEntries += RhythmEntry(nextRhythmEntryId++, wakeAtMillis, lightAtMillis, createdAtMillis)
    }

    override fun rhythmEntries(): List<RhythmEntry> = rhythmEntries.toList().reversed()

    override fun addRelationContact(name: String, note: String?, createdAtMillis: Long) {
        validateRelationContact(name)
        relationContacts += RelationContact(nextRelationContactId++, name.trim(), note?.trim()?.ifBlank { null }, null, createdAtMillis)
    }

    override fun relationContacts(): List<RelationContact> = relationContacts.toList().reversed()

    override fun setRelationMonitorsSelf(id: Long, monitorsSelf: Boolean) {
        val index = relationContacts.indexOfFirst { it.id == id }
        require(index >= 0) { "联系人不存在。" }
        relationContacts[index] = relationContacts[index].copy(monitorsSelf = monitorsSelf)
    }

    override fun addRelationEnergy(contactId: Long, mark: EnergyMark, createdAtMillis: Long) {
        require(relationContacts.any { it.id == contactId }) { "联系人不存在。" }
        relationEnergy += RelationEnergyEntry(nextRelationEnergyId++, contactId, mark, createdAtMillis)
    }

    override fun relationEnergy(): List<RelationEnergyEntry> = relationEnergy.toList().reversed()

    override fun addAltruismDraw(title: String, kind: AltruismKind, drawnAtMillis: Long) {
        require(title.isNotBlank()) { "请先抽一张小事。" }
        altruismDraws += AltruismDraw(nextAltruismDrawId++, title.trim(), kind, drawnAtMillis)
    }

    override fun altruismDraws(): List<AltruismDraw> = altruismDraws.toList().reversed()

    override fun completeAltruismDraw(id: Long, felt: AltruismFeel, completedAtMillis: Long) {
        val index = altruismDraws.indexOfFirst { it.id == id }
        require(index >= 0) { "这张小事不存在。" }
        val current = altruismDraws[index]
        require(current.felt == null) { "这张小事已经记下体感。" }
        altruismDraws[index] = current.copy(felt = felt, completedAtMillis = completedAtMillis)
    }

    override fun clearAllData() {
        history.clear()
        state = SafetyState()
        profile = UserProfile()
        emotionCards.clear()
        nextEmotionCardId = 1L
        cameraLogs.clear()
        nextCameraLogId = 1L
        worryCards.clear()
        nextWorryCardId = 1L
        microActions.clear()
        nextMicroActionId = 1L
        rhythmEntries.clear()
        nextRhythmEntryId = 1L
        relationContacts.clear()
        nextRelationContactId = 1L
        relationEnergy.clear()
        nextRelationEnergyId = 1L
        altruismDraws.clear()
        nextAltruismDrawId = 1L
    }
}

val evaluativeFactWords = listOf(
    "觉得", "感觉", "认为", "肯定", "故意", "讨厌", "轻视", "不喜欢", "很糟", "失败", "没用", "糟糕",
    "愤怒", "生气", "针对", "伤心", "失望",
)

fun firstEvaluativeWord(fact: String): String? = evaluativeFactWords.firstOrNull { it in fact }

fun cameraFactNeedsHint(fact: String): Boolean = firstEvaluativeWord(fact) != null

internal fun validateCameraLog(fact: String, inference: String) {
    require(fact.isNotBlank() || inference.isNotBlank()) { "至少写下一栏。" }
}

internal fun validateWorryCard(content: String, sealedAtMillis: Long, nextSessionAtMillis: Long, audioFileName: String? = null) {
    require(content.isNotBlank() || !audioFileName.isNullOrBlank()) { "请写下或录下要挂起来的念头。" }
    require(nextSessionAtMillis >= sealedAtMillis) { "下次忧虑专场时间无效。" }
}

internal fun validateRhythmEntry(wakeAtMillis: Long?, lightAtMillis: Long?, createdAtMillis: Long) {
    require(wakeAtMillis != null || lightAtMillis != null) { "至少记录起床或见光时间。" }
    require(listOfNotNull(wakeAtMillis, lightAtMillis).all { it in 1..createdAtMillis }) { "节律时间无效。" }
}

internal fun validateRelationContact(name: String) {
    require(name.isNotBlank()) { "请写下对方怎么称呼。" }
}

internal fun validateEmotionCard(emotion: String, event: String, hardestPart: String) {
    require(emotion in emotionVocabulary) { "请选择一个具体情绪词。" }
    require(event.isNotBlank()) { "请写下发生了什么。" }
    require(hardestPart.isNotBlank()) { "请写下最难受的具体部分。" }
}
