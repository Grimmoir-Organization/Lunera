package org.grimmoir.lunera.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class IntroFrameClockTest {
    @Test fun aMissedFrameCatchesUpInsteadOfStretchingTheIntro() {
        val clock = IntroFrameClock(1700f)
        assertEquals(0f, clock.frame(5_000_000_000L), 0f)
        assertEquals(16f, clock.frame(5_016_000_000L), 0f)
        assertEquals(600f, clock.frame(5_600_000_000L), 0f)
        assertEquals(1700f, clock.frame(6_700_000_000L), 0f)
    }

    @Test fun aLongPauseCompletesWithoutReplayingOldFrames() {
        val clock = IntroFrameClock(1700f)
        clock.frame(0L)
        assertEquals(1700f, clock.frame(10_000_000_000L), 0f)
        assertEquals(1700f, clock.frame(11_000_000_000L), 0f)
    }

    @Test fun duplicateOrOlderTimestampsNeverRewindTheAnimation() {
        val clock = IntroFrameClock(1700f)
        clock.frame(1_000_000_000L)
        assertEquals(100f, clock.frame(1_100_000_000L), 0f)
        assertEquals(100f, clock.frame(1_100_000_000L), 0f)
        assertEquals(100f, clock.frame(1_050_000_000L), 0f)
        assertEquals(200f, clock.frame(1_200_000_000L), 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnInvalidDuration() {
        IntroFrameClock(Float.NaN)
    }
}
