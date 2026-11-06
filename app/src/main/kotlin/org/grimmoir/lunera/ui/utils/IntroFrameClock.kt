package org.grimmoir.lunera.ui.utils

/** Elapsed display time: a missed frame must not extend the startup animation. */
internal class IntroFrameClock(private val durationMillis: Float) {
    init {
        require(durationMillis > 0f && durationMillis.isFinite())
    }

    private var startedAtNanos: Long? = null
    private var elapsedMillis = 0f

    fun frame(frameTimeNanos: Long): Float {
        val start = startedAtNanos ?: frameTimeNanos.also { startedAtNanos = it }
        val elapsed = ((frameTimeNanos - start) / 1_000_000f).coerceIn(0f, durationMillis)
        elapsedMillis = maxOf(elapsedMillis, elapsed)
        return elapsedMillis
    }
}
