package org.grimmoir.lunera.ui.player
import org.junit.Assert.*
import org.junit.Test
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SleepTimerEditorTest {
    @Test fun openingReflectsRunningModeAndDuration() { val vm=SleepTimerEditorViewModel(); vm.open(45,true); assertEquals(SleepTimerDraft(45f,true),vm.state.value) }
    @Test fun choosingMinutesPreservesFinishSongOption() { val vm=SleepTimerEditorViewModel();vm.open(30,true);vm.minutes(60f);assertEquals(SleepTimerDraft(60f,true),vm.state.value) }
    @Test fun endOfSongCanBeCheckedAndUnchecked() { val vm=SleepTimerEditorViewModel();vm.toggleEndOfSong();assertTrue(vm.state.value.endOfSong);vm.toggleEndOfSong();assertFalse(vm.state.value.endOfSong) }
    @Test fun reopeningDiscardsUnconfirmedChanges() { val vm=SleepTimerEditorViewModel();vm.open(30,false);vm.minutes(90f);vm.toggleEndOfSong();vm.open(30,false);assertEquals(SleepTimerDraft(30f,false),vm.state.value) }
    private fun timer(scope: kotlinx.coroutines.CoroutineScope, time: () -> Long): org.grimmoir.lunera.playback.SleepTimer {
        val player = java.lang.reflect.Proxy.newProxyInstance(androidx.media3.common.Player::class.java.classLoader, arrayOf(androidx.media3.common.Player::class.java)) { _, _, _ -> null } as androidx.media3.common.Player
        return org.grimmoir.lunera.playback.SleepTimer(scope, player, time)
    }
    @Test fun unchangedOkKeepsOriginalDeadline() = kotlinx.coroutines.test.runTest {
        val timer=timer(this) { testScheduler.currentTime }; timer.start(30)
        testScheduler.advanceTimeBy(60_000)
        val vm=SleepTimerEditorViewModel(); vm.open(30,false); vm.confirm(timer)
        assertEquals(1_800_000L,timer.triggerTime);timer.clear()
    }
    @Test fun changedOkReplacesDeadlineAndTurnOffStillClearsIt() = kotlinx.coroutines.test.runTest {
        val timer=timer(this) { testScheduler.currentTime }; timer.start(30)
        testScheduler.advanceTimeBy(60_000)
        val vm=SleepTimerEditorViewModel(); vm.open(30,false);vm.minutes(10f);vm.confirm(timer)
        assertEquals(660_000L,timer.triggerTime)
        timer.clear(); assertFalse(timer.isActive)
    }
    @Test fun unchangedOkWhileFinishingSongKeepsPendingBoundary() = kotlinx.coroutines.test.runTest {
        val player = java.lang.reflect.Proxy.newProxyInstance(androidx.media3.common.Player::class.java.classLoader,
            arrayOf(androidx.media3.common.Player::class.java)) { _, method, _ ->
                if (method.name == "getPlaybackState") androidx.media3.common.Player.STATE_READY else null
            } as androidx.media3.common.Player
        val timer = org.grimmoir.lunera.playback.SleepTimer(this, player) { testScheduler.currentTime }
        timer.start(5, true)
        testScheduler.advanceTimeBy(300_000); testScheduler.runCurrent()
        val vm = SleepTimerEditorViewModel(); vm.open(5, true); vm.confirm(timer)
        assertTrue(timer.pauseWhenSongEnd); assertEquals(-1L, timer.triggerTime)
        timer.clear()
    }
}
