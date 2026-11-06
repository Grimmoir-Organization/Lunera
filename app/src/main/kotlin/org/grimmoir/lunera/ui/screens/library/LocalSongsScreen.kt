/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package org.grimmoir.lunera.ui.screens.library
import org.grimmoir.lunera.ui.ios.compat.Icon
import com.composables.sfsymbols.SfSymbols
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import org.grimmoir.lunera.ui.component.LargeScreenTitle
import org.grimmoir.lunera.ui.component.TopFadeBlur
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import org.grimmoir.lunera.utils.LuneraLibraryMonitor

import androidx.compose.foundation.background
import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.component.SymbolIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import org.grimmoir.lunera.ui.ios.compat.IconButton
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.grimmoir.lunera.LocalPlayerAwareWindowInsets
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.R
import org.grimmoir.lunera.constants.LocalSongSortDescendingKey
import org.grimmoir.lunera.constants.LocalSongSortTypeKey
import org.grimmoir.lunera.constants.SongSortType
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.playback.queues.ListQueue
import org.grimmoir.lunera.ui.component.HideOnScrollFAB
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.grimmoir.lunera.ui.component.ListScrollRail
import org.grimmoir.lunera.ui.component.LocalMenuState
import org.grimmoir.lunera.ui.component.SongListItem
import org.grimmoir.lunera.ui.component.SortOption
import org.grimmoir.lunera.ui.component.SortPopupButton
import org.grimmoir.lunera.ui.component.buildAlphabetSectionIndex
import org.grimmoir.lunera.ui.menu.SongMenu
import org.grimmoir.lunera.ui.theme.AppleTokens
import org.grimmoir.lunera.ui.utils.bounceClick
import org.grimmoir.lunera.utils.listItemShape
import org.grimmoir.lunera.utils.rememberEnumPreference
import org.grimmoir.lunera.utils.rememberPreference

/**
 * Every song on the device, in one flat list.
 *
 * This is the tab local-only mode adds. The library's own Songs screen still exists and
 * still has its filter chips; this one deliberately has none — it is the whole on-device
 * library and nothing else, which is what makes the alphabet rail meaningful here.
 *
 * Sorting happens in memory rather than by re-querying: the local library is already a
 * list in the view model, so a sort change is a comparator over a few thousand rows
 * instead of a database round-trip and a fresh Flow emission.
 */
@Composable
fun LocalSongsScreen(
    navController: NavController,
    viewModel: LocalMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    // Shares the keys the local Home rows use, so changing the sort in one place is not
    // contradicted by the other.
    val (sortType, onSortTypeChange) = rememberEnumPreference(LocalSongSortTypeKey, SongSortType.NAME)
    val (sortDescending, onSortDescendingChange) = rememberPreference(LocalSongSortDescendingKey, false)

    val sortedSongs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val lastPlayed by viewModel.lastPlayed.collectAsStateWithLifecycle()
    val audioPermission = org.grimmoir.lunera.ui.component.rememberLocalAudioPermission { LuneraLibraryMonitor.request() }
    val visibleSongs = if (audioPermission.granted) sortedSongs else emptyList()
    val sectionIndexMap = remember(sortedSongs, sortType) {
        when (sortType) {
            SongSortType.NAME -> buildAlphabetSectionIndex(sortedSongs) { it.title }
            SongSortType.ARTIST -> buildAlphabetSectionIndex(sortedSongs) { song ->
                song.artists.firstOrNull()?.name.orEmpty()
            }

            else -> null
        }
    }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(0f) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    var focusJob by remember { mutableStateOf<Job?>(null) }
    val songIds = remember(visibleSongs) { visibleSongs.map { it.id } }
    val target = remember(songIds, mediaMetadata?.id, lastPlayed) { SongFocusPosition.find(songIds, mediaMetadata?.id, lastPlayed) }
    val highlightColor = AppTheme.colorScheme.primary

    // Progressive top blur so the list dissolves under the status bar instead
    // of cutting off hard — same technique as the rest of the app's screens.
    val hazeState = remember { HazeState() }
    val pageInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val sideInset = pageInsets.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)

    // The list reads its order at tap time, not from whatever index a row was composed with, so a sort
    // change can never start the queue from the previous order.
    val currentSongs by androidx.compose.runtime.rememberUpdatedState(sortedSongs)

    // This tab has no page behind it in local mode, so it paints its own light or dark background.
    Box(modifier = Modifier.fillMaxSize().background(if (org.grimmoir.lunera.ui.component.hasCustomHomeBackground()) androidx.compose.ui.graphics.Color.Transparent else AppTheme.colorScheme.background)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = sideInset,
                top = maxOf(pageInsets.calculateTopPadding(), if (sideInset > 0.dp) 96.dp else 116.dp),
                end = pageInsets.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                bottom = pageInsets.calculateBottomPadding(),
            ),
        ) {
            item(key = "header", contentType = "header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = AppleTokens.Gutter),
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                        style = AppTheme.typography.titleSmall,
                        color = AppTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.weight(1f))
                    Text("Sort by", style = AppTheme.typography.labelMedium)
                    SortPopupButton(
                        options = LocalSongSortOptions,
                        selected = sortType,
                        descending = sortDescending,
                        onSelectedChange = onSortTypeChange,
                        onDescendingChange = onSortDescendingChange,
                    )
                }
            }

            if (!audioPermission.granted) {
                item(key = "permission", contentType = "local_access") {
                    org.grimmoir.lunera.ui.screens.library.apple.LocalAccessCard(
                        blocked = audioPermission.blocked,
                        onAllow = audioPermission.request,
                    )
                }
            } else if (songs.isEmpty() && !isScanning) {
                item(key = "empty", contentType = "local_access") {
                    org.grimmoir.lunera.ui.component.LocalMusicEmptyState(onScan = { navController.navigate("settings/player/local_folders") })
                }
            } else if (songs.isEmpty()) {
                item(key = "empty", contentType = "header") {
                    Text(
                        text = stringResource(
                            if (isScanning) R.string.scanning_local_files else R.string.no_local_files,
                        ),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppleTokens.Gutter),
                    )
                }
            }

            itemsIndexed(
                items = visibleSongs,
                key = { _, song -> song.id },
                contentType = { _, _ -> "song" },
            ) { index, song ->
                SongListItem(
                    song = song,
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    // On-device files carry neither a like state nor a download state.
                    showLikedIcon = false,
                    showDownloadIcon = false,
                    shape = listItemShape(index, sortedSongs.size),
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show(opaqueSurface = true) {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            SymbolIcon(bounceOnEnter = true, symbol = sf("ellipsis"), contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            if (focusedId == song.id) drawRoundRect(highlightColor.copy(alpha = 0.28f * pulse.value), cornerRadius = CornerRadius(14.dp.toPx()))
                        }
                        .bounceClick {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                val ordered = currentSongs
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = context.getString(R.string.songs),
                                        items = ordered.map { it.toMediaItem() },
                                        startIndex = ordered.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                                    ),
                                )
                            }
                        },
                )
            }
        }

        ListScrollRail(
            lazyListState = lazyListState,
            itemCount = visibleSongs.size,
            sectionIndexMap = sectionIndexMap,
        )

        HideOnScrollFAB(
            visible = visibleSongs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.songs),
                        items = sortedSongs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
            hazeState = hazeState,
            onFocusClick = {
                val position = target
                if (position != null) {
                    focusJob?.cancel()
                    focusJob = scope.launch {
                        pulse.snapTo(0f)
                        focusedId = null
                        if (kotlin.math.abs(lazyListState.firstVisibleItemIndex - position.rowIndex) > 12) {
                            lazyListState.scrollToItem((position.rowIndex - 2).coerceAtLeast(0))
                        }
                        lazyListState.animateScrollToItem(position.rowIndex)
                        focusedId = position.songId
                        pulse.snapTo(1f)
                        delay(120)
                        pulse.animateTo(0f, tween(600))
                        focusedId = null
                    }
                }
            },
            focusEnabled = target != null,
        )
    }
}

private val LocalSongSortOptions = listOf(
    SortOption(SongSortType.NAME, R.string.sort_by_name),
    SortOption(SongSortType.ARTIST, R.string.sort_by_artist),
    SortOption(SongSortType.CREATE_DATE, R.string.sort_by_create_date),
    SortOption(SongSortType.PLAY_TIME, R.string.sort_by_play_time),
)
