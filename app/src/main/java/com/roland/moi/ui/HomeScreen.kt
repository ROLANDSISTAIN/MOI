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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.roland.moi.data.Section
import com.roland.moi.data.Store
import java.util.Calendar

@Composable
fun HomeScreen(d: AppData, shieldOn: Boolean, nav: Nav, onTab: (Int) -> Unit) {
    val now = System.currentTimeMillis()
    val today = Dates.dayKey()
    val quote = Quotes.today(d)
    val log = d.days.firstOrNull { it.day == today }
    val capsules = d.entries.filter { it.section == Section.CAPSULES.key && it.dueDate in 1..now && !it.done }
    val dow = Dates.dayOfWeek()
    val needsReview = (dow == Calendar.SUNDAY || dow == Calendar.MONDAY) &&
        d.reviews.none { it.week == Dates.weekKey(if (dow == Calendar.MONDAY) Dates.addDays(now, -1) else now) }
    val goals = d.entries.filter { it.section == Section.BUTS.key && !it.done }.take(3)
    val runToday = d.runs.any { Dates.dayKey(it.date) == today }
    val runDays = d.runs.map { Dates.dayKey(it.date) }.toSet()
    val runStreak = Dates.streak(runDays)
    val libDays = ((now - d.streakStart) / DAY_MS).toInt()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Dates.longDate(now), color = TextDim, fontSize = 13.sp)
                Text("${greeting()}, Roland", color = TextMain, fontSize = 26.sp, fontFamily = Serif)
            }
            IconButton(onClick = { nav.go(Route.Settings) }) { Icon(Icons.Outlined.Settings, "Réglages", tint = Gold) }
        }

        // ---- Parole du jour ----
        Card {
            SectionLabel("Ta parole du jour")
            Spacer(Modifier.height(10.dp))
            Text(quote.text, color = TextMain, fontSize = 18.sp, lineHeight = 26.sp, fontFamily = Serif, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(8.dp))
            Text("— " + quote.source, color = Gold, fontSize = 13.sp)
        }

        // ---- Capsules à ouvrir ----
        capsules.forEach { c ->
            Card(Modifier.border(1.dp, Gold, RoundedCornerShape(18.dp)).clickable { nav.go(Route.EntryView(c.id)) }) {
                SectionLabel("⏳ Une lettre du passé t'attend")
                Spacer(Modifier.height(6.dp))
                Text("« ${c.title} », écrite le ${Dates.shortDate(c.created)}. Appuie pour l'ouvrir.", color = TextMain, fontSize = 14.sp)
            }
        }

        // ---- Bilan de la semaine ----
        if (needsReview) {
            Card(Modifier.clickable {
                nav.go(Route.Review(Dates.weekKey(if (dow == Calendar.MONDAY) Dates.addDays(now, -1) else now)))
            }) {
                SectionLabel("Bilan de la semaine")
                Spacer(Modifier.height(6.dp))
                Text("3 minutes pour faire le point : tes victoires, ce que tu veux améliorer.", color = TextMain, fontSize = 14.sp)
            }
        }

        // ---- Journal du jour ----
        Card(Modifier.clickable { nav.go(Route.Journal(today)) }) {
            SectionLabel("Mon journal du jour")
            Spacer(Modifier.height(8.dp))
            if (log == null) {
                Text("Comment s'est passée ta journée ? Humeur, gratitude, victoire du jour.", color = TextDim, fontSize = 14.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (log.mood > 0) Text(MOODS[log.mood - 1], fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        if (log.victory.isNotBlank()) Text("🏅 " + log.victory, color = TextMain, fontSize = 14.sp, maxLines = 2)
                        if (log.gratitude.isNotBlank()) Text("🙏 " + log.gratitude, color = TextDim, fontSize = 13.sp, maxLines = 2)
                    }
                }
            }
        }

        // ---- Habitudes du jour ----
        Card {
            val doneCount = d.habits.count { today in it.done }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionLabel("Mes habitudes du jour") }
                Text("$doneCount/${d.habits.size}", color = Gold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            d.habits.forEach { h ->
                val done = today in h.done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { Store.toggleHabit(h.id) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (done) Gold else Surface2)
                            .border(1.dp, if (done) Gold else Line, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (done) Text("✓", color = Black, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Text("${h.emoji}  ${h.name}", color = if (done) TextDim else TextMain, fontSize = 15.sp)
                }
            }
        }

        // ---- Sport ----
        Card(Modifier.clickable { onTab(3) }) {
            SectionLabel("Sport du matin")
            Spacer(Modifier.height(8.dp))
            val todays = d.runs.filter { Dates.dayKey(it.date) == today }
            val p = todays.filter { it.kind == com.roland.moi.data.Run.KIND_POMPES }.sumOf { it.count }
            val c = todays.filter { it.kind == com.roland.moi.data.Run.KIND_CORDE }.sumOf { it.minutes }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪢", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (runToday) "Aujourd'hui : $c min de corde · $p pompes" else "Pas encore de sport aujourd'hui",
                        color = if (runToday) Gold else TextMain, fontSize = 15.sp,
                    )
                    Text("$runStreak jour${if (runStreak > 1) "s" else ""} d'affilée 🔥", color = TextDim, fontSize = 13.sp)
                }
            }
        }

        // ---- Buts en cours ----
        if (goals.isNotEmpty()) {
            Card {
                SectionLabel("Mes buts en cours")
                Spacer(Modifier.height(8.dp))
                goals.forEach { g ->
                    val total = g.steps.size
                    val ok = g.steps.count { it.done }
                    Column(Modifier.fillMaxWidth().clickable { nav.go(Route.EntryView(g.id)) }.padding(vertical = 8.dp)) {
                        Row {
                            Text("🎯 " + g.title, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (total > 0) Text("$ok/$total", color = Gold, fontSize = 13.sp)
                        }
                        if (total > 0) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ok.toFloat() / total },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = Gold, trackColor = Surface2, drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
        }

        // ---- Liberté (discret) ----
        ListRow(
            emoji = if (shieldOn) "🛡️" else "⚠️",
            title = "Liberté",
            subtitle = if (shieldOn) "Bouclier actif" else "Bouclier désactivé",
            trailing = "$libDays j",
        ) { nav.go(Route.Liberte) }
    }
}
