/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.utils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Groups on-device audio by the folder it lives in.
 *
 * The song table has no path column (an old migration dropped `localPath`), so
 * folders are read straight from MediaStore instead of the database. Song ids
 * are built exactly the way [LocalAudioScanner] builds them — the content URI —
 * so a folder's ids can be handed to `songsByIds` to get the scanned rows back.
 */
object LocalFolderIndex {



    data class Folder(
        val path: String,
        val name: String,
        val songIds: List<String>,
    ) {
        val storageId: String get() = songIds.firstOrNull()?.let { android.net.Uri.parse(it).pathSegments.firstOrNull() } ?: "external_primary"
        val relativePath: String get() = path.substringAfter(':', path)
    }

    /**
     * The column to query for a song's folder, and how to turn its raw value into
     * a bare directory path — shared with [LocalAudioScanner] so both derive the
     * exact same folder for the exact same song.
     */
    val pathColumn: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        }

    /** RELATIVE_PATH is already a directory ("Music/Rock/"); DATA is the file
     *  itself, so drop the file name. */
    fun folderPathOf(raw: String): String {
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) raw else raw.substringBeforeLast('/', "")
        return dir.trim('/')
    }

    // Folder contents change only after a rescan or app restart; reload on demand.
    @Volatile
    private var cache: List<Folder> = emptyList()
    val updates = kotlinx.coroutines.flow.MutableStateFlow<List<Folder>>(emptyList())

    suspend fun restore(context: Context) = withContext(Dispatchers.IO) {
        val roots = LuneraFolders.roots(context).map { it.toString() }.sorted().joinToString("|")
        runCatching {
            val json = org.json.JSONObject(java.io.File(context.filesDir, "lunera-folders.json").readText())
            if (json.getString("roots") != roots) return@runCatching
            val a = json.getJSONArray("folders")
            cache = (0 until a.length()).map { i -> val item = a.getJSONObject(i); val ids = item.getJSONArray("ids"); Folder(item.getString("path"), item.getString("name"), (0 until ids.length()).map { ids.getString(it) }) }
            updates.value = cache
        }
    }
    suspend fun load(context: Context): List<Folder> = withContext(Dispatchers.IO) {
        val roots = LuneraFolders.roots(context)
        val projection = arrayOf(MediaStore.Audio.Media._ID, pathColumn)

        // Same predicate as the scanner, so folder counts match what was imported.
        val selection =
            "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%')"
        val selectionArgs: Array<String>? = null

        val collections = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).map { MediaStore.Audio.Media.getContentUri(it) }
        } else {
            listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }

        val byPath = linkedMapOf<String, MutableList<String>>()

        collections.forEach { collection ->
            runCatching {
                context.contentResolver.query(collection, projection, selection, selectionArgs, null)
            }.getOrNull()?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val pathIndex = cursor.getColumnIndexOrThrow(pathColumn)
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(pathIndex) ?: continue
                    val path = folderPathOf(raw)
                    if (path.isEmpty() || !LuneraFolders.includes(roots, collection, path)) continue
                    val id = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)).toString()
                    byPath.getOrPut(collection.pathSegments.first() + ":" + path) { mutableListOf() }.add(id)
                }
            }
        }

        byPath
            .map { (path, ids) -> Folder(path = path, name = path.substringAfter(':').substringAfterLast('/'), songIds = ids) }
            .sortedBy { it.name.lowercase() }
            .also { folders ->
                cache = folders; updates.value = folders
                val json = org.json.JSONObject().put("roots", roots.map { it.toString() }.sorted().joinToString("|"))
                    .put("folders", org.json.JSONArray(folders.map { org.json.JSONObject().put("path", it.path).put("name", it.name).put("ids", org.json.JSONArray(it.songIds)) }))
                val file = android.util.AtomicFile(java.io.File(context.filesDir, "lunera-folders.json"))
                val output = file.startWrite()
                try { output.write(json.toString().toByteArray()); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output) }
            }
    }

    /** Ids for one folder, loading the index first if this process hasn't yet. */
    suspend fun songIdsFor(context: Context, path: String): List<String> =
        (cache.ifEmpty { load(context) }).firstOrNull { it.path == path || it.relativePath == path }?.songIds.orEmpty()
}
