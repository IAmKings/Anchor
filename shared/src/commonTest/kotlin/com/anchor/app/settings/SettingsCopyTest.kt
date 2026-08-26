package com.anchor.app.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsCopyTest {
    @Test
    fun reminderCopyCoversOnlyTheFourAllowedKinds() {
        assertEquals(4, ReminderToggle.entries.size)
        assertEquals("忧虑专场", reminderToggleTitle(ReminderToggle.WorrySession))
        assertEquals("危机后关怀", reminderToggleTitle(ReminderToggle.CrisisCare))
        assertFalse(ReminderToggle.entries.any { reminderToggleTitle(it).contains("好久没来") })
        assertFalse(reminderToggleDetail(ReminderToggle.WorrySession).contains("streak"))
    }

    @Test
    fun exportCompleteCopyHasNoPdfAndParsesSuccess() {
        assertFalse(exportCompleteTitle.contains("PDF"))
        assertFalse(exportCompleteBody.contains("PDF"))
        assertTrue(isExportSuccess("已生成 anchor-20260818-110000.anchor；密码未保存，请另行保管。"))
        assertFalse(isExportSuccess("无法生成加密导出文件。"))
        assertFalse(isExportSuccess("备份恢复成功。"))
        assertEquals(
            "anchor-20260818-110000.anchor",
            exportFileLabel("已生成 anchor-20260818-110000.anchor；密码未保存，请另行保管。"),
        )
        assertEquals("提取密码已复制", exportPasswordCopiedTitle)
        assertEquals("点击分享", exportShareLabel)
        assertEquals("生成加密导出文件", settingsExportAction)
        assertEquals("删除全部本地数据", settingsDeleteAction)
        assertEquals("新备份会带上本地录音。更早的备份恢复后，录音仍会缺失。", settingsAudioNotRestored)
        assertTrue(settingsAudioNotRestored.contains("录音"))
        assertFalse(settingsAudioNotRestored.contains("路径"))
        assertTrue(settingsLockscreenNote.contains("锚点"))
        assertFalse(settingsExportBody.contains("PDF"))
        assertFalse(settingsRemindersIntro.contains("好久没来"))
        assertTrue(settingsAboutBody.contains("不会上传"))
    }
}
