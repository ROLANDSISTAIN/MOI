package com.roland.moi.run

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.roland.moi.R
import com.roland.moi.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Service au premier plan de la course GPS : garde le GPS actif écran éteint,
 * met à jour la notification et annonce chaque kilomètre à voix haute.
 */
class RunService : Service(), LocationListener {

    private var lm: LocationManager? = null
    private var wake: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Course en cours", NotificationManager.IMPORTANCE_LOW))
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, notification(), type)
        } catch (e: Exception) {
            // Permission de localisation absente : on ne peut pas suivre la course
            stopSelf()
            return START_NOT_STICKY
        }

        if (lm == null) {
            lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this, Looper.getMainLooper())
            } catch (_: Exception) { }
        }
        if (wake == null) {
            wake = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "moi:course").apply { acquire(6 * 60 * 60 * 1000L) }
        }
        if (tts == null) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.FRENCH
                    ttsReady = true
                }
            }
        }
        if (ticker == null) {
            ticker = scope.launch {
                while (isActive) {
                    delay(5000)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIF_ID, notification())
                }
            }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        val km = RunTracker.onLocation(location)
        if (km > 0) announce(km)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun announce(km: Int) {
        if (!ttsReady) return
        val s = RunTracker.state.value
        val sec = s.elapsedMs() / 1000
        val pace = s.avgPace()
        val txt = buildString {
            append(if (km == 1) "1 kilomètre. " else "$km kilomètres. ")
            append("Temps : ${spoken(sec)}. ")
            if (pace > 0) append("Allure moyenne : ${pace / 60} minutes ${pace % 60} par kilomètre.")
        }
        tts?.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "km$km")
    }

    private fun spoken(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return buildString {
            if (h > 0) append("$h heure${if (h > 1) "s" else ""} ")
            append("$m minute${if (m > 1) "s" else ""}")
            if (s > 0) append(" $s")
        }
    }

    private fun notification(): android.app.Notification {
        val s = RunTracker.state.value
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(MainActivity.OPEN_RUN, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = when {
            s.status == RunState.Status.PAUSED -> "Course en pause"
            !s.gpsReady -> "Recherche du signal GPS…"
            else -> "Course en cours"
        }
        val text = "${RunTracker.km(s.km)} km · ${RunTracker.duration(s.elapsedMs() / 1000)} · ${RunTracker.pace(s.avgPace())} /km"
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onDestroy() {
        try { lm?.removeUpdates(this) } catch (_: Exception) { }
        lm = null
        try { if (wake?.isHeld == true) wake?.release() } catch (_: Exception) { }
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "moi_course"
        private const val NOTIF_ID = 77
    }
}
