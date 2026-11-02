package com.roland.moi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.DayLog
import com.roland.moi.data.Store
import com.roland.moi.data.WeekReview

// ======================= Journal du jour =======================

@Composable
fun JournalScreen(d: AppData, day: String, nav: Nav) {
    val old = remember(day) { d.days.firstOrNull { it.day == day } }
    var mood by remember { mutableIntStateOf(old?.mood ?: 0) }
    var gratitude by remember { mutableStateOf(old?.gratitude ?: "") }
    var victory by remember { mutableStateOf(old?.victory ?: "") }
    var text by remember { mutableStateOf(old?.text ?: "") }
    val t = Dates.parseDay(day)

    Page("Mon journal", onBack = { nav.back() }) {
        Text(Dates.longDate(t), color = TextDim, fontSize = 14.sp)
        SectionLabel("Mon humeur")
        MoodPicker(mood) { mood = it }
        Field(gratitude, { gratitude = it }, "Aujourd'hui, je suis reconnaissant pour…", minLines = 2)
        Field(victory, { victory = it }, "Ma victoire du jour", minLines = 2)
        Field(text, { text = it }, "Ce que j'ai vécu, ce que je ressens", minLines = 6)
        GoldButton("Enregistrer", Modifier.fillMaxWidth()) {
            Store.saveDay(DayLog(day, mood, gratitude.trim(), victory.trim(), text.trim()))
            nav.back()
        }
    }
}

@Composable
fun JournalListScreen(d: AppData, nav: Nav) {
    val today = Dates.dayKey()
    Page("Mon journal", onBack = { nav.back() }) {
        GoldButton(if (d.days.any { it.day == today }) "Modifier la page d'aujourd'hui" else "+ Écrire la page d'aujourd'hui", Modifier.fillMaxWidth()) {
            nav.go(Route.Journal(today))
        }
        if (d.days.isEmpty()) EmptyState("Chaque soir, quelques lignes : ton humeur, une gratitude, ta victoire du jour.")
        d.days.sortedByDescending { it.day }.forEach { log ->
            ListRow(
                emoji = if (log.mood > 0) MOODS[log.mood - 1] else "📓",
                title = Dates.longDate(Dates.parseDay(log.day)),
                subtitle = listOf(log.victory, log.gratitude, log.text).firstOrNull { it.isNotBlank() } ?: "",
            ) { nav.go(Route.Journal(log.day)) }
        }
    }
}

// ======================= Bilans =======================

private data class Stats(
    val libertyDays: Int, val temptations: Int, val resisted: Int,
    val runs: Int, val km: Double, val habitPct: Int, val avgMood: Double, val journalDays: Int,
    val achievements: List<String>,
)

private fun stats(d: AppData, from: Long, to: Long): Stats {
    val nDays = ((to - from) / DAY_MS).toInt().coerceAtLeast(1)
    val dayKeys = (0 until nDays).map { Dates.dayKey(Dates.addDays(from, it)) }.filter { Dates.parseDay(it) <= System.currentTimeMillis() }
    val habitTotal = d.habits.size * dayKeys.size
    val habitDone = d.habits.sumOf { h -> dayKeys.count { it in h.done } }
    val logs = d.days.filter { it.day in dayKeys && it.mood > 0 }
    val relapseDays = d.relapses.map { Dates.dayKey(it.time) }.toSet()
    val firstDay = Dates.startOfDay(minOf(d.streakStart, d.relapses.minOfOrNull { it.time } ?: d.streakStart))
    return Stats(
        libertyDays = dayKeys.count { it !in relapseDays && Dates.parseDay(it) >= firstDay },
        temptations = d.temptations.count { it in from until to },
        resisted = 0,
        runs = d.runs.count { it.date in from until to },
        km = d.runs.filter { it.date in from until to }.sumOf { it.km },
        habitPct = if (habitTotal == 0) 0 else habitDone * 100 / habitTotal,
        avgMood = if (logs.isEmpty()) 0.0 else logs.map { it.mood }.average(),
        journalDays = d.days.count { it.day in dayKeys },
        achievements = d.entries.filter {
            (it.section == "realisations" && it.date in from until to) ||
                (it.done && it.doneDate in from until to && it.section in listOf("buts", "foi", "peurs", "desirs"))
        }.map { it.title },
    )
}

@Composable
private fun StatsCard(s: Stats) {
    Card {
        SectionLabel("En chiffres")
        Spacer(Modifier.height(8.dp))
        StatLine("🛡️", "Liberté", "${s.libertyDays} jours")
        StatLine("⚔️", "Tentations repoussées par le Bouclier", "${s.temptations}")
        StatLine("🏃", "Joggings", "${s.runs} · ${"%.1f".format(s.km).replace('.', ',')} km")
        StatLine("✅", "Habitudes tenues", "${s.habitPct} %")
        StatLine("📓", "Pages de journal", "${s.journalDays}")
        if (s.avgMood > 0) StatLine(MOODS[(s.avgMood.toInt() - 1).coerceIn(0, 4)], "Humeur moyenne", "%.1f / 5".format(s.avgMood).replace('.', ','))
        if (s.achievements.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Accompli")
            s.achievements.forEach { Text("🏆 $it", color = TextMain, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun StatLine(emoji: String, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Gold, fontSize = 14.sp)
    }
}

private fun weekStartOf(week: String): Long {
    // On retrouve le lundi de la semaine en parcourant les jours proches d'aujourd'hui
    var t = Dates.startOfWeek()
    repeat(260) { if (Dates.weekKey(t) == week) return t; t = Dates.addDays(t, -7) }
    return Dates.startOfWeek()
}

@Composable
fun ReviewScreen(d: AppData, week: String, nav: Nav) {
    val old = remember(week) { d.reviews.firstOrNull { it.week == week } }
    var mood by remember { mutableIntStateOf(old?.mood ?: 0) }
    var wins by remember { mutableStateOf(old?.wins ?: "") }
    var improve by remember { mutableStateOf(old?.improve ?: "") }
    val start = remember(week) { weekStartOf(week) }
    val end = Dates.addDays(start, 7)

    Page("Bilan de la semaine", onBack = { nav.back() }) {
        Text("Du ${Dates.shortDate(start)} au ${Dates.shortDate(Dates.addDays(end, -1))}", color = TextDim, fontSize = 14.sp)
        StatsCard(stats(d, start, end))
        SectionLabel("Comment s'est passée ta semaine ?")
        MoodPicker(mood) { mood = it }
        Field(wins, { wins = it }, "Mes victoires de la semaine", minLines = 3)
        Field(improve, { improve = it }, "Ce que je veux améliorer la semaine prochaine", minLines = 3)
        GoldButton("Enregistrer mon bilan", Modifier.fillMaxWidth()) {
            Store.saveReview(WeekReview(week, mood, wins.trim(), improve.trim(), old?.created ?: System.currentTimeMillis()))
            nav.back()
        }
    }
}

@Composable
fun ReviewsScreen(d: AppData, nav: Nav) {
    val now = System.currentTimeMillis()
    Page("Mes bilans", onBack = { nav.back() }) {
        GoldButton("Faire le bilan de cette semaine", Modifier.fillMaxWidth()) { nav.go(Route.Review(Dates.weekKey(now))) }

        SectionLabel("Bilans mensuels (automatiques)")
        val firstUse = listOfNotNull(
            d.days.minOfOrNull { Dates.parseDay(it.day) }, d.runs.minOfOrNull { it.date },
            d.entries.minOfOrNull { it.created }, d.streakStart,
        ).minOrNull() ?: now
        var m = Dates.startOfMonth(now)
        val months = mutableListOf<Long>()
        while (m >= Dates.startOfMonth(firstUse) && months.size < 24) {
            months.add(m)
            m = Dates.startOfMonth(Dates.addDays(m, -1))
        }
        months.forEach { ms -> ListRow("🗓️", Dates.monthName(ms), if (ms == Dates.startOfMonth(now)) "En cours" else "") { nav.go(Route.Month(ms)) } }

        SectionLabel("Bilans de la semaine")
        if (d.reviews.isEmpty()) EmptyState("Chaque dimanche, MOI t'invite à faire le point sur ta semaine.")
        d.reviews.sortedByDescending { it.week }.forEach { w ->
            ListRow(
                emoji = if (w.mood > 0) MOODS[w.mood - 1] else "📊",
                title = "Semaine du " + Dates.shortDate(weekStartOf(w.week)),
                subtitle = w.wins,
            ) { nav.go(Route.Review(w.week)) }
        }
    }
}

@Composable
fun MonthScreen(d: AppData, start: Long, nav: Nav) {
    val end = Dates.startOfMonth(Dates.addDays(start, 32))
    val s = stats(d, start, end)
    val reviews = d.reviews.filter { val t = weekStartOf(it.week); t in start until end }
    Page(Dates.monthName(start), onBack = { nav.back() }) {
        StatsCard(s)
        if (reviews.isNotEmpty()) {
            Card {
                SectionLabel("Ce que j'ai écrit dans mes bilans")
                Spacer(Modifier.height(8.dp))
                reviews.forEach { w ->
                    if (w.wins.isNotBlank()) Text("🏅 " + w.wins, color = TextMain, fontSize = 14.sp)
                    if (w.improve.isNotBlank()) Text("↗ " + w.improve, color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        val best = d.days.filter { Dates.parseDay(it.day) in start until end && it.victory.isNotBlank() }
        if (best.isNotEmpty()) {
            Card {
                SectionLabel("Mes victoires du mois")
                Spacer(Modifier.height(8.dp))
                best.forEach { Text("• " + it.victory, color = TextMain, fontSize = 14.sp) }
            }
        }
    }
}
