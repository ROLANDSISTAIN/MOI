package com.roland.moi.data

import android.content.Context
import com.roland.moi.backup.BackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File

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

    /** true si l'utilisateur n'a encore rien fait (utile pour proposer la restauration). */
    fun isFresh(): Boolean {
        val d = _data.value
        return d.relapses.isEmpty() && d.temptations.isEmpty() && d.resistCount == 0
    }

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
    }

    fun exportJson(): String = _data.value.toJson().toString(2)

    private fun save(d: AppData) {
        val tmp = File(file.parentFile, "moi_data.tmp")
        tmp.writeText(d.toJson().toString())
        tmp.renameTo(file)
    }

    // ---------- Actions ----------

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

    fun bestStreak(d: AppData = _data.value) = maxOf(d.bestStreakMs, currentStreakMs(d))
}
