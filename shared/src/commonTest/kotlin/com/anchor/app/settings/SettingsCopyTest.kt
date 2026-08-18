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
    }
}
