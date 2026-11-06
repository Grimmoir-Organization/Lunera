package org.grimmoir.lunera.playback
import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerCombinationTest {
    private fun player(onPause: () -> Unit): Player = java.lang.reflect.Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
        when (method.name) { "pause" -> { onPause(); null }; "getPlaybackState" -> Player.STATE_READY; "toString" -> "Timer test player"; "hashCode" -> 1; else -> null }
    } as Player
    @Test fun durationThenFinishIgnoresEarlierSongEndsAndWaitsAfterDeadline() = runTest {
        var pauses=0;val timer=SleepTimer(this,player { pauses++ }) { testScheduler.currentTime }
        timer.start(1,true);timer.onMediaItemTransition(null,Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        assertFalse(timer.pauseWhenSongEnd);assertEquals(0,pauses)
        advanceTimeBy(60000);runCurrent();assertTrue(timer.pauseWhenSongEnd);assertEquals(0,pauses)
        timer.onMediaItemTransition(null,Player.MEDIA_ITEM_TRANSITION_REASON_SEEK);assertTrue(timer.isActive)
        timer.onMediaItemTransition(null,Player.MEDIA_ITEM_TRANSITION_REASON_AUTO);assertFalse(timer.isActive);assertEquals(1,pauses)
    }
    @Test fun exactDeadlineUsesWholePlaybackPauseAndDeduplicatesFallback() = runTest {
        var now=0L;var pauses=0;var directPauses=0;var alarm:(()->Unit)?=null;var deadline=0L
        val timer=SleepTimer(this,player { directPauses++ },pausePlayback={pauses++},scheduleDeadline={at,callback->deadline=at;alarm=callback},elapsedRealtime={now})
        timer.start(5);assertEquals(300000L,deadline);alarm!!();assertEquals(0,pauses)
        now=deadline;alarm!!();assertEquals(1,pauses);assertEquals(0,directPauses);assertFalse(timer.isActive)
        advanceUntilIdle();alarm!!();assertEquals(1,pauses)
    }
    @Test fun replacingTimerMakesOldAlarmHarmless() = runTest {
        var now=0L;var pauses=0;val alarms=mutableListOf<()->Unit>()
        val timer=SleepTimer(this,player { pauses++ },scheduleDeadline={_,callback->alarms+=callback},elapsedRealtime={now})
        timer.start(5);timer.start(10);alarms[0]();assertTrue(timer.isActive);assertEquals(0,pauses)
        now=600000L;alarms[1]();assertFalse(timer.isActive);assertEquals(1,pauses);advanceUntilIdle()
    }
    @Test fun clearCancelsAlarmAndPendingFinishSong() = runTest {
        var pauses=0;var scheduled=false
        val timer=SleepTimer(this,player { pauses++ },scheduleDeadline={_,_->scheduled=true},cancelDeadline={scheduled=false},elapsedRealtime={testScheduler.currentTime})
        timer.start(1,true);assertTrue(scheduled);advanceTimeBy(60000);runCurrent();assertFalse(scheduled);assertTrue(timer.pauseWhenSongEnd)
        timer.clear();timer.notifySongTransition();assertEquals(0,pauses);assertFalse(timer.isActive)
    }
    @Test fun deferredExpiryOnResumeArmsSongEndInsteadOfPausing() = runTest {
        var elapsed=0L;var pauses=0;val timer=SleepTimer(this,player { pauses++ }) { elapsed }
        timer.start(1,true);elapsed=70000;timer.onPlayWhenReadyChanged(true,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        assertTrue(timer.pauseWhenSongEnd);assertEquals(0,pauses);timer.onPlaybackStateChanged(Player.STATE_ENDED)
        assertEquals(1,pauses);assertFalse(timer.isActive)
    }
}
