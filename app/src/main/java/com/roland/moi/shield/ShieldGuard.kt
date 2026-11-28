package com.roland.moi.shield

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roland.moi.R
import com.roland.moi.notify.ReminderReceiver

/**
 * Le gardien du Bouclier : vérifie toutes les 15 min que le service d'accessibilité est actif.
 * - Si MOI a reçu la permission WRITE_SECURE_SETTINGS (une seule fois, par ADB depuis un PC),
 *   il réactive le Bouclier tout seul, sans rien demander.
 * - Sinon, il affiche une alerte « Bouclier désactivé » qui ouvre directement le bon réglage.
 */
object ShieldGuard {
    const val ACTION = "com.roland.moi.watchdog"
    private const val CHANNEL = "moi_shield_alert"
    private const val NOTIF_ID = 91
    private const val PREFS = "shield_guard"

    fun component(c: Context) = ComponentName(c, ShieldService::class.java).flattenToString()

    /** MOI peut-il réactiver le Bouclier tout seul ? */
    fun canSelfHeal(c: Context) =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Réactive le Bouclier si possible. Renvoie true si le Bouclier est (ou est redevenu) actif. */
    fun ensure(c: Context): Boolean {
        if (ShieldService.isEnabled(c)) {
            NotificationManagerCompat.from(c).cancel(NOTIF_ID)
            return true
        }
        if (canSelfHeal(c)) {
            runCatching {
                val cr = c.contentResolver
                val current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
                val list = current.split(':').filter { it.isNotBlank() && !it.contains("ShieldService") }.toMutableList()
                list.add(component(c))
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, list.joinToString(":"))
                Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            }
            if (ShieldService.isEnabled(c)) {
                c.getSharedPreferences(PREFS, 0).edit().putLong("healed", System.currentTimeMillis())
                    .putInt("heals", c.getSharedPreferences(PREFS, 0).getInt("heals", 0) + 1).apply()
                NotificationManagerCompat.from(c).cancel(NOTIF_ID)
                return true
            }
        }
        alert(c)
        return false
    }

    /** Nombre de réactivations automatiques (pour l'écran Bouclier). */
    fun heals(c: Context) = c.getSharedPreferences(PREFS, 0).getInt("heals", 0)

    /** Programme la vérification toutes les 15 min (survit à la mise en veille). */
    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, ACTION.hashCode(),
            Intent(c, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi,
        )
    }

    private fun alert(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Bouclier désactivé", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            c, NOTIF_ID,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Ton Bouclier est désactivé")
            .setContentText("Appuie ici pour le réactiver : Accessibilité → MOI — Bouclier.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(c).notify(NOTIF_ID, n) }
    }
}
