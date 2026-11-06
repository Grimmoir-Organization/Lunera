package org.grimmoir.lunera.playback

/** A late I/O completion cannot undo a newer close or queue request. */
object PlaybackRestorePolicy {
    fun canApply(request: Long, current: Long, closing: Boolean, userInitiated: Boolean): Boolean =
        request == current && (!closing || userInitiated)
}
