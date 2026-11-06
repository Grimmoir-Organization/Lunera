/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package org.grimmoir.lunera.ui.menu
import org.grimmoir.lunera.ui.component.DownloadRingIcon
import org.grimmoir.lunera.ui.component.sf
import org.grimmoir.lunera.ui.component.SymbolIcon
import org.grimmoir.lunera.ui.component.menuBottomInset
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.grimmoir.lunera.ui.ios.compat.CircularProgressIndicator
import org.grimmoir.lunera.ui.ios.compat.HorizontalDivider
import org.grimmoir.lunera.ui.ios.compat.Icon
import org.grimmoir.lunera.ui.ios.compat.IconButton
import org.grimmoir.lunera.ui.ios.compat.ListItem
import org.grimmoir.lunera.ui.ios.compat.LocalContentColor
import org.grimmoir.lunera.ui.ios.compat.AppTheme
import org.grimmoir.lunera.ui.ios.compat.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import org.grimmoir.lunera.LocalDatabase
import org.grimmoir.lunera.LocalDownloadUtil
import org.grimmoir.lunera.LocalPlayerConnection
import org.grimmoir.lunera.LocalSyncUtils
import org.grimmoir.lunera.R
import org.grimmoir.lunera.constants.ListItemHeight
import org.grimmoir.lunera.constants.ListThumbnailSize
import org.grimmoir.lunera.constants.ThumbnailCornerRadius
import org.grimmoir.lunera.constants.ThumbnailRoundedShape
import org.grimmoir.lunera.db.entities.SpeedDialItem
import org.grimmoir.lunera.db.entities.SongEntity
import org.grimmoir.lunera.extensions.toMediaItem
import org.grimmoir.lunera.models.MediaMetadata
import org.grimmoir.lunera.models.toMediaMetadata
import org.grimmoir.lunera.playback.ExoDownloadService
import org.grimmoir.lunera.playback.queues.YouTubeQueue
import org.grimmoir.lunera.ui.component.ListDialog
import org.grimmoir.lunera.ui.component.LocalBottomSheetPageState
import org.grimmoir.lunera.ui.component.IosMenuGroup
import org.grimmoir.lunera.ui.component.IosMenuItemData
import org.grimmoir.lunera.ui.component.NewAction
import org.grimmoir.lunera.ui.component.NewActionGrid
import org.grimmoir.lunera.ui.utils.ShowMediaInfo
import org.grimmoir.lunera.ui.utils.resize
import org.grimmoir.lunera.utils.joinByBullet
import org.grimmoir.lunera.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.music.innertube.models.SongItem

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
    onHistoryRemoved: () -> Unit = {}
) {
    // Online song actions were removed from the local player.
}
