package com.anchor.app.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.CrisisRegion

private val CardShape = RoundedCornerShape(16.dp)
private val Terracotta = Color(0xFFB26A4F)

@Composable
fun HelpNowScreen(
    region: CrisisRegion,
    youth: Boolean,
    onOpenGuide: () -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    onClose: () -> Unit,
) {
    val resource = crisisResource(region, youth)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text("你现在不是一个人", Modifier.semantics { heading() }, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "如果你现在处于立即危险中，请直接拨打 ${resource.emergency}。",
            color = Terracotta,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        )
        StepCard("第一步", "拨打心理援助热线", resource.hotline)
        StepCard("第二步", "前往精神科或急诊", "去最近的医院急诊，或直接预约精神科。")
        StepCard("第三步", "告诉一位身边可信的人", "找一个你信得过的朋友或家人，告诉他们你现在很难受。")
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            shape = CardShape,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("躯体因素也请一起查", fontWeight = FontWeight.SemiBold)
                Text(
                    "甲状腺异常、贫血、维生素缺乏、慢性疼痛或药物副作用等身体因素也可能表现为情绪症状。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
                TextButton(onClick = onOpenChecklist) { Text("查看就医准备清单") }
            }
        }
        Button(
            onClick = onOpenGuide,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) { Text("查看就医指南") }
        Text(
            "判断这个不是你的工作，是医生的工作。去做一次评估，是为了终结自我审判。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
        ) { Text("立即危险请拨 ${resource.emergency}") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepCard(step: String, title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(step, color = Terracotta, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        }
    }
}
