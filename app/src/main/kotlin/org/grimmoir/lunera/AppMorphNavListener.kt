/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import org.grimmoir.lunera.ui.component.prewarmHeroTint
import org.grimmoir.lunera.ui.player.prewarmMeshPalette
import org.grimmoir.lunera.ui.utils.markMorphDirection
import org.grimmoir.lunera.ui.utils.markSharedArtworkSource
import org.grimmoir.lunera.ui.utils.isMorphRoute
import org.grimmoir.lunera.ui.utils.morphArtworkId

/** Sets the shared artwork source and preloads its colors when navigation changes. */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun InstallMorphNavListener(navController: NavHostController, artworkState: org.grimmoir.lunera.ui.utils.NavigationArtworkState) {
    val context = LocalContext.current
    DisposableEffect(navController, artworkState) {
        var previousBackStackSize = -1
        var previousRoute: String? = null
        @SuppressLint("RestrictedApi")
        val listener = NavController.OnDestinationChangedListener { controller, destination, args ->
            val currentSize = controller.currentBackStack.value.size
            val isPop = previousBackStackSize != -1 && currentSize < previousBackStackSize
            previousBackStackSize = currentSize
            markMorphDirection(isPop)

            val artworkId = if (isPop && isMorphRoute(previousRoute)) {
                artworkState.selected?.id ?: args.morphArtworkId()
            } else {
                args.morphArtworkId()
            }
            val thumbnailUrl = markSharedArtworkSource(artworkState, artworkId, isPop)
            previousRoute = destination.route
            val collection = destination.route?.let { it.startsWith("album/") || it.contains("playlist/") } == true
            if (!collection) {
                prewarmHeroTint(context, thumbnailUrl)
                prewarmMeshPalette(context, thumbnailUrl)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}
