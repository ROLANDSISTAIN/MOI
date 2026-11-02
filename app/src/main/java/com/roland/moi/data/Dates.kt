package com.roland.moi.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Dates {
    private fun fmt(p: String) = SimpleDateFormat(p, Locale.FRENCH)

    fun dayKey(t: Long = System.currentTimeMillis()): String = fmt("yyyy-MM-dd").format(Date(t))

    fun parseDay(key: String): Long = runCatching { fmt("yyyy-MM-dd").parse(key)!!.time }.getOrDefault(0L)

    /** Semaine ISO, ex "2026-W45" (les semaines commencent le lundi). */
    fun weekKey(t: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance(Locale.FRANCE).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = t
        }
        return "%d-W%02d".format(c.getWeekYear(), c.get(Calendar.WEEK_OF_YEAR))
    }

    fun monthKey(t: Long = System.currentTimeMillis()): String = fmt("yyyy-MM").format(Date(t))

    fun startOfDay(t: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(t: Long, n: Int): Long = Calendar.getInstance().apply { timeInMillis = t; add(Calendar.DAY_OF_YEAR, n) }.timeInMillis

    /** Lundi 00:00 de la semaine qui contient t. */
    fun startOfWeek(t: Long = System.currentTimeMillis()): Long = Calendar.getInstance(Locale.FRANCE).apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = startOfDay(t)
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        if (timeInMillis > t) add(Calendar.WEEK_OF_YEAR, -1)
    }.timeInMillis

    fun startOfMonth(t: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
        timeInMillis = startOfDay(t); set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun dayOfWeek(t: Long = System.currentTimeMillis()): Int = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.DAY_OF_WEEK)

    fun longDate(t: Long): String = fmt("EEEE d MMMM yyyy").format(Date(t)).replaceFirstChar { it.uppercase() }
    fun shortDate(t: Long): String = fmt("d MMM yyyy").format(Date(t))
    fun monthName(t: Long): String = fmt("MMMM yyyy").format(Date(t)).replaceFirstChar { it.uppercase() }
    fun dayLetter(t: Long): String = fmt("EEEEE").format(Date(t)).uppercase()
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    /** Nombre de jours consécutifs (jusqu'à aujourd'hui, ou hier si pas encore fait aujourd'hui). */
    fun streak(doneDays: Set<String>): Int {
        var t = System.currentTimeMillis()
        if (dayKey(t) !in doneDays) t = addDays(t, -1)
        var n = 0
        while (dayKey(t) in doneDays) { n++; t = addDays(t, -1) }
        return n
    }

    fun bestStreak(doneDays: Set<String>): Int {
        val sorted = doneDays.map { parseDay(it) }.filter { it > 0 }.sorted()
        var best = 0; var cur = 0; var prev = 0L
        for (d in sorted) {
            cur = if (prev != 0L && dayKey(addDays(prev, 1)) == dayKey(d)) cur + 1 else 1
            best = maxOf(best, cur); prev = d
        }
        return best
    }
}
