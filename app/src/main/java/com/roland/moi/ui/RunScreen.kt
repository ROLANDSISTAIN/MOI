package com.roland.moi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.graphics.Paint
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Run
import com.roland.moi.run.GeoPoint
import com.roland.moi.run.RunState
import com.roland.moi.run.RunTracker
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.util.GeoPoint as OsmPoint

/** Poids pour estimer les calories : dernière pesée, sinon 75 kg. */
private fun weightKg(d: AppData) = d.bodyLogs.lastOrNull { it.weight > 0 }?.weight ?: 75.0

fun kcal(d: AppData, km: Double) = (weightKg(d) * km).toInt()

@Composable
fun RunScreen(d: AppData, nav: Nav) {
    val s by RunTracker.state.collectAsState()
    val ctx = LocalContext.current

    Page("Course", onBack = { nav.back() }) {
        when (s.status) {
            RunState.Status.IDLE -> RunStart(d, ctx)
            RunState.Status.RUNNING, RunState.Status.PAUSED -> RunLive(s, ctx)
            RunState.Status.FINISHED -> RunSummary(d, s, ctx, onSaved = { nav.back() })
        }
    }
}

@Composable
private fun RunStart(d: AppData, ctx: Context) {
    var hasPerm by remember { mutableStateOf(hasLocation(ctx)) }
    var gpsOn by remember { mutableStateOf(gpsEnabled(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPerm = res[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasPerm && gpsEnabled(ctx)) RunTracker.start(ctx)
    }
    LaunchedEffect(Unit) { while (true) { delay(2000); gpsOn = gpsEnabled(ctx); hasPerm = hasLocation(ctx) } }

    val courses = d.runs.filter { it.kind == Run.KIND_COURSE }
    val monthStart = Dates.startOfMonth()
    val weekStart = Dates.startOfWeek()

    HeroCard {
        SectionLabel("Prêt à courir")
        Spacer(Modifier.height(6.dp))
        Text("Lance le chrono, range ton téléphone.", color = TextMain, fontSize = 20.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Distance, temps et allure par GPS, sans connexion. Une voix t'annonce chaque kilomètre.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(150.dp).clip(CircleShape).background(Accent).border(6.dp, Accent.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        when {
                            !hasLocation(ctx) -> ask.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            !gpsEnabled(ctx) -> ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            else -> RunTracker.start(ctx)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = OnAccent, modifier = Modifier.size(44.dp))
                    Text("DÉMARRER", color = OnAccent, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    if (hasPerm && gpsOn) GpsProbe()

    if (!hasPerm) InfoRow(Icons.Outlined.GpsNotFixed, "Autorise la localisation", "Choisis « Pendant l'utilisation de l'appli ». Appuie sur Démarrer pour la demander.", Neon)
    else if (!gpsOn) InfoRow(Icons.Outlined.GpsNotFixed, "Active la localisation", "Le GPS du téléphone est coupé. Appuie sur Démarrer pour ouvrir le réglage.", Neon)

    ListRow(Icons.Outlined.BatteryAlert, "Pas d'économie de batterie pour MOI", "Sinon Android peut couper le GPS en pleine course. Appuie pour régler.", tint = AccentSoft) {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(RunTracker.km(courses.filter { it.date >= weekStart }.sumOf { it.km }), "km cette semaine", Modifier.weight(1f))
        StatTile(RunTracker.km(courses.filter { it.date >= monthStart }.sumOf { it.km }), "km ce mois", Modifier.weight(1f))
        StatTile(courses.maxOfOrNull { it.km }?.let { RunTracker.km(it) } ?: "0", "record (km)", Modifier.weight(1f))
    }

    Card {
        SectionLabel("Pour bien commencer")
        Spacer(Modifier.height(8.dp))
        Tip("Attends 1 minute dehors avant de partir : le GPS a besoin de voir le ciel.")
        Tip("Débutant : 1 min de course lente, 2 min de marche, pendant 20 min.")
        Tip("Va assez doucement pour pouvoir parler. Bois de l'eau avant de partir.")
    }
}

@Composable
private fun RunLive(s: RunState, ctx: Context) {
    var confirmStop by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) { while (true) { delay(500); now = android.os.SystemClock.elapsedRealtime() } }
    val sec = s.elapsedMs(now) / 1000
    val paused = s.status == RunState.Status.PAUSED

    if (s.error.isNotBlank()) {
        InfoRow(Icons.Outlined.GpsNotFixed, "Problème de suivi", s.error, Neon)
    } else if (!s.gpsReady) {
        val details = buildString {
            append("${s.satsSeen} satellites vus, ${s.satsUsed} utilisés")
            if (s.lastFixAt > 0 && s.accuracy < 900) append(" · précision ±${s.accuracy.toInt()} m")
        }
        InfoRow(Icons.Outlined.GpsNotFixed, "Recherche du signal GPS…", "$details. Reste à découvert, loin des murs. La distance démarre dès que la précision passe sous ${RunTracker.MAX_ACCURACY_M.toInt()} m.", AccentSoft)
    }

    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (paused) TextDim else Neon))
            Spacer(Modifier.width(8.dp))
            SectionLabel(if (paused) "En pause" else "Course en cours")
            Spacer(Modifier.weight(1f))
            if (s.gpsReady) Icon(Icons.Outlined.GpsFixed, null, tint = Success, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            RunTracker.duration(sec), color = TextMain, fontSize = 64.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, lineHeight = 68.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            BigStat(RunTracker.km(s.km), "km", Modifier.weight(1f))
            BigStat(RunTracker.pace(s.avgPace(now)), "allure moy.", Modifier.weight(1f))
            BigStat(RunTracker.pace(s.currentPace()), "allure actuelle", Modifier.weight(1f))
        }
    }

    RouteMap(s.points, live = true, modifier = Modifier.fillMaxWidth().height(260.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ControlButton(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, if (paused) "Reprendre" else "Pause", Accent, Modifier.weight(1f)) {
            if (paused) RunTracker.resume() else RunTracker.pause()
        }
        ControlButton(Icons.Outlined.Stop, "Terminer", Neon, Modifier.weight(1f)) { confirmStop = true }
    }
    Text("Tu peux éteindre l'écran : le suivi continue. Une notification reste affichée pendant la course.",
        color = TextDim, fontSize = 12.sp, lineHeight = 16.sp)

    if (confirmStop) ConfirmDialog("Terminer la course ?", "Tu pourras ensuite l'enregistrer.", "Terminer", { confirmStop = false }) {
        RunTracker.finish(ctx)
    }
}

@Composable
private fun RunSummary(d: AppData, s: RunState, ctx: Context, onSaved: () -> Unit) {
    var feeling by remember { mutableIntStateOf(4) }
    var note by remember { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val sec = s.activeMs / 1000

    HeroCard {
        SectionLabel("Course terminée")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(RunTracker.km(s.km), color = TextMain, fontSize = 56.sp, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = 58.sp)
            Spacer(Modifier.width(6.dp))
            Text("km", color = AccentSoft, fontSize = 20.sp, fontFamily = Display, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            BigStat(RunTracker.duration(sec), "temps", Modifier.weight(1f))
            BigStat(RunTracker.pace(if (s.km > 0.05) ((sec) / s.km).toInt() else 0), "allure /km", Modifier.weight(1f))
            BigStat("${kcal(d, s.km)}", "kcal env.", Modifier.weight(1f))
        }
    }
    if (s.points.size > 1) RouteMap(s.points, live = false, modifier = Modifier.fillMaxWidth().height(240.dp))

    Card {
        SectionLabel("Comment tu te sens ?")
        Spacer(Modifier.height(10.dp))
        MoodPicker(feeling) { feeling = it }
        Spacer(Modifier.height(12.dp))
        Field(note, { note = it }, "Note (facultatif)")
    }
    PrimaryButton("Enregistrer la course", Modifier.fillMaxWidth()) { RunTracker.save(feeling, note); onSaved() }
    GhostButton("Supprimer cette course", Modifier.fillMaxWidth(), color = TextDim) { confirmDiscard = true }

    if (confirmDiscard) ConfirmDialog("Supprimer ?", "Cette course ne sera pas enregistrée.", "Supprimer", { confirmDiscard = false }) {
        RunTracker.discard(ctx)
    }
}

/** Détail d'une course enregistrée. */
@Composable
fun RunViewScreen(d: AppData, id: String, nav: Nav) {
    val r = d.runs.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    Page("Course", onBack = { nav.back() }) {
        if (r == null) { EmptyState("Cette course n'existe plus."); return@Page }
        val sec = if (r.seconds > 0) r.seconds else r.minutes * 60
        val pts = remember(r.track) { RunTracker.decode(r.track) }
        Text(Dates.longDate(r.date), color = TextDim, fontSize = 13.sp)
        HeroCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(RunTracker.km(r.km), color = TextMain, fontSize = 52.sp, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = 54.sp)
                Spacer(Modifier.width(6.dp))
                Text("km", color = AccentSoft, fontSize = 18.sp, fontFamily = Display, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                BigStat(RunTracker.duration(sec.toLong()), "temps", Modifier.weight(1f))
                BigStat(RunTracker.pace(if (r.km > 0.05) (sec / r.km).toInt() else 0), "allure /km", Modifier.weight(1f))
                BigStat("${kcal(d, r.km)}", "kcal env.", Modifier.weight(1f))
            }
        }
        if (pts.size > 1) RouteMap(pts, live = false, modifier = Modifier.fillMaxWidth().height(280.dp))
        if (r.note.isNotBlank()) Card { Text(r.note, color = TextMain, fontSize = 14.sp) }
        GhostButton("Supprimer cette course", Modifier.fillMaxWidth(), color = TextDim) { confirm = true }
        if (confirm) ConfirmDialog("Supprimer ?", "La course sera effacée de ton historique.", "Supprimer", { confirm = false }) {
            com.roland.moi.data.Store.deleteRun(r.id); nav.back()
        }
    }
}

// ---------- Carte ----------

/**
 * Le tracé de la course sur une carte sombre. Sans connexion, le fond reste noir
 * mais le tracé s'affiche quand même.
 */
@Composable
fun RouteMap(points: List<GeoPoint>, live: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = java.io.File(ctx.cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid/tiles")
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            overlayManager.tilesOverlay.loadingBackgroundColor = AColor.BLACK
            overlayManager.tilesOverlay.loadingLineColor = Surface2.toArgb()
            setBackgroundColor(AColor.BLACK)
            controller.setZoom(16.5)
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
        }
    }
    val line = remember {
        Polyline(map).apply {
            outlinePaint.color = Accent.toArgb()
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }.also { map.overlays.add(it) }
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose { map.onPause(); map.onDetach() }
    }
    Box(modifier.clip(RoundedCornerShape(22.dp)).border(1.dp, Line, RoundedCornerShape(22.dp))) {
        AndroidView(factory = { map }, modifier = Modifier.matchParentSize()) { m ->
            val geo = points.map { OsmPoint(it.lat, it.lon) }
            line.setPoints(geo)
            if (geo.isNotEmpty()) {
                if (live || geo.size < 2) {
                    m.controller.setCenter(geo.last())
                } else {
                    m.post {
                        runCatching {
                            m.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(geo).increaseByScale(1.3f), false)
                        }
                    }
                }
            }
            m.invalidate()
        }
        if (points.isEmpty()) {
            Text("Le tracé apparaîtra ici", color = TextDim, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ---------- Petits composants ----------

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextMain, fontSize = 22.sp, fontFamily = Display, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(color).clickable(onClick = onClick).padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = OnAccent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = OnAccent, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, tint: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun Tip(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = AccentSoft, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

private fun hasLocation(ctx: Context) =
    ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun gpsEnabled(ctx: Context) =
    (ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)

/**
 * Indicateur de signal avant le départ : écoute le GPS pendant que l'écran est ouvert
 * pour que le téléphone trouve sa position avant même d'appuyer sur Démarrer.
 */
@android.annotation.SuppressLint("MissingPermission")
@Composable
private fun GpsProbe() {
    val ctx = LocalContext.current
    var acc by remember { mutableStateOf(-1f) }
    var used by remember { mutableIntStateOf(0) }
    var seen by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = android.location.LocationListener { l -> acc = if (l.hasAccuracy()) l.accuracy else 999f }
        val cb = object : android.location.GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
                var u = 0
                for (i in 0 until status.satelliteCount) if (status.usedInFix(i)) u++
                used = u; seen = status.satelliteCount
            }
        }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, android.os.Looper.getMainLooper()) }
        runCatching { lm.registerGnssStatusCallback(cb, android.os.Handler(android.os.Looper.getMainLooper())) }
        onDispose {
            runCatching { lm.removeUpdates(listener) }
            runCatching { lm.unregisterGnssStatusCallback(cb) }
        }
    }
    val good = acc in 0f..RunTracker.MAX_ACCURACY_M
    val (title, color) = when {
        good && acc <= 15f -> "Signal GPS excellent" to Success
        good -> "Signal GPS correct" to Success
        acc > 0 -> "Signal GPS faible" to AccentSoft
        else -> "Recherche du signal GPS…" to AccentSoft
    }
    val text = buildString {
        append("$seen satellites vus, $used utilisés")
        if (acc > 0 && acc < 900) append(" · précision ±${acc.toInt()} m")
        if (!good) append(". Attends dehors, à découvert, que le signal devienne correct avant de démarrer.")
        else append(". Tu peux démarrer.")
    }
    InfoRow(if (good) Icons.Outlined.GpsFixed else Icons.Outlined.GpsNotFixed, title, text, color)
}
