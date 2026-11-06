/** Copyright (C) 2026 Grimmoir Organization. SPDX-License-Identifier: GPL-3.0-only */
package org.grimmoir.lunera.ui.screens.library

import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import org.grimmoir.lunera.ui.ios.compat.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.grimmoir.lunera.LocalDatabase
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.LocalPlayerAwareWindowInsets
import org.grimmoir.lunera.constants.PlaylistSortType
import org.grimmoir.lunera.constants.SongSortType
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.db.entities.*
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.playback.queues.ListQueue
import org.grimmoir.lunera.ui.component.SymbolIcon
import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.component.LocalMenuState
import org.grimmoir.lunera.ui.menu.SongMenu
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import java.time.LocalDateTime

private const val FAVOURITES = "favourites"
fun nextLocalPlaylistName(names: List<String>): String = "Playlist " + ((names.mapNotNull { Regex("^Playlist (\\d+)$").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0) + 1)

@Composable
private fun PlaylistNameDialog(defaultName: String, busy: Boolean = false, error: String? = null, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Create playlist") }, text = {
        Column {
            OutlinedTextField(name, { name = it }, enabled = !busy, singleLine = true, label = { Text("Playlist name") })
            error?.let { Text(it, color = AppTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !busy && name.isNotBlank(), onClick = { onCreate(name.trim()) }) { Text(if (busy) "Creating…" else "Create") } }, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
}


suspend fun createLocalPlaylist(database: MusicDatabase, requestedName: String? = null): PlaylistEntity = withContext(Dispatchers.IO) {
    var created: PlaylistEntity? = null
    database.withTransaction {
        val names = playlists(PlaylistSortType.CREATE_DATE, false).first().map { it.playlist.name }
        val next = (names.mapNotNull { Regex("^Playlist (\\d+)$").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0) + 1
        created = PlaylistEntity(name = requestedName?.trim()?.takeIf { it.isNotEmpty() } ?: "Playlist $next", isLocal = true, bookmarkedAt = LocalDateTime.now())
        insert(created!!)
    }
    created!!
}

suspend fun appendLocalSongs(database: MusicDatabase, playlistId: String, ids: List<String>) = withContext(Dispatchers.IO) {
    database.withTransaction {
        val existing = playlistSongIds(playlistId)
        var position = playlistSongs(playlistId).first().maxOfOrNull { it.map.position }?.plus(1) ?: 0
        ids.distinct().filterNot { it in existing }.forEach { id ->
            if (song(id).first()?.song?.isLocal == true) insert(PlaylistSongMap(playlistId = playlistId, songId = id, position = position++))
        }
    }
}

@Composable
fun LocalPlaylistsScreen(navController: NavController) {
    val db = LocalDatabase.current.get()
    val playlists by remember(db) { db.playlists(PlaylistSortType.CREATE_DATE, false) }.collectAsStateWithLifecycle(emptyList())
    val favourites by remember(db) { db.likedSongs(SongSortType.CREATE_DATE, true) }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var queues by remember { mutableStateOf(false) }
    if (queues) org.grimmoir.lunera.ui.menu.LocalSavedQueues(onDismiss = { queues = false })
    var creating by remember { mutableStateOf(false) }
    var creationDefault by remember { mutableStateOf<String?>(null) }
    var creationError by remember { mutableStateOf<String?>(null) }
    creationDefault?.let { initial -> PlaylistNameDialog(initial, creating, creationError, { creationDefault = null }, { name ->
        creating = true
        scope.launch {
            runCatching { createLocalPlaylist(db, name) }.onSuccess { creationDefault = null; navController.navigate("local_collection/${it.id}") }.onFailure { creationError = "Could not create playlist" }
            creating = false
        }
    }) }
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = maxOf(insets.calculateTopPadding(), 116.dp), bottom = insets.calculateBottomPadding() + 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { CollectionRow("Favourites", "${favourites.count { it.song.isLocal }} songs", "heart.fill") { navController.navigate("local_collection/$FAVOURITES") } }
        item { TextButton(onClick = { queues = true }) { Text("Saved queues") } }
        item {
            TextButton(enabled = !creating, onClick = { creationError = null; creationDefault = nextLocalPlaylistName(playlists.map { it.playlist.name }) }, modifier = Modifier.fillMaxWidth()) { SymbolIcon(sf("plus"), null); Spacer(Modifier.width(12.dp)); Text("Create playlist") }
        }
        items(playlists.filter { it.playlist.browseId == null }, key = { it.id }) { playlist ->
            CollectionRow(playlist.playlist.name, "${playlist.songCount} songs", "music.note.list") { navController.navigate("local_collection/${playlist.id}") }
        }
    }
}

@Composable
private fun CollectionRow(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = AppTheme.colorScheme.surfaceContainer.copy(alpha = .88f), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SymbolIcon(sf(symbol), null, size = 24.dp, tint = if (symbol == "heart.fill") AppTheme.colorScheme.primary else AppTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, style = AppTheme.typography.titleMedium); Text(subtitle, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant) }
            SymbolIcon(sf("chevron.right"), null, size = 16.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalCollectionScreen(navController: NavController, id: String, filterKind: String? = null) {
    val db = LocalDatabase.current.get()
    val connection = LocalPlayerConnection.current
    val favourites = id == FAVOURITES
    val filteredCollection = filterKind != null
    val localSongs by remember(db) { db.localSongs(SongSortType.NAME, false) }.collectAsStateWithLifecycle(emptyList())
    val playlist by remember(id) { db.playlist(id) }.collectAsStateWithLifecycle(null)
    val maps by remember(id) { db.playlistSongs(id) }.collectAsStateWithLifecycle(emptyList())
    val liked by remember(db) { db.likedSongs(SongSortType.CREATE_DATE, true) }.collectAsStateWithLifecycle(emptyList())
    val songs = when (filterKind) {
        "album" -> localSongs.filter { it.song.albumId == id }
        "artist" -> localSongs.filter { it.artists.any { artist -> artist.id == id } }
        else -> if (favourites) liked.filter { it.song.isLocal } else maps.map { it.song }.filter { it.song.isLocal }
    }.distinctBy { it.id }
    val title = when (filterKind) {
        "album" -> songs.firstOrNull()?.song?.albumName ?: "Album"
        "artist" -> songs.firstOrNull()?.artists?.firstOrNull { it.id == id }?.name ?: "Artist"
        else -> if (favourites) "Favourites" else playlist?.playlist?.name ?: "Playlist"
    }
    val scope = rememberCoroutineScope()
    val menu = LocalMenuState.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    var rename by remember { mutableStateOf(false) }
    var name by remember(title) { mutableStateOf(title) }
    var deleting by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Rename playlist") }, text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }) }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { playlist?.playlist?.let { db.query { update(it.copy(name = name.trim())) } }; rename = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancel") } })
    if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Delete playlist?") }, text = { Text("Your audio files will stay on this device") }, confirmButton = { TextButton(onClick = { playlist?.playlist?.let { db.query { delete(it) } }; deleting = false; navController.popBackStack() }) { Text("Delete", color = AppTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } })
    if (adding) LocalSongPicker(existing = songs.map { it.id }.toSet(), onDismiss = { adding = false }, onSelected = { ids -> scope.launch { appendLocalSongs(db, id, ids); adding = false } })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = maxOf(insets.calculateTopPadding(), 64.dp), bottom = insets.calculateBottomPadding() + 16.dp)) {
        item {
            TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
            Text(title, style = AppTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
            Text("${songs.size} songs", color = AppTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = songs.isNotEmpty(), onClick = { connection?.playQueue(ListQueue(title, songs.map { it.toMediaItem() })) }) { Text("Play") }
                OutlinedButton(enabled = songs.isNotEmpty(), onClick = { connection?.playQueue(ListQueue(title, songs.shuffled().map { it.toMediaItem() })) }) { Text("Shuffle") }
            }
            if (!favourites && !filteredCollection) FlowRow {
                TextButton(onClick = { adding = true }) { Text("Add songs") }
                TextButton(onClick = { rename = true }) { Text("Rename") }
                TextButton(onClick = { deleting = true }) { Text("Delete") }
            }
            if (songs.isEmpty()) Text(if (favourites) "Tap the heart on a song to find it here" else "Add songs to this playlist", modifier = Modifier.padding(vertical = 32.dp))
        }
        items(songs, key = { it.id }) { song ->
            Row(Modifier.fillMaxWidth().clickable { connection?.playQueue(ListQueue(title, songs.map { it.toMediaItem() }, songs.indexOf(song))) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(song.song.title, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(song.artists.joinToString { it.name }, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant) }
                if (!favourites && !filteredCollection) {
                    val position = maps.indexOfFirst { it.song.id == song.id }
                    IconButton(enabled = position > 0, onClick = {
                        val previous = maps[position - 1].map; val current = maps[position].map
                        db.transaction { update(current.copy(position = previous.position)); update(previous.copy(position = current.position)) }
                    }) { SymbolIcon(sf("arrow.up"), "Move up") }
                }
                if (!filteredCollection) IconButton(onClick = { if (favourites) db.query { update(song.song.localToggleLike()) } else maps.firstOrNull { it.song.id == song.id }?.let { db.query { delete(it.map) } } }) { SymbolIcon(sf(if (favourites) "heart.fill" else "minus.circle"), if (favourites) "Remove from Favourites" else "Remove from playlist") }
                IconButton(onClick = { menu.show { SongMenu(song, navController = navController, onDismiss = menu::dismiss) } }) { SymbolIcon(sf("ellipsis"), "Song options") }
            }
        }
    }
}

@Composable
private fun LocalSongPicker(existing: Set<String>, onDismiss: () -> Unit, onSelected: (List<String>) -> Unit) {
    val db = LocalDatabase.current.get()
    val songs by remember(db) { db.localSongs(SongSortType.NAME, false) }.collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf(setOf<String>()) }
    var search by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add songs") }, text = {
        Column {
            OutlinedTextField(search, { search = it }, singleLine = true, placeholder = { Text("Search") })
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(songs.filter { it.id !in existing && it.song.title.contains(search, true) }, key = { it.id }) { song ->
                    Row(Modifier.fillMaxWidth().toggleable(value = song.id in selected, role = Role.Checkbox, onValueChange = { checked -> selected = if (checked) selected + song.id else selected - song.id }).heightIn(min = 48.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(song.id in selected, null); Spacer(Modifier.width(8.dp)); Text(song.song.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(enabled = selected.isNotEmpty(), onClick = { onSelected(selected.toList()) }) { Text("Add (${selected.size})") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun LocalAddToPlaylistDialog(songIds: List<String>, onDismiss: () -> Unit) {
    val db = LocalDatabase.current.get()
    val lists by remember(db) { db.playlists(PlaylistSortType.CREATE_DATE, false) }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var creationDefault by remember { mutableStateOf<String?>(null) }
    fun add(id: String?, name: String? = null) { if (busy) return; busy = true; scope.launch {
        runCatching { val target = id ?: createLocalPlaylist(db, name).id; appendLocalSongs(db, target, songIds) }.onSuccess { onDismiss() }.onFailure { error = "Could not add songs" }
        busy = false
    } }
    creationDefault?.let { initial -> PlaylistNameDialog(initial, busy, error, { creationDefault = null; error = null }, { name -> add(null, name) }) }
    if (creationDefault == null) AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Add to playlists") }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            item { TextButton(enabled = !busy, onClick = { error = null; creationDefault = nextLocalPlaylistName(lists.map { it.playlist.name }) }) { Text("Create playlist") } }
            items(lists.filter { it.playlist.browseId == null }, key = { it.id }) { list -> TextButton(enabled = !busy, onClick = { add(list.id) }, modifier = Modifier.fillMaxWidth()) { Text(list.playlist.name) } }
            error?.let { item { Text(it, color = AppTheme.colorScheme.error) } }
        }
    }, confirmButton = {}, dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
}
