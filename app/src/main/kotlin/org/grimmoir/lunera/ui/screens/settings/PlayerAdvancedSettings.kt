package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.runtime.Composable
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavController
/** Legacy route opens the local playback controls after online settings were removed. */
@Composable
fun PlayerAdvancedSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) { PlayerSettings(navController, scrollBehavior) }
