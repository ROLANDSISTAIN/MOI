package com.roland.moi.data

/** Description de chaque section de « Ma vie » : quels champs elle utilise et comment les nommer. */
enum class Section(
    val key: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val titleLabel: String,
    val textLabel: String,
    val tags: List<String> = emptyList(),
    val tagLabel: String = "Catégorie",
    val hasDate: Boolean = false,
    val hasPhoto: Boolean = false,
    val hasDue: Boolean = false,
    val dueLabel: String = "Échéance",
    val hasSteps: Boolean = false,
    val doneLabel: String? = null,
    val empty: String,
) {
    IDENTITE(
        "identite", "Mon identité", "🪪", "Qui je suis, mes valeurs, ce que je défends",
        "Titre (ex : Intégrité)", "Ce que ça veut dire pour moi",
        tags = listOf("Qui je suis", "Mes valeurs", "Ce que je défends"),
        empty = "Écris qui tu es vraiment. Tes valeurs, ce sur quoi tu ne négocies pas.",
    ),
    VISION(
        "vision", "Ma vision", "🔭", "L'homme que je veux être",
        "Ce que je veux être ou avoir", "À quoi ça ressemble concrètement",
        tags = listOf("Dans 1 an", "Dans 5 ans", "Dans 10 ans"), tagLabel = "Horizon",
        empty = "Décris l'homme que tu seras dans 1 an, 5 ans, 10 ans.",
    ),
    BUTS(
        "buts", "Mes buts", "🎯", "Mes objectifs et leurs étapes",
        "Mon but", "Pourquoi c'est important pour moi",
        tags = listOf("Spirituel", "Trading / Argent", "Santé / Sport", "Famille", "Projets", "Personnel"),
        hasDue = true, hasSteps = true, doneLabel = "Atteint",
        empty = "Fixe-toi un but, découpe-le en étapes, coche-les une par une.",
    ),
    REALISATIONS(
        "realisations", "Mes réalisations", "🏆", "Ce que j'ai déjà accompli",
        "Ce que j'ai accompli", "L'histoire derrière",
        hasDate = true, hasPhoto = true,
        empty = "Note tes victoires, petites et grandes. Elles te rappelleront de quoi tu es capable.",
    ),
    PEURS(
        "peurs", "Mes peurs", "🌑", "Les nommer pour les vaincre",
        "Ma peur", "Comment je la combats",
        doneLabel = "Vaincue",
        empty = "Une peur nommée perd la moitié de sa force.",
    ),
    DESIRS(
        "desirs", "Mes désirs", "✨", "Ce que mon cœur veut vraiment",
        "Mon désir", "Pourquoi je le veux",
        doneLabel = "Réalisé",
        empty = "Qu'est-ce que tu désires profondément ?",
    ),
    FOI(
        "foi", "Ma foi", "🙏", "Versets, sujets de prière, prières exaucées",
        "Verset ou sujet de prière", "Texte, référence, ce que Dieu me dit",
        tags = listOf("Verset", "Sujet de prière"), tagLabel = "Type",
        doneLabel = "Exaucée",
        empty = "Garde tes versets marquants et tes sujets de prière. Coche-les quand ils sont exaucés.",
    ),
    LECONS(
        "lecons", "Mes leçons", "📜", "Ce que la vie m'a appris",
        "La leçon", "Ce qui s'est passé",
        tags = listOf("Trading", "Vie", "Relations", "Argent", "Foi", "Travail"), tagLabel = "Domaine",
        empty = "Chaque erreur vaut de l'or si tu en tires la leçon.",
    ),
    LIVRES(
        "livres", "Livres à lire", "📚", "Ma bibliothèque : à lire, en cours, lus",
        "Titre et auteur", "Pourquoi le lire · mes notes",
        tags = listOf("En cours", "À lire", "Lu"), tagLabel = "Statut",
        empty = "Ajoute les livres que tu veux lire, et note ce que tu en retiens.",
    ),
    GENS(
        "gens", "Les gens qui comptent", "🤝", "Les personnes importantes pour moi",
        "Nom", "Ce qu'il ou elle représente pour moi",
        tags = listOf("Famille", "Ami", "Mentor", "Église", "Travail"), tagLabel = "Lien",
        hasPhoto = true,
        empty = "Les personnes qui t'élèvent. Ce qu'elles représentent pour toi.",
    ),
    MOMENTS(
        "moments", "Mes moments forts", "📸", "Mes souvenirs précieux",
        "Le moment", "Ce que j'ai ressenti",
        hasDate = true, hasPhoto = true,
        empty = "Garde tes meilleurs souvenirs, avec une photo et une date.",
    ),
    LETTRES(
        "lettres", "Lettres à moi-même", "✉️", "Le Bouclier te les montre dans les moments faibles",
        "Titre", "Ma lettre",
        empty = "Écris à l'homme qui sera tenté. Le Bouclier lui lira ta lettre.",
    ),
    CAPSULES(
        "capsules", "Capsule temporelle", "⏳", "Des lettres à ouvrir dans le futur",
        "Titre", "Ma lettre au futur moi",
        hasDue = true, dueLabel = "Date d'ouverture",
        empty = "Écris une lettre aujourd'hui. Elle restera scellée jusqu'à la date que tu choisis.",
    );

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: IDENTITE
    }
}
