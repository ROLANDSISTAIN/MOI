package com.roland.moi.shield

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.roland.moi.data.Store

/**
 * Le Bouclier : lit uniquement la barre d'adresse et ce qui est tapé dans les navigateurs.
 * Rien n'est enregistré à part l'heure de la tentation, et rien ne quitte le téléphone.
 * Il protège aussi MOI contre la désinstallation quand la protection est active.
 */
class ShieldService : AccessibilityService() {

    private var cooldownUntil = 0L
    private var lastScan = 0L
    private var lastGuardScan = 0L

    // Navigateurs trouvés sur le téléphone (toute app capable d'ouvrir une page web), même inconnus de la liste.
    @Volatile private var foundBrowsers: Set<String> = emptySet()
    private var browsersCheckedAt = 0L

    private fun refreshBrowsers(now: Long) {
        if (now - browsersCheckedAt < 5 * 60_000L) return
        browsersCheckedAt = now
        foundBrowsers = runCatching {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
            packageManager.queryIntentActivities(i, PackageManager.MATCH_ALL)
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != packageName && it !in Keywords.socialApps && !Protection.isGuardedPackage(it) }
                .toSet()
        }.getOrDefault(foundBrowsers)
    }

    private fun isBrowser(pkg: String) = pkg in Keywords.browsers || pkg in foundBrowsers

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        refreshBrowsers(System.currentTimeMillis())
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        val now = System.currentTimeMillis()

        if (Protection.isGuardedPackage(pkg)) {
            guard(now)
            return
        }

        if (pkg in Keywords.socialApps) {
            if (Store.data.value.socialShield && now >= cooldownUntil && !ShieldActivity.showing) social(e, now)
            return
        }

        refreshBrowsers(now)
        if (!isBrowser(pkg)) return
        if (now < cooldownUntil || ShieldActivity.showing) return

        // Option : bloquer les navigateurs avec VPN intégré (ils contournent le DNS)
        if (Store.data.value.blockVpnBrowsers && pkg in Keywords.vpnBrowsers &&
            e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            trigger("navigateur avec VPN")
            return
        }

        // 1) Le texte transporté par l'événement (frappe, titre de page…)
        val eventText = buildString {
            e.text?.forEach { append(it).append(' ') }
            e.contentDescription?.let { append(it) }
        }
        Keywords.match(eventText)?.let { trigger(it); return }

        // 2) La barre d'adresse (limité pour économiser la batterie)
        if (now - lastScan < 350) return
        lastScan = now
        val root = rootInActiveWindow ?: return
        scanRoot(root)?.let { trigger(it) }
    }

    // ---------- Apps sociales (Facebook, Instagram, TikTok, Telegram…) ----------

    private var lastSocialScan = 0L

    private fun social(e: AccessibilityEvent, now: Long) {
        // 1) Ce qui est tapé dans une barre de recherche
        if (e.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val src = e.source
            val hint = if (android.os.Build.VERSION.SDK_INT >= 26) src?.hintText?.toString() else null
            if (Keywords.isSearchField(src?.viewIdResourceName, hint)) {
                val typed = e.text?.joinToString(" ") ?: src?.text?.toString()
                Keywords.match(typed)?.let { trigger(it); return }
            }
        }
        // 2) Les liens ouverts dans le navigateur intégré (l'adresse affichée en haut)
        if (now - lastSocialScan < 1500) return
        lastSocialScan = now
        val root = rootInActiveWindow ?: return
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty() && visited < 250) {
            val n = stack.removeLast()
            visited++
            val text = n.text?.toString()
            if (text != null && text.length in 4..300) {
                Keywords.matchDomain(text)?.let { trigger(it); return }
                if (n.isEditable && Keywords.isSearchField(n.viewIdResourceName, null)) {
                    Keywords.match(text)?.let { trigger(it); return }
                }
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
        }
    }

    // ---------- Protection anti-désinstallation ----------

    private fun guard(now: Long) {
        if (!Protection.isActive()) return
        if (now - lastGuardScan < 400) return
        lastGuardScan = now
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < 400) {
            val n = stack.removeLast()
            visited++
            n.text?.toString()?.let { if (it.isNotBlank()) texts.add(Keywords.normalize(it.trim())) }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
        }
        if (Protection.screenIsDangerous(texts)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            performGlobalAction(GLOBAL_ACTION_HOME)
            startActivity(Intent(this, ProtectActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // ---------- Bouclier ----------

    private fun scanRoot(root: AccessibilityNodeInfo): String? {
        for (id in Keywords.urlBarIds) {
            val nodes = runCatching { root.findAccessibilityNodeInfosByViewId(id) }.getOrNull() ?: continue
            for (n in nodes) Keywords.match(n.text?.toString())?.let { return it }
        }
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { f ->
            Keywords.match(f.text?.toString())?.let { return it }
        }
        // Secours : parcourir les champs de saisie et barres d'adresse inconnus,
        // et lire le TITRE de la page affichée (attrape les sites au nom neutre).
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty() && visited < 200) {
            val n = stack.removeLast()
            visited++
            if (n.className?.toString() == "android.webkit.WebView") {
                // La zone de la page : son nom est le titre de la page. On ne lit pas le contenu lui-même.
                Keywords.match(n.text?.toString())?.let { return it }
                Keywords.match(n.contentDescription?.toString())?.let { return it }
                continue
            }
            val id = n.viewIdResourceName ?: ""
            val isField = n.isEditable || id.contains("url", true) || id.contains("search", true) ||
                id.contains("omnibox", true) || id.contains("address", true)
            if (isField) Keywords.match(n.text?.toString())?.let { return it }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.add(it) }
        }
        return null
    }

    private fun trigger(word: String) {
        cooldownUntil = System.currentTimeMillis() + 4000
        Store.recordTemptation()
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, ShieldActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("word", word)
        )
    }

    companion object {
        @Volatile var running = false

        fun isEnabled(c: Context): Boolean {
            val enabled = Settings.Secure.getString(
                c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(c.packageName + "/", ignoreCase = true) &&
                enabled.contains("ShieldService", ignoreCase = true)
        }
    }
}
