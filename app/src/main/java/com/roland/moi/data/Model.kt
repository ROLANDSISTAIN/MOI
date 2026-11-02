package com.roland.moi.data

import org.json.JSONArray
import org.json.JSONObject

data class Relapse(
    val time: Long,
    val streakMs: Long,
    val what: String,
    val trigger: String,
)

data class AppData(
    val streakStart: Long = System.currentTimeMillis(),
    val bestStreakMs: Long = 0L,
    val resistCount: Int = 0,
    val temptations: List<Long> = emptyList(),
    val relapses: List<Relapse> = emptyList(),
    val shieldMessage: String = "Tu vaux plus que quelques minutes de plaisir. Rappelle-toi l'homme que tu veux devenir.",
    val shieldVerse: String = "« Fuyez la débauche. » — 1 Corinthiens 6:18",
    val shieldDelaySec: Int = 30,
    val blockVpnBrowsers: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("streakStart", streakStart)
        put("bestStreakMs", bestStreakMs)
        put("resistCount", resistCount)
        put("temptations", JSONArray(temptations))
        put("relapses", JSONArray().apply {
            relapses.forEach {
                put(JSONObject().apply {
                    put("time", it.time)
                    put("streakMs", it.streakMs)
                    put("what", it.what)
                    put("trigger", it.trigger)
                })
            }
        })
        put("shieldMessage", shieldMessage)
        put("shieldVerse", shieldVerse)
        put("shieldDelaySec", shieldDelaySec)
        put("blockVpnBrowsers", blockVpnBrowsers)
        put("lastModified", lastModified)
    }

    companion object {
        fun fromJson(o: JSONObject): AppData {
            val d = AppData()
            val temps = o.optJSONArray("temptations") ?: JSONArray()
            val rel = o.optJSONArray("relapses") ?: JSONArray()
            return AppData(
                streakStart = o.optLong("streakStart", d.streakStart),
                bestStreakMs = o.optLong("bestStreakMs", 0L),
                resistCount = o.optInt("resistCount", 0),
                temptations = (0 until temps.length()).map { temps.getLong(it) },
                relapses = (0 until rel.length()).map {
                    val r = rel.getJSONObject(it)
                    Relapse(
                        time = r.optLong("time"),
                        streakMs = r.optLong("streakMs"),
                        what = r.optString("what"),
                        trigger = r.optString("trigger"),
                    )
                },
                shieldMessage = o.optString("shieldMessage", d.shieldMessage),
                shieldVerse = o.optString("shieldVerse", d.shieldVerse),
                shieldDelaySec = o.optInt("shieldDelaySec", 30),
                blockVpnBrowsers = o.optBoolean("blockVpnBrowsers", false),
                lastModified = o.optLong("lastModified", 0L),
            )
        }
    }
}
