package com.roland.moi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.BodyLog
import com.roland.moi.data.Dates
import com.roland.moi.data.Store

fun fmtNum(v: Double): String = if (v == Math.floor(v)) "${v.toInt()}" else "%.1f".format(v).replace('.', ',')

private fun parseNum(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

/** Programme « perte de poids » : la journée type (repas + séance de midi), l'assiette et les mesures. */
@Composable
fun VentreScreen(d: AppData, nav: Nav) {
    var measuring by remember { mutableStateOf(false) }

    Page("Perte de poids", onBack = { nav.back() }) {
        IntroCard()
        DayPlanCard()
        SaladCard()
        LunchCard()
        RenfoCard()
        MeasuresCard(d) { measuring = true }
        HabitsCard()
    }

    if (measuring) MeasureDialog(d) { measuring = false }
}

@Composable
private fun IntroCard() {
    Card {
        SectionLabel("Le programme")
        Spacer(Modifier.height(8.dp))
        Text("Perdre du gras, surtout au ventre", color = Gold, fontSize = 22.sp, fontFamily = Serif)
        Spacer(Modifier.height(8.dp))
        Text(
            "On ne peut pas perdre uniquement le ventre : on perd du gras partout, et le ventre diminue avec le reste. " +
                "L'assiette fait l'essentiel du résultat. Objectif réaliste : environ 0,5 kg par semaine.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
        )
    }
}

/** La journée type, chaque jour. */
@Composable
private fun DayPlanCard() {
    Card {
        SectionLabel("Ma journée type")
        Spacer(Modifier.height(10.dp))
        Slot("8 h", "Petit-déjeuner", "La salade d'œufs, avocat et légumes")
        Slot("12 h", "Séance dans la chambre", "Pompes, squats, gainage, puis la douche")
        Slot("13 h 30", "Déjeuner", "Riz, pâte ou spaghetti avec sauce : ¼ d'assiette de féculent")
        Slot("16 h 30", "Si j'ai faim", "Un fruit : orange, papaye, ananas")
        Slot("Après 19 h", "Plus rien à manger", "Seulement de l'eau")
        Spacer(Modifier.height(6.dp))
        Text("Après 10 h, ne mange plus avant la séance : on s'entraîne mieux le ventre léger. Bois de l'eau avant et après.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun Slot(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(time, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(84.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
}

/** Le plat du matin. */
@Composable
private fun SaladCard() {
    Card {
        SectionLabel("Le plat du matin")
        Spacer(Modifier.height(8.dp))
        Text("Salade d'œufs, avocat et légumes", color = Gold, fontSize = 20.sp, fontFamily = Serif)
        Spacer(Modifier.height(10.dp))
        Text("Pour 1 personne", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Bullet("2 œufs")
        Bullet("2 tomates")
        Bullet("1 concombre")
        Bullet("1 petit oignon")
        Bullet("½ avocat")
        Bullet("1 citron, un peu de sel")
        Spacer(Modifier.height(8.dp))
        Text("Préparation", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Bullet("1. Fais bouillir les œufs 10 min, passe-les à l'eau froide et écale-les.")
        Bullet("2. Coupe les tomates, le concombre, l'oignon et l'avocat dans un bol.")
        Bullet("3. Ajoute les œufs coupés, le jus de citron et le sel. Mélange.")
        Spacer(Modifier.height(6.dp))
        Text("Pas d'avocat ? Mets du chou ou des carottes râpées. Pas de pain ni de gari avec.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/** Le plat de midi. */
@Composable
private fun LunchCard() {
    Card {
        SectionLabel("Le plat de midi")
        Spacer(Modifier.height(8.dp))
        Text("Riz, pâte ou spaghetti avec sauce", color = Gold, fontSize = 20.sp, fontFamily = Serif)
        Spacer(Modifier.height(8.dp))
        Text("Le riz, la pâte et les spaghetti ne sont pas interdits : le problème, c'est quand il n'y a que ça.",
            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Bullet("¼ d'assiette de riz, pâte ou spaghetti : une louche, la taille d'un poing fermé")
        Bullet("¼ de protéines : poisson, poulet, œufs, haricots, fromage de soja")
        Bullet("½ de sauce et de légumes : gombo, adémè, épinards, tomate, chou…")
        Bullet("Peu d'huile dans la sauce, et on ne se ressert pas")
        Spacer(Modifier.height(6.dp))
        Text("Coche « Assiette équilibrée 🥗 » dans tes habitudes quand c'est fait.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun RenfoCard() {
    Card {
        SectionLabel("Ma séance de midi · 15 min")
        Spacer(Modifier.height(8.dp))
        Bullet("💪 Pompes : ton programme pompes du jour")
        Bullet("🦵 Squats : 3 × 15")
        Bullet("🧱 Planche (gainage) : 3 × 30 s, ventre serré")
        Spacer(Modifier.height(6.dp))
        Text("Garder ses muscles aide à brûler plus, même au repos. Et marche le plus possible au quotidien.",
            color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun MeasuresCard(d: AppData, onAdd: () -> Unit) {
    val logs = d.bodyLogs
    val w = logs.filter { it.weight > 0 }
    val t = logs.filter { it.waist > 0 }
    Card {
        SectionLabel("Mes mesures")
        Spacer(Modifier.height(8.dp))
        if (logs.isEmpty()) {
            Text("Note ton poids et ton tour de taille (au niveau du nombril, le matin à jeun). " +
                "Le tour de taille est plus parlant que la balance.", color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Measure("Poids", w.lastOrNull()?.weight, w.firstOrNull()?.weight, "kg", Modifier.weight(1f))
                Measure("Tour de taille", t.lastOrNull()?.waist, t.firstOrNull()?.waist, "cm", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            logs.takeLast(8).reversed().forEach { b ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(Dates.shortDate(Dates.parseDay(b.day)), color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(
                        listOfNotNull(
                            b.weight.takeIf { it > 0 }?.let { fmtNum(it) + " kg" },
                            b.waist.takeIf { it > 0 }?.let { fmtNum(it) + " cm" },
                        ).joinToString(" · "),
                        color = TextMain, fontSize = 13.sp,
                    )
                }
            }
            val last = logs.last().day
            val days = ((Dates.startOfDay() - Dates.parseDay(last)) / DAY_MS).toInt()
            if (days >= 7) {
                Spacer(Modifier.height(6.dp))
                Text("Dernière mesure il y a $days jours : c'est le moment.", color = Gold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        GoldButton("Noter mes mesures", Modifier.fillMaxWidth(), onClick = onAdd)
    }
}

@Composable
private fun Measure(label: String, now: Double?, start: Double?, unit: String, modifier: Modifier) {
    Column(modifier) {
        Text(now?.let { fmtNum(it) + " $unit" } ?: "—", color = Gold, fontSize = 22.sp, fontFamily = Serif, fontWeight = FontWeight.Bold)
        Text(label, color = TextDim, fontSize = 12.sp)
        if (now != null && start != null && now != start) {
            val diff = now - start
            Text(
                (if (diff < 0) "▼ " else "▲ ") + fmtNum(Math.abs(Math.round(diff * 10) / 10.0)) + " $unit depuis le début",
                color = if (diff < 0) Gold else Danger, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun HabitsCard() {
    Card {
        SectionLabel("Les petites habitudes")
        Spacer(Modifier.height(8.dp))
        Bullet("💧 De l'eau (fraîche ou non) à la place des jus et sodas")
        Bullet("🫗 Moins d'huile dans les sauces")
                Bullet("😴 Dormir 7 à 8 h : le manque de sommeil favorise le gras du ventre")
        Bullet("🚶 Marcher le plus possible")
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("•", color = Gold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MeasureDialog(d: AppData, onClose: () -> Unit) {
    val today = Dates.dayKey()
    val old = d.bodyLogs.firstOrNull { it.day == today }
    var weight by remember { mutableStateOf(old?.weight?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    var waist by remember { mutableStateOf(old?.waist?.takeIf { it > 0 }?.let(::fmtNum) ?: "") }
    val w = parseNum(weight)
    val t = parseNum(waist)
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Surface1,
        title = { Text("Mes mesures du jour", color = Gold, fontFamily = Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Poids (kg)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = num, singleLine = true)
                OutlinedTextField(waist, { waist = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Tour de taille (cm)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = num, singleLine = true)
                Text("Mesure le tour de taille au niveau du nombril, le matin, avant de manger.", color = TextDim, fontSize = 12.sp)
                if (old != null) {
                    TextButton(onClick = { Store.deleteBody(today); onClose() }) { Text("Supprimer la mesure du jour", color = Danger) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = w > 0 || t > 0, onClick = {
                Store.saveBody(BodyLog(today, w, t)); onClose()
            }) { Text("Enregistrer", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annuler", color = TextDim) } },
    )
}
