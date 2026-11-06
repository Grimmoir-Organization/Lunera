package org.grimmoir.lunera.ui.menu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.db.entities.LyricsEntity
import org.grimmoir.lunera.db.entities.SongEntity
import org.grimmoir.lunera.models.MediaMetadata
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.viewmodels.LocalLyricsEditorViewModel

@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    songProvider: () -> SongEntity?,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    onShowOffsetDialog: () -> Unit = {},
    viewModel: LocalLyricsEditorViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val connection = LocalPlayerConnection.current
    val metadata = mediaMetadataProvider()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(metadata.id) { viewModel.load(metadata.id, lyricsProvider()?.lyrics.orEmpty()) }
    var editing by remember { mutableStateOf(false) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.import(context, it); editing = true } }
    if (editing) IosPageSheet("Edit lyrics", { editing = false }) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(metadata.title, style = AppTheme.typography.titleMedium)
            Text("Type one line at a time, or import LRC, SRT, VTT or text. Timed lyrics follow playback automatically.", style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = state.text, onValueChange = viewModel::edit, label = { Text("Lyrics") }, modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 12)
            Text("Listen to the song and tap when each line begins.", style = AppTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { connection?.player?.let { if (it.isPlaying) it.pause() else it.play() } }) { Text("Play / pause") }
                TextButton(onClick = { connection?.player?.seekTo(0); viewModel.restart() }) { Text("Restart sync") }
            }
            val next = state.text.lines().drop(state.nextLine).firstOrNull { it.isNotBlank() }
            Button(onClick = { connection?.player?.currentPosition?.let(viewModel::stamp) }, enabled = next != null && connection != null, modifier = Modifier.fillMaxWidth()) { Text(if (next == null) "All lines timed" else "Stamp next line") }
            next?.let { Text(it, style = AppTheme.typography.bodySmall, maxLines = 2) }
            state.message?.let { Text(it, style = AppTheme.typography.bodySmall) }
            Button(onClick = viewModel::save, enabled = state.text.isNotBlank() && !state.saving, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Saving…" else "Save lyrics") }
            Spacer(Modifier.height(32.dp))
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp + menuBottomInset())) {
        IosSettingsGroup(items = listOf(
            IosSettingsItem(title = { Text("Edit & sync lyrics") }, leadingContent = { SymbolIcon(sf("pencil"), null) }, onClick = { editing = true }),
            IosSettingsItem(title = { Text("Import lyric file") }, description = { Text("LRC, SRT, VTT or plain text") }, leadingContent = { SymbolIcon(sf("doc"), null) }, onClick = { import.launch(arrayOf("*/*")) }),
            IosSettingsItem(title = { Text("Timing offset") }, leadingContent = { SymbolIcon(sf("clock"), null) }, onClick = { onDismiss(); onShowOffsetDialog() })
        ))
    }
}
