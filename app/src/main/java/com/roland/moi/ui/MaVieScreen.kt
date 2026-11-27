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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.AppData
import com.roland.moi.data.Section

/**
 * Les rubriques de « Ma vie » affichées, dans l'ordre.
 * Les autres (identité, peurs, désirs, gens, souvenirs, capsule, journal, bilans, année) sont masquées :
 * leurs données restent enregistrées et sauvegardées.
 */
val VISIBLE_SECTIONS = listOf(
    Section.VISION, Section.BUTS, Section.FOI, Section.DECISIONS,
    Section.LECONS, Section.REALISATIONS, Section.LETTRES, Section.LIVRES,
)

fun sectionIcon(s: Section): ImageVector = when (s) {
    Section.VISION -> Icons.Outlined.Visibility
    Section.BUTS -> Icons.Outlined.TrackChanges
    Section.FOI -> Icons.Outlined.VolunteerActivism
    Section.DECISIONS -> Icons.Outlined.Balance
    Section.LECONS -> Icons.Outlined.Lightbulb
    Section.REALISATIONS -> Icons.Outlined.EmojiEvents
    Section.LETTRES -> Icons.Outlined.MailOutline
    Section.LIVRES -> Icons.Outlined.MenuBook
    else -> Icons.Outlined.Notes
}

@Composable
fun MaVieScreen(d: AppData, nav: Nav) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TabHeader("Ma vie", "Ce qui compte vraiment")
        Spacer(Modifier.height(2.dp))
        VISIBLE_SECTIONS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { s ->
                    val n = d.entries.count { it.section == s.key }
                    Column(
                        Modifier.weight(1f).heightIn(min = 128.dp).clip(RoundedCornerShape(22.dp))
                            .background(Surface1).border(1.dp, Line, RoundedCornerShape(22.dp))
                            .clickable { nav.go(Route.Section(s.key)) }.padding(16.dp),
                    ) {
                        Row {
                            IconBadge(sectionIcon(s))
                            Spacer(Modifier.weight(1f))
                            if (n > 0) Text("$n", color = AccentSoft, fontSize = 15.sp, fontFamily = Display, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(s.title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
                        Text(s.subtitle, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
