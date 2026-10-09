package com.anchor.app.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anchor.app.onboarding.ThoughtIcon
import com.anchor.app.ui.AnchorIconWell
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnchorIconWell(ThoughtIcon, MaterialTheme.colorScheme.primary, size = 64.dp, tint = MaterialTheme.colorScheme.onPrimary)
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
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 是我 / 不确定 → 危机干预（crisis 动作用 secondary，与应用规范一致）；不是我 → 留在原地。
                CrisisAnswerButton(
                    label = crisisClarificationSelf,
                    emphasized = true,
                    onClick = onSelf,
                )
                CrisisAnswerButton(
                    label = crisisClarificationUncertain,
                    emphasized = true,
                    onClick = onUncertain,
                )
                CrisisAnswerButton(
                    label = crisisClarificationNotSelf,
                    emphasized = false,
                    onClick = onNotSelf,
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}

/** 澄清回答按钮：52dp 全宽 pill；emphasized = 危机干预走向（secondary 描边与文字）。 */
@Composable
private fun CrisisAnswerButton(
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (emphasized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            label,
            fontSize = 17.sp,
            color = if (emphasized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
