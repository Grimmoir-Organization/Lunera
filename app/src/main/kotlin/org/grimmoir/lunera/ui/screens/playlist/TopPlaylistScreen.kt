/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens.playlist
import org.grimmoir.lunera.ui.utils.FloatingChromeSpacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import org.grimmoir.lunera.ui.ios.compat.Button
import androidx.compose.material3.ButtonDefaults
import org.grimmoir.lunera.ui.ios.compat.Icon
import org.grimmoir.lunera.ui.ios.compat.Surface
import org.grimmoir.lunera.ui.ios.compat.OutlinedTextField as TextField
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFShuffle
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import org.grimmoir.lunera.utils.rememberPreference
import coil3.compose.AsyncImage
import org.grimmoir.lunera.ui.utils.bounceClick
import org.grimmoir.lunera.ui.component.buildAlphabetSectionIndex
import org.grimmoir.lunera.ui.component.LargeScreenTitle
import org.grimmoir.lunera.ui.component.AnimatedPlayPauseIcon
import org.grimmoir.lunera.ui.component.ExpandableText
import org.grimmoir.lunera.ui.component.rememberHeroTint
import org.grimmoir.lunera.ui.player.MeshGradientBackground
import org.grimmoir.lunera.ui.player.dimmed
import org.grimmoir.lunera.ui.theme.AppleTokens
import org.grimmoir.lunera.ui.component.ContrastCircleButton
import java.time.LocalDateTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.component.SymbolIcon
import org.grimmoir.lunera.ui.component.chromeShadow
import org.grimmoir.lunera.ui.utils.appTopBarWindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import org.grimmoir.lunera.ui.ios.compat.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import org.grimmoir.lunera.ui.ios.compat.IconButton
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.Text
import org.grimmoir.lunera.ui.ios.compat.TextButton
import com.composables.sfsymbols.monochrome.SFXmark
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.grimmoir.lunera.ui.utils.rememberHeroZoom
import org.grimmoir.lunera.ui.utils.heroPullZoom
import org.grimmoir.lunera.ui.utils.listOverscroll
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import org.grimmoir.lunera.LocalDownloadUtil
import org.grimmoir.lunera.ui.utils.combinedBounceClick
import org.grimmoir.lunera.LocalPlayerAwareWindowInsets
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.R
import org.grimmoir.lunera.constants.MyTopFilter
import org.grimmoir.lunera.db.entities.Song
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.playback.queues.ListQueue
import org.grimmoir.lunera.ui.component.ListScrollRail
import org.grimmoir.lunera.ui.component.DefaultDialog
import org.grimmoir.lunera.ui.component.EmptyPlaceholder
import org.grimmoir.lunera.ui.component.IconButton
import org.grimmoir.lunera.ui.component.LocalMenuState
import org.grimmoir.lunera.ui.component.SongListItem
import org.grimmoir.lunera.ui.component.SortHeader
import org.grimmoir.lunera.ui.menu.SelectionSongMenu
import org.grimmoir.lunera.ui.menu.SongMenu
import org.grimmoir.lunera.ui.menu.TopPlaylistMenu
import org.grimmoir.lunera.ui.utils.backToMain
import org.grimmoir.lunera.utils.listItemShape
import org.grimmoir.lunera.utils.makeTimeString
import org.grimmoir.lunera.viewmodels.TopPlaylistViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.grimmoir.lunera.ui.theme.HeroTintedContent
import org.grimmoir.lunera.ui.component.GlassComponent
import org.grimmoir.lunera.ui.component.LocalGlassEffectConfig
import org.grimmoir.lunera.ui.component.withFixedStyle
import org.grimmoir.lunera.ui.component.isGlassAllowed
import androidx.compose.ui.graphics.isSpecified
import org.grimmoir.lunera.LocalAmbientBackgroundColor
import org.grimmoir.lunera.ui.component.asSoftIconTint
import org.grimmoir.lunera.ui.utils.additiveChrome
import org.grimmoir.lunera.ui.component.liquidGlass
import org.grimmoir.lunera.ui.component.shapes.ContinuousRoundedRectangle
import org.grimmoir.lunera.ui.component.LocalAppBackdrop
import org.grimmoir.lunera.ui.component.backdrop.backdrops.layerBackdrop
import org.grimmoir.lunera.ui.component.backdrop.backdrops.rememberBackdropFreeze
import org.grimmoir.lunera.ui.component.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.CompositionLocalProvider
import org.grimmoir.lunera.ui.ios.compat.LocalContentColor
import org.grimmoir.lunera.ui.component.GlassCircleButton
import org.grimmoir.lunera.ui.component.ChromeScrim
import org.grimmoir.lunera.ui.component.rememberChromeScrimProgress
import androidx.compose.ui.draw.clip
import org.grimmoir.lunera.playback.DownloadTarget
import org.grimmoir.lunera.playback.combinedDownloadState
import org.grimmoir.lunera.playback.cancelDownloads
import org.grimmoir.lunera.playback.downloadSongs
import org.grimmoir.lunera.playback.removeDownloads
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsState(null)
    val currentSongs = songs
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val exitSearch = {
        focusManager.clearFocus()
        isSearching = false
        query = TextFieldValue()
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    val filteredSongs = remember(songs, query) {
        if (query.text.isEmpty()) songs ?: emptyList()
        else songs?.filter { song ->
            song.title.contains(query.text, true) ||
                song.artists.any { it.name.contains(query.text, true) }
        } ?: emptyList()
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    if (isSearching) {
        BackHandler(onBack = exitSearch)
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val sortType by viewModel.topPeriod.collectAsStateWithLifecycle()
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current.get()
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    val downloadState = remember(songs, downloads) {
        combinedDownloadState(songs, downloads) { it.song.id }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, name),
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        removeDownloads(context, songs?.map { it.song.id }.orEmpty())
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val state = rememberLazyListState()

    val heroUrl = songs?.firstOrNull()?.song?.thumbnailUrl
    // Instant seed: route thumb -> live tile thumb. Paints mesh bg frame one.
    val instantTopThumb = org.grimmoir.lunera.ui.component.detailSeed(
        viewModel.thumbArg,
        org.grimmoir.lunera.ui.utils.pendingSharedArtworkThumbFor(viewModel.top),
        heroUrl,
    )
    val bgTopUrl = instantTopThumb ?: heroUrl
    val topChrome = org.grimmoir.lunera.ui.component.rememberDetailChromeEntrance(iconCount = 1)
    val appearance = org.grimmoir.lunera.ui.component.rememberPlaylistAppearance(bgTopUrl, 0xFFFF9F0A.toInt())
    val tint = appearance.background
    org.grimmoir.lunera.PublishAmbientBackgroundColor(tint)
    val onTint = appearance.foreground

    // Keep top-bar glass fixed; the global preference controls the nav bar and mini player.
    val glassConfig = LocalGlassEffectConfig.current.withFixedStyle()
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassAllowed()
    val heroBackdrop = rememberLayerBackdrop()
    val hazeState = remember { HazeState() }

    // Capture the tinted list behind sibling chrome; an opaque capture avoids a doubled image.
    val listBackdrop = rememberLayerBackdrop()
    val backdropFreeze = rememberBackdropFreeze()
    val heroZoom = rememberHeroZoom()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint)
    ) {
        HeroTintedContent(tint = tint, backdrop = heroBackdrop) {
        val chromeShape = ContinuousRoundedRectangle(percent = 50)
        val chromeBackgroundModifier = if (useGlass) {
            Modifier.chromeShadow(chromeShape).liquidGlass(config = glassConfig, shape = chromeShape)
        } else {
            Modifier.background(LocalContentColor.current.copy(alpha = 0.15f), chromeShape)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Capture through a plain Box; LazyColumn's recycled item layers do not flatten reliably.
            Box(modifier = Modifier
            .nestedScroll(backdropFreeze.connection)

            // Cache list content so sibling redraws do not re-record the backdrop.
            .graphicsLayer()
            .layerBackdrop(listBackdrop, frozen = backdropFreeze.frozen)
            .graphicsLayer()
            .hazeSource(hazeState)) {
                org.grimmoir.lunera.ui.component.ArtworkMeshBackground(bgTopUrl, paused = { state.isScrollInProgress }, appearance = appearance)
            LazyColumn(
                state = state,
                overscrollEffect = heroZoom.listOverscroll(),
                modifier = Modifier.heroPullZoom(heroZoom),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                if (currentSongs != null) {
                        item(key = "playlist_header") {
                            TopPlaylistHeader(
                                onStartSearch = { isSearching = true },
                                searchValue = query.takeIf { isSearching },
                                onSearchValueChange = { query = it },
                                onSearchImeAction = { focusManager.clearFocus() },
                                searchFocusRequester = focusRequester,
                                    name = name,
                                    songs = currentSongs,
                                    likeLength = likeLength,
                                    downloadState = downloadState,
                                    onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                    menuState = menuState,
                                    modifier = Modifier.animateItem()
                                )
                        }

                    if (currentSongs.isEmpty()) {
                        item(key = "empty_placeholder") {
                            EmptyPlaceholder(
                                icon = R.drawable.music_note,
                                text = stringResource(R.string.playlist_is_empty),
                                modifier = Modifier.padding(top = 100.dp)
                            )
                        }
                    } else {
                        item(key = "songs_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = false,
                                    onSortTypeChange = {
                                        viewModel.topPeriod.value = it
                                    },
                                    onSortDescendingChange = {},
                                    sortTypeText = { sortType ->
                                        when (sortType) {
                                            MyTopFilter.ALL_TIME -> R.string.all_time
                                            MyTopFilter.DAY -> R.string.past_24_hours
                                            MyTopFilter.WEEK -> R.string.past_week
                                            MyTopFilter.MONTH -> R.string.past_month
                                            MyTopFilter.YEAR -> R.string.past_year
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    showDescending = false,
                                )
                            }
                        }
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> "song_row" },
                    ) { index, song ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(song.id)
                            } else {
                                selection.remove(song.id)
                            }
                        }

                        SongListItem(
                            song = song,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            flat = true,
                            secondaryAdditiveChrome = onTint == androidx.compose.ui.graphics.Color.White,
                            subtitleAdditiveChrome = false,
                            subtitleColor = onTint.copy(alpha = 0.78f),
                            shape = listItemShape(index, filteredSongs.size),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = song.id in selection,
                                        onCheckedChange = onCheckedChange
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        SymbolIcon(symbol = sf("ellipsis"), contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedBounceClick(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else if (song.song.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            songs?.let { currentSongs ->
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = name,
                                                        items = currentSongs.map { it.toMediaItem() },
                                                        startIndex = currentSongs.indexOfFirst { it.id == song.id }
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    },
                                )
                                .animateItem()
                        )
                    }
                }
                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(100.dp))
                }
            }
            }


            // Ranked by play count, never alphabetically, so the rail is a
            // proportional thumb rather than letters.
            ListScrollRail(
                lazyListState = state,
                itemCount = filteredSongs.size,
                sectionIndexMap = null,
            )

            val chromeScrimProgress = rememberChromeScrimProgress(state)
            // Sample the list's own attached capture (a sibling, so no
            // RenderNode cycle) rather than the ambient unattached heroBackdrop,
            // whose drawBackdrop early-returns and left this chrome flat.
            CompositionLocalProvider(LocalAppBackdrop provides listBackdrop) {
            org.grimmoir.lunera.ui.component.TopFadeBlur(hazeState, tint, Modifier.align(Alignment.TopCenter))
            // Build the modifier inside this provider so it captures the attached list backdrop.
            val chromeBackgroundModifier = if (useGlass) {
                Modifier.chromeShadow(chromeShape).liquidGlass(config = glassConfig, shape = chromeShape)
            } else {
                Modifier.background(LocalContentColor.current.copy(alpha = 0.15f), chromeShape)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
            ChromeScrim(progress = chromeScrimProgress)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(appTopBarWindowInsets())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (inSelectMode) {
                    GlassCircleButton(onClick = onExitSelectionMode) {
                        SymbolIcon(symbol = sf("SFXmark"), contentDescription = null)
                    }

                    Text(
                        text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                        style = AppTheme.typography.titleMedium,
                        color = onTint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    org.grimmoir.lunera.ui.component.GlassPillRow(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(chromeShape)
                            .then(org.grimmoir.lunera.ui.component.pillGlassBackground(chromeShape))
                            .padding(horizontal = 8.dp),
                    ) {
                        Checkbox(
                            checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == filteredSongs.size) {
                                    selection.clear()
                                } else {
                                    selection.clear()
                                    selection.addAll(filteredSongs.map { it.id })
                                }
                            }
                        )
                        val ambientColor = LocalAmbientBackgroundColor.current.value
                        IconButton(
                            enabled = selection.isNotEmpty(),
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = filteredSongs.filter { it.id in selection },
                                        onDismiss = menuState::dismiss,
                                        clearAction = onExitSelectionMode,
                                    )
                                }
                            },
                        ) {
                            SymbolIcon(symbol = sf("ellipsis"), contentDescription = null, modifier = Modifier.additiveChrome(useGlass && ambientColor.isSpecified), tint = if (useGlass && ambientColor.isSpecified) ambientColor.asSoftIconTint() else onTint)
                        }
                    }
                } else {
                    GlassCircleButton(
                        onClick = { if (isSearching) exitSearch() else navController.navigateUp() },
                        onLongClick = { if (isSearching) exitSearch() else navController.backToMain() },
                        modifier = with(topChrome) {
                            Modifier.backEnter().then(org.grimmoir.lunera.ui.component.backExitModifier())
                        },
                    ) {
SymbolIcon(symbol = sf("chevron.left"), contentDescription = null)
                    }

                    Text(
                        text = name,
                        color = onTint,
                        style = AppTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )

                }
            }
            }
            }
        }
      }
    }
}

@Composable
private fun TopPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    downloadState: Int,
    onShowRemoveDownloadDialog: () -> Unit,
    menuState: org.grimmoir.lunera.ui.component.MenuState,
    modifier: Modifier = Modifier,
    onStartSearch: () -> Unit = {},
    searchValue: TextFieldValue? = null,
    onSearchValueChange: (TextFieldValue) -> Unit = {},
    onSearchImeAction: () -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
val context = LocalContext.current
    val downloads by LocalDownloadUtil.current.get().downloads.collectAsStateWithLifecycle()
    val title = name
    val artwork = org.grimmoir.lunera.ui.utils.pendingSharedArtworkThumbFor(org.grimmoir.lunera.ui.utils.LocalMorphArtworkId.current)
        ?: songs.firstOrNull()?.thumbnailUrl
    val appearance = org.grimmoir.lunera.ui.component.rememberPlaylistAppearance(artwork)
    org.grimmoir.lunera.ui.component.CollectionDetailHeader(
        title = title,
        artwork = artwork,
        metadata = pluralStringResource(R.plurals.n_song, songs.size, songs.size) +
            if (likeLength > 0) " \u00b7 " + makeTimeString(likeLength * 1000L) else "",
        foreground = appearance.foreground,
        playing = isPlaying && songs.any { it.id == mediaMetadata?.id },
        enabled = songs.isNotEmpty(),
        onPlay = {
            if (songs.any { it.id == mediaMetadata?.id }) playerConnection.togglePlayPause()
            else playerConnection.playQueue(ListQueue(title = title, items = songs.map { it.toMediaItem() }))
        },
        onShuffle = { playerConnection.playQueue(ListQueue(title = title, items = songs.shuffled().map { it.toMediaItem() })) },
        onSearch = onStartSearch,
        searchValue = searchValue,
        onSearchValueChange = onSearchValueChange,
        onSearchImeAction = onSearchImeAction,
        searchFocusRequester = searchFocusRequester,
        onRightAction = {
                        menuState.show {
                            TopPlaylistMenu(
                                downloadState = downloadState,
                                onQueue = {
                                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                },
                                onDownload = {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                                        Download.STATE_DOWNLOADING -> cancelDownloads(
                                            context,
                                            songs.map { it.id },
                                            downloads,
                                        )

                                        else -> downloadSongs(
                                            context,
                                            songs.map { DownloadTarget(it.id, it.title, it.artists.firstOrNull()?.name, it.song.duration) },
                                            downloads,
                                        )
                                    }
                                },
                                onDismiss = { menuState.dismiss() }
                            )
                        }
                    },
        modifier = modifier.padding(top = org.grimmoir.lunera.ui.component.topBarContentPadding()),
    )
}
