# MOI — Document de passation

> Application Android personnelle de Roland SENYO.
> Ce document permet à quiconque (ou à une nouvelle session de Claude) de reprendre le projet là où il s'est arrêté.

**Dernière version livrée :** `build-7` (1.0.7), le 3 novembre 2026
**Téléchargement :** https://github.com/ROLANDSISTAIN/MOI/releases/latest/download/MOI.apk
**Dépôt :** https://github.com/ROLANDSISTAIN/MOI (public)

---

## 1. Qu'est-ce que MOI ?

MOI est « l'application de ma vie » : un espace privé, verrouillé, où Roland construit l'homme qu'il veut devenir. Elle contient :

- **Ma vie** : identité, vision, buts, décisions, réalisations, peurs, désirs, foi, leçons, livres, personnes qui comptent, moments forts, lettres à soi-même, capsule temporelle
- **Habitudes** quotidiennes avec séries 🔥
- **Sport** : corde à sauter et pompes, avec un programme pompes de 30 jours et un programme **ventre plat** (corde en intervalles, chrono sonore, assiette, mesures poids / tour de taille)
- **Journal** du jour, **bilans** de la semaine et du mois, **Mon année**
- **Liberté** : un compteur de jours et le **Bouclier**, qui détecte les recherches et sites à caractère sexuel dans les navigateurs et les apps sociales, puis affiche un écran plein

> ⚠️ **Principe voulu par Roland** : l'app n'est **pas centrée** sur l'arrêt du porno. C'est « un plus » parmi le reste. L'accueil montre la vie (parole du jour, journal, habitudes, sport, buts) et la Liberté n'est qu'une ligne discrète en bas.

Style : **noir élégant + doré** (`#0B0B0C` / `#D4AF37`), titres en serif. Tout en français.

---

## 2. Fonctionnalités (état actuel)

| Zone | Contenu |
|---|---|
| **Accueil** | Alerte sauvegarde en retard · 🆘 Je suis tenté · Parole du jour · Capsules à ouvrir · Bilan du dimanche · Journal du jour · Habitudes à cocher · Sport du matin · Buts en cours · Liberté (discret) · ⚙️ Réglages |
| **Ma vie** (onglet) | 14 sections (voir `Sections.kt`) + Mon journal + ✨ Mon année + Mes bilans + Liberté |
| **Habitudes** (onglet) | Par défaut : Prière du matin, Lecture de la Bible, Corde à sauter, Pompes, Lire 10 pages. Modifiables. Série, record, 7 derniers jours (on peut cocher un jour passé). |
| **Sport** (onglet) | Boutons 🪢 Corde (minutes, sauts) et 💪 Pompes (total, séries). Une séance coche l'habitude correspondante. Programme pompes 30 jours, **programme ventre plat** (`VentreScreen.kt` : semaines 1-2 = 10 × 30 s / 30 s, 3×/sem ; ensuite 13 × 1 min / 30 s, 4×/sem ; chrono qui sonne et garde l'écran allumé ; renfo ; assiette ; mesures poids + tour de taille), stats de la semaine et totales, graphiques, records, rappel du matin. |
| **🆘 Je suis tenté** | Respiration 4-4-6 × 4 cycles → lettre à soi-même + message + verset → « 20 pompes » (enregistrées) → victoire comptée |
| **Bouclier** | Service d'accessibilité. Navigateurs : barre d'adresse + texte tapé. Apps sociales : uniquement les champs de recherche et les adresses web affichées (pas les conversations ni les publications). Option : bloquer les navigateurs à VPN intégré. Écran plein avec délai réglable (10 à 120 s), message, verset et lettre à soi-même. |
| **Protection anti-désinstallation** | Réglages → Protection. Le Bouclier bloque les écrans « désinstaller / forcer l'arrêt / désactiver » de MOI dans Paramètres. Déverrouillage : attendre 24 h après la demande **ou** recopier la phrase d'engagement → fenêtre de 15 min. Option administrateur de l'appareil. |
| **Sécurité** | Code PIN (4 à 8 chiffres, hash SHA-256 × 10 000 + sel) + empreinte. Verrouillage après 30 s hors de l'app. `FLAG_SECURE` : captures d'écran bloquées (**voulu**). |
| **Notifications** | Parole du jour (7h00 par défaut) qui annonce aussi les capsules et le bilan du dimanche · Rappel sport (5h30 par défaut). Heures réglables. |
| **Sauvegarde** | Google Drive, dossier caché `appDataFolder` : `moi_backup.json` + chaque photo (`p_*.jpg`). Automatique 1 min après chaque changement + toutes les 12 h. Restauration manuelle (Réglages). Alerte sur l'accueil si plus de 3 jours sans sauvegarde. |

**Retirés à la demande de Roland :** carte DNS, habitude « plan de trading », jogging (remplacé par corde et pompes).

---

## 3. Architecture

Kotlin + Jetpack Compose (Material 3), une seule activité, sans base de données : tout est dans **un fichier JSON**.

```
app/src/main/java/com/roland/moi/
├── MoiApp.kt               Démarrage : Store, canal de notifications, rappels, sauvegarde périodique
├── data/
│   ├── Model.kt            AppData + toutes les classes (Entry, Habit, Run, DayLog, WeekReview…) + JSON
│   ├── Store.kt            Lecture/écriture de moi_data.json, migrations, actions (saveEntry, saveRun…)
│   ├── Sections.kt         Les 14 sections de « Ma vie » (libellés, tags, champs actifs)
│   ├── Quotes.kt           Parole du jour (tirée des textes de Roland + versets) et texte pour le Bouclier
│   ├── Dates.kt            Clés de jour/semaine, séries, formats français
│   ├── Photos.kt           Import/réduction des photos dans filesDir/photos (invisibles dans la galerie)
│   └── PinManager.kt       Code PIN + empreinte (jamais sauvegardés)
├── shield/
│   ├── ShieldService.kt    Service d'accessibilité : navigateurs, apps sociales, protection
│   ├── Keywords.kt         Mots-clés, domaines, liste des navigateurs / apps sociales / navigateurs VPN
│   ├── ShieldActivity.kt   L'écran plein « STOP »
│   ├── Protection.kt       Logique anti-désinstallation + AdminReceiver
│   └── ProtectActivity.kt  Écran « MOI est protégée »
├── backup/
│   ├── DriveBackup.kt      API Google Drive REST (AuthorizationClient, scope drive.appdata)
│   └── BackupWorker.kt     WorkManager (unique + périodique 12 h)
├── notify/Reminders.kt     AlarmManager + ReminderReceiver (reprogramme au démarrage)
└── ui/
    ├── MainActivity.kt     Verrou, onglets, routeur
    ├── Nav.kt              Routes + pile de navigation maison
    ├── Theme.kt / Components.kt / Util.kt   Couleurs, cartes, boutons, champs, dialogues
    └── *Screen.kt          Un fichier par écran
```

### Données
- Fichier : `filesDir/moi_data.json`, écrit de façon atomique (`.tmp` puis renommage)
- `AppData.SCHEMA = 4` (v4 : `fitStart`, `bodyLogs`, habitude « Assiette équilibrée 🥗 » ajoutée). **Pour toute évolution** du format : incrémenter `SCHEMA` et ajouter une étape dans `Store.migrate()`. Tous les champs se lisent avec `opt*()` et une valeur par défaut, pour que les anciennes sauvegardes restent lisibles.
- Une séance de sport est une `Run` avec `kind` = `corde` | `pompes` | `course` (ancien).
- Les livres en « Lu » reçoivent automatiquement un `doneDate` (utilisé par Mon année).

### Navigation
Pas de bibliothèque : `Nav` est une pile de `Route`. Les 4 onglets sont la base, les écrans s'empilent par-dessus. Retour = `nav.back()`.

---

## 4. Construire l'APK

**Personne ne construit en local.** GitHub Actions s'en charge à chaque `push` sur `main` (`.github/workflows/build.yml`) :

1. Java 17 + Gradle 8.10.2 → `gradle :app:assembleRelease`
2. `versionCode` = numéro du build GitHub (`BUILD_NUMBER`)
3. Publie une **Release** `build-N` contenant `MOI.apk` (marquée « latest »)
4. Pousse le résultat sur la branche **`build-log`** : `status.txt`, `errors.txt`, `tail.txt`

Pour suivre un build depuis une session sans accès à l'API GitHub :
```bash
git fetch origin build-log && git show FETCH_HEAD:status.txt
git show FETCH_HEAD:errors.txt | grep "^e: "    # erreurs Kotlin
```

Versions : AGP 8.7.2, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26, Compose BOM 2024.10.01.

### 🔑 Clé de signature — À NE JAMAIS PERDRE NI CHANGER
- Fichier : `app/moi.keystore` (alias `moi`, mots de passe dans `app/build.gradle.kts`)
- SHA-1 : `5B:67:68:4E:6D:94:4B:A4:61:D0:E5:8D:B2:87:AF:23:EA:5F:F3:83`
- Si elle change : les mises à jour ne s'installeront plus par-dessus (il faudrait désinstaller, et donc perdre les données locales) **et** la connexion Google Drive cassera.

---

## 5. Google Drive (Google Cloud)

- Projet Google Cloud **« MOI »** (créé avec un compte Google différent de celui du téléphone, ce qui ne pose pas de problème)
- **Google Drive API** activée
- Google Auth Platform : audience **Externe**, état **Test**
- Client OAuth **Android** : package `com.roland.moi` + SHA-1 ci-dessus
- Le compte Google du téléphone est ajouté en **utilisateur test** → la connexion **fonctionne** (testée le 3 nov. 2026)
- **Non résolu** : le bouton « Publier l'application » reste grisé (« terminez la configuration sur la page de branding »), alors que le branding semble complet. Roland a choisi de rester en mode Test. Conséquence possible : Google redemande l'autorisation de temps en temps. L'alerte « Sauvegarde en retard » (plus de 3 jours) est là pour ça.
- Aucun client ID dans le code : Google reconnaît l'app par le package et le SHA-1.

---

## 6. Installation sur le téléphone (Android, type Tecno/Infinix)

1. Ouvrir le lien de l'APK dans Chrome, puis installer (autoriser « sources inconnues »)
2. Si **Play Protect bloque** : Play Store → profil → Play Protect → ⚙️ → désactiver l'analyse → installer → **réactiver**
3. Bouclier → Accessibilité → « MOI — Bouclier ». Si « **Paramètres restreints** » : Paramètres → Applications → MOI → ⋮ → **Autoriser les paramètres restreints**
4. Autoriser les **notifications** au premier lancement
5. **Batterie** : Paramètres → Applications → MOI → Batterie → **Aucune restriction**, et **décocher MOI** dans « Gestion de l'économie d'énergie ». Sinon le téléphone tue le Bouclier et les rappels.
6. Mise à jour = installer le nouvel APK par-dessus (les données sont gardées)

Consommation vérifiée : MOI n'apparaît pas dans la liste d'utilisation de la batterie.

---

## 7. Points d'attention et limites connues

- **Bouclier** : il ne voit pas le contenu des vidéos ou images dans les feeds (TikTok, Instagram…), seulement les recherches et les adresses. Les identifiants d'interface des navigateurs peuvent changer avec leurs mises à jour (`Keywords.urlBarIds`). Le parcours de secours générique compense en partie.
- **Faux positifs** : les mots sont détectés comme mots entiers (`Keywords.words`), les domaines comme fragments (`domainParts`). Prudence en ajoutant des mots courts.
- **Protection** : elle repose sur la lecture des textes des écrans Paramètres (FR/EN). Une surcouche constructeur très différente peut la contourner.
- **Rappels** : `setAndAllowWhileIdle` (pas d'alarme exacte), donc quelques minutes de décalage possibles.
- **Dépôt public** : il ne contient que du code, jamais de données personnelles. Roland a choisi de le laisser public.
- **Confidentialité** : Roland a demandé que ce projet **ne soit pas enregistré dans la mémoire** de Claude. À respecter dans les prochaines sessions.

---

## 8. Idées pour la suite (non commencées)

- Widget d'écran d'accueil (parole du jour, habitudes), discret
- Export PDF de « Ma vie » ou de « Mon année »
- Restauration automatique proposée au premier lancement sur un nouveau téléphone
- Statistiques des moments faibles croisées avec l'humeur du journal
- Système de trading automatisé (projet séparé évoqué par Roland, à faire « plus tard »)

---

## 9. Historique

| Build | Contenu |
|---|---|
| 1 | V1 : Bouclier, compteur, PIN/empreinte, sauvegarde Drive (code), thème noir et doré |
| 2 | Retrait de la carte DNS |
| 3 | V2+V3 : refonte autour de « Ma vie », habitudes, sport, journal, bilans, citations, capsule, protection |
| 4 | Corde à sauter + pompes (fin du jogging), Livres à lire (5 livres préremplis), retrait du plan de trading |
| 5 | Sauvegarde des photos sur Drive + alerte sauvegarde en retard |
| 6 | (échec de compilation, Mon année) |
| 7 | 🆘 Je suis tenté, Bouclier apps sociales, programme pompes 30 jours, Mon année, Mes décisions |
| 8 | Programme ventre plat (corde en intervalles + chrono, assiette, mesures), habitude Assiette équilibrée |
