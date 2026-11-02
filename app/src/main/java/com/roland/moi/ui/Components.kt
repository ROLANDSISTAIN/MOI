package com.roland.moi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roland.moi.data.Photos

/** Écran avec barre de titre et retour. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour", tint = Gold) }
            } else Spacer(Modifier.width(16.dp))
            Text(title, color = Gold, fontSize = 22.sp, fontFamily = Serif, modifier = Modifier.weight(1f), maxLines = 1)
            actions()
        }
        val m = Modifier.fillMaxWidth().weight(1f).let { if (scroll) it.verticalScroll(rememberScrollState()) else it }
        Column(m.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, minLines: Int = 1, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold, unfocusedBorderColor = Line, focusedLabelColor = Gold,
            cursorColor = Gold, focusedTextColor = TextMain, unfocusedTextColor = TextMain,
        ),
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Gold else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text(text, color = if (selected) Black else TextMain, fontSize = 13.sp) }
}

val MOODS = listOf("😞", "😕", "😐", "🙂", "😄")

@Composable
fun MoodPicker(value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MOODS.forEachIndexed { i, e ->
            val v = i + 1
            Box(
                Modifier.size(50.dp).clip(CircleShape)
                    .background(if (value == v) Gold.copy(alpha = 0.25f) else Surface2)
                    .border(1.5.dp, if (value == v) Gold else Line, CircleShape)
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 24.sp) }
        }
    }
}

@Composable
fun PhotoBox(name: String, height: Dp = 200.dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(name) { Photos.load(ctx, name) }
    if (bmp != null) {
        Image(
            bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(14.dp)),
        )
    }
}

@Composable
fun Avatar(name: String, fallback: String, size: Dp = 44.dp) {
    val ctx = LocalContext.current
    val bmp = remember(name) { Photos.load(ctx, name, 300) }
    if (bmp != null) {
        Image(bmp.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
            Text(fallback, fontSize = (size.value * 0.45f).sp)
        }
    }
}

/** Ligne cliquable « emoji · titre · sous-titre ». */
@Composable
fun ListRow(emoji: String, title: String, subtitle: String = "", trailing: String = "", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1)
            .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) Text(subtitle, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
        }
        if (trailing.isNotBlank()) Text(trailing, color = Gold, fontSize = 13.sp)
    }
}

@Composable
fun EmptyState(text: String) {
    Text(text, color = TextDim, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = if (initial > 0) initial else System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { st.selectedDateMillis?.let(onPick); onDismiss() }) { Text("OK", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextDim) } },
    ) { DatePicker(state = st) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minutes: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val st = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        confirmButton = { TextButton(onClick = { onPick(st.hour * 60 + st.minute); onDismiss() }) { Text("OK", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextDim) } },
        text = { TimePicker(state = st) },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(title, color = Gold) },
        text = { Text(text, color = TextMain) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = TextDim) } },
    )
}
