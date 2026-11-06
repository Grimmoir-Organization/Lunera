package org.grimmoir.lunera.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/** One debounced refresh pipeline for the whole process, including background playback. */
object LuneraLibraryMonitor {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false
    private val watched = mutableSetOf<android.net.Uri>()
    private val observers = mutableListOf<ContentObserver>()
    fun request() { requests.trySend(Unit) }
    @OptIn(FlowPreview::class)
    @Synchronized fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { request() } }
        observers += observer
        val uris = mutableListOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Files.getContentUri("external"))
        if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.getExternalVolumeNames(app).forEach { volume -> uris += MediaStore.Audio.Media.getContentUri(volume); uris += MediaStore.Files.getContentUri(volume) }
        uris.distinct().forEach { uri -> runCatching { app.contentResolver.registerContentObserver(uri, true, observer); watched += uri } }
        scope.launch {
            app.dataStore.data.map { it[LuneraFoldersKey] }.distinctUntilChanged().collect { request() }
        }
        scope.launch {
            var lastScanAt = -30_000L
            requests.receiveAsFlow().debounce(1500).collect {
                // Files notifications include unrelated downloads. Coalesce bursts to at most
                // one full library/lyric scan per 30 seconds, retaining a trailing refresh.
                val waitMs = 30_000L - (android.os.SystemClock.elapsedRealtime() - lastScanAt)
                if (waitMs > 0) delay(waitMs)
                val permission = if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED) {
                    runCatching {
                        app.contentResolver.persistedUriPermissions.filter { it.isReadPermission }.forEach { grant ->
                            runCatching {
                                val uri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(grant.uri, android.provider.DocumentsContract.getTreeDocumentId(grant.uri))
                                if (uri !in watched) { app.contentResolver.registerContentObserver(uri, true, observer); watched += uri }
                            }
                        }
                        val db = EntryPointAccessors.fromApplication(app, LuneraLibraryAccess::class.java).database()
                        lastScanAt = android.os.SystemClock.elapsedRealtime()
                        LocalAudioScanner.scanAndInsert(app, db)
                    }.onFailure { if (it is CancellationException) throw it }
                }
            }
        }
        request()
    }
}
