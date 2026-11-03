package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Run
import com.roland.moi.data.Section
import java.util.Calendar

private fun yearStart(y: Int): Long = Calendar.getInstance().apply {
    clear(); set(Calendar.YEAR, y); set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

private fun yearOf(t: Long) = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.YEAR)

private class YearStats(d: AppData, y: Int) {
    private val from = yearStart(y)
    private val to = yearStart(y + 1)
    private fun inYear(t: Long) = t in from until to
    private val firstUse = listOfNotNull(
        d.streakStart, d.relapses.minOfOrNull { it.time }, d.entries.minOfOrNull { it.created },
        d.runs.minOfOrNull { it.date }, d.days.minOfOrNull { Dates.parseDay(it.day) },
    ).filter { it > 0 }.minOrNull() ?: System.currentTimeMillis()

    private val dayKeys: List<String> = run {
        val start = maxOf(from, Dates.startOfDay(firstUse))
        val end = minOf(to, System.currentTimeMillis())
        val out = ArrayList<String>()
        var t = start
        while (t < end) { out.add(Dates.dayKey(t)); t = Dates.addDays(t, 1) }
        out
    }
    private val relapseDays = d.relapses.filter { inYear(it.time) }.map { Dates.dayKey(it.time) }.toSet()

    val trackedDays = dayKeys.size
    val libertyDays = dayKeys.count { it !in relapseDays }
    val bestLiberty: Int = run {
        var best = 0; var cur = 0
        dayKeys.forEach { if (it in relapseDays) cur = 0 else { cur++; best = maxOf(best, cur) } }
        best
    }
    val temptations = d.temptations.count { inYear(it) }

    private val sport = d.runs.filter { inYear(it.date) }
    val sessions = sport.size
    val pompes = sport.filter { it.kind == Run.KIND_POMPES }.sumOf { it.count }
    val corde = sport.filter { it.kind == Run.KIND_CORDE }.sumOf { it.minutes }
    val sportDays = sport.map { Dates.dayKey(it.date) }.toSet().size
    val bestPompes = sport.filter { it.kind == Run.KIND_POMPES }.maxOfOrNull { it.count } ?: 0

    private val yearKeys = dayKeys.toSet()
    val habitChecks = d.habits.sumOf { h -> h.done.count { it in yearKeys } }
    val topHabit = d.habits.maxByOrNull { h -> h.done.count { it in yearKeys } }?.takeIf { h -> h.done.any { it in yearKeys } }
    val topHabitCount = topHabit?.done?.count { it in yearKeys } ?: 0

    private fun entries(s: Section) = d.entries.filter { it.section == s.key }
    val booksRead = entries(Section.LIVRES).filter { it.tag == "Lu" && (inYear(it.doneDate) || (it.doneDate == 0L && inYear(it.created))) }
    val prayersAnswered = entries(Section.FOI).filter { it.done && inYear(it.doneDate) }
    val achievements = entries(Section.REALISATIONS).filter { inYear(it.date) }.sortedBy { it.date }
    val goalsReached = entries(Section.BUTS).filter { it.done && inYear(it.doneDate) }
    val decisions = entries(Section.DECISIONS).filter { inYear(it.date) }.sortedBy { it.date }
    val fearsBeaten = entries(Section.PEURS).filter { it.done && inYear(it.doneDate) }
    val moments = entries(Section.MOMENTS).filter { inYear(it.date) }

    private val logs = d.days.filter { it.day in yearKeys }
    val journalPages = logs.size
    val avgMood = logs.filter { it.mood > 0 }.map { it.mood }.average().takeIf { !it.isNaN() } ?: 0.0
    val bestMonth: String? = logs.filter { it.mood > 0 }
        .groupBy { it.day.substring(0, 7) }
        .filter { it.value.size >= 3 }
        .maxByOrNull { e -> e.value.map { it.mood }.average() }
        ?.key?.let { Dates.monthName(Dates.parseDay("$it-01")) }
}

@Composable
fun YearScreen(d: AppData, year: Int, nav: Nav) {
    val s = remember(d.lastModified, year) { YearStats(d, year) }
    val now = yearOf(System.currentTimeMillis())
    val first = listOfNotNull(d.streakStart, d.entries.minOfOrNull { it.created }, d.runs.minOfOrNull { it.date })
        .minOrNull()?.let { yearOf(it) } ?: now

    Page("Mon année", onBack = { nav.back() }) {
        if (first < now) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (now downTo first).forEach { y -> Chip("$y", y == year) { nav.replace(Route.Year(y)) } }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(22.dp)).border(1.dp, Gold, RoundedCornerShape(22.dp)).padding(24.dp),
        ) {
            Text("$year", color = Gold, fontSize = 64.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
            Text(
                if (year == now) "Ton année jusqu'ici, Roland." else "Ton année, Roland.",
                color = TextMain, fontSize = 18.sp, fontFamily = Serif,
            )
            Spacer(Modifier.height(6.dp))
            Text("${s.trackedDays} jours vécus avec MOI", color = TextDim, fontSize = 13.sp)
        }

        Big("🛡️", "${s.libertyDays}", "jours de liberté", "Plus longue série : ${s.bestLiberty} jours · ${s.temptations} tentations repoussées par le Bouclier")
        Big(
            "💪", "${s.pompes}", "pompes",
            "${s.corde} min de corde à sauter · ${s.sessions} séances sur ${s.sportDays} jours" +
                if (s.bestPompes > 0) " · record : ${s.bestPompes} en une séance" else "",
        )
        Big(
            "✅", "${s.habitChecks}", "habitudes tenues",
            s.topHabit?.let { "Ta plus fidèle : ${it.emoji} ${it.name} (${s.topHabitCount} jours)" } ?: "Coche tes habitudes chaque jour pour les voir ici",
        )
        Big("📚", "${s.booksRead.size}", if (s.booksRead.size > 1) "livres lus" else "livre lu", s.booksRead.joinToString(" · ") { it.title.substringBefore(" — ") })
        Big("🙏", "${s.prayersAnswered.size}", if (s.prayersAnswered.size > 1) "prières exaucées" else "prière exaucée", s.prayersAnswered.joinToString(" · ") { it.title })

        ListCard("🏆 Mes réalisations", s.achievements.map { "${it.title} · ${Dates.shortDate(it.date)}" })
        ListCard("🎯 Buts atteints", s.goalsReached.map { it.title })
        ListCard("⚖️ Mes décisions", s.decisions.map { "${it.title} · ${Dates.shortDate(it.date)}" })
        ListCard("🌑 Peurs vaincues", s.fearsBeaten.map { it.title })

        if (s.journalPages > 0) {
            Big(
                "📓", "${s.journalPages}", if (s.journalPages > 1) "pages de journal" else "page de journal",
                (if (s.avgMood > 0) "Humeur moyenne : ${MOODS[(s.avgMood.toInt() - 1).coerceIn(0, 4)]} " + "%.1f/5".format(s.avgMood).replace('.', ',') else "") +
                    (s.bestMonth?.let { " · Ton mois le plus heureux : $it" } ?: ""),
            )
        }
        if (s.moments.isNotEmpty()) {
            Card {
                SectionLabel("📸 ${s.moments.size} moment${if (s.moments.size > 1) "s" else ""} fort${if (s.moments.size > 1) "s" else ""}")
                s.moments.firstOrNull { it.photo.isNotBlank() }?.let { Spacer(Modifier.height(10.dp)); PhotoBox(it.photo, 180.dp) }
                Spacer(Modifier.height(8.dp))
                s.moments.forEach { Text("• ${it.title}", color = TextMain, fontSize = 14.sp) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Chaque jour compte. Continue de construire l'homme que tu deviens. 👑",
            color = Gold, fontSize = 17.sp, lineHeight = 24.sp, fontFamily = Serif, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Big(emoji: String, value: String, label: String, detail: String) {
    Card {
        Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
            Text(emoji, fontSize = 26.sp, modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.padding(start = 10.dp))
            Text(value, color = Gold, fontSize = 48.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, lineHeight = 50.sp)
            Spacer(Modifier.padding(start = 8.dp))
            Text(label, color = TextMain, fontSize = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        if (detail.isNotBlank()) Text(detail, color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ListCard(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Card {
        SectionLabel("$title · ${items.size}")
        Spacer(Modifier.height(8.dp))
        items.forEach { Text("• $it", color = TextMain, fontSize = 14.sp, lineHeight = 20.sp) }
    }
}
