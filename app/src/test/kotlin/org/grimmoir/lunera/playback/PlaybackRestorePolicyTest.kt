package org.grimmoir.lunera.playback
import org.junit.Assert.*
import org.junit.Test
class PlaybackRestorePolicyTest {
    @Test fun delayedRestoreCannotUndoClose() { assertFalse(PlaybackRestorePolicy.canApply(10,11,true,false)) }
    @Test fun oldUserRequestCannotUndoNewClose() { assertFalse(PlaybackRestorePolicy.canApply(10,11,true,true)) }
    @Test fun automaticRestoreIsBlockedWhileClosed() { assertFalse(PlaybackRestorePolicy.canApply(11,11,true,false)) }
    @Test fun explicitReopenCanRestoreSavedQueue() { assertTrue(PlaybackRestorePolicy.canApply(11,11,true,true)) }
    @Test fun newQueueMakesPreviousRestoreStale() { assertFalse(PlaybackRestorePolicy.canApply(11,12,false,false)) }
}
