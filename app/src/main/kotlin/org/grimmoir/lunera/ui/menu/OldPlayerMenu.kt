package org.grimmoir.lunera.ui.menu
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.grimmoir.lunera.models.MediaMetadata
import org.grimmoir.lunera.ui.component.BottomSheetState
@Composable
fun OldPlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata?.let { LuneraLocalPlayerMenu(it, onDismiss, playerMenu = true) }
}
