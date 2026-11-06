/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package org.grimmoir.lunera.ui.menu
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.grimmoir.lunera.db.entities.*
import org.grimmoir.lunera.models.toMediaMetadata
@Composable
fun SongMenu(originalSong: Song, event: Event? = null, navController: NavController, playlistSong: PlaylistSong? = null, playlistBrowseId: String? = null, onDismiss: () -> Unit, isFromCache: Boolean = false) {
    LuneraLocalPlayerMenu(originalSong.toMediaMetadata(), onDismiss)
}
