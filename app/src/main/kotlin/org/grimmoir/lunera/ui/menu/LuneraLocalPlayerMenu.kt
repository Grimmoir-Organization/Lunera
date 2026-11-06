package org.grimmoir.lunera.ui.menu
import org.grimmoir.lunera.LocalRingtoneViewModel
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.grimmoir.lunera.extensions.toMediaItem
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import android.content.Intent
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.grimmoir.lunera.models.MediaMetadata
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.screens.settings.*
import org.grimmoir.lunera.utils.LocalAudioTags
import org.grimmoir.lunera.utils.LuneraFolders
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.utils.LocalAudioScanner

data class LocalEditState(val title: String = "", val artist: String = "", val album: String = "", val busy: Boolean = false, val error: String? = null, val done: Boolean = false)
@HiltViewModel
class LocalTrackEditViewModel @Inject constructor(@ApplicationContext private val context: Context, private val database: MusicDatabase) : ViewModel() {
    private val mutable = MutableStateFlow(LocalEditState())
    val state = mutable.asStateFlow()
    fun begin(metadata: MediaMetadata) { mutable.value = LocalEditState(metadata.title, metadata.artists.joinToString { it.name }, metadata.album?.title.orEmpty()) }
    fun title(value: String) { mutable.value = mutable.value.copy(title = value) }
    fun artist(value: String) { mutable.value = mutable.value.copy(artist = value) }
    fun album(value: String) { mutable.value = mutable.value.copy(album = value) }
    fun consumeDone() { mutable.value = mutable.value.copy(done = false) }
    fun failure(message: String) { mutable.value = mutable.value.copy(error = message, busy = false) }
    fun update(uri: Uri, relativePath: String? = null, recover: (IntentSender) -> Unit) {
        if (mutable.value.busy) return
        val draft = mutable.value
        viewModelScope.launch {
            mutable.value = mutable.value.copy(busy = true, error = null)
            try {
                if (relativePath == null) LocalAudioTags.write(context, uri, draft.title, draft.artist, draft.album)
                else withContext(Dispatchers.IO) {
                    require(Build.VERSION.SDK_INT >= 29) { "Moving music requires Android 10 or newer." }
                    require(relativePath.isNotBlank() && relativePath.split('/').none { it == ".." }) { "Invalid destination folder." }
                    val values = ContentValues().apply { put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath.trim('/') + "/") }
                    check(context.contentResolver.update(uri, values, null, null) == 1) { "The song could not be moved." }
                }
                LocalAudioScanner.scanAndInsert(context, database)
                mutable.value = mutable.value.copy(busy = false, done = true)
            } catch (e: RecoverableSecurityException) {
                mutable.value = mutable.value.copy(busy = false)
                recover(e.userAction.actionIntent.intentSender)
            } catch (e: Exception) { failure(e.message ?: "The audio file could not be updated.") }
        }
    }
    fun delete(uri: Uri, recover: (IntentSender) -> Unit) { viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { check(context.contentResolver.delete(uri, null, null) > 0) { "Audio file not found." } }
            LocalAudioScanner.scanAndInsert(context, database)
            mutable.value = mutable.value.copy(done = true)
        } catch (e: RecoverableSecurityException) { recover(e.userAction.actionIntent.intentSender) }
        catch (e: Exception) { failure(e.message ?: "The song could not be deleted.") }
    } }
    fun deleted() { viewModelScope.launch { LocalAudioScanner.scanAndInsert(context, database); mutable.value = mutable.value.copy(done = true) } }
}

@Composable
fun LuneraLocalPlayerMenu(metadata: MediaMetadata, onDismiss: () -> Unit, playerMenu: Boolean = false, vm: LocalTrackEditViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current
    val uri = remember(metadata.id) { Uri.parse(metadata.id) }
    val connection = LocalPlayerConnection.current
    val ringtone = LocalRingtoneViewModel.current
    val database = org.grimmoir.lunera.LocalDatabase.current.get()
    val song by remember(metadata.id) { database.song(metadata.id) }.collectAsStateWithLifecycle(null)
    var info by remember { mutableStateOf(false) }
    var addPlaylist by remember { mutableStateOf(false) }
    var addQueue by remember { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(false) }
    var move by remember { mutableStateOf(false) }
    var tempo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var requestedPath by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf(false) }
    val recoverSender = remember { mutableStateOf<((IntentSender) -> Unit)?>(null) }
    val approval = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingDelete && Build.VERSION.SDK_INT >= 30) { if (connection?.player?.currentMediaItem?.mediaId == metadata.id) connection.service.closeLuneraPlayback(); vm.deleted() }
            else if (pendingDelete) vm.delete(uri) { recoverSender.value?.invoke(it) }
            else vm.update(uri, requestedPath) { recoverSender.value?.invoke(it) }
        } else vm.failure("Permission was not granted. Your file was not changed.")
    }
    recoverSender.value = { approval.launch(IntentSenderRequest.Builder(it).build()) }
    var legacyOperation by remember { mutableStateOf<(() -> Unit)?>(null) }
    val legacyWrite = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) legacyOperation?.invoke() else vm.failure("Storage permission is required to change this audio file")
        legacyOperation = null
    }
    fun withLegacyWrite(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            legacyOperation = action
            legacyWrite.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else action()
    }
    fun write(path: String? = null) {
        requestedPath = path; pendingDelete = false
        if (Build.VERSION.SDK_INT >= 30) runCatching { approval.launch(IntentSenderRequest.Builder(MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender).build()) }.onFailure { vm.failure(it.message ?: "Could not request permission.") }
        else withLegacyWrite { vm.update(uri, path) { recoverSender.value?.invoke(it) } }
    }
    LaunchedEffect(metadata.id) { vm.begin(metadata) }
    LaunchedEffect(state.done) { if (state.done) { connection?.service?.refreshLuneraMetadata(metadata.id); vm.consumeDone(); onDismiss() } }
    if (tempo) TempoPitchDialog { tempo = false }
    if (addPlaylist) org.grimmoir.lunera.ui.screens.library.LocalAddToPlaylistDialog(listOf(metadata.id)) { addPlaylist = false }
    if (addQueue) LocalSavedQueues(metadata.id) { addQueue = false }
    if (info) IosPageSheet(title = "Song info", onDismiss = { info = false }) { org.grimmoir.lunera.ui.utils.ShowMediaInfo(metadata.id) }
    if (edit) IosPageSheet(title = "Edit tags", onDismiss = { edit = false }) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = state.title, onValueChange = vm::title, label = { Text("Title") })
            OutlinedTextField(value = state.artist, onValueChange = vm::artist, label = { Text("Artist") })
            OutlinedTextField(value = state.album, onValueChange = vm::album, label = { Text("Album") })
            state.error?.let { Text(it) }
            Button(onClick = { write() }, enabled = !state.busy && state.title.isNotBlank()) { Text(if (state.busy) "Saving…" else "Save tags") }
        }
    }
    if (move) {
        val foldersVm: MusicFolderPickerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val folders by foldersVm.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { foldersVm.load() }
        IosPageSheet(title = "Move to folder", onDismiss = { move = false }) {
            Text("Choose a destination on the same storage volume.", modifier = Modifier.padding(20.dp))
            state.error?.let { Text(it, modifier = Modifier.padding(20.dp)) }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(folders.folders.filter { it.uri.scheme == "lunera-folder" && it.uri.authority == uri.pathSegments.firstOrNull() }, key = { it.uri.toString() }) { folder ->
                    IosSettingsGroup(null, listOf(IosSettingsItem(title = { Text(LuneraFolders.label(folder.uri)) }, enabled = !state.busy, onClick = { write(folder.uri.path.orEmpty()) })))
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete song?") }, text = { Text("Delete ${metadata.title} from device storage? This cannot be undone.") }, confirmButton = { TextButton(onClick = {
        confirmDelete = false; pendingDelete = true
        if (Build.VERSION.SDK_INT >= 30) runCatching { approval.launch(IntentSenderRequest.Builder(MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender).build()) }.onFailure { vm.failure(it.message ?: "Could not request deletion.") }
        else withLegacyWrite { vm.delete(uri) { recoverSender.value?.invoke(it) } }
    }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    Column((if (playerMenu) Modifier.width(232.dp).clip(RoundedCornerShape(20.dp)).background(AppTheme.colorScheme.surface) else Modifier)
        .heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        if (!playerMenu) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(metadata.title, style = AppTheme.typography.titleSmall, maxLines = 2)
                Text(metadata.artists.joinToString { it.name }, style = AppTheme.typography.bodySmall, maxLines = 1)
            }
            androidx.compose.material3.IconToggleButton(checked = song?.song?.liked == true, onCheckedChange = { song?.let { row -> database.query { update(row.song.localToggleLike()) } } }) {
                SymbolIcon(sf(if (song?.song?.liked == true) "heart.fill" else "heart"), if (song?.song?.liked == true) "Remove from Favourites" else "Add to Favourites", tint = if (song?.song?.liked == true) AppTheme.colorScheme.primary else AppTheme.colorScheme.onSurface)
            }
        }
        state.error?.let { Text(it, modifier = Modifier.padding(16.dp)) }
        if (playerMenu) CompactPlayerActions(listOf(
            IosMenuItemData(title = { Text("Play speed & pitch") }, icon = { SymbolIcon(sf("slider.horizontal.3"), null) }, onClick = { tempo = true }),
            IosMenuItemData(title = { Text("Set as ringtone") }, icon = { SymbolIcon(sf("bell"), null) }, onClick = { ringtone.showTrimmer(metadata.id, metadata.title, metadata.artists.joinToString { it.name }, metadata.duration); onDismiss() }),
            IosMenuItemData(title = { Text("Edit tags") }, icon = { SymbolIcon(sf("pencil"), null) }, onClick = { edit = true }),
            IosMenuItemData(title = { Text("Move to a folder") }, icon = { SymbolIcon(sf("folder"), null) }, onClick = { move = true }),
            IosMenuItemData(title = { Text("Delete song") }, icon = { SymbolIcon(sf("trash"), null) }, onClick = { confirmDelete = true })
        )) else IosMenuGroup(listOf(
            IosMenuItemData(title = { Text("Song info") }, icon = { SymbolIcon(sf("info.circle"), null) }, onClick = { info = true }),
            IosMenuItemData(title = { Text("Play after current song") }, icon = { SymbolIcon(sf("text.line.first.and.arrowtriangle.forward"), null) }, onClick = { connection?.playNext(metadata.toMediaItem()); onDismiss() }),
            IosMenuItemData(title = { Text("Add to currently playing queue") }, icon = { SymbolIcon(sf("text.append"), null) }, onClick = { connection?.addToQueue(metadata.toMediaItem()); onDismiss() }),
            IosMenuItemData(title = { Text("Add to a queue") }, icon = { SymbolIcon(sf("list.bullet"), null) }, onClick = { addQueue = true }),
            IosMenuItemData(title = { Text("Add to playlists") }, icon = { SymbolIcon(sf("text.badge.plus"), null) }, onClick = { addPlaylist = true }),
            IosMenuItemData(title = { Text("Edit tags") }, icon = { SymbolIcon(sf("pencil"), null) }, onClick = { edit = true }),
            IosMenuItemData(title = { Text("Move to a folder") }, icon = { SymbolIcon(sf("folder"), null) }, onClick = { move = true }),
            IosMenuItemData(title = { Text("Share") }, icon = { SymbolIcon(sf("square.and.arrow.up"), null) }, onClick = {
                runCatching {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = context.contentResolver.getType(uri) ?: "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = android.content.ClipData.newUri(context.contentResolver, metadata.title, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Share song"))
                }.onFailure { vm.failure("No app is available to share this song") }
            }),
            IosMenuItemData(title = { Text("Delete permanently") }, icon = { SymbolIcon(sf("trash"), null) }, onClick = { confirmDelete = true })
        ))
    }
}

/** A small solid panel; shared by the classic and V2 player menu hosts. */
@Composable
private fun CompactPlayerActions(items: List<IosMenuItemData>) {
    CompositionLocalProvider(
        LocalContentColor provides AppTheme.colorScheme.onSurface,
    ) {
        Column {
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(onClick = { item.onClick?.invoke() })
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalSymbolIconSize provides 20.dp) { item.icon?.invoke() }
                    Spacer(Modifier.width(10.dp))
                    ProvideTextStyle(AppTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = AppTheme.colorScheme.onSurface)) { item.title() }
                }
            }
        }
}
}
