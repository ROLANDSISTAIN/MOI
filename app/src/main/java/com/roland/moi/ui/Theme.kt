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
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.R

// ---------- Palette 2026 : noir profond, bleu électrique, rouge néon ----------
val Black = Color(0xFF000000)
val Surface1 = Color(0xFF0C0D11)
val Surface2 = Color(0xFF15171D)
val Line = Color(0xFF22252E)
val Accent = Color(0xFF007BFF)          // bleu électrique
val AccentSoft = Color(0xFF4DA3FF)      // bleu clair, pour les textes d'accent sur fond noir
val Neon = Color(0xFFFF073A)            // rouge néon
val TextMain = Color(0xFFF4F6FA)
val TextDim = Color(0xFF8A90A0)
val Danger = Neon
val OnAccent = Color.White
val Success = Color(0xFF22D39A)

/** Dégradé « néon » utilisé pour les cartes mises en avant. */
val HeroBrush = Brush.linearGradient(listOf(Color(0xFF0A2A5E), Color(0xFF050B1A)))
val GlowBorder = Brush.linearGradient(listOf(Accent, Color(0xFF7A3CFF), Neon))

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentSoft,
    tertiary = Neon,
    background = Black,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    surfaceContainerHigh = Surface1,
    surfaceContainerHighest = Surface2,
    outline = Line,
    error = Neon,
)

/** Police des titres et des chiffres : Space Grotesk. */
val Display = FontFamily(
    Font(R.font.grotesk_regular, FontWeight.Normal),
    Font(R.font.grotesk_medium, FontWeight.Medium),
    Font(R.font.grotesk_medium, FontWeight.SemiBold),
    Font(R.font.grotesk_bold, FontWeight.Bold),
)

private val typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Display),
        displayMedium = t.displayMedium.copy(fontFamily = Display),
        displaySmall = t.displaySmall.copy(fontFamily = Display),
        headlineLarge = t.headlineLarge.copy(fontFamily = Display),
        headlineMedium = t.headlineMedium.copy(fontFamily = Display),
        headlineSmall = t.headlineSmall.copy(fontFamily = Display),
        titleLarge = t.titleLarge.copy(fontFamily = Display),
    )
}

@Composable
fun MoiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}

/** Style des grands titres d'écran. */
val TitleStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp, color = TextMain)

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, Line), RoundedCornerShape(22.dp))
            .padding(18.dp),
        content = content,
    )
}

/** Carte mise en avant : fond bleu nuit et liseré néon. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(HeroBrush, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, GlowBorder), RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = AccentSoft,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = Display,
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent, contentColor = Color.White,
            disabledContainerColor = Surface2, disabledContentColor = TextDim,
        ),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontFamily = Display, fontSize = 15.sp, modifier = Modifier.padding(vertical = 5.dp)) }
}

/** Bouton d'urgence rouge néon. */
@Composable
fun NeonButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.White),
    ) { Text(text, fontWeight = FontWeight.Bold, fontFamily = Display, fontSize = 15.sp, modifier = Modifier.padding(vertical = 5.dp)) }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = TextMain, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(text, fontFamily = Display, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 5.dp)) }
}
