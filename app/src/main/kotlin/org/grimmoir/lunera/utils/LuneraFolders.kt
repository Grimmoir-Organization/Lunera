package org.grimmoir.lunera.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import org.json.JSONArray

val LuneraFoldersKey = stringPreferencesKey("lunera_selected_trees")
object LuneraFolders {
    suspend fun roots(context: Context): List<Uri> {
        val raw = context.dataStore.data.first()[LuneraFoldersKey] ?: "[]"
        val grants = context.contentResolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri }.toSet()
        return runCatching { val a = JSONArray(raw); (0 until a.length()).map { Uri.parse(a.getString(it)) }.filter { it.scheme == "lunera-folder" || it in grants } }.getOrDefault(emptyList())
    }
    suspend fun add(context: Context, uri: Uri) {
        if (uri.scheme != "lunera-folder") context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val next = roots(context).plus(uri).distinct()
        context.dataStore.edit { it[LuneraFoldersKey] = JSONArray(next.map(Uri::toString)).toString() }
    }
    suspend fun remove(context: Context, uri: Uri) {
        val next = roots(context).minus(uri)
        context.dataStore.edit { it[LuneraFoldersKey] = JSONArray(next.map(Uri::toString)).toString() }
        runCatching { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    suspend fun replace(context: Context, selected: List<Uri>) {
        context.dataStore.edit { it[LuneraFoldersKey] = JSONArray(selected.distinct().map(Uri::toString)).toString() }
    }
    fun label(uri: Uri): String = StorageLabels.folderName(uri)
    fun includes(roots: List<Uri>, collection: Uri, path: String): Boolean = roots.any { root ->
        if (root.scheme == "lunera-folder") {
            val volume = collection.pathSegments.firstOrNull().orEmpty()
            val expected = root.authority.orEmpty()
            return@any (volume == expected || expected == "primary" && volume == "external_primary") && withinSelectedFolder(path, root.path.orEmpty())
        }
        val doc = runCatching { DocumentsContract.getTreeDocumentId(root) }.getOrNull() ?: return@any false
        val volume = doc.substringBefore(':')
        val relative = doc.substringAfter(':', "").trim('/')
        val actual = if (Build.VERSION.SDK_INT >= 29) path.trim('/') else path.trim('/').removePrefix(if (volume == "primary") "storage/emulated/0/" else "storage/$volume/").trim('/')
        val matchesVolume = Build.VERSION.SDK_INT < 29 || collection.pathSegments.firstOrNull().let { it.equals(volume, true) || volume == "primary" && it == "external_primary" }
        matchesVolume && withinSelectedFolder(actual, relative)
    }
}
internal fun withinSelectedFolder(path: String, root: String): Boolean = root.isNotEmpty() && (path.trim('/') == root.trim('/') || path.trim('/').startsWith(root.trim('/') + "/"))
