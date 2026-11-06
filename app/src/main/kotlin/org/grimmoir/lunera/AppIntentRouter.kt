package org.grimmoir.lunera
import android.content.Intent
import androidx.navigation.NavHostController
import org.grimmoir.lunera.playback.PlayerConnection
/** Lunera accepts local audio only; online sharing and room invitations have been removed. */
internal class AppIntentRouter(private val activity: MainActivity, private val currentPlayerConnection: () -> PlayerConnection?) {
    fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: return
        if (uri.scheme !in listOf("content", "file")) { intent.data = null; return }
        // MediaStore-backed open-with is handled through the local scan and library.
        navController.navigate("songs") { launchSingleTop = true }
        intent.data = null
    }
}
