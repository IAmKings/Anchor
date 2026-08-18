package com.anchor.app.wave

internal const val WAVE_TEN_MINUTES = 10L * 60 * 1_000

data class BodyLocation(
    val shortLabel: String,
    val fullLabel: String,
)

val waveBodyLocations = listOf(
    BodyLocation("胸口", "胸口发紧"),
    BodyLocation("喉咙", "喉咙堵"),
    BodyLocation("手心", "手心出汗"),
    BodyLocation("胃部", "胃部收紧"),
    BodyLocation("肩膀", "肩膀僵"),
    BodyLocation("头皮", "头皮发麻"),
    BodyLocation("呼吸", "呼吸浅"),
    BodyLocation("面部", "面部发烫"),
)

internal fun waveRecognizeTitle(): String = "我现在很难受，这是真的。"
internal fun waveRecognizeBody(): String = "承认这种感觉。不需要对抗，只需要看见它。"
internal fun waveRecognizeAction(): String = "我看见了"

internal fun waveLocateTitle(): String = "这种感觉，停在哪里？"
internal fun waveLocateBody(): String = "深呼吸，感受身体的哪个部位最沉重。"
internal fun waveLocateContinue(): String = "继续"

internal fun waveWaitTitle(done: Boolean): String =
    if (done) "它自己退了。你没有掐掉它，它也会走。" else "我们一起等浪潮过去"

internal fun waveWaitBody(location: String?): String = when {
    !location.isNullOrBlank() -> "留意：$location"
    else -> "什么都不用做，只是呼吸。"
}

internal fun waveAddTenMinutes(): String = "再加 10 分钟"
internal fun waveLeave(): String = "离开"
internal fun waveBackToToday(): String = "回到今天"
