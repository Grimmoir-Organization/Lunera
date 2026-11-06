package org.grimmoir.lunera.ui.screens.settings

import androidx.compose.runtime.Composable

data class SearchableSetting(
    val title: String,
    val description: String?,
    val category: String,
    val route: String,
    val opensLyricsSheet: Boolean = false,
    val opensAppleMusicImport: Boolean = false,
    val keywords: String = "",
)

/** One-shot request for the Appearance screen to open the lyrics animation sheet on arrival. */
object SettingsDeepLink {
    private var lyricsSheet = false

    fun requestLyricsSheet() {
        lyricsSheet = true
    }

    fun consumeLyricsSheet(): Boolean = lyricsSheet.also { lyricsSheet = false }
}

@Composable
fun getAllSearchableSettings(): List<SearchableSetting> = listOf(
    SearchableSetting("Themes & colors", null, "Appearance", "settings/appearance/theme"),
    SearchableSetting("Liquid Glass", null, "Appearance", "settings/appearance/liquidglass"),
    SearchableSetting("Player design", null, "Appearance", "settings/appearance/playertheme"),
    SearchableSetting("Playback controls", null, "Player & audio", "settings/player"),
    SearchableSetting("Song transitions", null, "Player & audio", "settings/song_transitions"),
    SearchableSetting("Music folders", null, "Library", "settings/player/local_folders"),
    SearchableSetting("Updates", null, "About Lunera", "settings/update"),
    SearchableSetting("About Lunera", null, "About Lunera", "settings/about"),
    SearchableSetting("Crash report", null, "About Lunera", "settings/help/crash_logs")
)
