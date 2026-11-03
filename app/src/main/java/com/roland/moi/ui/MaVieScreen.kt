package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Section

private data class Tile(val emoji: String, val title: String, val count: String, val route: Route)

@Composable
fun MaVieScreen(d: AppData, nav: Nav) {
    val tiles = Section.entries.map { s ->
        val n = d.entries.count { it.section == s.key }
        Tile(s.emoji, s.title, if (n == 0) "" else "$n", Route.Section(s.key))
    } + listOf(
        Tile("📓", "Mon journal", if (d.days.isEmpty()) "" else "${d.days.size}", Route.JournalList),
        Tile("✨", "Mon année", "${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}", Route.Year(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))),
        Tile("📊", "Mes bilans", if (d.reviews.isEmpty()) "" else "${d.reviews.size}", Route.Reviews),
        Tile("🛡️", "Liberté", "${((System.currentTimeMillis() - d.streakStart) / DAY_MS)} j", Route.Liberte),
    )

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ma vie", color = Gold, fontSize = 30.sp, fontFamily = Serif)
        Text("Tout ce qui fait de toi, toi.", color = TextDim, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 110.dp).clip(RoundedCornerShape(18.dp))
                            .background(Surface1).border(1.dp, Line, RoundedCornerShape(18.dp))
                            .clickable { nav.go(t.route) }.padding(16.dp),
                    ) {
                        Row {
                            Text(t.emoji, fontSize = 28.sp, modifier = Modifier.weight(1f))
                            Text(t.count, color = Gold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(t.title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
