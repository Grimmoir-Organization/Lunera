package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.grimmoir.lunera.constants.*
import org.grimmoir.lunera.utils.rememberPreference
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.component.IosSettingsGroup
import org.grimmoir.lunera.ui.component.IosSettingsItem
@Composable
fun PlayerSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val (queue, setQueue) = rememberPreference(PersistentQueueKey, true)
    val (silence, setSilence) = rememberPreference(SkipSilenceKey, false)
    val (normal, setNormal) = rememberPreference(AudioNormalizationKey, true)
    val (repeat, setRepeat) = rememberPreference(RememberShuffleAndRepeatKey, true)
    val (mute, setMute) = rememberPreference(PauseOnMute, false)
    val (screen, setScreen) = rememberPreference(KeepScreenOn, false)
    SettingsPage(navController = navController, title = "Player & audio") {
        IosSettingsGroup(title = "Playback", items = listOf(
            IosSettingsItem(title = { Text("Remember last queue") }, trailingContent = { Switch(queue, setQueue) }, onClick = { setQueue(!queue) }),
            IosSettingsItem(title = { Text("Remember shuffle and repeat") }, trailingContent = { Switch(repeat, setRepeat) }, onClick = { setRepeat(!repeat) })
        ))
        IosSettingsGroup(title = "Audio", items = listOf(
            IosSettingsItem(title = { Text("Normalize volume") }, trailingContent = { Switch(normal, setNormal) }, onClick = { setNormal(!normal) }),
            IosSettingsItem(title = { Text("Skip silence") }, trailingContent = { Switch(silence, setSilence) }, onClick = { setSilence(!silence) }),
            IosSettingsItem(title = { Text("Pause when muted") }, trailingContent = { Switch(mute, setMute) }, onClick = { setMute(!mute) }),
            IosSettingsItem(title = { Text("Keep screen on") }, trailingContent = { Switch(screen, setScreen) }, onClick = { setScreen(!screen) })
        ))
    }
}
