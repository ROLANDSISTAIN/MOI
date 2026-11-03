package com.roland.moi.data

/** La citation du jour, tirée de tes propres textes (et de quelques versets par défaut). */
object Quotes {
    data class Quote(val text: String, val source: String)

    private val defaults = listOf(
        Quote("Fortifie-toi et prends courage. Ne t'effraie point et ne t'épouvante point, car l'Éternel, ton Dieu, est avec toi partout où tu iras.", "Josué 1:9"),
        Quote("Je puis tout par celui qui me fortifie.", "Philippiens 4:13"),
        Quote("Car Dieu ne nous a pas donné un esprit de timidité, mais un esprit de force, d'amour et de sagesse.", "2 Timothée 1:7"),
        Quote("Ne vous conformez pas au siècle présent, mais soyez transformés par le renouvellement de l'intelligence.", "Romains 12:2"),
        Quote("Celui qui est lent à la colère vaut mieux qu'un héros, et celui qui est maître de lui-même, que celui qui prend des villes.", "Proverbes 16:32"),
        Quote("Recommande ton sort à l'Éternel, mets en lui ta confiance, et il agira.", "Psaume 37:5"),
        Quote("Toute discipline semble d'abord un sujet de tristesse et non de joie ; mais elle produit plus tard un fruit paisible de justice.", "Hébreux 12:11"),
    )

    fun pool(d: AppData): List<Quote> {
        val mine = d.entries.mapNotNull { e ->
            when (Section.of(e.section)) {
                Section.FOI -> if (e.tag == "Verset" && e.text.isNotBlank()) Quote(e.text, e.title) else null
                Section.IDENTITE -> Quote(e.text.ifBlank { e.title }, "Mon identité · ${e.title}")
                Section.VISION -> Quote(e.title + if (e.text.isNotBlank()) " — " + e.text else "", "Ma vision · ${e.tag}")
                Section.LECONS -> Quote(e.title, "Ma leçon")
                Section.DECISIONS -> Quote("J'ai décidé : ${e.title}", "Mes décisions · ${Dates.shortDate(e.date)}")
                Section.LETTRES -> Quote(e.text.take(280), "Lettre à moi-même")
                Section.REALISATIONS -> Quote("Souviens-toi : ${e.title}. Tu l'as fait.", "Mes réalisations")
                Section.BUTS -> if (!e.done) Quote("Ton but : ${e.title}", "Mes buts") else null
                else -> null
            }
        }.filter { it.text.isNotBlank() }
        return if (mine.size >= 3) mine + defaults.take(2) else mine + defaults
    }

    fun today(d: AppData): Quote {
        val p = pool(d)
        val day = Dates.dayKey()
        val i = Math.floorMod(day.hashCode(), p.size)
        return p[i]
    }

    /** Pour le Bouclier : une lettre à soi-même au hasard, ou un élément de la vision. */
    fun forShield(d: AppData): Quote? {
        val letters = d.entries.filter { it.section == Section.LETTRES.key && it.text.isNotBlank() }
        if (letters.isNotEmpty()) return letters.random().let { Quote(it.text, "Lettre à moi-même · ${it.title}") }
        val vision = d.entries.filter { it.section == Section.VISION.key }
        if (vision.isNotEmpty()) return vision.random().let { Quote(it.title, "Ma vision · ${it.tag}") }
        return null
    }
}
