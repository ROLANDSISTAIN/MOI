package com.roland.moi.shield

import java.text.Normalizer

/** Détection des recherches et sites à caractère sexuel (français + anglais). */
object Keywords {

    /** Mots détectés seulement comme mot entier (évite « sussex », « essexe », etc.). */
    private val words = listOf(
        "porn", "porno", "pornographie", "pornographique", "porne", "xxx", "sexe", "sex", "sexy",
        "nude", "nudes", "nue", "nues", "naked", "nsfw", "hentai", "rule34", "r34",
        "milf", "boobs", "tits", "seins nus", "fesses nues", "pussy", "blowjob",
        "sodomie", "masturbation", "masturber", "branlette", "orgasme",
        "erotique", "erotic", "camgirl", "cam girl", "webcam sexe", "onlyfans", "only fans",
        "fansly", "escorte girl", "strip tease", "striptease", "lingerie sexy",
        "baise", "plan cul", "salope", "chaudasse", "stepmom", "stepsister",
        "sextape", "sex tape", "leak nude", "deepnude", "xvideo", "xvideos", "xnxx", "xhamster",
        "pornhub", "youporn", "redtube", "brazzers", "spankbang", "eporner", "tnaflix", "porntrex",
        "chaturbate", "stripchat", "bongacams", "livejasmin", "cam4", "camsoda",
        "jacquieetmichel", "jacquie et michel", "nhentai", "hanime", "motherless", "beeg",
        "tube8", "porn300", "hqporner", "daftsex", "sxyprn", "thothub", "fapello", "erome",
    )

    /** Morceaux d'adresses web (détectés n'importe où dans l'URL). */
    private val domainParts = listOf(
        "pornhub", "xvideos", "xnxx", "xhamster", "youporn", "redtube", "brazzers", "spankbang",
        "eporner", "tnaflix", "porntrex", "chaturbate", "stripchat", "bongacams", "livejasmin",
        "camsoda", "onlyfans", "fansly", "nhentai", "hanime", "motherless", "hqporner",
        "daftsex", "sxyprn", "thothub", "fapello", "erome.com", "rule34", "jacquieetmichel",
        "porn", "xxx", "sex.com", "hentai", "nsfw",
    )

    private val wordRegex: Regex by lazy {
        val alt = words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        Regex("(^|[^a-z0-9])($alt)([^a-z0-9]|$)")
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    /** Renvoie le mot détecté, ou null si le texte est propre. */
    fun match(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.length < 2) return null
        val t = normalize(raw)
        // Les sites, même collés dans une URL (ex: fr.pornhub.com/...)
        domainParts.firstOrNull { t.contains(it) }?.let { return it }
        // Les mots dans une recherche (ex: "video sexe", "sexe+video")
        val spaced = t.replace('+', ' ').replace('_', ' ').replace('-', ' ').replace("%20", " ")
        return wordRegex.find(spaced)?.groupValues?.get(2)
    }

    /** Seulement les sites (pour les liens ouverts depuis les apps sociales). */
    fun matchDomain(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val t = normalize(raw.trim())
        if (t.contains(' ') || !t.contains('.')) return null // ce n'est pas une adresse
        return domainParts.firstOrNull { t.contains(it) }
    }

    /** Apps sociales : on surveille leurs recherches et les liens qu'elles ouvrent, pas les publications. */
    val socialApps = setOf(
        "com.facebook.katana", "com.facebook.lite", "com.facebook.orca", "com.facebook.mlite",
        "com.instagram.android", "com.instagram.lite",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go",
        "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus",
        "com.whatsapp", "com.whatsapp.w4b",
        "com.twitter.android", "com.snapchat.android", "com.reddit.frontpage",
        "com.pinterest", "com.google.android.youtube", "com.linkedin.android",
    )

    private val searchHints = listOf("search", "query", "recherch", "cherch", "explore", "find")

    fun isSearchField(viewId: String?, hint: String?): Boolean {
        val v = (viewId ?: "").lowercase() + " " + normalize(hint ?: "")
        return searchHints.any { v.contains(it) }
    }

    /** Navigateurs connus. Les autres apps ne sont pas surveillées. */
    val browsers = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
        "com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser.beta",
        "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.focus", "org.mozilla.fenix",
        "com.opera.browser", "com.opera.mini.native", "com.opera.gx", "com.opera.browser.beta",
        "com.microsoft.emmx", "com.brave.browser", "com.duckduckgo.mobile.android",
        "com.vivaldi.browser", "com.kiwibrowser.browser", "com.UCMobile.intl", "com.uc.browser.en",
        "com.mi.globalbrowser", "com.android.browser", "com.huawei.browser", "com.hihonor.baidu.browser",
        "com.transsion.phoenix", "com.yandex.browser", "com.cloudmosa.puffinFree",
        "com.ecosia.android", "org.torproject.torbrowser", "com.google.android.googlequicksearchbox",
        "mark.via.gp", "com.aloha.browser", "com.cake.browser", "org.chromium.chrome",
        "com.tencent.mtt", "com.mx.browser", "com.oupeng.mini.android",
    )

    /** Navigateurs avec VPN/proxy intégré, capables de contourner le DNS filtrant. */
    val vpnBrowsers = setOf(
        "com.opera.browser", "com.opera.gx", "com.opera.browser.beta", "com.aloha.browser",
        "org.torproject.torbrowser", "com.epicbrowser", "com.cloudmosa.puffinFree",
        "com.tenta.android", "com.opera.mini.native",
    )

    /** Identifiants des barres d'adresse des navigateurs les plus courants. */
    val urlBarIds = listOf(
        "com.android.chrome:id/url_bar", "com.android.chrome:id/search_box_text",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.sec.android.app.sbrowser:id/custom_tab_toolbar_url_bar_text",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "org.mozilla.firefox:id/mozac_browser_toolbar_edit_url_view",
        "org.mozilla.fenix:id/mozac_browser_toolbar_url_view",
        "com.opera.browser:id/url_field", "com.opera.mini.native:id/url_field",
        "com.microsoft.emmx:id/url_bar", "com.brave.browser:id/url_bar",
        "com.vivaldi.browser:id/url_bar", "com.kiwibrowser.browser:id/url_bar",
        "com.duckduckgo.mobile.android:id/omnibarTextInput",
        "com.mi.globalbrowser:id/url", "com.UCMobile.intl:id/titlebar",
        "com.google.android.googlequicksearchbox:id/googleapp_search_box",
    )
}
