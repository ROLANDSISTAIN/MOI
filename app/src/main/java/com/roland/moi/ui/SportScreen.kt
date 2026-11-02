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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Run
import com.roland.moi.data.Store
import com.roland.moi.notify.Reminders
import androidx.compose.ui.platform.LocalContext

private fun pace(r: Run): String {
    if (r.km <= 0 || r.minutes <= 0) return "—"
    val p = r.minutes / r.km
    return "%d'%02d\"/km".format(p.toInt(), ((p - p.toInt()) * 60).toInt())
}

private fun km(v: Double) = if (v == Math.floor(v)) "%.0f".format(v) else "%.1f".format(v).replace('.', ',')

@Composable
fun SportScreen(d: AppData, nav: Nav) {
    val ctx = LocalContext.current
    var edit by remember { mutableStateOf<Run?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val days = d.runs.map { Dates.dayKey(it.date) }.toSet()
    val streak = Dates.streak(days)
    val weekStart = Dates.startOfWeek(now)
    val monthStart = Dates.startOfMonth(now)
    val weekKm = d.runs.filter { it.date >= weekStart }.sumOf { it.km }
    val monthKm = d.runs.filter { it.date >= monthStart }.sumOf { it.km }
    val totalKm = d.runs.sumOf { it.km }
    val longest = d.runs.maxByOrNull { it.km }
    val fastest = d.runs.filter { it.km >= 1 && it.minutes > 0 }.minByOrNull { it.minutes / it.km }
    val today = d.runs.any { Dates.dayKey(it.date) == Dates.dayKey() }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Sport", color = Gold, fontSize = 30.sp, fontFamily = Serif)

        Card {
            SectionLabel("Matins d'affilée")
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$streak", color = Gold, fontSize = 72.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, lineHeight = 72.sp)
                Spacer(Modifier.width(8.dp))
                Text("🔥", fontSize = 26.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            Text(
                if (today) "Jogging fait aujourd'hui. Bravo 💪" else "Pas encore couru aujourd'hui.",
                color = if (today) TextMain else TextDim, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            GoldButton("+ J'ai couru", Modifier.fillMaxWidth()) { adding = true }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallStat("Semaine", "${km(weekKm)} km", Modifier.weight(1f))
            SmallStat("Mois", "${km(monthKm)} km", Modifier.weight(1f))
            SmallStat("Total", "${km(totalKm)} km", Modifier.weight(1f))
        }

        // Les 4 dernières semaines
        if (d.runs.isNotEmpty()) {
            Card {
                SectionLabel("Kilomètres par semaine")
                Spacer(Modifier.height(12.dp))
                val weeks = (3 downTo 0).map { Dates.addDays(weekStart, -7 * it) }
                val vals = weeks.map { ws -> d.runs.filter { it.date >= ws && it.date < Dates.addDays(ws, 7) }.sumOf { it.km } }
                val max = (vals.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    weeks.forEachIndexed { i, ws ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(km(vals[i]), color = TextDim, fontSize = 11.sp)
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

        if (longest != null) {
            Card {
                SectionLabel("Mes records")
                Spacer(Modifier.height(8.dp))
                Text("🏅 Plus longue course : ${km(longest.km)} km (${Dates.shortDate(longest.date)})", color = TextMain, fontSize = 14.sp)
                fastest?.let { Text("⚡ Meilleure allure : ${pace(it)} (${Dates.shortDate(it.date)})", color = TextMain, fontSize = 14.sp) }
                Text("📅 Meilleure série : ${Dates.bestStreak(days)} matins", color = TextMain, fontSize = 14.sp)
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
                androidx.compose.material3.Switch(
                    checked = d.sportOn,
                    onCheckedChange = { on -> Store.update { it.copy(sportOn = on) }; Reminders.scheduleAll(ctx) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Gold),
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
                    Column(Modifier.weight(1f)) {
                        Text(Dates.longDate(r.date), color = TextMain, fontSize = 14.sp)
                        Text(
                            "${km(r.km)} km · ${r.minutes} min · ${pace(r)}" + if (r.note.isNotBlank()) " · ${r.note}" else "",
                            color = TextDim, fontSize = 12.sp, maxLines = 2,
                        )
                    }
                    if (r.feeling > 0) Text(MOODS[r.feeling - 1], fontSize = 22.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (adding) RunDialog(null) { adding = false }
    edit?.let { r -> RunDialog(r) { edit = null } }
    if (pickTime) TimeDialog(d.sportTime, { pickTime = false }) { m ->
        Store.update { it.copy(sportTime = m, sportOn = true) }; Reminders.scheduleAll(ctx)
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Surface1, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(value, color = Gold, fontSize = 18.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun RunDialog(old: Run?, onClose: () -> Unit) {
    var kmText by remember { mutableStateOf(old?.km?.let { km(it) } ?: "") }
    var minText by remember { mutableStateOf(old?.minutes?.toString() ?: "") }
    var feeling by remember { mutableStateOf(old?.feeling ?: 4) }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var date by remember { mutableLongStateOf(old?.date ?: System.currentTimeMillis()) }
    var pickDate by remember { mutableStateOf(false) }
    val kmVal = kmText.replace(',', '.').toDoubleOrNull()
    val minVal = minText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface1,
        title = { Text(if (old == null) "🏃 Mon jogging" else "Modifier", color = Gold, fontFamily = Serif) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(kmText, { kmText = it }, label = { Text("km") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(minText, { minText = it.filter { c -> c.isDigit() } }, label = { Text("minutes") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                Text("Ressenti", color = TextDim, fontSize = 12.sp)
                MoodPicker(feeling) { feeling = it }
                OutlinedTextField(note, { note = it }, label = { Text("Note (parcours, météo…)") }, modifier = Modifier.fillMaxWidth())
                GhostButton("📅  " + Dates.shortDate(date), Modifier.fillMaxWidth()) { pickDate = true }
                if (old != null) {
                    TextButton(onClick = { Store.deleteRun(old.id); onClose() }) { Text("Supprimer cette course", color = Danger) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = kmVal != null && minVal != null, onClick = {
                Store.saveRun(Run(old?.id ?: Store.newId(), date, kmVal ?: 0.0, minVal ?: 0, feeling, note.trim()))
                onClose()
            }) { Text("Enregistrer", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler", color = TextDim) } },
    )
    if (pickDate) DateDialog(date, { pickDate = false }) { date = it }
}
