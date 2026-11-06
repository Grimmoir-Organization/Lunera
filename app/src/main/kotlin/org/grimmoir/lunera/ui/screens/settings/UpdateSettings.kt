package org.grimmoir.lunera.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.grimmoir.lunera.BuildConfig
import org.grimmoir.lunera.core.updater.*
import org.grimmoir.lunera.ui.component.*
import org.grimmoir.lunera.ui.ios.compat.*

@Composable
fun UpdateSettings(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    var automatic by remember { mutableStateOf(getAutoUpdateCheckSetting(context)) }
    var notifications by remember { mutableStateOf(getUpdateNotificationsSetting(context)) }
    SettingsPage(navController, title = "Updates", horizontalPadding = 16.dp) {
        IosSettingsGroup(items = listOf(
            IosSettingsItem(title = { Text("Installed version") }, description = { Text("Lunera ${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}") }),
            IosSettingsItem(title = { Text("Check for updates") }, description = { Text("Connect to the Lunera release service") }, onClick = { navController.navigate("update") }),
            IosSettingsItem(title = { Text("Check automatically") }, trailingContent = { Switch(automatic, { automatic = it; saveAutoUpdateCheckSetting(context, it) }) }, onClick = { automatic = !automatic; saveAutoUpdateCheckSetting(context, automatic) }),
            IosSettingsItem(title = { Text("Update notifications") }, trailingContent = { Switch(notifications, { notifications = it; saveUpdateNotificationsSetting(context, it) }) }, onClick = { notifications = !notifications; saveUpdateNotificationsSetting(context, notifications) }),
            IosSettingsItem(title = { Text("What's new") }, onClick = { navController.navigate("settings/changelog") })
        ))
        Spacer(Modifier.height(16.dp))
        Text("Music playback stays offline. Only update checks and downloads use the internet.", style = AppTheme.typography.bodySmall)
    }
}
