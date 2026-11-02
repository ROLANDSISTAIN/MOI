package com.roland.moi.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Store
import com.roland.moi.shield.Protection
import com.roland.moi.shield.ShieldService
import kotlinx.coroutines.delay

@Composable
fun ProtectionScreen(d: AppData, nav: Nav) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var phrase by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(Protection.isAdmin(ctx)) }
    LaunchedEffect(now / 3000) { isAdmin = Protection.isAdmin(ctx) }
    val unlocked = now < d.unlockUntil
    val shieldOn = ShieldService.isEnabled(ctx)

    Page("Protection", onBack = { nav.back() }) {
        Text(
            "Quand la protection est active, impossible de désinstaller MOI ou de désactiver le Bouclier sur un coup de tête. " +
                "Pour la retirer, il faut attendre 24 h après en avoir fait la demande, ou recopier ta phrase d'engagement.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
        )

        if (!d.protectOn) {
            if (!shieldOn) {
                Text("⚠️ Active d'abord le Bouclier : c'est lui qui surveille les tentatives de désinstallation.", color = Danger, fontSize = 13.sp)
            }
            GoldButton("Activer la protection", Modifier.fillMaxWidth(), enabled = shieldOn) {
                // 2 minutes de grâce pour autoriser MOI comme administrateur sans que le Bouclier ne bloque l'écran
                Store.update { it.copy(protectOn = true, unlockUntil = System.currentTimeMillis() + 120_000, unlockRequestedAt = 0L) }
                if (!Protection.isAdmin(ctx)) ctx.startActivity(Protection.adminIntent(ctx))
            }
            return@Page
        }

        Card {
            SectionLabel(if (unlocked) "🔓 Déverrouillée" else "👑 Active")
            Spacer(Modifier.height(8.dp))
            Text(
                if (unlocked) "Tu as ${((d.unlockUntil - now) / 60000) + 1} minutes pour faire tes modifications. Ensuite la protection se réactive."
                else "MOI et le Bouclier sont protégés.",
                color = TextMain, fontSize = 14.sp,
            )
            if (!isAdmin) {
                Spacer(Modifier.height(10.dp))
                Text("Renforce-la : autorise MOI comme administrateur de l'appareil.", color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                GhostButton("Renforcer", Modifier.fillMaxWidth()) {
                    Store.update { it.copy(unlockUntil = maxOf(it.unlockUntil, System.currentTimeMillis() + 120_000)) }
                    ctx.startActivity(Protection.adminIntent(ctx))
                }
            }
        }

        if (unlocked) {
            GhostButton("Retirer complètement la protection", Modifier.fillMaxWidth(), color = Danger) {
                Protection.removeAdmin(ctx)
                Store.update { it.copy(protectOn = false, unlockUntil = 0L) }
                Toast.makeText(ctx, "Protection retirée", Toast.LENGTH_SHORT).show()
            }
            GoldButton("Réactiver tout de suite", Modifier.fillMaxWidth()) { Store.update { it.copy(unlockUntil = 0L) } }
            return@Page
        }

        // ---- Option 1 : attendre 24 h ----
        Card {
            SectionLabel("Option 1 · Attendre 24 h")
            Spacer(Modifier.height(8.dp))
            if (d.unlockRequestedAt == 0L) {
                Text("La tentation passe. Si dans 24 h tu le veux toujours, tu pourras déverrouiller.", color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                GhostButton("Faire la demande", Modifier.fillMaxWidth()) { Protection.requestUnlock() }
            } else {
                val left = (d.unlockRequestedAt + Protection.WAIT_MS - now).coerceAtLeast(0)
                if (left > 0) {
                    val h = left / 3_600_000; val m = (left / 60_000) % 60
                    Text("Encore ${h} h ${m} min.", color = Gold, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    GhostButton("Annuler la demande", Modifier.fillMaxWidth(), color = TextDim) { Protection.cancelRequest() }
                } else {
                    GoldButton("Déverrouiller pour 15 minutes", Modifier.fillMaxWidth()) { Protection.openWindow() }
                }
            }
        }

        // ---- Option 2 : la phrase ----
        Card {
            SectionLabel("Option 2 · Ma phrase d'engagement")
            Spacer(Modifier.height(8.dp))
            Text("Recopie exactement :", color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("« ${Protection.PHRASE} »", color = Gold, fontSize = 14.sp, lineHeight = 20.sp, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(10.dp))
            Field(phrase, { phrase = it }, "Recopie la phrase", minLines = 3)
            Spacer(Modifier.height(10.dp))
            GoldButton("Déverrouiller pour 15 minutes", Modifier.fillMaxWidth(), enabled = Protection.phraseMatches(phrase)) {
                Protection.openWindow(); phrase = ""
            }
        }
    }
}
