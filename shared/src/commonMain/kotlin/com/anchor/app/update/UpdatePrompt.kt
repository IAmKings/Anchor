package com.anchor.app.update

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UpdatePromptDialog(
    remoteVersion: String,
    localVersion: String,
    onDownload: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(updatePromptTitle, fontSize = 20.sp) },
        text = { Text(updatePromptBody(remoteVersion, localVersion), fontSize = 15.sp, lineHeight = 22.sp) },
        confirmButton = {
            TextButton(onClick = onDownload, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(updateDownload, fontSize = 17.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(updateLater, fontSize = 17.sp)
            }
        },
    )
}
