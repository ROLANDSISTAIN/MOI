package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Quotes
import com.roland.moi.data.Run
import com.roland.moi.data.Section
import com.roland.moi.data.Store
import com.roland.moi.run.RunState
import com.roland.moi.run.RunTracker

/** Onglet « Aujourd'hui » : l'essentiel de la journée, d'un coup d'œil. */
@Composable
fun HomeScreen(d: AppData, shieldOn: Boolean, nav: Nav, onTab: (Int) -> Unit) {
    val now = System.currentTimeMillis()
    val today = Dates.dayKey()
    val todayQuote = Quotes.today(d)
    var other by remember { mutableStateOf<Quotes.Quote?>(null) }
    val quote = other ?: todayQuote
    val goals = d.entries.filter { it.section == Section.BUTS.key && !it.done }.take(3)
    val libDays = ((now - d.streakStart) / DAY_MS).toInt()
    val run by RunTracker.state.collectAsState()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TabHeader("${greeting()}, Roland", Dates.longDate(now)) {
            IconButton(onClick = { nav.go(Route.Settings) }) { Icon(Icons.Outlined.Settings, "Réglages", tint = TextDim) }
        }

        // ---- Course en cours ----
        if (run.status != RunState.Status.IDLE) {
            ListRow(Icons.Outlined.DirectionsRun, if (run.status == RunState.Status.FINISHED) "Course terminée" else "Course en cours",
                "${RunTracker.km(run.km)} km · appuie pour revenir à la course", tint = Neon) { nav.go(Route.Run) }
        }

        // ---- Sauvegarde en retard ----
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val stale = remember(d.lastModified) { com.roland.moi.backup.DriveBackup.isStale(ctx) }
        if (stale) {
            ListRow(Icons.Outlined.Warning, "Sauvegarde en retard", "Plus de 3 jours sans sauvegarde Google Drive. Appuie pour sauvegarder.", tint = Neon) {
                nav.go(Route.Settings)
            }
        }

        // ---- Liberté ----
        HeroCard(Modifier.clickable { onTab(1) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = if (shieldOn) Success else Neon, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (shieldOn) "Bouclier actif" else "Bouclier désactivé", color = if (shieldOn) Success else Neon, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$libDays", color = TextMain, fontSize = 64.sp, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = 66.sp)
                Spacer(Modifier.width(8.dp))
                Text(if (libDays > 1) "jours de liberté" else "jour de liberté", color = AccentSoft, fontSize = 16.sp,
                    fontFamily = Display, modifier = Modifier.padding(bottom = 10.dp))
            }
            Spacer(Modifier.height(12.dp))
            NeonButton("Je suis tenté", Modifier.fillMaxWidth()) { nav.go(Route.Sos) }
        }

        // ---- Habitudes du jour ----
        Card {
            val doneCount = d.habits.count { today in it.done }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionLabel("Mes habitudes") }
                Text("$doneCount/${d.habits.size}", color = AccentSoft, fontSize = 13.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text("Tout voir", color = TextDim, fontSize = 12.sp, modifier = Modifier.clickable { nav.go(Route.Habits) }.padding(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            if (d.habits.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { doneCount.toFloat() / d.habits.size },
                    modifier = Modifier.fillMaxWidth().height(4.dp), color = Accent, trackColor = Surface2, drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
            }
            d.habits.forEach { h ->
                val done = today in h.done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { Store.toggleHabit(h.id) }.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(if (done) Accent else Surface2)
                            .border(1.5.dp, if (done) Accent else Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Text("✓", color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Text(h.name, color = if (done) TextDim else TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(h.emoji, fontSize = 16.sp)
                }
            }
        }

        // ---- Sport du jour ----
        val todays = d.runs.filter { Dates.dayKey(it.date) == today }
        val pompes = todays.filter { it.kind == Run.KIND_POMPES }.sumOf { it.count }
        val km = todays.filter { it.kind == Run.KIND_COURSE }.sumOf { it.km }
        val ventre = VentreSeance.doneToday(d)
        val pDay = PushupPlan.dayIndex(d)
        SectionLabel("Sport du jour")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(RunTracker.km(km), "km courus", Modifier.weight(1f).clickable { nav.go(Route.Run) }, if (km > 0) AccentSoft else TextMain)
            StatTile("$pompes", "pompes", Modifier.weight(1f).clickable { nav.go(Route.Pushups) }, if (pompes > 0) AccentSoft else TextMain)
            StatTile(if (ventre) "✓" else "–", "séance ventre", Modifier.weight(1f).clickable { nav.go(Route.Ventre) }, if (ventre) Success else TextMain)
        }
        ListRow(Icons.Outlined.DirectionsRun, "Courir", "Suivi GPS, sans connexion") { nav.go(Route.Run) }
        if (pDay in 1..PushupPlan.DAYS) {
            ListRow(Icons.Outlined.FitnessCenter, "Pompes · jour $pDay", PushupPlan.summary(pDay), "$pDay/30") { nav.go(Route.Pushups) }
        }
        if (!ventre) {
            ListRow(Icons.Outlined.LocalFireDepartment, "Séance ventre", "${VentreSeance.minutes} min de fractionné · ${VentreSeance.doneThisWeek(d)}/${VentreSeance.PER_WEEK} cette semaine", tint = Neon) {
                nav.go(Route.Ventre)
            }
        }

        // ---- Parole du jour ----
        Card {
            SectionLabel("Parole du jour")
            Spacer(Modifier.height(10.dp))
            Text(quote.text, color = TextMain, fontSize = 17.sp, lineHeight = 25.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("— " + quote.source, color = AccentSoft, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    "Une autre", color = TextDim, fontSize = 13.sp,
                    modifier = Modifier.clickable { other = Quotes.random(d, quote) }.padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                )
            }
        }

        // ---- Buts en cours ----
        if (goals.isNotEmpty()) {
            Card {
                SectionLabel("Mes buts en cours")
                Spacer(Modifier.height(6.dp))
                goals.forEach { g ->
                    val total = g.steps.size
                    val ok = g.steps.count { it.done }
                    Column(Modifier.fillMaxWidth().clickable { nav.go(Route.EntryView(g.id)) }.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.TrackChanges, null, tint = AccentSoft, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(g.title, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (total > 0) Text("$ok/$total", color = AccentSoft, fontSize = 13.sp, fontFamily = Display)
                        }
                        if (total > 0) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ok.toFloat() / total },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = Accent, trackColor = Surface2, drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
