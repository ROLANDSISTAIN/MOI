package com.roland.moi.shield

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.roland.moi.data.AppData
import com.roland.moi.data.Store

/**
 * Protection anti-désinstallation.
 * Pour désactiver MOI ou le Bouclier, il faut soit attendre 24 h après en avoir fait la demande,
 * soit recopier la phrase d'engagement. Ensuite on a 15 minutes.
 */
object Protection {
    const val WAIT_MS = 24L * 60 * 60 * 1000
    const val WINDOW_MS = 15L * 60 * 1000
    const val PHRASE = "Je choisis de retirer ma protection en sachant que je mérite mieux que mes faiblesses et je prends la responsabilité de ce choix devant Dieu"

    fun isUnlocked(d: AppData = Store.data.value) = System.currentTimeMillis() < d.unlockUntil

    fun isActive(d: AppData = Store.data.value) = d.protectOn && !isUnlocked(d)

    fun waitLeftMs(d: AppData = Store.data.value): Long =
        if (d.unlockRequestedAt == 0L) WAIT_MS else (d.unlockRequestedAt + WAIT_MS - System.currentTimeMillis()).coerceAtLeast(0)

    fun requestUnlock() = Store.update { it.copy(unlockRequestedAt = System.currentTimeMillis()) }

    fun cancelRequest() = Store.update { it.copy(unlockRequestedAt = 0L) }

    fun openWindow() = Store.update { it.copy(unlockUntil = System.currentTimeMillis() + WINDOW_MS, unlockRequestedAt = 0L) }

    fun phraseMatches(typed: String) =
        typed.trim().replace(Regex("\\s+"), " ").equals(PHRASE, ignoreCase = true)

    // ---- Administrateur de l'appareil (empêche la désinstallation directe) ----

    fun admin(c: Context) = ComponentName(c, AdminReceiver::class.java)

    fun isAdmin(c: Context) =
        c.getSystemService(DevicePolicyManager::class.java).isAdminActive(admin(c))

    fun adminIntent(c: Context): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(c))
        .putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Empêche la désinstallation de MOI dans un moment de faiblesse.",
        )

    fun removeAdmin(c: Context) {
        runCatching { c.getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(admin(c)) }
    }

    // ---- Détection des écrans à protéger (dans Paramètres / désinstallation) ----

    private val actionWords = listOf(
        "desinstaller", "uninstall", "forcer l'arret", "forcer l’arret", "force stop", "desactiver", "deactivate",
        "effacer les donnees", "vider le stockage", "clear storage", "clear data", "supprimer les donnees",
    )
    private val markers = listOf("le bouclier de moi surveille", "la protection de moi empeche")

    fun isGuardedPackage(pkg: String) =
        pkg.contains("settings", true) || pkg.contains("packageinstaller", true) ||
            pkg.contains("permissioncontroller", true) || pkg == "com.android.vending"

    /** texts = textes de l'écran, déjà normalisés (minuscules, sans accents). */
    fun screenIsDangerous(texts: List<String>): Boolean {
        if (texts.any { t -> markers.any { t.startsWith(it) } }) return true
        val namesMoi = texts.any { it == "moi" || it == "moi — bouclier" || it == "moi - bouclier" || it.startsWith("desinstaller moi") || it.startsWith("uninstall moi") }
        if (!namesMoi) return false
        return texts.any { t -> actionWords.any { t.contains(it) } }
    }
}

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Es-tu sûr ? Souviens-toi de l'homme que tu veux devenir."
}
