/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import org.grimmoir.lunera.ui.screens.Screens

/** Whether the app runs on device files only. Provided once at the root; screens read this instead of the preference. */
val LocalLocalOnlyMode = androidx.compose.runtime.staticCompositionLocalOf { true }

/** Destinations shown by the app's navigation bars. */
@Stable
class AppNavItems(
    val localOnlyMode: Boolean,
    val navigationItems: List<Screens>,
    val floatingNavigationItems: List<Screens>,
)

@Composable
fun rememberAppNavItems(): AppNavItems {
    val items = remember { listOf(Screens.Songs, Screens.Playlists, Screens.Library, Screens.Search) }
    return remember(items) { AppNavItems(true, items, items) }
}
