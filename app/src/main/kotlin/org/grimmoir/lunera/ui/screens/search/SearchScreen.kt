package org.grimmoir.lunera.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.grimmoir.lunera.LocalDatabase
import org.grimmoir.lunera.LocalPlayerAwareWindowInsets
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.constants.SongSortType
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.playback.queues.ListQueue
import org.grimmoir.lunera.ui.component.LocalNavSearchState
import org.grimmoir.lunera.ui.component.LocalMenuState
import org.grimmoir.lunera.ui.component.SymbolIcon
import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.menu.SongMenu

@Composable
fun SearchScreen(navController: NavController, pureBlack: Boolean, accountImageUrl: String? = null, accountName: String = "", onAccountClick: () -> Unit = {}) {
    val db = LocalDatabase.current.get()
    val songs by remember(db) { db.localSongs(SongSortType.NAME, false) }.collectAsStateWithLifecycle(emptyList())
    val search = LocalNavSearchState.current
    val query = search.query.text.trim()
    val filtered = remember(songs, query) { songs.filter { query.isEmpty() || it.song.title.contains(query, true) || it.artists.any { a -> a.name.contains(query, true) } || it.song.albumName?.contains(query, true) == true } }
    val player = LocalPlayerConnection.current
    val menu = LocalMenuState.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = maxOf(insets.calculateTopPadding(), 116.dp), bottom = insets.calculateBottomPadding() + 16.dp)) {
        item { Text(if (query.isEmpty()) "Search your device music" else "${filtered.size} songs", modifier = Modifier.padding(vertical = 16.dp), color = AppTheme.colorScheme.onSurfaceVariant) }
        items(filtered, key = { it.id }) { song ->
            Row(Modifier.fillMaxWidth().clickable { player?.playQueue(ListQueue("Search", filtered.map { it.toMediaItem() }, filtered.indexOf(song))) }.padding(vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(song.song.title, maxLines = 2)
                    Text(song.artists.joinToString { it.name }, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { menu.show { SongMenu(song, navController = navController, onDismiss = menu::dismiss) } }) { SymbolIcon(sf("ellipsis"), "Song options") }
            }
        }
        if (filtered.isEmpty()) item { Text("No matching songs", modifier = Modifier.padding(vertical = 32.dp)) }
    }
}
