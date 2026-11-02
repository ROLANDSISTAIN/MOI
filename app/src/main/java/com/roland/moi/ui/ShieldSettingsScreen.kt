package com.roland.moi.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Store
import com.roland.moi.shield.ShieldActivity

@Composable
fun ShieldSettingsScreen(d: AppData, shieldOn: Boolean, nav: Nav) {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf(d.shieldMessage) }
    var verse by remember { mutableStateOf(d.shieldVerse) }
    var delay by remember { mutableFloatStateOf(d.shieldDelaySec.toFloat()) }
    val changed = message != d.shieldMessage || verse != d.shieldVerse || delay.toInt() != d.shieldDelaySec

    Page("Le Bouclier", onBack = { nav.back() }) {

        Card {
            SectionLabel("État")
            Spacer(Modifier.height(8.dp))
            Text(
                if (shieldOn) "Actif. Il te protège dans tous les navigateurs." else "Désactivé. Active « MOI — Bouclier » dans Accessibilité.",
                color = if (shieldOn) TextMain else Danger, fontSize = 14.sp,
            )
            if (!shieldOn) {
                Spacer(Modifier.height(10.dp))
                GoldButton("Ouvrir Accessibilité", Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        Card {
            SectionLabel("Ce que je verrai")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                label = { Text("Mon message") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = verse, onValueChange = { verse = it },
                label = { Text("Mon verset") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Spacer(Modifier.height(14.dp))
            Text("Délai avant de pouvoir fermer : ${delay.toInt()} secondes", color = TextMain, fontSize = 14.sp)
            Slider(
                value = delay, onValueChange = { delay = it }, valueRange = 10f..120f, steps = 10,
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Surface2),
            )
            Spacer(Modifier.height(6.dp))
            GoldButton("Enregistrer", Modifier.fillMaxWidth(), enabled = changed) {
                Store.update { it.copy(shieldMessage = message.trim(), shieldVerse = verse.trim(), shieldDelaySec = delay.toInt()) }
                Toast.makeText(ctx, "Enregistré", Toast.LENGTH_SHORT).show()
            }
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Navigateurs avec VPN")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Opera, Aloha, Tor… peuvent contourner le DNS. Si activé, le Bouclier s'affiche dès qu'on les ouvre.",
                        color = TextDim, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
                Switch(
                    checked = d.blockVpnBrowsers,
                    onCheckedChange = { on -> Store.update { it.copy(blockVpnBrowsers = on) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Gold),
                )
            }
        }

        GhostButton("Voir un aperçu du Bouclier", Modifier.fillMaxWidth()) {
            ctx.startActivity(Intent(ctx, ShieldActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Spacer(Modifier.height(8.dp))
    }
}
