/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.only
import org.grimmoir.lunera.constants.StopMusicOnTaskClearKey
import org.grimmoir.lunera.utils.get
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import org.grimmoir.lunera.constants.AppLanguageKey
import org.grimmoir.lunera.constants.DisableScreenshotKey
import org.grimmoir.lunera.constants.SYSTEM_DEFAULT
import org.grimmoir.lunera.db.MusicDatabase
import org.grimmoir.lunera.history.HistoryLocalDataSource
import org.grimmoir.lunera.playback.DownloadUtil
import org.grimmoir.lunera.playback.MusicService
import org.grimmoir.lunera.playback.PlaybackServiceConnector
import org.grimmoir.lunera.playback.PlayerConnection
import org.grimmoir.lunera.utils.SyncUtils
import org.grimmoir.lunera.utils.dataStore
import org.grimmoir.lunera.utils.setAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PLAY_INTRO = "org.grimmoir.lunera.extra.PLAY_INTRO"
        internal const val ACTION_SEARCH = "org.grimmoir.lunera.action.SEARCH"
        internal const val ACTION_LIBRARY = "org.grimmoir.lunera.action.LIBRARY"
    }

    @Inject
    lateinit var database: dagger.Lazy<MusicDatabase>

    @Inject
    lateinit var downloadUtil: dagger.Lazy<DownloadUtil>

    @Inject
    lateinit var syncUtils: dagger.Lazy<SyncUtils>

    @Inject
    lateinit var historyDataSource: dagger.Lazy<HistoryLocalDataSource>



    @Inject
    lateinit var playbackServiceConnector: PlaybackServiceConnector

    private var pendingIntent: Intent? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var activityStarted = false
    private var pendingPlaybackBind: Runnable? = null
    private var introRunning = false
    private var playbackBindRequested = false

    override fun onResume() {
        super.onResume()
        org.grimmoir.lunera.utils.LuneraLibraryMonitor.request()
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        if (MusicService.isRunning) {
            bindPlaybackService()
        } else {
            schedulePlaybackBind()
        }
    }

    private fun schedulePlaybackBind() {
        if (!activityStarted || introRunning || playbackBindRequested || pendingPlaybackBind != null) return
        val bind = Runnable {
            pendingPlaybackBind = null
            if (activityStarted && !introRunning) bindPlaybackService()
        }
        pendingPlaybackBind = bind
        // Let the revealed UI draw before initializing an inactive playback service.
        window.decorView.postDelayed(bind, 32L)
    }

    private fun onIntroVisibilityChanged(visible: Boolean) {
        introRunning = visible
        if (visible) {
            pendingPlaybackBind?.let(window.decorView::removeCallbacks)
            pendingPlaybackBind = null
        } else {
            schedulePlaybackBind()
        }
    }

    override fun onStop() {
        activityStarted = false
        pendingPlaybackBind?.let(window.decorView::removeCallbacks)
        pendingPlaybackBind = null
        playbackBindRequested = false
        playbackServiceConnector.unbind(this)
        super.onStop()
    }

    private fun bindPlaybackService() {
        if (!activityStarted || playbackBindRequested) return
        playbackBindRequested = true
        // The service promotes itself when bound, so bind only while this activity is visible.
        playbackServiceConnector.bind(this, lifecycleScope) { connection ->
            playerConnection = connection
        }
    }

    internal fun takePendingIntent(): Intent? {
        val current = pendingIntent
        pendingIntent = null
        return current
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Retain the intent until Compose's listener is ready to handle it.
        pendingIntent = intent
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
            org.grimmoir.lunera.ui.component.MenuAnchor.lastDown =
                androidx.compose.ui.geometry.Offset(ev.x, ev.y)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Cold launch from the launcher only (WelcomeActivity hands off with this extra);
        // notification/deep-link opens go straight to the app.
        val playIntro = savedInstanceState == null && intent.getBooleanExtra(EXTRA_PLAY_INTRO, false)
        introRunning = playIntro
        setContent {
            var showIntro by rememberSaveable { mutableStateOf(playIntro) }
            // Also covers a saved intro restored after configuration changes.
            SideEffect { onIntroVisibilityChanged(showIntro) }
            Box(Modifier.fillMaxSize()) {
                AppRootContent(
                    activity = this@MainActivity,
                    intentRouter = deepLinkRouter,
                    playerConnection = playerConnection,
                    database = database,
                    downloadUtil = downloadUtil,
                    syncUtils = syncUtils,
                    historyDataSource = historyDataSource,
                )
                if (showIntro) org.grimmoir.lunera.ui.component.LuneraIntro(onFinished = { showIntro = false })
            }
        }
    }

    internal val deepLinkRouter by lazy {
        AppIntentRouter(this, { playerConnection })
    }
}
