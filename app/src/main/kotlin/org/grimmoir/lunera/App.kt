/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import android.graphics.Bitmap
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.music.innertube.YouTube
import org.grimmoir.lunera.constants.*
import org.grimmoir.lunera.di.ApplicationScope
import org.grimmoir.lunera.utils.CrashHandler
import org.grimmoir.lunera.utils.DebugLogs
import org.grimmoir.lunera.utils.dataStore
import org.grimmoir.lunera.utils.get
import org.grimmoir.lunera.utils.migrateSharedPreferencesToMMKV
import org.grimmoir.lunera.utils.warmPreferenceMirror
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        // The recovery activity runs in its own process. Do not reopen the database,
        // preferences, native caches or playback integrations that may have crashed.
        val processName = if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()
        } else {
            (getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                .runningAppProcesses?.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
        }
        if (processName == "$packageName:crash") return
        org.grimmoir.lunera.core.UpdateNotificationHelper.cancelUpdateNotification(this)
        // Eclipse addons identify clients by UA; they require "Lunera/<app-version>".

        // StrictMode: debug-only, catches main-thread IO before it ships.
        // Release untouched — zero behavior change there.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }

        // Initialize MMKV before migrating preferences or restoring session state.
        MMKV.initialize(this)
        org.grimmoir.lunera.ui.theme.colorengine.ColorEngine.init(this)
        // Avoid per-artwork diagnostic work in release builds.
        org.grimmoir.lunera.ui.theme.colorengine.ColorEngine.setDebugEnabled(BuildConfig.DEBUG)
        migrateSharedPreferencesToMMKV(this, "widget_config")
        migrateSharedPreferencesToMMKV(this, "nanosonic_eq_profiles")
        migrateSharedPreferencesToMMKV(this, "vivi_eq_prefs")
        migrateSharedPreferencesToMMKV(this, "settings")

        applicationScope.launch(Dispatchers.IO) { dataStore.warmPreferenceMirror() }
        kotlinx.coroutines.runBlocking {
            dataStore.edit {
                it[LocalOnlyModeKey] = true
                it[SimilarContent] = false
                it[AppleArtworkKey] = false
                it[AlbumCanvasEnabledKey] = false
                it[CanvasThumbnailAnimationKey] = false
                it[AutoDjMixingEnabledKey] = false
                it[AutoLoadMoreKey] = false
                if (it[StopMusicOnTaskClearKey] == null) it[StopMusicOnTaskClearKey] = true
            }
        }
        org.grimmoir.lunera.utils.LuneraLibraryWorker.schedule(this)
        org.grimmoir.lunera.utils.LuneraLibraryMonitor.start(this)
        CrashHandler.install(this)



        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(DebugLogs.tree(this))
        }

        // Load the coil cache size on a background thread so the first image
        // render never blocks on a DataStore read.
        applicationScope.launch(Dispatchers.IO) {
            cachedCoilCacheSize = dataStore.data.map { it[MaxImageCacheSizeKey] ?: 512 }.first()
        }

        applicationScope.launch {

        }
    }

    @Volatile private var cachedCoilCacheSize: Int? = null

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Use the startup-seeded value to avoid a synchronous DataStore read on Main.
        val cacheSize = cachedCoilCacheSize ?: 512
        return ImageLoader.Builder(this).apply {
            crossfade(false)
            // Use HARDWARE bitmap configuration for direct zero-copy GPU texture uploads
            allowHardware(true)
            bitmapConfig(Bitmap.Config.HARDWARE)
            // Animated GIF backgrounds (Home background picker allows GIF): the
            // platform decoder needs API 28+, GifDecoder covers everything below.
            components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // User-supplied SVGs (player icons, DIY stickers). Vectors rasterise at the
                // size they are drawn at, so a custom icon never goes soft when scaled up.
                add(SvgDecoder.Factory())
                // Steps YouTube artwork down a size when the requested filename 404s, so
                // asking for maxresdefault is safe on the videos that do not have one.

                // Cover art that lives on an SMB or WebDAV source.

            }
            // Reuse decoded images while scrolling.
            memoryCache {
                // Limit memory use more on low-RAM devices.
                val activityManager = getSystemService(android.app.ActivityManager::class.java)
                val percent = if (activityManager?.isLowRamDevice == true) 0.15 else 0.25
                MemoryCache.Builder()
                    .maxSizePercent(context, percent)
                    .build()
            }
            if (cacheSize == 0) {
                diskCachePolicy(CachePolicy.DISABLED)
            } else {
                diskCache(
                    DiskCache.Builder()
                        // Keep artwork in filesDir: Android can clear cacheDir under storage pressure,
                        // including downloaded thumbnails. Coil still enforces the configured size cap.
                        .directory(filesDir.resolve("coil"))
                        .maxSizeBytes(cacheSize * 1024 * 1024L)
                        .build()
                )
            }
        }.build()
    }

    // Best-effort memory relief. Coil re-fetches from disk cache — no state lost.
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            runCatching { coil3.SingletonImageLoader.get(this).memoryCache?.clear() }
        }
    }

    companion object {
        /** MMKV file backing [authSeed]. Cache only — DataStore owns these values. */
        private const val AUTH_CACHE_ID = "auth_seed"

        suspend fun forgetAccount(context: Context) {
            Timber.d("forgetAccount: Starting logout process")

            Timber.d("forgetAccount: Clearing DataStore preferences")
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
            }
            Timber.d("forgetAccount: DataStore preferences cleared")

            Timber.d("forgetAccount: Clearing YouTube object auth state")
            YouTube.cookie = null
            YouTube.visitorData = null
            YouTube.dataSyncId = null
            Timber.d("forgetAccount: YouTube auth state cleared")

            // Remove WebView cookies so logout does not silently sign back in.
            Timber.d("forgetAccount: Clearing WebView CookieManager")
            withContext(Dispatchers.Main) {
                android.webkit.CookieManager.getInstance().apply {
                    removeAllCookies { removed ->
                        Timber.d("forgetAccount: CookieManager.removeAllCookies callback: removed=$removed")
                    }
                    flush()
                }
            }
            Timber.d("forgetAccount: Logout process complete")
        }
    }
}
