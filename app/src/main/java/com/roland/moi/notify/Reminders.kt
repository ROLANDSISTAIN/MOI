package com.roland.moi.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roland.moi.R
import com.roland.moi.data.Dates
import com.roland.moi.data.Quotes
import com.roland.moi.data.Section
import com.roland.moi.data.Store
import com.roland.moi.ui.MainActivity
import java.util.Calendar

/** Notifications du matin : citation du jour, rappel jogging, capsule à ouvrir, bilan du dimanche. */
object Reminders {
    private const val CHANNEL = "moi_matin"
    const val QUOTE = "quote"
    const val SPORT = "sport"

    fun createChannel(c: Context) {
        val ch = NotificationChannel(CHANNEL, "Rappels de MOI", NotificationManager.IMPORTANCE_DEFAULT)
        ch.lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun scheduleAll(c: Context) {
        val d = Store.data.value
        schedule(c, QUOTE, d.quoteTime, d.quoteOn)
        schedule(c, SPORT, d.sportTime, d.sportOn)
    }

    private fun pending(c: Context, kind: String): PendingIntent =
        PendingIntent.getBroadcast(
            c, kind.hashCode(),
            Intent(c, ReminderReceiver::class.java).setAction("com.roland.moi.$kind").putExtra("kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun schedule(c: Context, kind: String, minutes: Int, on: Boolean) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pending(c, kind)
        am.cancel(pi)
        if (!on) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    fun fire(c: Context, kind: String) {
        val d = Store.data.value
        when (kind) {
            QUOTE -> {
                val q = Quotes.today(d)
                show(c, 1, "Ta parole du jour", q.text + "\n— " + q.source)
                // Capsules qui s'ouvrent aujourd'hui
                val today = Dates.dayKey()
                d.entries.filter { it.section == Section.CAPSULES.key && Dates.dayKey(it.dueDate) == today }
                    .forEachIndexed { i, e -> show(c, 10 + i, "⏳ Une lettre du passé s'ouvre aujourd'hui", "« ${e.title} » t'attend dans MOI.") }
                // Dimanche : bilan de la semaine
                if (Dates.dayOfWeek() == Calendar.SUNDAY && d.reviews.none { it.week == Dates.weekKey() }) {
                    show(c, 3, "Bilan de la semaine", "Prends 3 minutes ce soir pour faire le point sur ta semaine.")
                }
            }
            SPORT -> {
                if (d.runs.none { Dates.dayKey(it.date) == Dates.dayKey() }) {
                    show(c, 2, "🏃 C'est l'heure du jogging", "Un matin de plus. Mets tes chaussures, le reste suivra.")
                }
            }
        }
        scheduleAll(c)
    }

    private fun show(c: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle("MOI").setContentText("Nouveau message").build()
            )
            .build()
        runCatching { NotificationManagerCompat.from(c).notify(id, n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Reminders.scheduleAll(c)
            else -> Reminders.fire(c, intent.getStringExtra("kind") ?: return)
        }
    }
}
