package org.grimmoir.lunera.ui.player

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepTimerDraft(val minutes: Float = 30f, val endOfSong: Boolean = false)

/** Draft edits never change a running timer until OK is pressed. */
class SleepTimerEditorViewModel : ViewModel() {
    private val mutable = MutableStateFlow(SleepTimerDraft())
    val state = mutable.asStateFlow()
    fun open(minutes: Int, endOfSong: Boolean) { mutable.value = SleepTimerDraft(minutes.coerceIn(5,120).toFloat(), endOfSong) }
    fun minutes(value: Float) { mutable.value = mutable.value.copy(minutes = value.coerceIn(5f,120f)) }
    fun toggleEndOfSong() { mutable.value = mutable.value.copy(endOfSong = !mutable.value.endOfSong) }
    fun confirm(timer: org.grimmoir.lunera.playback.SleepTimer) {
        val draft = mutable.value
        val minutes = kotlin.math.round(draft.minutes).toInt()
        if (!timer.isActive || minutes != timer.durationMinutes || draft.endOfSong != timer.finishSongAfterTimer) {
            timer.start(minutes, draft.endOfSong)
        }
    }
}
