package org.grimmoir.lunera.ui.screens.settings
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.grimmoir.lunera.ui.ios.compat.*
import org.grimmoir.lunera.ui.component.*
@Composable
fun AboutScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior, onBack: (() -> Unit)? = null) {
    SettingsPage(navController = navController, title = "About Lunera", horizontalPadding = 20.dp) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(org.grimmoir.lunera.R.drawable.lunera_logo), "Crimson moon", Modifier.size(88.dp))
        Spacer(Modifier.height(12.dp))
        Text("Lunera", style = AppTheme.typography.headlineLarge)
        Text("Version ${org.grimmoir.lunera.BuildConfig.VERSION_NAME} · Build ${org.grimmoir.lunera.BuildConfig.VERSION_CODE}", color = AppTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { navController.navigate("settings/changelog") }) { Text("What's new") }
        Spacer(Modifier.height(20.dp))
        IosSettingsGroup(items = listOf(IosSettingsItem(title = { Text("Grimmoir Organization") }, description = { Text("Play device music, organize playlists and read local lyrics. Internet access is used only for Lunera updates.") })))
        Spacer(Modifier.height(20.dp))
        Text("Free and open source · GPL-3.0", style = AppTheme.typography.titleMedium)
        Text("Original authors and dependency acknowledgements are preserved in the license notices and project source.", style = AppTheme.typography.bodySmall, color = AppTheme.colorScheme.onSurfaceVariant)
    }
}
