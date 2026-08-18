package com.anchor.app.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrisisClarificationDialog(
    phrase: String,
    previewing: Boolean = false,
    onNotSelf: () -> Unit,
    onSelf: () -> Unit,
    onUncertain: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                crisisClarificationTitle,
                Modifier.semantics { heading() },
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (previewing) {
                    Text(
                        crisisClarificationPreviewNote,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                Text(crisisClarificationBody, fontSize = 15.sp, lineHeight = 24.sp)
                Text(
                    "匹配到「$phrase」",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = onSelf, modifier = Modifier.fillMaxWidth()) {
                    Text(crisisClarificationSelf)
                }
                TextButton(onClick = onUncertain, modifier = Modifier.fillMaxWidth()) {
                    Text(crisisClarificationUncertain)
                }
                TextButton(onClick = onNotSelf, modifier = Modifier.fillMaxWidth()) {
                    Text(crisisClarificationNotSelf)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
