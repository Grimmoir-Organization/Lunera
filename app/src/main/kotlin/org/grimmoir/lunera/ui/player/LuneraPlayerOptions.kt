package org.grimmoir.lunera.ui.player
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.extensions.toggleRepeatMode
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.utils.ShowMediaInfo
import org.grimmoir.lunera.utils.makeTimeString
@Composable
fun LuneraPlayerOptions(navController: NavController, color: Color = AppTheme.colorScheme.onBackground) {
    val connection = LocalPlayerConnection.current ?: return
    val timer = rememberSleepTimerState(connection)
    SleepTimerDialogHost(connection, timer)
    val remaining by timer.timeLeft
    val repeat by connection.repeatMode.collectAsStateWithLifecycle()
    val pages = LocalBottomSheetPageState.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        PlayerOption("moon.zzz.fill", if (timer.enabled) if (connection.service.sleepTimer.pauseWhenSongEnd) "End of song" else makeTimeString(remaining.coerceAtLeast(0)) else "Sleep timer", color) { timer.showDialog.value = true }
        PlayerOption("info.circle", "Details", color) { connection.player.currentMediaItem?.mediaId?.let { id -> pages.showFloating { ShowMediaInfo(id) } } }
        PlayerOption(if (repeat == Player.REPEAT_MODE_ONE) "repeat.1" else "repeat", when (repeat) { Player.REPEAT_MODE_ONE -> "Repeat one"; Player.REPEAT_MODE_ALL -> "Repeat all"; else -> "Repeat off" }, color) { connection.player.toggleRepeatMode() }
        PlayerOption("slider.vertical.3", "Equalizer", color) { navController.navigate("settings/equalizer") }
    }
}
@Composable
private fun PlayerOption(symbol: String, label: String, color: Color, action: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = action, modifier = Modifier.size(48.dp)) { SymbolIcon(sf(symbol), contentDescription = label, tint = color) }
        Text(label, style = AppTheme.typography.labelSmall, color = color)
    }
}
