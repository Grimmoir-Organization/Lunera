package org.grimmoir.lunera.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.db.entities.LyricsEntity
import org.grimmoir.lunera.utils.LocalLyricFiles
import org.grimmoir.lunera.utils.LocalLyricsTiming

data class LocalLyricsEditorState(val id: String = "", val text: String = "", val nextLine: Int = 0, val message: String? = null, val saving: Boolean = false)
@HiltViewModel
class LocalLyricsEditorViewModel @Inject constructor(private val database: MusicDatabase) : ViewModel() {
    private val mutable = MutableStateFlow(LocalLyricsEditorState())
    val state = mutable.asStateFlow()
    fun load(id: String, text: String) { if (mutable.value.id != id) mutable.value = LocalLyricsEditorState(id, text.takeUnless { it == LyricsEntity.LYRICS_NOT_FOUND }.orEmpty()) }
    fun edit(text: String) { mutable.value = mutable.value.copy(text = text, nextLine = 0, message = null) }
    fun restart() { mutable.value = mutable.value.copy(nextLine = 0, message = null) }
    fun stamp(position: Long) {
        val current = mutable.value
        val lines = current.text.lines().toMutableList()
        val index = (current.nextLine until lines.size).firstOrNull { lines[it].isNotBlank() } ?: return
        lines[index] = LocalLyricsTiming.stamp(lines[index], position)
        mutable.value = current.copy(text = lines.joinToString("\n"), nextLine = index + 1, message = null)
    }
    fun import(context: Context, uri: Uri) {
        val songId = mutable.value.id
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { LocalLyricsTiming.normalize(LocalLyricFiles.readText(context, uri)) }
                .onSuccess { text -> mutable.update { current -> if (current.id == songId) current.copy(text = text, nextLine = 0, message = null) else current } }
                .onFailure { failure -> mutable.update { current -> if (current.id == songId) current.copy(message = failure.message ?: "Unable to open lyrics.") else current } }
        }
    }
    fun save() {
        val current = mutable.value
        if (current.id.isBlank() || current.text.isBlank()) return
        mutable.value = current.copy(saving = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { database.upsert(LyricsEntity(current.id, LocalLyricsTiming.normalize(current.text), "Manual")) }
                .onSuccess { mutable.update { latest -> if (latest.id == current.id) latest.copy(message = "Lyrics saved", saving = false) else latest } }
                .onFailure { mutable.update { latest -> if (latest.id == current.id) latest.copy(message = "Could not save lyrics", saving = false) else latest } }
        }
    }
}
