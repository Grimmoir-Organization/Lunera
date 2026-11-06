package org.grimmoir.lunera.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.utils.*
import org.grimmoir.lunera.ui.screens.library.LocalMusicViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun LocalFoldersSettingsScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: LocalMusicViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val raw by context.dataStore.data.collectAsStateWithLifecycle(initialValue = androidx.datastore.preferences.core.emptyPreferences())
    val scanning by vm.isScanning.collectAsStateWithLifecycle()
    var roots by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    LaunchedEffect(raw[LuneraFoldersKey]) { roots = LuneraFolders.roots(context) }
    val permission = rememberLocalAudioPermission { }
    var showPicker by remember { mutableStateOf(false) }
    val folderFiles = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); LuneraLibraryMonitor.request(); vm.scanDevice(context) }
    }
    if (showPicker) MusicFolderPicker(onDismiss = { showPicker = false }, onSaved = { vm.scanDevice(context) })
    SettingsPage(navController = navController, title = "Music folders", horizontalPadding = 16.dp, actions = {
        IconButton(onClick = { if (permission.granted) showPicker = true else permission.request() }) { SymbolIcon(sf("plus"), "Choose folders") }
    }) {
        Text("Updates automatically when your music changes.", style = AppTheme.typography.bodyMedium, color = AppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp))
        if (!permission.granted) LocalMusicPermissionPrompt(permission)
        if (roots.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { if (permission.granted) showPicker = true else permission.request() }, modifier = Modifier.fillMaxWidth()) { Text("Choose music folders") }
        }
        roots.groupBy { StorageLabels.volume(it) }.entries.sortedBy { !StorageLabels.primary(it.key) }.forEach { (volume, list) ->
            Spacer(Modifier.height(12.dp))
            IosSettingsGroup(title = StorageLabels.storageName(context, volume), items = list.sortedBy(StorageLabels::folderName).map { uri ->
                val lyricAccess = musicFolderHasAccess(context, uri)
                IosSettingsItem(
                    leadingContent = { SymbolIcon(sf("folder.fill"), null, tint = AppTheme.colorScheme.primary) },
                    title = { Text(StorageLabels.folderName(uri), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    description = { Text(StorageLabels.folderSubtitle(uri) + if (lyricAccess) "\nAutomatic lyrics enabled" else "\nAllow lyric files for automatic import", maxLines = 3, overflow = TextOverflow.Ellipsis) },
                    trailingContent = { Row {
                        IconButton(onClick = { folderFiles.launch(musicFolderDocument(uri)) }) { SymbolIcon(sf("folder.badge.plus"), "Allow files beside songs") }
                        IconButton(onClick = { scope.launch { LuneraFolders.remove(context, uri); LuneraLibraryMonitor.request(); vm.scanDevice(context) } }) { SymbolIcon(sf("minus.circle"), "Remove ${StorageLabels.folderName(uri)}") }
                    } }
                )
            })
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (scanning) "Updating library…" else "${roots.size} folders selected", style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = { vm.scanDevice(context) }, enabled = !scanning) { Text("Rescan now") }
        }
    }
}
