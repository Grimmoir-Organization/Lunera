package org.grimmoir.lunera.utils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.db.entities.LyricsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/** LRC is played by the existing timestamp-aware lyrics renderer; SRT/VTT are normalized to LRC. */
object LocalLyricsTiming {
    private val timestampPattern = Regex("(?m)^\\s*\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]")
    fun hasTimestamps(text: String): Boolean = timestampPattern.containsMatchIn(text)
    fun cleanPlainText(text: String): String = text.lines().mapNotNull { line ->
        val trimmed = line.trim()
        when {
            trimmed.matches(Regex("(?i)^\\d+\\s+contributors?$")) -> null
            trimmed.matches(Regex("(?i)^(you might also like|\\d+embed|embed)$")) -> null
            trimmed.matches(Regex("(?i)^\\d+\\s+contributors?.*lyrics.*")) ->
                Regex("(?i)lyrics").find(trimmed)?.let { trimmed.substring(it.range.last + 1).trim().takeIf(String::isNotEmpty) }
            else -> line
        }
    }.joinToString("\n").trim()

    fun timestamp(milliseconds: Long): String {
        val time = milliseconds.coerceAtLeast(0)
        return "[%02d:%02d.%02d]".format(java.util.Locale.US, time / 60000, time / 1000 % 60, time / 10 % 100)
    }
    fun stamp(line: String, milliseconds: Long): String = timestamp(milliseconds) + line.replace(Regex("^(?:\\[\\d+:\\d+(?:[.,]\\d+)?])+"), "")
    fun normalize(text: String): String {
        val clean = text.removePrefix("\uFEFF").replace("\r\n", "\n")
        if (!clean.contains("-->")) return if (hasTimestamps(clean)) clean else cleanPlainText(clean)
        val cue = Regex("(?:(\\d{1,3}):)?(\\d{2}):(\\d{2})[.,](\\d{1,3})\\s*-->")
        val output = mutableListOf<String>()
        var active: Long? = null
        for (line in clean.lines()) {
            val match = cue.find(line)
            when {
                match != null -> {
                    val parts = match.groupValues
                    active = ((parts[1].toLongOrNull() ?: 0) * 3600 + parts[2].toLong() * 60 + parts[3].toLong()) * 1000 + parts[4].padEnd(3, '0').take(3).toLong()
                }
                line.isBlank() -> active = null
                active != null -> output += timestamp(active) + line.replace(Regex("<[^>]*>"), "").trim()
            }
        }
        return output.joinToString("\n").ifBlank { clean }
    }
}

data class LocalLyricAudio(val id: String, val name: String, val folder: String, val volume: String, val title: String = "")
class LocalLyricFiles(private val context: Context) {
    private data class Child(val id: String, val name: String)
    private val listings = mutableMapOf<String, List<Child>>()
    private val directoryListings = mutableMapOf<String, List<String>>()
    private val matchIndexes = mutableMapOf<String, LyricFileMatcher.Index>()
    private fun children(tree: Uri, document: String): List<Child> = listings.getOrPut(tree.toString()+document) {
        runCatching {
            val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, document)
            context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                buildList { while (c.moveToNext()) add(Child(c.getString(0), c.getString(1))) }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
    fun read(audio: LocalLyricAudio): String? {
        val extensions = listOf("lrc", "srt", "vtt", "txt")
        val base = audio.name.substringBeforeLast('.', audio.name)
        val directory = if (audio.folder.startsWith("/storage/")) File(audio.folder) else if (StorageLabels.primary(audio.volume)) File(android.os.Environment.getExternalStorageDirectory(), audio.folder) else File("/storage/${audio.volume}", audio.folder)
        val localNames = directoryListings.getOrPut(directory.absolutePath) {
            runCatching { directory.list()?.toList().orEmpty() }.getOrDefault(emptyList())
        }
        val localIndex = matchIndexes.getOrPut("file:" + directory.absolutePath) { LyricFileMatcher.Index(localNames) }
        for (name in localIndex.candidates(audio.name).ifEmpty { if (audio.title.isNotBlank()) localIndex.candidates(audio.title + ".mp3") else emptyList() }) {
            val local = File(directory, name)
            runCatching { if (local.isFile && local.length() in 1..2_000_000 && local.canRead())
                LocalLyricsTiming.normalize(readText(context, Uri.fromFile(local))) else null
            }.getOrNull()?.let { return it }
        }
        for (grant in context.contentResolver.persistedUriPermissions.filter { it.isReadPermission && DocumentsContract.isTreeUri(it.uri) }) {
            val tree = grant.uri
            val root = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
            val volume = root.substringBefore(':')
            if (!(volume.equals(audio.volume, true) || StorageLabels.primary(volume) && StorageLabels.primary(audio.volume))) continue
            val audioFolder = audio.folder.trim('/').removePrefix(if (StorageLabels.primary(audio.volume)) "storage/emulated/0/" else "storage/${audio.volume}/").trim('/')
            val folder = root.substringAfter(':', "").trim('/')
            if (folder.isNotBlank() && !withinSelectedFolder(audioFolder, folder)) continue
            val suffix = audioFolder.removePrefix(folder).trim('/')
            var doc = root
            var valid = true
            for (part in suffix.split('/').filter { it.isNotBlank() }) {
                val next = children(tree, doc).firstOrNull { it.name == part }
                if (next == null) { valid = false; break }; doc = next.id
            }
            if (!valid) continue
            val files = children(tree, doc)
            val index = matchIndexes.getOrPut("tree:" + tree.toString() + doc) { LyricFileMatcher.Index(files.map { it.name }) }
            for (name in index.candidates(audio.name).ifEmpty { if (audio.title.isNotBlank()) index.candidates(audio.title + ".mp3") else emptyList() }) {
                val file = files.first { it.name == name }
                runCatching { readText(context, DocumentsContract.buildDocumentUriUsingTree(tree, file.id)) }.getOrNull()?.let { return LocalLyricsTiming.normalize(it) }
            }
        }
        return null
    }
    companion object {
        fun readText(context: Context, uri: Uri): String {
            val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size()+count <= 2_000_000) { "Lyric file is too large." }; output.write(buffer, 0, count) }; output.toByteArray()
            }
            val charset = if (bytes.size>=2 && bytes[0]==0xFF.toByte() && bytes[1]==0xFE.toByte()) Charsets.UTF_16LE else if(bytes.size>=2 && bytes[0]==0xFE.toByte() && bytes[1]==0xFF.toByte()) Charsets.UTF_16BE else Charsets.UTF_8
            return String(bytes, charset).removePrefix("\uFEFF")
        }
        suspend fun forSong(context: Context, id: String): String? = withContext(Dispatchers.IO) {
            runCatching {
                val uri=Uri.parse(id)
                val projection=arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, LocalFolderIndex.pathColumn, MediaStore.Audio.Media.TITLE)
                context.contentResolver.query(uri,projection,null,null,null)?.use { c ->
                    if (!c.moveToFirst()) null else LocalLyricFiles(context).read(LocalLyricAudio(id,c.getString(0),LocalFolderIndex.folderPathOf(c.getString(1)),uri.pathSegments.firstOrNull().orEmpty(), c.getString(2).orEmpty()))
                }
            }.getOrNull()
        }
        suspend fun sync(context: Context, database: MusicDatabase, audio: List<LocalLyricAudio>) {
            val files = LocalLyricFiles(context)
            for (song in audio) {
                val existing = database.lyrics(song.id).first()
                if (existing?.provider?.startsWith("Manual") == true) continue
                val text=files.read(song)?.takeIf { it.isNotBlank() } ?: continue
                if (existing?.lyrics != text || existing?.provider != "Local file") database.withTransaction {
                    // An edit may have been saved while the adjacent file was read.
                    val latest = lyrics(song.id).first()
                    if (latest?.provider?.startsWith("Manual") != true) upsert(LyricsEntity(song.id, text, "Local file"))
                }
            }
        }
    }
}
