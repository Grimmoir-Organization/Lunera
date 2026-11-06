package org.grimmoir.lunera.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.db.entities.Album
import org.grimmoir.lunera.db.entities.Artist
import org.grimmoir.lunera.db.entities.Song
import org.grimmoir.lunera.utils.LocalAudioScanner
import org.grimmoir.lunera.utils.LocalFolderIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import org.grimmoir.lunera.utils.dataStore
import org.grimmoir.lunera.constants.LocalSongSortTypeKey
import org.grimmoir.lunera.constants.LocalSongSortDescendingKey
import org.grimmoir.lunera.constants.SongSortType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val database: MusicDatabase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : ViewModel() {

    init { viewModelScope.launch { LocalFolderIndex.restore(context) } }

    val songs: StateFlow<List<Song>> = database.localSongsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sortOrder = context.dataStore.data.map { prefs ->
        val type = runCatching { SongSortType.valueOf(prefs[LocalSongSortTypeKey].orEmpty()) }.getOrDefault(SongSortType.NAME)
        type to (prefs[LocalSongSortDescendingKey] ?: false)
    }.distinctUntilChanged()
    val sortedSongs = combine(songs, sortOrder) { rows, (type, descending) ->
        val sorted = when (type) {
            SongSortType.NAME -> rows
            SongSortType.ARTIST -> rows.sortedBy { it.artists.firstOrNull()?.name?.lowercase().orEmpty() }
            SongSortType.CREATE_DATE -> rows.sortedBy { it.song.dateModified }
            SongSortType.PLAY_TIME -> rows.sortedBy { it.song.totalPlayTime }
        }
        if (descending) sorted.asReversed() else sorted
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayed = callbackFlow {
        val prefs = context.getSharedPreferences("lunera", 0)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "last_track") trySend(prefs.getString("last_track", null))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString("last_track", null))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val albums: StateFlow<List<Album>> = database.albumsLocalByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = database.artistsLocalByNameAsc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val indexedFolders = LocalFolderIndex.updates
    val folders = combine(indexedFolders, songs) { folders, songs ->
        val ids = songs.mapTo(hashSetOf()) { it.id }
        folders.map { it.copy(songIds = it.songIds.filter(ids::contains)) }
            .filter { it.songIds.isNotEmpty() }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderGroups = folders.map { rows ->
        rows.groupBy { it.storageId }.entries.sortedBy { !org.grimmoir.lunera.utils.StorageLabels.primary(it.key) }.map { (id, rows) ->
            org.grimmoir.lunera.ui.screens.settings.StorageFolderGroup(id, org.grimmoir.lunera.utils.StorageLabels.storageName(context, id), rows.map { row ->
                val uri = android.net.Uri.Builder().scheme("lunera-folder").authority(id).path(row.relativePath).build()
                org.grimmoir.lunera.ui.screens.settings.FolderChoice(uri, row.songIds.size)
            })
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResult = MutableStateFlow<LocalAudioScanner.ScanResult?>(null)
    val scanResult: StateFlow<LocalAudioScanner.ScanResult?> = _scanResult.asStateFlow()

    /** Set once a scan finishes, so re-entering the screen doesn't rescan on every visit. */
    private var lastScanAt = 0L

    /**
     * [force] is what a tapped Rescan button passes; screen-entry auto-scans leave it
     * false so a quick trip to the player and back doesn't kick off a full MediaStore
     * sweep every time.
     */
    fun scanDevice(context: Context, force: Boolean = true) {
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = LocalAudioScanner.scanAndInsert(context, database)
                _scanResult.value = result
                lastScanAt = System.currentTimeMillis()
                Timber.tag("LocalMusicViewModel").i("Scan complete: $result")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("LocalMusicViewModel").e(e, "Scan failed")
            } finally {
                _isScanning.value = false
            }
        }
    }

    private companion object {
        /** How stale an auto-scan may be before entering the screen triggers a fresh one. */
        const val AUTO_SCAN_INTERVAL_MS = 5 * 60 * 1000L
    }
}
