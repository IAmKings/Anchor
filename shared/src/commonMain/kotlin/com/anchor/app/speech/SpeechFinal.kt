package com.anchor.app.speech

import kotlin.random.Random

/** 按住说话的时长上限（秒）；引擎另有 10 分钟硬上限兜底。 */
const val VOICE_HOLD_LIMIT_SECONDS = 60

/**
 * 一次语音输入的最终产物，由平台端侧识别或 sherpa-ncnn 转写产出后一次性交给 UI。
 * [text] 与 [audioFileName] 至少一个非空：转写路径文字优先（sherpa 路径两者都有，
 * 平台端侧识别拿不到音频流所以只有文字），录音兜底只有录音。
 */
data class SpeechFinal(
    val text: String?,
    val audioFileName: String?,
) {
    /** LaunchedEffect 以此区分连续两次内容相同的产物。 */
    val id: Long = Random.nextLong()
}
