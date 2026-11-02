package com.roland.moi.ui

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Dates
import com.roland.moi.data.Habit
import com.roland.moi.data.Store

@Composable
fun HabitsScreen(d: AppData, nav: Nav) {
    val today = Dates.dayKey()
    val now = System.currentTimeMillis()
    val last7 = (6 downTo 0).map { Dates.addDays(now, -it) }

    Page("Mes habitudes", onBack = null, actions = {
        androidx.compose.material3.TextButton(onClick = { nav.go(Route.HabitsEdit) }) { Text("Modifier", color = Gold) }
    }) {
        val done = d.habits.count { today in it.done }
        Text(
            if (d.habits.isEmpty()) "Ajoute les habitudes que tu veux construire."
            else "Aujourd'hui : $done sur ${d.habits.size}" + if (done == d.habits.size && done > 0) " — journée parfaite 👑" else "",
            color = TextDim, fontSize = 14.sp,
        )
        d.habits.forEach { h ->
            val isDone = today in h.done
            val streak = Dates.streak(h.done)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1)
                    .border(1.dp, if (isDone) Gold else Line, RoundedCornerShape(18.dp))
                    .clickable { Store.toggleHabit(h.id) }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(h.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.name, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "🔥 $streak jour${if (streak > 1) "s" else ""} · record ${Dates.bestStreak(h.done)}",
                            color = TextDim, fontSize = 12.sp,
                        )
                    }
                    CheckBox(isDone)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    last7.forEach { t ->
                        val k = Dates.dayKey(t)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Dates.dayLetter(t), color = TextDim, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(if (k in h.done) Gold else Surface2)
                                    .clickable { Store.toggleHabit(h.id, k) },
                            )
                        }
                    }
                }
            }
        }
        Text("Astuce : tu peux cocher un jour passé en touchant son rond.", color = TextDim, fontSize = 12.sp)
    }
}

private val EMOJIS = listOf("🙏", "📖", "🏃", "💪", "📈", "📚", "💧", "🛏️", "🥗", "🧘", "✍️", "🎯", "🎵", "💰", "🚭", "🌅")

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HabitsEditScreen(d: AppData, nav: Nav) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    var toDelete by remember { mutableStateOf<Habit?>(null) }

    Page("Modifier mes habitudes", onBack = { nav.back() }) {
        d.habits.forEach { h ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface1).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${h.emoji}  ${h.name}", color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { toDelete = h }) { Icon(Icons.Outlined.Delete, "Supprimer", tint = TextDim) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SectionLabel("Nouvelle habitude")
        Field(name, { name = it }, "Nom (ex : Boire 2 L d'eau)")
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EMOJIS.forEach { e -> Chip(e, emoji == e) { emoji = e } }
        }
        GoldButton("Ajouter", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            Store.saveHabit(Habit(Store.newId(), name.trim(), emoji))
            name = ""
        }
    }

    toDelete?.let { h ->
        ConfirmDialog("Supprimer « ${h.name} » ?", "Son historique sera effacé.", "Supprimer", { toDelete = null }) {
            Store.deleteHabit(h.id)
        }
    }
}
