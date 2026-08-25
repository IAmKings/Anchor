package com.anchor.app.worry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WorryCopyTest {
    @Test
    fun hangSheetCopyIsShortAndUsesDynamicSealMessage() {
        assertEquals("把这个念头放进保险箱", hangSheetTitle)
        assertEquals("说话，或写一句…", hangFieldHint)
        assertEquals("封存", hangSealLabel)
        assertEquals("语音速记", hangSpeechLabel)
        assertEquals("停止并保存录音", hangSpeechStopLabel)
        assertFalse(hangSheetTitle.contains("今晚"))
        assertFalse(vaultSealedMessage(false, "明天 20:00").contains("今晚"))
    }

    @Test
    fun sealedMessageUsesNextSessionAndNeverShowsCardContent() {
        assertEquals("已存入保险箱，明天 20:00 统一开箱。", vaultSealedMessage(false, "明天 20:00"))
        assertEquals("已存入保险箱，可以在当前专场处理。", vaultSealedMessage(true, "今天 20:00"))
        assertEquals("这个已经在纸上了，明天 20:00 再说。", vaultRuminationMessage("明天 20:00"))
        assertFalse(vaultPendingSummary(3).contains("汇报"))
    }

    @Test
    fun overviewCopyKeepsExemptionPathAndDoesNotPressure() {
        assertEquals("忧虑保险箱", vaultTitle)
        assertEquals("念头已经在纸上了", vaultLockedCaption)
        assertEquals("现在就想处理", vaultAskOpenNowLabel)
        assertEquals("确认开箱", vaultConfirmOpenLabel)
        assertEquals("等到专场", vaultWaitForSessionLabel)
        assertEquals("专场已开启", vaultOpenCaption)
        assertEquals("开始处理第一项", vaultStartFirstLabel)
        assertEquals("语音挂卡", vaultAudioHangLabel)
        assertEquals("现在开箱后，请尽量为每张卡做一个选择，优先找“明天能做的一个动作”。", vaultConfirmOpenHint)
        assertFalse(vaultConfirmOpenHint.contains("务必"))
        assertFalse(vaultAcceptedQuote.contains("打卡"))
        assertFalse(vaultOpenBreath.contains("今晚"))
    }

    @Test
    fun processCopyKeepsThreeChoicesAndDynamicUnsolvableHint() {
        assertEquals("回到开箱", vaultBackToOverview)
        assertEquals("请选择处理方式", vaultProcessPrompt)
        assertEquals("明天能做的一个动作", vaultChooseActionLabel)
        assertEquals("暂时无解", vaultUnsolvableLabel)
        assertEquals("已不再重要", vaultDismissLabel)
        assertEquals("播放本地录音", vaultPlayAudioLabel)
        assertEquals("“语音挂卡”", vaultQuotedCard(""))
        assertEquals("“担心汇报”", vaultQuotedCard("担心汇报"))
        assertEquals("明天 20:00 前无需再想。", vaultUnsolvableHint("明天 20:00"))
        assertFalse(vaultUnsolvableHint("明天 20:00").contains("今晚"))
        assertFalse(vaultChooseActionLabel.contains("必须"))
    }

    @Test
    fun convertCopySyncsToHomeWithoutCheckInLanguage() {
        assertEquals("回到三选一", vaultBackToProcess)
        assertEquals("转化动作", vaultConvertTitle)
        assertEquals("明天要做的微行动", vaultConvertFieldLabel)
        assertEquals("确认并同步到首页", vaultConvertConfirmLabel)
        assertFalse(vaultConvertConfirmLabel.contains("打卡"))
        assertFalse(vaultConvertTitle.contains("完成率"))
    }

    @Test
    fun recordedLabelIsRelativeAndShameFree() {
        assertEquals("刚刚挂上", worryRecordedLabel(1_000, 1_000))
        assertEquals("记录于 2 分钟前", worryRecordedLabel(0, 2 * 60_000))
        assertEquals("记录于 3 小时前", worryRecordedLabel(0, 3 * 60 * 60_000))
        assertEquals("记录于 2 天前", worryRecordedLabel(0, 2 * 24 * 60 * 60_000))
    }

    @Test
    fun doneSummaryCountsAcceptedCardsNotStreaks() {
        assertEquals("今日受理了 3 项忧虑", vaultDoneSummary(3))
        assertEquals("这一刻没有需要处理的卡片。", vaultDoneSummary(0))
    }
}
