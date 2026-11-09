package com.roland.moi.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.BodyLog
import com.roland.moi.data.Dates
import com.roland.moi.data.Run
import com.roland.moi.data.Store
import kotlinx.coroutines.delay

/** Programme « ventre plat » : corde à sauter en intervalles + renforcement + alimentation + mesures. */
object VentrePlan {
    data class Seance(val rounds: Int, val workSec: Int, val restSec: Int, val perWeek: Int) {
        val totalSec get() = rounds * (workSec + restSec)
        val label get() = "$rounds × ${dur(workSec)} de saut / ${dur(restSec)} de repos"
    }

    /** Semaine du programme (1, 2, 3…), 0 si pas commencé. */
    fun week(d: AppData): Int {
        if (d.fitStart == 0L) return 0
        val days = ((Dates.startOfDay() - Dates.startOfDay(d.fitStart)) / DAY_MS).toInt().coerceAtLeast(0)
        return days / 7 + 1
    }

    /** Semaines 1-2 : 10 × 30 s / 30 s, 3 fois. Ensuite : 13 × 1 min / 30 s (≈ 20 min), 4 fois. */
    fun seance(week: Int) = if (week <= 2) Seance(10, 30, 30, 3) else Seance(13, 60, 30, 4)

    private fun weekStart(d: AppData, week: Int) = Dates.addDays(Dates.startOfDay(d.fitStart), 7 * (week - 1))

    /** Nombre de jours avec de la corde pendant la semaine en cours du programme. */
    fun doneThisWeek(d: AppData): Int {
        val w = week(d)
        if (w == 0) return 0
        val ws = weekStart(d, w)
        val we = Dates.addDays(ws, 7)
        return d.runs.filter { it.kind == Run.KIND_CORDE && it.date >= ws && it.date < we }.map { Dates.dayKey(it.date) }.toSet().size
    }

    fun doneToday(d: AppData): Boolean {
        val t = Dates.dayKey()
        return d.runs.any { it.kind == Run.KIND_CORDE && Dates.dayKey(it.date) == t }
    }

    fun dur(sec: Int) = if (sec % 60 == 0) "${sec / 60} min" else if (sec > 60) "${sec / 60} min ${sec % 60}" else "$sec s"
}

fun fmtNum(v: Double): String = if (v == Math.floor(v)) "${v.toInt()}" else "%.1f".format(v).replace('.', ',')

private fun parseNum(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

@Composable
fun VentreScreen(d: AppData, nav: Nav) {
    var timer by remember { mutableStateOf(false) }
    var measuring by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val week = VentrePlan.week(d)

    Page("Ventre plat", onBack = { nav.back() }) {
        if (week == 0) {
            Intro()
        } else if (timer) {
            IntervalTimer(VentrePlan.seance(week), week) { timer = false }
        } else {
            TodayCard(d, week) { timer = true }
            RenfoCard()
            MeasuresCard(d) { measuring = true }
            PlateCard()
            HabitsCard()
            GhostButton("Recommencer le programme à la semaine 1", Modifier.fillMaxWidth(), color = TextDim) { confirmReset = true }
        }
    }

    if (measuring) MeasureDialog(d) { measuring = false }
    if (confirmReset) ConfirmDialog(
        "Recommencer ?", "Le programme repartira de la semaine 1. Tes mesures et tes séances sont gardées.", "Recommencer",
        { confirmReset = false },
    ) { Store.update { it.copy(fitStart = Dates.startOfDay()) } }
}

@Composable
private fun Intro() {
    Card {
        SectionLabel("Le programme")
        Spacer(Modifier.height(8.dp))
        Text("Perdre du gras, surtout au ventre", color = Gold, fontSize = 22.sp, fontFamily = Serif)
        Spacer(Modifier.height(8.dp))
        Text(
            "On ne peut pas perdre uniquement le ventre : on perd du gras partout, et le ventre diminue avec le reste. " +
                "Deux leviers : l'assiette (environ 70 % du résultat) et le mouvement.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(10.dp))
        Bullet("Semaines 1-2 : corde 3 fois par semaine, 10 × 30 s de saut / 30 s de repos")
        Bullet("Semaines 3 et suivantes : 4 fois par semaine, 1 min de saut / 30 s de repos, environ 20 min")
        Bullet("10 min de pompes, squats et gainage après la corde")
        Bullet("Une assiette équilibrée à chaque repas")
        Bullet("Poids et tour de taille une fois par semaine")
        Spacer(Modifier.height(8.dp))
        Text("Objectif réaliste : environ 0,5 kg par semaine.", color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        GoldButton("Commencer aujourd'hui", Modifier.fillMaxWidth()) {
            Store.update { it.copy(fitStart = Dates.startOfDay()) }
        }
    }
}

@Composable
private fun TodayCard(d: AppData, week: Int, onStart: () -> Unit) {
    val s = VentrePlan.seance(week)
    val done = VentrePlan.doneThisWeek(d)
    Card {
        SectionLabel("Semaine $week" + if (week > 4) " · entretien" else "")
        Spacer(Modifier.height(8.dp))
        Text("🪢 ${VentrePlan.dur(s.totalSec)} de corde", color = Gold, fontSize = 30.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
        Text(s.label, color = TextMain, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        Row {
            Text("Séances cette semaine", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$done / ${s.perWeek}", color = Gold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (done.toFloat() / s.perWeek).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp), color = Gold, trackColor = Surface2, drawStopIndicator = {},
        )
        Spacer(Modifier.height(14.dp))
        when {
            VentrePlan.doneToday(d) -> Text("Séance du jour faite ✅ Bravo.", color = Gold, fontSize = 15.sp)
            done >= s.perWeek -> Text("Objectif de la semaine atteint 💪 Une séance en plus reste un bonus.", color = Gold, fontSize = 14.sp)
        }
        if (VentrePlan.doneToday(d) || done >= s.perWeek) Spacer(Modifier.height(10.dp))
        GoldButton("▶  Lancer le chrono", Modifier.fillMaxWidth(), onClick = onStart)
        Spacer(Modifier.height(4.dp))
        Text("Le chrono sonne à chaque changement et garde l'écran allumé.", color = TextDim, fontSize = 12.sp)
        if (week <= 2) {
            Spacer(Modifier.height(6.dp))
            Text("Semaine 3 : on passe à 4 séances de ${VentrePlan.dur(VentrePlan.seance(3).totalSec)}.", color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RenfoCard() {
    Card {
        SectionLabel("Après la corde · 10 min")
        Spacer(Modifier.height(8.dp))
        Bullet("💪 Pompes : 3 séries (ou ton programme pompes)")
        Bullet("🦵 Squats : 3 × 15")
        Bullet("🧱 Planche (gainage) : 3 × 30 s, ventre serré")
        Spacer(Modifier.height(6.dp))
        Text("Garder ses muscles aide à brûler plus, même au repos. Et marche le plus possible au quotidien.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun MeasuresCard(d: AppData, onAdd: () -> Unit) {
    val logs = d.bodyLogs
    val w = logs.filter { it.weight > 0 }
    val t = logs.filter { it.waist > 0 }
    Card {
        SectionLabel("Mes mesures")
        Spacer(Modifier.height(8.dp))
        if (logs.isEmpty()) {
            Text("Note ton poids et ton tour de taille (au niveau du nombril, le matin à jeun). " +
                "Le tour de taille est plus parlant que la balance.", color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Measure("Poids", w.lastOrNull()?.weight, w.firstOrNull()?.weight, "kg", Modifier.weight(1f))
                Measure("Tour de taille", t.lastOrNull()?.waist, t.firstOrNull()?.waist, "cm", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            logs.takeLast(8).reversed().forEach { b ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Dates.shortDate(Dates.parseDay(b.day)), color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        listOfNotNull(
                            b.weight.takeIf { it > 0 }?.let { fmtNum(it) + " kg" },
                            b.waist.takeIf { it > 0 }?.let { fmtNum(it) + " cm" },
                        ).joinToString(" · "),
                        color = TextMain, fontSize = 13.sp,
                    )
                }
            }
            val last = logs.last().day
            val days = ((Dates.startOfDay() - Dates.parseDay(last)) / DAY_MS).toInt()
            if (days >= 7) {
                Spacer(Modifier.height(6.dp))
                Text("Dernière mesure il y a $days jours : c'est le moment.", color = Gold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        GoldButton("Noter mes mesures", Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

@Composable
private fun Measure(label: String, now: Double?, start: Double?, unit: String, modifier: Modifier) {
    Column(modifier) {
        Text(now?.let { fmtNum(it) + " $unit" } ?: "—", color = Gold, fontSize = 22.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
        Text(label, color = TextDim, fontSize = 12.sp)
        if (now != null && start != null && now != start) {
            val diff = now - start
            Text(
                (if (diff < 0) "▼ " else "▲ ") + fmtNum(Math.abs(Math.round(diff * 10) / 10.0)) + " $unit depuis le début",
                color = if (diff < 0) Gold else Danger, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PlateCard() {
    Card {
        SectionLabel("Mon assiette")
        Spacer(Modifier.height(8.dp))
        Text("Le riz et les spaghetti ne sont pas interdits : le problème, c'est quand il n'y a que ça.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Bullet("½ assiette de légumes : gombo, épinards, adémè, tomate, chou, carotte…")
        Bullet("¼ de protéines : œufs, poisson, haricots, poulet, soja, fromage de soja")
        Bullet("¼ de riz ou spaghetti : une poignée cuite, pas une grosse assiette")
        Spacer(Modifier.height(6.dp))
        Text("Protéines et légumes calent plus longtemps : moins de faim entre les repas. " +
            "Coche « Assiette équilibrée 🥗 » dans tes habitudes quand c'est fait.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun HabitsCard() {
    Card {
        SectionLabel("Les petites habitudes")
        Spacer(Modifier.height(8.dp))
        Bullet("💧 De l'eau (fraîche ou non) à la place des jus et sodas")
        Bullet("🫗 Moins d'huile dans les sauces")
        Bullet("🌙 Ne pas manger tard le soir avant de dormir")
        Bullet("😴 Dormir 7 à 8 h : le manque de sommeil favorise le gras du ventre")
        Bullet("🚶 Marcher le plus possible")
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("•", color = Gold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
}

private enum class Phase { PREP, WORK, REST, DONE }

@Composable
private fun IntervalTimer(s: VentrePlan.Seance, week: Int, onClose: () -> Unit) {
    val view = LocalView.current
    var phase by remember { mutableStateOf(Phase.PREP) }
    var round by remember { mutableIntStateOf(1) }
    var left by remember { mutableIntStateOf(10) }
    var paused by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull() }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; tone?.release() }
    }

    LaunchedEffect(paused) {
        while (!paused && phase != Phase.DONE) {
            delay(1000)
            if (left > 1) {
                left--
                if (left <= 3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } else {
                when (phase) {
                    Phase.PREP -> { phase = Phase.WORK; left = s.workSec }
                    Phase.WORK -> if (round >= s.rounds) phase = Phase.DONE else { phase = Phase.REST; left = s.restSec }
                    Phase.REST -> { round++; phase = Phase.WORK; left = s.workSec }
                    Phase.DONE -> {}
                }
                tone?.startTone(if (phase == Phase.DONE) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP2, 500)
            }
        }
    }

    Card {
        SectionLabel("Semaine $week · ${s.label}")
        Spacer(Modifier.height(16.dp))
        if (phase == Phase.DONE) {
            Text("Séance terminée 🔥", color = Gold, fontSize = 30.sp, fontFamily = Serif, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("${s.rounds} rounds · ${VentrePlan.dur(s.totalSec)}", color = TextMain, fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            if (!saved) {
                GoldButton("Enregistrer la séance", Modifier.fillMaxWidth()) {
                    Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_CORDE,
                        minutes = (s.totalSec + 59) / 60, feeling = 4, note = "Ventre plat · semaine $week"))
                    saved = true
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Text("Enregistrée ✅ L'habitude « Corde à sauter » est cochée.", color = Gold, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("Maintenant : 10 min de pompes, squats et gainage.", color = TextDim, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
            }
            GhostButton("Fermer", Modifier.fillMaxWidth(), onClick = onClose)
        } else {
            val (title, color) = when (phase) {
                Phase.PREP -> "Prépare-toi" to TextMain
                Phase.WORK -> "SAUTE !" to Gold
                else -> "Repos" to TextDim
            }
            Text(title, color = color, fontSize = 28.sp, fontFamily = Serif, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("$left", color = color, fontSize = 110.sp, fontWeight = FontWeight.Bold, lineHeight = 110.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("Round $round / ${s.rounds}", color = TextMain, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ((round - 1).toFloat() / s.rounds).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = Gold, trackColor = Surface2, drawStopIndicator = {},
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton(if (paused) "▶  Reprendre" else "⏸  Pause", Modifier.weight(1f)) { paused = !paused }
                GhostButton("Arrêter", Modifier.weight(1f), color = TextDim, onClick = onClose)
            }
        }
    }
}

@Composable
private fun MeasureDialog(d: AppData, onClose: () -> Unit) {
    val today = Dates.dayKey()
    val old = d.bodyLogs.firstOrNull { it.day == today }
    var weight by remember { mutableStateOf(old?.weight?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    var waist by remember { mutableStateOf(old?.waist?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    val w = parseNum(weight)
    val t = parseNum(waist)
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface1,
        title = { Text("Mes mesures du jour", color = Gold, fontFamily = Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Poids (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = num, singleLine = true)
                OutlinedTextField(waist, { waist = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Tour de taille (cm)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = num, singleLine = true)
                Text("Mesure le tour de taille au niveau du nombril, le matin, avant de manger.", color = TextDim, fontSize = 12.sp)
                if (old != null) {
                    TextButton(onClick = { Store.deleteBody(today); onClose() }) { Text("Supprimer la mesure du jour", color = Danger) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = w > 0 || t > 0, onClick = {
                Store.saveBody(BodyLog(today, w, t)); onClose()
            }) { Text("Enregistrer", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler", color = TextDim) } },
    )
}
