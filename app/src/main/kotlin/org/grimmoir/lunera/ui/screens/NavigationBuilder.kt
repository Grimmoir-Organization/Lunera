/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import org.grimmoir.lunera.ui.utils.Motion
import org.grimmoir.lunera.ui.utils.LocalTabMotion
import org.grimmoir.lunera.ui.utils.LocalPageMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import org.grimmoir.lunera.ui.utils.LocalNavAnimatedVisibilityScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import org.grimmoir.lunera.ui.utils.LocalMorphArtworkId
import org.grimmoir.lunera.ui.utils.isMorphRoute
import org.grimmoir.lunera.ui.utils.morphArtworkId
import org.grimmoir.lunera.core.updater.UpdateScreen
import org.grimmoir.lunera.core.changelog.ChangelogScreen

import org.grimmoir.lunera.ui.screens.library.*
import org.grimmoir.lunera.ui.screens.settings.*
import org.grimmoir.lunera.ui.screens.equalizer.EqScreen
import org.grimmoir.lunera.ui.screens.equalizer.axion.AxionEqScreen
import org.grimmoir.lunera.ui.screens.settings.diy.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
    accountImageUrl: String? = null,
    accountName: String = "",
    onAccountClick: () -> Unit = { navController.navigate(Screens.Settings.route) },
) {
    sharedComposable(Screens.Home.route) { LocalPlaylistsScreen(navController) }
    sharedComposable(Screens.Songs.route) { LocalSongsScreen(navController) }
    sharedComposable(Screens.Playlists.route) { LocalPlaylistsScreen(navController) }
    sharedComposable(Screens.Library.route) { LocalFoldersScreen(navController) }
    sharedComposable(Screens.Search.route) { org.grimmoir.lunera.ui.screens.search.SearchScreen(navController = navController, pureBlack = false, accountImageUrl = null, accountName = "", onAccountClick = { navController.navigate("settings") }) }
    sharedComposable("local_collection/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
        LocalCollectionScreen(navController, it.arguments?.getString("id").orEmpty())
    }
    sharedComposable("local_folder/{path}", listOf(navArgument("path") { type = NavType.StringType })) {
        LocalFolderScreen(navController, it.arguments?.getString("path").orEmpty())
    }
    sharedComposable("album/{id}", listOf(navArgument("id") { type = NavType.StringType })) { LocalCollectionScreen(navController, it.arguments?.getString("id").orEmpty(), "album") }
    sharedComposable("artist/{id}", listOf(navArgument("id") { type = NavType.StringType })) { LocalCollectionScreen(navController, it.arguments?.getString("id").orEmpty(), "artist") }
    sharedComposable("settings/appearance/liquidglass/advanced") { GlassEffectSettings(navController, scrollBehavior, advanced = true) }
    sharedComposable("settings") { SettingsScreen(navController, scrollBehavior) }
    sharedComposable("settings/hub/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
        SettingsHubs.byId(it.arguments?.getString("id"))?.let { hub -> SettingsHubScreen(navController, hub) }
    }
    sharedComposable("settings/appearance/theme") { ThemeScreen(navController) }
    sharedComposable("settings/appearance/liquidglass") { GlassEffectSettings(navController, scrollBehavior) }
    sharedComposable("settings/appearance/playertheme") { PlayerThemeScreen(navController, scrollBehavior) }
    sharedComposable("settings/player") { PlayerSettings(navController, scrollBehavior) }
    sharedComposable("settings/player/advanced") { PlayerAdvancedSettings(navController, scrollBehavior) }
    sharedComposable("settings/song_transitions") { SongTransitionsScreen(navController) }
    sharedComposable("settings/player/local_folders") { LocalFoldersSettingsScreen(navController, scrollBehavior) }
    sharedComposable("settings/scan_music") { ScanMusicScreen(navController, scrollBehavior) }
    sharedComposable("settings/about") { AboutScreen(navController, scrollBehavior) }
    sharedComposable("settings/help/crash_logs") { CrashLogsSettingsScreen(navController) }
    sharedComposable("settings/update") { UpdateSettings(navController, scrollBehavior) }
    sharedComposable("update") { org.grimmoir.lunera.core.updater.UpdateScreen(navController) }
    sharedComposable("settings/changelog") { org.grimmoir.lunera.core.changelog.ChangelogScreen(navController, scrollBehavior) }
    sharedComposable("settings/lyrics") { LyricsSettingsScreen(navController) }
    sharedComposable("history") { HistoryScreen(navController) }
    sharedComposable("stats") { StatsScreen(navController) }
    sharedComposable("wrapped") { org.grimmoir.lunera.ui.screens.wrapped.WrappedScreen(navController) }
    dialog("equalizer") { EqScreen(navController) }
    sharedComposable("settings/equalizer") { AxionEqScreen(onBackClick = { navController.navigateUp() }) }
    sharedComposable("settings/diy") { DiyEditorScreen(navController) }
    sharedComposable("settings/diy/player_icons") { PlayerIconsScreen(navController, scrollBehavior) }
    sharedComposable("settings/diy/presets") { PresetsScreen(navController, scrollBehavior) }
}

/**
 * A destination that may open from a tile: reference-style "one shot" transition.
 *
 * Detail routes (album, playlist, artist...) slide in from the right while the tapped
 * tile's cover flies into the page's hero (`Modifier.sharedArtworkTarget`), and on pop
 * the page slides a quarter-width back out and fades while the cover flies home. The
 * screen behind holds still and dims, so the tile the cover returns to never moves.
 *
 * Non-detail destinations (tabs, settings...) keep the plain cross-fade: a slide on a
 * tab switch would read as drilling in when it is not.
 *
 * Publishes the destination's [AnimatedVisibilityScope] and its artwork id so the hero
 * can claim its half of the shared element without any per-screen plumbing.
 */
private fun NavGraphBuilder.sharedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
): Unit {
    val detail = isMorphRoute(route)
    val collection = route.startsWith("album/") || route.contains("playlist/")
    val darkRoute = org.grimmoir.lunera.ui.theme.isForcedDarkRoute(route)
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            if (LocalTabMotion.isChange(initialState.destination.route, targetState.destination.route)) {
                fadeIn(tween(LocalTabMotion.EnterMillis, delayMillis = LocalTabMotion.EnterDelayMillis, easing = FastOutSlowInEasing))
            } else if (collection) {
                if (org.grimmoir.lunera.ui.utils.isLibraryArtworkMorph()) fadeIn(tween(PageEnterMillis))
                else slideInHorizontally(Motion.collectionPush()) { it }
            } else if (detail) {
                slideInHorizontally(tween(PageEnterMillis, easing = FastOutSlowInEasing)) { it } +
                    fadeIn(tween(PageEnterMillis, easing = FastOutSlowInEasing))
            } else {
                fadeIn(tween(LocalPageMotion.EnterMillis, delayMillis = LocalPageMotion.EnterDelayMillis, easing = FastOutSlowInEasing))
            }
        },
        exitTransition = {
            val nextCollection = targetState.destination.route?.let { it.startsWith("album/") || it.contains("playlist/") } == true
            if (LocalTabMotion.isChange(initialState.destination.route, targetState.destination.route)) fadeOut(tween(LocalTabMotion.ExitMillis, easing = FastOutSlowInEasing))
            else if (nextCollection) fadeOut(tween(PageExitMillis), targetAlpha = if (org.grimmoir.lunera.ui.utils.isLibraryArtworkMorph()) 0f else .999f)
            else if (detail || isMorphRoute(targetState.destination.route)) fadeOut(tween(PageExitMillis), targetAlpha = 0.7f)
            else fadeOut(tween(LocalPageMotion.ExitMillis, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            val leavingCollection = initialState.destination.route?.let { it.startsWith("album/") || it.contains("playlist/") } == true
            if (LocalTabMotion.isChange(initialState.destination.route, targetState.destination.route)) fadeIn(tween(LocalTabMotion.EnterMillis, delayMillis = LocalTabMotion.EnterDelayMillis, easing = FastOutSlowInEasing))
            else if (leavingCollection) fadeIn(tween(PageEnterMillis), initialAlpha = if (org.grimmoir.lunera.ui.utils.isLibraryArtworkMorph()) 0f else .999f)
            else if (detail || isMorphRoute(initialState.destination.route)) fadeIn(tween(PageEnterMillis), initialAlpha = 0.7f)
            else fadeIn(tween(LocalPageMotion.EnterMillis, delayMillis = LocalPageMotion.EnterDelayMillis, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            if (LocalTabMotion.isChange(initialState.destination.route, targetState.destination.route)) fadeOut(tween(LocalTabMotion.ExitMillis, easing = FastOutSlowInEasing))
            else if (collection) {
                if (org.grimmoir.lunera.ui.utils.isLibraryArtworkMorph()) fadeOut(tween(PageExitMillis))
                else slideOutHorizontally(Motion.collectionPush()) { it }
            } else if (detail) {
                // Tween, not spring: predictive back scrubs this by gesture progress.
                slideOutHorizontally(tween(PageExitMillis, easing = FastOutSlowInEasing)) { it / 4 } +
                    fadeOut(tween(PageExitMillis, easing = FastOutSlowInEasing))
            } else {
                fadeOut(tween(LocalPageMotion.ExitMillis, easing = FastOutSlowInEasing))
            }
        },
    ) { entry ->
        CompositionLocalProvider(
            LocalNavAnimatedVisibilityScope provides this,
            LocalMorphArtworkId provides entry.morphArtworkId(),
        ) {
            if (collection) {
                val corner = org.grimmoir.lunera.ui.player.rememberScreenCornerRadius()
                val shape = remember(corner) { org.grimmoir.lunera.ui.component.shapes.ContinuousRoundedRectangle(corner) }
                val pageTransition = transition
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        clip = pageTransition.currentState != pageTransition.targetState
                        this.shape = shape
                    },
                ) { org.grimmoir.lunera.ui.theme.ForcedDarkScope(darkRoute) { content(entry) } }
            } else org.grimmoir.lunera.ui.theme.ForcedDarkScope(darkRoute) { content(entry) }
        }
    }
}

private const val PageEnterMillis = 400
private const val PageExitMillis = 400
