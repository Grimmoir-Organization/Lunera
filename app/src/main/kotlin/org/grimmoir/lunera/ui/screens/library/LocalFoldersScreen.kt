package org.grimmoir.lunera.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.grimmoir.lunera.LocalPlayerAwareWindowInsets
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.utils.LocalFolderIndex
import androidx.compose.ui.platform.LocalContext

@Composable
fun LocalMusicFolderRow(folder: LocalFolderIndex.Folder, navController: NavController) {
    Row(
        Modifier.fillMaxWidth().clickable { navController.navigate("local_folder/" + Uri.encode(folder.path)) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SymbolIcon(symbol = sf("folder.fill"), contentDescription = null, size = 28.dp, tint = AppTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = AppTheme.typography.bodyLarge)
            Text(folder.path, style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant)
        }
        Text(folder.songIds.size.toString(), color = AppTheme.colorScheme.onSurfaceVariant)
        SymbolIcon(symbol = sf("chevron.right"), contentDescription = "Open ${folder.name}", size = 16.dp)
    }
}

@Composable
fun LocalMusicFoldersEntry(count: Int?, navController: NavController) {
    Row(
        Modifier.fillMaxWidth().clickable { navController.navigate("library") }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SymbolIcon(symbol = sf("folder.fill"), contentDescription = null, tint = AppTheme.colorScheme.primary)
        Text("Folders", style = AppTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (count != null) Text(count.toString(), color = AppTheme.colorScheme.onSurfaceVariant)
        SymbolIcon(symbol = sf("chevron.right"), contentDescription = "Browse local folders", size = 16.dp)
    }
}

@Composable
fun LocalFoldersScreen(navController: NavController, viewModel: LocalMusicViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val groups by viewModel.folderGroups.collectAsStateWithLifecycle()
    val scanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val permission = rememberLocalAudioPermission { org.grimmoir.lunera.utils.LuneraLibraryMonitor.request() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Spacer(Modifier.height(12.dp)) }
        if (!permission.granted) item { LocalMusicPermissionPrompt(permission) }
        else {
            groups.forEach { group ->
                item(key = group.id) {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        IosSettingsGroup(title = group.name, items = group.folders.map { folder ->
                            val folderKey = group.id + ":" + org.grimmoir.lunera.utils.StorageLabels.path(folder.uri)
                            IosSettingsItem(
                                leadingContent = { SymbolIcon(sf("folder.fill"), null, tint = AppTheme.colorScheme.primary) },
                                title = { Text(folder.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                description = { Text(folder.path, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                trailingContent = { Text(folder.count.toString(), style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant) },
                                onClick = { navController.navigate("local_folder/" + Uri.encode(folderKey)) }
                            )
                        })
                    }
                }
            }
            if (folders.isEmpty()) item { Text(if (scanning) "Updating library…" else "Choose music folders in Settings → Library", modifier = Modifier.padding(24.dp)) }
        }
    }
}
