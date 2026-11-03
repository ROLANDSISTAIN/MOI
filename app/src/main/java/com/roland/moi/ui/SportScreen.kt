package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Run
import com.roland.moi.data.Store
import com.roland.moi.notify.Reminders

fun kindEmoji(k: String) = when (k) { Run.KIND_POMPES -> "💪"; Run.KIND_CORDE -> "🪢"; else -> "🏃" }
fun kindName(k: String) = when (k) { Run.KIND_POMPES -> "Pompes"; Run.KIND_CORDE -> "Corde à sauter"; else -> "Course" }

fun sessionSummary(r: Run): String = when (r.kind) {
    Run.KIND_POMPES -> "${r.count} pompes" + (if (r.sets > 0) " en ${r.sets} séries" else "") + (if (r.minutes > 0) " · ${r.minutes} min" else "")
    Run.KIND_CORDE -> "${r.minutes} min" + if (r.count > 0) " · ${r.count} sauts" else ""
    else -> "%.1f km · %d min".format(r.km, r.minutes).replace('.', ',')
}

@Composable
fun SportScreen(d: AppData, nav: Nav) {
    val ctx = LocalContext.current
    var edit by remember { mutableStateOf<Run?>(null) }
    var adding by remember { mutableStateOf<String?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val todayKey = Dates.dayKey()
    val days = d.runs.map { Dates.dayKey(it.date) }.toSet()
    val streak = Dates.streak(days)
    val weekStart = Dates.startOfWeek(now)
    val pompes = d.runs.filter { it.kind == Run.KIND_POMPES }
    val corde = d.runs.filter { it.kind == Run.KIND_CORDE }
    val todayPompes = pompes.filter { Dates.dayKey(it.date) == todayKey }.sumOf { it.count }
    val todayCorde = corde.filter { Dates.dayKey(it.date) == todayKey }.sumOf { it.minutes }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Sport", color = Gold, fontSize = 30.sp, fontFamily = Serif)

        Card {
            SectionLabel("Jours d'affilée")
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$streak", color = Gold, fontSize = 72.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, lineHeight = 72.sp)
                Spacer(Modifier.width(8.dp))
                Text("🔥", fontSize = 26.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            Text("Aujourd'hui : 🪢 $todayCorde min · 💪 $todayPompes pompes", color = TextMain, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton("🪢 Corde", Modifier.weight(1f)) { adding = Run.KIND_CORDE }
                GoldButton("💪 Pompes", Modifier.weight(1f)) { adding = Run.KIND_POMPES }
            }
        }

        // ---- Programme pompes 30 jours ----
        val pDay = PushupPlan.dayIndex(d)
        ListRow(
            emoji = "📅",
            title = "Programme pompes 30 jours",
            subtitle = when {
                pDay == 0 -> "Fais le test de départ pour commencer"
                pDay > PushupPlan.DAYS -> "Terminé 👑 — refais le test"
                PushupPlan.isRest(pDay) -> "Jour $pDay · repos"
                else -> "Jour $pDay · objectif ${PushupPlan.target(pDay, d.pushupMax)} pompes"
            },
            trailing = if (pDay in 1..PushupPlan.DAYS) "$pDay/30" else "",
        ) { nav.go(Route.Pushups) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStat("Pompes\ncette semaine", "${pompes.filter { it.date >= weekStart }.sumOf { it.count }}", Modifier.weight(1f))
            SmallStat("Corde\ncette semaine", "${corde.filter { it.date >= weekStart }.sumOf { it.minutes }} min", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStat("Pompes\nau total", "${pompes.sumOf { it.count }}", Modifier.weight(1f))
            SmallStat("Corde\nau total", "${corde.sumOf { it.minutes }} min", Modifier.weight(1f))
        }

        if (pompes.isNotEmpty()) WeekChart("Pompes par semaine", d.runs, Run.KIND_POMPES, weekStart) { it.count.toDouble() }
        if (corde.isNotEmpty()) WeekChart("Minutes de corde par semaine", d.runs, Run.KIND_CORDE, weekStart) { it.minutes.toDouble() }

        if (d.runs.isNotEmpty()) {
            Card {
                SectionLabel("Mes records")
                Spacer(Modifier.height(8.dp))
                pompes.maxByOrNull { it.count }?.let {
                    Text("💪 Plus de pompes en une séance : ${it.count} (${Dates.shortDate(it.date)})", color = TextMain, fontSize = 14.sp)
                }
                pompes.filter { it.sets > 0 }.maxByOrNull { it.count / it.sets }?.let {
                    Text("🔝 Meilleure série : ~${it.count / it.sets} pompes (${Dates.shortDate(it.date)})", color = TextMain, fontSize = 14.sp)
                }
                corde.maxByOrNull { it.minutes }?.let {
                    Text("🪢 Plus longue séance de corde : ${it.minutes} min (${Dates.shortDate(it.date)})", color = TextMain, fontSize = 14.sp)
                }
                Text("📅 Meilleure série de jours : ${Dates.bestStreak(days)}", color = TextMain, fontSize = 14.sp)
            }
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Rappel du matin")
                    Spacer(Modifier.height(4.dp))
                    Text(if (d.sportOn) "Chaque jour à ${Dates.time(d.sportTime)}" else "Désactivé", color = TextMain, fontSize = 14.sp)
                }
                TextButton(onClick = { pickTime = true }) { Text("Heure", color = Gold) }
                Switch(
                    checked = d.sportOn,
                    onCheckedChange = { on -> Store.update { it.copy(sportOn = on) }; Reminders.scheduleAll(ctx) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Gold),
                )
            }
        }

        if (d.runs.isNotEmpty()) {
            SectionLabel("Historique")
            d.runs.sortedByDescending { it.date }.take(60).forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1)
                        .border(1.dp, Line, RoundedCornerShape(14.dp)).clickable { edit = r }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(kindEmoji(r.kind), fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Dates.longDate(r.date), color = TextMain, fontSize = 14.sp)
                        Text(sessionSummary(r) + if (r.note.isNotBlank()) " · ${r.note}" else "", color = TextDim, fontSize = 12.sp, maxLines = 2)
                    }
                    if (r.feeling > 0) Text(MOODS[r.feeling - 1], fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    adding?.let { k -> SessionDialog(k, null) { adding = null } }
    edit?.let { r -> SessionDialog(r.kind, r) { edit = null } }
    if (pickTime) TimeDialog(d.sportTime, { pickTime = false }) { m ->
        Store.update { it.copy(sportTime = m, sportOn = true) }; Reminders.scheduleAll(ctx)
    }
}

@Composable
private fun WeekChart(title: String, runs: List<Run>, kind: String, weekStart: Long, value: (Run) -> Double) {
    Card {
        SectionLabel(title)
        Spacer(Modifier.height(12.dp))
        val weeks = (3 downTo 0).map { Dates.addDays(weekStart, -7 * it) }
        val vals = weeks.map { ws -> runs.filter { it.kind == kind && it.date >= ws && it.date < Dates.addDays(ws, 7) }.sumOf(value) }
        val max = (vals.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            weeks.forEachIndexed { i, ws ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("${vals[i].toInt()}", color = TextDim, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((vals[i] / max).toFloat().coerceAtLeast(0.03f) * 0.7f)
                            .clip(RoundedCornerShape(6.dp)).background(if (i == 3) Gold else Gold.copy(alpha = 0.45f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(if (i == 3) "Cette sem." else Dates.shortDate(ws).substringBeforeLast(" "), color = TextDim, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Surface1, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(value, color = Gold, fontSize = 20.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun SessionDialog(kind: String, old: Run?, onClose: () -> Unit) {
    val isPompes = kind == Run.KIND_POMPES
    var countText by remember { mutableStateOf(old?.count?.takeIf { it > 0 }?.toString() ?: "") }
    var setsText by remember { mutableStateOf(old?.sets?.takeIf { it > 0 }?.toString() ?: "") }
    var minText by remember { mutableStateOf(old?.minutes?.takeIf { it > 0 }?.toString() ?: "") }
    var feeling by remember { mutableIntStateOf(old?.feeling ?: 4) }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var date by remember { mutableLongStateOf(old?.date ?: System.currentTimeMillis()) }
    var pickDate by remember { mutableStateOf(false) }
    val count = countText.toIntOrNull() ?: 0
    val sets = setsText.toIntOrNull() ?: 0
    val minutes = minText.toIntOrNull() ?: 0
    val valid = if (isPompes) count > 0 else minutes > 0
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface1,
        title = { Text("${kindEmoji(kind)} ${kindName(kind)}", color = Gold, fontFamily = Serif) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isPompes) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(countText, { countText = it.filter(Char::isDigit) }, label = { Text("Pompes (total)") },
                            modifier = Modifier.weight(1f), keyboardOptions = num, singleLine = true)
                        OutlinedTextField(setsText, { setsText = it.filter(Char::isDigit) }, label = { Text("Séries") },
                            modifier = Modifier.weight(1f), keyboardOptions = num, singleLine = true)
                    }
                    OutlinedTextField(minText, { minText = it.filter(Char::isDigit) }, label = { Text("Minutes (facultatif)") },
                        modifier = Modifier.fillMaxWidth(), keyboardOptions = num, singleLine = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(minText, { minText = it.filter(Char::isDigit) }, label = { Text("Minutes") },
                            modifier = Modifier.weight(1f), keyboardOptions = num, singleLine = true)
                        OutlinedTextField(countText, { countText = it.filter(Char::isDigit) }, label = { Text("Sauts (facult.)") },
                            modifier = Modifier.weight(1f), keyboardOptions = num, singleLine = true)
                    }
                }
                Text("Ressenti", color = TextDim, fontSize = 12.sp)
                MoodPicker(feeling) { feeling = it }
                OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                GhostButton("📅  " + Dates.shortDate(date), Modifier.fillMaxWidth()) { pickDate = true }
                if (old != null) {
                    TextButton(onClick = { Store.deleteRun(old.id); onClose() }) { Text("Supprimer cette séance", color = Danger) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                Store.saveRun(
                    (old ?: Run(id = Store.newId(), date = date, kind = kind)).copy(
                        date = date, minutes = minutes, count = count, sets = sets, feeling = feeling, note = note.trim(),
                    )
                )
                onClose()
            }) { Text("Enregistrer", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler", color = TextDim) } },
    )
    if (pickDate) DateDialog(date, { pickDate = false }) { date = it }
}
