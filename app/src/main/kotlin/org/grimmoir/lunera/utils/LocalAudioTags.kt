package org.grimmoir.lunera.utils

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalAudioTags {
    fun file(context: Context, uri: Uri): File? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0)?.let(::File) else null
        }
    }.getOrNull()

    suspend fun lyrics(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val source = file(context, uri) ?: return@withContext null
        // Prefer a matching local LRC, then the embedded lyrics tag. No network lookup.
        val sidecar = File(source.parentFile, source.nameWithoutExtension + ".lrc")
        runCatching { if (sidecar.length() in 1..2_000_000) sidecar.readText() else null }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: runCatching { AudioFileIO.read(source).tag?.getFirst(FieldKey.LYRICS) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun write(context: Context, uri: Uri, title: String, artist: String, album: String) = withContext(Dispatchers.IO) {
        require(title.isNotBlank()) { "A song title is required." }
        val name = context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: error("Audio file not found.")
        val extension = name.substringAfterLast('.', "").lowercase()
        require(extension in setOf("mp3", "flac", "ogg", "opus", "m4a", "mp4", "wav", "aiff", "aif", "wma")) { "Tag editing is not supported for this audio format." }
        val directory = File(context.cacheDir, "tag-edit").apply { mkdirs() }
        val backup = File.createTempFile("original-", ".$extension", directory)
        val edited = File.createTempFile("edited-", ".$extension", directory)
        var writing = false
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> backup.outputStream().use { input.copyTo(it) } }
            backup.copyTo(edited, overwrite = true)
            val audio = AudioFileIO.read(edited)
            val tag = audio.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, title.trim())
            tag.setField(FieldKey.ARTIST, artist.trim())
            tag.setField(FieldKey.ALBUM, album.trim())
            AudioFileIO.write(audio)
            // Validate the newly tagged copy before touching the original.
            check(AudioFileIO.read(edited).tag.getFirst(FieldKey.TITLE) == title.trim()) { "The edited tag could not be verified." }
            val out = requireNotNull(context.contentResolver.openOutputStream(uri, "rwt"))
            writing = true
            out.use { output -> edited.inputStream().use { it.copyTo(output) } }
            writing = false
            val values = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, title.trim()); put(MediaStore.Audio.Media.ARTIST, artist.trim()); put(MediaStore.Audio.Media.ALBUM, album.trim()) }
            runCatching { context.contentResolver.update(uri, values, null, null) }
            file(context, uri)?.let { android.media.MediaScannerConnection.scanFile(context, arrayOf(it.path), null, null) }
            backup.delete()
        } catch (failure: Exception) {
            if (writing) runCatching {
                requireNotNull(context.contentResolver.openOutputStream(uri, "rwt")).use { out -> backup.inputStream().use { it.copyTo(out) } }
                backup.delete()
            }
            throw failure
        } finally { edited.delete() }
    }
}
