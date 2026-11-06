package org.grimmoir.lunera.playback
import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {
    private fun player(onPause: () -> Unit): Player = java.lang.reflect.Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
        when (method.name) { "pause" -> { onPause(); null }; "getPlaybackState" -> Player.STATE_READY; "toString" -> "Timer player"; "hashCode" -> 1; else -> null }
    } as Player
    @Test fun timerPausesAtDeadline() = runTest {
        var pauses = 0
        val timer = SleepTimer(this, player { pauses++ }) { testScheduler.currentTime }
        timer.start(1)
        assertEquals(60_000L, timer.triggerTime)
        advanceTimeBy(59_999); runCurrent(); assertEquals(0, pauses)
        advanceTimeBy(1); runCurrent(); assertEquals(1, pauses); assertFalse(timer.isActive)
    }
    @Test fun replacingTimerCancelsOldDeadline() = runTest {
        var pauses = 0
        val timer = SleepTimer(this, player { pauses++ }) { testScheduler.currentTime }
        timer.start(1); advanceTimeBy(30_000); timer.start(2)
        advanceTimeBy(30_000); runCurrent(); assertEquals(0, pauses)
        assertEquals(150_000L, timer.triggerTime)
        advanceUntilIdle(); assertEquals(1, pauses)
    }
    @Test fun clearCancelsPauseAndState() = runTest {
        var pauses = 0
        val timer = SleepTimer(this, player { pauses++ }) { testScheduler.currentTime }
        timer.start(1); timer.clear(); advanceUntilIdle()
        assertFalse(timer.isActive); assertEquals(0, pauses)
    }
    @Test fun endOfSongPausesOnlyOnce() = runTest {
        var pauses = 0
        val timer = SleepTimer(this, player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent(); assertTrue(timer.isActive)
        timer.notifySongTransition(); timer.notifySongTransition()
        assertEquals(1, pauses); assertFalse(timer.isActive)
    }
    @Test fun clearingEndOfSongNeverPausesLater() = runTest {
        var pauses=0;val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent();timer.clear();timer.notifySongTransition();advanceUntilIdle();assertEquals(0,pauses);assertFalse(timer.isActive)
    }
    @Test fun replacingWithFinishAfterTimerCancelsOldDeadline() = runTest {
        var pauses=0;val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1);advanceTimeBy(20000);timer.start(1, true); advanceTimeBy(60_000); runCurrent();advanceTimeBy(60000);runCurrent();assertEquals(0,pauses)
        timer.notifySongTransition();assertEquals(1,pauses)
    }
    @Test fun pendingSongFinishCanBeReplacedWithMinutes() = runTest {
        var pauses=0;val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent();timer.start(1);timer.notifySongTransition();assertEquals(0,pauses);assertFalse(timer.pauseWhenSongEnd)
        advanceUntilIdle();assertEquals(1,pauses);assertFalse(timer.isActive)
    }
    @Test fun manualSkipDoesNotConsumeEndOfSongTimer() = runTest {
        var pauses=0; val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent()
        timer.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        timer.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        assertEquals(0,pauses); assertTrue(timer.isActive)
        timer.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        assertEquals(1,pauses); assertFalse(timer.isActive)
    }
    @Test fun repeatOneStopsOnceAtBoundary() = runTest {
        var pauses=0; val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent()
        timer.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        timer.onPlaybackStateChanged(Player.STATE_ENDED)
        assertEquals(1,pauses); assertFalse(timer.isActive)
    }
    @Test fun finalSongAndNewTimerWorkAfterExpiry() = runTest {
        var pauses=0; val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1, true); advanceTimeBy(60_000); runCurrent(); timer.onPlaybackStateChanged(Player.STATE_ENDED)
        assertEquals(1,pauses); assertFalse(timer.isActive)
        timer.start(1); advanceTimeBy(60_000); runCurrent()
        assertEquals(2,pauses); assertFalse(timer.isActive)
    }
    @Test fun removedStandaloneModeIsRejectedWithoutChangingRunningTimer() = runTest {
        val timer = SleepTimer(this, player {}) { testScheduler.currentTime }
        timer.start(5)
        val deadline = timer.triggerTime
        try { timer.start(-1); fail("Standalone song-end mode must be rejected") }
        catch (_: IllegalArgumentException) { }
        assertEquals(deadline, timer.triggerTime)
        assertFalse(timer.pauseWhenSongEnd)
        timer.clear()
    }
    @Test fun resumeAfterElapsedDeadlineCannotExtendPausedTimer() = runTest {
        var pauses=0; var elapsed=0L
        val timer=SleepTimer(this,player { pauses++ }) { elapsed }
        timer.start(1);elapsed=120_000
        timer.onPlayWhenReadyChanged(true,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertFalse(timer.isActive);assertEquals(1,pauses)
        advanceUntilIdle();assertEquals(1,pauses)
    }
}
