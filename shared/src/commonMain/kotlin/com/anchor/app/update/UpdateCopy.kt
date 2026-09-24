package com.anchor.app.update

internal const val settingsCheckUpdate = "检查更新"
internal const val settingsUpdateChecking = "正在核对测试版。"
internal const val settingsUpdateCurrent = "已是当前测试版。"
internal const val settingsUpdateFailed = "这次没对上，可以再试。"
internal const val settingsUpdateNote = "检查更新只向 GitHub 询问版本号，不上传记录。"
internal const val updatePromptTitle = "有新的测试版"
internal const val updateDownload = "去下载"
internal const val updateLater = "稍后"

internal fun settingsVersionLine(versionName: String): String = "当前版本 $versionName"

internal fun settingsUpdateAvailable(versionName: String): String = "测试版 $versionName 已发布。"

internal fun updatePromptBody(remoteVersion: String, localVersion: String): String =
    "测试版 $remoteVersion 已发布。现在是 $localVersion。"

internal fun updateStatusLine(phase: UpdatePhase): String = when (phase) {
    UpdatePhase.Idle -> ""
    UpdatePhase.Checking -> settingsUpdateChecking
    UpdatePhase.UpToDate -> settingsUpdateCurrent
    is UpdatePhase.UpdateAvailable -> settingsUpdateAvailable(phase.versionName)
    UpdatePhase.Failed -> settingsUpdateFailed
}
