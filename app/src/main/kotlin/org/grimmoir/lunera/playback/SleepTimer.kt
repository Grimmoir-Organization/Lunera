/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.playback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    var player: Player,
    private val pausePlayback: (() -> Unit)? = null,
    private val scheduleDeadline: (Long, () -> Unit) -> Unit = { _, _ -> },
    private val cancelDeadline: () -> Unit = {},
    private val elapsedRealtime: () -> Long = android.os.SystemClock::elapsedRealtime,
) : Player.Listener {
    constructor(scope: CoroutineScope, player: Player, clock: () -> Long) : this(scope, player, null, { _, _ -> }, {}, clock)
    private var sleepTimerJob: Job? = null
    private var generation = 0L
    var finishSongAfterTimer by mutableStateOf(false)
        private set
    var triggerTime by mutableLongStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    var durationMinutes by mutableIntStateOf(30)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int, finishSong: Boolean = false) {
        require(minute > 0) { "A sleep timer requires a positive duration" }
        clear()
        finishSongAfterTimer = finishSong
        durationMinutes = minute
        triggerTime = elapsedRealtime() + minute.minutes.inWholeMilliseconds
        val token = generation
        val expire = { if (generation == token && triggerTime >= 0L && elapsedRealtime() >= triggerTime) expireCountdown() }
        scheduleDeadline(triggerTime, expire)
        sleepTimerJob = scope.launch {
            delay(minute.minutes)
            expire()
        }
    }

    private fun expireCountdown() {
        cancelDeadline()
        triggerTime = -1L
        val countdown = sleepTimerJob
        sleepTimerJob = null
        countdown?.cancel()
        if (finishSongAfterTimer && player.playbackState != Player.STATE_ENDED) {
            pauseWhenSongEnd = true
        } else {
            clear()
            pausePlayback?.invoke() ?: player.pause()
        }
    }

    /** Also called when crossfade swaps the session player. */
    fun notifySongTransition() {
        pauseAtSongEnd()
    }

    fun clear() {
        ++generation
        cancelDeadline()
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) pauseAtSongEnd()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // A paused device may suspend the coroutine clock. Honour the elapsed deadline
        // before playback can resume, rather than extending the timer by that sleep.
        if (playWhenReady && triggerTime >= 0L && elapsedRealtime() >= triggerTime) {
            expireCountdown()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED) pauseAtSongEnd()
    }

    private fun pauseAtSongEnd() {
        if (!pauseWhenSongEnd) return
        clear()
        pausePlayback?.invoke() ?: player.pause()
    }
}
