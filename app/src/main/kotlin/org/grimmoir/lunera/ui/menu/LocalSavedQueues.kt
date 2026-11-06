/** Copyright (C) 2026 Grimmoir Organization. SPDX-License-Identifier: GPL-3.0-only */
package org.grimmoir.lunera.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import org.grimmoir.lunera.ui.ios.compat.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.grimmoir.lunera.LocalDatabase
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.playback.queues.ListQueue

/** Separate stored queues; editing one never replaces the currently playing queue. */
@Composable
fun LocalSavedQueues(songIdToAdd: String? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = LocalDatabase.current.get()
    val connection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("lunera_saved_queues", android.content.Context.MODE_PRIVATE) }
    var names by remember { mutableStateOf(prefs.all.keys.sorted()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun ids(name: String) = runCatching { JSONArray(prefs.getString(name, "[]")).let { a -> (0 until a.length()).map { a.getString(it) } } }.getOrDefault(emptyList())
    fun add(name: String) {
        val id = songIdToAdd ?: return
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { prefs.edit().putString(name, JSONArray((ids(name) + id).distinct()).toString()).commit() }
            busy = false
            if (result) onDismiss() else error = "Could not save queue"
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (songIdToAdd == null) "Saved queues" else "Add to a queue") }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            if (songIdToAdd != null) item { TextButton(enabled = !busy, onClick = { var n = 1; while ("Queue $n" in names) n++; add("Queue $n") }) { Text("Create queue") } }
            if (names.isEmpty() && songIdToAdd == null) item { Text("Use a song’s options to create a saved queue") }
            items(names, key = { it }) { name ->
                Row(Modifier.fillMaxWidth()) {
                    TextButton(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                        if (songIdToAdd != null) add(name) else scope.launch {
                            val songs = withContext(Dispatchers.IO) { ids(name).mapNotNull { db.song(it).first() }.filter { it.song.isLocal } }
                            if (songs.isNotEmpty()) { connection?.playQueue(ListQueue(name, songs.map { it.toMediaItem() })); onDismiss() } else error = "This queue has no available songs"
                        }
                    }) { Text(name) }
                    if (songIdToAdd == null) TextButton(onClick = { prefs.edit().remove(name).apply(); names = names - name }) { Text("Delete") }
                }
            }
            error?.let { item { Text(it) } }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
