package com.roland.moi.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Store
import kotlinx.coroutines.delay
import java.util.Calendar


@Composable
fun LiberteScreen(d: AppData, shieldOn: Boolean, nav: Nav) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var showRelapse by remember { mutableStateOf(false) }

    val streakMs = (now - d.streakStart).coerceAtLeast(0)
    val days = (streakMs / DAY_MS).toInt()
    val next = MILESTONES.firstOrNull { it > days } ?: (days + 1)
    val prev = MILESTONES.lastOrNull { it <= days } ?: 0
    val progress = ((streakMs - prev * DAY_MS).toFloat() / ((next - prev) * DAY_MS)).coerceIn(0f, 1f)
    val best = maxOf(d.bestStreakMs, streakMs)
    val weekAgo = now - 7 * DAY_MS
    val weekTemptations = d.temptations.count { it > weekAgo }

    Page("Liberté", onBack = { nav.back() }, actions = {
        androidx.compose.material3.TextButton(onClick = { nav.go(Route.ShieldSettings) }) {
            Text("Bouclier", color = Gold)
        }
    }) {

        // ---- Le compteur ----
        Card {
            SectionLabel("Jours de liberté")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$days", color = Gold, fontSize = 88.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, lineHeight = 88.sp)
                Spacer(Modifier.width(10.dp))
                Text(if (days > 1) "jours" else "jour", color = TextMain, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            val h = (streakMs % DAY_MS) / 3_600_000
            val m = (streakMs / 60_000) % 60
            val s = (streakMs / 1000) % 60
            Text("%02d h  %02d min  %02d s".format(h, m, s), color = TextDim, fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Prochain palier : ${milestoneName(next)}", color = TextMain, fontSize = 13.sp)
                Text("${(progress * 100).toInt()} %", color = Gold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Gold, trackColor = Surface2, drawStopIndicator = {},
            )
        }

        // ---- Statistiques ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Record", formatStreak(best), Modifier.weight(1f))
            Stat("Victoires", "${d.resistCount}", Modifier.weight(1f))
            Stat("Tentations\n7 jours", "$weekTemptations", Modifier.weight(1f))
        }

        // ---- Bouclier ----
        if (!shieldOn) {
            Card(Modifier.border(1.dp, Danger, RoundedCornerShape(18.dp))) {
                SectionLabel("Bouclier désactivé")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Active « MOI — Bouclier » dans Accessibilité pour qu'il te protège dans tous tes navigateurs, même en navigation privée.",
                    color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                GoldButton("Activer le Bouclier", Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } else {
            Card {
                SectionLabel("🛡️ Bouclier actif")
                Spacer(Modifier.height(6.dp))
                Text("Tous tes navigateurs sont surveillés, y compris en navigation privée.", color = TextDim, fontSize = 13.sp)
            }
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

        // ---- Rechutes / leçons ----
        if (d.relapses.isNotEmpty()) {
            Card {
                SectionLabel("Ce que j'ai appris")
                Spacer(Modifier.height(10.dp))
                d.relapses.takeLast(5).reversed().forEach { r ->
                    Text(formatDate(r.time) + " · après " + formatStreak(r.streakMs), color = TextDim, fontSize = 12.sp)
                    if (r.trigger.isNotBlank()) Text("Déclencheur : ${r.trigger}", color = Gold, fontSize = 13.sp)
                    if (r.what.isNotBlank()) Text(r.what, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        GhostButton("J'ai rechuté", Modifier.fillMaxWidth(), color = Danger) { showRelapse = true }
        Spacer(Modifier.height(8.dp))
    }

    if (showRelapse) RelapseDialog(onDismiss = { showRelapse = false })
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(Surface1, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Text(value, color = Gold, fontSize = 20.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    .background(if (done) Gold else Surface2, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text((if (done) "✓ " else "") + milestoneName(m), color = if (done) Black else TextDim, fontSize = 12.sp)
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
                        .background(Gold, RoundedCornerShape(5.dp))
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
    val triggers = listOf("Ennui", "Solitude", "Stress", "Fatigue / tard le soir", "Perte en trading", "Réseaux sociaux", "Colère", "Tristesse", "Autre")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Ce n'est pas la fin.", color = Gold, fontFamily = Serif) },
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
                                selectedContainerColor = Gold, selectedLabelColor = Black,
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
            }
        },
        confirmButton = {
            TextButton(onClick = { Store.recordRelapse(what.trim(), trigger); onDismiss() }) {
                Text("Recommencer à zéro", color = Gold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextDim) } },
    )
}
