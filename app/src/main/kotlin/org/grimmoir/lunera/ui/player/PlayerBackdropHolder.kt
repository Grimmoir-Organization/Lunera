/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.player
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import org.grimmoir.lunera.ui.component.backdrop.Backdrop

/**
 * Carries the player's own background layer up to the app root, so menus opened from the
 * expanded player can sample the player instead of the whole app behind it.
 */
class PlayerBackdropHolder {
    var backdrop: Backdrop? by mutableStateOf(null)
}

val LocalPlayerBackdropHolder = compositionLocalOf<PlayerBackdropHolder?> { null }
