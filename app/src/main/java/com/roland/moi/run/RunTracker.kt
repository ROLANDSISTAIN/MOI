package com.roland.moi.run

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.roland.moi.data.Run
import com.roland.moi.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** Un point du tracé. */
data class GeoPoint(val lat: Double, val lon: Double)

/** État de la course en cours (ou terminée, en attente d'enregistrement). */
data class RunState(
    val status: Status = Status.IDLE,
    val points: List<GeoPoint> = emptyList(),
    val meters: Double = 0.0,
    val activeMs: Long = 0L,          // temps de course hors pauses
    val resumedAt: Long = 0L,         // elapsedRealtime du dernier départ/reprise
    val gpsReady: Boolean = false,    // a reçu au moins un point précis
    val accuracy: Float = 0f,
    val recent: List<Pair<Long, Double>> = emptyList(), // (temps actif ms, mètres) pour l'allure du moment
    val kmAnnounced: Int = 0,
    val startedAt: Long = 0L,         // heure de départ (currentTimeMillis)
    val satsUsed: Int = 0,            // satellites utilisés pour la position
    val satsSeen: Int = 0,            // satellites visibles
    val lastFixAt: Long = 0L,         // elapsedRealtime de la dernière position reçue
    val error: String = "",           // problème empêchant le suivi (affiché à l'écran)
) {
    enum class Status { IDLE, RUNNING, PAUSED, FINISHED }

    /** Temps actif à l'instant t (elapsedRealtime). */
    fun elapsedMs(now: Long = SystemClock.elapsedRealtime()): Long =
        activeMs + if (status == Status.RUNNING && resumedAt > 0) now - resumedAt else 0L

    val km: Double get() = meters / 1000.0

    /** Allure moyenne en secondes par km (0 si pas assez de distance). */
    fun avgPace(now: Long = SystemClock.elapsedRealtime()): Int =
        if (meters < 50) 0 else ((elapsedMs(now) / 1000.0) / km).roundToInt()

    /** Allure du moment, sur les ~30 dernières secondes. */
    fun currentPace(): Int {
        if (recent.size < 2) return 0
        val (t0, m0) = recent.first()
        val (t1, m1) = recent.last()
        val dm = m1 - m0
        if (dm < 20) return 0
        return (((t1 - t0) / 1000.0) / (dm / 1000.0)).roundToInt()
    }
}

/**
 * Le moteur de la course GPS. Le [RunService] lui envoie les positions ; l'écran lit [state].
 * Tout fonctionne sans connexion : le GPS passe par les satellites.
 */
object RunTracker {
    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state.asStateFlow()

    private var last: Location? = null

    val idle = RunState.Status.IDLE

    // Filtres anti-sauts du GPS (adaptés aux téléphones au GPS moins précis)
    const val MAX_ACCURACY_M = 50f
    private const val MIN_STEP_M = 5.0
    private const val MAX_SPEED_MS = 9.0   // ~32 km/h : au-delà, c'est un saut du GPS

    fun onSatellites(used: Int, seen: Int) {
        _state.value = _state.value.copy(satsUsed = used, satsSeen = seen)
    }

    fun onError(msg: String) {
        _state.value = _state.value.copy(error = msg)
    }

    fun start(ctx: Context) {
        last = null
        val now = SystemClock.elapsedRealtime()
        _state.value = RunState(status = RunState.Status.RUNNING, resumedAt = now, startedAt = System.currentTimeMillis())
        ContextCompat.startForegroundService(ctx, Intent(ctx, RunService::class.java))
    }

    fun pause() {
        val s = _state.value
        if (s.status != RunState.Status.RUNNING) return
        val now = SystemClock.elapsedRealtime()
        _state.value = s.copy(status = RunState.Status.PAUSED, activeMs = s.elapsedMs(now), resumedAt = 0L, recent = emptyList())
        last = null // pas de ligne droite entre la pause et la reprise
    }

    fun resume() {
        val s = _state.value
        if (s.status != RunState.Status.PAUSED) return
        _state.value = s.copy(status = RunState.Status.RUNNING, resumedAt = SystemClock.elapsedRealtime())
    }

    /** Arrête le suivi. La course reste affichée (FINISHED) jusqu'à [save] ou [discard]. */
    fun finish(ctx: Context) {
        val s = _state.value
        if (s.status == RunState.Status.IDLE || s.status == RunState.Status.FINISHED) return
        val now = SystemClock.elapsedRealtime()
        _state.value = s.copy(status = RunState.Status.FINISHED, activeMs = s.elapsedMs(now), resumedAt = 0L)
        ctx.stopService(Intent(ctx, RunService::class.java))
    }

    fun save(feeling: Int, note: String) {
        val s = _state.value
        if (s.status != RunState.Status.FINISHED) return
        val sec = (s.activeMs / 1000).toInt()
        Store.saveRun(
            Run(
                id = Store.newId(), date = if (s.startedAt > 0) s.startedAt else System.currentTimeMillis(),
                km = (s.km * 100).roundToInt() / 100.0, minutes = (sec + 30) / 60, seconds = sec,
                feeling = feeling, note = note, kind = Run.KIND_COURSE, track = encode(s.points),
            )
        )
        _state.value = RunState()
    }

    fun discard(ctx: Context) {
        ctx.stopService(Intent(ctx, RunService::class.java))
        _state.value = RunState()
    }

    /** Appelé par le service à chaque position. Renvoie le nombre de km franchis à annoncer (0 sinon). */
    fun onLocation(loc: Location): Int {
        val s0 = _state.value
        val acc = if (loc.hasAccuracy()) loc.accuracy else 999f
        val s = s0.copy(lastFixAt = SystemClock.elapsedRealtime(), error = "")
        if (s.status != RunState.Status.RUNNING) {
            _state.value = s.copy(accuracy = acc, gpsReady = s.gpsReady || acc <= MAX_ACCURACY_M)
            return 0
        }
        if (acc > MAX_ACCURACY_M) {
            _state.value = s.copy(accuracy = acc)
            return 0
        }
        val p = GeoPoint(loc.latitude, loc.longitude)
        val prev = last
        var meters = s.meters
        var points = s.points
        if (prev == null) {
            points = points + p
            last = loc
        } else {
            val d = prev.distanceTo(loc).toDouble()
            val dt = ((loc.elapsedRealtimeNanos - prev.elapsedRealtimeNanos) / 1e9).coerceAtLeast(0.5)
            // On ignore les petits déplacements dus à l'imprécision du GPS
            if (d < maxOf(MIN_STEP_M, acc * 0.6)) {
                _state.value = s.copy(accuracy = acc, gpsReady = true)
                return 0
            }
            if (d / dt > MAX_SPEED_MS) {
                _state.value = s.copy(accuracy = acc, gpsReady = true)
                return 0
            }
            meters += d
            points = points + p
            last = loc
        }
        val now = SystemClock.elapsedRealtime()
        val t = s.elapsedMs(now)
        val recent = (s.recent + (t to meters)).filter { t - it.first <= 30_000 }
        val km = (meters / 1000).toInt()
        val announce = if (km > s.kmAnnounced) km else 0
        _state.value = s.copy(
            points = points, meters = meters, accuracy = acc, gpsReady = true, recent = recent,
            kmAnnounced = maxOf(s.kmAnnounced, km),
        )
        return announce
    }

    // ---------- Tracé : stockage compact ----------

    /** « lat,lon;lat,lon;… » avec 5 décimales (~1 m), au plus 1500 points. */
    fun encode(points: List<GeoPoint>): String {
        if (points.isEmpty()) return ""
        val step = ((points.size + 1499) / 1500).coerceAtLeast(1)
        val kept = points.filterIndexed { i, _ -> i % step == 0 || i == points.lastIndex }
        return kept.joinToString(";") { "%.5f,%.5f".format(java.util.Locale.US, it.lat, it.lon) }
    }

    fun decode(track: String): List<GeoPoint> =
        if (track.isBlank()) emptyList()
        else track.split(';').mapNotNull { pair ->
            val parts = pair.split(',')
            val lat = parts.getOrNull(0)?.toDoubleOrNull()
            val lon = parts.getOrNull(1)?.toDoubleOrNull()
            if (lat != null && lon != null) GeoPoint(lat, lon) else null
        }

    // ---------- Formats ----------

    fun duration(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun pace(secPerKm: Int): String =
        if (secPerKm <= 0 || secPerKm > 59 * 60) "--'--\"" else "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)

    fun km(km: Double): String = "%.2f".format(km).replace('.', ',')
}
