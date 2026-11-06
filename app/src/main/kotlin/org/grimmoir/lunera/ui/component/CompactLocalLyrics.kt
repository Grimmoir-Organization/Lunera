/** Copyright (C) 2026 Grimmoir Organization. SPDX-License-Identifier: GPL-3.0-only */
package org.grimmoir.lunera.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.grimmoir.lunera.lyrics.LyricsEntry
import org.grimmoir.lunera.lyrics.primaryLyricLineIndex
import org.grimmoir.lunera.utils.LocalLyricsTiming

@Composable
fun CompactLocalLyrics(
    rawText: String, lines: List<LyricsEntry>, synced: Boolean,
    fontFamily: FontFamily, textColor: Color, fontSize: Float,
    followPlayback: Boolean, positionMs: () -> Long, onSeek: (Long) -> Unit,
) {
    val state = rememberLazyListState()
    val dragged by state.interactionSource.collectIsDraggedAsState()
    var resumeFollowAt by remember { mutableLongStateOf(0L) }
    val display = remember(lines, rawText, synced) { if (synced) lines.filter { it.text.isNotBlank() } else emptyList() }
    val plain = remember(rawText) { LocalLyricsTiming.cleanPlainText(rawText).lines() }
    var active by remember(rawText) { mutableIntStateOf(-1) }
    val currentPosition by rememberUpdatedState(positionMs)
    LaunchedEffect(display, synced) {
        if (synced) while (true) {
            active = primaryLyricLineIndex(display, currentPosition())
            delay(200)
        }
    }
    LaunchedEffect(dragged) { if (dragged) resumeFollowAt = android.os.SystemClock.elapsedRealtime() + 6000L }
    LaunchedEffect(active, followPlayback) {
        if (synced && followPlayback && !dragged && active >= 0 && android.os.SystemClock.elapsedRealtime() >= resumeFollowAt) {
            state.animateScrollToItem(active, -(state.layoutInfo.viewportSize.height / 3))
        }
    }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(if (synced) 16.dp else 8.dp)) {
        if (synced) itemsIndexed(display) { index, line ->
            Text(line.text, fontFamily = fontFamily, fontSize = fontSize.sp, lineHeight = (fontSize * 1.4f).sp,
                fontWeight = if (index == active) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor.copy(alpha = if (index == active || active < 0) 1f else .68f),
                modifier = Modifier.fillMaxWidth().clickable { onSeek(line.time.toLong()) })
        } else itemsIndexed(plain) { _, text ->
            Text(text, fontFamily = fontFamily, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal, color = textColor, modifier = Modifier.fillMaxWidth())
        }
    }
}
