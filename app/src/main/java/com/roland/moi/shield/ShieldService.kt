package com.roland.moi.shield

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.roland.moi.data.Store

/**
 * Le Bouclier : lit uniquement la barre d'adresse et ce qui est tapé dans les navigateurs.
 * Rien n'est enregistré à part l'heure de la tentation, et rien ne quitte le téléphone.
 */
class ShieldService : AccessibilityService() {

    private var cooldownUntil = 0L
    private var lastScan = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
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
        if (pkg !in Keywords.browsers) return
        val now = System.currentTimeMillis()
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

    private fun scanRoot(root: AccessibilityNodeInfo): String? {
        for (id in Keywords.urlBarIds) {
            val nodes = runCatching { root.findAccessibilityNodeInfosByViewId(id) }.getOrNull() ?: continue
            for (n in nodes) Keywords.match(n.text?.toString())?.let { return it }
        }
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { f ->
            Keywords.match(f.text?.toString())?.let { return it }
        }
        // Secours : parcourir les champs de saisie et barres d'adresse inconnus
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        while (stack.isNotEmpty() && visited < 200) {
            val n = stack.removeLast()
            visited++
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
