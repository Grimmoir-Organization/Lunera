package org.grimmoir.lunera
import org.junit.Assert.*
import org.junit.Test
import org.grimmoir.lunera.playback.disabledPlaybackResumption
class LuneraResumptionTest {
    @Test fun closedSessionDoesNotHangOrResume() {
        val future = disabledPlaybackResumption()
        assertTrue(future.isDone)
        try { future.get(1, java.util.concurrent.TimeUnit.SECONDS); fail("Expected disabled resumption") }
        catch (e: java.util.concurrent.ExecutionException) { assertTrue(e.cause is UnsupportedOperationException) }
    }
}
