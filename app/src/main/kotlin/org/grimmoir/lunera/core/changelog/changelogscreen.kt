/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package org.grimmoir.lunera.core.changelog

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.grimmoir.lunera.BuildConfig
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.ui.screens.settings.SettingsPage

/** Installed release notes are available offline and never depend on an upstream service. */
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    versionTag: String = "v${BuildConfig.VERSION_NAME}",
) {
    SettingsPage(navController, title = "What's new", horizontalPadding = 20.dp) {
        Text("Lunera ${BuildConfig.VERSION_NAME}", style = AppTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        listOf(
            "Sleep timer: stop at the deadline or finish the song after it",
            "Create playlists with an editable default name",
            "Updated playlist input dialogs",
            "Folder selection saves without repeated file-manager requests",
            "Compact local lyrics and improved adjacent-file matching",
            "One player favourite heart and readable All songs options",
            "Fading wallpaper headers and a fixed-frame crop editor",
            "Revised crimson moon-and-note logo"
        ).forEach { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}
