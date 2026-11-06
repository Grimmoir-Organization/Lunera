/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import org.grimmoir.lunera.ui.ios.foundation.IosTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.layout.layout
import org.grimmoir.lunera.ui.component.shapes.ContinuousCapsule
import org.grimmoir.lunera.ui.component.shapes.ContinuousRoundedRectangle
import org.grimmoir.lunera.ui.ios.foundation.IosMetrics
import org.grimmoir.lunera.ui.ios.compat.HorizontalDivider
import org.grimmoir.lunera.ui.ios.compat.Switch
import org.grimmoir.lunera.ui.ios.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.grimmoir.lunera.R
import org.grimmoir.lunera.constants.LyricsAnimationStyle
import org.grimmoir.lunera.constants.LyricsAnimationStyleKey
import org.grimmoir.lunera.constants.LyricsTextPositionKey
import org.grimmoir.lunera.constants.SwipeLyricsKey
import org.grimmoir.lunera.utils.rememberEnumPreference
import org.grimmoir.lunera.utils.rememberPreference
import org.grimmoir.lunera.constants.LyricsScrollKey

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

@Composable
fun ColumnScope.LyricsAnimationSheetContent() {
    val (selected, onSelect) = rememberEnumPreference(LyricsAnimationStyleKey, defaultValue = LyricsAnimationStyle.FLAMINGO)
    val (position, onPositionChange) = rememberEnumPreference(LyricsTextPositionKey, defaultValue = LyricsPosition.LEFT)
    val (autoScroll, onAutoScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (swipeToChange, onSwipeToChangeChange) = rememberPreference(SwipeLyricsKey, defaultValue = false)
    val animation = androidx.compose.animation.core.rememberInfiniteTransition(label = "lyrics sample")
    val progress by animation.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "sample lyric progress",
    )
    val selectedColor by androidx.compose.animation.animateColorAsState(
        targetValue = IosTheme.tint,
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "lyrics style accent",
    )

    val align = when (position) {
        LyricsPosition.LEFT -> androidx.compose.ui.text.style.TextAlign.Start
        LyricsPosition.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        LyricsPosition.RIGHT -> androidx.compose.ui.text.style.TextAlign.End
    }
    val fill = selected == LyricsAnimationStyle.FLAMINGO || selected == LyricsAnimationStyle.KARAOKE || selected == LyricsAnimationStyle.ARCHIVE_TUNE ||
        selected == LyricsAnimationStyle.APPLE || selected == LyricsAnimationStyle.APPLE_V2 ||
        selected == LyricsAnimationStyle.BITCHORD

    run {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Tap a style to preview and apply it",
                    style = IosTheme.typography.footnote,
                    color = IosTheme.colors.secondaryLabel,
                )
            }

            // The playing song's own lyrics, through the player's own lyrics view: what is
            // seen here is exactly what the player shows in the chosen style.
            val lyricsConnection = org.grimmoir.lunera.LocalPlayerConnection.current
            val lyricsTrack by remember(lyricsConnection) {
                lyricsConnection?.mediaMetadata ?: kotlinx.coroutines.flow.MutableStateFlow(null)
            }.collectAsStateWithLifecycle()
            val lyricsPlaying by remember(lyricsConnection) {
                lyricsConnection?.isPlaying ?: kotlinx.coroutines.flow.MutableStateFlow(false)
            }.collectAsStateWithLifecycle()
            // Paused or not, the preview keeps singing: a clock carries on from where the song
            // stopped and loops at its end, so every style can be watched moving.
            var pausedClock by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
            LaunchedEffect(lyricsPlaying, lyricsTrack?.id) {
                if (lyricsPlaying || lyricsConnection == null) return@LaunchedEffect
                val from = lyricsConnection.player.currentPosition.coerceAtLeast(0L)
                val length = ((lyricsTrack?.duration ?: 0) * 1000L).coerceAtLeast(30_000L)
                val startedAt = System.currentTimeMillis()
                while (true) {
                    androidx.compose.runtime.withFrameMillis { }
                    pausedClock = (from + System.currentTimeMillis() - startedAt) % length
                }
            }
            val previewClock: () -> Long = {
                if (lyricsPlaying) lyricsConnection?.player?.currentPosition ?: 0L else pausedClock
            }
            if (lyricsConnection != null && lyricsTrack != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(ContinuousRoundedRectangle(24.dp)),
                ) {
                    org.grimmoir.lunera.ui.player.NowPlayingBackground(
                        artworkUrl = lyricsTrack?.thumbnailUrl,
                        hero = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                    org.grimmoir.lunera.ui.player.InlineLyricsView(
                        mediaMetadata = lyricsTrack,
                        showLyrics = true,
                        positionProvider = previewClock,
                    )
                    Text(
                        lyricsTrack?.title.orEmpty(),
                        color = Color.White.copy(alpha = 0.6f),
                        style = IosTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(ContinuousRoundedRectangle(24.dp))
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(selectedColor.copy(alpha = 0.38f).compositeOver(Color(0xFF111217)), Color(0xFF111217)))),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    ) {
                        Text(
                            "Where the light meets the sea",
                            color = Color.White.copy(alpha = 0.38f),
                            style = IosTheme.typography.subheadline,
                            textAlign = align,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "We find our way back home",
                            style = IosTheme.typography.title3.let {
                                when {
                                    fill -> it.copy(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            0f to selectedColor,
                                            progress to selectedColor,
                                            (progress + 0.001f).coerceAtMost(1f) to Color.White.copy(alpha = 0.4f),
                                            1f to Color.White.copy(alpha = 0.4f),
                                        ),
                                    )
                                    selected == LyricsAnimationStyle.GLOW -> it.copy(
                                        color = selectedColor,
                                        shadow = androidx.compose.ui.graphics.Shadow(selectedColor, blurRadius = 8f + progress * 22f),
                                    )
                                    else -> it.copy(color = selectedColor)
                                }
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = align,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().graphicsLayer {
                                when (selected) {
                                    LyricsAnimationStyle.NONE -> Unit
                                    LyricsAnimationStyle.FADE -> alpha = 0.45f + progress * 0.55f
                                    LyricsAnimationStyle.SLIDE -> {
                                        translationX = (1f - progress) * 28.dp.toPx() * if (position == LyricsPosition.RIGHT) -1f else 1f
                                        alpha = 0.5f + progress * 0.5f
                                    }
                                    else -> {
                                        scaleX = 0.94f + progress * 0.06f
                                        scaleY = scaleX
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                            when (position) {
                                                LyricsPosition.LEFT -> 0f
                                                LyricsPosition.CENTER -> 0.5f
                                                LyricsPosition.RIGHT -> 1f
                                            },
                                            0.5f,
                                        )
                                    }
                                }
                            },
                        )
                        Text(
                            "Under the open sky",
                            color = Color.White.copy(alpha = 0.38f),
                            style = IosTheme.typography.subheadline,
                            textAlign = align,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "SAMPLE",
                        color = Color.White.copy(alpha = 0.45f),
                        style = IosTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                // Bleed to the sheet edge while the first chip still lines up with the 20dp gutter.
                modifier = Modifier.layout { measurable, constraints ->
                    val bleed = 32.dp.roundToPx()
                    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed / 2, 0) }
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(LyricsAnimationStyle.entries) { style ->
                    val active = style == selected
                    Text(
                        text = org.grimmoir.lunera.ui.component.lyricsStyleLabel(style),
                        color = if (active) Color.White else IosTheme.colors.label,
                        style = IosTheme.typography.subheadline,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(ContinuousCapsule())
                            .background(if (active) selectedColor else IosTheme.colors.secondarySystemFill)
                            .clickable { onSelect(style) }
                            .defaultMinSize(minHeight = 40.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = 16.dp),
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(IosMetrics.GroupedCornerRadius))
                    .background(IosTheme.colors.secondarySystemGroupedBackground),
            ) {
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.lyrics_text_position),
                        style = IosTheme.typography.body,
                        color = IosTheme.colors.label,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        Modifier
                            .clip(ContinuousCapsule())
                            .background(IosTheme.colors.secondarySystemFill)
                            .padding(3.dp),
                    ) {
                        LyricsPosition.entries.forEach { p ->
                            val active = p == position
                            Text(
                                text = when (p) {
                                    LyricsPosition.LEFT -> stringResource(R.string.left)
                                    LyricsPosition.CENTER -> stringResource(R.string.center)
                                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                                },
                                color = if (active) Color.White else IosTheme.colors.label,
                                style = IosTheme.typography.footnote,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(ContinuousCapsule())
                                    .background(if (active) selectedColor else Color.Transparent)
                                    .clickable { onPositionChange(p) }
                                    .defaultMinSize(minHeight = 34.dp)
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp), color = IosTheme.colors.separator, thickness = IosMetrics.SeparatorThickness)
                LyricsSheetSwitch(stringResource(R.string.lyrics_auto_scroll), autoScroll, onAutoScrollChange)
                HorizontalDivider(Modifier.padding(start = 16.dp), color = IosTheme.colors.separator, thickness = IosMetrics.SeparatorThickness)
                LyricsSheetSwitch(stringResource(R.string.lyrics_swipe_to_change_song), swipeToChange, onSwipeToChangeChange)
            }
        }
    }
}

@Composable
private fun LyricsSheetSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = IosTheme.typography.body, color = IosTheme.colors.label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
