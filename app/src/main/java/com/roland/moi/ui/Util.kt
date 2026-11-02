package com.roland.moi.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val DAY_MS = 24L * 60 * 60 * 1000

fun formatStreak(ms: Long): String {
    val days = ms / DAY_MS
    val hours = (ms % DAY_MS) / (60 * 60 * 1000)
    return when {
        days >= 2 -> "$days jours"
        days == 1L -> "1 jour et ${hours} h"
        else -> "${hours} h ${(ms / 60000) % 60} min"
    }
}

fun formatDate(t: Long): String =
    SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRENCH).format(Date(t))

val MILESTONES = listOf(1, 3, 7, 14, 21, 30, 60, 90, 180, 365, 730, 1000)

fun milestoneName(days: Int) = when (days) {
    1 -> "Premier jour"
    3 -> "3 jours"
    7 -> "1 semaine"
    14 -> "2 semaines"
    21 -> "21 jours — habitude"
    30 -> "1 mois"
    60 -> "2 mois"
    90 -> "90 jours — reboot"
    180 -> "6 mois"
    365 -> "1 an"
    730 -> "2 ans"
    1000 -> "1000 jours"
    else -> "$days jours"
}

fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return if (h in 5..17) "Bonjour" else "Bonsoir"
}
