package org.grimmoir.lunera.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.utils.*

data class FolderChoice(val uri: Uri, val count: Int, val name: String = StorageLabels.folderName(uri), val path: String = StorageLabels.folderSubtitle(uri))
data class StorageFolderGroup(val id: String, val name: String, val folders: List<FolderChoice>)
data class FolderPickerState(val folders: List<FolderChoice> = emptyList(), val selected: Set<Uri> = emptySet(), val loading: Boolean = true, val saving: Boolean = false, val error: String? = null, val groups: List<StorageFolderGroup> = emptyList())
@HiltViewModel
class MusicFolderPickerViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private val mutable = MutableStateFlow(FolderPickerState())
    val state = mutable.asStateFlow()
    fun load() { viewModelScope.launch {
        mutable.value = mutable.value.copy(loading = true, error = null)
        runCatching {
            withContext(Dispatchers.IO) {
                val roots = LuneraFolders.roots(context).map(StorageLabels::canonical).toSet()
                val counts = linkedMapOf<Uri, Int>()
                val collections = if (Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(context).map { MediaStore.Audio.Media.getContentUri(it) } else listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                for (collection in collections) {
                    context.contentResolver.query(collection, arrayOf(LocalFolderIndex.pathColumn), "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%' OR ${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val path = cursor.getString(0)?.let(LocalFolderIndex::folderPathOf).orEmpty()
                            if (path.isBlank()) continue
                            val uri = Uri.Builder().scheme("lunera-folder").authority(collection.pathSegments.first()).path(path).build()
                            counts[uri] = (counts[uri] ?: 0) + 1
                        }
                    }
                }
                val choices = (counts.keys + roots).distinct().map { FolderChoice(it, counts[it] ?: 0) }.sortedBy { it.name.lowercase() }
                val groups = choices.groupBy { StorageLabels.volume(it.uri) }.map { (id, list) -> StorageFolderGroup(id, StorageLabels.storageName(context, id), list) }.sortedWith(compareBy<StorageFolderGroup> { !StorageLabels.primary(it.id) }.thenBy { it.name })
                FolderPickerState(choices, roots, loading = false, groups = groups)
            }
        }.onSuccess { mutable.value = it }.onFailure { mutable.value = mutable.value.copy(loading = false, error = "Allow music access to choose folders.") }
    } }
    fun toggle(uri: Uri) { val current = mutable.value; if (current.loading || current.saving) return; mutable.value = current.copy(selected = if (uri in current.selected) current.selected - uri else current.selected + uri) }
    fun save(done: () -> Unit) { if (mutable.value.saving) return; viewModelScope.launch {
        mutable.value = mutable.value.copy(saving = true)
        runCatching { LuneraFolders.replace(context, mutable.value.selected.toList()) }
            .onSuccess { LuneraLibraryMonitor.request(); mutable.value = mutable.value.copy(saving = false); done() }
            .onFailure { mutable.value = mutable.value.copy(saving = false, error = "Could not save your folders. Try again.") }
    } }
}
@Composable
fun MusicFolderPicker(onDismiss: () -> Unit, onSaved: () -> Unit, vm: MusicFolderPickerViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    IosPageSheet(title = "Choose music folders", onDismiss = onDismiss) {
        Text("${state.selected.size} selected · Subfolders included", style = AppTheme.typography.bodyMedium, color = AppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp))
        Text("Done saves your music folders. Use Allow lyric files beside a selected folder in Library settings to enable automatic lyrics.", style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
        state.error?.let { Text(it, modifier = Modifier.padding(20.dp)) }
        if (state.loading) Text("Finding music folders…", modifier = Modifier.padding(24.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.groups.forEach { group ->
                item(key = "storage-${group.id}") {
                    IosSettingsGroup(title = group.name, items = group.folders.map { folder ->
                        IosSettingsItem(
                            leadingContent = { SymbolIcon(sf("folder.fill"), null, tint = AppTheme.colorScheme.primary) },
                            title = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            description = { Text("${folder.path} · ${folder.count} songs", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { vm.toggle(folder.uri) },
                            trailingContent = { SymbolIcon(sf(if (folder.uri in state.selected) "checkmark.circle.fill" else "circle"), if (folder.uri in state.selected) "Selected" else "Not selected", tint = if (folder.uri in state.selected) AppTheme.colorScheme.primary else AppTheme.colorScheme.onSurfaceVariant) }
                        )
                    })
                }
            }
        }
        Button(onClick = { vm.save { onSaved(); onDismiss() } }, enabled = !state.loading && !state.saving, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding()) { Text(if (state.saving) "Saving…" else "Done · ${state.selected.size} folders") }
    }
}

internal fun musicFolderDocument(folder: Uri): Uri {
    if (folder.scheme != "lunera-folder") return folder
    val volume = folder.authority.orEmpty().let { if (StorageLabels.primary(it)) "primary" else it }
    return android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "$volume:${folder.path.orEmpty().trim('/')}")
}
internal fun musicFolderHasAccess(context: Context, folder: Uri): Boolean {
    val target = StorageLabels.canonical(folder)
    return context.contentResolver.persistedUriPermissions.any { grant ->
        if (!grant.isReadPermission || !android.provider.DocumentsContract.isTreeUri(grant.uri)) false
        else {
            val root = StorageLabels.canonical(grant.uri)
            StorageLabels.volume(root).let { volume ->
                (volume.equals(StorageLabels.volume(target), true) || StorageLabels.primary(volume) && StorageLabels.primary(StorageLabels.volume(target))) &&
                    (root.path.orEmpty().trim('/').isEmpty() || withinSelectedFolder(target.path.orEmpty(), root.path.orEmpty()))
            }
        }
    }
}
