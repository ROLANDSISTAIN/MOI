package com.roland.moi.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import com.roland.moi.data.Run
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.BodyLog
import com.roland.moi.data.Dates
import com.roland.moi.data.Store

fun fmtNum(v: Double): String = if (v == Math.floor(v)) "${v.toInt()}" else "%.1f".format(v).replace('.', ',')

private fun parseNum(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

/** Programme « perte de poids » : la journée type (repas + séance de midi), l'assiette et les mesures. */
@Composable
fun VentreScreen(d: AppData, nav: Nav) {
    var measuring by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(false) }

    Page("Perte de poids", onBack = { if (timer) timer = false else nav.back() }) {
        if (timer) {
            SeanceTimer(onPompes = { timer = false; nav.go(Route.Pushups) }) { timer = false }
        } else {
            IntroCard()
            SeanceCard(d) { timer = true }
            DayPlanCard()
            SaladCard()
            LunchCard()
            MeasuresCard(d) { measuring = true }
            HabitsCard()
        }
    }

    if (measuring) MeasureDialog(d) { measuring = false }
}

@Composable
private fun IntroCard() {
    Card {
        SectionLabel("Le programme")
        Spacer(Modifier.height(8.dp))
        Text("Perdre du gras, surtout au ventre", color = Accent, fontSize = 22.sp, fontFamily = Display)
        Spacer(Modifier.height(8.dp))
        Text(
            "On ne peut pas perdre uniquement le ventre : on perd du gras partout, et le ventre diminue avec le reste. " +
                "L'assiette fait l'essentiel du résultat. Objectif réaliste : environ 0,5 kg par semaine.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
        )
    }
}

/** La journée type, chaque jour. */
@Composable
private fun DayPlanCard() {
    Card {
        SectionLabel("Ma journée type")
        Spacer(Modifier.height(10.dp))
        Slot("8 h", "Petit-déjeuner", "La salade d'œufs, chou et légumes")
        Slot("12 h", "Séance dans la chambre", "Fractionné 20 min + pompes du jour, puis la douche")
        Slot("13 h 30", "Déjeuner", "Riz, pâte ou spaghetti avec sauce : ¼ d'assiette de féculent")
        Slot("16 h 30", "Si j'ai faim", "Un fruit : orange, papaye, ananas")
        Slot("Après 19 h", "Plus rien à manger", "Seulement de l'eau")
        Spacer(Modifier.height(6.dp))
        Text("Après 10 h, ne mange plus avant la séance : on s'entraîne mieux le ventre léger. Bois de l'eau avant et après.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun Slot(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(time, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(84.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
}

/** Le plat du matin. */
@Composable
private fun SaladCard() {
    Card {
        SectionLabel("Le plat du matin")
        Spacer(Modifier.height(8.dp))
        Text("Salade d'œufs, chou et légumes", color = Accent, fontSize = 20.sp, fontFamily = Display)
        Spacer(Modifier.height(10.dp))
        Text("Pour 1 personne", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Bullet("2 œufs")
        Bullet("100 g de chou émincé")
        Bullet("2 tomates")
        Bullet("1 concombre")
        Bullet("1 petit oignon")
        Bullet("1 citron, un peu de sel")
        Spacer(Modifier.height(8.dp))
        Text("Préparation", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Bullet("1. Fais bouillir les œufs 10 min, passe-les à l'eau froide et écale-les.")
        Bullet("2. Coupe le chou en fines lamelles et lave-le bien.")
        Bullet("3. Coupe les tomates, le concombre et l'oignon, et mets-les dans un bol avec le chou.")
        Bullet("4. Ajoute les œufs coupés, le jus de citron et le sel. Mélange.")
        Spacer(Modifier.height(6.dp))
        Text("Pas de pain ni de gari avec.", color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/** Le plat de midi. */
@Composable
private fun LunchCard() {
    Card {
        SectionLabel("Le plat de midi")
        Spacer(Modifier.height(8.dp))
        Text("Riz, pâte ou spaghetti avec sauce", color = Accent, fontSize = 20.sp, fontFamily = Display)
        Spacer(Modifier.height(8.dp))
        Text("Le riz, la pâte et les spaghetti ne sont pas interdits : le problème, c'est quand il n'y a que ça.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Bullet("¼ d'assiette de riz, pâte ou spaghetti : une louche, la taille d'un poing fermé")
        Bullet("¼ de protéines : poisson, poulet, œufs, haricots, fromage de soja")
        Bullet("½ de sauce et de légumes : gombo, adémè, épinards, tomate, chou…")
        Bullet("Peu d'huile dans la sauce, et on ne se ressert pas")
        Spacer(Modifier.height(6.dp))
        Text("Coche « Assiette équilibrée 🥗 » dans tes habitudes quand c'est fait.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/**
 * Séance « perte du gras du ventre » : fractionné sans matériel, dans la chambre (≈ 21 min).
 * Le cardio d'intensité moyenne à élevée et le fractionné réduisent la graisse profonde du ventre
 * (méta-analyses : Sports Med 2018 ; 84 essais, 2024). 3 à 4 fois par semaine, 12 semaines au moins.
 */
object VentreSeance {
    const val PER_WEEK = 4
    const val ROUNDS = 4
    const val WORK = 30
    const val REST = 30
    const val WARMUP = 180
    const val COOLDOWN = 120
    val EXERCISES = listOf(
        "Montées de genoux" to "Sur place, genoux à hauteur des hanches, le plus vite possible",
        "Jumping jacks" to "Saute en écartant bras et jambes, puis reviens",
        "Squats rapides" to "Fesses en arrière, dos droit, remonte vite",
        "Burpees" to "Ou mountain climbers si c'est trop dur",
    )

    data class Step(val title: String, val detail: String, val sec: Int, val work: Boolean, val label: String)

    val STEPS: List<Step> = buildList {
        add(Step("Échauffement", "Marche sur place, bouge les bras", WARMUP, false, ""))
        for (r in 1..ROUNDS) {
            EXERCISES.forEachIndexed { i, (name, detail) ->
                add(Step(name, detail, WORK, true, "Tour $r / $ROUNDS"))
                if (!(r == ROUNDS && i == EXERCISES.lastIndex)) {
                    val next = EXERCISES[(i + 1) % EXERCISES.size].first
                    add(Step("Repos", "Ensuite : $next", REST, false, "Tour $r / $ROUNDS"))
                }
            }
        }
        add(Step("Retour au calme", "Marche lentement, respire profondément", COOLDOWN, false, ""))
    }

    val minutes: Int get() = (STEPS.sumOf { it.sec } + 59) / 60

    fun doneThisWeek(d: AppData): Int {
        val ws = Dates.startOfWeek()
        return d.runs.filter { it.kind == Run.KIND_VENTRE && it.date >= ws }.map { Dates.dayKey(it.date) }.toSet().size
    }

    fun doneToday(d: AppData): Boolean {
        val t = Dates.dayKey()
        return d.runs.any { it.kind == Run.KIND_VENTRE && Dates.dayKey(it.date) == t }
    }
}

/** La séance de midi : fractionné dans la chambre, puis les pompes du jour. */
@Composable
private fun SeanceCard(d: AppData, onStart: () -> Unit) {
    val done = VentreSeance.doneThisWeek(d)
    val per = VentreSeance.PER_WEEK
    Card {
        SectionLabel("Séance perte du gras du ventre")
        Spacer(Modifier.height(8.dp))
        Text(if (VentreSeance.doneToday(d)) "Séance du jour faite ✅" else "${VentreSeance.minutes} min de fractionné",
            color = Accent, fontSize = 24.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("3 à 4 fois par semaine, pendant au moins 12 semaines.", color = TextDim, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Bullet("1. Échauffement : 3 min de marche sur place")
        Bullet("2. ${VentreSeance.ROUNDS} tours, ${VentreSeance.WORK} s à fond puis ${VentreSeance.REST} s de repos pour chaque exercice :")
        Bullet("     · Montées de genoux sur place")
        Bullet("     · Jumping jacks (sauter en écartant bras et jambes)")
        Bullet("     · Squats rapides")
        Bullet("     · Burpees, ou mountain climbers si c'est trop dur")
        Bullet("3. Retour au calme : 2 min de marche lente")
        Bullet("4. Puis ton programme pompes du jour 💪")
        Spacer(Modifier.height(10.dp))
        Row {
            Text("Séances cette semaine", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$done / $per", color = Accent, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (done.toFloat() / per).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = Surface2, drawStopIndicator = {},
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("▶  Lancer la séance", Modifier.fillMaxWidth(), onClick = onStart)
        Spacer(Modifier.height(6.dp))
        Text("Le chrono te guide, sonne à chaque changement et garde l'écran allumé.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text("Les abdominaux seuls ne font pas fondre le ventre : c'est le cœur qui travaille qui brûle la graisse. " +
            "Trop dur au début ? 30 à 40 min de marche rapide marchent aussi.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SeanceTimer(onPompes: () -> Unit, onClose: () -> Unit) {
    val view = LocalView.current
    val steps = VentreSeance.STEPS
    var idx by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(steps[0].sec) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull() }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; tone?.release() }
    }

    LaunchedEffect(paused) {
        while (!paused && !finished) {
            delay(1000)
            if (left > 1) {
                left--
                if (left <= 3) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } else if (idx >= steps.lastIndex) {
                finished = true
                tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            } else {
                idx++
                left = steps[idx].sec
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            }
        }
    }

    Card {
        if (finished) {
            Text("Séance terminée 🔥", color = Accent, fontSize = 30.sp, fontFamily = Display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("${VentreSeance.minutes} min de fractionné", color = TextMain, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            if (!saved) {
                PrimaryButton("Enregistrer la séance", Modifier.fillMaxWidth()) {
                    Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_VENTRE,
                        minutes = VentreSeance.minutes, feeling = 4, note = "Séance ventre"))
                    saved = true
                }
            } else {
                Text("Enregistrée ✅", color = Accent, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("💪  Maintenant : mes pompes du jour", Modifier.fillMaxWidth(), onClick = onPompes)
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Fermer", Modifier.fillMaxWidth(), onClick = onClose)
        } else {
            val st = steps[idx]
            val color = if (st.work) Accent else TextMain
            if (st.label.isNotBlank()) {
                Text(st.label, color = TextDim, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(6.dp))
            Text(st.title, color = color, fontSize = 28.sp, fontFamily = Display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(if (left >= 60) "%d:%02d".format(left / 60, left % 60) else "$left", color = color, fontSize = 100.sp,
                fontWeight = FontWeight.Bold, lineHeight = 104.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(st.detail, color = TextDim, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (idx.toFloat() / steps.size).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = Surface2, drawStopIndicator = {},
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(if (paused) "▶  Reprendre" else "⏸  Pause", Modifier.weight(1f)) { paused = !paused }
                GhostButton("Arrêter", Modifier.weight(1f), color = TextDim, onClick = onClose)
            }
        }
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
                Text("Dernière mesure il y a $days jours : c'est le moment.", color = Accent, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Noter mes mesures", Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

@Composable
private fun Measure(label: String, now: Double?, start: Double?, unit: String, modifier: Modifier) {
    Column(modifier) {
        Text(now?.let { fmtNum(it) + " $unit" } ?: "—", color = Accent, fontSize = 22.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
        Text(label, color = TextDim, fontSize = 12.sp)
        if (now != null && start != null && now != start) {
            val diff = now - start
            Text(
                (if (diff < 0) "▼ " else "▲ ") + fmtNum(Math.abs(Math.round(diff * 10) / 10.0)) + " $unit depuis le début",
                color = if (diff < 0) Accent else Danger, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun HabitsCard() {
    Card {
        SectionLabel("Les petites habitudes")
        Spacer(Modifier.height(8.dp))
        Bullet("💧 De l'eau (fraîche ou non) à la place des jus et sodas")
        Bullet("🫗 Moins d'huile dans les sauces")
                Bullet("😴 Dormir 7 à 8 h : le manque de sommeil favorise le gras du ventre")
        Bullet("🚶 Marcher le plus possible")
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
        title = { Text("Mes mesures du jour", color = Accent, fontFamily = Display) },
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
            }) { Text("Enregistrer", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler", color = TextDim) } },
    )
}
