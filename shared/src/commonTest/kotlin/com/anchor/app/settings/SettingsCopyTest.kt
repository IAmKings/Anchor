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
        assertEquals("保存到文件夹", exportSaveToFolderLabel)
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

    @Test
    fun themeChoiceDefaultsToSystemAndLabelsTheThreeModes() {
        assertEquals(ThemeChoice.System, ThemeChoice.entries.first())
        assertEquals(listOf("跟随系统", "浅色", "深色"), ThemeChoice.entries.map(::themeChoiceLabel))
        assertFalse(ThemeChoice.System.useDark(systemDark = false))
        assertTrue(ThemeChoice.System.useDark(systemDark = true))
        assertFalse(ThemeChoice.Light.useDark(systemDark = true))
        assertTrue(ThemeChoice.Dark.useDark(systemDark = false))
        assertTrue(settingsAppearanceIntro.contains("跟随系统"))
    }

    @Test
    fun notificationPermissionIsSeparateFromTheFourReminders() {
        assertEquals(4, ReminderToggle.entries.size)
        assertEquals("系统通知", settingsNotificationTitle)
        assertEquals("已允许。锁屏只显示「锚点」，不显示正文。", settingsNotificationDetail(NotificationPermission.Granted))
        assertEquals("允许通知", settingsNotificationAction(NotificationPermission.Requestable))
        assertEquals("去系统设置打开", settingsNotificationAction(NotificationPermission.Blocked))
        val denied = settingsNotificationDetail(NotificationPermission.Requestable)
        assertEquals(denied, settingsNotificationDetail(NotificationPermission.Blocked))
        assertTrue(denied.contains("顶部提示"))
        assertFalse(denied.contains("必须"))
        assertFalse(ReminderToggle.entries.any { reminderToggleTitle(it) == settingsNotificationTitle })
    }

    @Test
    fun exactAlarmCopyExplainsALateNotificationWithoutNagging() {
        assertEquals("准时提醒", settingsExactAlarmTitle)
        assertEquals("已允许。浪潮到点会准时进通知栏。", settingsExactAlarmDetail(granted = true))
        assertEquals("去开启", settingsExactAlarmAction(granted = false))
        val denied = settingsExactAlarmDetail(granted = false)
        assertTrue(denied.contains("推迟"))
        assertFalse(denied.contains("必须"))
        assertFalse(denied.contains("streak"))
    }
}
