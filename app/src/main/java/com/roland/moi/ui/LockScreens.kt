package com.roland.moi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.PinManager

@Composable
fun SetupPinScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val title = if (first == null) "Crée ton code secret" else "Confirme ton code"
    PinPad(
        title = title,
        subtitle = error ?: "4 à 8 chiffres. Il protège tout ce que MOI contient.",
        pin = pin,
        isError = error != null,
        onDigit = { if (pin.length < 8) { pin += it; error = null } },
        onDelete = { pin = pin.dropLast(1) },
        confirmLabel = if (pin.length >= 4) "Valider" else null,
        onConfirm = {
            if (first == null) {
                first = pin; pin = ""
            } else if (first == pin) {
                PinManager.setPin(ctx, pin); onDone()
            } else {
                error = "Les deux codes ne correspondent pas"; first = null; pin = ""
            }
        },
    )
}

@Composable
fun LockScreen(bioAvailable: Boolean, onBiometric: () -> Unit, onUnlock: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (bioAvailable) onBiometric() }
    PinPad(
        title = "MOI",
        subtitle = if (error) "Code incorrect" else "Entre ton code",
        pin = pin,
        isError = error,
        onDigit = {
            if (pin.length < 8) {
                pin += it; error = false
                if (pin.length >= 4 && PinManager.check(ctx, pin)) onUnlock()
            }
        },
        onDelete = { pin = pin.dropLast(1) },
        confirmLabel = if (pin.length >= 4) "OK" else null,
        onConfirm = { if (PinManager.check(ctx, pin)) onUnlock() else { error = true; pin = "" } },
        extraKey = if (bioAvailable) Icons.Outlined.Fingerprint else null,
        onExtra = onBiometric,
    )
}

@Composable
private fun PinPad(
    title: String,
    subtitle: String,
    pin: String,
    isError: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    confirmLabel: String?,
    onConfirm: () -> Unit,
    extraKey: ImageVector? = null,
    onExtra: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().background(Black).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Gold, fontSize = 34.sp, fontFamily = Serif, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Spacer(Modifier.height(10.dp))
        Text(subtitle, color = if (isError) Danger else TextDim, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(maxOf(4, pin.length)) { i ->
                Box(
                    Modifier.size(14.dp)
                        .background(if (i < pin.length) Gold else Black, CircleShape)
                        .border(1.5.dp, if (isError) Danger else Gold, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) { r.forEach { Key(it) { onDigit(it) } } }
            Spacer(Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            if (extraKey != null) IconKey(extraKey, onExtra) else Spacer(Modifier.width(72.dp))
            Key("0") { onDigit("0") }
            IconKey(Icons.AutoMirrored.Outlined.Backspace, onDelete)
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.height(52.dp)) {
            if (confirmLabel != null) GoldButton(confirmLabel, Modifier.width(220.dp), onClick = onConfirm)
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).background(Surface1, CircleShape).border(1.dp, Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextMain, fontSize = 26.sp) }
}

@Composable
private fun IconKey(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(72.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Gold)
    }
}
