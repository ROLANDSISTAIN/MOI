package com.roland.moi.shield

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.ui.Black
import com.roland.moi.ui.GhostButton
import com.roland.moi.ui.Gold
import com.roland.moi.ui.MoiTheme
import com.roland.moi.ui.Serif
import com.roland.moi.ui.TextDim
import com.roland.moi.ui.TextMain

/** Affiché quand quelqu'un essaie de désinstaller ou désactiver MOI pendant que la protection est active. */
class ProtectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoiTheme {
                Column(
                    Modifier.fillMaxSize().background(Black).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("👑", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("MOI est protégée", color = Gold, fontSize = 28.sp, fontFamily = Serif, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tu as choisi de te protéger de tes moments faibles. " +
                            "Si tu veux vraiment retirer cette protection, ouvre MOI → Réglages → Protection.",
                        color = TextMain, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Il faudra attendre 24 h ou recopier ta phrase d'engagement.", color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    GhostButton("J'ai compris", Modifier.fillMaxWidth()) {
                        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    }
                }
            }
        }
    }
}
