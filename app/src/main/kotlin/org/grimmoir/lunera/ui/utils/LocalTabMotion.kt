package org.grimmoir.lunera.ui.utils

/** Brief tab fades keep two glass-heavy lists on screen for less than a full detail push. */
object LocalTabMotion {
    const val EnterMillis = 100
    const val ExitMillis = 50
    const val EnterDelayMillis = ExitMillis
    const val TotalMillis = EnterDelayMillis + EnterMillis
    private val tabs = setOf("songs", "playlists", "library", "search_input")
    fun isChange(from: String?, to: String?) = from != to && from in tabs && to in tabs
}

/** Short complete fades for ordinary pages over the persistent custom background. */
object LocalPageMotion {
    const val EnterMillis = 160
    const val ExitMillis = 70
    const val EnterDelayMillis = ExitMillis
    const val TotalMillis = EnterDelayMillis + EnterMillis
}
