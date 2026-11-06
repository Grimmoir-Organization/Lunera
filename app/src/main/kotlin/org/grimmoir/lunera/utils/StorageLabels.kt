package org.grimmoir.lunera.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract

object StorageLabels {
    fun volume(uri: Uri): String = if (uri.scheme == "lunera-folder") uri.authority.orEmpty() else runCatching { DocumentsContract.getTreeDocumentId(uri).substringBefore(':') }.getOrDefault("external_primary")
    fun path(uri: Uri): String = if (uri.scheme == "lunera-folder") uri.path.orEmpty().trim('/') else runCatching { DocumentsContract.getTreeDocumentId(uri).substringAfter(':', "").trim('/') }.getOrDefault("")
    fun folderName(uri: Uri): String = path(uri).substringAfterLast('/').ifBlank { "Music" }
    fun primary(id: String) = id in setOf("primary", "external", "external_primary")
    fun canonical(uri: Uri): Uri = if (uri.scheme == "lunera-folder") uri else Uri.Builder().scheme("lunera-folder").authority(if (primary(volume(uri))) "external_primary" else volume(uri).lowercase()).path(path(uri)).build()
    fun storageName(context: Context, id: String): String {
        if (primary(id)) return "Internal storage"
        val volumes = context.getSystemService(StorageManager::class.java).storageVolumes
        val volume = volumes.firstOrNull { it.uuid.equals(id, true) || Build.VERSION.SDK_INT >= 30 && it.mediaStoreVolumeName.equals(id, true) }
        val name = volume?.getDescription(context).orEmpty().trim()
        return name.takeIf { it.isNotBlank() && !it.equals(id, true) && !it.matches(Regex("[0-9a-fA-F-]+")) } ?: if (volume?.isRemovable != false) "SD card" else "Storage"
    }
    fun folderSubtitle(uri: Uri): String = path(uri).let { if (it.isBlank()) "/" else "$it/" }
}
