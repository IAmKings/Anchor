package com.anchor.app.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anchor.app.onboarding.privacyTitle
import com.anchor.app.onboarding.termsTitle
import com.anchor.app.ui.AnchorBackBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.update.UpdatePhase
import com.anchor.app.update.settingsCheckUpdate
import com.anchor.app.update.settingsUpdateNote
import com.anchor.app.update.settingsVersionLine
import com.anchor.app.update.updateDownload
import com.anchor.app.update.updateStatusLine

private val CardShape = RoundedCornerShape(16.dp)
private val SettingsActionMinHeight = 52.dp

@Composable
fun SettingsScreen(
    appLockAvailable: Boolean,
    appLockEnabled: Boolean,
    onToggleAppLock: () -> Unit,
    reminderAllows: Map<ReminderToggle, Boolean> = ReminderToggle.entries.associateWith { true },
    onToggleReminder: (ReminderToggle) -> Unit = {},
    notificationPermission: NotificationPermission? = null,
    onRequestNotificationPermission: () -> Unit = {},
    exactAlarmGranted: Boolean? = null,
    onRequestExactAlarm: () -> Unit = {},
    themeChoice: ThemeChoice = ThemeChoice.System,
    onThemeChoice: (ThemeChoice) -> Unit = {},
    speechMode: String = "",
    speechDownloading: Boolean = false,
    speechDownloadProgress: Int = 0,
    speechReady: Boolean = false,
    speechEnabled: Boolean = false,
    speechError: String? = null,
    onSpeechDownload: () -> Unit = {},
    onSpeechCancelDownload: () -> Unit = {},
    onSpeechToggle: (Boolean) -> Unit = {},
    onSpeechDelete: () -> Unit = {},
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
    onPreviewUnlock: () -> Unit = {},
    debugSeedVisible: Boolean = false,
    onSeedRhythmDemo: () -> Unit = {},
    onClearRhythmDemo: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenReassessment: () -> Unit = {},
    installedVersionName: String = "",
    updateCheckAvailable: Boolean = false,
    updatePhase: UpdatePhase = UpdatePhase.Idle,
    onCheckUpdate: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    onClose: () -> Unit,
) {
    var confirmingDeletion by remember { mutableStateOf(false) }
    var confirmingImport by remember { mutableStateOf(false) }
    AnchorBackBar(onBack = onClose, label = settingsBackLabel, title = settingsTitle) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(settingsIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)

            Text(settingsPrivacyTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            SettingsCard {
                ToggleRow(
                    title = settingsLockTitle,
                    detail = if (appLockAvailable) settingsLockAvailable else settingsLockUnavailable,
                    checked = appLockEnabled,
                    enabled = appLockAvailable,
                    onToggle = onToggleAppLock,
                )
                Text(settingsLockscreenNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            Text(settingsAppearanceTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(settingsAppearanceIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            SettingsCard {
                ThemeChoiceRow(selected = themeChoice, onSelect = onThemeChoice)
            }

            Text(settingsSpeechTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(settingsSpeechIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            SettingsCard {
                Text(settingsSpeechMode(speechEnabled, speechReady), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (speechDownloading) {
                    LinearProgressIndicator(
                        progress = { speechDownloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 8.dp),
                    )
                    Text(
                        "$speechDownloadProgress%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                if (!speechReady) {
                    Button(
                        onClick = if (speechDownloading) onSpeechCancelDownload else onSpeechDownload,
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(settingsSpeechDownloadAction(speechDownloading), fontSize = 17.sp)
                    }
                }
                if (speechReady) {
                    ToggleRow(
                        title = settingsSpeechEnableLabel,
                        detail = if (speechEnabled) "下次按住说话即生效" else "下载完成后可开启",
                        checked = speechEnabled,
                        enabled = true,
                        onToggle = { onSpeechToggle(!speechEnabled) },
                    )
                    TextButton(onClick = onSpeechDelete, modifier = Modifier.fillMaxWidth()) {
                        Text(settingsSpeechDeleteLabel)
                    }
                }
                speechError?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            Text(settingsRemindersTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(settingsRemindersIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            SettingsCard {
                notificationPermission?.let { permission ->
                    Text(settingsNotificationTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        settingsNotificationDetail(permission),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    if (permission != NotificationPermission.Granted) {
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(settingsNotificationAction(permission), fontSize = 17.sp)
                        }
                    }
                }
                exactAlarmGranted?.let { granted ->
                    Text(settingsExactAlarmTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        settingsExactAlarmDetail(granted),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    if (!granted) {
                        Button(
                            onClick = onRequestExactAlarm,
                            modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(settingsExactAlarmAction(granted), fontSize = 17.sp)
                        }
                    }
                }
                ReminderToggle.entries.forEach { toggle ->
                    ToggleRow(
                        title = reminderToggleTitle(toggle),
                        detail = reminderToggleDetail(toggle),
                        checked = reminderAllows[toggle] != false,
                        onToggle = { onToggleReminder(toggle) },
                    )
                }
            }

            Text(settingsDataTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            SettingsCard {
                Text(settingsExportTitle, fontWeight = FontWeight.SemiBold)
                Text(settingsExportBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
                OutlinedTextField(
                    value = exportPassword,
                    onValueChange = onExportPasswordChange,
                    label = { Text(settingsExportPasswordLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Button(
                    onClick = onExport,
                    enabled = exportPassword.length >= 8,
                    modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(settingsExportAction, fontSize = 17.sp)
                }
                if (confirmingImport) {
                    Text(settingsImportConfirm, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { confirmingImport = false; onImport() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(settingsImportConfirmAction, fontSize = 17.sp)
                    }
                    TextButton(onClick = { confirmingImport = false }, modifier = Modifier.fillMaxWidth()) { Text(settingsCancel) }
                } else {
                    Button(
                        onClick = { confirmingImport = true },
                        enabled = exportPassword.length >= 8,
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(settingsImportAction, fontSize = 17.sp) }
                }
                Text(settingsAudioNotRestored, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                exportStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp) }
            }

            SettingsCard {
                Text(settingsDeleteTitle, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                Text(settingsDeleteBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
                if (confirmingDeletion) {
                    Text(settingsDeleteConfirm, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { confirmingDeletion = false; onDeleteAllData() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) { Text(settingsDeleteConfirmAction, fontSize = 17.sp) }
                    TextButton(onClick = { confirmingDeletion = false }, modifier = Modifier.fillMaxWidth()) { Text(settingsCancel) }
                } else {
                    Button(
                        onClick = { confirmingDeletion = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) { Text(settingsDeleteAction, fontSize = 17.sp) }
                }
                deleteStatus?.let { Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp) }
            }

            Text(settingsAboutTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            SettingsCard {
                Text(settingsAboutBody)
                if (installedVersionName.isNotBlank()) {
                    Text(settingsVersionLine(installedVersionName), fontSize = 15.sp)
                }
                if (updateCheckAvailable) {
                    Text(settingsUpdateNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
                    val status = updateStatusLine(updatePhase)
                    if (status.isNotEmpty()) Text(status, fontSize = 15.sp)
                    Button(
                        onClick = onCheckUpdate,
                        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(settingsCheckUpdate, fontSize = 17.sp) }
                    if (updatePhase is UpdatePhase.UpdateAvailable) {
                        Button(
                            onClick = onOpenUpdate,
                            modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(updateDownload, fontSize = 17.sp) }
                    }
                }
                TextButton(onClick = onOpenHelp, modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight)) {
                    Text(settingsHelpLink, fontSize = 17.sp)
                }
                TextButton(onClick = onOpenTerms, modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight)) {
                    Text(termsTitle, fontSize = 17.sp)
                }
                TextButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight)) {
                    Text(privacyTitle, fontSize = 17.sp)
                }
                TextButton(onClick = onOpenReassessment, modifier = Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight)) {
                    Text(settingsReassessmentLink, fontSize = 17.sp)
                }
                TextButton(onClick = onPreviewOnboarding) { Text(settingsPreviewOnboarding) }
                TextButton(onClick = onPreviewReturnToPractice) { Text(settingsPreviewReturn) }
                TextButton(onClick = onPreviewMedicalGuide) { Text(settingsPreviewMedical) }
                TextButton(onClick = onPreviewHomeRelation) { Text(settingsPreviewRelation) }
                TextButton(onClick = onPreviewCrisisClarification) { Text(settingsPreviewCrisis) }
                TextButton(onClick = onPreviewOneThingLock) { Text(settingsPreviewLock) }
                TextButton(onClick = onPreviewUnlock) { Text(settingsPreviewUnlock) }
                if (debugSeedVisible) {
                    TextButton(onClick = onSeedRhythmDemo) { Text(settingsSeedRhythmDemo) }
                    TextButton(onClick = onClearRhythmDemo) { Text(settingsClearRhythmDemo) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ThemeChoiceRow(selected: ThemeChoice, onSelect: (ThemeChoice) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeChoice.entries.forEach { choice ->
            val chosen = choice == selected
            Surface(
                onClick = { onSelect(choice) },
                modifier = Modifier
                    .heightIn(min = SettingsActionMinHeight)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = chosen
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (chosen) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        themeChoiceLabel(choice),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (chosen) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
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
    Row(
        Modifier.fillMaxWidth().heightIn(min = SettingsActionMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}
