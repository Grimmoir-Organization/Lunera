package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.grimmoir.lunera.constants.*
import org.grimmoir.lunera.utils.rememberPreference
import org.grimmoir.lunera.ui.ios.compat.*
@Composable
fun SongTransitionsScreen(navController: NavController) {
    val (enabled, setEnabled) = rememberPreference(CrossfadeEnabledKey, false)
    val (duration, setDuration) = rememberPreference(CrossfadeDurationKey, 5f)
    SettingsPage(navController = navController, title = "Song transitions") {
        Text("Crossfade"); Switch(enabled, setEnabled)
        Text("Duration: ${duration.toInt()} seconds")
        Slider(value = duration, onValueChange = setDuration, valueRange = 1f..12f, enabled = enabled)
    }
}
