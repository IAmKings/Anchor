package com.anchor.app.relation

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.AltruismFeel
import com.anchor.app.storage.AltruismKind
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.EnergyMark
import com.anchor.app.storage.RelationContact

private enum class RelationStep { Hub, Inventory, Ledger, Altruism }

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun RelationFlow(
    store: AnchorStore,
    nowMillis: () -> Long,
    showInventory: Boolean = true,
    showAltruism: Boolean = true,
    onClose: () -> Unit,
) {
    var step by remember { mutableStateOf(RelationStep.Hub) }
    var revision by remember { mutableIntStateOf(0) }
    when (step) {
        RelationStep.Hub -> RelationHub(
            showInventory = showInventory,
            showAltruism = showAltruism,
            onInventory = { step = RelationStep.Inventory },
            onLedger = { step = RelationStep.Ledger },
            onAltruism = { step = RelationStep.Altruism },
            onClose = onClose,
        )
        RelationStep.Inventory -> InventoryScreen(
            store = store,
            nowMillis = nowMillis,
            revision = revision,
            onChanged = { revision++ },
            onBack = { step = RelationStep.Hub },
        )
        RelationStep.Ledger -> LedgerScreen(
            store = store,
            nowMillis = nowMillis,
            revision = revision,
            onChanged = { revision++ },
            onBack = { step = RelationStep.Hub },
        )
        RelationStep.Altruism -> AltruismScreen(
            store = store,
            nowMillis = nowMillis,
            revision = revision,
            onChanged = { revision++ },
            onBack = { step = RelationStep.Hub },
        )
    }
}

@Composable
private fun RelationHub(
    showInventory: Boolean,
    showAltruism: Boolean,
    onInventory: () -> Unit,
    onLedger: () -> Unit,
    onAltruism: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text(relationHubTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(relationHubBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        if (showInventory) {
            HubButton(inventoryTitle, inventoryHint, onInventory)
            HubButton(ledgerTitle, ledgerHint, onLedger)
        }
        if (showAltruism) {
            HubButton(altruismTitle, "默认先抽不社交的小事。", onAltruism)
        }
        Text(leavingCopy, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun HubButton(title: String, detail: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun InventoryScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    revision: Int,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val contacts = remember(revision) { store.relationContacts() }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("返回") }
        Text(inventoryTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(inventoryHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("对方怎么称呼") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("关系，可空。如同事") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
        Button(
            onClick = {
                store.addRelationContact(name, note, nowMillis())
                name = ""
                note = ""
                onChanged()
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) { Text("加进清单") }
        contacts.forEach { contact ->
            ContactCard(contact, onMonitor = { yes ->
                store.setRelationMonitorsSelf(contact.id, yes)
                onChanged()
            })
        }
        Text(leavingCopy, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ContactCard(contact: RelationContact, onMonitor: (Boolean) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            contact.note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
            Text(inventoryPrompt, fontSize = 15.sp, lineHeight = 22.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(monitorYes, contact.monitorsSelf == true) { onMonitor(true) }
                ChoiceChip(monitorNo, contact.monitorsSelf == false) { onMonitor(false) }
            }
            if (contact.monitorsSelf == true) {
                Text(reliefAdvice(contact.name), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, lineHeight = 22.sp)
                Text(notBreakingOffCopy(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LedgerScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    revision: Int,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    val contacts = remember(revision) { store.relationContacts() }
    val energy = remember(revision) { store.relationEnergy() }
    var selectedId by remember { mutableStateOf(contacts.firstOrNull()?.id) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("返回") }
        Text(ledgerTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(ledgerHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        if (contacts.isEmpty()) {
            Text("先在关系盘点里加一个人。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            contacts.forEach { contact ->
                ChoiceChip(contact.name, selectedId == contact.id, Modifier.fillMaxWidth()) { selectedId = contact.id }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selectedId?.let { store.addRelationEnergy(it, EnergyMark.Filled, nowMillis()); onChanged() }
                    },
                    enabled = selectedId != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(filledLabel) }
                OutlinedButton(
                    onClick = {
                        selectedId?.let { store.addRelationEnergy(it, EnergyMark.Drained, nowMillis()); onChanged() }
                    },
                    enabled = selectedId != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(drainedLabel) }
            }
        }
        energy.take(8).forEach { entry ->
            val name = contacts.firstOrNull { it.id == entry.contactId }?.name ?: "已删除"
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Text(
                    "$name · ${if (entry.mark == EnergyMark.Filled) filledLabel else drainedLabel}",
                    Modifier.fillMaxWidth().padding(16.dp),
                    fontSize = 15.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AltruismScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    revision: Int,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    val draws = remember(revision) { store.altruismDraws() }
    val open = draws.firstOrNull { it.felt == null }
    val pause = shouldPauseAltruism(draws)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("返回") }
        Text(altruismTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(peoplePleasingHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        if (pause) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f), shape = CardShape) {
                Text(pauseAltruismCopy(), Modifier.padding(16.dp), fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
        if (open == null) {
            Button(
                onClick = {
                    val card = nextAltruismCard(draws, preferSocial = false)
                    store.addAltruismDraw(card.title, card.kind, nowMillis())
                    onChanged()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text(drawNonSocialLabel) }
            OutlinedButton(
                onClick = {
                    val card = nextAltruismCard(draws, preferSocial = true)
                    store.addAltruismDraw(card.title, card.kind, nowMillis())
                    onChanged()
                },
                enabled = !pause,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text(drawSocialLabel) }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (open.kind == AltruismKind.NonSocial) "非社交" else "社交", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text(open.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
                }
            }
            Text(feelPrompt, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { store.completeAltruismDraw(open.id, AltruismFeel.Lighter, nowMillis()); onChanged() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(lighterLabel) }
                OutlinedButton(
                    onClick = { store.completeAltruismDraw(open.id, AltruismFeel.Tighter, nowMillis()); onChanged() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(tighterLabel) }
            }
        }
        Text(altruismFeelCopy(draws), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 14.sp)
    }
}
