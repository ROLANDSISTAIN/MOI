package com.roland.moi.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Store
import com.roland.moi.notify.Reminders
import com.roland.moi.shield.Keywords
import kotlinx.coroutines.delay
import java.util.Calendar


@Composable
fun LiberteScreen(d: AppData, shieldOn: Boolean, nav: Nav, asTab: Boolean = false) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var showRelapse by remember { mutableStateOf(false) }
    var showPledge by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    val streakMs = (now - d.streakStart).coerceAtLeast(0)
    val days = (streakMs / DAY_MS).toInt()
    val goal = d.freedomGoal.coerceAtLeast(1)
    val progress = (streakMs.toFloat() / (goal * DAY_MS).toFloat()).coerceIn(0f, 1f)
    val reached = days >= goal
    val best = maxOf(d.bestStreakMs, streakMs)
    val weekAgo = now - 7 * DAY_MS
    val weekTemptations = d.temptations.count { it > weekAgo }
    val pledged = Dates.dayKey(now) in d.pledgeDays

    Page("Liberté", onBack = if (asTab) null else ({ nav.back() })) {

        // ---- Le compteur ----
        Column(
            Modifier.fillMaxWidth()
                .background(HeroBrush, RoundedCornerShape(22.dp))
                .border(1.dp, GlowBorder, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text("Jours de liberté", color = TextMain, fontSize = 22.sp, fontFamily = Display)
            Spacer(Modifier.height(26.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Num("$days", if (days > 1) "Jours" else "Jour")
                Num("%02d".format((streakMs % DAY_MS) / 3_600_000), "Heures")
                Num("%02d".format((streakMs / 60_000) % 60), "Minutes")
                Num("%02d".format((streakMs / 1000) % 60), "Secondes")
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Accent, trackColor = Color.White.copy(alpha = 0.18f), drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showGoal = true },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Objectif : ${goalName(goal)}  ✎", color = TextMain, fontSize = 14.sp)
                Text("${(progress * 100).toInt()} %", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ---- Actions ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("↺", "Rechute", Modifier.weight(1f)) { showRelapse = true }
            ActionTile(if (pledged) "✓" else "✋", if (pledged) "Engagé" else "Engagement", Modifier.weight(1f), highlight = pledged) {
                showPledge = true
            }
            ActionTile("🛡️", "Bouclier", Modifier.weight(1f)) { nav.go(Route.ShieldSettings) }
        }

        // ---- Bouton panique ----
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                .background(Danger.copy(alpha = 0.22f))
                .border(1.dp, Danger, RoundedCornerShape(50))
                .clickable { nav.go(Route.Sos) }.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚠️  Bouton panique", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        // ---- Bouclier désactivé ----
        if (!shieldOn) {
            Card(Modifier.border(1.dp, Danger, RoundedCornerShape(18.dp))) {
                SectionLabel("Bouclier désactivé")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Active « MOI — Bouclier » dans Accessibilité pour qu'il te protège dans tous tes navigateurs, même en navigation privée.",
                    color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Activer le Bouclier", Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        // ---- Outils ----
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ListRow(
                "📖", "Mes rechutes et leçons",
                if (d.relapses.isEmpty()) "Aucune rechute enregistrée" else "${d.relapses.size} au total",
                trailing = "›",
            ) { nav.go(Route.Relapses) }
            ListRow(
                "⏰", "Rappel d'engagement",
                if (d.pledgeOn) "Chaque jour à ${Dates.time(d.pledgeTime)}" else "Désactivé",
                trailing = "›",
            ) { if (d.pledgeOn) showReminder = true else pickTime = true }
            ListRow("🌿", "Respiration", "Une minute pour laisser passer la vague", trailing = "›") { nav.go(Route.Breathe) }
            ListRow("💬", "Mes mots", "Ma lettre, mon message, mon verset", trailing = "›") { nav.go(Route.SelfTalk) }
        }

        // ---- Projection ----
        Card {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (reached) "👑" else "🕒", fontSize = 26.sp)
                Spacer(Modifier.height(8.dp))
                if (reached) {
                    Text("Objectif atteint", color = Accent, fontSize = 26.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Choisis un objectif plus grand.", color = TextDim, fontSize = 14.sp)
                } else {
                    Text("Tu atteindras ton objectif le", color = TextMain, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Dates.shortDate(Dates.addDays(d.streakStart, goal)), color = Accent,
                        fontSize = 28.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Continue. Un jour à la fois.", color = TextDim, fontSize = 14.sp)
                }
            }
        }

        // ---- Statistiques ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Record", formatStreak(best), Modifier.weight(1f))
            Stat("Victoires", "${d.resistCount}", Modifier.weight(1f))
            Stat("Tentations\n7 jours", "$weekTemptations", Modifier.weight(1f))
        }

        // ---- Paliers ----
        Card {
            SectionLabel("Paliers")
            Spacer(Modifier.height(12.dp))
            Milestones(days)
        }

        // ---- Moments faibles ----
        if (d.temptations.isNotEmpty()) {
            Card {
                SectionLabel("Tes moments faibles")
                Spacer(Modifier.height(4.dp))
                Text("Quand les tentations arrivent (${d.temptations.size} au total)", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                WeakMoments(d.temptations)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showRelapse) RelapseDialog(onDismiss = { showRelapse = false })

    if (showPledge) AlertDialog(
        onDismissRequest = { showPledge = false },
        containerColor = Surface1,
        title = { Text(if (pledged) "Engagement pris ✓" else "Mon engagement du jour", color = Accent, fontFamily = Display) },
        text = {
            Text(
                if (pledged) "Tu t'es engagé pour aujourd'hui. Tiens parole : un jour à la fois."
                else "Aujourd'hui, je choisis de rester libre. Pas pour toujours : juste aujourd'hui. Et je tiendrai parole.",
                color = TextMain, fontSize = 15.sp, lineHeight = 22.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (!pledged) Store.pledgeToday(); showPledge = false }) {
                Text(if (pledged) "OK" else "Je m'engage", color = Accent)
            }
        },
        dismissButton = { if (!pledged) TextButton(onClick = { showPledge = false }) { Text("Plus tard", color = TextDim) } },
    )

    if (showGoal) AlertDialog(
        onDismissRequest = { showGoal = false },
        containerColor = Surface1,
        title = { Text("Mon objectif", color = Accent, fontFamily = Display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GOALS.forEach { g ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (g == goal) Accent else Surface2)
                            .clickable { Store.update { it.copy(freedomGoal = g) }; showGoal = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) { Text(goalName(g), color = if (g == goal) OnAccent else TextMain, fontSize = 15.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showGoal = false }) { Text("Fermer", color = TextDim) } },
    )

    if (showReminder) AlertDialog(
        onDismissRequest = { showReminder = false },
        containerColor = Surface1,
        title = { Text("Rappel d'engagement", color = Accent, fontFamily = Display) },
        text = {
            Text(
                "Chaque jour à ${Dates.time(d.pledgeTime)}, si tu ne t'es pas encore engagé, MOI te le rappelle.",
                color = TextMain, fontSize = 15.sp, lineHeight = 22.sp,
            )
        },
        confirmButton = { TextButton(onClick = { showReminder = false; pickTime = true }) { Text("Changer l'heure", color = Accent) } },
        dismissButton = {
            TextButton(onClick = {
                Store.update { it.copy(pledgeOn = false) }; Reminders.scheduleAll(ctx); showReminder = false
            }) { Text("Désactiver", color = Danger) }
        },
    )

    if (pickTime) TimeDialog(d.pledgeTime, { pickTime = false }) { m ->
        Store.update { it.copy(pledgeTime = m, pledgeOn = true) }; Reminders.scheduleAll(ctx)
    }
}

private val GOALS = listOf(30, 90, 180, 365, 730, 1000)

private fun goalName(days: Int) = when (days) {
    30 -> "30 jours"
    90 -> "90 jours"
    180 -> "6 mois"
    365 -> "365 jours"
    730 -> "2 ans"
    else -> "$days jours"
}

@Composable
private fun Num(value: String, label: String) {
    Column {
        Text(value, color = TextMain, fontSize = 40.sp, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
        Text(label, color = TextMain.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun ActionTile(icon: String, label: String, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Surface1)
            .border(1.dp, if (highlight) Accent else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = if (highlight) Accent else TextMain, fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (highlight) Accent else TextMain, fontSize = 13.sp, maxLines = 1)
    }
}

/** Toutes les rechutes, de la plus récente à la plus ancienne, avec ce qu'elles ont appris. */
@Composable
fun RelapsesScreen(d: AppData, nav: Nav) {
    Page("Mes rechutes et leçons", onBack = { nav.back() }) {
        if (d.relapses.isEmpty()) {
            EmptyState("Aucune rechute enregistrée. Continue comme ça.")
        } else {
            d.relapses.reversed().forEach { r ->
                Card {
                    Text(formatDate(r.time) + " · après " + formatStreak(r.streakMs), color = TextDim, fontSize = 12.sp)
                    if (r.trigger.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Déclencheur : ${r.trigger}", color = Accent, fontSize = 13.sp)
                    }
                    if (r.what.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(r.what, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(Surface1, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Text(value, color = Accent, fontSize = 20.sp, fontFamily = Display, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Milestones(days: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MILESTONES.forEach { m ->
            val done = days >= m
            Box(
                Modifier
                    .background(if (done) Accent else Surface2, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text((if (done) "✓ " else "") + milestoneName(m), color = if (done) OnAccent else TextDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WeakMoments(times: List<Long>) {
    val buckets = IntArray(4)
    val cal = Calendar.getInstance()
    times.forEach {
        cal.timeInMillis = it
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val i = when (h) { in 5..11 -> 0; in 12..17 -> 1; in 18..22 -> 2; else -> 3 }
        buckets[i]++
    }
    val labels = listOf("Matin", "Après-midi", "Soir", "Nuit")
    val max = (buckets.maxOrNull() ?: 1).coerceAtLeast(1)
    labels.forEachIndexed { i, l ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l, color = TextMain, fontSize = 13.sp, modifier = Modifier.width(84.dp))
            Box(Modifier.weight(1f).height(10.dp).background(Surface2, RoundedCornerShape(5.dp))) {
                Box(
                    Modifier.fillMaxWidth(buckets[i].toFloat() / max).fillMaxHeight()
                        .background(Accent, RoundedCornerShape(5.dp))
                )
            }
            Text("${buckets[i]}", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(36.dp).padding(start = 8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelapseDialog(onDismiss: () -> Unit) {
    var what by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    val triggers = listOf("Ennui", "Solitude", "Stress", "Fatigue / tard le soir", "Perte en trading", "Réseaux sociaux", "Colère", "Tristesse", "Autre")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Ce n'est pas la fin.", color = Accent, fontFamily = Display) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Une chute n'efface pas le chemin parcouru. Apprends d'elle : qu'est-ce qui s'est passé ?",
                    color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text("Le déclencheur", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    triggers.forEach { t ->
                        FilterChip(
                            selected = trigger == t,
                            onClick = { trigger = if (trigger == t) "" else t },
                            label = { Text(t, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent, selectedLabelColor = OnAccent,
                                labelColor = TextMain,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = what, onValueChange = { what = it },
                    label = { Text("Ce qui s'est passé, ce que je ferai autrement") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = site, onValueChange = { site = it }, singleLine = true,
                    label = { Text("Par quel site ou quel mot ? (facultatif)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Écris juste son nom : le Bouclier le bloquera dès maintenant, pour que cette porte reste fermée.",
                    color = TextDim, fontSize = 12.sp, lineHeight = 16.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Store.recordRelapse(what.trim(), trigger)
                val w = Keywords.cleanExtra(site)
                if (Keywords.compact(w).length >= 4) Store.update { cur ->
                    if (cur.shieldExtraWords.any { Keywords.compact(it) == Keywords.compact(w) }) cur
                    else cur.copy(shieldExtraWords = cur.shieldExtraWords + w)
                }
                onDismiss()
            }) {
                Text("Recommencer à zéro", color = Accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextDim) } },
    )
}
