package com.roland.moi.data

import org.json.JSONArray
import org.json.JSONObject

data class Relapse(
    val time: Long,
    val streakMs: Long,
    val what: String,
    val trigger: String,
)

data class Step(val text: String, val done: Boolean = false)

/** Une entrée générique de « Ma vie » (valeur, peur, but, souvenir, verset…). */
data class Entry(
    val id: String,
    val section: String,
    val title: String = "",
    val text: String = "",
    val tag: String = "",
    val date: Long = 0L,
    val dueDate: Long = 0L,
    val done: Boolean = false,
    val doneDate: Long = 0L,
    val photo: String = "",
    val steps: List<Step> = emptyList(),
    val created: Long = System.currentTimeMillis(),
)

data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val done: Set<String> = emptySet(), // dates "yyyy-MM-dd"
    val created: Long = System.currentTimeMillis(),
)

data class Run(
    val id: String,
    val date: Long,
    val km: Double,
    val minutes: Int,
    val feeling: Int,
    val note: String = "",
)

data class DayLog(
    val day: String, // "yyyy-MM-dd"
    val mood: Int = 0,
    val gratitude: String = "",
    val victory: String = "",
    val text: String = "",
)

data class WeekReview(
    val week: String, // "yyyy-Www"
    val mood: Int = 0,
    val wins: String = "",
    val improve: String = "",
    val created: Long = System.currentTimeMillis(),
)

data class AppData(
    // ---- Liberté ----
    val streakStart: Long = System.currentTimeMillis(),
    val bestStreakMs: Long = 0L,
    val resistCount: Int = 0,
    val temptations: List<Long> = emptyList(),
    val relapses: List<Relapse> = emptyList(),
    val shieldMessage: String = "Tu vaux plus que quelques minutes de plaisir. Rappelle-toi l'homme que tu veux devenir.",
    val shieldVerse: String = "« Fuyez la débauche. » — 1 Corinthiens 6:18",
    val shieldDelaySec: Int = 30,
    val blockVpnBrowsers: Boolean = false,
    // ---- Ma vie ----
    val entries: List<Entry> = emptyList(),
    val habits: List<Habit> = defaultHabits(),
    val runs: List<Run> = emptyList(),
    val days: List<DayLog> = emptyList(),
    val reviews: List<WeekReview> = emptyList(),
    // ---- Réglages ----
    val quoteOn: Boolean = true,
    val quoteTime: Int = 7 * 60,        // minutes depuis minuit
    val sportOn: Boolean = true,
    val sportTime: Int = 5 * 60 + 30,
    val protectOn: Boolean = false,
    val unlockRequestedAt: Long = 0L,
    val unlockUntil: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 2)
        put("streakStart", streakStart)
        put("bestStreakMs", bestStreakMs)
        put("resistCount", resistCount)
        put("temptations", JSONArray(temptations))
        put("relapses", arr(relapses) {
            JSONObject().put("time", it.time).put("streakMs", it.streakMs).put("what", it.what).put("trigger", it.trigger)
        })
        put("shieldMessage", shieldMessage)
        put("shieldVerse", shieldVerse)
        put("shieldDelaySec", shieldDelaySec)
        put("blockVpnBrowsers", blockVpnBrowsers)
        put("entries", arr(entries) { e ->
            JSONObject().put("id", e.id).put("section", e.section).put("title", e.title).put("text", e.text)
                .put("tag", e.tag).put("date", e.date).put("dueDate", e.dueDate).put("done", e.done)
                .put("doneDate", e.doneDate).put("photo", e.photo).put("created", e.created)
                .put("steps", arr(e.steps) { s -> JSONObject().put("text", s.text).put("done", s.done) })
        })
        put("habits", arr(habits) { h ->
            JSONObject().put("id", h.id).put("name", h.name).put("emoji", h.emoji)
                .put("done", JSONArray(h.done.sorted())).put("created", h.created)
        })
        put("runs", arr(runs) { r ->
            JSONObject().put("id", r.id).put("date", r.date).put("km", r.km).put("minutes", r.minutes)
                .put("feeling", r.feeling).put("note", r.note)
        })
        put("days", arr(days) { d ->
            JSONObject().put("day", d.day).put("mood", d.mood).put("gratitude", d.gratitude)
                .put("victory", d.victory).put("text", d.text)
        })
        put("reviews", arr(reviews) { w ->
            JSONObject().put("week", w.week).put("mood", w.mood).put("wins", w.wins)
                .put("improve", w.improve).put("created", w.created)
        })
        put("quoteOn", quoteOn)
        put("quoteTime", quoteTime)
        put("sportOn", sportOn)
        put("sportTime", sportTime)
        put("protectOn", protectOn)
        put("unlockRequestedAt", unlockRequestedAt)
        put("unlockUntil", unlockUntil)
        put("lastModified", lastModified)
    }

    companion object {
        fun defaultHabits() = listOf(
            Habit("h_priere", "Prière du matin", "🙏"),
            Habit("h_bible", "Lecture de la Bible", "📖"),
            Habit("h_jogging", "Jogging", "🏃"),
            Habit("h_trading", "Respecter mon plan de trading", "📈"),
            Habit("h_lecture", "Lire 10 pages", "📚"),
        )

        private fun <T> arr(list: List<T>, f: (T) -> JSONObject) = JSONArray().apply { list.forEach { put(f(it)) } }

        private fun <T> list(o: JSONObject, key: String, f: (JSONObject) -> T): List<T> {
            val a = o.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { runCatching { f(a.getJSONObject(it)) }.getOrNull() }
        }

        private fun strings(a: JSONArray?): List<String> =
            if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

        fun fromJson(o: JSONObject): AppData {
            val d = AppData()
            val temps = o.optJSONArray("temptations") ?: JSONArray()
            return AppData(
                streakStart = o.optLong("streakStart", d.streakStart),
                bestStreakMs = o.optLong("bestStreakMs", 0L),
                resistCount = o.optInt("resistCount", 0),
                temptations = (0 until temps.length()).map { temps.getLong(it) },
                relapses = list(o, "relapses") {
                    Relapse(it.optLong("time"), it.optLong("streakMs"), it.optString("what"), it.optString("trigger"))
                },
                shieldMessage = o.optString("shieldMessage", d.shieldMessage),
                shieldVerse = o.optString("shieldVerse", d.shieldVerse),
                shieldDelaySec = o.optInt("shieldDelaySec", 30),
                blockVpnBrowsers = o.optBoolean("blockVpnBrowsers", false),
                entries = list(o, "entries") { e ->
                    Entry(
                        id = e.getString("id"), section = e.optString("section"), title = e.optString("title"),
                        text = e.optString("text"), tag = e.optString("tag"), date = e.optLong("date"),
                        dueDate = e.optLong("dueDate"), done = e.optBoolean("done"), doneDate = e.optLong("doneDate"),
                        photo = e.optString("photo"), created = e.optLong("created"),
                        steps = list(e, "steps") { s -> Step(s.optString("text"), s.optBoolean("done")) },
                    )
                },
                habits = if (o.has("habits")) list(o, "habits") { h ->
                    Habit(h.getString("id"), h.optString("name"), h.optString("emoji"),
                        strings(h.optJSONArray("done")).toSet(), h.optLong("created"))
                } else defaultHabits(),
                runs = list(o, "runs") { r ->
                    Run(r.getString("id"), r.optLong("date"), r.optDouble("km", 0.0), r.optInt("minutes"),
                        r.optInt("feeling"), r.optString("note"))
                },
                days = list(o, "days") { x ->
                    DayLog(x.getString("day"), x.optInt("mood"), x.optString("gratitude"), x.optString("victory"), x.optString("text"))
                },
                reviews = list(o, "reviews") { w ->
                    WeekReview(w.getString("week"), w.optInt("mood"), w.optString("wins"), w.optString("improve"), w.optLong("created"))
                },
                quoteOn = o.optBoolean("quoteOn", true),
                quoteTime = o.optInt("quoteTime", d.quoteTime),
                sportOn = o.optBoolean("sportOn", true),
                sportTime = o.optInt("sportTime", d.sportTime),
                protectOn = o.optBoolean("protectOn", false),
                unlockRequestedAt = o.optLong("unlockRequestedAt"),
                unlockUntil = o.optLong("unlockUntil"),
                lastModified = o.optLong("lastModified", 0L),
            )
        }
    }
}
