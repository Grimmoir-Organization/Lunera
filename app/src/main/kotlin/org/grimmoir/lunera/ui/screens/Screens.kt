/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import org.grimmoir.lunera.R

/**
 * A bottom-bar / side-bar destination.
 *
 * One icon per destination per UI style — deliberately not a selected/unselected
 * pair. iOS ships outline/filled pairs because its tab bar has no selection
 * indicator; this bar has both a glass puck and an accent content colour, so a
 * filled variant would be a third signal saying the same thing. Two artworks per
 * tab also have to be optically aligned against each other or the icon visibly
 * shifts on selection, which is exactly the cheap-looking wobble the pair would
 * have bought us.
 */
@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val route: String,
    @DrawableRes val icon: Int,
    // SF Symbols-style lookalike used when the Apple Music UI style is active.
    // Defaults to the classic icon, so a destination only names this when it
    // actually has a distinct iOS drawing.
    @DrawableRes val iosIcon: Int = icon,
) {
    /** The icon to draw for the current UI style. */
    fun icon(appleMusicUi: Boolean): Int = if (appleMusicUi) iosIcon else icon

    object Home : Screens(
        titleId = R.string.lunera_player,
        route = "home",
        icon = R.drawable.play,
    )

    object Playlists : Screens(
        titleId = R.string.playlists,
        route = "playlists",
        icon = R.drawable.accord_library,
    )

    object Search : Screens(
        titleId = R.string.search,
        route = "search_input",
        icon = R.drawable.search,
        iosIcon = R.drawable.cosmos_search,
    )



    /**
     * Flat, alphabetised list of every song on the device. Only ever a tab in local-only
     * mode -- with YouTube on, "all songs" has no bounded meaning.
     */
    object Songs : Screens(
        titleId = R.string.lunera_all_songs,
        route = "songs",
        icon = R.drawable.music_note,
    )

    object Library : Screens(
        titleId = R.string.lunera_folders,
        route = "library",
        icon = R.drawable.accord_library,
    )


    object Settings : Screens(
        titleId = R.string.settings,
        route = "settings",
        icon = R.drawable.settings,
    )

    companion object {
        // Lazy initialization avoids reading singleton routes before their objects initialize.
        val MainScreens: List<Screens> by lazy { listOf(Songs, Playlists, Library, Search) }

        /**
         * Local-only mode promotes [Songs] and omits online destinations.
         */
        fun mainScreens(localOnly: Boolean): List<Screens> =
            MainScreens

        /** Every route that counts as a tab root, in either mode. */
        val MainRoutes: Set<String> by lazy { (MainScreens + Songs).map { it.route }.toSet() }
    }
}
