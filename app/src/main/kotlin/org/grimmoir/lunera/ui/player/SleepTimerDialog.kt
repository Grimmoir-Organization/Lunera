/** Copyright (C) 2026 Grimmoir Organization. SPDX-License-Identifier: GPL-3.0-only */
package org.grimmoir.lunera.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import kotlin.math.roundToInt

/** A real dialog content surface: interactive controls are not placed inside an alert message slot. */
@Composable
fun SleepTimerDialog(
    sleepTimerValue: Float, onSleepTimerValueChange: (Float) -> Unit,
    endOfSong: Boolean, onEndOfSongChange: () -> Unit,
    running: Boolean, remaining: String, onConfirm: () -> Unit,
    onDismiss: () -> Unit, onTurnOff: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(horizontal = 4.dp), shape = RoundedCornerShape(24.dp), color = AppTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sleep timer", style = AppTheme.typography.titleLarge)
                Text(remaining, style = AppTheme.typography.bodyMedium, color = AppTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text("${sleepTimerValue.roundToInt()} minutes", style = AppTheme.typography.titleMedium)
                }
                Slider(value = sleepTimerValue, onValueChange = onSleepTimerValueChange, valueRange = 5f..120f, steps = 22, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().toggleable(endOfSong, role = Role.Checkbox, onValueChange = { onEndOfSongChange() }).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = endOfSong, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Finish the song after the timer")
                }
                if (running) TextButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) { Text("Turn off", color = AppTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
