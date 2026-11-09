package com.roland.moi.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.identity.Identity
import com.roland.moi.backup.DriveBackup
import com.roland.moi.data.PinManager
import com.roland.moi.data.Store
import kotlinx.coroutines.launch

private enum class Pending { NONE, BACKUP, RESTORE }

@Composable
fun SettingsScreen(d: com.roland.moi.data.AppData, nav: Nav, onChangePin: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(Pending.NONE) }
    var confirmRestore by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf(PinManager.biometricEnabled(ctx)) }
    var pickQuote by remember { mutableStateOf(false) }

    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    suspend fun withToken(token: String, what: Pending) {
        DriveBackup.setConnected(ctx, true)
        if (what == Pending.RESTORE) {
            try {
                val json = DriveBackup.download(token)
                if (json == null) toast("Aucune sauvegarde trouvée sur ce compte Google")
                else {
                    Store.replaceAll(json)
                    val n = DriveBackup.restorePhotos(ctx, token)
                    toast("Tout est revenu ✓" + if (n > 0) " ($n photos)" else "")
                }
            } catch (t: Throwable) { toast("Erreur : ${t.message}") }
        } else {
            val err = DriveBackup.upload(ctx, token, Store.exportJson())
            toast(err?.let { "Erreur : $it" } ?: "Sauvegardé sur Google Drive ✓")
        }
        busy = false; refresh++
    }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        val what = pending
        scope.launch {
            try {
                val r = Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(res.data)
                val tok = r.accessToken
                if (tok != null) withToken(tok, what) else { busy = false; toast("Connexion annulée") }
            } catch (t: Throwable) { busy = false; toast("Connexion annulée") }
        }
    }

    fun run(what: Pending) {
        busy = true; pending = what
        scope.launch {
            when (val a = DriveBackup.authorize(ctx)) {
                is DriveBackup.Auth.Token -> withToken(a.token, what)
                is DriveBackup.Auth.NeedsConsent -> consent.launch(IntentSenderRequest.Builder(a.intent.intentSender).build())
                is DriveBackup.Auth.Error -> { busy = false; toast("Erreur Google : ${a.message}") }
            }
        }
    }

    Page("Réglages", onBack = { nav.back() }) {
        ListRow("🛡️", "Le Bouclier", "Message, verset, délai, navigateurs VPN") { nav.go(Route.ShieldSettings) }
        ListRow(
            "👑", "Protection anti-désinstallation",
            if (d.protectOn) "Active" else "Désactivée",
        ) { nav.go(Route.Protection) }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Parole du jour")
                    Spacer(Modifier.height(4.dp))
                    Text(if (d.quoteOn) "Notification chaque matin à ${com.roland.moi.data.Dates.time(d.quoteTime)}" else "Désactivée",
                        color = TextMain, fontSize = 14.sp)
                }
                androidx.compose.material3.TextButton(onClick = { pickQuote = true }) { Text("Heure", color = Gold) }
                Switch(
                    checked = d.quoteOn,
                    onCheckedChange = { on -> Store.update { it.copy(quoteOn = on) }; com.roland.moi.notify.Reminders.scheduleAll(ctx) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Gold),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("Elle te rappelle aussi tes capsules à ouvrir et ton bilan du dimanche.", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text("Ce qu'elle peut te donner (un type différent chaque jour) :", color = TextMain, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            com.roland.moi.data.Sagesse.ALL.chunked(2).forEach { row ->
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        val on = k in d.quoteKinds
                        Chip(com.roland.moi.data.Sagesse.label(k), on) {
                            Store.update { it.copy(quoteKinds = if (on) it.quoteKinds - k else it.quoteKinds + k) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (d.quoteKinds.none { it in com.roland.moi.data.Sagesse.ALL }) {
                Text("Rien de coché : MOI te donnera des versets.", color = TextDim, fontSize = 12.sp)
            }
        }

        Card {
            SectionLabel("Sauvegarde Google Drive")
            Spacer(Modifier.height(8.dp))
            val connected = remember(refresh) { DriveBackup.isConnected(ctx) }
            val last = remember(refresh) { DriveBackup.lastBackup(ctx) }
            val err = remember(refresh) { DriveBackup.lastError(ctx) }
            Text(
                when {
                    !connected -> "Pas encore connectée. Tes données sont sauvegardées automatiquement dans un dossier caché de ton Google Drive, que seule MOI peut lire."
                    last > 0 -> "Automatique. Dernière sauvegarde : ${formatDate(last)}"
                    else -> "Connectée. Première sauvegarde en cours…"
                },
                color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
            )
            if (err != null && connected) {
                Spacer(Modifier.height(6.dp))
                Text("Dernière erreur : $err", color = Danger, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            GoldButton(
                if (busy) "Patiente…" else if (connected) "Sauvegarder maintenant" else "Connecter mon compte Google",
                Modifier.fillMaxWidth(), enabled = !busy,
            ) { run(Pending.BACKUP) }
            Spacer(Modifier.height(8.dp))
            GhostButton("Restaurer depuis Google Drive", Modifier.fillMaxWidth()) { confirmRestore = true }
        }

        Card {
            SectionLabel("Sécurité")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Déverrouiller avec l'empreinte", color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = bio,
                    onCheckedChange = { bio = it; PinManager.setBiometric(ctx, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Gold),
                )
            }
            Spacer(Modifier.height(8.dp))
            GhostButton("Changer mon code", Modifier.fillMaxWidth(), onClick = onChangePin)
            Spacer(Modifier.height(8.dp))
            Text(
                "MOI se verrouille dès que tu la quittes plus de 30 secondes. Les captures d'écran sont bloquées.",
                color = TextDim, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
        Text("MOI · version ${appVersion(ctx)}", color = TextDim, fontSize = 11.sp)
    }

    if (pickQuote) TimeDialog(d.quoteTime, { pickQuote = false }) { m ->
        Store.update { it.copy(quoteTime = m, quoteOn = true) }; com.roland.moi.notify.Reminders.scheduleAll(ctx)
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = Surface1,
            title = { Text("Restaurer ?", color = Gold) },
            text = { Text("Les données de ta dernière sauvegarde Google Drive vont remplacer celles de ce téléphone.", color = TextMain) },
            confirmButton = { TextButton(onClick = { confirmRestore = false; run(Pending.RESTORE) }) { Text("Restaurer", color = Gold) } },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Annuler", color = TextDim) } },
        )
    }
}

private fun appVersion(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1"
