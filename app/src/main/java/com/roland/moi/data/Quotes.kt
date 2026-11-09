package com.roland.moi.data

/** La parole du jour : tes propres textes, des versets, de la sagesse et de la motivation. */
object Quotes {
    data class Quote(val text: String, val source: String)

    /** Tes propres textes de « Ma vie ». */
    fun mine(d: AppData): List<Quote> = d.entries.mapNotNull { e ->
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

    private fun listFor(kind: String, d: AppData): List<Quote> = when (kind) {
        Sagesse.MOI -> mine(d)
        Sagesse.BIBLE -> Sagesse.bible
        Sagesse.SAGESSE -> Sagesse.sagesse
        else -> Sagesse.motivation
    }

    /** Les familles choisies dans les Réglages, dans un ordre fixe (celles qui sont vides sont sautées). */
    private fun kinds(d: AppData): List<String> =
        Sagesse.ALL.filter { it in d.quoteKinds && listFor(it, d).isNotEmpty() }.ifEmpty { listOf(Sagesse.BIBLE) }

    /**
     * Chaque jour, on passe à la famille suivante (mes textes → verset → sagesse → motivation → …),
     * et dans chaque famille on avance dans une liste mélangée : pas de répétition avant d'avoir tout vu.
     */
    fun today(d: AppData): Quote {
        val day = Math.floorDiv(Dates.startOfDay() + 12 * 3600_000L, 24 * 3600_000L).toInt()
        val ks = kinds(d)
        val kind = ks[Math.floorMod(day, ks.size)]
        val list = listFor(kind, d).shuffled(java.util.Random(kind.hashCode().toLong()))
        return list[Math.floorMod(day / ks.size, list.size)]
    }

    /** Une autre parole au hasard, parmi les familles choisies. */
    fun random(d: AppData, not: Quote? = null): Quote {
        val all = kinds(d).flatMap { listFor(it, d) }
        return all.filter { it != not }.ifEmpty { all }.random()
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
