package com.roland.moi.shield

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.Quotes
import com.roland.moi.data.Store
import com.roland.moi.ui.Black
import com.roland.moi.ui.GhostButton
import com.roland.moi.ui.Gold
import com.roland.moi.ui.GoldButton
import com.roland.moi.ui.MoiTheme
import com.roland.moi.ui.Serif
import com.roland.moi.ui.TextDim
import com.roland.moi.ui.TextMain
import com.roland.moi.ui.formatStreak
import kotlinx.coroutines.delay

/** L'écran plein qui s'affiche à la place du site. */
class ShieldActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* bloqué pendant le délai */ }
        })
        setContent { MoiTheme { ShieldScreen(onDone = ::finishToHome) } }
    }

    override fun onResume() { super.onResume(); showing = true }
    override fun onPause() { super.onPause(); showing = false }

    private fun finishToHome() {
        showing = false
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        @Volatile var showing = false
    }
}

@Composable
private fun ShieldScreen(onDone: () -> Unit) {
    val d = Store.data.value
    var left by remember { mutableIntStateOf(d.shieldDelaySec) }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
    }
    val streak = formatStreak(Store.currentStreakMs(d))

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("STOP.", color = Gold, fontSize = 46.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
        Spacer(Modifier.height(10.dp))
        Text("Tu es à $streak de liberté.", color = TextDim, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Text(d.shieldMessage, color = TextMain, fontSize = 22.sp, lineHeight = 31.sp, fontFamily = Serif, textAlign = TextAlign.Center)
        if (d.shieldVerse.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            Text(d.shieldVerse, color = Gold, fontSize = 16.sp, lineHeight = 23.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
        }
        val letter = remember { Quotes.forShield(d) }
        if (letter != null) {
            Spacer(Modifier.height(28.dp))
            Text(letter.source.uppercase(), color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(letter.text, color = TextMain, fontSize = 16.sp, lineHeight = 23.sp, fontFamily = Serif, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(44.dp))
        Text("Respire. Lève-toi. Bois un verre d'eau. Change de pièce.", color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        GoldButton(
            text = if (left > 0) "J'ai résisté ($left s)" else "J'ai résisté 💪",
            enabled = left <= 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Store.recordResisted()
            onDone()
        }
        Spacer(Modifier.height(12.dp))
        if (left <= 0) {
            GhostButton("Fermer", modifier = Modifier.fillMaxWidth(), color = TextDim) { onDone() }
        }
    }
}
