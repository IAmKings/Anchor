package com.anchor.app.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.CrisisRegion

private val CardShape = RoundedCornerShape(16.dp)
private val HelpActionMinHeight = 52.dp

@Composable
fun HelpNowScreen(
    region: CrisisRegion,
    youth: Boolean,
    onOpenGuide: () -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    onClose: () -> Unit,
) {
    val resource = crisisResource(region, youth)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    helpNowImmediateDanger(resource.emergency),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().heightIn(min = HelpActionMinHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) { Text(helpNowCallEmergency(resource.emergency), fontSize = 17.sp) }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
            Text(helpNowTitle, Modifier.semantics { heading() }, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            StepCard(1, helpNowStep1Label, helpNowStep1Title, resource.hotline)
            StepCard(2, helpNowStep2Label, helpNowStep2Title, helpNowStep2Body)
            StepCard(3, helpNowStep3Label, helpNowStep3Title, helpNowStep3Body)
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                shape = CardShape,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(helpNowSomaticTitle, fontWeight = FontWeight.SemiBold)
                    Text(
                        helpNowSomaticBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                    TextButton(onClick = onOpenChecklist) { Text(helpNowChecklistAction) }
                }
            }
            Button(
                onClick = onOpenGuide,
                modifier = Modifier.fillMaxWidth().heightIn(min = HelpActionMinHeight),
                shape = RoundedCornerShape(12.dp),
            ) { Text(helpNowGuideAction, fontSize = 17.sp) }
            Text(
                helpNowMedicalQuote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepCard(index: Int, step: String, title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("$index", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}
