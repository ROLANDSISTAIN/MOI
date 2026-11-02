package com.roland.moi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Black = Color(0xFF0B0B0C)
val Surface1 = Color(0xFF151517)
val Surface2 = Color(0xFF1E1E21)
val Line = Color(0xFF2A2A2E)
val Gold = Color(0xFFD4AF37)
val GoldSoft = Color(0xFFB8963A)
val TextMain = Color(0xFFF2EFE6)
val TextDim = Color(0xFF9A978F)
val Danger = Color(0xFFC0563F)

private val scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Black,
    secondary = GoldSoft,
    background = Black,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Line,
    error = Danger,
)

@Composable
fun MoiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

val Serif = FontFamily.Serif

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Line), RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Gold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold, contentColor = Black,
            disabledContainerColor = Surface2, disabledContentColor = TextDim,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp)) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = TextMain, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(text, modifier = Modifier.padding(vertical = 4.dp)) }
}
