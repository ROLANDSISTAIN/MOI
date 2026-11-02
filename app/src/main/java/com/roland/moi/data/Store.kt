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
            AppData(entries = starterBooks()).also { save(it) }
        }
        if (_data.value.schema < AppData.SCHEMA) {
            _data.value = migrate(_data.value)
            save(_data.value)
        }
    }

    /** Mise à jour des données d'une ancienne version de MOI. */
    private fun migrate(old: AppData): AppData {
        var d = old
        if (d.schema < 3) {
            val habits = d.habits.filterNot { it.id == "h_trading" || it.id == "h_jogging" }.toMutableList()
            if (habits.none { it.id == "h_corde" }) habits.add(minOf(2, habits.size), Habit("h_corde", "Corde à sauter", "🪢"))
            if (habits.none { it.id == "h_pompes" }) habits.add(minOf(3, habits.size), Habit("h_pompes", "Pompes", "💪"))
            val books = if (d.entries.none { it.section == Section.LIVRES.key }) starterBooks() else emptyList()
            d = d.copy(habits = habits, entries = d.entries + books)
        }
        return d.copy(schema = AppData.SCHEMA)
    }

    private fun starterBooks(): List<Entry> {
        val t = System.currentTimeMillis()
        fun b(i: Int, title: String, why: String, tag: String = "À lire") =
            Entry(id = "livre_$i", section = Section.LIVRES.key, title = title, text = why, tag = tag, created = t - i)
        return listOf(
            b(1, "Un rien peut tout changer — James Clear",
                "Construire de petites habitudes qui changent une vie. L'esprit même de MOI. À lire en premier.", "En cours"),
            b(2, "Le combat de tout homme — Stephen Arterburn & Fred Stoeker",
                "La pureté, vue par des hommes chrétiens, pour des hommes. Pour gagner le combat de l'intérieur."),
            b(3, "Trading dans la zone — Mark Douglas",
                "La psychologie du trader : discipline, émotions, penser en probabilités."),
            b(4, "Les 21 lois irréfutables du leadership — John C. Maxwell",
                "Pour grandir comme leader et guider la jeunesse."),
            b(5, "La psychologie de l'argent — Morgan Housel",
                "Le rapport à l'argent, la patience et la richesse sur le long terme."),
        )
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
        // Une séance coche automatiquement l'habitude correspondante du jour
        val day = Dates.dayKey(r.date)
        val habitId = when (r.kind) { Run.KIND_POMPES -> "h_pompes"; Run.KIND_CORDE -> "h_corde"; else -> "" }
        d.copy(
            runs = runs.sortedBy { it.date },
            habits = d.habits.map { h -> if (h.id == habitId) h.copy(done = h.done + day) else h },
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
