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
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Programme pompes sur 30 jours, calculé à partir du test de départ. */
object PushupPlan {
    const val DAYS = 30
    const val SETS = 5
    private val rest = setOf(7, 14, 21, 28)

    fun isRest(day: Int) = day in rest

    /** Objectif total du jour : de 2× ton max (jour 1) à 4,5× ton max (jour 30). */
    fun target(day: Int, max: Int): Int {
        if (isRest(day)) return 0
        val m = max.coerceAtLeast(1)
        return (m * (2.0 + 2.5 * (day - 1) / (DAYS - 1))).roundToInt().coerceAtLeast(SETS)
    }

    fun perSet(day: Int, max: Int) = ceil(target(day, max) / SETS.toDouble()).toInt()

    /** Jour du programme (1 à 30), ou > 30 s'il est terminé. 0 si pas commencé. */
    fun dayIndex(d: AppData): Int {
        if (d.pushupStart == 0L) return 0
        return ((Dates.startOfDay() - Dates.startOfDay(d.pushupStart)) / DAY_MS).toInt() + 1
    }

    fun doneOn(d: AppData, day: Int): Int {
        val key = Dates.dayKey(Dates.addDays(d.pushupStart, day - 1))
        return d.runs.filter { it.kind == Run.KIND_POMPES && Dates.dayKey(it.date) == key }.sumOf { it.count }
    }

    fun success(d: AppData, day: Int) = isRest(day) || doneOn(d, day) >= target(day, d.pushupMax)
}

@Composable
fun PushupProgramScreen(d: AppData, nav: Nav) {
    val day = PushupPlan.dayIndex(d)
    Page("Programme pompes", onBack = { nav.back() }) {
        when {
            day == 0 -> StartTest(null)
            day > PushupPlan.DAYS -> {
                val ok = (1..PushupPlan.DAYS).count { PushupPlan.success(d, it) }
                Card {
                    SectionLabel("Programme terminé 👑")
                    Spacer(Modifier.height(8.dp))
                    Text("$ok jours réussis sur ${PushupPlan.DAYS}.", color = TextMain, fontSize = 16.sp)
                    Text("Ton test de départ : ${d.pushupMax} pompes d'affilée.", color = TextDim, fontSize = 14.sp)
                }
                Grid(d, PushupPlan.DAYS + 1)
                StartTest("Refais le test : combien maintenant ?")
            }
            else -> Today(d, day)
        }
    }
}

@Composable
private fun StartTest(title: String?) {
    var text by remember { mutableStateOf("") }
    val n = text.toIntOrNull() ?: 0
    Card {
        SectionLabel(title ?: "Le test de départ")
        Spacer(Modifier.height(8.dp))
        Text(
            "Fais le maximum de pompes d'affilée, sans t'arrêter, en gardant le dos droit. " +
                "MOI calcule ensuite ton programme sur 30 jours : 5 séries par jour, de plus en plus fortes, avec un jour de repos chaque semaine.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            text, { text = it.filter(Char::isDigit) }, label = { Text("Pompes d'affilée") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(12.dp))
        GoldButton("Commencer le programme", Modifier.fillMaxWidth(), enabled = n > 0) {
            Store.update { it.copy(pushupStart = Dates.startOfDay(), pushupMax = n) }
        }
    }
}

@Composable
private fun Today(d: AppData, day: Int) {
    var confirmReset by remember { mutableStateOf(false) }
    val target = PushupPlan.target(day, d.pushupMax)
    val done = PushupPlan.doneOn(d, day)

    Card {
        SectionLabel("Jour $day sur ${PushupPlan.DAYS}")
        Spacer(Modifier.height(8.dp))
        if (PushupPlan.isRest(day)) {
            Text("Jour de repos 😌", color = Gold, fontSize = 28.sp, fontFamily = Serif)
            Spacer(Modifier.height(6.dp))
            Text("Tes muscles grandissent pendant le repos. Profite, la corde à sauter reste permise.", color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        } else {
            Text("$target pompes", color = Gold, fontSize = 44.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
            Text("${PushupPlan.SETS} séries de ${PushupPlan.perSet(day, d.pushupMax)} · 1 min de repos entre chaque", color = TextMain, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Row {
                Text("Fait aujourd'hui", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("$done / $target", color = Gold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (done.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = Gold, trackColor = Surface2, drawStopIndicator = {},
            )
            Spacer(Modifier.height(14.dp))
            if (done >= target) {
                Text("Objectif du jour atteint 💪", color = Gold, fontSize = 15.sp)
            } else {
                GoldButton("J'ai fait mes ${PushupPlan.SETS} séries", Modifier.fillMaxWidth()) {
                    Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_POMPES,
                        count = target - done, sets = PushupPlan.SETS, feeling = 4, note = "Programme · jour $day"))
                }
                Spacer(Modifier.height(4.dp))
                Text("Tu peux aussi les noter une par une dans Sport → 💪 Pompes : elles comptent aussi.", color = TextDim, fontSize = 12.sp)
            }
        }
    }

    if (day < PushupPlan.DAYS) {
        val next = day + 1
        Text(
            "Demain : " + if (PushupPlan.isRest(next)) "repos" else "${PushupPlan.target(next, d.pushupMax)} pompes",
            color = TextDim, fontSize = 13.sp,
        )
    }
    Grid(d, day)
    Text("Test de départ : ${d.pushupMax} pompes d'affilée", color = TextDim, fontSize = 12.sp)
    GhostButton("Recommencer avec un nouveau test", Modifier.fillMaxWidth(), color = TextDim) { confirmReset = true }

    if (confirmReset) ConfirmDialog("Recommencer ?", "Le programme repartira du jour 1 avec un nouveau test.", "Recommencer", { confirmReset = false }) {
        Store.update { it.copy(pushupStart = 0L, pushupMax = 0) }
    }
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
                        ok && !PushupPlan.isRest(day) -> Gold
                        PushupPlan.isRest(day) -> Surface2
                        past -> Danger.copy(alpha = 0.35f)
                        else -> Black
                    }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(bg)
                            .border(if (day == today) 2.dp else 1.dp, if (day == today) Gold else Line, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (PushupPlan.isRest(day)) "😌" else "$day",
                            color = if (ok && !PushupPlan.isRest(day)) Black else TextDim, fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
