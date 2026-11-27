package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Programme pompes sur 30 jours, pensé pour quelqu'un qui fait 10 pompes d'affilée.
 * 5 séries par jour, 1 min de repos entre chaque. Repos les jours 7, 14, 21 et 28. Jour 30 : test final.
 * Semaine 1 : 27 pompes par jour, environ 6 par série → jour 29 : 75 pompes, 15 par série.
 */
object PushupPlan {
    const val DAYS = 30
    const val SETS = 5
    const val START_MAX = 10
    const val TEST_DAY = 30
    private val rest = setOf(7, 14, 21, 28)

    fun isRest(day: Int) = day in rest
    fun isTest(day: Int) = day == TEST_DAY

    /** Rang du jour d'entraînement (0 pour le jour 1, 24 pour le jour 29). */
    private fun rank(day: Int) = (1 until day).count { !isRest(it) }

    /** Objectif total du jour : de 27 (jour 1) à 75 (jour 29), +2 à chaque séance. */
    fun target(day: Int): Int = if (isRest(day) || isTest(day)) 0 else 27 + 2 * rank(day)

    /** Les 5 séries du jour, les plus grosses d'abord (ex. 6 · 6 · 5 · 5 · 5). */
    fun sets(day: Int): List<Int> {
        val t = target(day)
        if (t == 0) return emptyList()
        val base = t / SETS
        val extra = t % SETS
        return List(SETS) { if (it < extra) base + 1 else base }
    }

    /** Jour du programme (1 à 30), ou > 30 s'il est terminé. 0 si pas commencé. */
    fun dayIndex(d: AppData): Int {
        if (d.pushupStart == 0L) return 0
        return ((Dates.startOfDay() - Dates.startOfDay(d.pushupStart)) / DAY_MS).toInt() + 1
    }

    private fun runsOn(d: AppData, day: Int): List<Run> {
        val key = Dates.dayKey(Dates.addDays(d.pushupStart, day - 1))
        return d.runs.filter { it.kind == Run.KIND_POMPES && Dates.dayKey(it.date) == key }
    }

    fun doneOn(d: AppData, day: Int): Int = runsOn(d, day).sumOf { it.count }

    /** Résultat du test final (0 si pas encore fait). */
    fun testResult(d: AppData): Int =
        if (d.pushupStart == 0L) 0 else runsOn(d, TEST_DAY).filter { it.note == TEST_NOTE }.maxOfOrNull { it.count } ?: 0

    const val TEST_NOTE = "Test final"

    fun success(d: AppData, day: Int) = when {
        isRest(day) -> true
        isTest(day) -> testResult(d) > 0
        else -> doneOn(d, day) >= target(day)
    }

    fun summary(day: Int): String = when {
        isRest(day) -> "repos"
        isTest(day) -> "test final"
        else -> "${target(day)} pompes"
    }
}

@Composable
fun PushupProgramScreen(d: AppData, nav: Nav) {
    val day = PushupPlan.dayIndex(d)
    Page("Programme pompes", onBack = { nav.back() }) {
        when {
            day == 0 -> Start()
            day > PushupPlan.DAYS -> {
                val ok = (1..PushupPlan.DAYS).count { PushupPlan.success(d, it) }
                val res = PushupPlan.testResult(d)
                Card {
                    SectionLabel("Programme terminé 👑")
                    Spacer(Modifier.height(8.dp))
                    Text("$ok jours réussis sur ${PushupPlan.DAYS}.", color = TextMain, fontSize = 16.sp)
                    Text(
                        if (res > 0) "Départ : ${PushupPlan.START_MAX} pompes d'affilée · arrivée : $res"
                        else "Départ : ${PushupPlan.START_MAX} pompes d'affilée",
                        color = TextDim, fontSize = 14.sp,
                    )
                }
                Grid(d, PushupPlan.DAYS + 1)
                PrimaryButton("Recommencer le programme", Modifier.fillMaxWidth()) {
                    Store.update { it.copy(pushupStart = Dates.startOfDay(), pushupMax = PushupPlan.START_MAX) }
                }
            }
            else -> Today(d, day)
        }
    }
}

@Composable
private fun Start() {
    Card {
        SectionLabel("Le programme")
        Spacer(Modifier.height(8.dp))
        Text("Pour qui fait 10 pompes d'affilée", color = Accent, fontSize = 22.sp, fontFamily = Display)
        Spacer(Modifier.height(8.dp))
        Bullet("5 séries par jour, 1 min de repos entre chaque")
        Bullet("Semaine 1 : 27 pompes par jour (6 · 6 · 5 · 5 · 5)")
        Bullet("+2 pompes à chaque séance")
        Bullet("Jour 29 : 75 pompes (5 × 15)")
        Bullet("Repos les jours 7, 14, 21 et 28")
        Bullet("Jour 30 : test final, le maximum d'affilée")
        Spacer(Modifier.height(6.dp))
        Text("Dos droit, ventre serré, la poitrine descend presque au sol. Si une série est trop dure, finis-la sur les genoux.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Commencer aujourd'hui", Modifier.fillMaxWidth()) {
            Store.update { it.copy(pushupStart = Dates.startOfDay(), pushupMax = PushupPlan.START_MAX) }
        }
    }
}

@Composable
private fun Today(d: AppData, day: Int) {
    var confirmReset by remember { mutableStateOf(false) }

    Card {
        SectionLabel("Jour $day sur ${PushupPlan.DAYS}")
        Spacer(Modifier.height(8.dp))
        when {
            PushupPlan.isRest(day) -> {
                Text("Jour de repos 😌", color = Accent, fontSize = 28.sp, fontFamily = Display)
                Spacer(Modifier.height(6.dp))
                Text("Tes muscles grandissent pendant le repos.", color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
            }
            PushupPlan.isTest(day) -> FinalTest(d)
            else -> {
                val target = PushupPlan.target(day)
                val done = PushupPlan.doneOn(d, day)
                Text("$target pompes", color = Accent, fontSize = 44.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                Text(PushupPlan.sets(day).joinToString("  ·  "), color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("${PushupPlan.SETS} séries · 1 min de repos entre chaque", color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Row {
                    Text("Fait aujourd'hui", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("$done / $target", color = Accent, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (done.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = Surface2, drawStopIndicator = {},
                )
                Spacer(Modifier.height(14.dp))
                if (done >= target) {
                    Text("Objectif du jour atteint 💪", color = Accent, fontSize = 15.sp)
                } else {
                    PrimaryButton("J'ai fait mes ${PushupPlan.SETS} séries", Modifier.fillMaxWidth()) {
                        Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_POMPES,
                            count = target - done, sets = PushupPlan.SETS, feeling = 4, note = "Programme · jour $day"))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Tu peux aussi les noter une par une dans Sport → 💪 Pompes : elles comptent aussi.", color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }

    if (day < PushupPlan.DAYS) {
        Text("Demain : " + PushupPlan.summary(day + 1), color = TextDim, fontSize = 13.sp)
    }
    Grid(d, day)
    GhostButton("Recommencer au jour 1", Modifier.fillMaxWidth(), color = TextDim) { confirmReset = true }

    if (confirmReset) ConfirmDialog("Recommencer ?", "Le programme repartira du jour 1. Tes pompes déjà faites restent enregistrées.", "Recommencer", { confirmReset = false }) {
        Store.update { it.copy(pushupStart = Dates.startOfDay(), pushupMax = PushupPlan.START_MAX) }
    }
}

@Composable
private fun FinalTest(d: AppData) {
    val res = PushupPlan.testResult(d)
    var text by remember { mutableStateOf("") }
    val n = text.toIntOrNull() ?: 0
    Text("Test final 🏁", color = Accent, fontSize = 28.sp, fontFamily = Display)
    Spacer(Modifier.height(6.dp))
    if (res > 0) {
        Text("$res pompes d'affilée", color = Accent, fontSize = 36.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
        Text("Au départ : ${PushupPlan.START_MAX}. ${if (res > PushupPlan.START_MAX) "+${res - PushupPlan.START_MAX} en 30 jours 👑" else ""}",
            color = TextMain, fontSize = 14.sp)
    } else {
        Text("Échauffe-toi, puis fais le maximum de pompes d'affilée, sans t'arrêter. Au départ, tu en faisais ${PushupPlan.START_MAX}.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            text, { text = it.filter(Char::isDigit).take(3) }, label = { Text("Pompes d'affilée") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Enregistrer mon test", Modifier.fillMaxWidth(), enabled = n > 0) {
            Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_POMPES,
                count = n, sets = 1, feeling = 5, note = PushupPlan.TEST_NOTE))
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("•", color = Accent, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Grid(d: AppData, today: Int) {
    Card {
        SectionLabel("Mes 30 jours")
        Spacer(Modifier.height(10.dp))
        (1..PushupPlan.DAYS).chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { day ->
                    val past = day < today
                    val ok = day <= today && PushupPlan.success(d, day)
                    val bg = when {
                        ok && !PushupPlan.isRest(day) -> Accent
                        PushupPlan.isRest(day) -> Surface2
                        past -> Danger.copy(alpha = 0.35f)
                        else -> Black
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(bg)
                            .border(if (day == today) 2.dp else 1.dp, if (day == today) Accent else Line, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (PushupPlan.isRest(day)) "😌" else if (PushupPlan.isTest(day)) "🏁" else "$day",
                            color = if (ok && !PushupPlan.isRest(day)) OnAccent else TextDim, fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
