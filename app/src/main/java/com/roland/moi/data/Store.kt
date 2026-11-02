package com.roland.moi.data

import android.content.Context
import com.roland.moi.backup.BackupWorker
import com.roland.moi.notify.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Toutes les données de MOI, dans un seul fichier JSON privé (facile à sauvegarder). */
object Store {
    private lateinit var file: File
    private lateinit var appContext: Context
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> get() = _data

    fun init(context: Context) {
        appContext = context.applicationContext
        file = File(appContext.filesDir, "moi_data.json")
        _data.value = if (file.exists()) {
            runCatching { AppData.fromJson(JSONObject(file.readText())) }.getOrElse { AppData() }
        } else {
            AppData().also { save(it) }
        }
    }

    fun newId(): String = UUID.randomUUID().toString().take(12)

    @Synchronized
    fun update(block: (AppData) -> AppData) {
        val next = block(_data.value).copy(lastModified = System.currentTimeMillis())
        _data.value = next
        save(next)
        BackupWorker.scheduleSoon(appContext)
    }

    /** Remplace tout (restauration depuis Google Drive). */
    @Synchronized
    fun replaceAll(json: String) {
        val restored = AppData.fromJson(JSONObject(json))
        _data.value = restored
        save(restored)
        Reminders.scheduleAll(appContext)
    }

    fun exportJson(): String = _data.value.toJson().toString()

    private fun save(d: AppData) {
        val tmp = File(file.parentFile, "moi_data.tmp")
        tmp.writeText(d.toJson().toString())
        tmp.renameTo(file)
    }

    // ---------- Liberté ----------

    fun currentStreakMs(d: AppData = _data.value) = System.currentTimeMillis() - d.streakStart

    fun recordTemptation() = update { it.copy(temptations = (it.temptations + System.currentTimeMillis()).takeLast(2000)) }

    fun recordResisted() = update { it.copy(resistCount = it.resistCount + 1) }

    fun recordRelapse(what: String, trigger: String) = update {
        val streak = currentStreakMs(it)
        it.copy(
            relapses = it.relapses + Relapse(System.currentTimeMillis(), streak, what, trigger),
            bestStreakMs = maxOf(it.bestStreakMs, streak),
            streakStart = System.currentTimeMillis(),
        )
    }

    // ---------- Ma vie ----------

    fun saveEntry(e: Entry) = update { d ->
        val exists = d.entries.any { it.id == e.id }
        d.copy(entries = if (exists) d.entries.map { if (it.id == e.id) e else it } else d.entries + e)
    }

    fun deleteEntry(id: String) = update { d -> d.copy(entries = d.entries.filterNot { it.id == id }) }

    fun entry(id: String): Entry? = _data.value.entries.firstOrNull { it.id == id }

    // ---------- Habitudes ----------

    fun toggleHabit(id: String, day: String = Dates.dayKey()) = update { d ->
        d.copy(habits = d.habits.map { h ->
            if (h.id != id) h else h.copy(done = if (day in h.done) h.done - day else h.done + day)
        })
    }

    fun saveHabit(h: Habit) = update { d ->
        val exists = d.habits.any { it.id == h.id }
        d.copy(habits = if (exists) d.habits.map { if (it.id == h.id) h else it } else d.habits + h)
    }

    fun deleteHabit(id: String) = update { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }

    // ---------- Sport ----------

    fun saveRun(r: Run) = update { d ->
        val exists = d.runs.any { it.id == r.id }
        val runs = if (exists) d.runs.map { if (it.id == r.id) r else it } else d.runs + r
        // Un jogging coche automatiquement l'habitude « Jogging » du jour
        val day = Dates.dayKey(r.date)
        d.copy(
            runs = runs.sortedBy { it.date },
            habits = d.habits.map { h -> if (h.id == "h_jogging") h.copy(done = h.done + day) else h },
        )
    }

    fun deleteRun(id: String) = update { d -> d.copy(runs = d.runs.filterNot { it.id == id }) }

    // ---------- Journal & bilans ----------

    fun saveDay(log: DayLog) = update { d ->
        d.copy(days = (d.days.filterNot { it.day == log.day } + log).sortedBy { it.day })
    }

    fun saveReview(w: WeekReview) = update { d ->
        d.copy(reviews = (d.reviews.filterNot { it.week == w.week } + w).sortedBy { it.week })
    }
}
