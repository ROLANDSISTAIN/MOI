package com.roland.moi.data

import com.roland.moi.data.Quotes.Quote

/**
 * La bibliothèque intégrée de la parole du jour (hors ligne).
 * Versets : Louis Segond. Citations : auteurs connus, attributions vérifiées autant que possible.
 */
object Sagesse {
    const val MOI = "moi"
    const val BIBLE = "bible"
    const val SAGESSE = "sagesse"
    const val MOTIVATION = "motivation"
    val ALL = listOf(MOI, BIBLE, SAGESSE, MOTIVATION)

    fun label(kind: String) = when (kind) {
        MOI -> "✍️ Mes textes"
        BIBLE -> "📖 Versets"
        SAGESSE -> "🦉 Sagesse et citations"
        else -> "🔥 Motivation"
    }

    val bible = listOf(
        Quote("Fortifie-toi et prends courage. Ne t'effraie point et ne t'épouvante point, car l'Éternel, ton Dieu, est avec toi partout où tu iras.", "Josué 1:9"),
        Quote("Je puis tout par celui qui me fortifie.", "Philippiens 4:13"),
        Quote("Car Dieu ne nous a pas donné un esprit de timidité, mais un esprit de force, d'amour et de sagesse.", "2 Timothée 1:7"),
        Quote("Ne vous conformez pas au siècle présent, mais soyez transformés par le renouvellement de l'intelligence.", "Romains 12:2"),
        Quote("Celui qui est lent à la colère vaut mieux qu'un héros, et celui qui est maître de lui-même, que celui qui prend des villes.", "Proverbes 16:32"),
        Quote("Recommande ton sort à l'Éternel, mets en lui ta confiance, et il agira.", "Psaume 37:5"),
        Quote("Toute discipline semble d'abord un sujet de tristesse et non de joie ; mais elle produit plus tard pour ceux qui ont été ainsi exercés un fruit paisible de justice.", "Hébreux 12:11"),
        Quote("Garde ton cœur plus que toute autre chose, car de lui viennent les sources de la vie.", "Proverbes 4:23"),
        Quote("Va vers la fourmi, paresseux ; considère ses voies, et deviens sage.", "Proverbes 6:6"),
        Quote("Celui qui fréquente les sages devient sage, mais celui qui se plaît avec les insensés s'en trouve mal.", "Proverbes 13:20"),
        Quote("Recommande à l'Éternel tes œuvres, et tes projets réussiront.", "Proverbes 16:3"),
        Quote("Les projets de l'homme diligent ne mènent qu'à l'abondance, mais celui qui agit avec précipitation n'arrive qu'à la disette.", "Proverbes 21:5"),
        Quote("Comme le fer aiguise le fer, ainsi un homme aiguise un autre homme.", "Proverbes 27:17"),
        Quote("Car sept fois le juste tombe, et il se relève.", "Proverbes 24:16"),
        Quote("Confie-toi en l'Éternel de tout ton cœur, et ne t'appuie pas sur ta sagesse ; reconnais-le dans toutes tes voies, et il aplanira tes sentiers.", "Proverbes 3:5-6"),
        Quote("Si tu vois un homme habile dans son ouvrage, il se tient auprès des rois.", "Proverbes 22:29"),
        Quote("Celui qui agit d'une main lâche s'appauvrit, mais la main des diligents enrichit.", "Proverbes 10:4"),
        Quote("Une réponse douce calme la fureur, mais une parole dure excite la colère.", "Proverbes 15:1"),
        Quote("Il y a un temps pour tout, un temps pour toute chose sous les cieux.", "Ecclésiaste 3:1"),
        Quote("Si quelqu'un d'entre vous manque de sagesse, qu'il la demande à Dieu, qui donne à tous simplement et sans reproche, et elle lui sera donnée.", "Jacques 1:5"),
        Quote("Ne nous lassons pas de faire le bien ; car nous moissonnerons au temps convenable, si nous ne nous relâchons pas.", "Galates 6:9"),
        Quote("Mais ceux qui se confient en l'Éternel renouvellent leur force. Ils prennent le vol comme les aigles ; ils courent, et ne se lassent point, ils marchent, et ne se fatiguent point.", "Ésaïe 40:31"),
        Quote("Aucune tentation ne vous est survenue qui n'ait été humaine, et Dieu, qui est fidèle, ne permettra pas que vous soyez tentés au-delà de vos forces.", "1 Corinthiens 10:13"),
        Quote("Tout ce que vous faites, faites-le de bon cœur, comme pour le Seigneur et non pour des hommes.", "Colossiens 3:23"),
        Quote("Ne vous inquiétez donc pas du lendemain ; car le lendemain aura soin de lui-même. À chaque jour suffit sa peine.", "Matthieu 6:34"),
        Quote("Celui qui est fidèle dans les moindres choses l'est aussi dans les grandes.", "Luc 16:10"),
        Quote("Car je connais les projets que j'ai formés sur vous, dit l'Éternel, projets de paix et non de malheur, afin de vous donner un avenir et de l'espérance.", "Jérémie 29:11"),
        Quote("Que personne ne méprise ta jeunesse ; mais sois un modèle pour les fidèles, en parole, en conduite, en charité, en foi, en pureté.", "1 Timothée 4:12"),
        Quote("Cherchez premièrement le royaume et la justice de Dieu ; et toutes ces choses vous seront données par-dessus.", "Matthieu 6:33"),
        Quote("L'Éternel est mon berger : je ne manquerai de rien.", "Psaume 23:1"),
        Quote("Ta parole est une lampe à mes pieds, et une lumière sur mon sentier.", "Psaume 119:105"),
        Quote("Le commencement de la sagesse, c'est la crainte de l'Éternel.", "Proverbes 9:10"),
        Quote("Ne crains rien, car je suis avec toi ; ne promène pas des regards inquiets, car je suis ton Dieu ; je te fortifie, je viens à ton secours.", "Ésaïe 41:10"),
        Quote("Soyez forts et que votre cœur s'affermisse, vous tous qui espérez en l'Éternel !", "Psaume 31:25"),
        Quote("Mais il m'a dit : Ma grâce te suffit, car ma puissance s'accomplit dans la faiblesse.", "2 Corinthiens 12:9"),
    )

    val sagesse = listOf(
        // Philosophes et auteurs
        Quote("Ce n'est pas parce que les choses sont difficiles que nous n'osons pas, c'est parce que nous n'osons pas qu'elles sont difficiles.", "Sénèque"),
        Quote("Ce n'est pas que nous ayons peu de temps, c'est que nous en perdons beaucoup.", "Sénèque"),
        Quote("Il n'y a pas de vent favorable pour celui qui ne sait pas où il va.", "D'après Sénèque"),
        Quote("Ne perds plus de temps à discuter de ce que doit être un homme de bien. Sois-en un.", "Marc Aurèle"),
        Quote("La meilleure façon de se venger d'un ennemi, c'est de ne pas lui ressembler.", "Marc Aurèle"),
        Quote("Ce qui trouble les hommes, ce ne sont pas les choses, mais les jugements qu'ils portent sur les choses.", "Épictète"),
        Quote("Il y a ce qui dépend de nous, et ce qui ne dépend pas de nous.", "Épictète"),
        Quote("Un voyage de mille lieues commence toujours par un premier pas.", "Lao Tseu"),
        Quote("Nous sommes ce que nous faisons de manière répétée. L'excellence n'est donc pas un acte, mais une habitude.", "Will Durant, d'après Aristote"),
        Quote("Vous ne vous élevez pas au niveau de vos objectifs. Vous tombez au niveau de vos systèmes.", "James Clear"),
        Quote("Chaque action que vous faites est un vote pour le type de personne que vous voulez devenir.", "James Clear"),
        Quote("Le leadership, c'est l'influence. Rien de plus, rien de moins.", "John C. Maxwell"),
        Quote("Si tu ne peux pas voler, cours. Si tu ne peux pas courir, marche. Si tu ne peux pas marcher, rampe. Mais quoi que tu fasses, continue d'avancer.", "Martin Luther King"),
        Quote("Après avoir gravi une haute colline, on découvre qu'il en reste beaucoup d'autres à gravir.", "Nelson Mandela"),
        Quote("Fais ce que tu peux, avec ce que tu as, là où tu es.", "Cité par Theodore Roosevelt"),
        Quote("On ne voit bien qu'avec le cœur. L'essentiel est invisible pour les yeux.", "Antoine de Saint-Exupéry"),
        Quote("En Afrique, quand un vieillard meurt, c'est une bibliothèque qui brûle.", "Amadou Hampâté Bâ"),
        Quote("Bien faire vaut mieux que bien dire.", "Benjamin Franklin"),
        Quote("Vingt fois sur le métier remettez votre ouvrage.", "Boileau"),
        Quote("Rien ne sert de courir ; il faut partir à point.", "La Fontaine"),
        Quote("Patience et longueur de temps font plus que force ni que rage.", "La Fontaine"),
        Quote("Aide-toi, le ciel t'aidera.", "La Fontaine"),
        Quote("À vaincre sans péril, on triomphe sans gloire.", "Corneille"),
        Quote("Le cœur a ses raisons que la raison ne connaît point.", "Pascal"),
        Quote("La discipline est le pont entre les objectifs et leur accomplissement.", "Jim Rohn"),
        Quote("La motivation te fait commencer. L'habitude te fait continuer.", "Jim Ryun"),
        // Proverbes
        Quote("Si tu veux aller vite, marche seul. Si tu veux aller loin, marchons ensemble.", "Proverbe africain"),
        Quote("Le séjour dans l'eau ne transforme pas un tronc d'arbre en crocodile.", "Proverbe africain"),
        Quote("La pluie ne tombe pas sur un seul toit.", "Proverbe africain"),
        Quote("Quand tu ne sais plus où tu vas, retourne-toi et regarde d'où tu viens.", "Proverbe africain"),
        Quote("C'est au bout de la vieille corde qu'on tisse la nouvelle.", "Proverbe africain"),
        Quote("Celui qui pose des questions ne se perd jamais.", "Proverbe africain"),
        Quote("Un seul bracelet ne tinte pas.", "Proverbe africain"),
        Quote("L'arbre ne tombe pas au premier coup de hache.", "Proverbe africain"),
        Quote("Le mensonge donne des fleurs mais pas de fruits.", "Proverbe africain"),
        Quote("Ce que le vieux voit assis, le jeune ne le voit pas debout.", "Proverbe africain"),
        Quote("Petit à petit, l'oiseau fait son nid.", "Proverbe"),
        Quote("Qui veut voyager loin ménage sa monture.", "Proverbe"),
        Quote("Mieux vaut prévenir que guérir.", "Proverbe"),
        Quote("L'habit ne fait pas le moine.", "Proverbe"),
        Quote("Il faut battre le fer pendant qu'il est chaud.", "Proverbe"),
    )

    val motivation = listOf(
        Quote("Tu n'as pas besoin d'être parfait aujourd'hui. Seulement un peu meilleur qu'hier.", "Motivation"),
        Quote("Personne ne viendra faire tes pompes à ta place.", "Motivation"),
        Quote("Fais aujourd'hui ce que l'homme que tu seras dans un an te remerciera d'avoir fait.", "Motivation"),
        Quote("Une petite victoire chaque jour, c'est 365 victoires dans l'année.", "Motivation"),
        Quote("La discipline, c'est choisir entre ce que tu veux maintenant et ce que tu veux le plus.", "Motivation"),
        Quote("Tu ne contrôles pas le résultat. Tu contrôles l'effort.", "Motivation"),
        Quote("Les jours où tu n'en as pas envie sont ceux qui comptent le plus.", "Motivation"),
        Quote("Ne compte pas les jours. Fais que les jours comptent.", "Motivation"),
        Quote("Le confort d'aujourd'hui est souvent le regret de demain.", "Motivation"),
        Quote("Commence là où tu es. Utilise ce que tu as. Fais ce que tu peux.", "Motivation"),
        Quote("Un homme se mesure à ce qu'il fait quand personne ne le regarde.", "Motivation"),
        Quote("Tomber n'est pas un échec. Rester à terre, si.", "Motivation"),
        Quote("Ce que tu répètes chaque jour finit par te construire.", "Motivation"),
        Quote("Le marché sera encore là demain. Ton capital, pas forcément. La patience paie.", "Motivation · trading"),
        Quote("Un bon trade n'est pas un trade gagnant : c'est un trade qui respecte ton plan.", "Motivation · trading"),
        Quote("La régularité bat le talent quand le talent n'est pas régulier.", "Motivation"),
        Quote("Ton corps entend tout ce que ton esprit dit. Parle-lui avec force.", "Motivation"),
        Quote("Une heure de discipline vaut mieux qu'une semaine de bonnes intentions.", "Motivation"),
        Quote("Tu es le seul à pouvoir écrire ton histoire. Écris un beau chapitre aujourd'hui.", "Motivation"),
        Quote("Le succès est la somme de petits efforts répétés jour après jour.", "Motivation"),
        Quote("Les grands arbres viennent de petites graines plantées avec patience.", "Motivation"),
        Quote("Sois fier de chaque pas, même petit. Il te rapproche du but.", "Motivation"),
        Quote("Ta vie change le jour où ta décision devient plus forte que ton excuse.", "Motivation"),
        Quote("Le meilleur moment pour commencer était hier. Le deuxième meilleur, c'est maintenant.", "Motivation"),
        Quote("Garde les yeux sur l'homme que tu deviens, pas sur ce que tu laisses derrière toi.", "Motivation"),
    )
}
