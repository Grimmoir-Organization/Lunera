package org.grimmoir.lunera.ui.screens.library
import org.junit.Assert.*
import org.junit.Test
class PlaylistDefaultNameTest {
 @Test fun favouritesDoesNotCount() { assertEquals("Playlist 1",nextLocalPlaylistName(listOf("Favourites"))) }
 @Test fun nextNumberFollowsExistingNumberedPlaylists() { assertEquals("Playlist 9",nextLocalPlaylistName(listOf("Favourites","Playlist 2","Evening","Playlist 8"))) }
}
