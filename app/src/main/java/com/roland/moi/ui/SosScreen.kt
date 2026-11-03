package com.roland.moi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Quotes
import com.roland.moi.data.Run
import com.roland.moi.data.Store
import kotlinx.coroutines.delay

private enum class SosStep { BREATHE, WORDS, MOVE, DONE }

/** Le bouton « Je suis tenté » : respirer, relire ses mots, bouger. */
@Composable
fun SosScreen(d: AppData, nav: Nav) {
    var step by remember { mutableStateOf(SosStep.BREATHE) }
    LaunchedEffect(Unit) { Store.update { it.copy(sosCount = it.sosCount + 1) } }

    Page("Je suis tenté", onBack = { nav.back() }) {
        when (step) {
            SosStep.BREATHE -> Breathe { step = SosStep.WORDS }
            SosStep.WORDS -> Words(d) { step = SosStep.MOVE }
            SosStep.MOVE -> Move { done ->
                if (done) {
                    Store.saveRun(Run(id = Store.newId(), date = System.currentTimeMillis(), kind = Run.KIND_POMPES, count = 20, sets = 1, feeling = 4, note = "Après une tentation 💪"))
                }
                Store.recordResisted()
                step = SosStep.DONE
            }
            SosStep.DONE -> {
                Spacer(Modifier.height(40.dp))
                Text("👑", fontSize = 56.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("Victoire.", color = Gold, fontSize = 34.sp, fontFamily = Serif, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    "Tu as choisi l'homme que tu veux devenir. Elle compte dans tes victoires.",
                    color = TextMain, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                GoldButton("Retour", Modifier.fillMaxWidth()) { nav.back() }
            }
        }
    }
}

@Composable
private fun Breathe(onDone: () -> Unit) {
    // 4 s inspirer · 4 s retenir · 6 s expirer — 4 cycles (≈ 1 minute)
    val scale = remember { Animatable(0.55f) }
    var label by remember { mutableStateOf("Prépare-toi") }
    var cycle by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        repeat(4) { i ->
            cycle = i + 1
            label = "Inspire"; scale.animateTo(1f, tween(4000, easing = LinearEasing))
            label = "Retiens"; delay(4000)
            label = "Expire"; scale.animateTo(0.55f, tween(6000, easing = LinearEasing))
        }
        label = "C'est bien."
        finished = true
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Respire avec le cercle.", color = TextDim, fontSize = 15.sp)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(260.dp).scale(scale.value).background(Gold.copy(alpha = 0.18f), CircleShape).border(2.dp, Gold, CircleShape))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Gold, fontSize = 30.sp, fontFamily = Serif)
                if (cycle > 0 && !finished) Text("$cycle / 4", color = TextDim, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("La tentation est une vague. Elle monte, puis elle redescend. Toujours.", color = TextMain, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        GoldButton(if (finished) "Continuer" else "Passer", Modifier.fillMaxWidth(), onClick = onDone)
    }
}

@Composable
private fun Words(d: AppData, onNext: () -> Unit) {
    val letter = remember { Quotes.forShield(d) }
    val streakDays = ((System.currentTimeMillis() - d.streakStart) / DAY_MS).toInt()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card {
            SectionLabel("Ce que tu as déjà gagné")
            Spacer(Modifier.height(6.dp))
            Text("$streakDays jour${if (streakDays > 1) "s" else ""} de liberté · ${d.resistCount} victoires", color = Gold, fontSize = 18.sp, fontFamily = Serif)
        }
        if (letter != null) {
            Card {
                SectionLabel(letter.source)
                Spacer(Modifier.height(8.dp))
                Text(letter.text, color = TextMain, fontSize = 17.sp, lineHeight = 25.sp, fontFamily = Serif)
            }
        }
        Card {
            Text(d.shieldMessage, color = TextMain, fontSize = 16.sp, lineHeight = 23.sp)
            if (d.shieldVerse.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(d.shieldVerse, color = Gold, fontSize = 15.sp, fontStyle = FontStyle.Italic)
            }
        }
        if (letter == null) {
            Text("Astuce : écris une « Lettre à moi-même » dans Ma vie. Elle s'affichera ici.", color = TextDim, fontSize = 12.sp)
        }
        GoldButton("Maintenant, bouge", Modifier.fillMaxWidth(), onClick = onNext)
    }
}

@Composable
private fun Move(onFinish: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Text("💪", fontSize = 64.sp)
        Text("20 pompes", color = Gold, fontSize = 40.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pose le téléphone. Fais-les maintenant, même lentement, même en plusieurs fois. " +
                "Ton corps va brûler l'énergie de la tentation.",
            color = TextMain, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        GoldButton("C'est fait ✓", Modifier.fillMaxWidth()) { onFinish(true) }
        Spacer(Modifier.height(10.dp))
        GhostButton("Je ne peux pas maintenant", Modifier.fillMaxWidth(), color = TextDim) { onFinish(false) }
    }
}
