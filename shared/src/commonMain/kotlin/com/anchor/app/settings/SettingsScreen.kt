package com.anchor.app.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardShape = RoundedCornerShape(16.dp)
private val Terracotta = Color(0xFFB26A4F)

@Composable
fun SettingsScreen(
    appLockAvailable: Boolean,
    appLockEnabled: Boolean,
    onToggleAppLock: () -> Unit,
    reminderAllows: Map<ReminderToggle, Boolean> = ReminderToggle.entries.associateWith { true },
    onToggleReminder: (ReminderToggle) -> Unit = {},
    exportPassword: String,
    onExportPasswordChange: (String) -> Unit,
    exportStatus: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    deleteStatus: String?,
    onDeleteAllData: () -> Unit,
    onOpenHelp: () -> Unit = {},
    onPreviewOnboarding: () -> Unit = {},
    onPreviewReturnToPractice: () -> Unit = {},
    onPreviewMedicalGuide: () -> Unit = {},
    onPreviewHomeRelation: () -> Unit = {},
    onPreviewCrisisClarification: () -> Unit = {},
    onPreviewOneThingLock: () -> Unit = {},
    onOpenReassessment: () -> Unit = {},
    onClose: () -> Unit,
) {
    var confirmingDeletion by remember { mutableStateOf(false) }
    var confirmingImport by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text("我的", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("管理应用锁、四类提醒和本地数据。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)

        Text("隐私与安全", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        SettingsCard {
            ToggleRow(
                title = "应用锁",
                detail = if (appLockAvailable) "开启后，每次打开都需要验证。" else "当前设备暂不支持生物识别或设备凭据。",
                checked = appLockEnabled,
                enabled = appLockAvailable,
                onToggle = onToggleAppLock,
            )
            Text("锁屏通知只显示「锚点」，不显示正文。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }

        Text("提醒", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("只有这四类。关掉后不追问、不挽留。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        SettingsCard {
            ReminderToggle.entries.forEach { toggle ->
                ToggleRow(
                    title = reminderToggleTitle(toggle),
                    detail = reminderToggleDetail(toggle),
                    checked = reminderAllows[toggle] != false,
                    onToggle = { onToggleReminder(toggle) },
                )
            }
        }

        Text("数据管理", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        SettingsCard {
            Text("加密导出", fontWeight = FontWeight.SemiBold)
            Text("导出 JSON 与 CSV。密码不会保存，遗失后无法恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
            OutlinedTextField(
                value = exportPassword,
                onValueChange = onExportPasswordChange,
                label = { Text("导出密码（至少 8 个字符）") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Button(onClick = onExport, enabled = exportPassword.length >= 8, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("生成加密导出文件")
            }
            if (confirmingImport) {
                Text("恢复会替换本机现有记录，确定继续吗？", color = Terracotta, fontWeight = FontWeight.SemiBold)
                Button(onClick = { confirmingImport = false; onImport() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("确认并选择备份")
                }
                TextButton(onClick = { confirmingImport = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            } else {
                Button(
                    onClick = { confirmingImport = true },
                    enabled = exportPassword.length >= 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("选择加密备份并恢复") }
            }
            Text("备份中的录音正文当前无法恢复。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            exportStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp) }
        }

        SettingsCard {
            Text("彻底删除所有数据", fontWeight = FontWeight.SemiBold, color = Terracotta)
            Text("此操作不可逆。评估、记录、录音、提醒、计时、导出缓存和应用锁都会被清掉。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
            if (confirmingDeletion) {
                Text("确定要彻底删除吗？删除后无法恢复。", color = Terracotta, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { confirmingDeletion = false; onDeleteAllData() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
                ) { Text("确认彻底删除") }
                TextButton(onClick = { confirmingDeletion = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            } else {
                Button(
                    onClick = { confirmingDeletion = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
                ) { Text("删除全部本地数据") }
            }
            deleteStatus?.let { Text(it, color = Terracotta, fontSize = 14.sp) }
        }

        Text("关于", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        SettingsCard {
            Text("锚点只把数据留在这台设备。没有账号，也不会上传。")
            TextButton(onClick = onOpenHelp) { Text("就医与帮助入口") }
            TextButton(onClick = onOpenReassessment) { Text("复评量表") }
            TextButton(onClick = onPreviewOnboarding) { Text("预览首启评估（不保存）") }
            TextButton(onClick = onPreviewReturnToPractice) { Text("预览回归练习（不改状态）") }
            TextButton(onClick = onPreviewMedicalGuide) { Text("预览就医指南（不改地区）") }
            TextButton(onClick = onPreviewHomeRelation) { Text("预览首页回血 / 抽干 / 耗竭（不改记录）") }
            TextButton(onClick = onPreviewCrisisClarification) { Text("预览危机澄清（不改状态）") }
            TextButton(onClick = onPreviewOneThingLock) { Text("预览一次一件锁定（不改选择）") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = { content() })
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}
