package com.roland.moi.ui

import androidx.compose.runtime.mutableStateListOf

sealed class Route {
    data object Liberte : Route()
    data object ShieldSettings : Route()
    data object Settings : Route()
    data object Protection : Route()
    data object HabitsEdit : Route()
    data class Section(val key: String) : Route()
    data class EntryView(val id: String) : Route()
    data class EntryEdit(val section: String, val id: String? = null) : Route()
    data class Journal(val day: String) : Route()
    data object JournalList : Route()
    data class Review(val week: String) : Route()
    data object Reviews : Route()
    data class Month(val start: Long) : Route()
    data object Sos : Route()
    data object Pushups : Route()
    data class Year(val year: Int) : Route()
}

/** Petite pile de navigation : les onglets sont la base, les écrans s'empilent par-dessus. */
class Nav {
    val stack = mutableStateListOf<Route>()
    fun go(r: Route) { stack.add(r) }
    fun back(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.lastIndex); true } else false
    fun replace(r: Route) { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex); stack.add(r) }
    val current: Route? get() = stack.lastOrNull()
}
