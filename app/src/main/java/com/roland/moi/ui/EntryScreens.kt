package com.roland.moi.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Entry
import com.roland.moi.data.Photos
import com.roland.moi.data.Section
import com.roland.moi.data.Step
import com.roland.moi.data.Store

private fun isSealed(e: Entry) =
    e.section == Section.CAPSULES.key && e.dueDate > System.currentTimeMillis()

// ======================= Liste d'une section =======================

@Composable
fun SectionScreen(d: AppData, key: String, nav: Nav) {
    val s = Section.of(key)
    val items = d.entries.filter { it.section == s.key }.sortedWith(
        compareBy<Entry> { it.done }.thenByDescending { if (s.hasDate) it.date else it.created }
    )
    Page(s.title, onBack = { nav.back() }) {
        Text(s.subtitle, color = TextDim, fontSize = 14.sp)
        GoldButton("+ Ajouter", Modifier.fillMaxWidth()) { nav.go(Route.EntryEdit(s.key)) }
        if (items.isEmpty()) EmptyState(s.empty)

        val groups: List<Pair<String, List<Entry>>> =
            if (s.tags.isNotEmpty()) {
                (s.tags + "").map { t -> t to items.filter { if (t.isEmpty()) it.tag !in s.tags else it.tag == t } }
                    .filter { it.second.isNotEmpty() }
            } else listOf("" to items)

        groups.forEach { (tag, list) ->
            if (tag.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionLabel(tag)
            }
            list.forEach { e -> EntryRow(s, e) { nav.go(Route.EntryView(e.id)) } }
        }
    }
}

@Composable
private fun EntryRow(s: Section, e: Entry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1)
            .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (s.hasPhoto) {
            Avatar(e.photo, s.emoji)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            val sealed = isSealed(e)
            Text(
                if (sealed) "🔒 " + e.title else e.title,
                color = if (e.done) TextDim else TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (e.done && s == Section.BUTS) TextDecoration.LineThrough else null,
            )
            val sub = when {
                sealed -> "S'ouvrira le ${Dates.shortDate(e.dueDate)}"
                s == Section.CAPSULES -> "Ouverte · écrite le ${Dates.shortDate(e.created)}"
                s.hasDate && e.date > 0 -> Dates.shortDate(e.date) + if (e.text.isNotBlank()) " · " + e.text else ""
                else -> e.text
            }
            if (sub.isNotBlank()) Text(sub, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            if (s.hasSteps && e.steps.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { e.steps.count { it.done }.toFloat() / e.steps.size },
                    modifier = Modifier.fillMaxWidth().height(4.dp), color = Gold, trackColor = Surface2, drawStopIndicator = {},
                )
            }
        }
        if (e.done && s.doneLabel != null) {
            Spacer(Modifier.width(8.dp))
            Text("✓ " + s.doneLabel, color = Gold, fontSize = 12.sp)
        }
    }
}

// ======================= Lecture d'une entrée =======================

@Composable
fun EntryViewScreen(d: AppData, id: String, nav: Nav) {
    val ctx = LocalContext.current
    val e = d.entries.firstOrNull { it.id == id }
    if (e == null) { LaunchedEffect(Unit) { nav.back() }; return }
    val s = Section.of(e.section)
    var askDelete by remember { mutableStateOf(false) }
    val sealed = isSealed(e)

    // Une capsule ouverte pour la première fois est marquée comme lue
    LaunchedEffect(e.id) {
        if (s == Section.CAPSULES && !sealed && !e.done) Store.saveEntry(e.copy(done = true, doneDate = System.currentTimeMillis()))
    }

    Page(s.title, onBack = { nav.back() }, actions = {
        if (!sealed) IconButton(onClick = { nav.go(Route.EntryEdit(s.key, e.id)) }) { Icon(Icons.Outlined.Edit, "Modifier", tint = Gold) }
        IconButton(onClick = { askDelete = true }) { Icon(Icons.Outlined.Delete, "Supprimer", tint = TextDim) }
    }) {
        if (sealed) {
            Spacer(Modifier.height(40.dp))
            Text("🔒", fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(e.title, color = Gold, fontSize = 24.sp, fontFamily = Serif, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(
                "Scellée le ${Dates.shortDate(e.created)}.\nElle s'ouvrira le ${Dates.longDate(e.dueDate)}.",
                color = TextDim, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            return@Page
        }

        if (s.hasPhoto && e.photo.isNotBlank()) PhotoBox(e.photo, 260.dp)
        if (e.tag.isNotBlank()) SectionLabel(e.tag)
        Text(e.title, color = TextMain, fontSize = 26.sp, fontFamily = Serif, lineHeight = 32.sp)
        val meta = buildList {
            if (s.hasDate && e.date > 0) add(Dates.longDate(e.date))
            if (s == Section.CAPSULES) add("Écrite le ${Dates.shortDate(e.created)}")
            else if (s.hasDue && e.dueDate > 0) add("${s.dueLabel} : ${Dates.shortDate(e.dueDate)}")
            if (e.done && e.doneDate > 0 && s.doneLabel != null && s != Section.CAPSULES) add("${s.doneLabel} le ${Dates.shortDate(e.doneDate)}")
        }
        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), color = Gold, fontSize = 13.sp)
        if (e.text.isNotBlank()) {
            Text(
                e.text, color = TextMain, fontSize = 17.sp, lineHeight = 26.sp,
                fontFamily = if (s == Section.LETTRES || s == Section.CAPSULES || s == Section.FOI) Serif else null,
                fontStyle = if (s == Section.FOI && e.tag == "Verset") FontStyle.Italic else null,
            )
        }

        if (s.hasSteps && e.steps.isNotEmpty()) {
            Card {
                SectionLabel("Étapes · ${e.steps.count { it.done }}/${e.steps.size}")
                Spacer(Modifier.height(8.dp))
                e.steps.forEachIndexed { i, st ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val steps = e.steps.toMutableList().also { it[i] = st.copy(done = !st.done) }
                            Store.saveEntry(e.copy(steps = steps))
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBox(st.done)
                        Spacer(Modifier.width(12.dp))
                        Text(st.text, color = if (st.done) TextDim else TextMain, fontSize = 15.sp,
                            textDecoration = if (st.done) TextDecoration.LineThrough else null)
                    }
                }
            }
        }

        val canBeDone = s.doneLabel != null && s != Section.CAPSULES && !(s == Section.FOI && e.tag == "Verset")
        if (canBeDone) {
            if (e.done) {
                GhostButton("Annuler « ${s.doneLabel} »", Modifier.fillMaxWidth(), color = TextDim) {
                    Store.saveEntry(e.copy(done = false, doneDate = 0L))
                }
            } else {
                GoldButton("Marquer comme « ${s.doneLabel} »", Modifier.fillMaxWidth()) {
                    Store.saveEntry(e.copy(done = true, doneDate = System.currentTimeMillis()))
                }
            }
        }
    }

    if (askDelete) ConfirmDialog("Supprimer ?", "« ${e.title} » sera supprimé définitivement.", "Supprimer", { askDelete = false }) {
        Photos.delete(ctx, e.photo)
        Store.deleteEntry(e.id)
        nav.back()
    }
}

@Composable
fun CheckBox(checked: Boolean) {
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
            .background(if (checked) Gold else Surface2)
            .border(1.dp, if (checked) Gold else Line, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) { if (checked) Text("✓", color = Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

// ======================= Ajout / modification =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryEditScreen(d: AppData, key: String, id: String?, nav: Nav) {
    val ctx = LocalContext.current
    val s = Section.of(key)
    val old = remember(id) { id?.let { i -> d.entries.firstOrNull { it.id == i } } }
    var title by remember { mutableStateOf(old?.title ?: "") }
    var text by remember { mutableStateOf(old?.text ?: "") }
    var tag by remember { mutableStateOf(old?.tag ?: s.tags.firstOrNull() ?: "") }
    var date by remember { mutableLongStateOf(old?.date ?: if (s.hasDate) System.currentTimeMillis() else 0L) }
    var due by remember { mutableLongStateOf(old?.dueDate ?: if (s == Section.CAPSULES) Dates.addDays(System.currentTimeMillis(), 365) else 0L) }
    var photo by remember { mutableStateOf(old?.photo ?: "") }
    val steps = remember { mutableStateListOf<Step>().apply { addAll(old?.steps ?: emptyList()) } }
    var newStep by remember { mutableStateOf("") }
    var pickDate by remember { mutableStateOf(false) }
    var pickDue by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = Photos.import(ctx, uri)
            if (name.isNotBlank()) { if (photo != old?.photo) Photos.delete(ctx, photo); photo = name }
        }
    }

    Page(if (old == null) "${s.emoji} Nouveau" else "Modifier", onBack = { nav.back() }) {
        Text(s.title, color = TextDim, fontSize = 14.sp)

        if (s.tags.isNotEmpty()) {
            Text(s.tagLabel, color = TextDim, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.tags.forEach { t -> Chip(t, tag == t) { tag = t } }
            }
        }

        Field(title, { title = it }, s.titleLabel)
        Field(text, { text = it }, s.textLabel, minLines = if (s == Section.LETTRES || s == Section.CAPSULES) 8 else 4)

        if (s.hasDate) {
            GhostButton("📅  " + (if (date > 0) Dates.longDate(date) else "Choisir la date"), Modifier.fillMaxWidth()) { pickDate = true }
        }
        if (s.hasDue) {
            val label = if (due > 0) "${s.dueLabel} : ${Dates.shortDate(due)}" else "${s.dueLabel} (facultatif)"
            GhostButton("⏳  $label", Modifier.fillMaxWidth()) { pickDue = true }
        }

        if (s.hasPhoto) {
            if (photo.isNotBlank()) {
                Box {
                    PhotoBox(photo, 200.dp)
                    IconButton(onClick = { if (photo != old?.photo) Photos.delete(ctx, photo); photo = "" }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Outlined.Close, "Retirer", tint = Gold)
                    }
                }
            }
            GhostButton(if (photo.isBlank()) "📷  Ajouter une photo" else "📷  Changer la photo", Modifier.fillMaxWidth()) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        if (s.hasSteps) {
            SectionLabel("Étapes")
            steps.forEachIndexed { i, st ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}. ${st.text}", color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { steps.removeAt(i) }) { Icon(Icons.Outlined.Close, "Retirer", tint = TextDim) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(newStep, { newStep = it }, "Nouvelle étape", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GhostButton("+") { if (newStep.isNotBlank()) { steps.add(Step(newStep.trim())); newStep = "" } }
            }
        }

        if (s == Section.CAPSULES) {
            Text("Une fois enregistrée, la lettre sera scellée jusqu'à la date d'ouverture. Même toi, tu ne pourras pas la relire avant.",
                color = TextDim, fontSize = 12.sp, lineHeight = 17.sp)
        }

        GoldButton("Enregistrer", Modifier.fillMaxWidth(), enabled = title.isNotBlank() || text.isNotBlank()) {
            if (old != null && old.photo != photo) Photos.delete(ctx, old.photo)
            val e = (old ?: Entry(id = Store.newId(), section = s.key)).copy(
                title = title.trim().ifBlank { text.trim().take(40) },
                text = text.trim(), tag = tag, date = date, dueDate = due, photo = photo, steps = steps.toList(),
            )
            Store.saveEntry(e)
            if (old == null) nav.replace(Route.EntryView(e.id)) else nav.back()
        }
    }

    if (pickDate) DateDialog(date, { pickDate = false }) { date = it }
    if (pickDue) DateDialog(due, { pickDue = false }) { due = it }
}
