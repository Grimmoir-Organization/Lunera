package org.grimmoir.lunera

import org.grimmoir.lunera.ui.screens.Screens

/** Chrome policy for the local library, player and release screens. */
internal object AppRoutePolicy {
    val tabRootRoutes = setOf(Screens.Songs.route, Screens.Playlists.route, Screens.Library.route, Screens.Search.route, Screens.Settings.route)
    fun isTabRoot(route: String?) = route in tabRootRoutes
    fun isWebViewRoute(route: String?) = false
    fun showsNavigation(route: String?) = route !in setOf("equalizer", "update")
    fun showsPlayer(route: String?) = route !in setOf("equalizer", "update")
    fun showsBottomSheetPlayer(route: String?) = route != "update"
    fun needsDownloadState(route: String?) = false
    fun showsFallbackTopHaze(route: String?) = route != null && route !in tabRootRoutes && !route.startsWith("settings") && !route.startsWith("local_collection/")
}
