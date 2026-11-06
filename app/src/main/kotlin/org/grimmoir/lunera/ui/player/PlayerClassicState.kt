/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.player
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.only
import androidx.compose.ui.platform.LocalContext
import org.grimmoir.lunera.applecanvas.AppleMusicCanvasProvider
import org.grimmoir.lunera.canvas.TidalCanvasProvider
import org.grimmoir.lunera.ui.player.CanvasArtworkPlaybackCache
import org.grimmoir.lunera.ui.player.normalizeCanvasArtistName
import org.grimmoir.lunera.ui.player.normalizeCanvasSongTitle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableFloatState
import org.grimmoir.lunera.playback.PlayerConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.grimmoir.lunera.canvas.CanvasArtwork
import org.grimmoir.lunera.constants.CanvasSource
import org.grimmoir.lunera.constants.PlayerBackgroundStyle
import org.grimmoir.lunera.models.MediaMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Sleep-timer UI state: whether one is running, the time left, and the dialog's own state. */
@Stable
class SleepTimerState(
    val enabled: Boolean,
    val timeLeft: State<Long>,
    val showDialog: MutableState<Boolean>,
    val value: MutableFloatState,
)

@Composable
fun rememberSleepTimerState(playerConnection: PlayerConnection): SleepTimerState {
    val sleepTimerEnabled = playerConnection.service.sleepTimer.isActive

    val timeLeft = remember { mutableLongStateOf(0L) }
    LaunchedEffect(sleepTimerEnabled, playerConnection.service.sleepTimer.triggerTime, playerConnection.service.sleepTimer.pauseWhenSongEnd) {
        if (sleepTimerEnabled) {
            while (isActive) {
                timeLeft.longValue =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - android.os.SystemClock.elapsedRealtime()
                    }
                delay(1000L)
            }
        } else {
            timeLeft.longValue = 0L
        }
    }

    val showDialog = remember { mutableStateOf(false) }
    val value = remember { mutableFloatStateOf(30f) }
    return SleepTimerState(sleepTimerEnabled, timeLeft, showDialog, value)
}

/** The dialog that starts a sleep timer; renders nothing while [SleepTimerState.showDialog] is false. */
@Composable
fun SleepTimerDialogHost(playerConnection: PlayerConnection, state: SleepTimerState) {
    var showSleepTimerDialog by state.showDialog
    if (showSleepTimerDialog) {
        val timer = playerConnection.service.sleepTimer
        val editor: SleepTimerEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val draft by editor.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { editor.open(timer.durationMinutes, timer.finishSongAfterTimer) }
        SleepTimerDialog(
            sleepTimerValue = draft.minutes,
            onSleepTimerValueChange = editor::minutes,
            endOfSong = draft.endOfSong,
            onEndOfSongChange = editor::toggleEndOfSong,
            running = timer.isActive,
            remaining = if (!timer.isActive) "Timer off" else if (timer.pauseWhenSongEnd) "Stops at the end of this song" else "Time left: " + org.grimmoir.lunera.utils.makeTimeString(state.timeLeft.value.coerceAtLeast(0)),
            onConfirm = {
                editor.confirm(timer)
                showSleepTimerDialog = false
            },
            onTurnOff = { timer.clear(); showSleepTimerDialog = false },
            onDismiss = { showSleepTimerDialog = false },
        )
    }
}

/** The classic APPLE_MUSIC background's canvas candidates (V2 fetches its own in PlayerV2Canvas.kt). */
@Stable
class ClassicCanvasState(
    val candidates: MutableState<List<CanvasArtwork>>,
    val index: MutableIntState,
)

@Composable
fun rememberClassicCanvasState(
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    canvasSource: CanvasSource,
    useAppleMusicPlayer: Boolean,
    enableCanvas: Boolean,
): ClassicCanvasState {
    return ClassicCanvasState(remember(mediaMetadata?.id) { mutableStateOf(emptyList()) }, remember(mediaMetadata?.id) { mutableIntStateOf(0) })
    }
